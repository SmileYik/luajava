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

import org.eu.smileyik.luajava.type.LuaArray;
import org.eu.smileyik.luajava.util.LRUCache;
import org.eu.smileyik.luajava.util.ParamRef;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eu.smileyik.luajava.reflect.ConvertablePriority.*;

public class SimpleReflectUtil implements ReflectUtil {
    private static final Object EMPTY = new Object();
    // 筛选掉带 LuaArray 的, LuaArray 都不给带缓存查询.
    private final static Function<Class<?>, Boolean> isAllowCache = clazz -> clazz != LuaArray.class;

    private final Map<ReflectExecutableCacheKey, Set<IExecutable<Method>>> cachedMethods;
    private final Map<ReflectExecutableCacheKey, Optional<IExecutable<Constructor<?>>>> cachedConstructors;

    private final Map<ReflectFieldCacheKey, IFieldAccessor> cachedFields;
    private final Map<ReflectExecutableCacheKey, Boolean> cachedExistsMethod;

    private final Map<Class<?>, Map<String, Set<Method>>> classMethods;
    private final Map<Class<?>, Map<String, Field>> classFields;

    public SimpleReflectUtil(int cacheCapacity) {
        cachedMethods = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));
        cachedConstructors = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));
        cachedFields = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));
        cachedExistsMethod = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));

        classMethods = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));
        classFields = Collections.synchronizedMap(new LRUCache<>(cacheCapacity));
    }

    /**
     * find class defined field by field name.
     *
     * @param clazz           target class
     * @param name            field name
     * @param ignoreFinal     ignore final field
     * @param ignoreStatic    ignore field
     * @param ignoreNotStatic ignore not field
     * @param ignoreNotPublic ignore not public field
     * @return the target filed
     */
    public IFieldAccessor findFieldByName(Class<?> clazz, String name,
                                          boolean ignoreFinal,
                                          boolean ignoreStatic,
                                          boolean ignoreNotStatic,
                                          boolean ignoreNotPublic) {
        ReflectFieldCacheKey cacheKey = new ReflectFieldCacheKey(
                clazz, name, ignoreFinal, ignoreStatic, ignoreNotStatic, ignoreNotPublic);
        if (cachedFields.containsKey(cacheKey)) {
            return cachedFields.get(cacheKey);
        }

        Object target = ReflectUtil.foreachClass(clazz, false, currentClass -> {
            Field field = findFieldsByName(clazz, name);
            if (field != null) {
                int modifiers = field.getModifiers();
                if (ignoreFinal && Modifier.isFinal(modifiers)) return EMPTY;
                else if (ignoreStatic && Modifier.isStatic(modifiers)) return EMPTY;
                else if (ignoreNotStatic && !Modifier.isStatic(modifiers)) return EMPTY;
                else if (ignoreNotPublic && !Modifier.isPublic(modifiers)) return EMPTY;
                return field;
            }
            return null;
        });

        if (target != null && target != EMPTY) {
            Field field = (Field) target;
            field.setAccessible(true);
            ReflectField reflectField = new ReflectField((Field) target);
            cachedFields.putIfAbsent(cacheKey, reflectField);
            return reflectField;
        }

        return null;
    }

    /**
     * find constructor by params.
     * @param clazz             target class
     * @param params            params
     * @param ignoreNotPublic   ignore not public constructor
     * @param ignoreStatic      ..
     * @param ignoreNotStatic   ..
     * @return result. my be null
     */
    public LuaInvokedMethod<IExecutable<Constructor<?>>> findConstructorByParams(
            Class<?> clazz, Object[] params, boolean ignoreNotPublic,
            boolean ignoreStatic, boolean ignoreNotStatic
    ) {
        // build params
        boolean allowCache = true;
        int paramsCount = params == null ? 0 : params.length;
        Class<?>[] paramTypes = new Class<?>[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            paramTypes[i] = params[i] == null ? null : params[i].getClass();
            if (allowCache) allowCache = isAllowCache.apply(paramTypes[i]);
        }

        ReflectExecutableCacheKey cacheKey = null;
        if (allowCache) {
            cacheKey = new ReflectExecutableCacheKey(clazz, null,
                    paramTypes, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
            Optional<IExecutable<Constructor<?>>> result = cachedConstructors.get(cacheKey);
            if (result != null) {
                if (result.isPresent()) {
                    LuaInvokedMethod<IExecutable<Constructor<?>>> currentConst = new LuaInvokedMethod<>();
                    currentConst.reset(result.get());
                    Class<?>[] parameterTypes = currentConst.getExecutable().getExecutable().getParameterTypes();
                    ParamRef<Object> overwrite = ParamRef.wrapper();
                    for (int i = 0; i < paramsCount; i++) {
                        isConvertableType(Integer.MAX_VALUE, params[i], parameterTypes[i], overwrite);
                        if (!overwrite.isEmpty()) {
                            currentConst.overwriteParam(i, overwrite.getParamAndClear());
                        }
                    }

                    return currentConst;
                }
                return null;
            }
        }

        int priority = Integer.MAX_VALUE;
        ParamRef<Object> overwrite = ParamRef.wrapper();
        ReflectConstructor wrapper = new ReflectConstructor();
        LuaInvokedMethod<IExecutable<Constructor<?>>> currentConst = new LuaInvokedMethod<>();
        LinkedList<LuaInvokedMethod<IExecutable<Constructor<?>>>> matchedList = new LinkedList<>();
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != paramsCount) continue;
            if (ReflectUtil.checkExecutableModifiers(constructor, ignoreNotPublic, ignoreStatic, ignoreNotStatic))
                continue;

            wrapper.setConstructor(constructor);
            int p = ReflectUtil.checkMethodPriority(constructor, wrapper,
                    currentConst, matchedList, paramsCount, params, priority, overwrite);
            if (p != NOT_MATCH && (priority = p) == FULL_MATCH) {
                break;
            }
        }

        LuaInvokedMethod<IExecutable<Constructor<?>>> target = null;
        if (!matchedList.isEmpty()) {
            target = matchedList.getFirst();
        }
        if (allowCache) {
            cachedConstructors.put(cacheKey, Optional.ofNullable(target == null ? null : target.getExecutable()));
        }
        return target;
    }

    /**
     * find method(s) by gave params (cacheable version).
     * @param clazz      target class
     * @param methodName method name
     * @param params     target params instance
     * @param justFirst  just find the first at target class or find all fit methods.
     * @param ignoreNotPublic skip the method which is not public
     * @param ignoreStatic    skip the method witch is
     * @param ignoreNotStatic skip the method witch is not
     * @return list of suitable methods.
     */
    public LinkedList<LuaInvokedMethod<IExecutable<Method>>> findMethodByParams(
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    ) {
        // build params
        boolean allowCache = true;
        int paramsCount = params.length;
        Class<?>[] paramTypes = new Class<?>[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            paramTypes[i] = params[i] == null ? null : params[i].getClass();
            if (allowCache) allowCache = isAllowCache.apply(paramTypes[i]);
        }

        // find cache
        ReflectExecutableCacheKey cacheKey = null;
        LinkedList<LuaInvokedMethod<IExecutable<Method>>> matchedList = new LinkedList<>();
        if (allowCache) {
            cacheKey = new ReflectExecutableCacheKey(clazz, methodName, paramTypes);
            Set<IExecutable<Method>> cachedMethods = this.cachedMethods.get(cacheKey);
            if (cachedMethods == null) {
                return findMethodByParams(cacheKey, clazz, methodName, params,
                        justFirst, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
            }

            int priority = Integer.MAX_VALUE;
            ParamRef<Object> overwrite = ParamRef.wrapper();
            LuaInvokedMethod<IExecutable<Method>> currentMethod = new LuaInvokedMethod<>();
            for (IExecutable<Method> methodWrapper : cachedMethods) {
                Method method = methodWrapper.getExecutable();
                if (ReflectUtil.checkExecutableModifiers(method, ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
                    continue;
                }
                int currentPriority = ReflectUtil.checkMethodPriority(method, methodWrapper,
                        currentMethod, matchedList, paramsCount, params, priority, overwrite);
                if (currentPriority != NOT_MATCH && (priority = currentPriority) == FULL_MATCH) {
                    break;
                }
            }
        }

        // 如果缓存没有就重新查找.
        if (matchedList.isEmpty()) {
            return findMethodByParams(cacheKey, clazz, methodName, params,
                    justFirst, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
        } else if (justFirst && matchedList.size() > 1) {
            matchedList.subList(1, matchedList.size()).clear();
        }

        return matchedList;
    }

    /**
     * find exists method by name.
     * @param clazz             target class
     * @param name              method name
     * @param ignoreNotPublic   ignore not public method
     * @param ignoreStatic      ignore method
     * @param ignoreNotStatic   ignore not method
     * @return true or false.
     */
    public boolean existsMethodByName(
            Class<?> clazz, String name,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    ) {
        ReflectExecutableCacheKey cacheKey = new ReflectExecutableCacheKey(
                clazz, name, null, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
        Boolean result = cachedExistsMethod.getOrDefault(cacheKey, null);
        if (result != null) {
            return result;
        }

        result = null != ReflectUtil.foreachClass(clazz, true, currentClass -> {
            for (Method method : findMethodsByName(currentClass, name)) {
                if (!ReflectUtil.checkExecutableModifiers(method, ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
                    return true;
                }
            }
            return null;
        });

        cachedExistsMethod.putIfAbsent(cacheKey, result);
        return result;
    }

    /**
     * find method(s) by gave params.
     * @param cacheKey   cache key, nullable
     * @param clazz      target class
     * @param methodName method name
     * @param params     target params instance
     * @param justFirst  just find the first at target class or find all fit methods.
     * @param ignoreNotPublic skip the method which is not public
     * @param ignoreStatic    skip the method witch is
     * @param ignoreNotStatic skip the method witch is not
     * @return list of suitable methods.
     */
    public LinkedList<LuaInvokedMethod<IExecutable<Method>>> findMethodByParams(
            ReflectExecutableCacheKey cacheKey,
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    ) {
        if (cacheKey != null) {
            cachedMethods.computeIfAbsent(cacheKey, it -> new HashSet<>());
        }

        // temp variables.
        ParamRef<Object> overwrite = ParamRef.wrapper();
        LuaInvokedMethod<IExecutable<Method>> currentMethod = new LuaInvokedMethod<>();
        Set<ReflectExecutableCacheKey> visited = new HashSet<>();
        ReflectMethod wrapper = new ReflectMethod();
        int paramsCount = params.length;
        // results.
        LinkedList<LuaInvokedMethod<IExecutable<Method>>> matchedList = new LinkedList<>();
        ReflectUtil.foreachClass(clazz, true, new Function<Class<?>, Boolean>() {
            int priority = Integer.MAX_VALUE;
            @Override
            public Boolean apply(Class<?> currentClass) {
                for (Method method : findMethodsByName(currentClass, methodName)) {
                    // filters
                    if (method.getParameterCount() != paramsCount ||
                            ReflectUtil.checkExecutableModifiers(method, ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
                        continue;
                    }
                    ReflectExecutableCacheKey check = new ReflectExecutableCacheKey(null, methodName, method.getParameterTypes());
                    if (visited.contains(check)) continue;

                    wrapper.setMethod(method);
                    int currentPriority = ReflectUtil.checkMethodPriority(method, wrapper,
                            currentMethod, matchedList, paramsCount, params, priority, overwrite);
                    if (currentPriority != NOT_MATCH) {
                        visited.add(check);
                        if ((priority = currentPriority) == FULL_MATCH) return true;
                    }
                }
                return null;
            }
        });

        if (cacheKey != null) {
            cachedMethods.get(cacheKey)
                    .addAll(matchedList.stream()
                            .map(LuaInvokedMethod::getExecutable)
                            .collect(Collectors.toSet()));
        }
        if (justFirst && matchedList.size() > 1) {
            matchedList.subList(1, matchedList.size()).clear();
        }
        return matchedList;
    }

    protected Set<Method> findMethodsByName(Class<?> clazz, String name) {
        Map<String, Set<Method>> map = classMethods.get(clazz);
        if (map != null) {
            return map.getOrDefault(name, Collections.emptySet());
        }

        Map<String, Set<Method>> methods = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methods.computeIfAbsent(method.getName(), k -> new HashSet<>())
                    .add(method);
        }
        classMethods.put(clazz, methods);
        return methods.getOrDefault(name, Collections.emptySet());
    }

    protected Field findFieldsByName(Class<?> clazz, String name) {
        Map<String, Field> map = classFields.get(clazz);
        if (map != null) {
            return map.getOrDefault(name, null);
        }

        Map<String, Field> fields = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            fields.put(field.getName(), field);
        }
        classFields.put(clazz, fields);
        return fields.getOrDefault(name, null);
    }
}
