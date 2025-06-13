package org.eu.smileyik.luajava.util;

import java.lang.reflect.Array;
import java.util.Iterator;

public class ArrayIterator implements Iterator<Object>, IndexableIterator<Object> {
    private final Object obj;
    private final int len;
    private int index;

    public ArrayIterator(Object obj) {
        this.obj = obj;
        this.len = Array.getLength(obj);
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < len;
    }

    @Override
    public Object next() {
        return Array.get(obj, index++);
    }

    public int getIndex() {
        return index;
    }
}
