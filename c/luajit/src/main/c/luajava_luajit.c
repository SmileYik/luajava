#include <stdio.h>
#include <stdlib.h>
#include <lua.h>
#include <lauxlib.h>
#include <lualib.h>
#include "luajava.h"
#include "luajava_api.h"
#include "compatible.h"

/************************************************************************
 *   JNI Called function
 *      Lua Exported Function
 ************************************************************************/

JNIEXPORT jint JNICALL Java_org_keplerproject_luajava_LuaState__1getGcCount(
    JNIEnv *env, jobject jobj, jobject cptr) {
  lua_State *L = getStateFromCPtr(env, cptr);

  return (jint)lua_getgccount(L);
}