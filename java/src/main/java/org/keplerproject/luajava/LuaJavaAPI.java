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

package org.keplerproject.luajava;

import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.reflect.ConvertablePriority;
import org.eu.smileyik.luajava.reflect.LuaInvokedMethod;
import org.eu.smileyik.luajava.reflect.ReflectUtil;
import org.eu.smileyik.luajava.util.*;

import java.lang.reflect.*;
import java.util.LinkedList;

/**
 * Class that contains functions accessed by lua.
 *
 * @author Thiago Ponte
 */
public final class LuaJavaAPI {
    private static final String METATABLE_KEY_ITERATOR = "__JavaIterator";
    private LuaJavaAPI() {
    }

    public static int objectIter(int luaState) throws LuaException {
        LuaStateFacade facade = LuaStateFactory.getExistingState(luaState);
        return facade.lockThrow(l -> {
            if (!l.getMetaTable(1) || !l.isTable(-1)) {
                throw new LuaException("This object has no meta table");
            }
            l.pushString(METATABLE_KEY_ITERATOR);
            l.rawGet(-2);
            IndexableIterator<?> iterator = null;
            if (l.isObject(-1)) {
                iterator = (IndexableIterator<?>) facade.rawToJavaObject(-1).getOrThrow(LuaException.class);
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
        }).getOrThrow(LuaException.class);
    }

    /**
     * concat object to string, at lease one string type.
     * @param luaState lua state
     * @return number of returned objects.
     * @throws LuaException
     */
    public static int objectConcat(int luaState) throws LuaException {
        LuaStateFacade facade = LuaStateFactory.getExistingState(luaState);
        facade.lockThrow(l -> {
            Object a = facade.rawToJavaObject(1).getOrThrow(LuaException.class);
            Object b = facade.rawToJavaObject(2).getOrThrow(LuaException.class);
            String ret = null;
            if (a instanceof String) {
                ret = (String) a + BoxedTypeHelper.toString(b);
            } else if (b instanceof String) {
                ret = BoxedTypeHelper.toString(a) + (String) b;
            } else {
                throw new LuaException("In the concat operation, at least one string type is required. left: " + a + ", right: " + b);
            }
            facade.rawPushObjectValue(ret).justThrow(LuaException.class);
        }).justThrow(LuaException.class);
        return 1;
    }

    /**
     * Java implementation of the metamethod __index for normal objects
     *
     * @param luaState   int that indicates the state used
     * @param obj        Object to be indexed
     * @param methodName the name of the method
     * @return number of returned objects
     */
    public static int objectIndex(int luaState, Object obj, String methodName)
            throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);

        return luaStateFacade.lockThrow(L -> {
            int top = L.getTop();

            Object[] objs = new Object[top - 1];
            Method method = null;

            Class<?> clazz = obj instanceof Class ? (Class<?>) obj : obj.getClass();
            method = findMethod(luaStateFacade, L, clazz, methodName, objs, top);

            // If method is null means there isn't one receiving the given arguments
            if (method == null) {
                throw new LuaException("Invalid method call. No such method.");
            }

            Object ret;
            try {
                if (Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }

                //if (obj instanceof Class)
                if (Modifier.isStatic(method.getModifiers())) {
                    ret = method.invoke(null, objs);
                } else {
                    ret = method.invoke(obj, objs);
                }
            } catch (Exception e) {
                throw new LuaException(e);
            }

            // Void function returns null
            if (ret == null) {
                return 0;
            }

            // push result
            luaStateFacade.rawPushObjectValue(ret).justThrow(LuaException.class);

            return 1;
        }).getOrThrow(LuaException.class);
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
        if (Array.getLength(obj) < index)
            throw new LuaException("Index out of bounds.");

        luaStateFacade.pushObjectValue(Array.get(obj, index - 1)).justThrow(LuaException.class);
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
    public static int classIndex(int luaState, Class<?> clazz, String searchName)
            throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return luaStateFacade.lockThrow(L -> {
            int res;

            res = checkField(luaState, clazz, searchName);

            if (res != 0) {
                return 1;
            }

            if (checkMethod(luaState, clazz, searchName)) {
                return 2;
            }

            return 0;
        }).getOrThrow(LuaException.class);
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
    public static int objectNewIndex(int luaState, Object obj, String fieldName)
            throws LuaException {
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        // like a.b = 1
        Class<?> targetClass = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        Field field = ReflectUtil.findFieldByName(targetClass, fieldName,
                false, false, obj == targetClass, luaStateFacade.isIgnoreNotPublic());
        if (field == null) {
            throw new LuaException("Error accessing field " + fieldName);
        }
        if (!field.isAccessible()) field.setAccessible(true);

        luaStateFacade.lockThrow(L -> {
            Class<?> type = field.getType();
            Result<Object, Object> setObjRet = compareTypes(luaStateFacade, L, type, 3);
            if (setObjRet.isError()) {
                throw new LuaException("Invalid type.");
            }
            try {
                field.set(obj, setObjRet.getValue());
            } catch (IllegalArgumentException e) {
                throw new LuaException("Ilegal argument to set field.", e);
            } catch (IllegalAccessException e) {
                throw new LuaException("Field not accessible.", e);
            }
        }).justThrow(LuaException.class);
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

        luaStateFacade.lockThrow(L -> {
            Result<Object, Object> result = compareTypes(luaStateFacade, L, type, 3);
            if (result.isError()) {
                throw new LuaException("Invalid type.");
            }
            Object setObj = result.getValue();
            Array.set(obj, index - 1, setObj);
        }).justThrow(LuaException.class);

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
    public static int javaNewInstance(int luaState, String className)
            throws LuaException {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new LuaException(e);
        }

        return javaNew(luaState, clazz);
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

        luaStateFacade.lockThrow(L -> {
            Object ret = getObjInstance(luaStateFacade, L, clazz);
            luaStateFacade.rawPushObjectValue(ret);
        }).justThrow(LuaException.class);
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
    public static int javaLoadLib(int luaState, String className, String methodName)
            throws LuaException {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new LuaException(e);
        }

        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return luaStateFacade.lockThrow(L -> {
            try {
                Method mt = clazz.getMethod(methodName, LuaState.class);
                Object obj = mt.invoke(null, L);

                if (obj instanceof Integer) {
                    return (Integer) obj;
                } else
                    return 0;
            } catch (Exception e) {
                throw new LuaException("Error on calling method. Library could not be loaded. " + e.getMessage());
            }
        }).getOrThrow(LuaException.class);
    }

    private static Object getObjInstance(LuaStateFacade luaStateFacade, LuaState L, Class<?> clazz)
            throws LuaException {
        int top = L.getTop();

        int paramsCount = top - 1;
        Object[] objs = new Object[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            objs[i] = luaStateFacade.rawToJavaObject(i + 2).getOrThrow(LuaException.class);
        }

        LuaInvokedMethod<Constructor<?>> result = ReflectUtil.findConstructorByParams(
                clazz, objs, luaStateFacade.isIgnoreNotPublic(), false, false);

        if (result == null) {
            throw new LuaException("Couldn't instantiate java Object");
        }
        result.getOverwriteParams().forEach((idx, obj) -> objs[idx] = obj);
        Object ret;
        try {
            ret = result.getExecutable().newInstance(objs);
        } catch (Exception e) {
            throw new LuaException(e);
        }
        return ret;
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
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        Class<?> targetClass = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        Field field = ReflectUtil.findFieldByName(targetClass, fieldName,
                false, false, obj == targetClass, luaStateFacade.isIgnoreNotPublic());
        if (field == null) return 0;
        try {
            Object o = field.get(obj);
            return luaStateFacade.pushObjectValue(o).isSuccess() ? 1 : 0;
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
        Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        boolean isStatic = clazz == obj;
        LuaStateFacade luaStateFacade = LuaStateFactory.getExistingState(luaState);
        return ReflectUtil.existsMethodByName(clazz, methodName, luaStateFacade.isIgnoreNotPublic(), false, isStatic);
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

        luaStateFacade.lockThrow(L -> {
            try {
                if (!(L.isTable(2))) {
                    throw new LuaException("Parameter is not a table. Can't create proxy.");
                }
                LuaObject luaObj = luaStateFacade.getLuaObject(2).getOrThrow(LuaException.class);
                Object proxy = luaObj.createProxy(implem).getOrThrow(LuaException.class);
                L.pushJavaObject(proxy);
            } catch (Exception e) {
                throw new LuaException(e);
            }

        }).justThrow(LuaException.class);
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
        }
        if (paramRef.isEmpty()) {
            return Result.success(luaObj);
        }
        return Result.success(paramRef.getParam());
    }

    private static Method findMethod(LuaStateFacade luaStateFacade,
                                     LuaState L, Class<?> clazz,
                                     String methodName, Object[] retObjs, int top) throws LuaException {
        // Convert lua params to java params
        int paramsCount = top - 1;
        Object[] objs = new Object[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            objs[i] = luaStateFacade.rawToJavaObject(i + 2).getOrThrow(LuaException.class);
        }

        LinkedList<LuaInvokedMethod<Method>> list = ReflectUtil.findMethodByParams(
                clazz, methodName, objs, false, luaStateFacade.isIgnoreNotPublic(), false, false);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            throw new LuaException("Found multi result for method " + methodName + " in class " + clazz);
        }
        LuaInvokedMethod<Method> invokedMethod = list.getFirst();
        invokedMethod.getOverwriteParams().forEach((idx, obj) -> objs[idx] = obj);
        System.arraycopy(objs, 0, retObjs, 0, objs.length);
        return invokedMethod.getExecutable();
    }
}
 