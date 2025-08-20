package org.eu.smileyik.luajava;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class LuaReadWriteTest extends BaseTest {

    final String lua = "" +
            "a = 'Hello'\n" +
            "local b = 'world!'\n" +
            "function concat(str)\n" +
            "    return a .. ' ' .. str\n" +
            "end\n" +
            "local c = concat(b)\n" +
            "print(c)";

    public byte[] getBytesFromLuaState() throws IOException {
        try (LuaStateFacade facade = newLuaState()) {
            LuaState luaState = facade.getLuaState();
            facade.loadString(lua);
            try (ILuaReadWriteEntity.SimpleWrite simpleWrite = new ILuaReadWriteEntity.SimpleWrite()) {
                int i = luaState.dump(simpleWrite, true);
                ByteArrayOutputStream outputStream = (ByteArrayOutputStream) simpleWrite.getOutputStream();
                byte[] byteArray = outputStream.toByteArray();
                System.out.println(Arrays.toString(byteArray));
                System.out.println(new String(byteArray));
                return byteArray;
            }
        }
    }

    @Test
    public void testWrite() throws IOException {
        LuaStateFacade luaStateFacade = newLuaState();
        LuaState luaState = luaStateFacade.getLuaState();
        luaStateFacade.loadString(lua);
        try (ILuaReadWriteEntity.SimpleWrite simpleWrite = new ILuaReadWriteEntity.SimpleWrite()) {
            int i = luaState.dump(simpleWrite, true);
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) simpleWrite.getOutputStream();
            byte[] byteArray = outputStream.toByteArray();
            System.out.println(Arrays.toString(byteArray));
            System.out.println(new String(byteArray));
        }
        luaStateFacade.pcall(0, 0, 0).getOrSneakyThrow();
    }

    final byte[] luaData = new byte[] {
            27, 76, 74, 2, 8, 111, 97, 32, 61, 32, 39, 72, 101, 108, 108, 111, 39, 10, 108, 111, 99, 97, 108, 32, 98, 32, 61, 32, 39, 119, 111, 114, 108, 100, 33, 39, 10, 102, 117, 110, 99, 116, 105, 111, 110, 32, 99, 111, 110, 99, 97, 116, 40, 115, 116, 114, 41, 10, 32, 32, 32, 32, 114, 101, 116, 117, 114, 110, 32, 97, 32, 46, 46, 32, 39, 32, 39, 32, 46, 46, 32, 115, 116, 114, 10, 101, 110, 100, 10, 108, 111, 99, 97, 108, 32, 99, 32, 61, 32, 99, 111, 110, 99, 97, 116, 40, 98, 41, 10, 112, 114, 105, 110, 116, 40, 99, 41, 46, 0, 1, 4, 0, 2, 0, 5, 12, 3, 2, 54, 1, 0, 0, 39, 2, 1, 0, 18, 3, 0, 0, 38, 1, 3, 1, 76, 1, 2, 0, 6, 32, 6, 97, 1, 1, 1, 1, 1, 115, 116, 114, 0, 0, 6, 0, 108, 3, 0, 5, 0, 6, 0, 12, 21, 0, 7, 39, 0, 0, 0, 55, 0, 1, 0, 39, 0, 2, 0, 51, 1, 3, 0, 55, 1, 4, 0, 54, 1, 4, 0, 18, 3, 0, 0, 66, 1, 2, 2, 54, 2, 5, 0, 18, 4, 1, 0, 66, 2, 2, 1, 75, 0, 1, 0, 10, 112, 114, 105, 110, 116, 11, 99, 111, 110, 99, 97, 116, 0, 11, 119, 111, 114, 108, 100, 33, 6, 97, 10, 72, 101, 108, 108, 111, 1, 1, 2, 5, 3, 6, 6, 6, 7, 7, 7, 7, 98, 0, 4, 9, 99, 0, 5, 4, 0, 0
    };

    @Test
    public void testRead() throws IOException {
        byte[] luaData = getBytesFromLuaState();
        LuaStateFacade luaStateFacade = newLuaState();
        LuaState luaState = luaStateFacade.getLuaState();
        ILuaReadWriteEntity.SimpleRead simpleRead = new ILuaReadWriteEntity.SimpleRead(
                new ByteArrayInputStream(luaData), 2048
        );
        int result = luaState.load(simpleRead, "LoadTest", "b");
        System.out.println("result: " + result);
        luaStateFacade.pcall(0, 0, 0).getOrSneakyThrow();
    }

    @Test
    public void copyFunctionTest() throws IOException {
        String lua =
                "" +
                        "local first = 'anc'\n" +
                        "local mmmap = {a = 1, b = '2', c = true, d = {e = 1}, f = function() end}\n" +
                        "print('啊')\n" +
                        "function hello() \n" +
                        "    print('hello world')\n return 1\n" +
                        "end\n" +
                        "local a = 1\n" +
                        "print(a)\n" +
                        "hello()\n" +
                        "local h = function(name)\n" +
                        "    local newName = 'hello ' .. name\n" +
                        "    return function ()\n" +
                        "        print(newName)\n" +
                        "    end\n" +
                        "end\n" +
                        "hFunc = h('abc')\n" +
                        "hFunc()\n";

        LuaStateFacade a = newLuaState();
        a.openLibs();
        LuaState as = a.getLuaState();
        a.loadString(lua).getOrSneakyThrow();
        a.pcall(0, 0, 0).getOrSneakyThrow();
        as.getGlobal("hello");
        System.out.println("a function: " + a.type(-1));

        byte[] luaData;
        try (ILuaReadWriteEntity.SimpleWrite simpleWrite = new ILuaReadWriteEntity.SimpleWrite()) {
            int i = as.dump(simpleWrite, false);
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) simpleWrite.getOutputStream();
            byte[] byteArray = outputStream.toByteArray();
            System.out.println(Arrays.toString(byteArray));
            System.out.println(new String(byteArray));
            luaData = byteArray;
        }


        LuaStateFacade b = newLuaState();
        b.openLibs();
        LuaState bs = b.getLuaState();
        ILuaReadWriteEntity.SimpleRead simpleRead = new ILuaReadWriteEntity.SimpleRead(
                new ByteArrayInputStream(luaData), 2048
        );
        int result = bs.load(simpleRead, "LoadTest", "b");
        System.out.println("result: " + result);
        b.pcall(0, 0, 0).getOrSneakyThrow();
    }

    @Test
    public void copyTest() throws ExecutionException, InterruptedException {
        String lua =
                "" +
                        "local first = 'anc'\n" +
                        "local mmmap = {a = 1, b = '2', c = true, d = {e = 1}, f = function() print('----f----') end}\n" +
                        "mmmap.g = mmmap;\n" +
                        "mmmap.h = mmmap;\n" +
                        "mmmap.d.h = mmmap.g\n" +
                        "print('啊')\n" +
                        "function hello() \n" +
                        "    print('hello world')\n return 1\n" +
                        "end\n" +
                        "local a = 1\n" +
                        "print(a)\n" +
                        "hello()\n" +
                        "local h = function(name)\n" +
                        "    local newName = 'hello ' .. name\n" +
                        "    return function ()\n" +
                        "        print(newName)\nprint(mmmap)\n for k, v in pairs(mmmap) do print(k, v) end\n" +
                        "        mmmap.f()\n" +
                        "        mmmap.h.f()\n" +
                        "    end\n" +
                        "end\n" +
                        "hFunc = h('abc')\n" +
                        "hFunc()\nreturn hello";

        LuaStateFacade luaStateFacade = newLuaState();
        System.out.println(LuaState.LUA_VERSION);
        LuaState luaState = luaStateFacade.getLuaState();
        luaState.openLibs();
        luaStateFacade.loadString(lua);
        luaStateFacade.pcall(0, 1, 0).getOrSneakyThrow();
        luaState.getGlobal("hFunc");

        LuaStateFacade another = newLuaState();
        another.openLibs();

        // luaState.getGlobal("hello");
        if (luaState.copyValue(-1, another.getLuaState())) {
            // SimpleRspServer.start(16500, another).waitConnection();
            another.pcall(0, 1, 0).getOrSneakyThrow();
            System.out.println("---" + another.isNumber(-1));
        }

        // debugServer.close();
    }
}
