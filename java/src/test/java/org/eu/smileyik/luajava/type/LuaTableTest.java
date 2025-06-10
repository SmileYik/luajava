package org.eu.smileyik.luajava.type;

import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.*;

import java.util.Objects;

class LuaTableTest {

    static {
        LoadLibrary.load();
    }

    @Test
    public void forEachTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end}";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.lockThrow(L -> {
            L.openLibs();
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            LuaObject luaObject = facade.getLuaObject("map").getOrThrow(LuaException.class);

            assert luaObject instanceof LuaTable;
            LuaTable table = (LuaTable) luaObject;
            try {
                table.forEach((key, value) -> {
                    System.out.printf("Key: %s; %s\nValue: %s; %s\n",
                            Objects.toString(key), key.getClass(),
                            Objects.toString(value), value.getClass());
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println(((LuaTable) luaObject).isArray());
        });

        facade.close();
    }

    @Test
    public void asMapTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end, e = {f = 5, g = 6}}\n" +
                "map2 = {a2 = 4}; map2[2] = 4; map2[map] = 3; map2[map.d] = 4";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.lockThrow(L -> {
            try {
                L.openLibs();
                int exp = L.LdoString(lua);
                if (exp != 0) {
                    throw new LuaException(L.toString(-1));
                }
                LuaObject luaObject = facade.getLuaObject("map").getOrThrow();

                assert luaObject instanceof LuaTable;
                LuaTable table = (LuaTable) luaObject;
                System.out.println(table.asMap());
                System.out.println(table.asDeepMap());
                System.out.println(table.asStringMap(Object.class));

                table = (LuaTable) facade.getLuaObject("map2").getOrThrow();
                System.out.println(table.asMap());
                System.out.println(table.asDeepMap());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        facade.close();
    }

}