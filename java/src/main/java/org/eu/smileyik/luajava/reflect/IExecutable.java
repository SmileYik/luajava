package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;

public interface IExecutable <T extends Executable> {
    public Object invoke(Object instance, Object[] params) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    public T getExecutable();
}
