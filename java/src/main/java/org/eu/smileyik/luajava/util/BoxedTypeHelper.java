/*
 * BoxedTypeHelper.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.util;

import java.util.*;
import java.util.function.Function;

public class BoxedTypeHelper {

    private static final Set<Class<?>> UNBOXED_NUMBER_TYPE;
    private static final Map<Class<?>, Class<?>> BOXED_2_UNBOXED_TYPES;
    private static final Map<Class<?>, Class<?>> UNBOXED_2_BOXED_TYPES;
    private static final Map<Class<?>, Function<Double, Number>> NUMBER_TRANSFORM;

    private static final Map<Class<?>, Function<Object, String>> ARRAY_TO_STRING = new HashMap<>();

    static {
        HashMap<Class<?>, Class<?>> boxed2UnboxedType = new HashMap<>();
        boxed2UnboxedType.put(Boolean.class, boolean.class);
        boxed2UnboxedType.put(Byte.class, byte.class);
        boxed2UnboxedType.put(Character.class, char.class);
        boxed2UnboxedType.put(Double.class, double.class);
        boxed2UnboxedType.put(Float.class, float.class);
        boxed2UnboxedType.put(Integer.class, int.class);
        boxed2UnboxedType.put(Long.class, long.class);
        boxed2UnboxedType.put(Short.class, short.class);
        BOXED_2_UNBOXED_TYPES = Collections.unmodifiableMap(boxed2UnboxedType);

        HashMap<Class<?>, Class<?>> unboxed2boxedType = new HashMap<>();
        boxed2UnboxedType.forEach((k, v) -> unboxed2boxedType.put(v, k));
        UNBOXED_2_BOXED_TYPES = Collections.unmodifiableMap(unboxed2boxedType);

        HashSet<Class<?>> unboxedNumberType = new HashSet<>();
        unboxedNumberType.add(int.class);
        unboxedNumberType.add(long.class);
        unboxedNumberType.add(float.class);
        unboxedNumberType.add(double.class);
        unboxedNumberType.add(short.class);
        unboxedNumberType.add(byte.class);
        UNBOXED_NUMBER_TYPE = Collections.unmodifiableSet(unboxedNumberType);

        Map<Class<?>, Function<Double, Number>> numberTransform = new HashMap<>();
        numberTransform.put(Double.class, Number::doubleValue);
        numberTransform.put(Float.class, Number::floatValue);
        numberTransform.put(Integer.class, Number::intValue);
        numberTransform.put(Long.class, Number::longValue);
        numberTransform.put(Short.class, Number::shortValue);
        numberTransform.put(Byte.class, Number::byteValue);
        numberTransform.put(double.class, numberTransform.get(Double.class));
        numberTransform.put(float.class, numberTransform.get(Float.class));
        numberTransform.put(int.class, numberTransform.get(Integer.class));
        numberTransform.put(long.class, numberTransform.get(Long.class));
        numberTransform.put(short.class, numberTransform.get(Short.class));
        numberTransform.put(byte.class, numberTransform.get(Byte.class));
        NUMBER_TRANSFORM = Collections.unmodifiableMap(numberTransform);

        ARRAY_TO_STRING.put(int.class, obj -> Arrays.toString((int[]) obj));
        ARRAY_TO_STRING.put(long.class, obj -> Arrays.toString((long[]) obj));
        ARRAY_TO_STRING.put(double.class, obj -> Arrays.toString((double[]) obj));
        ARRAY_TO_STRING.put(boolean.class, obj -> Arrays.toString((boolean[]) obj));
        ARRAY_TO_STRING.put(byte.class, obj -> Arrays.toString((byte[]) obj));
        ARRAY_TO_STRING.put(char.class, obj -> Arrays.toString((char[]) obj));
        ARRAY_TO_STRING.put(short.class, obj -> Arrays.toString((short[]) obj));
        ARRAY_TO_STRING.put(float.class, obj -> Arrays.toString((float[]) obj));
    }

    public static boolean isBoxedType(Class<?> type) {
        return BOXED_2_UNBOXED_TYPES.containsKey(type);
    }

    public static boolean isUnboxedType(Class<?> type) {
        return UNBOXED_2_BOXED_TYPES.containsKey(type);
    }

    public static Class<?> getUnboxedType(Class<?> type) {
        return BOXED_2_UNBOXED_TYPES.getOrDefault(type, type);
    }

    public static Class<?> getBoxedType(Class<?> type) {
        return UNBOXED_2_BOXED_TYPES.getOrDefault(type, type);
    }

    public static boolean isNumberType(Class<?> type) {
        return isBoxedNumberType(type) || isUnboxedNumberType(type);
    }

    public static boolean isBoxedNumberType(Class<?> type) {
        return Number.class.isAssignableFrom(type);
    }

    public static boolean isUnboxedNumberType(Class<?> type) {
        return UNBOXED_NUMBER_TYPE.contains(type);
    }

    /**
     * Transform double number to target type.
     * @param db        double number
     * @param target    target number type.
     * @return if target is not number type will return null.
     * @see BoxedTypeHelper#isNumberType(Class)
     */
    public static Number covertNumberTo(Double db, Class<?> target) {
        Function<Double, Number> function = NUMBER_TRANSFORM.get(target);
        return function == null ? null : function.apply(db);
    }

    public static String toString(Object obj) {
        if (obj == null) return null;
        Class<?> aClass = obj.getClass();
        if (aClass.isArray()) {
            Class<?> componentType = aClass.getComponentType();
            boolean flag = true;
            while (componentType.isArray()) {
                flag = false;
                componentType = componentType.getComponentType();
            }
            if (componentType.isPrimitive() && flag) {
                Function<Object, String> func = ARRAY_TO_STRING.get(componentType);
                if (func != null) {
                    return func.apply(obj);
                }
            }
            if (!componentType.isPrimitive()) {
                return Arrays.deepToString((Object[]) obj);
            }
        }
        return Objects.toString(obj);
    }
}
