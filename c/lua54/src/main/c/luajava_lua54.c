/******************************************************************************
 * luajava_lua54.c, SmileYik, 2025-8-10
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