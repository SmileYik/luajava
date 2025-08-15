package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.FunctionNameBreakPoint;

import java.io.IOException;

public class AddFunctionNameBreakPointRspCommand extends BreakPointRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "function", "functionname", "func", "f",
                "method", "methodname", "method", "m",
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.addBreakPoint(new FunctionNameBreakPoint(args[1]));
        return "OK";
    }
}
