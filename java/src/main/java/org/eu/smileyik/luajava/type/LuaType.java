package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaState;

import java.util.HashMap;
import java.util.Map;

public class LuaType {
    final public static int NONE = LuaState.LUA_TNONE;
    final public static int NIL = LuaState.LUA_TNIL;
    final public static int BOOLEAN = LuaState.LUA_TBOOLEAN;
    final public static int LIGHT_USERDATA = LuaState.LUA_TLIGHTUSERDATA;
    final public static int NUMBER = LuaState.LUA_TNUMBER;
    final public static int STRING = LuaState.LUA_TSTRING;
    final public static int TABLE = LuaState.LUA_TTABLE;
    final public static int FUNCTION = LuaState.LUA_TFUNCTION;
    final public static int USERDATA = LuaState.LUA_TUSERDATA;
    final public static int THREAD = LuaState.LUA_TTHREAD;

    final public static String NIL_NAME = "[Lua NIL]";

    private static final Map<Integer, String> LUA_TYPE_NAME_MAP = new HashMap<Integer, String>() {
        {
            put(NONE, "[Lua None]");
            put(NIL, NIL_NAME);
            put(BOOLEAN, "[Lua Boolean]");
            put(LIGHT_USERDATA, "[Lua LightUserData]");
            put(NUMBER, "[Lua Number]");
            put(STRING, "[Lua String]");
            put(TABLE, "[Lua Table]");
            put(FUNCTION, "[Lua Function]");
            put(USERDATA, "[Lua UserData]");
            put(THREAD, "[Lua Thread]");
        }
    };

    public static String typeName(int type) {
        return LUA_TYPE_NAME_MAP.getOrDefault(type, "unknown");
    }
}
