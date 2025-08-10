/*
 * LuaType.java, SmileYik, 2025-8-10
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
 */

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
