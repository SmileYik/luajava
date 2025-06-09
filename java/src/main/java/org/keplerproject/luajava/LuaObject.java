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

import org.eu.smileyik.luajava.type.InnerTypeHelper;
import org.eu.smileyik.luajava.util.ResourceCleaner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
public class LuaObject implements AutoCloseable {
    protected static final ResourceCleaner CLEANER = new ResourceCleaner();
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
            CLEANER.register(this, cleanTask);
            return cleanTask;
        });
    }

    /**
     * Creates a reference to an object in the given index of the stack
     *
     * @param luaState
     * @param index of the object on the lua stack
     */
    protected static LuaObject create(LuaStateFacade luaState, int index) {
        return luaState.lock((L) -> {
            return InnerTypeHelper.createLuaObject(L, index)
                    .orElseGet(() -> new LuaObject(luaState, index));
        });
    }

    /**
     * Creates a reference to an object in the variable globalName
     *
     * @param luaState
     * @param globalName
     */
    protected static LuaObject create(LuaStateFacade luaState, String globalName) {
        return luaState.lock((L) -> {
            L.getGlobal(globalName);
            LuaObject luaObject = create(luaState, -1);
            L.pop(1);
            return luaObject;
        });
    }

    /**
     * Creates a reference to an object inside another object
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name that index the field
     */
    protected static LuaObject create(LuaObject parent, String name) throws LuaException {
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata()) {
                throw new LuaException("Object parent should be a table or userdata .");
            }

            parent.push();
            L.pushString(name);
            L.getTable(-2);
            L.remove(-2);
            LuaObject luaObject = create(luaState, -1);
            L.pop(1);
            return luaObject;
        });
    }

    /**
     * This static method creates a LuaObject from a table that is indexed by a number.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (number) that index the field
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    protected static LuaObject create(LuaObject parent, Number name) throws LuaException {
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata())
                throw new LuaException("Object parent should be a table or userdata .");

            parent.push();
            L.pushNumber(name.doubleValue());
            L.getTable(-2);
            L.remove(-2);
            LuaObject luaObject = create(luaState, -1);
            L.pop(1);
            return luaObject;
        });
    }

    /**
     * This static method creates a LuaObject from a table that is indexed by a LuaObject.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (LuaObject) that index the field
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    protected static LuaObject create(LuaObject parent, LuaObject name) throws LuaException {
        if (parent.getLuaState() != name.getLuaState())
            throw new LuaException("LuaStates must be the same!");
        LuaStateFacade luaState = parent.getLuaState();
        return luaState.lockThrow(L -> {
            if (!parent.isTable() && !parent.isUserdata())
                throw new LuaException("Object parent should be a table or userdata .");

            LuaState L = parent.getLuaState();

            parent.push();
            name.push();
            L.getTable(-2);
            L.remove(-2);



            LuaObject luaObject = create(luaState, -1);
            L.pop(1);
            return luaObject;
        });
    }

    /**
     * Gets the Object's State
     */
    public LuaStateFacade getLuaState() {
        return luaState;
    }

    @Override
    public void close() {
        cleanTask.run();
    }

    /**
     * Pushes the object represented by <code>this</code> into L's stack
     */
    public void push() {
        luaState.lock(l -> {
            l.rawGetI(LuaState.LUA_REGISTRYINDEX, ref);
        });
    }

    public boolean isNil() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isNil(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isBoolean() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isBoolean(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isNumber() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isNumber(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isString() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isString(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isFunction() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isFunction(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isJavaObject() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isObject(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isJavaFunction() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isJavaFunction(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isTable() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isTable(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public boolean isUserdata() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.isUserdata(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public int type() {
        return luaState.lock(luaState -> {
            push();
            int type = luaState.type(-1);
            luaState.pop(1);
            return type;
        });
    }

    public boolean getBoolean() {
        return luaState.lock(luaState -> {
            push();
            boolean bool = luaState.toBoolean(-1);
            luaState.pop(1);
            return bool;
        });
    }

    public double getNumber() {
        return luaState.lock(luaState -> {
            push();
            double db = luaState.toNumber(-1);
            luaState.pop(1);
            return db;
        });
    }

    public String getString() {
        return luaState.lock(luaState -> {
            push();
            String str = luaState.toString(-1);
            luaState.pop(1);
            return str;
        });
    }

    public Object getObject() throws LuaException {
        return luaState.lockThrow(luaState -> {
            push();
            Object obj = luaState.getObjectFromUserdata(-1);
            luaState.pop(1);
            return obj;
        });
    }

    /**
     * If <code>this</code> is a table or userdata tries to get
     * a field value.
     */
    public LuaObject getField(String field) throws LuaException {
        return luaState.getLuaObject(this, field);
    }

    /**
     * Calls the object represented by <code>this</code> using Lua function pcall.
     *
     * @param args  -
     *              Call arguments
     * @param _nres -
     *              Number of objects returned
     * @return Object[] - Returned Objects
     * @throws LuaException
     */
    public Object[] call(Object[] args, int _nres) throws LuaException {
        return luaState.lockThrow(innerL -> {
            if (!isFunction() && !isTable() && !isUserdata())
                throw new LuaException("Invalid object. Not a function, table or userdata .");
            int nres = _nres;
            int top = innerL.getTop();
            push();
            int nargs;
            if (args != null) {
                nargs = args.length;
                for (int i = 0; i < nargs; i++) {
                    Object obj = args[i];
                    luaState.pushObjectValue(obj);
                }
            } else
                nargs = 0;

            int err = innerL.pcall(nargs, nres, 0);

            if (err != 0) {
                String str;
                if (innerL.isString(-1)) {
                    str = innerL.toString(-1);
                    innerL.pop(1);
                } else
                    str = "";

                if (err == LuaState.LUA_ERRRUN) {
                    str = "Runtime error. " + str;
                } else if (err == LuaState.LUA_ERRMEM) {
                    str = "Memory allocation error. " + str;
                } else if (err == LuaState.LUA_ERRERR) {
                    str = "Error while running the error handler function. " + str;
                } else {
                    str = "Lua Error code " + err + ". " + str;
                }

                throw new LuaException(str);
            }

            if (nres == LuaState.LUA_MULTRET)
                nres = innerL.getTop() - top;
            if (innerL.getTop() - top < nres) {
                throw new LuaException("Invalid Number of Results .");
            }

            Object[] res = new Object[nres];

            for (int i = nres; i > 0; i--) {
                res[i - 1] = luaState.toJavaObject(-1);
                innerL.pop(1);
            }
            return res;
        });
    }

    /**
     * Calls the object represented by <code>this</code> using Lua function pcall. Returns 1 object
     *
     * @param args -
     *             Call arguments
     * @return Object - Returned Object
     * @throws LuaException
     */
    public Object call(Object[] args) throws LuaException {
        return call(args, 1)[0];
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

    /**
     * Function that creates a java proxy to the object represented by <code>this</code>
     *
     * @param implem Interfaces that are implemented, separated by <code>,</code>
     */
    public Object createProxy(String implem) throws ClassNotFoundException, LuaException {
        synchronized (luaState) {
            if (!isTable())
                throw new LuaException("Invalid Object. Must be Table.");

            StringTokenizer st = new StringTokenizer(implem, ",");
            Class<?>[] interfaces = new Class[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
                interfaces[i] = Class.forName(st.nextToken());

            InvocationHandler handler = new LuaInvocationHandler(this);

            return Proxy.newProxyInstance(this.getClass().getClassLoader(), interfaces, handler);
        }
    }
}
