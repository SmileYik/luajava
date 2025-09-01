package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectMethod implements IExecutable<Method> {
    protected final Method method;
    public ReflectMethod(Method method) {
        this.method = method;
    }

    @Override
    public Object invoke(Object instance, Object[] params) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(instance, params);
    }

    @Override
    public Method getExecutable() {
        return method;
    }
}
