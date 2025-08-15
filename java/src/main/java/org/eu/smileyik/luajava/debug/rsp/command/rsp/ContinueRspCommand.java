package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;

public class ContinueRspCommand implements RspCommand {
    @Override
    public String[] commandKeys() {
        return new String[]{
                "continue",
                "c",
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.addCommand(debugServer.getContinueType());
        return "OK";
    }
}
