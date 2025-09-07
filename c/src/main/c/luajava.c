/******************************************************************************
 * luajava.c, SmileYik, 2025-8-10
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
 ******************************************************************************/

/******************************************************************************
 * $Id$
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
 ******************************************************************************/

/***************************************************************************
 *
 * $ED
 *    This module is the implementation of luajava's dynamic library.
 *    In this module lua's functions are exported to be used in java by jni,
 *    and also the functions that will be used and exported to lua so that
 *    Java Objects' functions can be called.
 *
 *****************************************************************************/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <lua.h>
#include <lauxlib.h>
#include <lualib.h>
#include "luajava.h"
#include "luajava_api.h"
#include "compatible.h"

/*
** Assumes the table is on top of the stack.
*/
static void set_info(lua_State *L) {
    lua_pushliteral(L, "_COPYRIGHT");
    lua_pushliteral(L, "Copyright (C) 2003-2007 Kepler Project\n"
                        "Copyright (c) 2025 Smile Yik");
    lua_settable(L, -3);
    lua_pushliteral(L, "_DESCRIPTION");
    lua_pushliteral(L, "LuaJava is a script tool for Java");
    lua_settable(L, -3);
    lua_pushliteral(L, "_NAME");
    lua_pushliteral(L, "LuaJava");
    lua_settable(L, -3);
    lua_pushliteral(L, "_VERSION");
#ifndef LUAJAVA_VERSION
    lua_pushliteral(L, "Unknown");
#else
    lua_pushliteral(L, LUAJAVA_VERSION);
#endif
    lua_settable(L, -3);
}

/**************************** JNI FUNCTIONS ****************************/

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _luaVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1luaVersion(
        JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, LUA_VERSION);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _luaRegistryIndex
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1luaRegistryIndex(
        JNIEnv *env, jclass clazz) {
    return (jint) LUA_REGISTRYINDEX;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _luaGlobalsIndex
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1luaGlobalsIndex(
        JNIEnv *env, jclass clazz) {
    return (jint) LUAJAVA_LUA_RIDX_GLOBALS;
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState_luajava_1open(
        JNIEnv *env, jobject jobj, jobject cptr, jint stateId) {
    
    lua_State *L;

    L = getStateFromCPtr(env, cptr);

    lua_pushstring(L, LUAJAVASTATEINDEX);
    lua_pushnumber(L, (lua_Number)stateId);
    lua_settable(L, LUA_REGISTRYINDEX);

    lua_newtable(L);

    lua_setglobal(L, "luajava");

    lua_getglobal(L, "luajava");

    set_info(L);

    lua_pushstring(L, "bindClass");
    lua_pushcfunction(L, &javaBindClass);
    lua_settable(L, -3);

    lua_pushstring(L, "class2Obj");
    lua_pushcfunction(L, &javaClass2Obj);
    lua_settable(L, -3);

    lua_pushstring(L, "new");
    lua_pushcfunction(L, &javaNew);
    lua_settable(L, -3);

    lua_pushstring(L, "newInstance");
    lua_pushcfunction(L, &javaNewInstance);
    lua_settable(L, -3);

    lua_pushstring(L, "loadLib");
    lua_pushcfunction(L, &javaLoadLib);
    lua_settable(L, -3);

    lua_pushstring(L, "createProxy");
    lua_pushcfunction(L, &createProxy);
    lua_settable(L, -3);

    lua_pushstring(L, "env");
    lua_pushcfunction(L, &getJNIEnv);
    lua_settable(L, -3);

    lua_pop(L, 1);
    pushJNIEnv(env, L);
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT jobject JNICALL
Java_org_eu_smileyik_luajava_LuaState__1getObjectFromUserdata(
    JNIEnv *env,
    jobject jobj,
    jobject cptr,
    jint index
) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);
    jobject *obj;

    if (!isJavaObject(L, index)) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Index is not a java object");
        return NULL;
    }

    obj = (jobject *)lua_touserdata(L, index);

    return *obj;
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT jboolean JNICALL Java_org_eu_smileyik_luajava_LuaState__1isObject(
        JNIEnv *env, jobject jobj, jobject cptr, jint index) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);

    return (isJavaObject(L, index) ? JNI_TRUE : JNI_FALSE);
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushJavaObject(
        JNIEnv *env, jobject jobj, jobject cptr, jobject obj) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);

    pushJavaObject(L, obj);
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushJavaClass(
        JNIEnv *env, jobject jobj, jobject cptr, jclass obj) {
    lua_State *L = getStateFromCPtr(env, cptr);

    pushJavaClass(L, obj);
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushJavaArray(
        JNIEnv *env, jobject jobj, jobject cptr, jobject obj) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);

    pushJavaArray(L, obj);
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT void JNICALL
Java_org_eu_smileyik_luajava_LuaState__1pushJavaFunction(JNIEnv *env,
    jobject jobj,
    jobject cptr,
    jobject obj
) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);

    jobject *userData, globalRef;

    globalRef = (*env)->NewGlobalRef(env, obj);

    userData = (jobject *)lua_newuserdata(L, sizeof(jobject));
    *userData = globalRef;

    /* Creates metatable */
    lua_newtable(L);

    /* pushes the __index metamethod */
    lua_pushstring(L, LUACALLMETAMETHODTAG);
    lua_pushcfunction(L, &luaJavaFunctionCall);
    lua_rawset(L, -3);

    /* pusher the __gc metamethod */
    lua_pushstring(L, LUAGCMETAMETHODTAG);
    lua_pushcfunction(L, &gc);
    lua_rawset(L, -3);

    lua_pushstring(L, LUAJAVAOBJECTIND);
    lua_pushboolean(L, 1);
    lua_rawset(L, -3);

    if (lua_setmetatable(L, -2) == 0) {
        (*env)->ThrowNew(
                env, (*env)->FindClass(env, "org/eu/smileyik/luajava/LuaException"),
                "Index is not a java object");
    }
}

/************************************************************************
 *   JNI Called function
 *      LuaJava API Function
 ************************************************************************/

JNIEXPORT jboolean JNICALL
Java_org_eu_smileyik_luajava_LuaState__1isJavaFunction(JNIEnv *env,
                                                        jobject jobj,
                                                        jobject cptr,
                                                        jint idx) {
    /* Get luastate */
    lua_State *L = getStateFromCPtr(env, cptr);
    jobject *obj;

    if (!isJavaObject(L, idx)) {
        return JNI_FALSE;
    }

    obj = (jobject *)lua_touserdata(L, idx);

    return isJavaFunctionInstance(env, obj);
}

JNIEXPORT jint JNICALL
Java_org_eu_smileyik_luajava_LuaState__1isLuaArray(
    JNIEnv *env,
    jobject jobj,
    jobject cptr,
    jint idx
) {
    lua_State *L = getStateFromCPtr(env, cptr);
    return isLuaArray(L, (int) idx);
}

/*********************** LUA API FUNCTIONS ******************************/

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jobject JNICALL
Java_org_eu_smileyik_luajava_LuaState__1open(JNIEnv *env, jobject jobj) {
    lua_State *L = lua_open();
    return newCPtr(env, (jlong)L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openBase(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_base( L );
    lua_pushcfunction(L, luaopen_base);
    lua_pushstring(L, "");
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openTable(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_table( L );
    lua_pushcfunction(L, luaopen_table);
    lua_pushstring(L, LUA_TABLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openIo(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_io( L );
    lua_pushcfunction(L, luaopen_io);
    lua_pushstring(L, LUA_IOLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openOs(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_os( L );
    lua_pushcfunction(L, luaopen_os);
    lua_pushstring(L, LUA_OSLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openString(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_string( L );
    lua_pushcfunction(L, luaopen_string);
    lua_pushstring(L, LUA_STRLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openMath(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_math( L );
    lua_pushcfunction(L, luaopen_math);
    lua_pushstring(L, LUA_MATHLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openDebug(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_debug( L );
    lua_pushcfunction(L, luaopen_debug);
    lua_pushstring(L, LUA_DBLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openPackage(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    // luaopen_package( L );
    lua_pushcfunction(L, luaopen_package);
    lua_pushstring(L, LUA_LOADLIBNAME);
    lua_call(L, 1, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1openLibs(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    luaL_openlibs(L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1close(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (L) {
        resetCPtr(env, cptr);
        lua_close(L);
    }
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jobject JNICALL Java_org_eu_smileyik_luajava_LuaState__1newthread(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    lua_State *newThread  = lua_newthread(L);
    // origin returns L's pointer. 
    // may return newThread's 
    return newCPtr(env, (jlong)newThread);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1getTop(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_gettop(L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1setTop(
        JNIEnv *env, jobject jobj, jobject cptr, jint top) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_settop(L, (int)top);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushValue(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pushvalue(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1remove(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_remove(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1insert(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_insert(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1replace(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_replace(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1checkStack(
        JNIEnv *env, jobject jobj, jobject cptr, jint sz) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_checkstack(L, (int)sz);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1xmove(
        JNIEnv *env, jobject jobj, jobject from, jobject to, jint n) {
    lua_State *fr = getStateFromCPtr(env, from);
    lua_State *t = getStateFromCPtr(env, to);

    lua_xmove(fr, t, (int)n);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isNumber(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isnumber(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isString(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isstring(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isFunction(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isfunction(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isCFunction(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_iscfunction(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isUserdata(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isuserdata(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_istable(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isBoolean(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isboolean(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isNil(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isnil(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isNone(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isnone(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1isNoneOrNil(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_isnoneornil(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1type(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_type(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1typeName(
        JNIEnv *env, jobject jobj, jobject cptr, jint tp) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *name = lua_typename(L, tp);

    return (*env)->NewStringUTF(env, name);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1equal(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx1, jint idx2) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_equal(L, idx1, idx2);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1rawequal(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx1, jint idx2) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_rawequal(L, idx1, idx2);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1lessthan(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx1, jint idx2) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint) lua_lessthan(L, idx1, idx2);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jdouble JNICALL Java_org_eu_smileyik_luajava_LuaState__1toNumber(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jdouble)lua_tonumber(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1toInteger(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_tointeger(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1toBoolean(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_toboolean(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1toString(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx
) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *str = lua_tostring(L, idx);
    size_t strLen = strlen(str);
    jchar *to = (jchar *) malloc(sizeof(jchar) * (strLen + 1));
    if (to == NULL) {
        return (*env)->NewStringUTF(env, str);
    }
    size_t toIdx = 0;
    const unsigned char *ptr = (const unsigned char *) str;
    unsigned int cp;
    while (*ptr) {
        cp = 0;
        if ((*ptr & 0x80) == 0) {
            cp = *ptr;
            ptr += 1;
        } else if ((*ptr & 0xE0) == 0xC0) {
            if (ptr + 1 < (const unsigned char *) str + strLen) {
                cp = ((*ptr & 0x1F) << 6) | (ptr[1] & 0x3F);
                ptr += 2;
            }
        } else if ((*ptr & 0xF0) == 0xE0) {
            if (ptr + 2 < (const unsigned char *) str + strLen) {
                cp = ((*ptr & 0x0F) << 12) | ((ptr[1] & 0x3F) << 6) | (ptr[2] & 0x3F);
                ptr += 3;
            }
        } else if ((*ptr & 0xF8) == 0xF0) {
            if (ptr + 3 < (const unsigned char *) str + strLen) {
                cp = ((*ptr & 0x07) << 18) | ((ptr[1] & 0x3F) << 12) | ((ptr[2] & 0x3F) << 6) | (ptr[3] & 0x3F);
                ptr += 4;
            }
        } else {
            ptr++;
            continue;
        }

        if (cp) {
            if (cp <= 0xFFFF) {
                // BMP character
                to[toIdx++] = (jchar) cp;
            } else {
                // Non-BMP character, needs surrogate pair
                cp -= 0x10000;
                to[toIdx++] = (jchar) (0xD800 + (cp >> 10));
                to[toIdx++] = (jchar)(0xDC00 + (cp & 0x3FF));
            }
        } else {
            ptr++;
        }
    }
    jstring jstr = (*env)->NewString(env, to, toIdx);
    free(to);
    return jstr;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jlong JNICALL Java_org_eu_smileyik_luajava_LuaState__1toPointer(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);
    return (jlong) lua_topointer(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1strlen(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_strlen(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1objlen(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_objlen(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jobject JNICALL Java_org_eu_smileyik_luajava_LuaState__1toThread(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L, *thr;
    L = getStateFromCPtr(env, cptr);
    thr = lua_tothread(L, (int)idx);
    return newCPtr(env, (jlong)thr);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushNil(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pushnil(L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushNumber(
        JNIEnv *env, jobject jobj, jobject cptr, jdouble number) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pushnumber(L, (lua_Number)number);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushInteger(
        JNIEnv *env, jobject jobj, jobject cptr, jint number) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pushinteger(L, (lua_Integer)number);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL
Java_org_eu_smileyik_luajava_LuaState__1pushString__Lorg_eu_smileyik_luajava_CPtr_2Ljava_lang_String_2(
        JNIEnv *env, jobject jobj, jobject cptr, jstring str) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *uniStr;

    uniStr = (*env)->GetStringUTFChars(env, str, NULL);

    if (uniStr == NULL)
        return;

    lua_pushstring(L, uniStr);

    (*env)->ReleaseStringUTFChars(env, str, uniStr);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL
Java_org_eu_smileyik_luajava_LuaState__1pushString__Lorg_eu_smileyik_luajava_CPtr_2_3BI(
        JNIEnv *env, jobject jobj, jobject cptr, jbyteArray bytes, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    char *cBytes;

    cBytes = (char *)(*env)->GetByteArrayElements(env, bytes, NULL);

    lua_pushlstring(L, cBytes, n);

    (*env)->ReleaseByteArrayElements(env, bytes, cBytes, 0);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pushBoolean(
        JNIEnv *env, jobject jobj, jobject cptr, jint jbool) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pushboolean(L, (int)jbool);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1getTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_gettable(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1getField(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jstring k) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *uniStr;
    uniStr = (*env)->GetStringUTFChars(env, k, NULL);

    lua_getfield(L, (int)idx, uniStr);

    (*env)->ReleaseStringUTFChars(env, k, uniStr);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1rawGet(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_rawget(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1rawGetI(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_rawgeti(L, idx, n);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1createTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint narr, jint nrec) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_createtable(L, (int)narr, (int)nrec);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1newTable(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_newtable(L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1getMetaTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return lua_getmetatable(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1getFEnv(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_getfenv(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1setTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_settable(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1setField(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jstring k) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *uniStr;
    uniStr = (*env)->GetStringUTFChars(env, k, NULL);

    lua_setfield(L, (int)idx, uniStr);

    (*env)->ReleaseStringUTFChars(env, k, uniStr);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1rawSet(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_rawset(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1rawSetI(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_rawseti(L, idx, n);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1setMetaTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return lua_setmetatable(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1setFEnv(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return lua_setfenv(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1call(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArgs, jint nResults) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_call(L, nArgs, nResults);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1pcall(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArgs, jint nResults,
        jint errFunc) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_pcall(L, nArgs, nResults, errFunc);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1yield(
        JNIEnv *env, jobject jobj, jobject cptr, jint nResults) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_yield(L, nResults);
}

#ifdef LUAJAVA_ENABLE_METHOD_RESUME_LUA_51
/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1resume(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArgs) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_resume(L, nArgs);
}
#endif // LUAJAVA_ENABLE_METHOD_RESUME_LUA_51

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1status(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_status(L);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1gc(
        JNIEnv *env, jobject jobj, jobject cptr, jint what, jint data) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_gc(L, what, data);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1next(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)lua_next(L, idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1error(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    lua_error(L);
    return (jint) 1;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1concat(
        JNIEnv *env, jobject jobj, jobject cptr, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_concat(L, n);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1pop(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
    lua_State *L = getStateFromCPtr(env, cptr);

    lua_pop(L, (int)idx);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1setGlobal(
        JNIEnv *env, jobject jobj, jobject cptr, jstring name) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *str = (*env)->GetStringUTFChars(env, name, NULL);

    lua_setglobal(L, str);

    (*env)->ReleaseStringUTFChars(env, name, str);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1getGlobal(
        JNIEnv *env, jobject jobj, jobject cptr, jstring name) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *str = (*env)->GetStringUTFChars(env, name, NULL);

    lua_getglobal(L, str);

    (*env)->ReleaseStringUTFChars(env, name, str);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LdoFile(
        JNIEnv *env, jobject jobj, jobject cptr, jstring fileName) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *file = (*env)->GetStringUTFChars(env, fileName, NULL);

    int ret;

    ret = luaL_dofile(L, file);

    (*env)->ReleaseStringUTFChars(env, fileName, file);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LdoString(
        JNIEnv *env, jobject jobj, jobject cptr, jstring str) {
    lua_State *L = getStateFromCPtr(env, cptr);

    const char *utfStr = (*env)->GetStringUTFChars(env, str, NULL);

    int ret;

    ret = luaL_dostring(L, utfStr);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LgetMetaField(
        JNIEnv *env, jobject jobj, jobject cptr, jint obj, jstring e) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *str = (*env)->GetStringUTFChars(env, e, NULL);
    int ret;

    ret = luaL_getmetafield(L, (int)obj, str);

    (*env)->ReleaseStringUTFChars(env, e, str);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LcallMeta(
        JNIEnv *env, jobject jobj, jobject cptr, jint obj, jstring e) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *str = (*env)->GetStringUTFChars(env, e, NULL);
    int ret;

    ret = luaL_callmeta(L, (int)obj, str);

    (*env)->ReleaseStringUTFChars(env, e, str);

    return (jint)ret;
}

#ifdef LUAJAVA_ENABLE_METHOD_TYPEEROR
/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1Ltyperror(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArg, jstring tName) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *name = (*env)->GetStringUTFChars(env, tName, NULL);
    int ret;

    ret = luaL_typerror(L, (int)nArg, name);

    (*env)->ReleaseStringUTFChars(env, tName, name);

    return (jint)ret;
}
#endif // LUAJAVA_ENABLE_METHOD_TYPEEROR

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LargError(
        JNIEnv *env, jobject jobj, jobject cptr, jint numArg, jstring extraMsg) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *msg = (*env)->GetStringUTFChars(env, extraMsg, NULL);
    int ret;

    ret = luaL_argerror(L, (int)numArg, msg);

    (*env)->ReleaseStringUTFChars(env, extraMsg, msg);

    return (jint)ret;
    ;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL
Java_org_eu_smileyik_luajava_LuaState__1LcheckString(JNIEnv *env,
                                                    jobject jobj,
                                                    jobject cptr,
                                                    jint numArg) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *res;

    res = luaL_checkstring(L, (int)numArg);

    return (*env)->NewStringUTF(env, res);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1LoptString(
        JNIEnv *env, jobject jobj, jobject cptr, jint numArg, jstring def) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *d = (*env)->GetStringUTFChars(env, def, NULL);
    const char *res;
    jstring ret;

    res = luaL_optstring(L, (int)numArg, d);

    ret = (*env)->NewStringUTF(env, res);

    (*env)->ReleaseStringUTFChars(env, def, d);

    return ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jdouble JNICALL
Java_org_eu_smileyik_luajava_LuaState__1LcheckNumber(JNIEnv *env,
                                                    jobject jobj,
                                                    jobject cptr,
                                                    jint numArg) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jdouble)luaL_checknumber(L, (int)numArg);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jdouble JNICALL Java_org_eu_smileyik_luajava_LuaState__1LoptNumber(
        JNIEnv *env, jobject jobj, jobject cptr, jint numArg, jdouble def) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jdouble)luaL_optnumber(L, (int)numArg, (lua_Number)def);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LcheckInteger(
        JNIEnv *env, jobject jobj, jobject cptr, jint numArg) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)luaL_checkinteger(L, (int)numArg);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LoptInteger(
        JNIEnv *env, jobject jobj, jobject cptr, jint numArg, jint def) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)luaL_optinteger(L, (int)numArg, (lua_Integer)def);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LcheckStack(
        JNIEnv *env, jobject jobj, jobject cptr, jint sz, jstring msg) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *m = (*env)->GetStringUTFChars(env, msg, NULL);

    luaL_checkstack(L, (int)sz, m);

    (*env)->ReleaseStringUTFChars(env, msg, m);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LcheckType(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArg, jint t) {
    lua_State *L = getStateFromCPtr(env, cptr);

    luaL_checktype(L, (int)nArg, (int)t);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LcheckAny(
        JNIEnv *env, jobject jobj, jobject cptr, jint nArg) {
    lua_State *L = getStateFromCPtr(env, cptr);

    luaL_checkany(L, (int)nArg);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LnewMetatable(
        JNIEnv *env, jobject jobj, jobject cptr, jstring tName) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *name = (*env)->GetStringUTFChars(env, tName, NULL);
    int ret;

    ret = luaL_newmetatable(L, name);

    (*env)->ReleaseStringUTFChars(env, tName, name);

    return (jint)ret;
    ;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LgetMetatable(
        JNIEnv *env, jobject jobj, jobject cptr, jstring tName) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *name = (*env)->GetStringUTFChars(env, tName, NULL);

    luaL_getmetatable(L, name);

    (*env)->ReleaseStringUTFChars(env, tName, name);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1Lwhere(
        JNIEnv *env, jobject jobj, jobject cptr, jint lvl) {
    lua_State *L = getStateFromCPtr(env, cptr);

    luaL_where(L, (int)lvl);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1Lref(
        JNIEnv *env, jobject jobj, jobject cptr, jint t) {
    lua_State *L = getStateFromCPtr(env, cptr);

    return (jint)luaL_ref(L, (int)t);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LunRef(
        JNIEnv *env, jobject jobj, jobject cptr, jint t, jint ref) {
    lua_State *L = getStateFromCPtr(env, cptr);

    luaL_unref(L, (int)t, (int)ref);
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LgetN
    (JNIEnv * env , jobject jobj , jobject cptr , jint t)
{
     lua_State * L = getStateFromCPtr( env , cptr );
     // luaL_getn 方法在 Lua 5.1 中不存在
     // 可以设置或读取表中的n字段.
    if (!lua_istable( L, (int) t )) {
        lua_pushstring(L, "Target object type is not table!"); \
        lua_error(L);
    }
    lua_getfield( L , (int) t, "n" );
    lua_Integer len = lua_tointeger( L , (int) t );
    lua_pop( L, 1 );

    return ( jint ) len;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/


JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1LsetN
    (JNIEnv * env , jobject jobj , jobject cptr , jint t , jint n)
{
     lua_State * L = getStateFromCPtr( env , cptr );
     // luaL_setn 方法在 Lua 5.1 中不存在
     // 可以设置或读取表中的n字段.
    lua_pushstring( L , "n" );
    lua_pushinteger( L, (int) n );
    if ( t < 0 ) t -= 2;
    lua_settable( L , t );
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LloadFile(
        JNIEnv *env, jobject jobj, jobject cptr, jstring fileName) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    int ret;

    ret = luaL_loadfile(L, fn);

    (*env)->ReleaseStringUTFChars(env, fileName, fn);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LloadBuffer(
        JNIEnv *env, jobject jobj, jobject cptr, jbyteArray buff, jlong sz,
        jstring n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    jbyte *cBuff = (*env)->GetByteArrayElements(env, buff, NULL);
    const char *name = (*env)->GetStringUTFChars(env, n, NULL);
    int ret;

    ret = luaL_loadbuffer(L, (const char *)cBuff, (int)sz, name);

    (*env)->ReleaseStringUTFChars(env, n, name);

    (*env)->ReleaseByteArrayElements(env, buff, cBuff, 0);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1LloadString(
        JNIEnv *env, jobject jobj, jobject cptr, jstring str) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *fn = (*env)->GetStringUTFChars(env, str, NULL);
    int ret;

    ret = luaL_loadstring(L, fn);

    (*env)->ReleaseStringUTFChars(env, str, fn);

    return (jint)ret;
}

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1Lgsub(
        JNIEnv *env, jobject jobj, jobject cptr, jstring s, jstring p, jstring r) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *utS = (*env)->GetStringUTFChars(env, s, NULL);
    const char *utP = (*env)->GetStringUTFChars(env, p, NULL);
    const char *utR = (*env)->GetStringUTFChars(env, r, NULL);

    const char *sub = luaL_gsub(L, utS, utP, utR);

    (*env)->ReleaseStringUTFChars(env, s, utS);
    (*env)->ReleaseStringUTFChars(env, p, utP);
    (*env)->ReleaseStringUTFChars(env, r, utR);

    return (*env)->NewStringUTF(env, sub);
}

#ifdef LUAJAVA_ENABLE_METHOD_FINDTABLE
/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1LfindTable(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jstring fname,
        jint szhint) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char *name = (*env)->GetStringUTFChars(env, fname, NULL);

    const char *sub = luaL_findtable(L, (int)idx, name, (int)szhint);

    (*env)->ReleaseStringUTFChars(env, fname, name);

    return (*env)->NewStringUTF(env, sub);
}
#endif // LUAJAVA_ENABLE_METHOD_FINDTABLE

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _luaDump
 * Signature:
 * (Lorg/eu/smileyik/luajava/CPtr;Lorg/eu/smileyik/luajava/ILuaReadWriteEntity;I)I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1luaDump(
        JNIEnv *env, jobject jobj, jobject cptr, jobject userdata, jint strip) {
    lua_State *L = getStateFromCPtr(env, cptr);
    return (jint) LUA_DUMP(L, luajavaLuaWriter, userdata, strip);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _luaLoad
 * Signature:
 * (Lorg/eu/smileyik/luajava/CPtr;Lorg/eu/smileyik/luajava/ILuaReadWriteEntity;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1luaLoad(
        JNIEnv *env, jobject jobj, jobject cptr, jobject userdata,
        jstring chunkName, jstring mode) {
    lua_State *L = getStateFromCPtr(env, cptr);
    const char* chunkNameStr = (*env)->GetStringUTFChars(env, chunkName, NULL);
    const char* modeStr = (*env)->GetStringUTFChars(env, mode, NULL);
    int result = LUA_LOAD(L, luajavaLuaReader, userdata, chunkNameStr, modeStr);
    (*env)->ReleaseStringUTFChars(env, chunkName, chunkNameStr);
    (*env)->ReleaseStringUTFChars(env, mode, modeStr);
    return (jint) result;
}

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1copyValue(
        JNIEnv *env, jobject jobj, jobject src, jint idx, jobject dest) {
    lua_State *srcL = getStateFromCPtr(env, src);
    lua_State *destL = getStateFromCPtr(env, dest);
    return (jint) luajavaCopyLuaValueWrapper(srcL, idx, destL);
}

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1newGlobalEnv(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    return luajavaNewGlobalEnv(L);
}

JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1copyTableIfNotExists(
        JNIEnv *env, jobject jobj, jobject src, jint idx, jobject dest) {
    lua_State *srcL = getStateFromCPtr(env, src);
    lua_State *destL = getStateFromCPtr(env, dest);
    return (jint) luajavaCopyTableIfNotExists(srcL, idx, destL);
}

// ********************** Debug API ***********************

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _setHook
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;II)V
 */
JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1setHook(
        JNIEnv *env, jobject jobj, jobject cptr, jint mask, jint count) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return;
    lua_sethook(L, luajavaLuaHook, mask, count);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getHookMask
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;)I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1getHookMask(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return -1;
    return lua_gethookmask(L);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getHookCount
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;)I
 */
JNIEXPORT jint JNICALL Java_org_eu_smileyik_luajava_LuaState__1getHookCount(
        JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return -1;
    return lua_gethookcount(L);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getStack
 * Signature:
 * (Lorg/eu/smileyik/luajava/CPtr;I)Lorg/eu/smileyik/luajava/debug/LuaDebug;
 */
JNIEXPORT jobject JNICALL Java_org_eu_smileyik_luajava_LuaState__1getStack(
        JNIEnv *env, jobject jobj, jobject cptr, jint level) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    lua_Debug *ar = (lua_Debug *) malloc(sizeof(lua_Debug));
    memset(ar, 0, sizeof(lua_Debug));
    if (lua_getstack(L, level, ar)) {
        return luajavaNewLuaDebug(L, env, ar, "");
    }
    free(ar);
    return NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getInfo
 * Signature:
 * (Lorg/eu/smileyik/luajava/CPtr;Ljava/lang/String;)Lorg/eu/smileyik/luajava/debug/LuaDebug;
 */
JNIEXPORT jobject JNICALL Java_org_eu_smileyik_luajava_LuaState__1getInfo(
        JNIEnv *env, jobject jobj, jobject cptr, jlong arPtr, jstring jWhat) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    const char *what = (*env)->GetStringUTFChars(env, jWhat, NULL);
    lua_Debug *ar = (lua_Debug *) ((jbyte *) arPtr);
    if (lua_getinfo(L, what, ar)) {
        jobject result = luajavaNewLuaDebug(L, env, ar, what);
        (*env)->ReleaseStringUTFChars(env, jWhat, what);
        return result;
    }
    (*env)->ReleaseStringUTFChars(env, jWhat, what);
    return NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getLocal
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;JI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1getLocal(
        JNIEnv *env, jobject jobj, jobject cptr, jlong arPtr, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    lua_Debug *ar = (lua_Debug *) ((jbyte *) arPtr);
    const char* name = lua_getlocal(L, ar, n);
    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _setLocal
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;JI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1setLocal(
        JNIEnv *env, jobject jobj, jobject cptr, jlong arPtr, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    lua_Debug *ar = (lua_Debug *) ((jbyte *) arPtr);
    const char* name = lua_setlocal(L, ar, n);
    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _getUpValue
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1getUpValue(
        JNIEnv *env, jobject jobj, jobject cptr, jint funcIndex, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    const char* name = lua_getupvalue(L, funcIndex, n);
    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _setUpValue
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eu_smileyik_luajava_LuaState__1setUpValue(
        JNIEnv *env, jobject jobj, jobject cptr, jint funcIndex, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return NULL;

    const char* name = lua_setupvalue(L, funcIndex, n);
    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _freeLuaDebug
 * Signature: (J)V;
 */
JNIEXPORT void JNICALL
Java_org_eu_smileyik_luajava_LuaState__1freeLuaDebug(JNIEnv *env, jobject jobj, jlong arPtr) {
    lua_Debug *ar = (lua_Debug *) ((jbyte *) arPtr);
    free(ar);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _longSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_eu_smileyik_luajava_LuaState__1longSize(JNIEnv *env, jclass jobj) {
    return sizeof(long);
}
