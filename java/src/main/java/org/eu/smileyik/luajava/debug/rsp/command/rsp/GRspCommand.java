package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;

import java.io.IOException;

import static org.eu.smileyik.luajava.debug.rsp.RspDebugServer.fastRepeat;
import static org.eu.smileyik.luajava.debug.rsp.RspDebugServer.long2Hex;

public class GRspCommand implements RspCommand {
    @Override
    public String[] commandKeys() {
        return new String[]{"g"};
    }

    @Override
    public String handle(RspDebugServer debugServer, String command, String[] args) throws IOException {
        LuaDebug currentDebugInfo = debugServer.getCurrentDebugInfo();
        long line = currentDebugInfo == null ? 0 : currentDebugInfo.getCurrentLine();
        return fastRepeat(long2Hex(line), 16);
    }
}
