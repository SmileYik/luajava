/*
 * LuaState.java, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * $Id: LuaState.java,v 1.11 2007-09-17 19:28:40 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LuaState if the main class of LuaJava for the Java developer.
 * LuaState is a mapping of most of Lua's C API functions.
 * LuaState also provides many other functions that will be used to manipulate
 * objects between Lua and Java.
 *
 * @author Thiago Ponte
 */
public class LuaState {
    final public static String LUA_VERSION = _luaVersion();

    final public static int LUA_GLOBALSINDEX = _luaGlobalsIndex();
    final public static int LUA_REGISTRYINDEX = _luaRegistryIndex();
    final public static int LUA_TNONE = -1;
    final public static int LUA_TNIL = 0;
    final public static int LUA_TBOOLEAN = 1;
    final public static int LUA_TLIGHTUSERDATA = 2;
    final public static int LUA_TNUMBER = 3;
    final public static int LUA_TSTRING = 4;
    final public static int LUA_TTABLE = 5;
    final public static int LUA_TFUNCTION = 6;
    final public static int LUA_TUSERDATA = 7;
    final public static int LUA_TTHREAD = 8;
    /**
     * Specifies that an unspecified (multiple) number of return arguments
     * will be returned by a call.
     */
    final public static int LUA_MULTRET = -1;
    final public static int LUA_YIELD = 1;

    /*
     * error codes for `lua_load' and `lua_pcall'
     */
    /**
     * a runtime error.
     */
    final public static int LUA_ERRRUN = 2;
    /**
     * syntax error during pre-compilation.
     */
    final public static int LUA_ERRSYNTAX = 3;
    /**
     * memory allocation error. For such errors, Lua does not call
     * the error handler function.
     */
    final public static int LUA_ERRMEM = 4;
    /**
     * error while running the error handler function.
     */
    final public static int LUA_ERRERR = 5;
    // Gargabe Collection Functions
    final public static int LUA_GCSTOP = 0;
    final public static int LUA_GCRESTART = 1;
    final public static int LUA_GCCOLLECT = 2;
    final public static int LUA_GCCOUNT = 3;
    final public static int LUA_GCCOUNTB = 4;
    final public static int LUA_GCSTEP = 5;
    final public static int LUA_GCSETPAUSE = 6;
    final public static int LUA_GCSETSTEPMUL = 7;

    // since lua 5.2: arith
    public static final int LUA_OPADD;
    public static final int LUA_OPSUB;
    public static final int LUA_OPMUL;
    public static final int LUA_OPDIV;
    public static final int LUA_OPMOD;
    public static final int LUA_OPPOW;
    public static final int LUA_OPUNM;

    // since lua 5.3: arith
    public static final int LUA_OPIDI;
    public static final int LUA_OPBAN;
    public static final int LUA_OPBOR;
    public static final int LUA_OPBXO;
    public static final int LUA_OPSHL;
    public static final int LUA_OPSHR;
    public static final int LUA_OPBNO;

    // since lua 5.2: compare
    public static final int LUA_OPEQ = 0;
    public static final int LUA_OPLT = 1;
    public static final int LUA_OPLE = 2;

    private final static String LUAJAVA_LIB = "luajava-1.1";

    /**
     * Opens the library containing the luajava API
     */
    static {
        // Remove
        // System.loadLibrary(LUAJAVA_LIB);
        switch (LUA_VERSION) {
            case "Lua 5.2":
                LUA_OPADD = 0;
                LUA_OPSUB = 1;
                LUA_OPMUL = 2;
                LUA_OPDIV = 3;
                LUA_OPMOD = 4;
                LUA_OPPOW = 5;
                LUA_OPUNM = 6;
                // ignore
                LUA_OPIDI = 6;
                LUA_OPBAN = 7;
                LUA_OPBOR = 8;
                LUA_OPBXO = 9;
                LUA_OPSHL = 10;
                LUA_OPSHR = 11;
                LUA_OPBNO = 13;
                break;
            default:
                // since lua 5.3
                LUA_OPADD = 0;
                LUA_OPSUB = 1;
                LUA_OPMUL = 2;
                LUA_OPMOD = 3;
                LUA_OPPOW = 4;
                LUA_OPDIV = 5;
                LUA_OPIDI = 6;
                LUA_OPBAN = 7;
                LUA_OPBOR = 8;
                LUA_OPBXO = 9;
                LUA_OPSHL = 10;
                LUA_OPSHR = 11;
                LUA_OPUNM = 12;
                LUA_OPBNO = 13;
                break;
        }
    }

    private CPtr luaState;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Constructor to instance a new LuaState and initialize it with LuaJava's functions
     *
     * @param stateId
     */
    protected LuaState(int stateId) {
        luaState = _open();
        luajava_open(luaState, stateId);
    }

    /**
     * Receives a existing state and initializes it
     *
     * @param luaState
     */
    protected LuaState(CPtr luaState, int stateId) {
        this.luaState = luaState;
        luajava_open(luaState, stateId);
    }

    // LuaLibAux
    private static native int _LdoFile(CPtr ptr, String fileName);

    /**
     * Closes state and removes the object from the LuaStateFactory
     */
    protected void clearRef() {
        if (!isClosed() && closed.compareAndSet(false, true)) {
            _close(luaState);
            this.luaState = null;
        }
    }

    /**
     * Returns <code>true</code> if state is closed.
     */
    public boolean isClosed() {
        return luaState == null || luaState.getPeer() == 0 || closed.get();
    }

    /**
     * Return the long representing the LuaState pointer
     *
     * @return long
     */
    public long getCPtrPeer() {
        return (luaState != null) ? luaState.getPeer() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        LuaState luaState1 = (LuaState) object;
        return Objects.equals(luaState, luaState1.luaState);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(luaState);
    }

    /********************* Lua Native Interface *************************/

    private static native String _luaVersion();
    private static native int _luaRegistryIndex();
    private static native int _luaGlobalsIndex();

    private native CPtr _open();

    private native void _close(CPtr ptr);

    private native CPtr _newthread(CPtr ptr);

    // Stack manipulation
    private native int _getTop(CPtr ptr);

    private native void _setTop(CPtr ptr, int idx);

    private native void _pushValue(CPtr ptr, int idx);

    private native void _remove(CPtr ptr, int idx);

    private native void _insert(CPtr ptr, int idx);

    private native void _replace(CPtr ptr, int idx);

    private native int _checkStack(CPtr ptr, int sz);

    private native void _xmove(CPtr from, CPtr to, int n);

    // Access functions
    private native int _isNumber(CPtr ptr, int idx);

    private native int _isString(CPtr ptr, int idx);

    private native int _isCFunction(CPtr ptr, int idx);

    private native int _isUserdata(CPtr ptr, int idx);

    private native int _type(CPtr ptr, int idx);

    private native String _typeName(CPtr ptr, int tp);

    private native int _equal(CPtr ptr, int idx1, int idx2);

    private native int _rawequal(CPtr ptr, int idx1, int idx2);

    private native int _lessthan(CPtr ptr, int idx1, int idx2);

    private native double _toNumber(CPtr ptr, int idx);

    private native int _toInteger(CPtr ptr, int idx);

    private native int _toBoolean(CPtr ptr, int idx);

    private native String _toString(CPtr ptr, int idx);

    private native int _objlen(CPtr ptr, int idx);

    private native CPtr _toThread(CPtr ptr, int idx);

    // Push functions
    private native void _pushNil(CPtr ptr);

    private native void _pushNumber(CPtr ptr, double number);

    private native void _pushInteger(CPtr ptr, int integer);

    private native void _pushString(CPtr ptr, String str);

    private native void _pushString(CPtr ptr, byte[] bytes, int n);

    private native void _pushBoolean(CPtr ptr, int bool);

    // Get functions
    private native void _getTable(CPtr ptr, int idx);

    private native void _getField(CPtr ptr, int idx, String k);

    private native void _rawGet(CPtr ptr, int idx);

    private native void _rawGetI(CPtr ptr, int idx, int n);

    private native void _createTable(CPtr ptr, int narr, int nrec);

    private native int _getMetaTable(CPtr ptr, int idx);

    private native void _getFEnv(CPtr ptr, int idx);

    // Set functions
    private native void _setTable(CPtr ptr, int idx);

    private native void _setField(CPtr ptr, int idx, String k);

    private native void _rawSet(CPtr ptr, int idx);

    private native void _rawSetI(CPtr ptr, int idx, int n);

    private native int _setMetaTable(CPtr ptr, int idx);

    private native int _setFEnv(CPtr ptr, int idx);

    private native void _call(CPtr ptr, int nArgs, int nResults);

    private native int _pcall(CPtr ptr, int nArgs, int Results, int errFunc);

    // Coroutine Functions
    private native int _yield(CPtr ptr, int nResults);

    /**
     * removed since lua 5.2
     * @param ptr
     * @param nargs
     * @return
     */
    private native int _resume(CPtr ptr, int nargs);

    private native int _status(CPtr ptr);

    private native int _gc(CPtr ptr, int what, int data);

    // Miscellaneous Functions
    private native int _error(CPtr ptr);

    private native int _next(CPtr ptr, int idx);

    private native void _concat(CPtr ptr, int n);

    // Some macros
    private native void _pop(CPtr ptr, int n);

    private native void _newTable(CPtr ptr);

    private native int _strlen(CPtr ptr, int idx);

    private native int _isFunction(CPtr ptr, int idx);

    private native int _isTable(CPtr ptr, int idx);

    private native int _isNil(CPtr ptr, int idx);

    private native int _isBoolean(CPtr ptr, int idx);

    private native int _isThread(CPtr ptr, int idx);

    private native int _isNone(CPtr ptr, int idx);

    private native int _isNoneOrNil(CPtr ptr, int idx);

    private native void _setGlobal(CPtr ptr, String name);

    private native void _getGlobal(CPtr ptr, String name);

    private native int _getGcCount(CPtr ptr);
    //private native int _doBuffer(CPtr ptr, byte[] buff, long sz, String n);

    private native int _LdoString(CPtr ptr, String string);

    private native int _LgetMetaField(CPtr ptr, int obj, String e);

    private native int _LcallMeta(CPtr ptr, int obj, String e);

    private native int _Ltyperror(CPtr ptr, int nArg, String tName);

    private native int _LargError(CPtr ptr, int numArg, String extraMsg);

    private native String _LcheckString(CPtr ptr, int numArg);

    private native String _LoptString(CPtr ptr, int numArg, String def);

    private native double _LcheckNumber(CPtr ptr, int numArg);

    private native double _LoptNumber(CPtr ptr, int numArg, double def);

    private native int _LcheckInteger(CPtr ptr, int numArg);

    private native int _LoptInteger(CPtr ptr, int numArg, int def);

    private native void _LcheckStack(CPtr ptr, int sz, String msg);

    private native void _LcheckType(CPtr ptr, int nArg, int t);

    private native void _LcheckAny(CPtr ptr, int nArg);

    private native int _LnewMetatable(CPtr ptr, String tName);

    private native void _LgetMetatable(CPtr ptr, String tName);

    private native void _Lwhere(CPtr ptr, int lvl);

    private native int _Lref(CPtr ptr, int t);

    private native void _LunRef(CPtr ptr, int t, int ref);

    // luaL_getn 方法在 LuaJIT 2.1.1748459687 中不存在
    // private native int _LgetN(CPtr ptr, int t);

    // luaL_setn 方法在 LuaJIT 2.1.1748459687 中不存在
    // private native void _LsetN(CPtr ptr, int t, int n);

    private native int _LloadFile(CPtr ptr, String fileName);

    private native int _LloadBuffer(CPtr ptr, byte[] buff, long sz, String name);

    private native int _LloadString(CPtr ptr, String s);

    private native String _Lgsub(CPtr ptr, String s, String p, String r);

    private native String _LfindTable(CPtr ptr, int idx, String fname, int szhint);

    private native void _openBase(CPtr ptr);

    private native void _openTable(CPtr ptr);

    private native void _openIo(CPtr ptr);

    private native void _openOs(CPtr ptr);

    private native void _openString(CPtr ptr);

    private native void _openMath(CPtr ptr);

    private native void _openDebug(CPtr ptr);

    private native void _openPackage(CPtr ptr);

    // ******************** addition since lua 5.2 start ***********************
    /**
     * added since lua 5.2
     * @param ptr
     * @param idx
     * @return
     */
    private native int _rawlen(CPtr ptr, int idx);

    /**
     * added since lua 5.2
     * @param ptr
     * @param idx1
     * @param idx2
     * @param op
     * @return
     */
    private native int _compare(CPtr ptr, int idx1, int idx2, int op);

    private native void _arith(CPtr ptr, int op);

    private native void _len(CPtr ptr, int idx);

    /**
     * added since lua 5.2
     * @param ptr
     * @param threadPtr
     * @param nargs
     * @return
     */
    private native int _resume(CPtr ptr, CPtr threadPtr, int nargs);

    private native int _pushthread(CPtr ptr, CPtr threadPtr);

    private native void _setuservalue(CPtr ptr, int idx);
    private native void _getuservalue(CPtr ptr, int idx);
    private native int _absindex(CPtr ptr, int idx);
    private native void _openCoroutine(CPtr ptr);

    // ******************** addition since lua 5.2 stop ************************

    // ******************** addition since lua 5.3 start ***********************

    private native void _rotate(CPtr ptr, int idx, int n);

    private native void _openUtf8(CPtr ptr);

    // ******************** addition since lua 5.3 stop ************************

    // ******************** addition since lua 5.4 start ***********************

    public int getIUserValue(int idx, int n) {
        return _getiuservalue(luaState, idx, n);
    }

    public int setIUserValue(int idx, int n) {
        return _setiuservalue(luaState, idx, n);
    }

    public void warning(String msg, int tocont) {
        _warning(luaState, msg, tocont);
    }

    public int resume(LuaState threadL, int nargs, int nrets) {
        return _resume(luaState, threadL.luaState, nargs, nrets);
    }

    // ******************** addition since lua 5.4 stop ************************


    // Java Interface -----------------------------------------------------

    private native void _openLibs(CPtr ptr);

    // STACK MANIPULATION

    public CPtr newThread() {
        return _newthread(luaState);
    }

    public int getTop() {
        return _getTop(luaState);
    }

    public void setTop(int idx) {
        _setTop(luaState, idx);
    }

    public void pushValue(int idx) {
        _pushValue(luaState, idx);
    }

    public void remove(int idx) {
        _remove(luaState, idx);
    }

    public void insert(int idx) {
        _insert(luaState, idx);
    }

    public void replace(int idx) {
        _replace(luaState, idx);
    }

    public int checkStack(int sz) {
        return _checkStack(luaState, sz);
    }

    // ACCESS FUNCTION

    protected void xmove(LuaState to, int n) {
        _xmove(luaState, to.luaState, n);
    }

    public boolean isNumber(int idx) {
        return (_isNumber(luaState, idx) != 0);
    }

    public boolean isString(int idx) {
        return (_isString(luaState, idx) != 0);
    }

    public boolean isFunction(int idx) {
        return (_isFunction(luaState, idx) != 0);
    }

    public boolean isCFunction(int idx) {
        return (_isCFunction(luaState, idx) != 0);
    }

    public boolean isUserdata(int idx) {
        return (_isUserdata(luaState, idx) != 0);
    }

    public boolean isTable(int idx) {
        return (_isTable(luaState, idx) != 0);
    }

    public boolean isBoolean(int idx) {
        return (_isBoolean(luaState, idx) != 0);
    }

    public boolean isNil(int idx) {
        return (_isNil(luaState, idx) != 0);
    }

    public boolean isThread(int idx) {
        return (_isThread(luaState, idx) != 0);
    }

    public boolean isNone(int idx) {
        return (_isNone(luaState, idx) != 0);
    }

    public boolean isNoneOrNil(int idx) {
        return (_isNoneOrNil(luaState, idx) != 0);
    }

    public int type(int idx) {
        return _type(luaState, idx);
    }

    public String typeName(int tp) {
        return _typeName(luaState, tp);
    }

    public int equal(int idx1, int idx2) {
        return _equal(luaState, idx1, idx2);
    }

    public boolean rawequal(int idx1, int idx2) {
        return _rawequal(luaState, idx1, idx2) == 1;
    }

    public int lessthan(int idx1, int idx2) {
        return _lessthan(luaState, idx1, idx2);
    }

    public double toNumber(int idx) {
        return _toNumber(luaState, idx);
    }

    public int toInteger(int idx) {
        return _toInteger(luaState, idx);
    }

    public boolean toBoolean(int idx) {
        return (_toBoolean(luaState, idx) != 0);
    }

    public String toString(int idx) {
        return _toString(luaState, idx);
    }

    public int strLen(int idx) {
        return _strlen(luaState, idx);
    }

    public int objLen(int idx) {
        return _objlen(luaState, idx);
    }

    //PUSH FUNCTIONS

    public CPtr toThread(int idx) {
        return _toThread(luaState, idx);
    }

    public void pushNil() {
        _pushNil(luaState);
    }

    public void pushNumber(double db) {
        _pushNumber(luaState, db);
    }

    public void pushInteger(int integer) {
        _pushInteger(luaState, integer);
    }

    public void pushString(String str) {
        if (str == null)
            _pushNil(luaState);
        else
            _pushString(luaState, str);
    }

    public void pushString(byte[] bytes) {
        if (bytes == null)
            _pushNil(luaState);
        else
            _pushString(luaState, bytes, bytes.length);
    }

    // GET FUNCTIONS

    public void pushBoolean(boolean bool) {
        _pushBoolean(luaState, bool ? 1 : 0);
    }

    public void getTable(int idx) {
        _getTable(luaState, idx);
    }

    public void getField(int idx, String k) {
        _getField(luaState, idx, k);
    }

    public void rawGet(int idx) {
        _rawGet(luaState, idx);
    }

    public void rawGetI(int idx, int n) {
        _rawGetI(luaState, idx, n);
    }

    public void createTable(int narr, int nrec) {
        _createTable(luaState, narr, nrec);
    }

    public void newTable() {
        _newTable(luaState);
    }

    // if returns false, there is no metatable
    public boolean getMetaTable(int idx) {
        return _getMetaTable(luaState, idx) != 0;
    }

    // SET FUNCTIONS

    public void getFEnv(int idx) {
        _getFEnv(luaState, idx);
    }

    public void setTable(int idx) {
        _setTable(luaState, idx);
    }

    public void setField(int idx, String k) {
        _setField(luaState, idx, k);
    }

    public void rawSet(int idx) {
        _rawSet(luaState, idx);
    }

    public void rawSetI(int idx, int n) {
        _rawSetI(luaState, idx, n);
    }

    // if returns 0, cannot set the metatable to the given object
    public int setMetaTable(int idx) {
        return _setMetaTable(luaState, idx);
    }

    // if object is not a function returns 0
    public int setFEnv(int idx) {
        return _setFEnv(luaState, idx);
    }

    public void call(int nArgs, int nResults) {
        _call(luaState, nArgs, nResults);
    }

    // returns 0 if ok of one of the error codes defined
    public int pcall(int nArgs, int nResults, int errFunc) {
        return _pcall(luaState, nArgs, nResults, errFunc);
    }

    public int yield(int nResults) {
        return _yield(luaState, nResults);
    }

    /**
     * removed since lua 5.2
     * @param nArgs
     * @return
     */
    public int resume(int nArgs) {
        return _resume(luaState, nArgs);
    }

    public int status() {
        return _status(luaState);
    }

    public int gc(int what, int data) {
        return _gc(luaState, what, data);
    }

    public int getGcCount() {
        return _getGcCount(luaState);
    }

    public int next(int idx) {
        return _next(luaState, idx);
    }

    public int error() {
        return _error(luaState);
    }

    public void concat(int n) {
        _concat(luaState, n);
    }

    // FUNCTION FROM lauxlib
    // returns 0 if ok
    public int LdoFile(String fileName) {
        return _LdoFile(luaState, fileName);
    }

    // returns 0 if ok
    public int LdoString(String str) {
        return _LdoString(luaState, str);
    }

    public int LgetMetaField(int obj, String e) {
        return _LgetMetaField(luaState, obj, e);
    }

    public int LcallMeta(int obj, String e) {
        return _LcallMeta(luaState, obj, e);
    }

    public int Ltyperror(int nArg, String tName) {
        return _Ltyperror(luaState, nArg, tName);
    }

    public int LargError(int numArg, String extraMsg) {
        return _LargError(luaState, numArg, extraMsg);
    }

    public String LcheckString(int numArg) {
        return _LcheckString(luaState, numArg);
    }

    public String LoptString(int numArg, String def) {
        return _LoptString(luaState, numArg, def);
    }

    public double LcheckNumber(int numArg) {
        return _LcheckNumber(luaState, numArg);
    }

    public double LoptNumber(int numArg, double def) {
        return _LoptNumber(luaState, numArg, def);
    }

    public int LcheckInteger(int numArg) {
        return _LcheckInteger(luaState, numArg);
    }

    public int LoptInteger(int numArg, int def) {
        return _LoptInteger(luaState, numArg, def);
    }

    public void LcheckStack(int sz, String msg) {
        _LcheckStack(luaState, sz, msg);
    }

    public void LcheckType(int nArg, int t) {
        _LcheckType(luaState, nArg, t);
    }

    public void LcheckAny(int nArg) {
        _LcheckAny(luaState, nArg);
    }

    public int LnewMetatable(String tName) {
        return _LnewMetatable(luaState, tName);
    }

    public void LgetMetatable(String tName) {
        _LgetMetatable(luaState, tName);
    }

    public void Lwhere(int lvl) {
        _Lwhere(luaState, lvl);
    }

    public int Lref(int t) {
        return _Lref(luaState, t);
    }

    public void LunRef(int t, int ref) {
        _LunRef(luaState, t, ref);
    }

    // luaL_getn 方法在 LuaJIT 2.1.1748459687 中不存在
    // public int LgetN(int t) {
    //     return _LgetN(luaState, t);
    // }

    // luaL_setn 方法在 LuaJIT 2.1.1748459687 中不存在
    // public void LsetN(int t, int n) {
    //     _LsetN(luaState, t, n);
    // }

    public int LloadFile(String fileName) {
        return _LloadFile(luaState, fileName);
    }

    public int LloadString(String s) {
        return _LloadString(luaState, s);
    }

    public int LloadBuffer(byte[] buff, String name) {
        return _LloadBuffer(luaState, buff, buff.length, name);
    }

    public String Lgsub(String s, String p, String r) {
        return _Lgsub(luaState, s, p, r);
    }

    //IMPLEMENTED C MACROS

    public String LfindTable(int idx, String fname, int szhint) {
        return _LfindTable(luaState, idx, fname, szhint);
    }

    public void pop(int n) {
        //setTop(- (n) - 1);
        _pop(luaState, n);
    }

    public void getGlobal(String global) {
//    pushString(global);
//    getTable(LUA_GLOBALSINDEX.intValue());
        _getGlobal(luaState, global);
    }

    /**
     * Pops a value from the stack and sets it as the new value of global name.
     * @param name
     */
    public void setGlobal(String name) {
        //pushString(name);
        //insert(-2);
        //setTable(LUA_GLOBALSINDEX.intValue());
        _setGlobal(luaState, name);
    }

    // Functions to open lua libraries
    public void openBase() {
        _openBase(luaState);
    }

    public void openTable() {
        _openTable(luaState);
    }

    public void openIo() {
        _openIo(luaState);
    }

    public void openOs() {
        _openOs(luaState);
    }

    public void openString() {
        _openString(luaState);
    }

    public void openMath() {
        _openMath(luaState);
    }

    public void openDebug() {
        _openDebug(luaState);
    }

    public void openPackage() {
        _openPackage(luaState);
    }

    // ******************** addition since lua 5.2 start ***********************
    /**
     * added since lua 5.2
     * @param idx
     * @return
     */
    public int rawLen(int idx) {
        return _rawlen(luaState, idx);
    }

    /**
     * added since lua 5.2
     * @param idx1
     * @param idx2
     * @param op
     * @return
     */
    public boolean compare(int idx1, int idx2, int op) {
        return 0 != _compare(luaState, idx1, idx2, op);
    }

    public void arith(int op) {
        _arith(luaState, op);
    }

    /**
     * get the target object length.
     * @param idx target object index.
     */
    public void len(int idx) {
        _len(luaState, idx);
    }

    /**
     * added since lua 5.2
     * @param thread
     * @param nargs
     * @return
     */
    public int resume(LuaState thread, int nargs) {
        return _resume(luaState, thread.luaState, nargs);
    }

    public int pushThread(LuaState thread) {
        return _pushthread(luaState, thread.luaState);
    }

    /**
     * set uservalue to target userdata.
     * @apiNote the uservalue must be nil or a lua table.
     * @param idx userdata index
     */
    public void setUserValue(int idx) {
        _setuservalue(luaState, idx);
    }

    /**
     * get uservalue from target userdata.
     * @param idx userdata index
     */
    public void getUserValue(int idx) {
        _getuservalue(luaState, idx);
    }

    /**
     * get the absolute index.
     * @param idx idx
     * @return index.
     */
    public int absIndex(int idx) {
        return _absindex(luaState, idx);
    }

    public void openCoroutine() {
        _openCoroutine(luaState);
    }

    // ******************** addition since lua 5.2 stop ***********************

    // ******************** addition since lua 5.3 start ***********************

    public void rotate(int idx, int n) {
        _rotate(luaState, idx, n);
    }

    public void openUtf8() {
        _openUtf8(luaState);
    }

    // ******************** addition since lua 5.3 stop ************************

    // ******************** addition since lua 5.4 start ***********************

    private native int _getiuservalue(CPtr ptr, int idx, int n);
    private native int _setiuservalue(CPtr ptr, int idx, int n);
    private native void _warning(CPtr ptr, String msg, int tocont);
    private native int _resume(CPtr ptr, CPtr threadPtr, int nargs, int nrets);

    // ******************** addition since lua 5.4 stop ************************


    /********************** Luajava API Library **********************/

    public void openLibs() {
        _openLibs(luaState);
    }

    /**
     * Initializes lua State to be used by luajava
     *
     * @param cptr
     * @param stateId
     */
    private native void luajava_open(CPtr cptr, int stateId);

    /**
     * Gets a Object from a userdata
     *
     * @param L
     * @param idx index of the lua stack
     * @return Object
     */
    private native Object _getObjectFromUserdata(CPtr L, int idx) throws LuaException;

    /**
     * Returns whether a userdata contains a Java Object
     *
     * @param L
     * @param idx index of the lua stack
     * @return boolean
     */
    private native boolean _isObject(CPtr L, int idx);

    /**
     * Pushes a Java Object into the state stack
     *
     * @param L
     * @param obj
     */
    private native void _pushJavaObject(CPtr L, Object obj);

    /**
     * Pushes a Java Class into the state stack
     *
     * @param L
     * @param clazz
     */
    private native void _pushJavaClass(CPtr L, Class<?> clazz);

    /**
     * Pushes a Java Array into the state stack
     *
     * @param L
     * @param obj
     */
    private native void _pushJavaArray(CPtr L, Object obj);

    /**
     * Pushes a JavaFunction into the state stack
     *
     * @param L
     * @param func
     */
    private native void _pushJavaFunction(CPtr L, JavaFunction func) throws LuaException;

    /**
     * Returns whether a userdata contains a Java Function
     *
     * @param L
     * @param idx index of the lua stack
     * @return boolean
     */
    private native boolean _isJavaFunction(CPtr L, int idx);

    /**
     * Gets a Object from Lua
     *
     * @param idx index of the lua stack
     * @return Object
     * @throws LuaException if the lua object does not represent a java object.
     */
    public Object getObjectFromUserdata(int idx) throws LuaException {
        return _getObjectFromUserdata(luaState, idx);
    }

    /**
     * Tells whether a lua index contains a java Object
     *
     * @param idx index of the lua stack
     * @return boolean
     */
    public boolean isObject(int idx) {
        return _isObject(luaState, idx);
    }

    /**
     * Pushes a Java Class into the lua stack.<br>
     *
     * @param clazz Java Class instance to be pushed into lua
     */
    public void pushJavaClass(Class<?> clazz) {
        _pushJavaClass(luaState, clazz);
    }

    /**
     * Pushes a Java Object into the lua stack.<br>
     * This function does not check if the object is from a class that could
     * be represented by a lua type. Eg: java.lang.String could be a lua string.
     *
     * @param obj Object to be pushed into lua
     */
    public void pushJavaObject(Object obj) {
        _pushJavaObject(luaState, obj);
    }

    public void pushJavaArray(Object obj) throws LuaException {
        if (!obj.getClass().isArray())
            throw new LuaException("Object is not an array.");

        _pushJavaArray(luaState, obj);
    }

    /**
     * Pushes a JavaFunction into the state stack
     *
     * @param func
     */
    public void pushJavaFunction(JavaFunction func) throws LuaException {
        _pushJavaFunction(luaState, func);
    }

    /**
     * Returns whether a userdata contains a Java Function
     *
     * @param idx index of the lua stack
     * @return boolean
     */
    public boolean isJavaFunction(int idx) {
        return _isJavaFunction(luaState, idx);
    }
}
