package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

public class LuaString extends LuaObject implements GettableType<String> {
    /**
     * Creates a reference to an object in the given index of the stack
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY</strong>
     *
     * @param L
     * @param index of the object on the lua stack
     * @see LuaObject#create(LuaState, int)
     */
    protected LuaString(LuaState L, int index) {
        super(L, index);
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public int type() {
        return LuaType.STRING;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
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

    @Override
    public String get() {
        return getString();
    }
}
