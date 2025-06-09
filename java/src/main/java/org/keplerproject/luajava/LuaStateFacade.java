package org.keplerproject.luajava;

import org.eu.smileyik.luajava.util.ParamRef;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.keplerproject.luajava.LuaState.*;

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


    protected LuaStateFacade(int stateId) {
        this.stateId = stateId;
        this.luaState = new LuaState(stateId);
    }

    protected LuaStateFacade(CPtr cPtr) {
        ParamRef<LuaStateFacade> existLuaState = ParamRef.wrapper();
        this.stateId = LuaStateFactory.insertLuaState(this, cPtr, existLuaState);
        this.luaState = existLuaState.isEmpty() ?
                new LuaState(cPtr, this.stateId) : existLuaState.getParamAndClear().luaState;
    }

    public long getCPtrPeer() {
        return luaState.getCPtrPeer();
    }

    public <T> T lockThrowAll(LuaStateDangerFunction<T> function) throws Exception {
        lock.lock();
        try {
            return function.apply(luaState);
        } finally {
            lock.unlock();
        }
    }

    public <T> T lockThrow(LuaStateFunction<T> function) throws LuaException {
        lock.lock();
        try {
            return function.apply(luaState);
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

    public void lockThrowAll(LuaStateDangerConsumer consumer) throws Exception {
        lock.lock();
        try {
            consumer.accept(luaState);
        } finally {
            lock.unlock();
        }
    }

    public void lockThrow(LuaStateConsumer consumer) throws LuaException {
        lock.lock();
        try {
            consumer.accept(luaState);
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

    // === LUA OBJECT ===

    /**
     * Creates a reference to an object inside another object
     *
     * @param parent The Lua Table or Userdata that contains the Field.
     * @param name   The name that index the field
     * @return LuaObject
     * @throws LuaException if parent is not a table or userdata
     */
    public LuaObject getLuaObject(LuaObject parent, String name) throws LuaException {
        if (parent.luaState.getCPtrPeer() != luaState.getCPtrPeer())
            throw new LuaException("Object must have the same LuaState as the parent!");
        return LuaObject.create(parent, name);
    }

    /**
     * Creates a reference to an object in the variable globalName
     *
     * @param globalName
     * @return LuaObject
     */
    public LuaObject getLuaObject(String globalName) {
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
    public LuaObject getLuaObject(LuaObject parent, Number name)
            throws LuaException {
        if (parent.luaState.getCPtrPeer() != luaState.getCPtrPeer())
            throw new LuaException("Object must have the same LuaState as the parent!");

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
    public LuaObject getLuaObject(LuaObject parent, LuaObject name)
            throws LuaException {
        if (parent.getLuaState().getCPtrPeer() != luaState.getCPtrPeer() ||
                parent.getLuaState().getCPtrPeer() != name.getLuaState().getCPtrPeer())
            throw new LuaException("Object must have the same LuaState as the parent!");

        return LuaObject.create(parent, name);
    }

    /**
     * Creates a reference to an object in the <code>index</code> position
     * of the stack
     *
     * @param index position on the stack
     * @return LuaObject
     */
    public LuaObject getLuaObject(int index) {
        return LuaObject.create(this, index);
    }

    /**
     * Function that returns a Java Object equivalent to the one in the given
     * position of the Lua Stack.
     *
     * @param idx Index in the Lua Stack
     * @return Java object equivalent to the Lua one
     */
    public Object toJavaObject(int idx) throws LuaException {
        lock.lock();
        try {
            Object obj = null;
            int type = luaState.type(idx);
            switch (type) {
                case LUA_TBOOLEAN:
                    obj = luaState.toBoolean(idx);
                    break;
                case LUA_TSTRING:
                    obj = luaState.toString(idx);
                    break;
                case LUA_TFUNCTION:
                case LUA_TTABLE:
                    obj = getLuaObject(idx);
                    break;
                case LUA_TNUMBER:
                    obj = luaState.toNumber(idx);
                    break;
                case LUA_TUSERDATA:
                    obj = luaState.isObject(idx) ? luaState.getObjectFromUserdata(idx) : getLuaObject(idx);
                    break;
            }
            return obj;
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
    public void pushObjectValue(Object obj) throws LuaException {
        lock.lock();
        try {
            if (obj == null) {
                luaState.pushNil();
            } else if (obj instanceof Boolean) {
                luaState.pushBoolean((Boolean) obj);
            } else if (obj instanceof Number) {
                luaState.pushNumber(((Number) obj).doubleValue());
            } else if (obj instanceof String) {
                luaState.pushString((String) obj);
            } else if (obj instanceof JavaFunction) {
                luaState.pushJavaFunction((JavaFunction) obj);
            } else if (obj instanceof LuaObject) {
                ((LuaObject) obj).push();
            } else if (obj instanceof byte[]) {
                luaState.pushString((byte[]) obj);
            } else if (obj.getClass().isArray()) {
                luaState.pushJavaArray(obj);
            } else {
                luaState.pushJavaObject(obj);
            }
        } finally {
            lock.unlock();
        }
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

    // PUSH FUNCTIONS

    public LuaStateFacade toThread(int idx) {
        lock.lock();
        try {
            return new LuaStateFacade(luaState.toThread(idx));
        } finally {
            lock.unlock();
        }
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
}
