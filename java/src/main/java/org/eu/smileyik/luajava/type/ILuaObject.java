package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaStateFacade;

public interface ILuaObject {
    LuaStateFacade getLuaState();

    public void push();
}
