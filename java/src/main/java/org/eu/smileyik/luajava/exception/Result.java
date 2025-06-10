package org.eu.smileyik.luajava.exception;

import java.util.function.Supplier;

public class Result <T, E> {
    private final String message;
    private final T value;
    private final E error;

    private Result(String message, T value, E error) {
        this.message = message;
        this.value = value;
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public T getValue() {
        return value;
    }

    public E getError() {
        return error;
    }

    boolean isError() {
        return error != null;
    }

    boolean isSuccess() {
        return !isError();
    }

    public T orElse(T other) {
        return isError() ? other : value;
    }

    public T orElseGet(Supplier<T> other) {
        return isError() ? other.get() : value;
    }

    public static <T, E> Result<T, E> of(T value, E error) {
        return new Result<T, E>(null, value, error);
    }

    public static <T, E> Result<T, E> of(T value, E error, String message) {
        return new Result<T, E>(message, value, error);
    }

    public static <T, E> Result<T, E> success(T value) {
        return of(value, null);
    }

    public static <T, E> Result<T, E> success(T value, String message) {
        return of(value, null, message);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return of(null, error);
    }

    public static <T, E> Result<T, E> failure(E error, String message) {
        return of(null, error, message);
    }

    public static <T, E> Result<T, E> failure(T value, E error, String message) {
        assert error != null;
        return of(value, error, message);
    }
}
