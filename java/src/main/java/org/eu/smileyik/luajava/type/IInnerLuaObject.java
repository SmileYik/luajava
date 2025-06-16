package org.eu.smileyik.luajava.type;

public interface IInnerLuaObject {

    /**
     * push this object to stack top without lock.
     */
    public void rawPush();
}
