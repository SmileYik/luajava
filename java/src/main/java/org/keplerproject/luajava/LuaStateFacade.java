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

    public int rawequal(int idx1, int idx2) {
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
     * @return if returns 0, there is no metatable
     */
    public int getMetaTable(int idx) {
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

    // returns 0 if ok of one of the error codes defined
    public int pcall(int nArgs, int nResults, int errFunc) {
        return lock(l -> {
            return l.pcall(nArgs, nResults, errFunc);
        });
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

    // returns 0 if ok
    public int LdoFile(String fileName) {
        return lock(l -> {
            return l.LdoFile(fileName);
        });
    }

    // FUNCTION FROM lauxlib
    // returns 0 if ok
    public int LdoString(String str) {
        return lock(l -> {
            return l.LdoString(str);
        });
    }

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

    public int LloadFile(String fileName) {
        return lock(l -> {
            return l.LloadFile(fileName);
        });
    }

    public int LloadString(String s) {
        return lock(l -> {
            return l.LloadString(s);
        });
    }

    public int LloadBuffer(byte[] buff, String name) {
        return lock(l -> {
            // Special case: Assuming LloadBuffer on 'l' still needs 'buff.length'
            return l.LloadBuffer(buff, name);
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

    public void getGlobal(String global) {
        lock(l -> {
            l.getGlobal(global);
        });
    }

    public void setGlobal(String name) {
        lock(l -> {
            l.setGlobal(name);
        });
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
