package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaStateFacade;

public interface ILuaObject {
    /**
     * get lua state.
     * @return the lua state. if lua state is closed then may return null.
     */
    LuaStateFacade getLuaState();

    /**
     * return this object is closed or not.
     * @return if closed then return true.
     */
    boolean isClosed();

    /**
     * push this object to the lua stack top with lock.
     */
    public void push();

    /**
     * get this object's type.
     * @return
     */
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
