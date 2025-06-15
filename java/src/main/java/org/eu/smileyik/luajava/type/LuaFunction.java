package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFacade;

public class LuaFunction extends LuaObject implements ILuaCallable {
    public static final String TYPE_NAME = LuaType.typeName(LuaType.FUNCTION);

    /**
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY, EXPECT YOU KNOW WHAT YOU ARE DOING</strong>
     *
     * @param L     lua state
     * @param index index
     * @see LuaObject#create(LuaStateFacade, int)
     */
    protected LuaFunction(LuaStateFacade L, int index) {
        super(L, index);
    }

    @Override
    public String toString() {
        return TYPE_NAME;
    }

    @Override
    public int type() {
        return LuaType.FUNCTION;
    }
}
