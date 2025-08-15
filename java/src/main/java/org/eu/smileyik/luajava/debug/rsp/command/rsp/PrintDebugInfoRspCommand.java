package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;

public class PrintDebugInfoRspCommand extends PrintRspCommand {
    @Override
    protected String[] innerCommandKeys() {
        return new String[] {
                "info", "i"
        };
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        LuaDebug currentDebugInfo = debugServer.getCurrentDebugInfo();
        String message = "Currently, there is not have a debug info\n";
        if (currentDebugInfo != null) {
            message = new StringBuilder("LuaDebug: \n")
                    .append("  event:          ").append(currentDebugInfo.getEvent()).append("\n")
                    .append("  name:           ").append(currentDebugInfo.getName()).append(",    ")
                    .append("  nameWhat:       ").append(currentDebugInfo.getNameWhat()).append("\n")
                    .append("  what:           ").append(currentDebugInfo.getWhat()).append("\n")
                    // .append("  source: ").append(currentDebugInfo.getSource()).append("\n")
                    .append("  srcLen:         ").append(currentDebugInfo.getSrcLen()).append(",    ")
                    .append("  currentLine:    ").append(currentDebugInfo.getCurrentLine()).append("\n")
                    .append("  lineDefine:     ").append(currentDebugInfo.getLineDefine()).append(",    ")
                    .append("  lastLineDefine: ").append(currentDebugInfo.getLastLineDefine()).append("\n")
                    .append("  nUps:           ").append(currentDebugInfo.getnUps()).append(",    ")
                    .append("  nParams:        ").append(currentDebugInfo.getnParams()).append("\n")
                    .append("  isVarArg:       ").append(currentDebugInfo.getIsVarArg()).append(",    ")
                    .append("  isTailCall:     ").append(currentDebugInfo.getIsTailCall()).append("\n")
                    .append("  fTransfer:      ").append(currentDebugInfo.getfTransfer()).append(",    ")
                    .append("  nTransfer:      ").append(currentDebugInfo.getnTransfer()).append("\n")
                    .append("  shortSrc:       ").append(currentDebugInfo.getShortSrc()).append("\n")
                    .toString();
        }
        debugServer.sendMessage(message);
        return "OK";
    }
}
