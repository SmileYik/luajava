package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectMethod implements IExecutable<Method>, Copiable {
    protected Method method;

    protected ReflectMethod() {

    }

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

    protected void setMethod(Method method) {
        this.method = method;
    }

    public ReflectMethod copy() {
        return new ReflectMethod(method);
    }
}
