/*
 * InnerTypeHelper.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFacade;

import java.util.Optional;

public class InnerTypeHelper {

    /**
     * Create the subclass of LuaObject.
     * @param luaState  Lua state
     * @param idx       Index
     * @return If returned empty then need fallback and create LuaObject.
     */
    public static Optional<LuaObject> createLuaObject(LuaStateFacade luaState, int idx) {
        return luaState.lock(l -> {
            int type = l.type(idx);
            switch (type) {
                case LuaType.FUNCTION:
                    return Optional.of(new LuaFunction(luaState, idx));
                case LuaType.TABLE:
                    return Optional.of(LuaTable.createTable(luaState, idx));
                case LuaType.BOOLEAN:
                    return Optional.of(new LuaBoolean(luaState, idx));
                case LuaType.NUMBER:
                    return Optional.of(new LuaNumber(luaState, idx));
                case LuaType.USERDATA:
                    return Optional.of(new LuaUserdata(luaState, idx));
                case LuaType.STRING:
                    return Optional.of(new LuaString(luaState, idx));
            }
            return Optional.empty();
        });
    }
}
