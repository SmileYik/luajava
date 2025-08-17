package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.rsp.command.hook.PrintVariableCommand;

import java.io.IOException;

public class PrintUpvalueVariablesRspCommand extends PrintRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "upvalue", "u"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        debugServer.addCommand(new PrintVariableCommand(
                PrintVariableCommand.SCOPE_UPVALUE,
                args.length > 1 ? args[1] : null
        ));
        try {
            debugServer.waitFillMessage();
        } catch (InterruptedException ignored) {
        }
        return "OK";
    }
}
