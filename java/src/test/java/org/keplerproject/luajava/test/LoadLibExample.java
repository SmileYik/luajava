package org.keplerproject.luajava.test;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFacade;

import java.util.Date;


/**
 * Example of a library in Java openned in Lua using loadLib
 *
 * @author Thiago Ponte
 */
public class LoadLibExample {
    private final static String LIB_NAME = "eg";

    private static void getLibTable(LuaState L) {
        L.getGlobal(LIB_NAME);
        if (L.isNil(-1)) {
            L.pop(1);
            L.newTable();
            L.pushValue(-1);
            L.setGlobal(LIB_NAME);
        }
    }

    /**
     * Method called by loadLib
     */
    public static void open(LuaStateFacade facade) throws LuaException {
        facade.lockThrow(L -> {
            getLibTable(L);

            L.pushString("example");

            L.pushJavaFunction(new JavaFunction(facade) {
                /**
                 * Example for loadLib.
                 * Prints the time and the first parameter, if any.
                 */
                public int execute() throws LuaException {
                    System.out.println(new Date());

                    if (L.lock(LuaState::getTop) > 1) {
                        System.out.println(getParam(2));
                    }

                    return 0;
                }
            });

            L.setTable(-3);

            L.pop(1);
        });
    }
}
