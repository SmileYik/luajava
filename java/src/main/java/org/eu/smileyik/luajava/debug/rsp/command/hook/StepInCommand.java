package org.eu.smileyik.luajava.debug.rsp.command.hook;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.DebugServer;

public class StepInCommand implements Command {
    public static final Command INSTANCE = new StepInCommand();

    @Override
    public boolean handle(DebugServer debugServer, LuaStateFacade facade, LuaDebug ar) {
        debugServer.step(true);
        facade.getLuaState().setHook(LuaState.LUA_MASKLINE, 0);
        return true;
    }
}
