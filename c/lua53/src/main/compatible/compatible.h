#ifndef LUA_JAVA_COMPATIBLE_HEADER
#define LUA_JAVA_COMPATIBLE_HEADER

#include "compatible_lua53.h"

#ifdef LUA_RIDX_GLOBALS
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_RIDX_GLOBALS
#else
#define LUAJAVA_LUA_RIDX_GLOBALS LUA_GLOBALSINDEX
#endif

#endif  // LUA_JAVA_COMPATIBLE_HEADER
