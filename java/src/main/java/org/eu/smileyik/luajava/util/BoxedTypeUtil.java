package org.eu.smileyik.luajava.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BoxedTypeUtil {

    public static final Map<Class<?>, Class<?>> UNBOXED_NUMBER_TYPES = new HashMap<Class<?>, Class<?>>() {
        {
            put(int.class, Integer.class);
            put(long.class, Long.class);
            put(float.class, Float.class);
            put(double.class, Double.class);
            put(short.class, Short.class);
            put(byte.class, Byte.class);
        }
    };

    /**
     * unbox value to target primitive type.
     * @param primitiveType primitive type
     * @param value value
     * @return
     * @param <T>
     */
    public static <T> T unboxNumberTo(Class<T> primitiveType, Object value) {
        if (value.getClass().isPrimitive()) {
            return primitiveType.cast(value);
        }

        if (value instanceof Number && UNBOXED_NUMBER_TYPES.containsKey(primitiveType)) {
            return castNumber(primitiveType, ((Number) value));
        }
        return primitiveType.cast(value);
    }

    private static <T> T castNumber(Class<T> type, Number value) {
        if (type == int.class || Integer.class.isAssignableFrom(type)) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == long.class || Long.class.isAssignableFrom(type)) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (type == float.class || Float.class.isAssignableFrom(type)) {
            return (T) Float.valueOf(((Number) value).floatValue());
        } else if (type == double.class || Double.class.isAssignableFrom(type)) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == short.class || Short.class.isAssignableFrom(type)) {
            return (T) Short.valueOf(((Number) value).shortValue());
        } else if (type == byte.class || Byte.class.isAssignableFrom(type)) {
            return (T) Byte.valueOf(((Number) value).byteValue());
        }
        return type.cast(value);
    }

    public static void main(String[] args) {
        // 要创建的数组类型
        Class<?> componentType = double.class;

        // 数组长度
        int length = 5;

        // 使用反射创建数组
        Object array = Array.newInstance(componentType, length);

        // 检查是否是数组
        if (array.getClass().isArray() && array instanceof int[]) {
            System.out.println("成功创建了一个 int[] 数组");
        }

        // 向数组中写入数据
        for (int i = 0; i < length; i++) {
            Array.set(array, i, i + 10);  // 自动装箱为 Integer，但底层会自动处理为 int
        }

        // 读取数组内容
        for (int i = 0; i < length; i++) {
            int value = Array.getInt(array, i);  // 推荐方式获取基本类型值
            System.out.println("array[" + i + "] = " + value);
        }
    }
}
