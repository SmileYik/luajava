package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
import org.keplerproject.luajava.LuaException;

public interface ILuaCallable extends ILuaObject {
    /**
     * Calls the object represented by <code>this</code> using Lua function pcall. Returns 1 object
     *
     * @param args -
     *             Call arguments
     * @return Object - Returned Object
     */
    public default Result<Object, ? extends LuaException> call(Object... args) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        return getLuaState().pcall(this, args);
    }

    /**
     * Calls the object represented by <code>this</code> using Lua function pcall.
     *
     * @param nres -
     *             Number of objects returned
     * @param args -
     *             Call arguments
     * @return Object[] - Returned Objects
     */
    public default Result<Object[], ? extends LuaException> call(int nres, Object... args) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        return getLuaState().pcall(this, args, nres);
    }
}
