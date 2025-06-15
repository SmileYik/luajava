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