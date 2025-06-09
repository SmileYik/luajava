package org.keplerproject.luajava;

import org.eu.smileyik.luajava.util.BoxedTypeHelper;

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
        this.stateId = LuaStateFactory.insertLuaState(this, cPtr);
        this.luaState = new LuaState(cPtr, this.stateId);
    }

    public long getCPtrPeer() {
        return luaState.getCPtrPeer();
    }

    public <T> T lockThrow(LuaStateFunction<T> function) throws LuaException {
        lock.lock();
        try {
            T apply = function.apply(luaState);
            if (apply instanceof LuaState) {
                throw new RuntimeException("Cannot return LuaState instance");
            }
            return apply;
        } finally {
            lock.unlock();
        }
    }

    public <T> T lock(LuaStateSafeFunction<T> function) {
        lock.lock();
        try {
            T apply = function.apply(luaState);
            if (apply instanceof LuaState) {
                throw new RuntimeException("Cannot return LuaState instance");
            }
            return apply;
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
    public synchronized Object toJavaObject(int idx) throws LuaException {
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

    /**
     * When you call a function in lua, it may return a number, and the
     * number will be interpreted as a <code>Double</code>.<br>
     * This function converts the number into a type specified by
     * <code>retType</code>
     *
     * @param db      lua number to be converted
     * @param retType type to convert to
     * @return The converted number
     */
    public static Number convertLuaNumber(Double db, Class<?> retType) {
        return BoxedTypeHelper.covertNumberTo(db, retType);
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

    @Override
    public void close() throws Exception {
        LuaStateFactory.removeLuaState(stateId);
        luaState.close();
    }
}
