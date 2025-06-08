package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaState;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class LuaArray extends LuaTable {
    private interface ArrayTransform <T> extends Function<LuaArray, T> {
        public static final ArrayTransform<Object> EMPTY = array -> null;
        default T apply(LuaArray array) {
            try {
                return transform(array);
            } catch (Exception t) {
                return null;
            }
        }

        T transform(LuaArray array) throws Exception;
    }
    private static final Map<Class<?>, ArrayTransform<Object>> UNBOXED_ARRAY_TRANSFORMERS = new HashMap<>() {
        {
            put(int[].class, LuaArray::toIntArray);
            put(long[].class, LuaArray::toLongArray);
            put(float[].class, LuaArray::toFloatArray);
            put(boolean[].class, LuaArray::toBooleanArray);
            put(char[].class, LuaArray::toCharArray);
            put(short[].class, LuaArray::toShortArray);
            put(byte[].class, LuaArray::toByteArray);
            put(double[].class, LuaArray::toDoubleArray);
        }
    };

    protected final int len;

    /**
     * create lua array.
     * @param L      lua state
     * @param index  index
     * @param len    array length
     */
    protected LuaArray(LuaState L, int index, int len) {
        super(L, index);
        this.len = len;
    }

    @Override
    public String toString() {
        return "[Lua Array]";
    }

    @Override
    public boolean isArray() {
        return true;
    }

    public int length() {
        return len;
    }

    public byte[] toByteArray() throws Exception {
        List<Number> list = asList(Number.class);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            bytes[i] = number.byteValue();
        }
        return bytes;
    }

    public short[] toShortArray() throws Exception {
        List<Number> list = asList(Number.class);
        short[] shorts = new short[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            shorts[i] = number.shortValue();
        }
        return shorts;
    }

    public int[] toIntArray() throws Exception {
        List<Number> list = asList(Number.class);
        int[] nums = new int[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            nums[i] = number.intValue();
        }
        return nums;
    }

    public long[] toLongArray() throws Exception {
        List<Number> list = asList(Number.class);
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            longs[i] = number.longValue();
        }
        return longs;
    }

    public float[] toFloatArray() throws Exception {
        List<Number> list = asList(Number.class);
        float[] floats = new float[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            floats[i] = number.floatValue();
        }
        return floats;
    }

    public boolean[] toBooleanArray() throws Exception {
        List<Boolean> list = asList(Boolean.class);
        boolean[] bool = new boolean[len];
        for (int i = 0; i < len; i++) {
            bool[i] = list.get(i);;
        }
        return bool;
    }

    public char[] toCharArray() throws Exception {
        List<Character> list = asList(Character.class);
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = list.get(i);
        }
        return chars;
    }

    public double[] toDoubleArray() throws Exception {
        List<Number> list = asList(Number.class);
        double[] doubles = new double[len];
        for (int i = 0; i < len; i++) {
            Number number = list.get(i);
            doubles[i] = number.doubleValue();
        }
        return doubles;
    }

    public List<Object> asList() throws Exception {
        return asList(Object.class);
    }

    public <T> List<T> asList(Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        forEachValue(clazz, list::add);
        return list;
    }

    public Object[] asArray() throws Exception {
        return asArray(Object.class);
    }

    public <T> T[] asArray(Class<T> clazz) throws Exception {
        List<T> list = asList(clazz);
        T[] t = (T[]) Array.newInstance(clazz, 0);
        return list.toArray(t);
    }

    public <T> T asPrimitiveArray(Class<T> tClass) {
        return (T) UNBOXED_ARRAY_TRANSFORMERS.getOrDefault(tClass, ArrayTransform.EMPTY).apply(this);
    }

    public void forEachValue(Consumer<Object> consumer) throws Exception {
        forEachValue(Object.class, consumer);
    }

    /**
     * just for value.
     * @param tClass   element type
     * @param consumer consumer
     * @param <T>      element type, Cannot be primitive type
     * @throws Exception any exception
     */
    public <T> void forEachValue(Class<T> tClass, Consumer<T> consumer) throws Exception {
        synchronized (L) {
            push();
            for (int i = 1; i <= len; i++) {
                L.rawGetI(-1, i);
                try {
                    Object javaObject = L.toJavaObject(-1);
                    consumer.accept(tClass.cast(javaObject));
                } catch (Exception e) {
                    L.pop(1);
                    throw e;
                } finally {
                    L.pop(1);
                }
            }
            L.pop(1);
        }
    }

    /**
     * it's array version.
     * @param kClass   Key type, Always be <code>Integer</code>
     * @param vClass   Value type
     * @param consumer consumer
     * @param <K> Always be <code>Integer</code>, Cannot be primitive type
     * @param <V> Value type, Cannot be primitive type
     * @throws Exception any exception.
     */
    @Override
    public <K, V> void forEach(Class<K> kClass, Class<V> vClass, BiConsumer<K, V> consumer) throws Exception {
        synchronized (L) {
            push();
            for (int i = 1; i <= len; i++) {
                L.rawGetI(-1, i);
                try {
                    Object javaObject = L.toJavaObject(-1);
                    consumer.accept(kClass.cast(i - 1), vClass.cast(javaObject));
                } catch (Exception e) {
                    L.pop(1);
                    throw e;
                } finally {
                    L.pop(1);
                }
            }
            L.pop(1);
        }
    }

    public <V> void forEach(Class<V> vClass, BiConsumer<Integer, V> consumer) throws Exception {
        forEach(Integer.class, vClass, consumer);
    }

    /**
     * foreach table entry. it will stop if throws exception.
     * @param consumer consumer, the first type always be <code>Integer</code>
     * @throws Exception any exception
     */
    @Override
    public void forEach(BiConsumer<Object, Object> consumer) throws Exception {
        forEach(Object.class, Object.class, consumer);
    }
}
