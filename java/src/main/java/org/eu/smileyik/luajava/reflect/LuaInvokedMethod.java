package org.eu.smileyik.luajava.reflect;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;

public class LuaInvokedMethod<T extends Executable> {
    private T executable;
    private final Map<Integer, Object> overwriteParams;

    public LuaInvokedMethod(LuaInvokedMethod<T> other) {
        this.executable = other.executable;
        this.overwriteParams = new HashMap<Integer, Object>(other.overwriteParams);
    }

    public LuaInvokedMethod() {
        this.overwriteParams = new HashMap<>();
    }

    public void reset(T executable) {
        this.executable = executable;
        this.overwriteParams.clear();
    }

    public void clear() {
        this.executable = null;
    }

    public T getExecutable() {
        return executable;
    }

    public Map<Integer, Object> getOverwriteParams() {
        return overwriteParams;
    }

    public void overwriteParam(int key, Object value) {
        overwriteParams.put(key, value);
    }

    public LuaInvokedMethod<T> done() {
        return new LuaInvokedMethod<>(this);
    }
}
