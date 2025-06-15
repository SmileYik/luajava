package org.keplerproject.luajava.test;

import org.eu.smileyik.luajava.BaseTest;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LuaStateFacade;

public class AwtTest extends BaseTest {

    @Test
    public void test() throws Exception {
        try (LuaStateFacade facade = newLuaState()) {
            facade.evalFile("test/awtTest.lua").getOrThrow();
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        newLuaState().evalFile("java/test/awtTest.lua").justThrow();
    }
}
