package org.eu.smileyik.luajava.test;

import org.eu.smileyik.luajava.BaseTest;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringTest extends BaseTest {

    public static String a = "😀";
    public static String b = "Hello 你好啊!";
    public static String c = "Hello";
    public static String d = "Halo";
    public static String e = "你好";
    public static String f = "你好呀";
    public static String lua = "a = \"😀\"\n" +
            "b = \"Hello 你好啊!\"\n" +
            "c = \"Hello\"\n" +
            "d = \"Halo\"\n" +
            "e = \"你好\"\n" +
            "f = \"你好呀\"\n" +
            "print(a, b, c, d, e, f)";

    @Test
    public void test() {
        LuaStateFacade facade = newLuaState();
        facade.evalString(lua);
        assertEquals(a, facade.getGlobal("a").getOrSneakyThrow());
        assertEquals(b, facade.getGlobal("b").getOrSneakyThrow());
        assertEquals(c, facade.getGlobal("c").getOrSneakyThrow());
        assertEquals(d, facade.getGlobal("d").getOrSneakyThrow());
        assertEquals(e, facade.getGlobal("e").getOrSneakyThrow());
        assertEquals(f, facade.getGlobal("f").getOrSneakyThrow());
    }

    @Test
    public void testFile() {
        LuaStateFacade facade = newLuaState();
        facade.evalFile("test/stringtest.lua");
        assertEquals(a, facade.getGlobal("a").getOrSneakyThrow());
        assertEquals(b, facade.getGlobal("b").getOrSneakyThrow());
        assertEquals(c, facade.getGlobal("c").getOrSneakyThrow());
        assertEquals(d, facade.getGlobal("d").getOrSneakyThrow());
        assertEquals(e, facade.getGlobal("e").getOrSneakyThrow());
        assertEquals(f, facade.getGlobal("f").getOrSneakyThrow());
    }
}
