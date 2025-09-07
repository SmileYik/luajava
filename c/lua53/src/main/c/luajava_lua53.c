/******************************************************************************
 * luajava_lua53.c, SmileYik, 2025-8-10
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
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _rotate
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;II)V
 */
JNIEXPORT void JNICALL Java_org_eu_smileyik_luajava_LuaState__1rotate(
        JNIEnv *env, jobject jobj, jobject cptr, jint idx, jint n) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return;
    lua_rotate(L, idx, n);
}

/*
 * Class:     org_eu_smileyik_luajava_LuaState
 * Method:    _openUtf8
 * Signature: (Lorg/eu/smileyik/luajava/CPtr;)V
 */
JNIEXPORT void JNICALL
Java_org_eu_smileyik_luajava_LuaState__1openUtf8(JNIEnv *env, jobject jobj, jobject cptr) {
    lua_State *L = getStateFromCPtr(env, cptr);
    if (!L) return;
    luaopen_utf8(L);
}
