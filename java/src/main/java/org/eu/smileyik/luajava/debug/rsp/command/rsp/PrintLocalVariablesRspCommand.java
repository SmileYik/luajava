package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;
import java.util.Map;

public class PrintLocalVariablesRspCommand extends PrintRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "local", "l"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        String message = "";
        Map<String, String> variables = debugServer.getStringLocalVariables();
        if (args.length > 1) {
            String name = args[1];
            message = buildVariableMessage(variables, "local", name);
        } else {
            message = buildVariableMessage(variables, "local");
        }
        debugServer.sendMessage(message);
        return "OK";
    }
}
