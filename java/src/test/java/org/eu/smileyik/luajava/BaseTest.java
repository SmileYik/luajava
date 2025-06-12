package org.eu.smileyik.luajava;

import org.keplerproject.luajava.LoadLibrary;
import org.keplerproject.luajava.LuaStateFacade;
import org.keplerproject.luajava.LuaStateFactory;

public class BaseTest {
    static {
        LoadLibrary.load();
    }

    protected LuaStateFacade newLuaState() {
        LuaStateFacade luaStateFacade = LuaStateFactory.newLuaState();
        luaStateFacade.openLibs();
        return luaStateFacade;
    }
}
