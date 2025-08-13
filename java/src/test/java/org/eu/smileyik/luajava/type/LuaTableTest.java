/*
 * LuaTableTest.java, SmileYik, 2025-8-10
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

import org.eu.smileyik.luajava.*;
import org.eu.smileyik.luajava.exception.Result;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LuaTableTest extends BaseTest {

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

    @Test
    public void tableGetFieldTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end, e = {f = 5, g = 6}}\n" +
                "map2 = {a2 = 4}; map2[2] = 4; map2[map] = 3; map2[map.d] = 4";
        try (LuaStateFacade f = newLuaState()) {
            f.evalString(lua)
                    .mapResultValue(v -> f.getGlobal("map2", LuaTable.class)
                            .justCast(LuaTable.class, LuaException.class))
                    .ifSuccessThen(table -> {
                        LuaTable map = f.getGlobal("map", LuaTable.class).getOrSneakyThrow();
                        LuaFunction func = (LuaFunction) map.get("d").getOrSneakyThrow();

                        assertEquals(4.0, table.get(2).getOrSneakyThrow());
                        assertEquals(4.0, table.get("a2").getOrSneakyThrow());
                        assertEquals(3.0, table.get(map).getOrSneakyThrow());
                        assertEquals(4.0, table.get(func).getOrSneakyThrow());
                        func.call();
                    }).justThrow();
        }
    }

    @Test
    public void putTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end, e = {f = 5, g = 6}}\n" +
                "map2 = {a2 = 4}; map2[2] = 4; map2[map] = 3; map2[map.d] = 4";
        try (LuaStateFacade f = newLuaState()) {
            f.evalString(lua).getOrThrow();
            LuaTable table = f.getGlobal("map", LuaTable.class).getOrSneakyThrow();
            // put string
            table.put("f", "7").getOrThrow();
            assertEquals(table.get("f").getOrThrow(), "7");
            // put int
            table.put(8, "8").getOrThrow();
            assertEquals(table.get(8).getOrThrow(), "8");
        }
    }

}