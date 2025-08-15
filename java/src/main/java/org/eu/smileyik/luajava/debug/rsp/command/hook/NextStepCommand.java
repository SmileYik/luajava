package org.eu.smileyik.luajava.debug.rsp.command.hook;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.DebugServer;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.NextLineBreakPoint;

public class NextStepCommand implements Command {
    public static final NextStepCommand INSTANCE = new NextStepCommand();

    @Override
    public boolean handle(DebugServer debugServer, LuaStateFacade facade, LuaDebug ar) {
        debugServer.step(true);
        facade.getLuaState().setHook(LuaState.LUA_MASKLINE, 0);
        debugServer.addBreakPoint(new NextLineBreakPoint(ar.getCurrentLine() + 1));
        return true;
    }
}
