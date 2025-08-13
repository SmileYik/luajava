/*
 * LuaArray.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.LuaException;
import org.eu.smileyik.luajava.LuaObject;
import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.exception.Result;

import java.lang.reflect.Array;
import java.util.*;
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

    protected int len;

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

    public boolean isEmpty() {
        return len == 0;
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
     *
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
                // endless loop...
                if (Objects.equals(this, v)) {
                    throw new RuntimeException("Cannot asDeepList, because same array as element. ");
                }
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
                if (Objects.equals(this, v)) {
                    throw new RuntimeException("Cannot asDeepList, because same array as element. ");
                }
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
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
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
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
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
     * get the array object at target index.
     * @param idx index
     * @return result
     */
    public Result<Object, ? extends LuaException> at(int idx) {
        return luaState.lockThrow(l -> {
            return doAt(idx).getOrThrow(LuaException.class);
        });
    }

    /**
     * get the array object at target index. without lock lua state.
     * @param idx index
     * @return result
     */
    public Result<Object, ? extends LuaException> doAt(int idx) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        if (idx < 0 || idx >= len) {
            return Result.failure(new LuaException("out of bounds: idx=" + idx + ", len=" + len));
        }
        LuaState inner = luaState.getLuaState();
        int top = inner.getTop();
        try {
            rawPush();
            inner.rawGetI(-1, idx + 1);
            return luaState.rawToJavaObject(-1);
        } finally {
            inner.setTop(top);
        }
    }

    /**
     * set object to target index
     * @param idx idx
     * @param obj obj
     * @return result.
     */
    public Result<Void, ? extends Exception> set(int idx, Object obj) {
        return luaState.lock(l -> {
            return rawSet(idx, obj);
        });
    }

    /**
     * set object to target index without lock lua state.
     * @param idx idx
     * @param obj obj
     * @return result
     */
    public Result<Void, ? extends LuaException> rawSet(int idx, Object obj) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        if (idx < 0 || idx >= len) {
            return Result.failure(new LuaException("out of bounds: idx=" + idx + ", len=" + len));
        } else if (obj instanceof LuaObject && !Objects.equals(luaState, ((LuaObject) obj).getLuaState())) {
            return Result.failure(new LuaException("Lua objects are in different lua states!"));
        }
        LuaState inner = luaState.getLuaState();
        int top = inner.getTop();
        try {
            rawPush();
            return luaState.rawPushObjectValue(obj)
                            .mapValue(it -> {
                                inner.rawSetI(-2, idx + 1);
                                return null;
                            });
        } finally {
            inner.setTop(top);
        }
    }

    public Result<Void, ? extends LuaException> add(Object obj) {
        return luaState.lock(it -> {
            return rawAdd(obj);
        });
    }

    public synchronized Result<Void, ? extends LuaException> rawAdd(Object obj) {
        return rawSet(len++, obj).ifFailureThen(it -> len -= 1);
    }

    @Override
    public Result<Void, ? extends LuaException> rawPut(Object key, Object value) {
        throw new UnsupportedOperationException("Lua Array not support put method. please use add method");
    }

    @Override
    public Result<Void, ? extends LuaException> rawRemove(Object key) {
        throw new UnsupportedOperationException("Lua Array not support remove method.");
    }

    public Result<Void, ? extends LuaException> remove(int idx) {
        return luaState.lock(it -> {
            return rawRemove(idx);
        });
    }

    /**
     * remove the value without lock.
     * @param idx index.
     * @return the result.
     */
    public Result<Void, ? extends LuaException> rawRemove(int idx) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        if (idx < 0 || idx >= len) {
            return Result.failure(new LuaException("out of bounds: idx=" + idx + ", len=" + len));
        }

        LuaState inner = luaState.getLuaState();
        rawPush();
        for (int i = idx + 1; i < len; i++) {
            luaState.rawGetI(-1, i + 1);
            luaState.rawSetI(-2, i);
        }
        inner.pushNil();
        inner.rawSetI(-2, len);
        len -= 1;
        inner.pop(1);
        return Result.success();
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
