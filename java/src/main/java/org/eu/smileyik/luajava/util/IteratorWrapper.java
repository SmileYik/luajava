package org.eu.smileyik.luajava.util;

import java.util.Iterator;

public class IteratorWrapper implements Iterator<Object>, IndexableIterator<Object> {
    private final Iterator<?> iterator;
    private int index = 0;

    public IteratorWrapper(Iterator<?> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        index += 1;
        return iterator.next();
    }

    public int getIndex() {
        return index;
    }

}
