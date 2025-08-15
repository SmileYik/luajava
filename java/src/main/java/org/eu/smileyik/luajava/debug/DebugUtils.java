package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.type.LuaTable;

import java.util.*;

public class DebugUtils {
    private static Set<String> GLOBAL_VARIABLE_BLACKLIST = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "next", "select", "string", "utf8", "xpcall",
            "_G", "luajava", "error", "type", "getmetatable",
            "pairs", "load", "ipairs", "pcall", "assert",
            "rawequal", "collectgarbage", "table", "package",
            "debug", "os", "io", "coroutine", "_VERSION",
            "require", "loadfile", "rawset", "tonumber",
            "warn", "print", "setmetatable", "dofile",
            "rawget", "rawlen", "math", "tostring"
    )));

    public static Map<String, Object> getLocalVariable(LuaStateFacade facade, LuaDebug ar) {
        if (ar == null) {
            return Collections.emptyMap();
        }
        LuaState luaState = facade.getLuaState();

        Map<String, Object> map = new HashMap<>();
        String name = null;
        int idx = 1;
        while ((name = luaState.getLocal(ar, idx++)) != null) {
            if (!name.startsWith("(")) {
                map.put(name, facade.rawToJavaObject(-1).orElseGet(() -> null));
            }
            luaState.pop(1);
        }
        return map;
    }

    /**
     * Get global variable. (not include default global variable)
     * @param facade
     * @param ar
     * @return
     */
    public static Map<String, Object> getGlobalVariable(LuaStateFacade facade, LuaDebug ar) {
        if (ar == null) {
            return Collections.emptyMap();
        }
        LuaState luaState = facade.getLuaState();
        luaState.pushGlobalTable();
        Object o = facade.rawToJavaObject(-1).orElseGet(() -> null);
        facade.pop(1);
        if (o instanceof LuaTable) {
            Map<String, Object> map = new HashMap<>();
            ((LuaTable) o).forEach((k, v) -> {
                if (k instanceof String && !GLOBAL_VARIABLE_BLACKLIST.contains(k)) {
                    map.put((String) k, v);
                }
            });
            return map;
        }
        return Collections.emptyMap();
    }

    public static Map<String, Object> getUpValues(LuaStateFacade facade, LuaDebug ar) {
        if (ar == null) {
            return Collections.emptyMap();
        }
        LuaState luaState = facade.getLuaState();
        if (!luaState.isFunction(-1)) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<>();
        String name = null;
        int idx = 1;
        while ((name = luaState.getUpValue(-1, idx++)) != null) {
            map.put(name, facade.rawToJavaObject(-1).orElseGet(() -> null));
            luaState.pop(1);
        }
        return map;
    }
}
