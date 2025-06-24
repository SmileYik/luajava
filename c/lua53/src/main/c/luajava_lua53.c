#include "luajava.h"
#include "luajava_api.h"
#include <lauxlib.h>
#include <lua.h>
#include <lualib.h>

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _rotate
 * Signature: (Lorg/keplerproject/luajava/CPtr;II)V
 */
JNIEXPORT void JNICALL Java_org_keplerproject_luajava_LuaState__1rotate(
    JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  lua_rotate(L, idx, n);
}

/*
 * Class:     org_keplerproject_luajava_LuaState
 * Method:    _openUtf8
 * Signature: (Lorg/keplerproject/luajava/CPtr;)V
 */
JNIEXPORT void JNICALL
Java_org_keplerproject_luajava_LuaState__1openUtf8(JNIEnv *env, jobject jobj, jobject cptr) {
  lua_State *L = getStateFromCPtr(env, cptr);
  if (!L) return;
  luaopen_utf8(L);
}
