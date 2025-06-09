package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFacade;

public class LuaNumber extends LuaObject {
    /**
     * Creates a reference to an object in the given index of the stack
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY, EXPECT YOU KNOW WHAT YOU ARE DOING</strong>
     *
     * @param L
     * @param index of the object on the lua stack
     * @see LuaObject#create(LuaStateFacade, int)
     */
    protected LuaNumber(LuaStateFacade L, int index) {
        super(L, index);
    }

    @Override
    public String toString() {
        return Double.toString(getNumber());
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.NUMBER;
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

    public byte getByte() {
        return (byte) getNumber();
    }

    public int getInt() {
        return (int) getNumber();
    }

    public short getShort() {
        return (short) getNumber();
    }

    public long getLong() {
        return (long) getNumber();
    }

    public float getFloat() {
        return (float) getNumber();
    }

    public double getDouble() {
        return (double) getNumber();
    }
}
