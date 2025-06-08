package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

public class LuaFunction extends LuaObject {
    public static final String TYPE_NAME = LuaType.typeName(LuaType.FUNCTION);

    protected LuaFunction(LuaState L, int index) {
        super(L, index);
    }

    @Override
    public String toString() {
        return TYPE_NAME;
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.FUNCTION;
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
}
