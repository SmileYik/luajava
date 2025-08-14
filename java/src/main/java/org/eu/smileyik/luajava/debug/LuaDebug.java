package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.LuaState;

import java.nio.ByteBuffer;

public class LuaDebug {
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

    private static final LuaDebugReader LUA_DEBUG_READER;

    static {
        LuaDebugReader reader = null;
        switch (LuaState.LUA_VERSION) {
            case "Lua 5.1":
                reader = new LuaDebugReaderLua51();
                break;
            case "Lua 5.2":
            case "Lua 5.3":
                reader = new LuaDebugReaderLua52();
                break;
            case "Lua 5.4":
            default:
                reader = new LuaDebugReaderLua54();
                break;
        }
        LUA_DEBUG_READER = reader;
    }

    private final long ptr;

    private final int event;
    private final String name;
    private final String nameWhat;
    private final String what;
    private final String source;
    private final long srcLen;
    private final int currentLine;
    private final int lineDefine;
    private final int lastLineDefine;
    private final int nUps;
    private final short nParams;
    private final byte isVarArg;
    private final byte isTailCall;
    private final int fTransfer;
    private final int nTransfer;
    private final String shortSrc;

    public LuaDebug(long ptr) {
        this(ptr, 0, (String) null, (String) null, (String) null, (String) null,
                0L, 0, 0, 0, 0,
                (short) 0, (byte) 0, (byte) 0, 0, 0, (String) null);
    }

    public LuaDebug(long ptr, int event, String name, String nameWhat, String what, String source,
                    long srcLen, int currentLine, int lineDefine, int lastLineDefine, int nUps,
                    short nParams, byte isVarArg, byte isTailCall, int fTransfer, int nTransfer, String shortSrc) {
        this.ptr = ptr;
        this.event = event;
        this.name = name;
        this.nameWhat = nameWhat;
        this.what = what;
        this.source = source;
        this.srcLen = srcLen;
        this.currentLine = currentLine;
        this.lineDefine = lineDefine;
        this.lastLineDefine = lastLineDefine;
        this.nUps = nUps;
        this.nParams = nParams;
        this.isVarArg = isVarArg;
        this.isTailCall = isTailCall;
        this.fTransfer = fTransfer;
        this.nTransfer = nTransfer;
        this.shortSrc = shortSrc;
    }

    public static LuaDebug newInstance(long ptr, ByteBuffer buffer,
                                       String name, String nameWhat, String what, String source) {
        return LUA_DEBUG_READER.read(ptr, buffer, name, nameWhat, what, source);
    }

    public long getPtr() {
        return ptr;
    }

    public int getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public String getNameWhat() {
        return nameWhat;
    }

    public String getWhat() {
        return what;
    }

    public String getSource() {
        return source;
    }

    public long getSrcLen() {
        return srcLen;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getLineDefine() {
        return lineDefine;
    }

    public int getLastLineDefine() {
        return lastLineDefine;
    }

    public int getnUps() {
        return nUps;
    }

    public short getnParams() {
        return nParams;
    }

    public byte getIsVarArg() {
        return isVarArg;
    }

    public byte getIsTailCall() {
        return isTailCall;
    }

    public int getfTransfer() {
        return fTransfer;
    }

    public int getnTransfer() {
        return nTransfer;
    }

    public String getShortSrc() {
        return shortSrc;
    }

    @Override
    public String toString() {
        return "LuaDebug{" +
                "ptr=" + ptr +
                ", event=" + event +
                ", name='" + name + '\'' +
                ", nameWhat='" + nameWhat + '\'' +
                ", what='" + what + '\'' +
                ", source='" + source + '\'' +
                ", srcLen=" + srcLen +
                ", currentLine=" + currentLine +
                ", lineDefine=" + lineDefine +
                ", lastLineDefine=" + lastLineDefine +
                ", nUps=" + nUps +
                ", nParams=" + nParams +
                ", isVarArg=" + isVarArg +
                ", isTailCall=" + isTailCall +
                ", fTransfer=" + fTransfer +
                ", nTransfer=" + nTransfer +
                ", shortSrc='" + shortSrc + '\'' +
                '}';
    }
}
