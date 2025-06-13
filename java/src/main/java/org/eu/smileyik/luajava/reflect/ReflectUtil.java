package org.eu.smileyik.luajava.reflect;

import org.eu.smileyik.luajava.type.LuaArray;
import org.eu.smileyik.luajava.util.ParamRef;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eu.smileyik.luajava.reflect.ConvertablePriority.*;

public class ReflectUtil {
    private static final Object EMPTY = new Object();

    private static final ConcurrentMap<ReflectExecutableCacheKey, Set<Method>> CACHED_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ReflectExecutableCacheKey, Optional<Constructor<?>>> CACHED_CONSTRUCTOR = new ConcurrentHashMap<>();

    private static final ConcurrentMap<ReflectFieldCacheKey, Field> CACHED_FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ReflectExecutableCacheKey, Boolean> CACHED_EXISTS_METHOD = new ConcurrentHashMap<>();

    // 筛选掉带 LuaArray 的, LuaArray 都不给带缓存查询.
    private static final Function<Class<?>, Boolean> isAllowCache = clazz -> clazz != LuaArray.class;

    /**
     * find class defined field by field name.
     * @param clazz             target class
     * @param name              field name
     * @param ignoreFinal       ignore final field
     * @param ignoreStatic      ignore static field
     * @param ignoreNotStatic   ignore not static field
     * @param ignoreNotPublic   ignore not public field
     * @return the target filed
     */
    public static Field findFieldByName(Class<?> clazz, String name,
                                        boolean ignoreFinal,
                                        boolean ignoreStatic,
                                        boolean ignoreNotStatic,
                                        boolean ignoreNotPublic) {
        ReflectFieldCacheKey cacheKey = new ReflectFieldCacheKey(
                clazz, name, ignoreFinal, ignoreStatic, ignoreNotStatic, ignoreNotPublic);
        Field f = null;
        if (CACHED_FIELDS.containsKey(cacheKey)) {
            return CACHED_FIELDS.get(cacheKey);
        }

//        Class<?> c = clazz;
//        while (f == null && c != null) {
//            for (Field field : c.getDeclaredFields()) {
//                if (!field.getName().equals(name)) {
//                    continue;
//                }
//                int modifiers = field.getModifiers();
//                if (ignoreFinal && Modifier.isFinal(modifiers)) continue;
//                if (ignoreStatic && Modifier.isStatic(modifiers)) continue;
//                if (ignoreNotStatic && !Modifier.isStatic(modifiers)) continue;
//                if (ignoreNotPublic && !Modifier.isPublic(modifiers)) continue;
//
//                f = field;
//                break;
//            }
//            c = c.getSuperclass();
//        }

        try {
            f = clazz.getDeclaredField(name);
            int modifiers = f.getModifiers();
            if (ignoreFinal && Modifier.isFinal(modifiers)) f = null;
            else if (ignoreStatic && Modifier.isStatic(modifiers)) f = null;
            else if (ignoreNotStatic && !Modifier.isStatic(modifiers)) f = null;
            else if (ignoreNotPublic && !Modifier.isPublic(modifiers)) f = null;
        } catch (NoSuchFieldException ignore) {

        }

        if (f != null) CACHED_FIELDS.putIfAbsent(cacheKey, f);
        return f;
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
    public static LuaInvokedMethod<Constructor<?>> findConstructorByParams(
            Class<?> clazz, Object[] params, boolean ignoreNotPublic,
            boolean ignoreStatic, boolean ignoreNotStatic
    ) {
        boolean allowCache = true;
        int paramsCount = params == null ? 0 : params.length;
        Class<?>[] paramTypes = new Class<?>[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            paramTypes[i] = params[i].getClass();
            allowCache &= isAllowCache.apply(paramTypes[i]);
        }
        ReflectExecutableCacheKey cacheKey = null;
        if (allowCache) {
            cacheKey = new ReflectExecutableCacheKey(clazz, null,
                    paramTypes, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
            Optional<Constructor<?>> result = CACHED_CONSTRUCTOR.get(cacheKey);
            if (result != null) {
                if (result.isPresent()) {
                    LuaInvokedMethod<Constructor<?>> currentConst = new LuaInvokedMethod<>();
                    currentConst.reset(result.get());
                    Class<?>[] parameterTypes = currentConst.getExecutable().getParameterTypes();
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
        LuaInvokedMethod<Constructor<?>> currentConst = new LuaInvokedMethod<>();
        LinkedList<LuaInvokedMethod<Constructor<?>>> matchedList = new LinkedList<>();
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != paramsCount) continue;
            if (checkExecutableModifiers(constructor, ignoreNotPublic, ignoreStatic, ignoreNotStatic)) continue;

            int p = checkMethodPriority(constructor,
                    currentConst, matchedList, paramsCount, params, priority, overwrite);
            if (p != NOT_MATCH) {
                priority = p;
                if (p == FULL_MATCH) break;
            }
        }

        LuaInvokedMethod<Constructor<?>> target = null;
        if (!matchedList.isEmpty()) {
            target = matchedList.getFirst();
        }
        if (allowCache) {
            CACHED_CONSTRUCTOR.put(cacheKey, Optional.ofNullable(target == null ? null : target.getExecutable()));
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
     * @param ignoreStatic    skip the method witch is static
     * @param ignoreNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public static LinkedList<LuaInvokedMethod<Method>> findMethodByParams(
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
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
                        justFirst, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
            }

            int priority = Integer.MAX_VALUE;
            ParamRef<Object> overwrite = ParamRef.wrapper();
            LuaInvokedMethod<Method> currentMethod = new LuaInvokedMethod<>();

            for (Method method : cachedMethods) {
                if (checkExecutableModifiers(method,
                        ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
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
     * @param ignoreStatic      ignore static method
     * @param ignoreNotStatic   ignore not static method
     * @return true or false.
     */
    public static boolean existsMethodByName(
            Class<?> clazz, String name,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
    ) {
        ReflectExecutableCacheKey cacheKey = new ReflectExecutableCacheKey(
                clazz, name, null, ignoreNotPublic, ignoreStatic, ignoreNotStatic);
        Boolean result = CACHED_EXISTS_METHOD.getOrDefault(cacheKey, null);
        if (result != null) {
            return result;
        }

        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    if (!checkExecutableModifiers(method, ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
                        CACHED_EXISTS_METHOD.putIfAbsent(cacheKey, true);
                        return true;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        CACHED_EXISTS_METHOD.putIfAbsent(cacheKey, false);
        return false;
    }

    /**
     * find method(s) by gave params.
     * @param cacheKey   cache key, nullable
     * @param clazz      target class
     * @param methodName method name
     * @param params     target params instance
     * @param justFirst  just find the first at target class or find all fit methods.
     * @param ignoreNotPublic skip the method which is not public
     * @param ignoreStatic    skip the method witch is static
     * @param ignoreNotStatic skip the method witch is not static
     * @return list of suitable methods.
     */
    public static LinkedList<LuaInvokedMethod<Method>> findMethodByParams(
            ReflectExecutableCacheKey cacheKey,
            Class<?> clazz, String methodName,
            Object[] params, boolean justFirst,
            boolean ignoreNotPublic,
            boolean ignoreStatic,
            boolean ignoreNotStatic
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
        Set<ReflectExecutableCacheKey> checkedMethods = new HashSet<>();

        // find methods
        Class<?> c = clazz;
        int paramsCount = params.length;
        while (c != null) {
            for (Method method : c.getDeclaredMethods()) {
                // filters
                if (!method.getName().equals(methodName) ||
                        method.getParameterCount() != paramsCount) {
                    continue;
                } else if (checkExecutableModifiers(method,
                        ignoreNotPublic, ignoreStatic, ignoreNotStatic)) {
                    continue;
                }
                ReflectExecutableCacheKey check = new ReflectExecutableCacheKey(null, methodName, method.getParameterTypes());
                if (checkedMethods.contains(check)) continue;
                int currentPriority = checkMethodPriority(
                        method, currentMethod, matchedList,
                        paramsCount, params, priority, overwrite);
                if (currentPriority != NOT_MATCH) {
                    priority = currentPriority;
                    checkedMethods.add(check);
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
    private static boolean checkExecutableModifiers(Executable method, boolean ignoreNotPublic,
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
    private static <T extends Executable> int checkMethodPriority(
            T method, LuaInvokedMethod<T> currentMethod,
            List<LuaInvokedMethod<T>> matchedList,
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
                return currentPriority;
            }
        }

        return NOT_MATCH;
    }
}
