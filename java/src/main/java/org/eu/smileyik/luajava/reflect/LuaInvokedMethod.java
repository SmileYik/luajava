/*
 * LuaInvokedMethod.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.reflect;

import java.util.HashMap;
import java.util.Map;

public class LuaInvokedMethod<T> {
    private T executable;
    private final Map<Integer, Object> overwriteParams;

    public LuaInvokedMethod(LuaInvokedMethod<T> other) {
        this.executable = (other.executable instanceof Copiable) ?
                (T) ((Copiable) other.executable).copy() : other.executable;
        this.overwriteParams = new HashMap<Integer, Object>(other.overwriteParams);
    }

    public LuaInvokedMethod() {
        this.overwriteParams = new HashMap<>();
    }

    public void reset(T executable) {
        this.executable = executable;
        this.overwriteParams.clear();
    }

    public void clear() {
        this.executable = null;
    }

    public T getExecutable() {
        return executable;
    }

    public Map<Integer, Object> getOverwriteParams() {
        return overwriteParams;
    }

    public void overwriteParam(int key, Object value) {
        overwriteParams.put(key, value);
    }

    public LuaInvokedMethod<T> done() {
        return new LuaInvokedMethod<>(this);
    }
}
