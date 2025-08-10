/*
 * LuaObject.java, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * $Id: LuaObject.java,v 1.7 2007-09-17 19:28:40 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava;

import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.type.*;
import org.eu.smileyik.luajava.util.ResourceCleaner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a Lua object of any type. A LuaObject is constructed by a {@link LuaState} object using one of
 * the four methods:
 * <ul>
 * <li>{@link LuaStateFacade#getLuaObject(String globalName)}</li>
 * <li>{@link LuaStateFacade#getLuaObject(LuaObject parent, String name)}</li>
 * <li>{@link LuaStateFacade#getLuaObject(LuaObject parent, Number name)}</li>
 * <li>{@link LuaStateFacade#getLuaObject(LuaObject parent, LuaObject name)}</li>
 * <li>{@link LuaStateFacade#getLuaObject(int index)}</li>
 * </ul>
 * The LuaObject will represent only the object itself, not a variable or a stack index, so when you change a string,
 * remember that strings are immutable objects in Lua, and the LuaObject you have will represent the old one.
 *
 * <h2>Proxies</h2>
 * <p>
 * LuaJava allows you to implement a class in Lua, like said before. If you want to create this proxy from Java, you
 * should have a LuaObject representing the table that has the functions that implement the interface. From this
 * LuaObject you can call the <code>createProxy(String implements)</code>. This method receives the string with the
 * name of the interfaces implemented by the object separated by comma.
 *
 * @author Rizzato
 * @author Thiago Ponte
 */
public class LuaObject implements ILuaObject, IInnerLuaObject, AutoCloseable {
    private static final class CleanTask implements Runnable {
        private final LuaStateFacade luaState;
        private final Integer ref;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private CleanTask(LuaStateFacade luaState, Integer ref) {
            this.luaState = luaState;
            this.ref = ref;
        }

        @Override
        public void run() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            // if lua state is closed that means this object also be cleaned.
            else if (luaState.isClosed()) {
                return;
            }
            try {
                luaState.lock(L -> {
                    if (L.getCPtrPeer() != 0)
                        L.LunRef(LuaState.LUA_REGISTRYINDEX, ref);
                });
            } catch (Exception e) {
                System.err.println("Unable to release object " + ref);
            }
        }
    }

    protected Integer ref;

    protected final LuaStateFacade luaState;
    private final CleanTask cleanTask;

    /**
     * Creates a reference to an object in the given index of the stack
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY</strong>
     *
     * @param luaState
     * @param index of the object on the lua stack
     * @see LuaObject#create(LuaStateFacade, int)
     */
    protected LuaObject(LuaStateFacade luaState, int index) {
        this.luaState = luaState;
        this.cleanTask = luaState.lock(L -> {
            L.pushValue(index);
            ref = L.Lref(LuaState.LUA_REGISTRYINDEX);

            CleanTask cleanTask = new CleanTask(luaState, ref);
            ResourceCleaner.getInstance().register(this, cleanTask);
            return cleanTask;
        });
    }

    /**
     * Creates a reference to an object in the given index of the stack
     *
     * @param luaState
     * @param index of the object on the lua stack
     */
    protected static Result<LuaObject, ? extends LuaException> create(LuaStateFacade luaState, int index) {
        return Result.success(luaState.lock((L) -> {
            if (L.isNil(index)) {
                return null;
            }
            return InnerTypeHelper.createLuaObject(luaState, index)
                    .orElseGet(() -> new LuaObject(luaState, index));
        }));
    }

    /**
     * Creates a reference to an object in the variable globalName
     *
     * @param luaState
     * @param globalName
     */
    protected static Result<LuaObject, ? extends LuaException> create(LuaStateFacade luaState, String globalName) {
        return luaState.lockThrow((L) -> {
            L.getGlobal(globalName);
            try {
                return create(luaState, -1).getOrThrow(LuaException.class);
            } finally {
                L.pop(1);
            }
        });
    }

    /**
     * Creates a reference to an object inside another object
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name that index the field
     */
    protected static Result<LuaObject, ? extends LuaException> create(LuaObject parent, String name) {
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata()) {
                throw new LuaException("Object parent should be a table or userdata .");
            }
            parent.push();
            L.pushString(name);
            L.getTable(-2);
            L.remove(-2);
            try {
                return create(luaState, -1).getOrThrow(LuaException.class);
            } finally {
                L.pop(1);
            }
        });
    }

    /**
     * This static method creates a LuaObject from a table that is indexed by a number.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (number) that index the field
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    protected static Result<LuaObject, ? extends LuaException> create(LuaObject parent, Number name) {
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata()) {
                throw new LuaException("Object parent should be a table or userdata .");
            }
            try {
                parent.push();
                L.pushNumber(name.doubleValue());
                L.getTable(-2);
                L.remove(-2);
                return create(luaState, -1).getOrThrow(LuaException.class);
            } finally {
                L.pop(1);
            }
        });
    }

    /**
     * This static method creates a LuaObject from a table that is indexed by a LuaObject.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (LuaObject) that index the field
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    protected static Result<LuaObject, ? extends LuaException> create(LuaObject parent, LuaObject name) {
        if (parent.getLuaState() != name.getLuaState())
            return Result.failure(new LuaException("LuaStates must be the same!"));
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata())
                throw new LuaException("Object parent should be a table or userdata .");

            parent.push();
            name.push();
            L.getTable(-2);
            L.remove(-2);
            try {
                return create(luaState, -1).getOrThrow(LuaException.class);
            } finally {
                L.pop(1);
            }
        });
    }

    /**
     * Gets the Object's State
     */
    @Override
    public LuaStateFacade getLuaState() {
        return luaState;
    }

    @Override
    public void close() {
        cleanTask.run();
    }

    @Override
    public boolean isClosed() {
        return luaState == null || luaState.isClosed();
    }

    /**
     * check this lua object is callable or not.
     * @return if callable then return true.
     */
    public boolean isCallable() {
        return !isClosed() && this instanceof ILuaCallable;
    }

    /**
     * Pushes the object represented by <code>this</code> into L's stack
     */
    @Override
    public void push() {
        if (isClosed()) return;
        luaState.lock(l -> {
            l.rawGetI(LuaState.LUA_REGISTRYINDEX, ref);
        });
    }

    @Override
    public void rawPush() {
        if (isClosed()) return;
        luaState.getLuaState().rawGetI(LuaState.LUA_REGISTRYINDEX, ref);
    }

    public boolean isJavaObject() {
        if (isClosed()) return false;
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isObject(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isJavaFunction() {
        if (isClosed()) return false;
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isJavaFunction(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public int type() {
        if (isClosed()) return LuaType.NIL;
        return luaState.lock(luaState -> {
            push();
            int type = luaState.type(-1);
            luaState.pop(1);
            return type;
        });
    }

    public boolean getBoolean() {
        if (isClosed()) return false;
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.toBoolean(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public double getNumber() {
        if (isClosed()) return 0;
        return luaState.lock(luaState -> {
            push();
            double db = luaState.toNumber(-1);
            luaState.pop(1);
            return db;
        });
    }

    public String getString() {
        if (isClosed()) return null;
        return luaState.lock(luaState -> {
            push();
            String str = luaState.toString(-1);
            luaState.pop(1);
            return str;
        });
    }

    public Object getObject() throws LuaException {
        if (isClosed()) return null;
        return luaState.lockThrow(luaState -> {
            push();
            Object obj = luaState.getObjectFromUserdata(-1);
            luaState.pop(1);
            return obj;
        });
    }

    public String toString() {
        return luaState.lock(luaState -> {
            try {
                if (isNil())
                    return "nil";
                else if (isBoolean())
                    return String.valueOf(getBoolean());
                else if (isNumber())
                    return String.valueOf(getNumber());
                else if (isString())
                    return getString();
                else if (isFunction())
                    return "Lua Function";
                else if (isJavaObject())
                    return getObject().toString();
                else if (isUserdata())
                    return "Userdata";
                else if (isTable())
                    return "Lua Table";
                else if (isJavaFunction())
                    return "Java Function";
                else
                    return null;
            } catch (LuaException e) {
                return null;
            }
        });
    }

    @Override
    public boolean equals(Object object) {
        if (isClosed()) return false;
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        LuaObject luaObject = (LuaObject) object;
        return Objects.equals(luaState, luaObject.luaState) &&
                (Objects.equals(ref, luaObject.ref) || isRawEqualInLua((LuaObject) object));
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, luaState);
    }

    private boolean isRawEqualInLua(LuaObject other) {
        if (isClosed()) return false;
        return luaState.lock(l -> {
            try {
                rawPush();
                other.rawPush();
                return l.rawequal(-1, -2);
            } finally {
                l.pop(2);
            }
        });
    }

    /**
     * Function that creates a java proxy to the object represented by <code>this</code>
     *
     * @param implem Interfaces that are implemented, separated by <code>,</code>
     */
    public Result<Object, ? extends Exception> createProxy(String implem) throws LuaException {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed"));
        if (!isTable())
            throw new LuaException("Invalid Object. Must be Table.");
        return luaState.lockThrowAll(L -> {
            StringTokenizer st = new StringTokenizer(implem, ",");
            Class<?>[] interfaces = new Class[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
                interfaces[i] = Class.forName(st.nextToken());

            InvocationHandler handler = new LuaInvocationHandler(this);

            return Proxy.newProxyInstance(this.getClass().getClassLoader(), interfaces, handler);
        });
    }
}
