/*
 * LuaJavaAPI.java, SmileYik, 2025-8-10
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

/*
 * $Id: LuaJavaAPI.java,v 1.5 2007-04-17 23:47:50 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.eu.smileyik.luajava;

import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.reflect.*;
import org.eu.smileyik.luajava.util.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Class that contains functions accessed by lua.
 *
 * @author Thiago Ponte
 * @author Smile Yik
 */
public final class LuaJavaAPI {
    private static ReflectUtil reflectUtil = new SimpleReflectUtil(1024);
    private static final String METATABLE_KEY_ITERATOR = "__JavaIterator";
    /**
     * if index name starts with this then ignore field check.
     */
    private static final String FORCE_ACCESS_METHOD_PREFIX = "_m_";

    private LuaJavaAPI() {
    }

    /**
     * Replace current reflect util instance.
     * @param reflectUtil Notnull
     */
    public static void setReflectUtil(ReflectUtil reflectUtil) {
        if (reflectUtil == null) {
            return;
        }
        LuaJavaAPI.reflectUtil = reflectUtil;
    }

    public static ReflectUtil getReflectUtil() {
        return reflectUtil;
    }

    public static int objectIter(int luaState) throws LuaException {
        LuaStateFacade facade = LuaStateFactory.getExistingState(luaState);
        LuaState l = facade.getLuaState();
        if (!l.getMetaTable(1) || !l.isTable(-1)) {
            throw new LuaException("This object has no meta table");
        }
        l.pushString(METATABLE_KEY_ITERATOR);
        l.rawGet(-2);
        IndexableIterator<?> iterator = null;
        if (l.isObject(-1)) {
            iterator = (IndexableIterator<?>) facade.rawToJavaObject(-1)
                    .getOrThrow(LuaException.class);
        } else {
            Object obj = facade.rawToJavaObject(1).getOrThrow(LuaException.class);
            if (obj instanceof Iterable<?>) {
                iterator = new IteratorWrapper(((Iterable<?>) obj).iterator());
            } else if (obj.getClass().isArray()) {
                iterator = new ArrayIterator(obj);
            } else {
                throw new LuaException("Cannot use pairs to " + obj.getClass());
            }
            l.getMetaTable(1);
            l.pushString(METATABLE_KEY_ITERATOR);
            facade.rawPushObjectValue(iterator).getOrThrow(LuaException.class);
            l.rawSet(-3);
        }
        l.pop(1);
        if (iterator.hasNext()) {
            Object next = iterator.next();
            l.pushInteger(iterator.getIndex());
            facade.rawPushObjectValue(next).getOrThrow(LuaException.class);
            return 2;
        } else {
            l.getMetaTable(1);
            l.pushString(METATABLE_KEY_ITERATOR);
            l.pushNil();
            l.rawSet(-3);
            l.pop(1);
            return 0;
        }
    }

    /**
     * concat object to string, at lease one string type.
     * @param luaState lua state
     * @return number of returned objects.
     * @throws LuaException
     */
    public static int objectConcat(int luaState) throws LuaException {
        LuaStateFacade facade = LuaStateFactory.getExistingState(luaState);
        Object a = facade.rawToJavaObject(1).getOrThrow(LuaException.class);
        Object b = facade.rawToJavaObject(2).getOrThrow(LuaException.class);
        String ret = null;
        if (a instanceof String) {
            ret = a + BoxedTypeHelper.toString(b);
        } else if (b instanceof String) {
            ret = BoxedTypeHelper.toString(a) + b;
        } else {
            throw new LuaException("In the concat operation, at least one string type is required. left: " + a + ", right: " + b);
        }
        facade.rawPushObjectValue(ret).justThrow(LuaException.class);
        return 1;
    }

    /**
     * Java implementation of the metamethod __index for normal objects
     *
     * @param luaState   int that indicates the state used
     * @param obj        Object to be indexed
     * @param methodName the name of the method
     * @param classIndex is class static method or object method.
     * @return number of returned objects
     */
    public static int objectIndex(int luaState, Object obj, String methodName, boolean classIndex) throws Exception {
        // remove method prefix
        if (methodName != null && methodName.startsWith(FORCE_ACCESS_METHOD_PREFIX)) {
            methodName = methodName.substring(FORCE_ACCESS_METHOD_PREFIX.length());
            if (methodName.isEmpty())  return 0;
        }

        Class<?> clazz;
        if (classIndex) {
            if (obj instanceof Class) {
                clazz = (Class<?>) obj;
            } else {
                throw new LuaException("Object is not a class: " + obj);
            }
        } else {
            clazz = obj.getClass();
        }

        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        LuaState L = luaStateFacade.getLuaState();

        int top = L.getTop();
        Object[] objs = new Object[top - 1];
        IExecutable<Method> methodWrapper = findMethod(luaStateFacade, clazz, methodName, objs, top);

        // If method is null means there isn't one receiving the given arguments
        if (methodWrapper == null) {
            throw new LuaException(String.format("Invalid method call. No such method named '%s' in class %s", methodName, clazz.getName()));
        }

        Method method = methodWrapper.getExecutable();
        Object ret = methodWrapper.invoke(Modifier.isStatic(method.getModifiers()) ? null : obj, objs);

        // Void function returns null
        if (ret == null) return 0;
        // push result
        luaStateFacade.rawPushObjectValue(ret).justThrow(LuaException.class);
        return 1;
    }

    /**
     * Java function that implements the __index for Java arrays
     *
     * @param luaState int that indicates the state used
     * @param obj      Object to be indexed
     * @param index    index number of array. Since Lua index starts from 1,
     *                 the number used will be (index - 1)
     * @return number of returned objects
     */
    public static int arrayIndex(int luaState, Object obj, int index) throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        if (!obj.getClass().isArray())
            throw new LuaException("Object indexed is not an array.");
        else if (Array.getLength(obj) < index)
            throw new LuaException("Index out of bounds.");

        luaStateFacade.rawPushObjectValue(Array.get(obj, index - 1)).justThrow(LuaException.class);
        return 1;
    }

    /**
     * Java function to be called when a java Class metamethod __index is called.
     * This function returns 1 if there is a field with searchName and 2 if there
     * is a method if the searchName
     *
     * @param luaState   int that represents the state to be used
     * @param clazz      class to be indexed
     * @param searchName name of the field or method to be accessed
     * @return number of returned objects
     * @throws LuaException
     */
    public static int classIndex(int luaState, Class<?> clazz, String searchName) throws LuaException {
        int res = checkField(luaState, clazz, searchName);
        if (res != 0) {
            return 1;
        } else if (checkClassMethod(luaState, clazz, searchName)) {
            return 2;
        }
        return 0;
    }

    /**
     * Java function to be called when a java object metamethod __newindex is called.
     *
     * @param luaState  int that represents the state to be used
     * @param obj       to be used
     * @param fieldName name of the field to be set
     * @return number of returned objects
     * @throws LuaException
     */
    public static int objectNewIndex(int luaState, Object obj, String fieldName) throws LuaException {
        // like a.b = 1
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        Class<?> targetClass = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        IFieldAccessor fieldAccessor = reflectUtil.findFieldByName(targetClass, fieldName,
                false, false, false, luaStateFacade.isIgnoreNotPublic());
        if (fieldAccessor == null) {
            throw new LuaException(String.format("Error accessing field '%s' in class %s", fieldName, targetClass.getName()));
        }
        // checkField method already checked the obj can access this field or not.
        Class<?> type = fieldAccessor.getField().getType();
        Result<Object, Object> setObjRet = compareTypes(luaStateFacade, luaStateFacade.getLuaState(), type, 3);
        if (setObjRet.isError()) {
            throw new LuaException("Invalid type.");
        }
        try {
            fieldAccessor.set(obj, setObjRet.getValue());
        } catch (IllegalAccessException e) {
            throw new LuaException("Field not accessible.", e);
        }
        return 0;
    }

    /**
     * Java function to be called when a java array metamethod __newindex is called.
     *
     * @param luaState int that represents the state to be used
     * @param obj      to be used
     * @param index    index number of array. Since Lua index starts from 1,
     *                 the number used will be (index - 1)
     * @return number of returned objects
     * @throws LuaException
     */
    public static int arrayNewIndex(int luaState, Object obj, int index)
            throws LuaException {
        if (!obj.getClass().isArray())
            throw new LuaException("Object indexed is not an array.");
        if (Array.getLength(obj) < index)
            throw new LuaException("Index out of bounds.");
        Class<?> type = obj.getClass().getComponentType();

        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        LuaState l = luaStateFacade.getLuaState();
        Result<Object, Object> result = compareTypes(luaStateFacade, l, type, 3);
        if (result.isError()) {
            throw new LuaException("Invalid type.");
        }
        Object setObj = result.getValue();
        Array.set(obj, index - 1, setObj);

        return 0;
    }

    /**
     * Pushes a new instance of a java Object of the type className
     *
     * @param luaState  int that represents the state to be used
     * @param className name of the class
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaNewInstance(int luaState, String className) throws Exception {
        return javaNew(luaState, Class.forName(className));
    }

    /**
     * javaNew returns a new instance of a given clazz
     *
     * @param luaState int that represents the state to be used
     * @param clazz    class to be instanciated
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaNew(int luaState, Class<?> clazz) throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        Object ret = getObjInstance(luaStateFacade, luaStateFacade.getLuaState(), clazz);
        luaStateFacade.rawPushObjectValue(ret);
        return 1;
    }

    /**
     * Calls the static method <code>methodName</code> in class <code>className</code>
     * that receives a LuaState as first parameter.
     *
     * @param luaState   int that represents the state to be used
     * @param className  name of the class that has the open library method
     * @param methodName method to open library
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaLoadLib(int luaState, String className, String methodName) throws Exception {
        Class<?> clazz = Class.forName(className);
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        try {
            Method mt = clazz.getMethod(methodName, LuaState.class);
            Object obj = mt.invoke(null, luaStateFacade.getLuaState());

            if (obj instanceof Integer) {
                return (Integer) obj;
            } else {
                return 0;
            }
        } catch (Exception e) {
            throw new LuaException("Error on calling method. Library could not be loaded. " + e.getMessage());
        }
    }

    /**
     * Checks if there is a field on the obj with the given name
     *
     * @param luaState  int that represents the state to be used
     * @param obj       object to be inspected
     * @param fieldName name of the field to be inpected
     * @return number of returned objects
     */
    public static int checkField(int luaState, Object obj, String fieldName) throws LuaException {
        if (obj == null) return 0;
        // if it has force access method prefix then ignore check field.
        if (fieldName == null || fieldName.startsWith(FORCE_ACCESS_METHOD_PREFIX)) return 0;

        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        Class<?> targetClass = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        boolean isStatic = targetClass == obj;
        IFieldAccessor fieldAccessor = reflectUtil.findFieldByName(targetClass, fieldName,
                false, false, isStatic, luaStateFacade.isIgnoreNotPublic());
        if (fieldAccessor == null) return 0;
        try {
            Object ret = fieldAccessor.get(obj);
            luaStateFacade.rawPushObjectValue(ret).justThrow(LuaException.class);
            return 1;
        } catch (IllegalAccessException ignore) { }
        return 0;
    }

    /**
     * Checks to see if there is a method with the given name.
     *
     * @param luaState   int that represents the state to be used
     * @param obj        object to be inspected
     * @param methodName name of the field to be inpected
     * @return return 1 then means has method named target name
     */
    public static boolean checkMethod(int luaState, Object obj, String methodName) {
        if (obj == null) return false;
        // remove method prefix
        if (methodName != null && methodName.startsWith(FORCE_ACCESS_METHOD_PREFIX)) {
            methodName = methodName.substring(FORCE_ACCESS_METHOD_PREFIX.length());
            if (methodName.isEmpty()) return false;
        }

        Class<?> clazz = obj.getClass();
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return reflectUtil.existsMethodByName(
                clazz, methodName, luaStateFacade.isIgnoreNotPublic(), false, false);
    }

    /**
     * Checks to see if there is a static method with the given name.
     *
     * @param luaState   int that represents the state to be used
     * @param obj        object to be inspected
     * @param methodName name of the field to be inpected
     * @return return 1 then means has method named target name
     */
    private static boolean checkClassMethod(int luaState, Object obj, String methodName) {
        if (obj == null) return false;

        Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return reflectUtil.existsMethodByName(
                clazz, methodName, luaStateFacade.isIgnoreNotPublic(), false, true);
    }

    /**
     * Function that creates an object proxy and pushes it into the stack
     *
     * @param luaState int that represents the state to be used
     * @param implem   interfaces implemented separated by comma (<code>,</code>)
     * @return number of returned objects
     * @throws LuaException
     */
    public static int createProxyObject(int luaState, String implem)
            throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        LuaState L = luaStateFacade.getLuaState();
        if (!(L.isTable(2))) {
            throw new LuaException("Parameter is not a table. Can't create proxy.");
        }
        luaStateFacade.getLuaObject(2)
                .thenMap(it -> it.createProxy(implem))
                .ifSuccessThen(L::pushJavaObject)
                .justThrow(LuaException.class);
        return 1;
    }

    private static Result<Object, Object> compareTypes(LuaStateFacade luaStateFacade,
                                                       LuaState L, Class<?> parameter, int idx)
            throws LuaException {
        ParamRef<Object> paramRef = ParamRef.wrapper();
        Object luaObj = luaStateFacade.rawToJavaObject(idx).getOrThrow(LuaException.class);
        byte ret = ConvertablePriority.isConvertableType(Integer.MAX_VALUE, luaObj, parameter, paramRef);
        if (ret == ConvertablePriority.NOT_MATCH) {
            return Result.failure("");
        } else if (paramRef.isEmpty()) {
            return Result.success(luaObj);
        }
        return Result.success(paramRef.getParam());
    }

    /**
     * get lua params from lua state stack
     */
    private static Object[] getLuaParams(LuaStateFacade luaStateFacade, int paramsCount) throws LuaException {
        Object[] objs = new Object[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            objs[i] = luaStateFacade.rawToJavaObject(i + 2).getOrThrow(LuaException.class);
        }
        return objs;
    }

    /**
     * find class's constructor and new instance.
     */
    private static Object getObjInstance(LuaStateFacade luaStateFacade, LuaState L, Class<?> clazz)
            throws LuaException {
        int top = L.getTop();

        int paramsCount = top - 1;
        Object[] objs = getLuaParams(luaStateFacade, paramsCount);
        LuaInvokedMethod<IExecutable<Constructor<?>>> result = reflectUtil.findConstructorByParams(
                clazz, objs, luaStateFacade.isIgnoreNotPublic(), false, false);

        if (result == null) {
            throw new LuaException("Couldn't instantiate java Object");
        }
        result.getOverwriteParams().forEach((idx, obj) -> {
            Object old = objs[idx];
            try {
                objs[idx] = obj;
            } finally {
                closeParamsObject(old);
            }
        });
        Object ret;
        try {
            ret = result.getExecutable().invoke(null, objs);
        } catch (Exception e) {
            throw new LuaException(e);
        }
        return ret;
    }

    private static IExecutable<Method> findMethod(LuaStateFacade luaStateFacade, Class<?> clazz,
                                                  String methodName, Object[] retObjs, int top) throws LuaException {
        // Convert lua params to java params
        int paramsCount = top - 1;
        Object[] objs = getLuaParams(luaStateFacade, paramsCount);

        LinkedList<LuaInvokedMethod<IExecutable<Method>>> list = reflectUtil.findMethodByParams(
                clazz, methodName, objs, luaStateFacade.isJustUseFirstMethod(),
                luaStateFacade.isIgnoreNotPublic(), false, false);

        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            throw new LuaException("Found multi result for method " + methodName + " in class " + clazz);
        }

        LuaInvokedMethod<IExecutable<Method>> invokedMethod = list.getFirst();
        invokedMethod.getOverwriteParams().forEach((idx, obj) -> {
            Object old = objs[idx];
            try {
                objs[idx] = obj;
            } finally {
                closeParamsObject(old);
            }
        });
        System.arraycopy(objs, 0, retObjs, 0, objs.length);
        return invokedMethod.getExecutable();
    }

    private static void closeParamsObject(Object obj) {
        if (obj instanceof LuaObject) {
            ((LuaObject) obj).close();
        }
    }

    private static void closeParamsObject(Object[] objs) {
        for (Object obj : objs) {
            closeParamsObject(obj);
        }
    }

    // **************** Debug API ****************

    public static void debugLuaHook(int luaState, LuaDebug luaDebug) {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        luaStateFacade.debugHook(luaDebug);
    }

    public static LuaDebug newLuaDebug(long ptr, ByteBuffer buffer,
                                       String name, String nameWhat, String what, String source) {
        return LuaDebug.newInstance(ptr, buffer, name, nameWhat, what, source);
    }

    // **************** Stream API ****************

    /**
     * dump from lua
     * @param luaState lua state
     * @param in c allocate bytebuffer, read bytes from here.
     * @param entity user data
     */
    public static void luaWrite(int luaState, ByteBuffer in, ILuaReadWriteEntity entity) throws IOException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        entity.luaWrite(luaStateFacade, in);
    }

    /**
     * load to lua
     * @param luaState
     * @param out c allocate bytebuffer, write bytes to here.
     * @param entity user data
     * @return written bytes
     */
    public static int luaRead(int luaState, ByteBuffer out, ILuaReadWriteEntity entity) throws IOException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return entity.luaRead(luaStateFacade, out);
    }

    // **************** Exception API ****************

    /**
     * This method will be called by lua state has exception.
     * @param luaState
     * @param throwable
     * @return
     */
    public static Throwable throwsByC(int luaState, Throwable throwable) {
        try {
            LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
            Throwable newOne = luaStateFacade.throwsByC(throwable);
            return newOne == null ? throwable : newOne;
        } catch (Throwable e) {
            try {
                e.printStackTrace(System.err);
            } catch (Throwable ignored) {
            }
        }
        return throwable;
    }
}
 