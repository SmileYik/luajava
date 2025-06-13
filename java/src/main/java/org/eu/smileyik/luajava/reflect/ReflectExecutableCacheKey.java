package org.eu.smileyik.luajava.reflect;

import java.util.Arrays;
import java.util.Objects;

public class ReflectExecutableCacheKey {
    private final Class<?> clazz;
    private final String methodName;
    private final Class<?>[] parameterTypes;

    private final boolean ignoreNotPublic;
    private final boolean ignoreStatic;
    private final boolean ignoreNotStatic;

    public ReflectExecutableCacheKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes,
                                     boolean ignoreNotPublic, boolean ignoreStatic, boolean ignoreNotStatic) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.ignoreNotPublic = ignoreNotPublic;
        this.ignoreStatic = ignoreStatic;
        this.ignoreNotStatic = ignoreNotStatic;
    }

    public ReflectExecutableCacheKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;

        this.ignoreNotPublic = false;
        this.ignoreStatic = false;
        this.ignoreNotStatic = false;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ReflectExecutableCacheKey that = (ReflectExecutableCacheKey) object;
        return ignoreNotPublic == that.ignoreNotPublic &&
                ignoreStatic == that.ignoreStatic &&
                ignoreNotStatic == that.ignoreNotStatic &&
                Objects.equals(clazz, that.clazz) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.deepEquals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, methodName,
                Arrays.hashCode(parameterTypes), ignoreNotPublic, ignoreStatic, ignoreNotStatic);
    }
}
