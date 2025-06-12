package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.exception.Result;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LuaTableTest {

    static {
        LoadLibrary.load();
    }

    private void doForEachCheck(LuaTable table) {
        AtomicInteger count = new AtomicInteger(0);
        table.forEach((key, value) -> {
            try {
                assertInstanceOf(String.class, key);
                switch ((String) key) {
                    case "b":
                        count.incrementAndGet();
                        assertInstanceOf(Double.class, value, "map.b should be Double");
                        assertEquals(value, 2d, "map.b should be 2");
                        break;
                    case "a":
                        count.incrementAndGet();
                        assertInstanceOf(Double.class, value, "map.a should be Double");
                        assertEquals(value, 1d, "map.a should be 1");
                        break;
                    case "c":
                        count.incrementAndGet();
                        assertInstanceOf(String.class, value, "map.c should be String");
                        assertEquals(value, "3", "map.c should be '3'");
                        break;
                    case "d":
                        count.incrementAndGet();
                        assertInstanceOf(LuaFunction.class, value, "map.d should be LuaFunction");
                        break;
                    default:
                        throw new AssertionError("Unknown key: " + key);
                }
            } finally {
                if (value instanceof LuaObject) {
                    ((LuaObject) value).close();
                }
            }
        });
        assertEquals(count.get(), 4, "map entry count should be 4");
    }

    @Test
    public void forEachWithChainTest() throws Exception {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end}";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        facade.evalString(lua)
                .mapResultValue(v -> facade.getLuaObject("map"))
                .mapResultValue(luaObject -> {
                    assertInstanceOf(LuaTable.class, luaObject, "map must be LuaTable");
                    return Result.success((LuaTable) luaObject);
                })
                .mapResultValue(table -> {
                    doForEachCheck(table);
                    return Result.success();
                })
                .justThrow();
        facade.close();
    }

    @Test
    public void forEachTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end}";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        facade.evalString(lua).replaceErrorString(it -> "Error: " + it).getOrThrow();
        LuaObject luaObject = facade.getLuaObject("map").getOrThrow();
        assert luaObject instanceof LuaTable;
        LuaTable table = (LuaTable) luaObject;
        doForEachCheck(table);
        facade.close();
    }

    @Test
    public void asMapTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end, e = {f = 5, g = 6}}\n" +
                "map2 = {a2 = 4}; map2[2] = 4; map2[map] = 3; map2[map.d] = 4";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.lockThrowAll(L -> {
            L.openLibs();
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            LuaObject luaObject = facade.getLuaObject("map").getOrThrow();

            assert luaObject instanceof LuaTable;
            LuaTable table = (LuaTable) luaObject;
            System.out.println(table.asMap().getOrThrow());
            System.out.println(table.asDeepMap().getOrThrow());
            System.out.println(table.asStringMap(Object.class).getOrThrow());

            table = (LuaTable) facade.getLuaObject("map2").getOrThrow();
            System.out.println(table.asMap().getOrThrow());
            System.out.println(table.asDeepMap().getOrThrow());
            LuaTable finalTable = table;
            assertThrows(ClassCastException.class, () -> {
                finalTable.asDeepMap(String.class, String.class).justThrow();
            });
        }).getOrThrow();

        facade.close();
    }

}