/*
 * ILuaCallable.java, SmileYik, 2025-8-10
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
