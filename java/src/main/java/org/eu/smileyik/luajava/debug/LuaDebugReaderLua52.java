package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.LuaState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LuaDebugReaderLua52 implements LuaDebugReader {

    // struct lua_Debug {
    //     int event;
    //     const char *name;	/* (n) */
    //     const char *namewhat;	/* (n) 'global', 'local', 'field', 'method' */
    //     const char *what;	/* (S) 'Lua', 'C', 'main', 'tail' */
    //     const char *source;	/* (S) */
    //     int currentline;	/* (l) */
    //     int linedefined;	/* (S) */
    //     int lastlinedefined;	/* (S) */
    //     unsigned char nups;	/* (u) number of upvalues */
    //     unsigned char nparams;/* (u) number of parameters */
    //     char isvararg;        /* (u) */
    //     char istailcall;	/* (t) */
    //     char short_src[LUA_IDSIZE]; /* (S) */
    //     /* private part */
    //     struct CallInfo *i_ci;  /* active function */
    // };

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
        int currentLine = buffer.getInt();
        int lineDefine = buffer.getInt();
        int lastLineDefine = buffer.getInt();
        short nUps = (short) Byte.toUnsignedInt(buffer.get());
        short nParams = (short) Byte.toUnsignedInt(buffer.get());
        byte isVarArg = buffer.get();
        byte isTailCall = buffer.get();
        String shortSrc = readShortSrc(buffer);

        return new LuaDebug(ptr, event,
                name, nameWhat, what, source,
                0, currentLine, lineDefine,
                lastLineDefine, nUps, nParams,
                isVarArg, isTailCall, 0,
                0, shortSrc);
    }
}
