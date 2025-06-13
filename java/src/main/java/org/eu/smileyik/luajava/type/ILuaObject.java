package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaStateFacade;

public interface ILuaObject {
    LuaStateFacade getLuaState();

    public void push();

    public int type();

    public default boolean isBoolean() {
        return type() == LuaType.BOOLEAN;
    }

    public default boolean isNumber() {
        return type() == LuaType.NUMBER;
    }

    public default boolean isString() {
        return type() == LuaType.STRING;
    }

    public default boolean isNil() {
        return type() == LuaType.NIL;
    }

    public default boolean isTable() {
        return type() == LuaType.TABLE;
    }

    public default boolean isFunction() {
        return type() == LuaType.FUNCTION;
    }

    public default boolean isUserdata() {
        return type() == LuaType.USERDATA;
    }
}
