package org.eu.smileyik.luajava.test2;

import org.eu.smileyik.luajava.BaseTest;
import org.eu.smileyik.luajava.LuaException;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.exception.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThrowsHookTest extends BaseTest {

    @Test
    public void testThrowsHook() throws Exception {
        LuaStateFacade luaStateFacade = newLuaState();
        luaStateFacade.setGlobal("obj", new Object() {
            public void throwsExp() {
                int a = 1 / 0;
            }
        });
        assertTrue(luaExceptionContains(luaStateFacade, "InvocationTargetException"));
        luaStateFacade.setThrowableHook(exp -> {
            while (exp.getCause() != null) {
                exp = exp.getCause();
            }
            return exp;
        });
        assertTrue(luaExceptionContains(luaStateFacade, "ArithmeticException"));
    }

    private boolean luaExceptionContains(LuaStateFacade luaStateFacade, String keyword) {
        Result<Integer, LuaException> result = luaStateFacade.evalString("obj:throwsExp()");
        if (result.isError()) {
            try {
                String message = result.getError().getMessage();
                System.out.println(message);
                return message.contains(keyword);
            } catch (NullPointerException e) {
                return false;
            }
        }
        return false;
    }
}
