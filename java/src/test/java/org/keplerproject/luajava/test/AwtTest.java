package org.keplerproject.luajava.test;

import org.eu.smileyik.luajava.BaseTest;
import org.junit.jupiter.api.Test;

public class AwtTest extends BaseTest {

    @Test
    public void test() throws Exception {
        newLuaState().evalFile("test/awtTest.lua").justThrow();
    }

    public static void main(String[] args) throws Exception {
        newLuaState().evalFile("java/test/awtTest.lua").justThrow();
    }
}
