package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.command.hook.NextStepCommand;

import java.io.IOException;

public class NextStepRspCommand implements RspCommand {
    @Override
    public String[] commandKeys() {
        return new String[]{
                "m.nextstep",
                "m.next",
                "m.n",
                "mode.nextstep",
                "mode.next",
                "mode.n"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.sendMessage("set c command to: next step mode\n");
        debugServer.setContinueType(NextStepCommand.INSTANCE);
        return "OK";
    }
}
