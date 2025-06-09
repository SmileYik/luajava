package org.eu.smileyik.luajava.util;

import java.util.*;
import java.util.function.Function;

public class BoxedTypeHelper {

    private static final Set<Class<?>> UNBOXED_NUMBER_TYPE;
    private static final Map<Class<?>, Class<?>> BOXED_2_UNBOXED_TYPES;
    private static final Map<Class<?>, Class<?>> UNBOXED_2_BOXED_TYPES;
    private static final Map<Class<?>, Function<Double, Number>> NUMBER_TRANSFORM;

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
}
