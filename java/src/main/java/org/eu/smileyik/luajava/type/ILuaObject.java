/*
 * ILuaObject.java, SmileYik, 2025-8-10
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

import org.keplerproject.luajava.LuaStateFacade;

public interface ILuaObject {
    /**
     * get lua state.
     * @return the lua state. if lua state is closed then may return null.
     */
    LuaStateFacade getLuaState();

    /**
     * return this object is closed or not.
     * @return if closed then return true.
     */
    boolean isClosed();

    /**
     * push this object to the lua stack top with lock.
     */
    public void push();

    /**
     * get this object's type.
     * @return
     */
    public int type();

    public default boolean isBoolean() {
        return type() == LuaType.BOOLEAN;
    }

    public default boolean isNumber() {
        return type() == LuaType.NUMBER;
    }

    public default boolean isString() {
        return type() == LuaType.STRING;
    }

    public default boolean isNil() {
        return type() == LuaType.NIL;
    }

    public default boolean isTable() {
        return type() == LuaType.TABLE;
    }

    public default boolean isFunction() {
        return type() == LuaType.FUNCTION;
    }

    public default boolean isUserdata() {
        return type() == LuaType.USERDATA;
    }
}
