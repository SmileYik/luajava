package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectConstructor implements IExecutable<Constructor<?>> {

    protected final Constructor<?> constructor;

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
}
