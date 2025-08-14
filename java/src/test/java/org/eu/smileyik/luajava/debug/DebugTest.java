package org.eu.smileyik.luajava.debug;

import org.eu.smileyik.luajava.BaseTest;
import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.junit.jupiter.api.Test;

public class DebugTest extends BaseTest {
    @Test
    public void testDebug() {
        String lua =
                "print('啊')\n" +
                "function hello() \n" +
                "    print('hello world')\n" +
                "end\n" +
                "local a = 1\n" +
                "print(a)\n" +
                "hello()";

        LuaStateFacade luaStateFacade = newLuaState();
        System.out.println(LuaState.LUA_VERSION);
        luaStateFacade.setDebugHook((fade, luaDebug) -> {
            System.out.println("------------");
            System.out.println(luaDebug.toString().replace("\n", "\\n"));
            LuaDebug debug = null;
            LuaState state = luaStateFacade.getLuaState();
            int level = 1;
            while ((debug = state.getStack(level)) != null) {
                System.out.println(debug.toString().replace("\n", "\\n"));
                debug = state.getInfo(debug, "lSnu");
                System.out.println(debug.toString().replace("\n", "\\n"));
                fade.getLuaState().freeLuaDebug(debug);
                level += 1;
            }
        });
        LuaState luaState = luaStateFacade.getLuaState();
        luaState.openLibs();
        luaState.setHook(LuaState.LUA_MASKLINE, 0);
        luaStateFacade.evalString(lua);
    }
}
