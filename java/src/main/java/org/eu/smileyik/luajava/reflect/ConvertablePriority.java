package org.eu.smileyik.luajava.reflect;

import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.type.LuaArray;
import org.eu.smileyik.luajava.util.BoxedTypeHelper;
import org.eu.smileyik.luajava.util.ParamRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    public static final byte ARRAY_TO_OBJECT_ARRAY          = 62;
    public static final byte ARRAY_TO_OBJECT                = 65;

    public static final byte ANY_TO_OBJECT = 70;

    public static final Map<Class<?>, Byte> DOUBLE_CONVERT_PRIMITIVE;
    public static final Map<Class<?>, Byte> DOUBLE_ARRAY_CONVERT_PRIMITIVE;

    static {
        Map<Class<?>, Byte> doubleConvert = new HashMap<>();
        doubleConvert.put(Double.TYPE, DOUBLE_TO_DOUBLE);
        doubleConvert.put(Double.class, DOUBLE_TO_DOUBLE);
        doubleConvert.put(Float.TYPE, DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.TYPE, DOUBLE_TO_LONG);
        doubleConvert.put(Integer.TYPE, DOUBLE_TO_INT);
        doubleConvert.put(Short.TYPE, DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.TYPE, DOUBLE_TO_BYTE);
        doubleConvert.put(Float.class, DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.class, DOUBLE_TO_LONG);
        doubleConvert.put(Integer.class, DOUBLE_TO_INT);
        doubleConvert.put(Short.class, DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.class, DOUBLE_TO_BYTE);
        DOUBLE_CONVERT_PRIMITIVE = Collections.unmodifiableMap(doubleConvert);

        doubleConvert.clear();
        doubleConvert.put(Double.TYPE, ARRAY_DOUBLE_TO_DOUBLE);
        doubleConvert.put(Double.class, ARRAY_DOUBLE_TO_DOUBLE);
        doubleConvert.put(Float.TYPE, ARRAY_DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.TYPE, ARRAY_DOUBLE_TO_LONG);
        doubleConvert.put(Integer.TYPE, ARRAY_DOUBLE_TO_INT);
        doubleConvert.put(Short.TYPE, ARRAY_DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.TYPE, ARRAY_DOUBLE_TO_BYTE);
        doubleConvert.put(Float.class, ARRAY_DOUBLE_TO_FLOAT);
        doubleConvert.put(Long.class, ARRAY_DOUBLE_TO_LONG);
        doubleConvert.put(Integer.class, ARRAY_DOUBLE_TO_INT);
        doubleConvert.put(Short.class, ARRAY_DOUBLE_TO_SHORT);
        doubleConvert.put(Byte.class, ARRAY_DOUBLE_TO_BYTE);

        DOUBLE_ARRAY_CONVERT_PRIMITIVE = Collections.unmodifiableMap(doubleConvert);
    }

    public static byte double2TypePriority(Class<?> target) {
        if (target == null) return NOT_MATCH;
        return DOUBLE_CONVERT_PRIMITIVE.getOrDefault(target, NOT_MATCH);
    }

    public static byte arrayPriority(Class<?> componentType) {
        if (componentType == null) return NOT_MATCH;
        Byte b = DOUBLE_ARRAY_CONVERT_PRIMITIVE.get(componentType);
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

        Result<?, ? extends Exception> result;
        if (componentType.isPrimitive()) {
            result = luaObj.asPrimitiveArray(toType);
        } else {
            result = luaObj.asArray(componentType).justCast();
        }
        if (result.isError()) {
            return NOT_MATCH;
        } else if (overwrite != null) {
            overwrite.setParam(result.getValue());
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
}
