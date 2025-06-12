package org.eu.smileyik.luajava.reflect;

import java.util.Arrays;
import java.util.Objects;

public class ReflectExecutableCacheKey {
    private final Class<?> clazz;
    private final String methodName;
    private final Class<?>[] parameterTypes;

    public ReflectExecutableCacheKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
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
        ReflectExecutableCacheKey cacheKey = (ReflectExecutableCacheKey) object;
        return Objects.equals(clazz, cacheKey.clazz) &&
                Objects.equals(methodName, cacheKey.methodName) &&
                Objects.deepEquals(parameterTypes, cacheKey.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, methodName, Arrays.hashCode(parameterTypes));
    }
}
