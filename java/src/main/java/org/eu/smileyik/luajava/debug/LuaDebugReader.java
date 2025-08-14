package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.LuaState;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public interface LuaDebugReader {

    public LuaDebug read(long ptr, ByteBuffer buffer,
                         String name, String nameWhat,
                         String what, String source);

    public default long readSizeT(ByteBuffer buffer) {
        return LuaState.LONG_SIZE == 8 ? buffer.getLong() : buffer.getInt();
    }

    public default String readShortSrc(ByteBuffer buffer) {
        byte[] shortSrcBytes = new byte[buffer.remaining() - LuaState.LONG_SIZE];
        int len = 0;
        while (len < shortSrcBytes.length && shortSrcBytes[len] != 0) {
            len++;
        }
        return new String(shortSrcBytes, 0, len, StandardCharsets.UTF_8);
    }
}
