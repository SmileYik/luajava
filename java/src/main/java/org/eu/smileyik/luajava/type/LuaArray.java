package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
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
    Function<LuaArray, Result<?, ? extends Exception>> EMPTY =
            array -> Result.failure(new IllegalArgumentException("Need primitive type!"));
    private static final Map<Class<?>, Function<LuaArray, Result<?, ? extends Exception>>> UNBOXED_ARRAY_TRANSFORMERS;

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
        return asDeepList(Object.class).replaceErrorString(Object::toString).toString();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    public int length() {
        return len;
    }

    public Result<byte[], ? extends Exception> toByteArray() {
        byte[] bytes = new byte[len];
        return forEach(Double.class, (idx, num) -> bytes[idx] = num.byteValue())
                .replaceValue(bytes);
    }

    public Result<short[], ? extends Exception> toShortArray() {
        short[] shorts = new short[len];
        return forEach(Double.class, (idx, num) -> shorts[idx] = num.shortValue())
                .replaceValue(shorts);
    }

    public Result<int[], ? extends Exception> toIntArray() {
        int[] nums = new int[len];
        return forEach(Double.class, (idx, num) -> nums[idx] = num.intValue())
                .replaceValue(nums);
    }

    public Result<long[], ? extends Exception> toLongArray() {
        long[] longs = new long[len];
        return forEach(Double.class, (idx, num) -> longs[idx] = num.longValue())
                .replaceValue(longs);
    }

    public Result<float[], ? extends Exception> toFloatArray() {
        float[] floats = new float[len];
        return forEach(Double.class, (idx, num) -> floats[idx] = num.floatValue())
                .replaceValue(floats);
    }

    public Result<boolean[], ? extends Exception> toBooleanArray() {
        boolean[] bool = new boolean[len];
        return forEach(Boolean.class, (idx, b) -> bool[idx] = b)
                .replaceValue(bool);
    }

    public Result<char[], ? extends Exception> toCharArray() {
        char[] chars = new char[len];
        return forEach(String.class, (idx, str) -> chars[idx] = str.charAt(0))
                .replaceValue(chars);
    }

    public Result<double[], ? extends Exception> toDoubleArray() {
        double[] doubles = new double[len];
        return forEach(Double.class, (idx, num) -> doubles[idx] = num)
                .replaceValue(doubles);
    }

    public Result<List<Object>, ? extends Exception> asList() {
        return asList(Object.class);
    }

    /**
     * <strong>Not support primitive type!</strong>
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> Result<List<T>, ? extends Exception> asList(Class<T> clazz) {
        if (clazz.isPrimitive()) {
            return Result.failure(new IllegalArgumentException("Primitive type is not supported"));
        } else if (clazz == Character.class) {
            return asCharacterList().justCast();
        }
        List<T> list = new ArrayList<>(len);
        return forEachValue(clazz, list::add).replaceValue(list);
    }

    public Result<List<Character>, ? extends Exception> asCharacterList() {
        List<Character> list = new ArrayList<>(len);
        return forEachValue(String.class, it -> list.add(it.charAt(0)))
                .replaceValue(list);
    }

    /**
     * for example: <code>{{1, 2, 3}, {}}</code> -> <code>[[1.0, 2.0, 3.0], []]</code>
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> Result<List<T>, ? extends Exception> asDeepList(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return Result.failure(new IllegalArgumentException("Primitive type is not supported"));
        } else if (clazz == Character.class) {
            return asDeepCharacterList();
        }
        List<Object> list = new ArrayList<>(len);
        // once exception then need stop!
        return forEachValue(v -> {
            if (v instanceof LuaArray) {
                list.add(((LuaArray) v).asDeepList(clazz).getOrSneakyThrow());
            } else {
                list.add(clazz.cast(v));
            }
        }).replaceValue((List<T>) list);
    }

    public <T> Result<List<T>, ? extends Exception> asDeepCharacterList() {
        List<Object> list = new ArrayList<>(len);
        // once exception then need stop!
        return forEachValue(v -> {
            if (v instanceof LuaArray) {
                list.add(((LuaArray) v).asDeepCharacterList().getOrSneakyThrow());
            } else {
                list.add(String.valueOf(v).charAt(0));
            }
        }).replaceValue((List<T>) list);
    }

    public Result<Object[], ? extends Exception> asArray() {
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
    public <T> Result<T[], ? extends Exception> asArray(Class<T> clazz) {
        if (clazz.isPrimitive()) {
            return Result.failure(new IllegalArgumentException("Primitive type is not supported"));
        }
        return asList(clazz).mapValue(list -> list.toArray((T[]) Array.newInstance(clazz, 0)));
    }

    /**
     * <strong>Only support primitive array type!</strong>
     * @param tClass primitive array, like <code>int[].class</code>
     * @return
     * @param <T>
     */
    public <T> Result<T, ? extends Exception> asPrimitiveArray(Class<T> tClass) {
        if (!tClass.isArray() || !tClass.getComponentType().isPrimitive()) {
            return Result.failure(new IllegalArgumentException("Not a primitive array type: " + tClass));
        }
        return UNBOXED_ARRAY_TRANSFORMERS.getOrDefault(tClass, EMPTY).apply(this).justCast();
    }

    public Result<Void, ? extends Exception> forEachValue(Consumer<Object> consumer) {
        return forEachValue(Object.class, consumer);
    }

    /**
     * just for value.
     * @param tClass   element type
     * @param consumer consumer
     * @param <T>      element type, Cannot be primitive type
     * @throws Exception any exception
     */
    public <T> Result<Void, ? extends Exception> forEachValue(Class<T> tClass, Consumer<T> consumer) {
        return luaState.lockThrowAll(l -> {
            int top = l.getTop();
            try {
                push();
                for (int i = 1; i <= len; i++) {
                    l.rawGetI(-1, i);
                    // i want finished loop if happened error.
                    Object javaObject = luaState.toJavaObject(-1).getOrThrow();
                    consumer.accept(tClass.cast(javaObject));
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
     * it's array version.
     * @param kClass   Key type, Always be <code>Integer</code>
     * @param vClass   Value type
     * @param consumer consumer
     * @param <K> Always be <code>Integer</code>, Cannot be primitive type
     * @param <V> Value type, Cannot be primitive type
     * @throws Exception any exception.
     */
    @Override
    public <K, V> Result<Void, ? extends Exception> forEach(Class<K> kClass, Class<V> vClass, BiConsumer<K, V> consumer) {
        return luaState.lockThrowAll(l -> {
            int top = l.getTop();
            try {
                push();
                for (int i = 1; i <= len; i++) {
                    l.rawGetI(-1, i);
                    Object javaObject = luaState.toJavaObject(-1).getOrThrow();
                    consumer.accept(kClass.cast(i - 1), vClass.cast(javaObject));
                    l.pop(1);
                }
                l.pop(1);
            } finally {
                l.setTop(top);
            }
            return null;
        });
    }

    public <V> Result<Void, ? extends Exception> forEach(Class<V> vClass, BiConsumer<Integer, V> consumer) {
        return forEach(Integer.class, vClass, consumer);
    }

    /**
     * foreach table entry. it will stop if throws exception.
     *
     * @param consumer consumer, the first type always be <code>Integer</code>
     * @return
     * @throws Exception any exception
     */
    @Override
    public Result<Void, ? extends Exception> forEach(BiConsumer<Object, Object> consumer) {
        return forEach(Object.class, Object.class, consumer);
    }
}
