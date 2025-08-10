/*
 * LoadLibExample.java, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
