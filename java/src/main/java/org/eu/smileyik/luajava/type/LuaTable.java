package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFacade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LuaTable extends LuaObject implements ILuaCallable, ILuaFieldGettable {

    /**
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY, EXPECT YOU KNOW WHAT YOU ARE DOING</strong>
     *
     * @param L     lua state
     * @param index index
     * @see LuaObject#create(LuaStateFacade, int)
     */
    protected LuaTable(LuaStateFacade L, int index) {
        super(L, index);
    }

    protected static LuaTable createTable(LuaStateFacade luaStateFacade, int index) {
        return luaStateFacade.lock(l -> {
            Optional<Integer> result = isArrayTable(l, index);
            if (result.isPresent()) {
                return new LuaArray(luaStateFacade, index, result.get());
            } else {
                return new LuaTable(luaStateFacade, index);
            }
        });
    }

    @Override
    public String toString() {
        return asDeepMap().replaceErrorString(it -> "[Cannot Transform this LuaTable to Map]").toString();
    }

    @Override
    public boolean isTable() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.TABLE;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isUserdata() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    /**
     * force cast to LuaTable.
     * @return if this instance is not LuaArray then will return itself.
     *         otherwise return a new instance of LuaTable and close the
     *         origin LuaArray instance.
     */
    public LuaTable asTable() {
        if (this instanceof LuaArray) {
            try {
                return luaState.lock(it -> {
                    try {
                        rawPush();
                        return new LuaTable(luaState, -1);
                    } finally {
                        it.pop(1);
                    }
                });
            } finally {
                close();
            }
        } else {
            return this;
        }
    }

    public Result<Map<String, Object>, ? extends Exception> asDeepStringMap() {
        return asDeepMap(String.class, Object.class);
    }

    public <V> Result<Map<String, V>, ? extends Exception> asDeepStringMap(Class<V> vClass) {
        return asDeepMap(String.class, vClass);
    }

    public Result<Map<Object, Object>, ? extends Exception> asDeepMap() {
        return asDeepMap(Object.class, Object.class);
    }

    /**
     * Convert table to map, also try to convert key or value to map. it will stop if.
     * @param kClass map key type
     * @param vClass map value type
     * @return A map
     * @param <K> key type, Cannot be primitive type
     * @param <V> value type, Cannot be primitive type
     * @throws Exception throw any exception
     */
    public <K, V> Result<Map<K, V>, ? extends Exception> asDeepMap(Class<K> kClass, Class<V> vClass) {
        Map<K, V> map = new HashMap<>();
        return forEach(kClass, vClass, (k, v) -> {
            Object realK = k, realV = v;
            if (k instanceof LuaTable) {
                if (equals(k)) {
                    throw new RuntimeException("Cannot call asDeepMap because this map also as key element in this map");
                }
                realK = ((LuaTable) k).asDeepMap(kClass, vClass).getOrSneakyThrow();
            }
            if (v instanceof LuaTable) {
                if (equals(v)) {
                    throw new RuntimeException("Cannot call asDeepMap because this map also as value element in this map");
                }
                realV = ((LuaTable) v).asDeepMap(kClass, vClass).getOrSneakyThrow();
            }
            map.put((K) realK, (V) realV);
        }).replaceValue(map);
    }

    public Result<Map<Object, Object>, ? extends Exception> asMap() {
        return asMap(Object.class, Object.class);
    }

    /**
     * Convert table to map. it will stop if.
     * @param kClass map key type
     * @param vClass map value type
     * @return A map
     * @param <K> key type, Cannot be primitive type
     * @param <V> value type, Cannot be primitive type
     * @throws Exception throw any exception
     */
    public <K, V> Result<Map<K, V>, ? extends Exception> asMap(Class<K> kClass, Class<V> vClass) {
        Map<K, V> map = new HashMap<>();
        return forEach(kClass, vClass, map::put).replaceValue(map);
    }

    public Result<Map<String, Object>, ? extends Exception> asStringMap() {
        return asStringMap(Object.class);
    }

    public <V> Result<Map<String, V>, ? extends Exception> asStringMap(Class<V> vClass) {
        return asMap(String.class, vClass);
    }

    /**
     * foreach table entry. it will stop if.
     * @param kClass   Key type
     * @param vClass   Value type
     * @param consumer consumer
     * @param <K> Key Type, Cannot be primitive type
     * @param <V> Value Type, Cannot be primitive type
     * @throws Exception any exception
     */
    public <K, V> Result<Void, ? extends Exception> forEach(Class<K> kClass,
                                                            Class<V> vClass, BiConsumer<K, V> consumer) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        return luaState.lockThrowAll(l ->  {
            int top = l.getTop();
            try {
                push();
                l.pushNil();
                while (l.next(-2) != 0) {
                    // i want out of loop if happened exception.
                    Object key = luaState.toJavaObject(-2).getOrThrow();
                    Object value = luaState.toJavaObject(-1).getOrThrow();
                    consumer.accept(kClass.cast(key), vClass.cast(value));
                    l.pop(1);
                }
                l.pop(1);
            } finally {
                l.setTop(top);
            }
            return null;
        });
    }

    /**
     * foreach table entry. it will stop if.
     *
     * @param consumer consumer
     * @return
     * @throws Exception any exception
     */
    public Result<Void, ? extends Exception> forEach(BiConsumer<Object, Object> consumer) {
        return forEach(Object.class, Object.class, consumer);
    }

    /**
     * get field by object key.
     * @param key key
     * @return result.
     */
    public Result<Object, ? extends LuaException> get(Object key) {
        return luaState.lock(l -> {
            return doGet(key);
        });
    }

    /**
     * get filed by object key without lock lua state.
     * @param key key
     * @return result.
     */
    public Result<Object, ? extends LuaException> doGet(Object key) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        LuaState inner = luaState.getLuaState();
        int top = inner.getTop();
        try {
            rawPush();
            if (key instanceof LuaObject) {
                ((LuaObject) key).rawPush();
            } else {
                Result<Void, ? extends LuaException> result = luaState.pushObjectValue(key);
                if (result.isError()) return result.justCast();
            }
            inner.getTable(-2);
            return luaState.toJavaObject(-1);
        } finally {
            inner.setTop(top);
        }
    }

    /**
     * put key-value entry to this table with lock.
     * @param key   key
     * @param value value.
     * @return the result.
     */
    public Result<Void, ? extends LuaException> put(Object key, Object value) {
        luaState.lock();
        try {
            return rawPut(key, value);
        } finally {
            luaState.unlock();
        }
    }

    /**
     * put key-value entry to this table without lock.
     * @param key   key
     * @param value value.
     * @return the result.
     */
    public Result<Void, ? extends LuaException> rawPut(Object key, Object value) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        LuaState inner = luaState.getLuaState();
        int top = inner.getTop();
        try {
            rawPush();
            return luaState.rawPushObjectValue(key)
                    .mapResultValue((it) -> luaState.rawPushObjectValue(value))
                    .ifSuccessThen(it -> inner.setTable(-3));
        } finally {
            inner.setTop(top);
        }
    }

    /**
     * remove the key without lock
     * @param key key
     * @return result
     */
    public Result<Void, ? extends LuaException> rawRemove(Object key) {
        return rawPut(key, null);
    }

    /**
     * remove the key with lock
     * @param key key
     * @return result
     */
    public Result<Void, ? extends LuaException> remove(Object key) {
        return put(key, null);
    }

    /**
     * Check the table is Array or not. Will traverse the table.
     * <strong>SHOULD CALL IN LuaStateFacade.lock()</strong>
     * @param L     lua state
     * @param index index
     * @return the array length, if the table is empty ({})
     *         or the table only a series of consecutive numbers starting with 1 as key.
     */
    private static Optional<Integer> isArrayTable(LuaState L, int index) {
        if (index < 0) index -= 1;
        int i = 0;
        L.pushNil();
        while (L.next(index) != 0) {
            if (L.type(-2) != LuaType.NUMBER) {
                L.pop(2);
                return Optional.empty();
            }
            double number = L.toNumber(-2);
            int idx = (int) number;
            if (number != idx || idx <= i) {
                L.pop(2);
                return Optional.empty();
            }
            i = idx;
            L.pop(1);
        }
        return Optional.of(i);
    }
}
