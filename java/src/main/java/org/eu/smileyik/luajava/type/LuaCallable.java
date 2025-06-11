package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
import org.keplerproject.luajava.LuaException;

public interface LuaCallable extends ILuaObject {
    public default Result<Object, ? extends LuaException> call(Object... args) {
        return getLuaState().pcall(this, args);
    }

    public default Result<Object[], ? extends LuaException> call(int nres, Object... args) {
        return getLuaState().pcall(this, args, nres);
    }
}
