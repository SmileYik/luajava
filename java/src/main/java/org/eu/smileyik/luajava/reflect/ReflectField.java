package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Field;

public class ReflectField implements IFieldAccessor {
    protected final Field field;

    public ReflectField(Field field) {
        this.field = field;
    }

    @Override
    public Object get(Object instance) throws IllegalAccessException {
        return field.get(instance);
    }

    @Override
    public void set(Object instance, Object value) throws IllegalAccessException {
        field.set(instance, value);
    }

    @Override
    public Field getField() {
        return field;
    }
}
