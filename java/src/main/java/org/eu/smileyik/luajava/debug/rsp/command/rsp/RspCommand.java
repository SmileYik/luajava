package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface RspCommand {

    static final RspCommand[] COMMANDS = new RspCommand[] {
            new AddFunctionNameBreakPointRspCommand(),
            new RemoveFunctionNameBreakPointRspCommand(),
            new AddLineBreakPointRspCommand(),
            new RemoveLineBreakPointRspCommand(),

            new ContinueRspCommand(),
            new StepInRspCommand(),
            new NextStepRspCommand(),
            new SetContinueRspCommand(),

            new GRspCommand(),
            new PRspCommand(),

            new PrintLocalVariablesRspCommand(),
            new PrintGlobalVariablesRspCommand(),
            new PrintUpvalueVariablesRspCommand(),
            new PrintDebugInfoRspCommand()
    };

    static final Map<String, RspCommand> COMMAND_MAP = buildCommandMap(COMMANDS);

    static final Map<String, String> SIM_COMMAND_RESP = RspDebugServer.ofMap(
            "qfThreadInfo", "l16500",
            "qsThreadInfo", "l16500",
            "?", "T05",
            "qC", "QC16500",
            "qAttached", "0",
            "qsThreadInfo", "l16500",
            "qsThreadInfo", "l16500"
    );

    static Map<String, RspCommand> buildCommandMap(RspCommand[] commands) {
        Map<String, RspCommand> map = new HashMap<String, RspCommand>();
        for (RspCommand command : commands) {
            for (String key : command.commandKeys()) {
                map.put(key, command);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    String[] commandKeys();
    String handle(RspDebugServer debugServer, String command, String[] args) throws IOException;
}
