package org.eu.smileyik.luajava.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BoxedTypeHelper {
    private static final Set<Class<?>> UNBOXED_NUMBER_TYPE = new HashSet<Class<?>>() {
        {
            add(int.class);
            add(long.class);
            add(float.class);
            add(double.class);
            add(short.class);
            add(byte.class);
        }
    };
    private static final Map<Class<?>, Class<?>> BOXED_2_UNBOXED_TYPES = new HashMap<Class<?>, Class<?>>() {
        {
            put(Boolean.class, boolean.class);
            put(Byte.class, byte.class);
            put(Character.class, char.class);
            put(Double.class, double.class);
            put(Float.class, float.class);
            put(Integer.class, int.class);
            put(Long.class, long.class);
            put(Short.class, short.class);
        }
    };

    private static final Map<Class<?>, Class<?>> UNBOXED_2_BOXED_TYPES = new HashMap<Class<?>, Class<?>>() {
        {
            put(boolean.class, Boolean.class);
            put(byte.class, Byte.class);
            put(char.class, Character.class);
            put(short.class, Short.class);
            put(int.class, Integer.class);
            put(long.class, Long.class);
            put(float.class, Float.class);
            put(double.class, Double.class);
        }
    };

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
}
