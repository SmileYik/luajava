package org.eu.smileyik.luajava.debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class LuaDebug {
    //int event;
    //const char *name;	/* (n) */
    //const char *namewhat;	/* (n) 'global', 'local', 'field', 'method' */
    //const char *what;	/* (S) 'Lua', 'C', 'main', 'tail' */
    //const char *source;	/* (S) */
    //size_t srclen;	/* (S) */
    //int currentline;	/* (l) */
    //int linedefined;	/* (S) */
    //int lastlinedefined;	/* (S) */
    //unsigned char nups;	/* (u) number of upvalues */
    //unsigned char nparams;/* (u) number of parameters */
    //char isvararg;        /* (u) */
    //char istailcall;	/* (t) */
    //unsigned short ftransfer;   /* (r) index of first value transferred */
    //unsigned short ntransfer;   /* (r) number of transferred values */
    //char short_src[LUA_IDSIZE]; /* (S) */
    private long ptr;

    private int event;
    private String name;
    private String nameWhat;
    private String what;
    private String source;
    private long srcLen;
    private int currentLine;
    private int lineDefine;
    private int lastLineDefine;
    private short nUps;
    private short nParams;
    private byte isVarArg;
    private byte isTailCall;
    private int fTransfer;
    private int nTransfer;
    private String shortSrc;

    private LuaDebug() {

    }

    public LuaDebug(long ptr, int event, String name, String nameWhat, String what, String source, long srcLen, int currentLine, int lineDefine, int lastLineDefine, short nUps, short nParams, byte isVarArg, byte isTailCall, int fTransfer, int nTransfer, String shortSrc) {
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

    public static LuaDebug newInstance(long ptr, ByteBuffer luaDebug, int longLen,
                                       String name, String nameWhat, String what, String source) {
        LuaDebug debug = new LuaDebug();
        luaDebug.order(ByteOrder.nativeOrder());
        debug.ptr = ptr;
        debug.event = luaDebug.getInt(); luaDebug.getInt();
        debug.name = name;         luaDebug.getLong();
        debug.nameWhat = nameWhat; luaDebug.getLong();
        debug.what = what;         luaDebug.getLong();
        debug.source = source;     luaDebug.getLong();
        if (longLen == 8) {
            debug.srcLen = luaDebug.getLong();
        } else {
            debug.srcLen = luaDebug.getInt();
        }
        debug.currentLine = luaDebug.getInt();
        debug.lineDefine = luaDebug.getInt();
        debug.lastLineDefine = luaDebug.getInt();
        debug.nUps = (short) Byte.toUnsignedInt(luaDebug.get());
        debug.nParams = (short) Byte.toUnsignedInt(luaDebug.get());
        debug.isVarArg = luaDebug.get();
        debug.isTailCall = luaDebug.get();
        debug.fTransfer = Short.toUnsignedInt(luaDebug.getShort());
        debug.nTransfer = Short.toUnsignedInt(luaDebug.getShort());
        byte[] shortSrc = new byte[luaDebug.remaining() - 8 - longLen];
        luaDebug.get(shortSrc);
        int len = 0;
        while (len < shortSrc.length && shortSrc[len] != 0) {
            len++;
        }
        debug.shortSrc = new String(shortSrc, 0, len, StandardCharsets.UTF_8);
        return debug;
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

    public short getnUps() {
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
