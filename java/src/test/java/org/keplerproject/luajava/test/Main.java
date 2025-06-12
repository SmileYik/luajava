/*
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava.test;

import org.eu.smileyik.luajava.type.ILuaCallable;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.*;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class Main {

    static {
        LoadLibrary.load();
    }

    static String str = "a = 'campo a';" +
            "b = 'campo b';" +
            "c = 'campo c';" +
            "tab= { a='tab a'; b='tab b'; c='tab c', d={ e='tab d e'} } ;" +
            "function imprime (str)	print(str);	return 'joao', 1	end;" +
            "luaPrint={implements='org.keplerproject.luajava.test.Printable', print=function(str)print('Printing from lua :'..str)end  }";


    @Test
    public void test() throws LuaException, ClassNotFoundException {
        LuaStateFacade luaState = LuaStateFactory.newLuaState();
        luaState.lockThrow(L -> {
            L.openBase();

            L.LdoString(str);

            LuaObject func = luaState.getLuaObject("imprime").getOrThrow(LuaException.class);
            assertTrue(func.isCallable());
            Object[] teste = ((ILuaCallable) func).call(2, new Object[]{"TESTANDO"}).getOrThrow(LuaException.class);
            System.out.println(teste[0]);
            System.out.println(teste[1]);

            System.out.println("PROXY TEST :");
            Printable p = new ObjPrint();
            p.print("TESTE 1");

            LuaObject o = luaState.getLuaObject("luaPrint").getOrThrow(LuaException.class);
            try {
                p = (Printable) o.createProxy("org.keplerproject.luajava.test.Printable").getOrThrow(LuaException.class);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            p.print("Teste 2");
        });

        luaState.close();
    }
}
