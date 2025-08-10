/*
 * ReflectExecutableCacheKey.java, SmileYik, 2025-8-10
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
