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
