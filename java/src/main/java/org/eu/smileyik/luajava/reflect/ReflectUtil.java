/*
 * ReflectUtil.java, SmileYik, 2025-8-10
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

import org.eu.smileyik.luajava.util.ParamRef;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

import static org.eu.smileyik.luajava.reflect.ConvertablePriority.NOT_MATCH;
import static org.eu.smileyik.luajava.reflect.ConvertablePriority.isConvertableType;

public interface ReflectUtil {
    /**
     * find class defined field by field name.
     *
     * @param clazz           target class
     * @param name            field name
     * @param ignoreFinal     ignore final field
     * @param ignoreStatic    ignore static field
     * @param ignoreNotStatic ignore not static field
     * @param ignoreNotPublic ignore not public field
     * @return the target filed
     */
    public IFieldAccessor findFieldByName(Class<?> clazz, String name,
                                          boolean ignoreFinal,
                                          boolean ignoreStatic,
                                          boolean ignoreNotStatic,
                                          boolean ignoreNotPublic);

    /**
     * find constructor by params.
     *
     * @param clazz           target class
     * @param params          params
     * @param ignoreNotPublic ignore not public constructor
     * @param ignoreStatic    ..
     * @param ignoreNotStatic ..
     * @return result. may be null
     */
    public LuaInvokedMethod<IExecutable<Constructor<?>>> findConstructorByParams(
            Class<?> clazz, Object[] params, boolean ignoreNotPublic,
            boolean ignoreStatic, boolean ignoreNotStatic
    );

    /**
     * find method(s) by gave params (cacheable version).
     *
     * @param clazz           target class
     * @param methodName      method name
     * @param params          target params instance
     * @param justFirst       just find the first at target class or find all fit methods.
     * @param ignoreNotPublic skip the method which is not public
     * @param ignoreStatic    skip the method witch is static
     * @param ignoreNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public LinkedList<LuaInvokedMethod<IExecutable<Method>>> findMethodByParams(
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    );

    /**
     * find exists method by name.
     *
     * @param clazz           target class
     * @param name            method name
     * @param ignoreNotPublic ignore not public method
     * @param ignoreStatic    ignore static method
     * @param ignoreNotStatic ignore not static method
     * @return true or false.
     */
    public boolean existsMethodByName(
            Class<?> clazz, String name,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    );

    /**
     * find method(s) by gave params.
     *
     * @param cacheKey        cache key, nullable
     * @param clazz           target class
     * @param methodName      method name
     * @param params          target params instance
     * @param justFirst       just find the first at target class or find all fit methods.
     * @param ignoreNotPublic skip the method which is not public
     * @param ignoreStatic    skip the method witch is static
     * @param ignoreNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public LinkedList<LuaInvokedMethod<IExecutable<Method>>> findMethodByParams(
            ReflectExecutableCacheKey cacheKey,
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    );

    public static void findSuperclasses(LinkedList<Class<?>> queue, Class<?> clazz) {
        if (clazz.getSuperclass() != null) {
            queue.addLast(clazz.getSuperclass());
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> i : interfaces) {
            if (i != null) {
                queue.addLast(i);
            }
        }
    }

    /**
     * return true means need skip check this method.
     */
    public static boolean checkExecutableModifiers(Executable method, boolean ignoreNotPublic,
                                            boolean ignoreStatic, boolean ignoreNotStatic) {
        int modifiers = method.getModifiers();
        boolean isStatic = Modifier.isStatic(modifiers);
        if (ignoreStatic && isStatic) return true;
        if (ignoreNotStatic && !isStatic) return true;
        boolean isPublic = Modifier.isPublic(modifiers);
        return ignoreNotPublic && !isPublic;
    }

    /**
     * check method and return new priority.
     * if returned is NOT_MATCH then means priority is not changed.
     */
    public static <T extends Executable> int checkMethodPriority(
            T method,
            IExecutable<T> wrapperedMethod,
            LuaInvokedMethod<IExecutable<T>> currentMethod,
            List<LuaInvokedMethod<IExecutable<T>>> matchedList,
            int paramsCount, Object[] params,
            int priority, ParamRef<Object> overwrite

    ) {
        // calculate priority.
        int currentPriority = 0;
        Class<?>[] parameters = method.getParameterTypes();
        currentMethod.reset(wrapperedMethod);
        for (int i = 0; i < paramsCount; i++) {
            if (currentPriority >= priority) {
                currentPriority = NOT_MATCH;
                break;
            }

            int p = isConvertableType(priority - currentPriority, params[i], parameters[i], overwrite);
            if (p == NOT_MATCH) {
                currentPriority = NOT_MATCH;
                overwrite.clear();
                break;
            }
            currentPriority += p;
            if (!overwrite.isEmpty()) {
                currentMethod.overwriteParam(i, overwrite.getParamAndClear());
            }
        }

        if (currentPriority != NOT_MATCH) {
            if (currentPriority < priority) {
                matchedList.clear();
                matchedList.add(currentMethod.done());
                return currentPriority;
            } else if (currentPriority == priority) {
                matchedList.add(currentMethod.done());
                return currentPriority;
            }
        }

        return NOT_MATCH;
    }

    public static <T> T foreachClass(Class<?> clazz, boolean foreachInterface,
                                     Function<Class<?>, T> function) {
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> stack = new LinkedList<>();
        stack.add(clazz);

        while (!stack.isEmpty()) {
            clazz = stack.pop();
            T apply = function.apply(clazz);
            if (apply != null) {
                return apply;
            }

            if (foreachInterface) {
                for (Class<?> i : clazz.getInterfaces()) {
                    if (visited.add(i)) {
                        stack.push(i);
                    }
                }
            }
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && visited.add(superclass)) {
                stack.push(superclass);
            }
        }
        return null;
    }
}
