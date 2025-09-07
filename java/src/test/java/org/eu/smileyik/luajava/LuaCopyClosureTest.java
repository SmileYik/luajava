package org.eu.smileyik.luajava;

import org.eu.smileyik.luajava.exception.Result;
import org.eu.smileyik.luajava.type.ILuaCallable;
import org.eu.smileyik.luajava.type.ILuaObject;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class LuaCopyClosureTest extends BaseTest {

    @Test
    public void stableTest() throws Exception {
        String lua = "local banned_ench = {[1] = 1, [2] = 2, [3] = 3}\n" +
                "local Thread = luajava.bindClass(\"java.lang.Thread\")\n" +
                "local Date = luajava.bindClass(\"java.util.Date\")\n" +
                "local SimpleDateFormat = luajava.bindClass(\"java.text.SimpleDateFormat\")\n" +
                "local MessageFormat = luajava.bindClass(\"java.text.MessageFormat\")\n" +
                "local function testFunction(abc)\n" +
                "    local sum = 0\n" +
                "    for k, v in pairs(banned_ench) do\n" +
                "        sum = sum + k\n" +
                "    end\n" +
                "    local date = luajava.new(Date)\n" +
                "    local dateformat = luajava.new(SimpleDateFormat, \"yyyy-MM-dd HH:mm:ss\")\n" +
                "    local message = MessageFormat:format(\"{0} - Still not player join sever...?, abc is {1}, sum is {2}, current time is {3}\", {\n" +
                "        Thread:currentThread():getName(), abc, sum, 123\n" +
                "    })\n" +
                "end\n" +
                "return testFunction";
        long targetTime = System.currentTimeMillis() + 1000 * 60 * 60 * 3;
        Helper helper = new Helper();
        try (LuaStateFacade facade = newLuaState()) {
            facade.setGlobal("helper", helper);
            facade.evalString(lua).justThrow();
            ILuaCallable callable = (ILuaCallable) facade.toJavaObject(-1).getOrThrow();
            int i = 0;
            while (System.currentTimeMillis() < targetTime) {
                helper.submit(callable, 0, System.currentTimeMillis()).getOrThrow();
                if (i++ % 1000 == 0) {
                    System.gc();
                }
            }
        } finally {
            helper.close();
        }
    }

    public static final class Helper {
        private final LuaStateFacade facade = newLuaState();

        public Result<Object[], LuaException> submit(ILuaCallable luaCallable, int _nres, Object... params) {
            LuaStateFacade srcF = luaCallable.getLuaState();
            LuaStateFacade destF = facade;
            LuaState srcL = srcF.getLuaState();
            LuaState destL = destF.getLuaState();


            // call closure
            destF.lock();
            int top;
            try {
                top = destL.getTop();
                if (transferClosure(srcF, destF, luaCallable, params)) {
                    return destF.doPcall(params.length, _nres, 0)
                            .mapResultValue(v -> {
                                if (_nres == 0) return Result.success();
                                int nres = _nres;
                                int currentTop = destL.getTop();
                                if (nres == LuaState.LUA_MULTRET) {
                                    nres = currentTop - top;
                                }
                                if (currentTop - top < nres) {
                                    return Result.failure(new LuaException("Invalid Number of Results .")).justCast();
                                }

                                // copy returns
                                Object[] res = new Object[nres];
                                srcF.lock();
                                try {
                                    for (int i = nres - 1; i >= 0; i--) {
                                        Result<Object, ? extends LuaException> ret = destF.rawToJavaObject(-1);
                                        if (ret.isError()) return ret.justCast();
                                        res[i] = ret.getValue();
                                        if (res[i] instanceof ILuaObject) {
                                            if (destL.copyValue(-1, srcL)) {
                                                res[i] = srcF.rawToJavaObject(-1).getOrSneakyThrow();
                                                srcL.pop(1);
                                            } else {
                                                res[i] = null;
                                            }
                                        }
                                        destL.pop(1);
                                    }
                                } finally {
                                    srcF.unlock();
                                }
                                return Result.success(res);
                            });
                }
            } finally {
                destL.setTop(0);
                destF.unlock();
            }
            return Result.success();
        }

        private boolean transferClosure(
                LuaStateFacade srcF,
                LuaStateFacade destF,
                ILuaCallable callable,
                Object... params
        ) {
            LuaState srcL = srcF.getLuaState();
            LuaState destL = destF.getLuaState();

            // copy lua closure
            srcF.lock();
            try {
                callable.push();
                try {
                    if (srcL.copyValue(-1, destL)) {
                        // copy global value
                        String firstUpValue = srcL.getUpValue(-1, 1);
                        if (Objects.equals("_ENV", firstUpValue)) {
                            // copy _ENV for lua52+
                            destL.getUpValue(-1, 1);
                            srcL.copyTableIfNotExists(-1, destL);
                            srcL.pop(1);
                            destL.pop(1);
                        } else {
                            if (firstUpValue != null) {
                                srcL.pop(1);
                            }
                            // copy _G for luajit
                            srcL.getGlobal("_G");
                            destL.getGlobal("_G");
                            srcL.copyTableIfNotExists(-1, destL);

                            // copy real _G
                            srcL.pushString("_LUAJAVA_G_REF");
                            srcL.getTable(-2);
                            destL.pushString("_LUAJAVA_G_REF");
                            destL.getTable(-2);
                            if (!srcL.isNil(-1) && !destL.isNil(-1)) {
                                int srcRef = srcL.toInteger(-1);
                                int destRef = destL.toInteger(-1);
                                srcL.rawGetI(LuaState.LUA_REGISTRYINDEX, srcRef);
                                destL.rawGetI(LuaState.LUA_REGISTRYINDEX, destRef);
                                srcL.copyTableIfNotExists(-1, destL);
                                srcL.pop(1);
                                destL.pop(1);
                            }

                            srcL.pop(2);
                            destL.pop(2);
                        }

                        // copy params
                        for (Object param : params) {
                            if (param instanceof LuaObject) {
                                ((LuaObject) param).rawPush();
                                if (!srcL.copyValue(-1, destL)) {
                                    destL.pushNil();
                                }
                            } else {
                                destF.rawPushObjectValue(param)
                                        .ifFailureThen(e -> destL.pushNil());
                            }
                        }
                        return true;
                    }
                } finally {
                    srcL.pop(1);
                }
            } finally {
                srcF.unlock();
            }
            return false;
        }

        public void close() {
            facade.close();
        }
    }
}
