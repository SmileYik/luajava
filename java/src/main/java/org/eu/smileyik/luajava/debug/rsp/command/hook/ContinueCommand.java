package org.eu.smileyik.luajava.debug.rsp.command.hook;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

public class ContinueCommand implements Command {
    public static final Command INSTANCE = new ContinueCommand();

    @Override
    public boolean handle(RspDebugServer debugServer, LuaStateFacade facade, LuaDebug ar) {
        debugServer.step(false);
        facade.getLuaState().setHook(LuaState.LUA_MASKLINE, 0);
        return true;
    }
}
