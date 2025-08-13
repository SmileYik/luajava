/*
 * LuaInvocationHandler.java, SmileYik, 2025-8-10
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

/*
 * $Id: LuaInvocationHandler.java,v 1.4 2006-12-22 14:06:40 thiago Exp $
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

package org.eu.smileyik.luajava;

import org.eu.smileyik.luajava.type.ILuaCallable;
import org.eu.smileyik.luajava.type.ILuaFieldGettable;
import org.eu.smileyik.luajava.util.BoxedTypeHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Class that implements the InvocationHandler interface.
 * This class is used in the LuaJava's proxy system.
 * When a proxy object is accessed, the method invoked is
 * called from Lua
 *
 * @author Rizzato
 * @author Thiago Ponte
 */
public class LuaInvocationHandler implements InvocationHandler {
    private final LuaObject obj;


    public LuaInvocationHandler(LuaObject obj) {
        this.obj = obj;
    }

    /**
     * Function called when a proxy object function is invoked.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws LuaException {
        final LuaStateFacade facade = obj.getLuaState();
        return facade.lockThrow(l -> {
            String methodName = method.getName();
            LuaObject func = ((ILuaFieldGettable) obj)
                    .getField(methodName)
                    .getOrThrow(LuaException.class);
            if (func == null || func.isNil()) return null;
            if (!func.isCallable()) {
                throw new LuaException("Method " + methodName + " is not a callable");
            }

            Class<?> retType = method.getReturnType();
            Object ret = null;

            // Checks if returned type is void. if it is returns null.
            if (retType.equals(Void.class) || retType.equals(void.class)) {
                facade.doPcall((ILuaCallable) func, args, 0)
                        .justThrow(LuaException.class);
            } else {
                ret = facade.doPcall((ILuaCallable) func, args, 1)
                        .mapValue(it -> {
                            Object o = it[0];
                            if (o instanceof Double) {
                                return BoxedTypeHelper.covertNumberTo((Double) o, retType);
                            }
                            return o;
                        })
                        .getOrThrow(LuaException.class);
            }
            return ret;
        }).getOrThrow(LuaException.class);
    }
}
