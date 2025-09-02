/*
 * LuaStateFacade.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava;

import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.type.IInnerLuaObject;
import org.eu.smileyik.luajava.type.ILuaCallable;
import org.eu.smileyik.luajava.util.ParamRef;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static org.eu.smileyik.luajava.LuaState.*;

public class LuaStateFacade implements AutoCloseable {
    public interface LuaStateFunction<T> {
        public T apply(LuaState luaState) throws LuaException;
    }

    public interface LuaStateConsumer {
        public void accept(LuaState luaState) throws LuaException;
    }

    public interface LuaStateDangerFunction<T> {
        public T apply(LuaState luaState) throws Exception;
    }

    public interface LuaStateDangerConsumer {
        public void accept(LuaState luaState) throws Exception;
    }

    public interface LuaStateSafeFunction<T> {
        public T apply(LuaState luaState);
    }

    public interface LuaStateSafeConsumer {
        public void accept(LuaState luaState);
    }

    private final Lock lock = new ReentrantLock();
    private final int stateId;
    private final LuaState luaState;
    private final boolean ignoreNotPublic;
    /**
     * if set true, then will use the first result when searched method list include more than one result.
     * and will not throw error.
     */
    private boolean justUseFirstMethod;
    private BiConsumer<LuaStateFacade, LuaDebug> debugHook = null;

    protected LuaStateFacade(int stateId, boolean ignoreNotPublic) {
        this.stateId = stateId;
        this.luaState = new LuaState(stateId);
        this.ignoreNotPublic = ignoreNotPublic;
    }

    protected LuaStateFacade(CPtr cPtr) {
        ParamRef<LuaStateFacade> existLuaState = ParamRef.wrapper();
        this.stateId = LuaStateFactory.insertLuaState(this, cPtr, existLuaState);
        this.luaState = existLuaState.isEmpty() ?
                new LuaState(cPtr, this.stateId) : existLuaState.getParamAndClear().luaState;
        this.ignoreNotPublic = existLuaState.isEmpty() || existLuaState.getParamAndClear().ignoreNotPublic;
    }

    public long getCPtrPeer() {
        return luaState.getCPtrPeer();
    }

    public boolean isIgnoreNotPublic() {
        return ignoreNotPublic;
    }

    public boolean isJustUseFirstMethod() {
        return justUseFirstMethod;
    }

    public void setJustUseFirstMethod(boolean justUseFirstMethod) {
        this.justUseFirstMethod = justUseFirstMethod;
    }

    public void setDebugHook(BiConsumer<LuaStateFacade, LuaDebug> debugHook) {
        lock(it -> {
            this.debugHook = debugHook;
        });
    }

    protected void debugHook(LuaDebug ar) {
        if (debugHook != null) {
            debugHook.accept(this, ar);
        }
    }

    public <T> Result<T, ? extends Exception> lockThrowAll(LuaStateDangerFunction<T> function) {
        lock.lock();
        try {
            return Result.success(function.apply(luaState));
        } catch (Exception e) {
            return Result.failure(e);
        } finally {
            lock.unlock();
        }
    }

    public <T> Result<T, ? extends LuaException> lockThrow(LuaStateFunction<T> function) {
        lock.lock();
        try {
            return Result.success(function.apply(luaState));
        } catch (LuaException e) {
            return Result.failure(e);
        } finally {
            lock.unlock();
        }
    }

    public <T> T lock(LuaStateSafeFunction<T> function) {
        lock.lock();
        try {
            return function.apply(luaState);
        } finally {
            lock.unlock();
        }
    }

    public Result<?, ? extends Exception> lockThrowAll(LuaStateDangerConsumer consumer) {
        lock.lock();
        try {
            consumer.accept(luaState);
            return Result.success();
        } catch (Exception e) {
            return Result.failure(e);
        } finally {
            lock.unlock();
        }
    }

    public Result<?, ? extends LuaException> lockThrow(LuaStateConsumer consumer) {
        lock.lock();
        try {
            consumer.accept(luaState);
            return Result.success();
        } catch (LuaException e) {
            return Result.failure(e);
        } finally {
            lock.unlock();
        }
    }

    public void lock(LuaStateSafeConsumer consumer) {
        lock.lock();
        try {
            consumer.accept(luaState);
        } finally {
            lock.unlock();
        }
    }

    public Result<Integer, LuaException> evalString(final String str) {
        return lock(l -> {
            int exp = l.LdoString(str);
            if (exp != 0) {
                String err = getErrorMessage(exp);
                return Result.failure(new LuaException(err), err);
            }
            return Result.success(exp);
        });
    }

    public Result<Integer, LuaException> evalFile(final String filename) {
        return lock(l -> {
            int exp = l.LdoFile(filename);
            if (exp != 0) {
                String err = getErrorMessage(exp);
                return Result.failure(new LuaException(err), err);
            }
            return Result.success(exp);
        });
    }

    public Result<Integer, LuaException> loadString(final String str) {
        return lock(l -> {
            int exp = l.LloadString(str);
            if (exp != 0) {
                String err = getErrorMessage(exp);
                return Result.failure(new LuaException(err), err);
            }
            return Result.success(exp);
        });
    }

    public Result<Integer, LuaException> loadFile(final String filename) {
        return lock(l -> {
            int exp = l.LloadFile(filename);
            if (exp != 0) {
                String err = getErrorMessage(exp);
                return Result.failure(new LuaException(err), err);
            }
            return Result.success(exp);
        });
    }

    public Result<Integer, LuaException> load(byte[] buffer, String name) {
        return lock(l -> {
            int exp = l.LloadBuffer(buffer, name);
            if (exp != 0) {
                String err = getErrorMessage(exp);
                return Result.failure(new LuaException(err), err);
            }
            return Result.success(exp);
        });
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public Lock getLock() {
        return lock;
    }

    public LuaState getLuaState() {
        return luaState;
    }

    public int getStateId() {
        return stateId;
    }

    @Override
    public void close() {
        lock.lock();
        try {
            LuaStateFactory.removeLuaState(stateId);
            if (luaState != null && !luaState.isClosed()) {
                luaState.clearRef();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isClosed() {
        return luaState.isClosed();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        LuaStateFacade facade = (LuaStateFacade) object;
        return stateId == facade.stateId && Objects.equals(luaState, facade.luaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateId, luaState);
    }

    protected String getErrorMessage(int err) {
        String ret = null;
        switch (err) {
            case LUA_ERRRUN:
                ret = "Runtime error. ";
                break;
            case LUA_ERRMEM:
                ret = "Memory allocation error. ";
                break;
            case LUA_ERRERR:
                ret = "Error while running the error handler function.";
                break;
            default:
                break;
        }
        if (luaState.isString(-1)) {
            ret = ret == null ? luaState.toString(-1) : (ret + luaState.toString(-1));
        }
        return ret;
    }

    // === LUA OBJECT ===

    /**
     * Creates a reference to an object inside another object
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name that index the field
     * @return LuaObject
     * @throws LuaException if parent is not a table or userdata
     */
    public Result<LuaObject, ? extends LuaException> getLuaObject(LuaObject parent, String name) {
        if (parent.luaState.getCPtrPeer() != luaState.getCPtrPeer()) {
            return Result.failure(new LuaException("Object must have the same LuaState as the parent!"));
        }
        return LuaObject.create(parent, name);
    }

    /**
     * Creates a reference to an object in the variable globalName
     *
     * @param globalName
     * @return LuaObject
     */
    public Result<LuaObject, ? extends LuaException> getLuaObject(String globalName) {
        return LuaObject.create(this, globalName);
    }

    /**
     * This constructor creates a LuaObject from a table that is indexed by a number.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (number) that index the field
     * @return LuaObject
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    public Result<LuaObject, ? extends LuaException> getLuaObject(LuaObject parent, Number name) {
        if (parent.luaState.getCPtrPeer() != luaState.getCPtrPeer()) {
            return Result.failure(new LuaException("Object must have the same LuaState as the parent!"));
        }

        return LuaObject.create(parent, name);
    }

    /**
     * This constructor creates a LuaObject from a table that is indexed by any LuaObject.
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name (LuaObject) that index the field
     * @return LuaObject
     * @throws LuaException When the parent object isn't a Table or Userdata
     */
    public Result<LuaObject, ? extends LuaException> getLuaObject(LuaObject parent, LuaObject name) {
        if (parent.getLuaState().getCPtrPeer() != luaState.getCPtrPeer() ||
                parent.getLuaState().getCPtrPeer() != name.getLuaState().getCPtrPeer()) {
            return Result.failure(new LuaException("Object must have the same LuaState as the parent!"));
        }

        return LuaObject.create(parent, name);
    }

    /**
     * Creates a reference to an object in the <code>index</code> position
     * of the stack
     *
     * @param index position on the stack
     * @return LuaObject
     */
    public Result<LuaObject, ? extends LuaException> getLuaObject(int index) {
        return LuaObject.create(this, index);
    }

    /**
     * Creates a reference to an object in the <code>index</code> position
     * of the stack
     *
     * @param index position on the stack
     * @return LuaObject
     */
    public Result<LuaObject, ? extends LuaException> rawGetLuaObject(int index) {
        return LuaObject.rawCreate(this, index);
    }

    /**
     * Function that returns a Java Object equivalent to the one in the given
     * position of the Lua Stack.
     *
     * @param idx Index in the Lua Stack
     * @return Java object equivalent to the Lua one
     */
    public Result<Object, ? extends LuaException> toJavaObject(int idx) {
        lock.lock();
        try {
            return rawToJavaObject(idx);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Function that returns a Java Object equivalent to the one in the given
     * position of the Lua Stack.
     *
     * @param idx Index in the Lua Stack
     * @return Java object equivalent to the Lua one
     */
    public Result<Object, ? extends LuaException> rawToJavaObject(int idx) {
        int type = luaState.type(idx);
        switch (type) {
            case LUA_TBOOLEAN:
                return Result.success(luaState.toBoolean(idx));
            case LUA_TSTRING:
                return Result.success(luaState.toString(idx));
            case LUA_TFUNCTION:
            case LUA_TTABLE:
                return rawGetLuaObject(idx).justCast();
            case LUA_TNUMBER:
                return Result.success(luaState.toNumber(idx));
            case LUA_TUSERDATA:
                try {
                    if (luaState.isObject(idx)) {
                        return Result.success(luaState.getObjectFromUserdata(idx));
                    } else {
                        return getLuaObject(idx).justCast();
                    }
                } catch (LuaException e) {
                    return Result.failure(e);
                }
        }
        return Result.success(null);
    }

    /**
     * Pushes into the stack any object value.<br>
     * This function checks if the object could be pushed as a lua type, if not
     * pushes the java object.
     *
     * @param obj
     */
    public Result<Void, ? extends LuaException> pushObjectValue(Object obj) {
        lock.lock();
        try {
            return rawPushObjectValue(obj);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pushes into the stack any object value.<br>
     * This function checks if the object could be pushed as a lua type, if not
     * pushes the java object.
     *
     * @param obj
     */
    public Result<Void, ? extends LuaException> rawPushObjectValue(Object obj) {
        if (obj == null) {
            luaState.pushNil();
        } else if (obj instanceof Boolean) {
            luaState.pushBoolean((Boolean) obj);
        } else if (obj instanceof Number) {
            luaState.pushNumber(((Number) obj).doubleValue());
        } else if (obj instanceof String) {
            luaState.pushString((String) obj);
        } else if (obj instanceof JavaFunction) {
            try {
                luaState.pushJavaFunction((JavaFunction) obj);
            } catch (LuaException e) {
                return Result.failure(e);
            }
        } else if (obj instanceof LuaObject) {
            ((LuaObject) obj).rawPush();
        } else if (obj instanceof byte[]) {
            luaState.pushString((byte[]) obj);
        } else if (obj.getClass().isArray()) {
            try {
                luaState.pushJavaArray(obj);
            } catch (LuaException e) {
                return Result.failure(e);
            }
        } else if (obj instanceof Class<?>) {
            luaState.pushJavaClass((Class<?>) obj);
        } else {
            luaState.pushJavaObject(obj);
        }
        return Result.success();
    }

    // STACK MANIPULATION

    public LuaStateFacade newThread() {
        lock.lock();
        try {
            return new LuaStateFacade(luaState.newThread());
        } finally {
            lock.unlock();
        }
    }

    public int getTop() {
        return lock(LuaState::getTop);
    }

    public void setTop(int idx) {
        lock(l -> {
            l.setTop(idx);
        });
    }

    public void pushValue(int idx) {
        lock(l -> {
            l.pushValue(idx);
        });
    }

    public void remove(int idx) {
        lock(l -> {
            l.remove(idx);
        });
    }

    public void insert(int idx) {
        lock(l -> {
            l.insert(idx);
        });
    }

    public void replace(int idx) {
        lock(l -> {
            l.replace(idx);
        });
    }

    public int checkStack(int sz) {
        return lock(l -> {
            return l.checkStack(sz);
        });
    }

    // ACCESS FUNCTION

    public void xmove(LuaStateFacade to, int n) {
        lock.lock();
        try {
            luaState.xmove(to.luaState, n);
        } finally {
            lock.unlock();
        }
    }

    public boolean isNumber(int idx) {
        return lock(l -> {
            return l.isNumber(idx);
        });
    }

    public boolean isString(int idx) {
        return lock(l -> {
            return l.isString(idx);
        });
    }

    public boolean isFunction(int idx) {
        return lock(l -> {
            return l.isFunction(idx);
        });
    }

    public boolean isCFunction(int idx) {
        return lock(l -> {
            return l.isCFunction(idx);
        });
    }

    public boolean isUserdata(int idx) {
        return lock(l -> {
            return l.isUserdata(idx);
        });
    }

    public boolean isTable(int idx) {
        return lock(l -> {
            return l.isTable(idx);
        });
    }

    public boolean isBoolean(int idx) {
        return lock(l -> {
            return l.isBoolean(idx);
        });
    }

    public boolean isNil(int idx) {
        return lock(l -> {
            return l.isNil(idx);
        });
    }

    public boolean isThread(int idx) {
        return lock(l -> {
            return l.isThread(idx);
        });
    }

    public boolean isNone(int idx) {
        return lock(l -> {
            return l.isNone(idx);
        });
    }

    public boolean isNoneOrNil(int idx) {
        return lock(l -> {
            return l.isNoneOrNil(idx);
        });
    }

    public int type(int idx) {
        return lock(l -> {
            return l.type(idx);
        });
    }

    public String typeName(int tp) {
        return lock(l -> {
            return l.typeName(tp);
        });
    }

    public int equal(int idx1, int idx2) {
        return lock(l -> {
            return l.equal(idx1, idx2);
        });
    }

    public boolean rawequal(int idx1, int idx2) {
        return lock(l -> {
            return l.rawequal(idx1, idx2);
        });
    }

    public int lessthan(int idx1, int idx2) {
        return lock(l -> {
            return l.lessthan(idx1, idx2);
        });
    }

    public double toNumber(int idx) {
        return lock(l -> {
            return l.toNumber(idx);
        });
    }

    public int toInteger(int idx) {
        return lock(l -> {
            return l.toInteger(idx);
        });
    }

    public boolean toBoolean(int idx) {
        return lock(l -> {
            return l.toBoolean(idx);
        });
    }

    public String toString(int idx) {
        return lock(l -> {
            return l.toString(idx);
        });
    }

    public int strLen(int idx) {
        return lock(l -> {
            return l.strLen(idx);
        });
    }

    public int objLen(int idx) {
        return lock(l -> {
            return l.objLen(idx);
        });
    }

    // PUSH FUNCTIONS

    public LuaStateFacade toThread(int idx) {
        lock.lock();
        try {
            return new LuaStateFacade(luaState.toThread(idx));
        } finally {
            lock.unlock();
        }
    }

    public void pushNil() {
        lock(LuaState::pushNil);
    }

    public void pushNumber(double db) {
        lock(l -> {
            l.pushNumber(db);
        });
    }

    public void pushInteger(int integer) {
        lock(l -> {
            l.pushInteger(integer);
        });
    }

    public void pushString(String str) {
        lock(l -> {
            l.pushString(str);
        });
    }

    public void pushString(byte[] bytes) {
        lock(l -> {
            l.pushString(bytes);
        });
    }

    // GET FUNCTIONS

    public void pushBoolean(boolean bool) {
        lock(l -> {
            l.pushBoolean(bool);
        });
    }

    public void getTable(int idx) {
        lock(l -> {
            l.getTable(idx);
        });
    }

    public void getField(int idx, String k) {
        lock(l -> {
            l.getField(idx, k);
        });
    }

    public void rawGet(int idx) {
        lock(l -> {
            l.rawGet(idx);
        });
    }

    public void rawGetI(int idx, int n) {
        lock(l -> {
            l.rawGetI(idx, n);
        });
    }

    public void createTable(int narr, int nrec) {
        lock(l -> {
            l.createTable(narr, nrec);
        });
    }

    public void newTable() {
        lock(LuaState::newTable);
    }

    /**
     *
     * @param idx
     * @return if returns false, there is no metatable
     */
    public boolean getMetaTable(int idx) {
        return lock(l -> {
            return l.getMetaTable(idx);
        });
    }

    // SET FUNCTIONS

    public void getFEnv(int idx) {
        lock(l -> {
            l.getFEnv(idx);
        });
    }

    public void setTable(int idx) {
        lock(l -> {
            l.setTable(idx);
        });
    }

    public void setField(int idx, String k) {
        lock(l -> {
            l.setField(idx, k);
        });
    }

    public void rawSet(int idx) {
        lock(l -> {
            l.rawSet(idx);
        });
    }

    public void rawSetI(int idx, int n) {
        lock(l -> {
            l.rawSetI(idx, n);
        });
    }

    // if returns 0, cannot set the metatable to the given object
    public int setMetaTable(int idx) {
        return lock(l -> {
            return l.setMetaTable(idx);
        });
    }

    // if object is not a function returns 0
    public int setFEnv(int idx) {
        return lock(l -> {
            return l.setFEnv(idx);
        });
    }

    public void call(int nArgs, int nResults) {
        lock(l -> {
            l.call(nArgs, nResults);
        });
    }

    public Result<Void, LuaException> pcall(int nArgs, int nResults, int errFunc) {
        return lock(l -> {
            return doPcall(nArgs, nResults, errFunc);
        });
    }

    /**
     * unlocked version pcall.
     * <strong>This method is not safe!</strong>
     * 
     * @see LuaStateFacade#pcall(int, int, int)
     */
    public Result<Void, LuaException> doPcall(int nArgs, int nResults, int errFunc) {
        int exp = luaState.pcall(nArgs, nResults, errFunc);
        if (exp != 0) {
            String err = getErrorMessage(exp);
            return Result.failure(new LuaException(err), err);
        }
        return Result.success();
    }

    /**
     * Calls the object represented by <code>callable</code> using Lua function pcall. Returns 1 object
     *
     * @param args -
     *             Call arguments
     * @return Object - Returned Object
     * @throws LuaException
     */
    public Result<Object, ? extends LuaException> pcall(ILuaCallable callable, Object[] args) {
        return pcall(callable, args, 1).mapValue(it -> it[0]);
    }

    /**
     * Calls the object represented by <code>callable</code> using Lua function pcall.
     *
     * @param args -
     *             Call arguments
     * @param nres -
     *             Number of objects returned
     * @return Object[] - Returned Objects
     */
    public Result<Object[], ? extends LuaException> pcall(ILuaCallable callable, Object[] args, int nres) {
        lock.lock();
        try {
            return doPcall(callable, args, nres);
        } finally {
            lock.unlock();
        }
    }


    /**
     * Calls the object represented by <code>callable</code> using Lua function pcall.
     *
     * @param args  -
     *              Call arguments
     * @param _nres -
     *              Number of objects returned
     * @return Object[] - Returned Objects
     */
    public Result<Object[], ? extends LuaException> doPcall(ILuaCallable callable, Object[] args, int _nres) {
        IInnerLuaObject innerObject = (IInnerLuaObject) callable;
        final int top = luaState.getTop();
        int nargs = 0;

        try {
            // push function and push params.
            innerObject.rawPush();
            if (args != null) {
                nargs = args.length;
                for (Object obj : args) {
                    Result<Void, ? extends LuaException> pushResult = rawPushObjectValue(obj);
                    if (pushResult.isError()) {
                        // return Result.failure(pushResult.getError(),
                        // "Convert Java object to lua function params failed");
                        return pushResult.justCast();
                    }
                }
            }

            return doPcall(nargs, _nres, 0).mapResultValue((v) -> {
                int nres = _nres;
                int currentTop = luaState.getTop();
                if (nres == LuaState.LUA_MULTRET) {
                    nres = currentTop - top;
                }
                if (currentTop - top < nres) {
                    return Result.failure(new LuaException("Invalid Number of Results .")).justCast();
                }

                Object[] res = new Object[nres];
                for (int i = nres - 1; i >= 0; i--) {
                    Result<Object, ? extends LuaException> ret = rawToJavaObject(-1);
                    if (ret.isError()) return ret.justCast();
                    res[i] = ret.getValue();
                    luaState.pop(1);
                }
                return Result.success(res);
            });
        } finally {
            luaState.setTop(top);
        }
    }

    public int yield(int nResults) {
        return lock(l -> {
            return l.yield(nResults);
        });
    }

    public int resume(int nArgs) {
        return lock(l -> {
            return l.resume(nArgs);
        });
    }

    public int status() {
        return lock(LuaState::status);
    }

    public int gc(int what, int data) {
        return lock(l -> {
            return l.gc(what, data);
        });
    }

    public int getGcCount() {
        return lock(LuaState::getGcCount);
    }

    public int next(int idx) {
        return lock(l -> {
            return l.next(idx);
        });
    }

    public int error() {
        return lock(LuaState::error);
    }

    public void concat(int n) {
        lock(l -> {
            l.concat(n);
        });
    }

    // FUNCTION FROM lauxlib

    public int LgetMetaField(int obj, String e) {
        return lock(l -> {
            return l.LgetMetaField(obj, e);
        });
    }

    public int LcallMeta(int obj, String e) {
        return lock(l -> {
            return l.LcallMeta(obj, e);
        });
    }

    public int Ltyperror(int nArg, String tName) {
        return lock(l -> {
            return l.Ltyperror(nArg, tName);
        });
    }

    public int LargError(int numArg, String extraMsg) {
        return lock(l -> {
            return l.LargError(numArg, extraMsg);
        });
    }

    public String LcheckString(int numArg) {
        return lock(l -> {
            return l.LcheckString(numArg);
        });
    }

    public String LoptString(int numArg, String def) {
        return lock(l -> {
            return l.LoptString(numArg, def);
        });
    }

    public double LcheckNumber(int numArg) {
        return lock(l -> {
            return l.LcheckNumber(numArg);
        });
    }

    public double LoptNumber(int numArg, double def) {
        return lock(l -> {
            return l.LoptNumber(numArg, def);
        });
    }

    public int LcheckInteger(int numArg) {
        return lock(l -> {
            return l.LcheckInteger(numArg);
        });
    }

    public int LoptInteger(int numArg, int def) {
        return lock(l -> {
            return l.LoptInteger(numArg, def);
        });
    }

    public void LcheckStack(int sz, String msg) {
        lock(l -> {
            l.LcheckStack(sz, msg);
        });
    }

    public void LcheckType(int nArg, int t) {
        lock(l -> {
            l.LcheckType(nArg, t);
        });
    }

    public void LcheckAny(int nArg) {
        lock(l -> {
            l.LcheckAny(nArg);
        });
    }

    public int LnewMetatable(String tName) {
        return lock(l -> {
            return l.LnewMetatable(tName);
        });
    }

    public void LgetMetatable(String tName) {
        lock(l -> {
            l.LgetMetatable(tName);
        });
    }

    public void Lwhere(int lvl) {
        lock(l -> {
            l.Lwhere(lvl);
        });
    }

    public int Lref(int t) {
        return lock(l -> {
            return l.Lref(t);
        });
    }

    public void LunRef(int t, int ref) {
        lock(l -> {
            l.LunRef(t, ref);
        });
    }

    public String Lgsub(String s, String p, String r) {
        return lock(l -> {
            return l.Lgsub(s, p, r);
        });
    }

    //IMPLEMENTED C MACROS

    public String LfindTable(int idx, String fname, int szhint) {
        return lock(l -> {
            return l.LfindTable(idx, fname, szhint);
        });
    }

    public void pop(int n) {
        lock(l -> {
            l.pop(n);
        });
    }

    /**
     * get lua global variable
     * @param globalName variable name
     * @return target or error
     */
    public Result<Object, ? extends LuaException> getGlobal(String globalName) {
        return getGlobal(globalName, Object.class);
    }

    /**
     * get lua global variable and cast to
     * @param globalName variable name
     * @param tClass expect type
     * @return target or error (lua error or class cast error)
     * @param <T> expect type
     */
    public <T> Result<T, ? extends LuaException> getGlobal(String globalName, Class<T> tClass) {
        lock.lock();
        try {
            luaState.getGlobal(globalName);
            try {
                return this.rawToJavaObject(-1)
                        .mapResultValue(it -> tClass.isInstance(it) ?
                                Result.success(tClass.cast(it)) :
                                Result.failure(new LuaException("failed to convert " + it + " to " + tClass)));
            } finally {
                luaState.pop(1);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * set lua object to lua global variable.
     * @param name      variable name
     * @param luaObject obj
     * @return result
     */
    public Result<Void, ? extends LuaException> setGlobal(String name, LuaObject luaObject) {
        return lockThrowAll(l -> {
            luaObject.rawPush();
            l.setGlobal(name);
        }).justCast();
    }

    /**
     * set java object(include lua object) to lua global variable.
     * @param name variable name
     * @param obj  object
     * @return result.
     */
    public Result<Void, ? extends LuaException> setGlobal(String name, Object obj) {
        if (obj instanceof LuaObject) {
            return setGlobal(name, (LuaObject) obj);
        }
        return lockThrowAll(l -> {
            rawPushObjectValue(obj).justThrow();
            l.setGlobal(name);
        }).justCast();
    }

    // Functions to open lua libraries
    public void openBase() {
        lock(LuaState::openBase);
    }

    public void openTable() {
        lock(LuaState::openTable);
    }

    public void openIo() {
        lock(LuaState::openIo);
    }

    public void openOs() {
        lock(LuaState::openOs);
    }

    public void openString() {
        lock(LuaState::openString);
    }

    public void openMath() {
        lock(LuaState::openMath);
    }

    public void openDebug() {
        lock(LuaState::openDebug);
    }

    public void openPackage() {
        lock(LuaState::openPackage);
    }

    // ******************** addition since lua 5.2 start ***********************
    /**
     * added since lua 5.2
     * @param idx
     * @return
     */
    public int rawLen(int idx) {
        return lock(it -> {
            return it.rawLen(idx);
        });
    }

    /**
     * added since lua 5.2
     * @param idx1
     * @param idx2
     * @param op
     * @return
     */
    public boolean compare(int idx1, int idx2, int op) {
        return lock(l -> {
            return l.compare(idx1, idx2, op);
        });
    }

    public void arith(int op) {
        lock(l -> {
            l.arith(op);
        });
    }

    public void len(int idx) {
        lock(l -> {
            l.len(idx);
        });
    }

    /**
     * added since lua 5.2
     * @param thread
     * @param nargs
     * @return
     */
    public int resume(LuaState thread, int nargs) {
        return lock(l -> {
            return l.resume(thread, nargs);
        });
    }

    public int pushThread(LuaState thread) {
        return lock(l -> {
            return l.pushThread(thread);
        });
    }

    public void setUserValue(int idx) {
        lock(l -> {
            l.setUserValue(idx);
        });
    }

    public void getUserValue(int idx) {
        lock(l -> {
            l.getUserValue(idx);
        });
    }

    public int absIndex(int idx) {
        return lock(it -> {
            return it.absIndex(idx);
        });
    }

    // ******************** addition since lua 5.2 stop ***********************

    /********************** Luajava API Library **********************/

    public void openLibs() {
        lock(LuaState::openLibs);
    }

    /**
     * Gets a Object from Lua
     *
     * @param idx index of the lua stack
     * @return Object
     * @throws LuaException if the lua object does not represent a java object.
     */
    public Object getObjectFromUserdata(int idx) throws LuaException {
        return lockThrow(l -> {
           return l.getObjectFromUserdata(idx);
        });
    }

    /**
     * Tells whether a lua index contains a java Object
     *
     * @param idx index of the lua stack
     * @return boolean
     */
    public boolean isObject(int idx) {
        return lock(l -> {
            return l.isObject(idx);
        });
    }

    /**
     * Pushes a Java Object into the lua stack.<br>
     * This function does not check if the object is from a class that could
     * be represented by a lua type. Eg: java.lang.String could be a lua string.
     *
     * @param obj Object to be pushed into lua
     */
    public void pushJavaObject(Object obj) {
        lock(l -> {
            l.pushJavaObject(obj);
        });
    }

    public void pushJavaClass(Class<?> clazz) {
        lock(l -> {
            l.pushJavaClass(clazz);
        });
    }


    public void pushJavaArray(Object obj) throws LuaException {
        lockThrow(l -> {
            l.pushJavaArray(obj);
        });
    }

    /**
     * Pushes a JavaFunction into the state stack
     *
     * @param func
     */
    public void pushJavaFunction(JavaFunction func) throws LuaException {
        lockThrow(l -> {
            l.pushJavaFunction(func);
        });
    }

    /**
     * Returns whether a userdata contains a Java Function
     *
     * @param idx index of the lua stack
     * @return boolean
     */
    public boolean isJavaFunction(int idx) {
        return lock(l -> {
            return l.isJavaFunction(idx);
        });
    }

}
