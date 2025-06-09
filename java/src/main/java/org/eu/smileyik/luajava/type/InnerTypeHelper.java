package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFacade;

import java.util.Optional;

public class InnerTypeHelper {

    /**
     * Create the subclass of LuaObject.
     * @param luaState  Lua state
     * @param idx       Index
     * @return If returned empty then need fallback and create LuaObject.
     */
    public static Optional<LuaObject> createLuaObject(LuaStateFacade luaState, int idx) {
        return luaState.lock(l -> {
            int type = l.type(idx);
            switch (type) {
                case LuaType.FUNCTION:
                    return Optional.of(new LuaFunction(luaState, idx));
                case LuaType.TABLE:
                    return Optional.of(LuaTable.create(luaState, idx));
                case LuaType.BOOLEAN:
                    return Optional.of(new LuaBoolean(luaState, idx));
                case LuaType.NUMBER:
                    return Optional.of(new LuaNumber(luaState, idx));
                case LuaType.USERDATA:
                    return Optional.of(new LuaUserdata(luaState, idx));
                case LuaType.STRING:
                    return Optional.of(new LuaString(luaState, idx));
            }
            return Optional.empty();
        });
    }
}
