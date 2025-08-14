package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.LuaState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LuaDebugReaderLua54 implements LuaDebugReader {

    // int event;
    // const char *name;	/* (n) */
    // const char *namewhat;	/* (n) 'global', 'local', 'field', 'method' */
    // const char *what;	/* (S) 'Lua', 'C', 'main', 'tail' */
    // const char *source;	/* (S) */
    // size_t srclen;	/* (S) */
    // int currentline;	/* (l) */
    // int linedefined;	/* (S) */
    // int lastlinedefined;	/* (S) */
    // unsigned char nups;	/* (u) number of upvalues */
    // unsigned char nparams;/* (u) number of parameters */
    // char isvararg;        /* (u) */
    // char istailcall;	/* (t) */
    // unsigned short ftransfer;   /* (r) index of first value transferred */
    // unsigned short ntransfer;   /* (r) number of transferred values */
    // char short_src[LUA_IDSIZE]; /* (S) */

    @Override
    public LuaDebug read(long ptr, ByteBuffer buffer,
                         String name, String nameWhat,
                         String what, String source) {
        buffer.order(ByteOrder.nativeOrder());
        int event = buffer.getInt();
        if (LuaState.LONG_SIZE > 4) {
            buffer.position(buffer.position() + 4);
        }
        buffer.position(buffer.position() + (LuaState.LONG_SIZE << 2));
        long srcLen = readSizeT(buffer);
        int currentLine = buffer.getInt();
        int lineDefine = buffer.getInt();
        int lastLineDefine = buffer.getInt();
        short nUps = (short) Byte.toUnsignedInt(buffer.get());
        short nParams = (short) Byte.toUnsignedInt(buffer.get());
        byte isVarArg = buffer.get();
        byte isTailCall = buffer.get();
        int fTransfer = Short.toUnsignedInt(buffer.getShort());
        int nTransfer = Short.toUnsignedInt(buffer.getShort());
        String shortSrc = readShortSrc(buffer);

        return new LuaDebug(ptr, event,
                name, nameWhat, what, source,
                srcLen, currentLine, lineDefine,
                lastLineDefine, nUps, nParams,
                isVarArg, isTailCall, fTransfer,
                nTransfer, shortSrc);
    }
}
