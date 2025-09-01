package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectConstructor implements IExecutable<Constructor<?>>, Copiable {

    protected Constructor<?> constructor;

    protected ReflectConstructor() {

    }

    public ReflectConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Object invoke(Object instance, Object[] params) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        return constructor.newInstance(params);
    }

    @Override
    public Constructor<?> getExecutable() {
        return constructor;
    }

    protected void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public ReflectConstructor copy() {
        return new ReflectConstructor(constructor);
    }
}
