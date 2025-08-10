/*
 * LuajavaTest.java, SmileYik, 2025-8-10
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

package org.keplerproject.luajava;

import org.eu.smileyik.luajava.type.ILuaFieldGettable;
import org.junit.jupiter.api.Test;

public class LuajavaTest {

    static {
        LoadLibrary.load();
    }

    @org.junit.jupiter.api.Test
    public void consoleTest() {
        Console.main(new String[]{});
    }

    @Test
    public void luaObjectGcTest() throws InterruptedException {
        String lua = "map = {b = 2}";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        System.out.println(LuaState.LUA_VERSION);
        facade.lock(L -> {
            L.openLibs();
            int exp = L.LdoString(lua);
            System.out.println(exp);
            for (int i = 0; i < 10; i++) {
                LuaObject luaObject = facade.getLuaObject("map").getValue();
                assert luaObject instanceof ILuaFieldGettable;
                LuaObject b = null;
                try {
                    b = ((ILuaFieldGettable) luaObject)
                            .getField("b")
                            .getOrThrow(LuaException.class);
                } catch (LuaException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(b);
                b = null;
                luaObject = null;
            }
        });

        System.out.println("请求GC...");
        System.gc();
        Thread.sleep(2000); // 等待清理完成
        facade.close();
    }
}
