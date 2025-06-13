package org.eu.smileyik.luajava.reflect;

import org.eu.smileyik.luajava.type.LuaArray;
import org.eu.smileyik.luajava.util.ParamRef;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eu.smileyik.luajava.reflect.ConvertablePriority.*;

public class ReflectUtil {
    private static final ConcurrentMap<ReflectExecutableCacheKey, Set<Method>> CACHED_METHODS = new ConcurrentHashMap<>();

    // 筛选掉带 LuaArray 的, LuaArray 都不给带缓存查询.
    private static final Function<Class<?>, Boolean> isAllowCache = clazz -> clazz != LuaArray.class;

    /**
     * find method(s) by gave params (cacheable version).
     * @param clazz      target class
     * @param methodName method name
     * @param params     target params instance
     * @param justFirst  just find the first at target class or find all fit methods.
     * @param skipNotPublic skip the method which is not public
     * @param skipStatic    skip the method witch is static
     * @param skipNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public static LinkedList<LuaInvokedMethod<Method>> findMethodByParams(
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean skipNotPublic,
            boolean skipStatic,
            boolean skipNotStatic
    ) {
        boolean allowCache = true;
        int paramsCount = params.length;
        Class<?>[] paramTypes = new Class<?>[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            paramTypes[i] = params[i].getClass();
            allowCache &= isAllowCache.apply(paramTypes[i]);
        }

        ReflectExecutableCacheKey cacheKey = null;
        LinkedList<LuaInvokedMethod<Method>> matchedList = new LinkedList<>();
        if (allowCache) {
            cacheKey = new ReflectExecutableCacheKey(clazz, methodName, paramTypes);
            Set<Method> cachedMethods = CACHED_METHODS.get(cacheKey);
            if (cachedMethods == null) {
                return findMethodByParams(cacheKey, clazz, methodName, params,
                        justFirst, skipNotPublic, skipStatic, skipNotStatic);
            }

            int priority = Integer.MAX_VALUE;
            ParamRef<Object> overwrite = ParamRef.wrapper();
            LuaInvokedMethod<Method> currentMethod = new LuaInvokedMethod<>();

            for (Method method : cachedMethods) {
                if (checkMethodModifiers(method,
                        skipNotPublic, skipStatic, skipNotStatic)) {
                    continue;
                }
                int currentPriority = checkMethodPriority(
                        method, currentMethod, matchedList,
                        paramsCount, params, priority, overwrite);
                if (currentPriority != NOT_MATCH) {
                    priority = currentPriority;
                    if (currentPriority == FULL_MATCH) break;
                }
            }
        }

        // 如果缓存没有就重新查找.
        if (matchedList.isEmpty()) {
            return findMethodByParams(cacheKey, clazz, methodName, params,
                    justFirst, skipNotPublic, skipStatic, skipNotStatic);
        } else if (justFirst && matchedList.size() > 1) {
            matchedList.subList(1, matchedList.size()).clear();
        }
        return matchedList;
    }

    /**
     * find method(s) by gave params.
     * @param cacheKey   cache key, nullable
     * @param clazz      target class
     * @param methodName method name
     * @param params     target params instance
     * @param justFirst  just find the first at target class or find all fit methods.
     * @param skipNotPublic skip the method which is not public
     * @param skipStatic    skip the method witch is static
     * @param skipNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public static LinkedList<LuaInvokedMethod<Method>> findMethodByParams(
            ReflectExecutableCacheKey cacheKey,
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean skipNotPublic,
            boolean skipStatic,
            boolean skipNotStatic
    ) {
        if (cacheKey != null && !CACHED_METHODS.containsKey(cacheKey)) {
            CACHED_METHODS.putIfAbsent(cacheKey, new HashSet<>());
        }

        // results.
        LinkedList<LuaInvokedMethod<Method>> matchedList = new LinkedList<>();
        int priority = Integer.MAX_VALUE;
        // temp variables.
        ParamRef<Object> overwrite = ParamRef.wrapper();
        LuaInvokedMethod<Method> currentMethod = new LuaInvokedMethod<>();

        // find methods
        Class<?> c = clazz;
        int paramsCount = params.length;
        while (c != null) {
            for (Method method : c.getDeclaredMethods()) {
                // filters
                if (!method.getName().equals(methodName) ||
                        method.getParameterCount() != paramsCount) {
                    continue;
                } else if (checkMethodModifiers(method,
                        skipNotPublic, skipStatic, skipNotStatic)) {
                    continue;
                }
                int currentPriority = checkMethodPriority(
                        method, currentMethod, matchedList,
                        paramsCount, params, priority, overwrite);
                if (currentPriority != NOT_MATCH) {
                    priority = currentPriority;
                    if (currentPriority == FULL_MATCH) break;
                }
            }
            c = c.getSuperclass();
        }
        if (cacheKey != null) {
            CACHED_METHODS.get(cacheKey)
                    .addAll(matchedList.stream()
                            .map(LuaInvokedMethod::getExecutable)
                            .collect(Collectors.toSet()));
        }
        if (justFirst && matchedList.size() > 1) {
            matchedList.subList(1, matchedList.size()).clear();
        }
        return matchedList;
    }

    /**
     * return true means need skip check this method.
     */
    private static boolean checkMethodModifiers(Method method, boolean skipNotPublic,
                                                boolean skipStatic, boolean skipNotStatic) {
        int modifiers = method.getModifiers();
        boolean isStatic = Modifier.isStatic(modifiers);
        if (skipStatic && isStatic) return true;
        if (skipNotStatic && !isStatic) return true;
        boolean isPublic = Modifier.isPublic(modifiers);
        return skipNotPublic && !isPublic;
    }

    /**
     * check method and return new priority.
     * if returned is NOT_MATCH then means priority is not changed.
     */
    private static int checkMethodPriority(
            Method method, LuaInvokedMethod<Method> currentMethod,
            List<LuaInvokedMethod<Method>> matchedList,
            int paramsCount, Object[] params,
            int priority, ParamRef<Object> overwrite

    ) {
        // calculate priority.
        int currentPriority = 0;
        Class<?>[] parameters = method.getParameterTypes();
        currentMethod.reset(method);
        for (int i = 0; i < paramsCount; i++) {
            if (currentPriority >= priority) {
                currentPriority = NOT_MATCH;
                break;
            }

            int p = isConvertableType(priority - currentPriority,
                    params[i], parameters[i], overwrite);
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
            }
        }

        return NOT_MATCH;
    }
}
