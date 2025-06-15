package org.keplerproject.luajava.test;

import org.eu.smileyik.luajava.BaseTest;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LuaStateFacade;

public class SwingTest extends BaseTest {

    @Test
    public void test() throws Exception {
        try (LuaStateFacade facade = newLuaState()) {
            facade.evalFile("test/swingTest.lua").justThrow();
            Thread.sleep(10000);
        }
    }

    public static void main(String[] args) throws Exception {
        newLuaState().evalFile("java/test/swingTest.lua").justThrow();
    }
}
