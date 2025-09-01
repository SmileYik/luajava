package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Field;

public interface IFieldAccessor {

    public Object get(Object instance) throws IllegalAccessException;

    public void set(Object instance, Object value) throws IllegalAccessException;

    Field getField();
}
