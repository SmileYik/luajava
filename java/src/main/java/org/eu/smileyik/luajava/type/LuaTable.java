package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class LuaTable extends LuaObject {
    public static final String TYPE_NAME = LuaType.typeName(LuaType.TABLE);

    public LuaTable(LuaState L, int index) {
        super(L, index);
    }

    protected static LuaTable create(LuaState L, int index) {
        return isArrayTable(L, index) ? new LuaArray(L, index) : new LuaTable(L, index);
    }

    @Override
    public String toString() {
        return TYPE_NAME;
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

    public Map<String, Object> asDeepStringMap() throws Throwable {
        return asDeepMap(String.class, Object.class);
    }

    public <V> Map<String, V> asDeepStringMap(Class<V> vClass) throws Throwable {
        return asDeepMap(String.class, vClass);
    }

    public Map<Object, Object> asDeepMap() throws Throwable {
        return asDeepMap(Object.class, Object.class);
    }

    /**
     * Convert table to map, also try to convert key or value to map. it will stop if throws exception.
     * @param kClass map key type
     * @param vClass map value type
     * @return A map
     * @param <K> key type, Cannot be primitive type
     * @param <V> value type, Cannot be primitive type
     * @throws Throwable throw any exception
     */
    public <K, V> Map<K, V> asDeepMap(Class<K> kClass, Class<V> vClass) throws Throwable {
        Map<K, V> map = new HashMap<>();
        forEach(kClass, vClass, (k, v) -> {
            Object realK = k, realV = v;
            if (k instanceof LuaTable) {
                try {
                    realK = ((LuaTable) k).asDeepMap(kClass, vClass);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            if (v instanceof LuaTable) {
                try {
                    realV = ((LuaTable) v).asDeepMap(kClass, vClass);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            map.put((K) realK, (V) realV);
        });
        return map;
    }

    public Map<Object, Object> asMap() throws Throwable {
        return asMap(Object.class, Object.class);
    }

    /**
     * Convert table to map. it will stop if throws exception.
     * @param kClass map key type
     * @param vClass map value type
     * @return A map
     * @param <K> key type, Cannot be primitive type
     * @param <V> value type, Cannot be primitive type
     * @throws Throwable throw any exception
     */
    public <K, V> Map<K, V> asMap(Class<K> kClass, Class<V> vClass) throws Throwable {
        Map<K, V> map = new HashMap<>();
        forEach(kClass, vClass, map::put);
        return map;
    }

    public Map<String, Object> asStringMap() throws Throwable {
        return asStringMap(Object.class);
    }

    public <V> Map<String, V> asStringMap(Class<V> vClass) throws Throwable {
        return asMap(String.class, vClass);
    }

    /**
     * foreach table entry. it will stop if throws exception.
     * @param kClass   Key type
     * @param vClass   Value type
     * @param consumer consumer
     * @param <K> Key Type, Cannot be primitive type
     * @param <V> Value Type, Cannot be primitive type
     * @throws Throwable any exception
     */
    public <K, V> void forEach(Class<K> kClass, Class<V> vClass, BiConsumer<K, V> consumer) throws Throwable {
        synchronized (L) {
            push();
            L.pushNil();
            while (L.next(-2) != 0) {
                try {
                    Object key = L.toJavaObject(-2);
                    Object value = L.toJavaObject(-1);
                    consumer.accept(kClass.cast(key), vClass.cast(value));
                } catch (Throwable e) {
                    L.pop(2);
                    throw e;
                } finally {
                    L.pop(1);
                }
            }
            L.pop(1);
        }
    }

    /**
     * foreach table entry. it will stop if throws exception.
     * @param consumer consumer
     * @throws Throwable any exception
     */
    public void forEach(BiConsumer<Object, Object> consumer) throws Throwable {
        forEach(Object.class, Object.class, consumer);
    }

    /**
     * Check the table is Array or not. Will traverse the table.
     * @param L     lua state
     * @param index index
     * @return if the table is empty ({})
     *         or the table only a series of consecutive numbers starting with 1 as key.
     */
    protected static boolean isArrayTable(LuaState L, int index) {
        synchronized (L) {
            int len = L.objLen(index);
            index -= 1;
            int i = 0;
            L.pushNil();
            while (L.next(index) != 0) {
                if (L.type(-2) != LuaType.NUMBER) {
                    L.pop(2);
                    return false;
                }
                double number = L.toNumber(-2);
                if (number != (int) number || number < 1) {
                    L.pop(2);
                    return false;
                }
                int idx = (int) number;
                if (idx - i != 1) {
                    L.pop(2);
                    return false;
                }
                i = idx;
                L.pop(1);
            }
            return len == i;
        }
    }
}
