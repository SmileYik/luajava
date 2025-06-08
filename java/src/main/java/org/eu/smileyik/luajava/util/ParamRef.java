package org.eu.smileyik.luajava.util;

public class ParamRef <T> {
    private T param;

    protected ParamRef() {

    }

    protected ParamRef(T param) {
        this.param = param;
    }

    public T getParam() {
        return param;
    }

    public T getParamAndClear() {
        try {
            return param;
        } finally {
            param = null;
        }
    }

    public void setParam(T param) {
        this.param = param;
    }

    public boolean isEmpty() {
        return param == null;
    }

    public void clear() {
        param = null;
    }

    public static <T> ParamRef<T> wrapper() {
        return new ParamRef<T>();
    }

    public static <T> ParamRef<T> wrapper(T param) {
        return new ParamRef<T>(param);
    }
}
