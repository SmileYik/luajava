#ifndef LUA_JAVA_COMPATIBLE_LUAJIT_HEADER
#define LUA_JAVA_COMPATIBLE_LUAJIT_HEADER

#ifdef LUA_RIDX_GLOBALS
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_RIDX_GLOBALS
#else
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_GLOBALSINDEX
#endif

#define LUAJAVA_ENABLE_METHOD_TYPEEROR
#define LUAJAVA_ENABLE_METHOD_FINDTABLE

#endif  // LUA_JAVA_COMPATIBLE_LUAJIT_HEADER


