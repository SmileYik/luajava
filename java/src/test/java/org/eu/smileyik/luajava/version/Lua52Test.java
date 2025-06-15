package org.eu.smileyik.luajava.version;

import org.eu.smileyik.luajava.type.LuaTable;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LoadLibrary;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFacade;
import org.keplerproject.luajava.LuaStateFactory;

import java.util.Random;

public class Lua52Test {
    static {
        LoadLibrary.load("lua-5.2.4");
    }

    @Test
    public void versionTest() {
        assert LuaState.LUA_VERSION.equals("Lua 5.2");
    }

    @Test
    public void setTableUserValueTest() {
        Object obj = new Object();
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            facade.lockThrow(l -> {
                facade.openBase();
                facade.pushObjectValue(obj).getOrSneakyThrow();
                facade.newTable();
                facade.pushString("name");
                facade.pushObjectValue(obj).getOrSneakyThrow();
                facade.setTable(-3);
                facade.getLuaState().setUserValue(-2);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUserValueTest() {
        Object obj = new Object();
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            facade.openBase();
            facade.pushObjectValue(obj).getOrSneakyThrow();
            facade.newTable();
            facade.pushString("name");
            facade.pushObjectValue(obj).getOrSneakyThrow();
            facade.setTable(-3);
            System.out.println(facade.isTable(-1));
            facade.getLuaState().setUserValue(-2);
            assert facade.getTop() == 1;
            facade.getLuaState().getUserValue(-1);
            assert facade.getTop() == 2;
            assert facade.isTable(-1);
            LuaTable table = (LuaTable) facade.toJavaObject(-1).getOrSneakyThrow();
            Object name = table.get("name").getOrSneakyThrow();
            assert name == obj;
            table.close();
            facade.pop(1);
            facade.pushNil();
            facade.getLuaState().setUserValue(-2);
            assert facade.getTop() == 1;
            facade.getLuaState().getUserValue(-1);
            assert facade.isNil(-1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void lenTest() {
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            facade.openBase();
            facade.evalString("map = {a = 1, b = 2, c = '3'}").getOrSneakyThrow();
            facade.getLuaState().getGlobal("map");
            assert facade.getTop() == 1;
            facade.getLuaState().len(-1);
            assert facade.getTop() == 2;
            assert facade.isNumber(-1);
            assert facade.toInteger(-1) == 0;
            facade.evalString("array = {'1', 2, 3, '4', function() end}").getOrSneakyThrow();
            facade.getLuaState().getGlobal("array");
            facade.getLuaState().len(-1);
            assert facade.toInteger(-1) == 5;
        }
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
                assert facade.toNumber(-1) == (double) i1 / i2 : "arith div failed";
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
    public void compareTest() {
        try (LuaStateFacade facade = LuaStateFactory.newLuaState()) {
            Random random = new Random();
            for (int i = 0; i < 10000; i++) {
                int i1 = random.nextInt(10);
                int i2 = random.nextInt(10);
                facade.pushNumber(i1);
                facade.pushNumber(i2);
                boolean resultLt = facade.getLuaState().compare(-2, -1, LuaState.LUA_OPLT);
                boolean resultLe = facade.getLuaState().compare(-2, -1, LuaState.LUA_OPLE);
                boolean resultEq = facade.getLuaState().compare(-2, -1, LuaState.LUA_OPEQ);
                assert (resultLt) == (i1 < i2) :
                        String.format("compare failed: %d < %d = %s but got %s", i1, i2, i1 < i2, resultLt);
                assert (resultLe) == (i1 <= i2) :
                        String.format("compare failed: %d <= %d = %s but got %s", i1, i2, i1 <= i2, resultLe);
                assert (resultEq) == (i1 == i2) :
                        String.format("compare failed: %d == %d = %s but got %s", i1, i2, i1 == i2, resultEq);
                facade.pop(2);
            }
        }
    }
}
