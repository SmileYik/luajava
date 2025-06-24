#include "luajava.h"
#include "luajava_api.h"
#include <lauxlib.h>
#include <lua.h>
#include <lualib.h>

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _getiuservalue
 * Signature: (Lorg/keplerproject/luajava/CPtr;II)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1getiuservalue(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return 0;
  return lua_getiuservalue(L, idx, n);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _setiuservalue
 * Signature: (Lorg/keplerproject/luajava/CPtr;II)I
 */
JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1setiuservalue(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return 0;
  return lua_setiuservalue(L, idx, n);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _warning
 * Signature: (Lorg/keplerproject/luajava/CPtr;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1warning(
    JNIEnv *env, jobject jobj, jobject cptr, jstring jstr, jint nocont) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  const char* msg = (*env)->GetStringUTFChars(env, jstr, NULL);
  lua_warning(L, msg, nocont);
  (*env)->ReleaseStringUTFChars(env, jstr, msg);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _resume
 * Signature:
 * (Lorg/keplerproject/luajava/CPtr;Lorg/keplerproject/luajava/CPtr;II)I
 */
JNIEXPORT jint JNICALL
Java_org_keplerproject_luajava_LuaState__1resume__Lorg_keplerproject_luajava_CPtr_2Lorg_keplerproject_luajava_CPtr_2II(
    JNIEnv *env, jobject jobj, jobject cptr, jobject threadCptr, jint nargs, jint nrets) {
  lua_State *L = getStateFromCPtr(env, cptr);
  lua_State *T = getStateFromCPtr(env, threadCptr);
  if (!L || !T) return 0;
  return lua_resume(L, T, nargs, &nrets);
}