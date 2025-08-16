package org.eu.smileyik.luajava.debug.rsp.command.hook;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

public interface Command {
    /**
     * handle command
     * @return true then need jump out hook method.
     */
    public boolean handle(RspDebugServer debugServer, LuaStateFacade facade, LuaDebug ar);
}
