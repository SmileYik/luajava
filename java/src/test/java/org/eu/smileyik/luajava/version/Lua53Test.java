/*
 * Lua53Test.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.version;

import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LoadLibrary;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFacade;
import org.keplerproject.luajava.LuaStateFactory;

import java.util.Random;

public class Lua53Test {
    static {
        LoadLibrary.load("lua-5.3.6");
    }

    @Test
    public void versionTest() {
        assert LuaState.LUA_VERSION.equals("Lua 5.3");
    }

    @Test
    public void arithTest() {
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            facade.openBase();

            Random random = new Random();
            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(100) - 50;
                int i2 = random.nextInt(100) - 50;
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPADD);
                assert facade.toNumber(-1) == i1 + i2 : "arith add failed";
                facade.pop(1);
            }

            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(100) - 50;
                int i2 = random.nextInt(100) - 50;
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPSUB);
                assert facade.toNumber(-1) == i1 - i2 : "arith sub failed";
                facade.pop(1);
            }

            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(100) - 50;
                int i2 = random.nextInt(100) - 50;
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPMUL);
                assert facade.toNumber(-1) == i1 * i2 : "arith multi failed";
                facade.pop(1);
            }

            int[] nums = new int[] { 1, 2, -2, 4, -4, 8, -8 };
            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(100) - 50;
                int i2 = nums[random.nextInt(nums.length)];
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPDIV);
                assert facade.toNumber(-1) == (double) i1 / i2 : String.format("arith mod failed: %d / %d = %f but got %f", i1, i2, (double) i1 / i2, facade.toNumber(-1));
                facade.pop(1);
            }

            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(100) + 1;
                int i2 = random.nextInt(100) + 1;
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPMOD);
                assert facade.toNumber(-1) == i1 % i2 : String.format("arith mod failed: %d %% %d = %d but got %f", i1, i2, i1 % i2, facade.toNumber(-1));
                facade.pop(1);
            }

            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(10) + 1;
                int i2 = random.nextInt(10);
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                facade.getLuaState().arith(LuaState.LUA_OPPOW);
                assert facade.toNumber(-1) == Math.pow(i1, i2) : String.format("arith pow failed: %d^%d = %f but got %f", i1, i2, Math.pow(i1, i2), facade.toNumber(-1));
                facade.pop(1);
            }

            for (int i = 0; i < 100; i++) {
                int i1 = random.nextInt(10000);
                facade.pushNumber(i1);
                facade.getLuaState().arith(LuaState.LUA_OPUNM);
                assert facade.toNumber(-1) == -i1 : "arith opunm failed";
                facade.pop(1);
            }
        }
    }

    @Test
    public void rotateTest() {
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            facade.openBase();
            facade.pushNumber(1);
            facade.pushNumber(2);
            facade.pushNumber(3);
            facade.pushNumber(4);
            facade.pushNumber(5);
            // 1 2 3 4 5
            //
            facade.getLuaState().rotate(1, 1);
            assert facade.toInteger(1) == 5 : "rotate failed";
            assert facade.toInteger(5) == 4 : "rotate failed";
            // 5 1 2 3 4
            facade.getLuaState().rotate(1, -1);
            assert facade.toInteger(1) == 1 : "rotate failed";
            assert facade.toInteger(-1) == 5 : "rotate failed";

        }
    }
}
