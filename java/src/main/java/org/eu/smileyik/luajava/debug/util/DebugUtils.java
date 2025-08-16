package org.eu.smileyik.luajava.debug.util;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.type.LuaTable;

import java.util.*;

public class DebugUtils {
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
                if (k instanceof String) {
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

    public static String variableToString(Object variable) {
        if (variable == null) {
            return "nil";
        } else if (variable instanceof String) {
            return String.format("\"%s\"", variable);
        } else if (variable instanceof LuaTable) {
            try {
                return variableToString(new HashSet<>(), (LuaTable) variable);
            } catch (Exception e) {
                return "[Lua Table]";
            }
        } else {
            return variable.toString();
        }
    }

    public static String variableToString(Set<Object> checked, LuaTable variable) {
        if (variable == null) {
            return "nil";
        }

        checked.add(variable);
        Map<Object, Object> map = new HashMap<>();
        variable.forEach((key, value) -> {
            if (value instanceof LuaTable) {
                if (!checked.add(value)) {
                    return;
                }
            }
            map.put(key, value);
        });
        List<String> list = new ArrayList<>();
        map.forEach((key, value) -> {
            String keyString = "nil";
            String valueString = "nil";

            if (value instanceof LuaTable) {
                valueString = variableToString(checked, (LuaTable) value);
            } else {
                valueString = variableToString(value);
            }
            if (key instanceof LuaTable) {
                keyString = variableToString(checked, (LuaTable) key);
            } else {
                keyString = variableToString(key);
            }

            list.add(String.format("[%s]=%s", keyString, valueString));
        });

        return String.format("{%s}", String.join(", ", list));
    }
}
