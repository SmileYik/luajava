package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;
import java.util.Map;

public class PrintGlobalVariablesRspCommand extends PrintRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "global", "g"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        String message = "";
        Map<String, String> variables = debugServer.getStringGlobalVariables();
        if (args.length > 1) {
            String name = args[1];
            message = buildVariableMessage(variables, "global", name);
        } else {
            message = buildVariableMessage(variables, "global");
        }
        debugServer.sendMessage(message);
        return "OK";
    }
}
