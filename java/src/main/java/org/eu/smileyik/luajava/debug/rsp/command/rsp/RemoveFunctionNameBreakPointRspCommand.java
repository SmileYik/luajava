package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.FunctionNameBreakPoint;

import java.io.IOException;

public class RemoveFunctionNameBreakPointRspCommand extends BreakPointRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "rfunction", "rfunctionname", "rfunc", "rf",
                "dmethod", "dmethodname", "dmethod", "dm",
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.removeBreakPoint(new FunctionNameBreakPoint(args[1]));
        return "OK";
    }
}
