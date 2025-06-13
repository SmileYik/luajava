package org.eu.smileyik.luajava.util;

import java.util.Iterator;

public interface IndexableIterator <T> extends Iterator<T> {
    int getIndex();
}
