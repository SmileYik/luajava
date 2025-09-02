/*
 * ConvertablePriority.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.reflect;

import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.type.LuaArray;
import org.eu.smileyik.luajava.util.BoxedTypeHelper;
import org.eu.smileyik.luajava.util.LRUCache;
import org.eu.smileyik.luajava.util.ParamRef;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConvertablePriority {
    public static final byte NOT_MATCH  = -1;
    public static final byte FULL_MATCH =  0;

    public static final byte NULL_TO_ALL           = 10;
    public static final byte ASSIGNABLE            = 20;
    public static final byte BOXED_TYPE_EQUALS     = 25;
    public static final byte BOXED_TYPE_ASSIGNABLE = 30;

    // double convert to other number type.
    public static final byte DOUBLE_TO_DOUBLE = 40;
    public static final byte DOUBLE_TO_FLOAT  = 41;
    public static final byte DOUBLE_TO_LONG   = 42;
    public static final byte DOUBLE_TO_INT    = 43;
    public static final byte DOUBLE_TO_SHORT  = 44;
    public static final byte DOUBLE_TO_BYTE   = 45;

    // array double ...
    public static final byte ARRAY_DOUBLE_TO_DOUBLE = 50;
    public static final byte ARRAY_DOUBLE_TO_FLOAT  = 51;
    public static final byte ARRAY_DOUBLE_TO_LONG   = 52;
    public static final byte ARRAY_DOUBLE_TO_INT    = 53;
    public static final byte ARRAY_DOUBLE_TO_SHORT  = 54;
    public static final byte ARRAY_DOUBLE_TO_BYTE   = 55;

    // array to other
    public static final byte ARRAY_TO_PRIMITIVE_ARRAY       = 60;
    public static final byte ARRAY_TO_SPECIFIC_OBJECT_ARRAY = 61;
    public static final byte ARRAY_TO_OBJECT_ARRAY          = 64;
    public static final byte ARRAY_TO_OBJECT                = 65;

    public static final byte ANY_TO_OBJECT = 70;

    public static final Map<Integer, Byte> DOUBLE_CONVERT_PRIMITIVE;
    public static final Map<Integer, Byte> DOUBLE_ARRAY_CONVERT_PRIMITIVE;
    public static final Map<Integer, Function<LuaArray, ?>> UNBOXED_LUA_ARRAY_TRANSFORMERS;
    public static final Map<Integer, Function<Object[], ?>> OBJECT_ARRAY_TRANSFORMERS;

    private static final LRUCache<Long, Object[]> LUA_ARRAY_CACHE = new LRUCache<>(64);

    static {
        Map<Integer, Byte> doubleConvert = new HashMap<>();
        doubleConvert.put(Double.TYPE.hashCode(), DOUBLE_TO_DOUBLE);
        doubleConvert.put(Double.class.hashCode(), DOUBLE_TO_DOUBLE);
        doubleConvert.put(Float.TYPE.hashCode(), DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.TYPE.hashCode(), DOUBLE_TO_LONG);
        doubleConvert.put(Integer.TYPE.hashCode(), DOUBLE_TO_INT);
        doubleConvert.put(Short.TYPE.hashCode(), DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.TYPE.hashCode(), DOUBLE_TO_BYTE);
        doubleConvert.put(Float.class.hashCode(), DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.class.hashCode(), DOUBLE_TO_LONG);
        doubleConvert.put(Integer.class.hashCode(), DOUBLE_TO_INT);
        doubleConvert.put(Short.class.hashCode(), DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.class.hashCode(), DOUBLE_TO_BYTE);
        DOUBLE_CONVERT_PRIMITIVE = Collections.unmodifiableMap(doubleConvert);

        doubleConvert.clear();
        doubleConvert.put(Double.TYPE.hashCode(), ARRAY_DOUBLE_TO_DOUBLE);
        doubleConvert.put(Double.class.hashCode(), ARRAY_DOUBLE_TO_DOUBLE);
        doubleConvert.put(Float.TYPE.hashCode(), ARRAY_DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.TYPE.hashCode(), ARRAY_DOUBLE_TO_LONG);
        doubleConvert.put(Integer.TYPE.hashCode(), ARRAY_DOUBLE_TO_INT);
        doubleConvert.put(Short.TYPE.hashCode(), ARRAY_DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.TYPE.hashCode(), ARRAY_DOUBLE_TO_BYTE);
        doubleConvert.put(Float.class.hashCode(), ARRAY_DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.class.hashCode(), ARRAY_DOUBLE_TO_LONG);
        doubleConvert.put(Integer.class.hashCode(), ARRAY_DOUBLE_TO_INT);
        doubleConvert.put(Short.class.hashCode(), ARRAY_DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.class.hashCode(), ARRAY_DOUBLE_TO_BYTE);
        DOUBLE_ARRAY_CONVERT_PRIMITIVE = Collections.unmodifiableMap(doubleConvert);

        Map<Integer, Function<LuaArray, ?>> unboxedLuaArrayTransformers = new HashMap<>();
        unboxedLuaArrayTransformers.put(int[].class.hashCode(), ConvertablePriority::luaArrayToIntArray);
        unboxedLuaArrayTransformers.put(long[].class.hashCode(), ConvertablePriority::luaArrayToLongArray);
        unboxedLuaArrayTransformers.put(float[].class.hashCode(), ConvertablePriority::luaArrayToFloatArray);
        unboxedLuaArrayTransformers.put(boolean[].class.hashCode(), ConvertablePriority::luaArrayToBooleanArray);
        unboxedLuaArrayTransformers.put(char[].class.hashCode(), ConvertablePriority::luaArrayToCharArray);
        unboxedLuaArrayTransformers.put(short[].class.hashCode(), ConvertablePriority::luaArrayToShortArray);
        unboxedLuaArrayTransformers.put(byte[].class.hashCode(), ConvertablePriority::luaArrayToByteArray);
        unboxedLuaArrayTransformers.put(double[].class.hashCode(), ConvertablePriority::luaArrayToDoubleArray);
        UNBOXED_LUA_ARRAY_TRANSFORMERS = Collections.unmodifiableMap(unboxedLuaArrayTransformers);

        Map<Integer, Function<Object[], ?>> objectArrayTransformers = new HashMap<>();
        objectArrayTransformers.put(int[].class.hashCode(), ConvertablePriority::objectArrayToIntArray);
        objectArrayTransformers.put(long[].class.hashCode(), ConvertablePriority::objectArrayToLongArray);
        objectArrayTransformers.put(float[].class.hashCode(), ConvertablePriority::objectArrayToFloatArray);
        objectArrayTransformers.put(boolean[].class.hashCode(), ConvertablePriority::objectArrayToBooleanArray);
        objectArrayTransformers.put(char[].class.hashCode(), ConvertablePriority::objectArrayToCharArray);
        objectArrayTransformers.put(short[].class.hashCode(), ConvertablePriority::objectArrayToShortArray);
        objectArrayTransformers.put(byte[].class.hashCode(), ConvertablePriority::objectArrayToByteArray);
        objectArrayTransformers.put(double[].class.hashCode(), ConvertablePriority::objectArrayToDoubleArray);
        OBJECT_ARRAY_TRANSFORMERS = Collections.unmodifiableMap(objectArrayTransformers);
    }

    public static byte double2TypePriority(Class<?> target) {
        if (target == null) return NOT_MATCH;
        return DOUBLE_CONVERT_PRIMITIVE.getOrDefault(target.hashCode(), NOT_MATCH);
    }

    public static byte arrayPriority(Class<?> componentType) {
        if (componentType == null) return NOT_MATCH;
        Byte b = DOUBLE_ARRAY_CONVERT_PRIMITIVE.get(componentType.hashCode());
        if (b != null) return b;
        if (componentType.isPrimitive()) {
            return ARRAY_TO_PRIMITIVE_ARRAY;
        } else if (componentType == Object.class) {
            return ARRAY_TO_OBJECT_ARRAY;
        } else {
            return ARRAY_TO_SPECIFIC_OBJECT_ARRAY;
        }
    }

    private static byte doIsConvertableArray(int limitedPriority, LuaArray luaObj, Class<?> toType, ParamRef<Object> overwrite) {
        Class<?> componentType = toType.getComponentType();
        byte priority = arrayPriority(componentType);
        if (priority == NOT_MATCH) return NOT_MATCH;
        if (limitedPriority < priority) return NOT_MATCH;

        long luaPointer = luaObj.rawGetLuaPointer();
        Object[] objects = LUA_ARRAY_CACHE.computeIfAbsent(luaPointer, ptr -> {
            Object[] array = new Object[luaObj.length()];
            luaObj.rawForEach(Integer.class, Object.class, (i, v) -> {
                array[i] = v;
                return false;
            });
            return array;
        });
        Object array = componentType.isPrimitive() ?
                OBJECT_ARRAY_TRANSFORMERS.get(toType.hashCode()).apply(objects) :
                objectArrayToObjectArray(objects, componentType);

//        Object array = componentType.isPrimitive() ?
//                UNBOXED_LUA_ARRAY_TRANSFORMERS.get(toType.hashCode()).apply(luaObj) :
//                luaArrayToObjectArray(luaObj, componentType);


        if (array == null) return NOT_MATCH;
        else if (overwrite != null) {
            overwrite.setParam(array);
        }

        return priority;
    }

    /**
     * check lua object can be converted to target type or not.
     * @param limitedPriority   limited max returned priority.
     * @param luaObj            lua object
     * @param toType            target type
     * @param overwrite         if not want converted result then allow set to null.
     * @return the result. may over limitedPriority.
     * @see ConvertablePriority#NOT_MATCH
     */
    public static byte isConvertableType(int limitedPriority, Object luaObj,
                                         Class<?> toType, ParamRef<Object> overwrite) {
        // 如果lua类型为null, 那么仅有能转换为非基础类型
        if (luaObj == null) {
            return toType.isPrimitive() ? NOT_MATCH : NULL_TO_ALL;
        }

        // 当 to 为 from 的超类时, 若两个类相同则返回优先级 0, 不等返回优先级 1;
        boolean isToObject = toType == Object.class;
        Class<?> fromClass = luaObj.getClass();
        if (fromClass == toType) {
            return FULL_MATCH;
        } else if (toType.isAssignableFrom(fromClass) && !isToObject) {
            return ASSIGNABLE;
        }

        // 获取装箱类型 去除可以直接自动转换的类型
        Class<?> boxedFromClass = BoxedTypeHelper.getBoxedType(fromClass);
        Class<?> boxedToClass = BoxedTypeHelper.getBoxedType(toType);
        if (boxedToClass.isAssignableFrom(boxedFromClass) && !isToObject) {
            return boxedFromClass == boxedToClass ? BOXED_TYPE_EQUALS : BOXED_TYPE_ASSIGNABLE;
        }

        // 如果两个都是数字类型, 因为lua不分数字, 所以要转换.
        // 因为能够直接互相转换的已经被去除了, 剩下的都是得相互转换的.
        if (BoxedTypeHelper.isBoxedNumberType(boxedFromClass) &&
                BoxedTypeHelper.isBoxedNumberType(boxedToClass)) {
            // over limited
            byte priority = double2TypePriority(toType);
            if (priority == NOT_MATCH || limitedPriority < priority) {
                return NOT_MATCH;
            }
            if (overwrite != null) {
                overwrite.setParam(BoxedTypeHelper.covertNumberTo(
                        ((Number) luaObj).doubleValue(), boxedToClass));
            }
            return priority;
        }

        // 数组转换
        boolean isLuaArray = luaObj instanceof LuaArray;
        if (isLuaArray && toType.isArray()) {
            return doIsConvertableArray(limitedPriority, (LuaArray) luaObj, toType, overwrite);
        }

        // at latest to handle object type.
        if (isToObject) {
            if (limitedPriority < ARRAY_TO_OBJECT) return NOT_MATCH;
            if (isLuaArray) {
                return ARRAY_TO_OBJECT;
            } else {
                return ANY_TO_OBJECT;
            }
        }

        return NOT_MATCH;
    }

    private static Object objectArrayToObjectArray(Object[] objects, Class<?> toType) {
        Object array = Array.newInstance(toType, objects.length);
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) continue;
            else if (toType.isInstance(obj)) Array.set(array, i, obj);
            else return null;
        }
        return array;
    }

    private static byte[] objectArrayToByteArray(Object[] objects) {
        byte[] array = new byte[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).byteValue();
            else return null;
        }
        return array;
    }

    private static short[] objectArrayToShortArray(Object[] objects) {
        short[] array = new short[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).shortValue();
            else return null;
        }
        return array;
    }

    private static int[] objectArrayToIntArray(Object[] objects) {
        int[] array = new int[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).intValue();
            else return null;
        }
        return array;
    }

    private static long[] objectArrayToLongArray(Object[] objects) {
        long[] array = new long[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).longValue();
            else return null;
        }
        return array;
    }

    private static float[] objectArrayToFloatArray(Object[] objects) {
        float[] array = new float[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).floatValue();
            else return null;
        }
        return array;
    }

    private static double[] objectArrayToDoubleArray(Object[] objects) {
        double[] array = new double[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof Number) array[i] = ((Number) obj).doubleValue();
            else return null;
        }
        return array;
    }

    private static boolean[] objectArrayToBooleanArray(Object[] objects) {
        boolean[] array = new boolean[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj instanceof Boolean) array[i] = (boolean) obj;
            else return null;
        }
        return array;
    }

    private static char[] objectArrayToCharArray(Object[] objects) {
        char[] array = new char[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj == null) return null;
            else if (obj instanceof String) array[i] = ((String) obj).charAt(0);
            else return null;
        }
        return array;
    }

    private static Object luaArrayToObjectArray(LuaArray luaObj, Class<?> toType) {
        Object array = Array.newInstance(toType, luaObj.length());
        Result<Boolean, ? extends Exception> result = luaObj.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null || toType.isInstance(v)) {
                Array.set(array, k, v);
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static byte[] luaArrayToByteArray(LuaArray luaArray) {
        byte[] array = new byte[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).byteValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static short[] luaArrayToShortArray(LuaArray luaArray) {
        short[] array = new short[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).shortValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static int[] luaArrayToIntArray(LuaArray luaArray) {
        int[] array = new int[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).intValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static long[] luaArrayToLongArray(LuaArray luaArray) {
        long[] array = new long[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).longValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static double[] luaArrayToDoubleArray(LuaArray luaArray) {
        double[] array = new double[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).doubleValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static float[] luaArrayToFloatArray(LuaArray luaArray) {
        float[] array = new float[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Number) {
                array[k] = ((Number) v).floatValue();
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static boolean[] luaArrayToBooleanArray(LuaArray luaArray) {
        boolean[] array = new boolean[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof Boolean) {
                array[k] = (Boolean) v;
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }

    private static char[] luaArrayToCharArray(LuaArray luaArray) {
        char[] array = new char[luaArray.length()];
        Result<Boolean, ? extends Exception> result = luaArray.rawForEach(Integer.class, Object.class, (k, v) -> {
            if (v == null) return true;
            else if (v instanceof String) {
                array[k] = ((String) v).charAt(0);
                return false;
            }
            return true;
        });
        return result.isSuccess() && result.getValue() ? array : null;
    }
}
