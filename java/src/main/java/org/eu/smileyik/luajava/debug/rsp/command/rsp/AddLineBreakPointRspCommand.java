package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.LineNumberBreakPoint;

import java.io.IOException;

public class AddLineBreakPointRspCommand extends BreakPointRspCommand {

    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "line", "l"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        try {
            int lineNumber = Integer.parseInt(args[1]);
            debugServer.addBreakPoint(new LineNumberBreakPoint(lineNumber));
        } catch (NumberFormatException e) {
            debugServer.sendMessage(String.format("'%s' is not a number", args[1]));
        }
        return "OK";
    }
}
