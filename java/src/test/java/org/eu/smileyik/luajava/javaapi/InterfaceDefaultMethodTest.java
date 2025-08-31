package org.eu.smileyik.luajava.javaapi;

import org.eu.smileyik.luajava.BaseTest;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.junit.jupiter.api.Test;

public class InterfaceDefaultMethodTest extends BaseTest {

    @Test
    public void test() throws Exception {
        LuaStateFacade luaStateFacade = newLuaState();
        luaStateFacade.setGlobal("a", new A());
        luaStateFacade.evalString("a:a()").justThrow();
        luaStateFacade.evalString("a:superA()").justThrow();
        luaStateFacade.evalString("a:b()").justThrow();
    }

    public static interface ISuperA {
        public default void superA() {
            System.out.println("superA");
        }
    }

    public static interface IA extends ISuperA {
        public default void a() {
            System.out.println("a");
        }

        public void b();
    }

    public static class A implements IA {

        @Override
        public void b() {
            System.out.println("b");
        }
    }
}
