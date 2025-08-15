package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.command.hook.StepInCommand;

import java.io.IOException;

public class StepInRspCommand implements RspCommand {
    @Override
    public String[] commandKeys() {
        return new String[]{
                "mode.stepin",
                "mode.step",
                "mode.s",
                "m.stepin",
                "m.step",
                "m.s",
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.sendMessage("set c command to: step in mode\n");
        debugServer.setContinueType(StepInCommand.INSTANCE);
        return "OK";
    }
}
