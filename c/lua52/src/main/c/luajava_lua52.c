/******************************************************************************
 * luajava_lua52.c, SmileYik, 2025-8-10
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

#include "luajava.h"
#include "luajava_api.h"
#include <lauxlib.h>
#include <lua.h>
#include <lualib.h>

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _rawlen
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1rawlen(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return 0;
  lua_absindex;
  return (jint) lua_rawlen(L, idx);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _compare
 * Signature: (Lorg/keplerproject/luajava/CPtr;III)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1compare(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx1, jint idx2, jint op) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return 0;
  return (jint) lua_compare(L, idx1, idx2, op);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _arith
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1arith(JNIEnv *env,
                                                                       jobject jobj,
                                                                       jobject cptr,
                                                                       jint op) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  lua_arith(L, op);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _len
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1len(JNIEnv *env,
                                                                     jobject jobj,
                                                                     jobject cptr,
                                                                     jint idx) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  lua_len(L, idx);
}

#ifdef LUAJAVA_ENABLE_METHOD_RESUME_LUA_52
/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _resume
 * Signature:
 * (Lorg/keplerproject/luajava/CPtr;Lorg/keplerproject/luajava/CPtr;I)I
 */
JNIEXPORT jint JNICALL
Java_org_keplerproject_luajava_LuaState__1resume__Lorg_keplerproject_luajava_CPtr_2Lorg_keplerproject_luajava_CPtr_2I(
    JNIEnv * env, jobject jobj, jobject cptr, jobject threadCptr, jint nargs) {
  lua_State *L = getStateFromCPtr(env, cptr);
  lua_State *T = getStateFromCPtr(env, threadCptr);
  if (!L || !T) return 0;
  
  return lua_resume(L, T, nargs);
}
#endif // LUAJAVA_ENABLE_METHOD_RESUME_LUA_52

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _pushthread
 * Signature:
 * (Lorg/keplerproject/luajava/CPtr;Lorg/keplerproject/luajava/CPtr;)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1pushthread(
    JNIEnv *env, jobject jobj, jobject cptr, jobject threadCptr) {
  // lua_State *L = getStateFromCPtr(env, cptr);
  lua_State *T = getStateFromCPtr(env, threadCptr);
  if (!T) return 0;
  
  return lua_pushthread(T);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _setuservalue
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1setuservalue(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  lua_setuservalue(L, idx);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _getuservalue
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1getuservalue(
  JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  lua_getuservalue(L, idx);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _absindex
 * Signature: (Lorg/keplerproject/luajava/CPtr;I)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1absindex(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return idx;
  return lua_absindex(L, idx);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _openCoroutine
 * Signature: (Lorg/keplerproject/luajava/CPtr;)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1openCoroutine(
    JNIEnv *env, jobject jobj, jobject cptr)
{
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  luaopen_coroutine(L);
}