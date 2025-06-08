package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import java.util.Optional;

public class InnerTypeHelper {

    /**
     * Create the subclass of LuaObject.
     * @param l    Lua state
     * @param idx  Index
     * @return If returned empty then need fallback and create LuaObject.
     */
    public static Optional<LuaObject> createLuaObject(LuaState l, int idx) {
        int type = l.type(idx);
        switch (type) {
            case LuaType.FUNCTION:
                return Optional.of(new LuaFunction(l, idx));
            case LuaType.TABLE:
                return Optional.of(LuaTable.create(l, idx));
            case LuaType.BOOLEAN:
                return Optional.of(new LuaBoolean(l, idx));
            case LuaType.NUMBER:
                return Optional.of(new LuaNumber(l, idx));
            case LuaType.USERDATA:
                return Optional.of(new LuaUserdata(l, idx));
            case LuaType.STRING:
                return Optional.of(new LuaString(l, idx));
        }
        return Optional.empty();
    }
}
