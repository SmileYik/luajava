/******************************************************************************
 * compatible_lua52.h, SmileYik, 2025-8-10
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

#ifndef LUA_JAVA_COMPATIBLE_LUA52_HEADER
#define LUA_JAVA_COMPATIBLE_LUA52_HEADER

#ifdef LUA_RIDX_GLOBALS
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_RIDX_GLOBALS
#else
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_GLOBALSINDEX
#endif

// same metatable for class instance, need uservalue support.
#define LUAJAVA_FORCE_SAME_METATABLE_CLASS
// same metatable for java object instance, need uservalue support.
#define LUAJAVA_FORCE_SAME_METATABLE_OBJECT
// same metatable for java array instance, need uservalue support.
#define LUAJAVA_FORCE_SAME_METATABLE_ARRAY

// alternative metatable to uservalue.
#define LUAJAVA_GET_METATABLE( L , OBJ_IDX ) lua_getuservalue(L, OBJ_IDX)
#define LUAJAVA_SET_METATABLE( L , OBJ_IDX ) lua_setuservalue(L, OBJ_IDX)

// lua_dump / lua_load
#define LUA_DUMP(L, LUA_WRITER, USERDATA, STRIP) lua_dump(L, LUA_WRITER, USERDATA, STRIP);
#define LUA_LOAD(L, LUA_READER, USERDATA, CHUNCK_NAME, MODE) lua_load(L, LUA_READER, USERDATA, CHUNCK_NAME, MODE);

// lua 5.2 remove lua_open()
#define lua_open()                      luaL_newstate()

// lua 5.2 remove lua_equal( L, idx1 , idx2 ), 应该使用 lua_compare
#define lua_equal( L , IDX1 , IDX2 )    lua_compare( L, IDX1 , IDX2 , LUA_OPEQ)

// lua 5.2 remove lua_lessthan( L, idx1 , idx2 ), 应该使用 lua_compare
#define lua_lessthan( L , IDX1 , IDX2 ) lua_compare( L, IDX1 , IDX2 , LUA_OPLT)

// lua 5.2 remove lua_strlen, 应该使用 lua_rawlen(L, idx)
#define lua_strlen( L , IDX )           lua_rawlen( L , IDX )

// lua 5.2 remove lua_objlen, 用lua_rawlen代替
#define lua_objlen( L , IDX )           lua_rawlen( L , IDX )

// lua 5.2 remove lua_getfenv
#define lua_getfenv( L , IDX )          ;

// lua 5.2 remove lua_setfenv
#define lua_setfenv( L , IDX )          0

// Lua 5.2: Function lua_resume has an extra parameter, from. Pass NULL or the thread doing the call.
#ifdef LUAJAVA_ENABLE_METHOD_RESUME_LUA_51
#undef LUAJAVA_ENABLE_METHOD_RESUME_LUA_51
#endif
#define LUAJAVA_ENABLE_METHOD_RESUME_LUA_52

// Lua 5.2: luaL_typerror was removed. Write your own version if you need it.
#define luaL_typerror( L, N_ARG, NAME ) 0


#endif  // LUA_JAVA_COMPATIBLE_LUA52_HEADER
