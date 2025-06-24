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
