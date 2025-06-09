package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaStateFacade;

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
    private static final Map<Class<?>, ArrayTransform<Object>> UNBOXED_ARRAY_TRANSFORMERS;

    static {
        UNBOXED_ARRAY_TRANSFORMERS = new HashMap<>();
        UNBOXED_ARRAY_TRANSFORMERS.put(int[].class, LuaArray::toIntArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(long[].class, LuaArray::toLongArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(float[].class, LuaArray::toFloatArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(boolean[].class, LuaArray::toBooleanArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(char[].class, LuaArray::toCharArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(short[].class, LuaArray::toShortArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(byte[].class, LuaArray::toByteArray);
        UNBOXED_ARRAY_TRANSFORMERS.put(double[].class, LuaArray::toDoubleArray);
    }

    protected final int len;

    /**
     * create lua array. and make sure the object at this index is exactly array style table.
     * actually a LuaArray instance could be created by the factory method of LuaTable.
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY, EXPECT YOU KNOW WHAT YOU ARE DOING</strong>
     *
     * @param L      lua state
     * @param index  index
     * @param len    array length
     */
    protected LuaArray(LuaStateFacade L, int index, int len) {
        super(L, index);
        this.len = len;
    }

    @Override
    public String toString() {
        try {
            return asDeepList(Object.class).toString();
        } catch (Exception e) {
            return "[Lua Array]";
        }
    }

    @Override
    public boolean isArray() {
        return true;
    }

    public int length() {
        return len;
    }

    public byte[] toByteArray() throws Exception {
        byte[] bytes = new byte[len];
        forEach(Double.class, (idx, num) -> bytes[idx] = num.byteValue());
        return bytes;
    }

    public short[] toShortArray() throws Exception {
        short[] shorts = new short[len];
        forEach(Double.class, (idx, num) -> shorts[idx] = num.shortValue());
        return shorts;
    }

    public int[] toIntArray() throws Exception {
        int[] nums = new int[len];
        forEach(Double.class, (idx, num) -> nums[idx] = num.intValue());
        return nums;
    }

    public long[] toLongArray() throws Exception {
        long[] longs = new long[len];
        forEach(Double.class, (idx, num) -> longs[idx] = num.longValue());
        return longs;
    }

    public float[] toFloatArray() throws Exception {
        float[] floats = new float[len];
        forEach(Double.class, (idx, num) -> floats[idx] = num.floatValue());
        return floats;
    }

    public boolean[] toBooleanArray() throws Exception {
        boolean[] bool = new boolean[len];
        forEach(Boolean.class, (idx, b) -> bool[idx] = b);
        return bool;
    }

    public char[] toCharArray() throws Exception {
        char[] chars = new char[len];
        forEach(String.class, (idx, str) -> chars[idx] = str.charAt(0));
        return chars;
    }

    public double[] toDoubleArray() throws Exception {
        double[] doubles = new double[len];
        forEach(Double.class, (idx, num) -> doubles[idx] = num);
        return doubles;
    }

    public List<Object> asList() throws Exception {
        return asList(Object.class);
    }

    /**
     * <strong>Not support primitive type!</strong>
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> List<T> asList(Class<T> clazz) throws Exception {
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Primitive type is not supported");
        } else if (clazz == Character.class) {
            return (List<T>) asCharacterList();
        }
        List<T> list = new ArrayList<>(len);
        forEachValue(clazz, list::add);
        return list;
    }

    public List<Character> asCharacterList() throws Exception {
        List<Character> list = new ArrayList<>(len);
        forEachValue(String.class, it -> list.add(it.charAt(0)));
        return list;
    }

    /**
     * for example: <code>{{1, 2, 3}, {}}</code> -> <code>[[1.0, 2.0, 3.0], []]</code>
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> List<T> asDeepList(Class<?> clazz) throws Exception {
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Primitive type is not supported");
        } else if (clazz == Character.class) {
            return asDeepCharacterList();
        }
        List<Object> list = new ArrayList<>(len);
        forEachValue(v -> {
            if (v instanceof LuaArray) {
                try {
                    list.add(((LuaArray) v).asDeepList(clazz));
                } catch (Exception e) {
                    throw new IllegalArgumentException("LuaArray.asDeepList Error", e);
                }
            } else {
                list.add(clazz.cast(v));
            }
        });
        return (List<T>) list;
    }

    public <T> List<T> asDeepCharacterList() throws Exception {
        List<Object> list = new ArrayList<>(len);
        forEachValue(v -> {
            if (v instanceof LuaArray) {
                try {
                    list.add(((LuaArray) v).asDeepCharacterList());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                list.add(String.valueOf(v).charAt(0));
            }
        });
        return (List<T>) list;
    }

    public Object[] asArray() throws Exception {
        return asArray(Object.class);
    }

    /**
     * <strong>Not support primitive type!</strong>
     * if you want covert to primitive type array, please use <code>asPrimitiveArray()</code>
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     * @see LuaArray#asPrimitiveArray(Class)
     */
    public <T> T[] asArray(Class<T> clazz) throws Exception {
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Primitive type is not supported");
        }
        List<T> list = asList(clazz);
        T[] t = (T[]) Array.newInstance(clazz, 0);
        return list.toArray(t);
    }

    /**
     * <strong>Only support primitive array type!</strong>
     * @param tClass primitive array, like <code>int[].class</code>
     * @return
     * @param <T>
     */
    public <T> T asPrimitiveArray(Class<T> tClass) {
        if (!tClass.isArray() || !tClass.getComponentType().isPrimitive()) {
            throw new IllegalArgumentException("Not a primitive array type: " + tClass);
        }
        return tClass.cast(UNBOXED_ARRAY_TRANSFORMERS.getOrDefault(tClass, ArrayTransform.EMPTY).apply(this));
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
        luaState.lockThrowAll(l -> {
            int top = l.getTop();
            try {
                push();
                for (int i = 1; i <= len; i++) {
                    l.rawGetI(-1, i);
                    Object javaObject = luaState.toJavaObject(-1);
                    consumer.accept(tClass.cast(javaObject));
                    l.pop(1);
                }
                l.pop(1);
            } finally {
                l.setTop(top);
            }
        });
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
        luaState.lockThrowAll(l -> {
            int top = l.getTop();
            try {
                push();
                for (int i = 1; i <= len; i++) {
                    l.rawGetI(-1, i);
                    Object javaObject = luaState.toJavaObject(-1);
                    consumer.accept(kClass.cast(i - 1), vClass.cast(javaObject));
                    l.pop(1);
                }
                l.pop(1);
            } finally {
                l.setTop(top);
            }

        });
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
