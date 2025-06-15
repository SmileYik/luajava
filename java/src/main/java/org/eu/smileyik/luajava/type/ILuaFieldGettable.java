package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;

public interface ILuaFieldGettable extends ILuaObject {
    /**
     * If <code>this</code> is a table or userdata tries to get
     * a field value.
     */
    public default Result<LuaObject, ? extends LuaException> getField(String field) {
        if (isClosed()) return Result.failure(new LuaException("This lua state is closed!"));
        return getLuaState().getLuaObject((LuaObject) this, field);
    }
}
