package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFacade;

public class LuaBoolean extends LuaObject {
    /**
     * Creates a reference to an object in the given index of the stack
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY, EXPECT YOU KNOW WHAT YOU ARE DOING</strong>
     *
     * @param L
     * @param index of the object on the lua stack
     * @see LuaObject#create(LuaStateFacade, int)
     */
    protected LuaBoolean(LuaStateFacade L, int index) {
        super(L, index);
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.BOOLEAN;
    }

    @Override
    public String toString() {
        return Boolean.toString(getBoolean());
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isTable() {
        return false;
    }

    @Override
    public boolean isUserdata() {
        return false;
    }
}
