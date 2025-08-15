package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.command.hook.ContinueCommand;

import java.io.IOException;

public class SetContinueRspCommand implements RspCommand {
    @Override
    public String[] commandKeys() {
        return new String[]{
                "mode.continue",
                "mode.c",
                "m.continue",
                "m.c"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.sendMessage("set c command to: continue mode\n");
        debugServer.setContinueType(ContinueCommand.INSTANCE);
        return "OK";
    }
}
