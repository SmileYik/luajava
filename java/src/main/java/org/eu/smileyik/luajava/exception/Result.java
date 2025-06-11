package org.eu.smileyik.luajava.exception;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result <T, E> {
    /**
     * SUCCESS INSTANCE.
     */
    private static final Result<?, ?> SUCCESS = new Result<>(null, null, null);

    private final String message;
    private final T value;
    private final E error;

    private Result(String message, T value, E error) {
        this.message = message;
        this.value = value;
        this.error = error;
    }

    /**
     * get the message.
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * just get value
     * @return the value, if this instance is error, then maybe return null.
     */
    public T getValue() {
        return value;
    }

    /**
     * just get error
     * @return the error value, if this instance means success, then this must be null.
     */
    public E getError() {
        return error;
    }

    /**
     * check this instance means error or not
     * @return true means error
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * success
     * @return true means success
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * get the value if success, return other value if failed.
     * @param other the other value.
     * @return return value or other.
     */
    public T orElse(T other) {
        return isError() ? other : value;
    }

    /**
     * get the value if success, return other value if failed.
     * @param other the other value.
     * @return return value or other.
     */
    public T orElseGet(Supplier<T> other) {
        return isError() ? other.get() : value;
    }

    /**
     * if this is an error, then throws.
     * @throws Exception any exception.
     */
    public void justThrow() throws Exception {
        if (isError()) {
            if (error instanceof Exception) {
                throw (Exception) error;
            } else {
                throw new RuntimeException(message == null ? Objects.toString(error) : message + error);
            }
        }
    }

    /**
     * if this is an error, then throws.
     * @param clazz target exception type
     * @param <Err> target exception type
     * @throws Err target exception type
     */
    public <Err extends Exception> void justThrow(Class<Err> clazz) throws Err {
        if (isError()) {
            if (clazz.isAssignableFrom(error.getClass())) {
                throw clazz.cast(error);
            } else if (error instanceof Throwable) {
                throw new RuntimeException(message, (Exception) error);
            } else {
                throw new RuntimeException(message == null ? Objects.toString(error) : message + error);
            }
        }
    }

    /**
     * if error then throw, else return the value.
     * @return the value
     * @throws Exception exception.
     */
    public T getOrThrow() throws Exception {
        justThrow();
        return value;
    }

    /**
     * if error then throw, else return the value.
     * @param clazz the exception type.
     * @return the value
     */
    public <Err extends Exception> T getOrThrow(Class<Err> clazz) throws Err {
        justThrow(clazz);
        return value;
    }

    /**
     * get value or sneaky throw exception
     * @return the value
     */
    public T getOrSneakyThrow() {
        if (isError()) {
            Throwable ret = (error instanceof Throwable) ? (Throwable) error :  null;
            if (ret == null) {
                throw new RuntimeException(message == null ? Objects.toString(error) : message + error);
            } else {
                throw new RuntimeException(message, ret);
            }
        }
        return value;
    }

    /**
     * just cast this result to target result.
     * if you know it's work then you can use it.
     * @return the target result.
     * @param <RT>
     * @param <RE>
     */
    public <RT, RE> Result<RT, RE> justCast() {
        return (Result<RT, RE>) this;
    }

    private <RT, RE> Result<RT, RE> justReplaceValue(RT newValue) {
        if (newValue == value) return (Result<RT, RE>) this;
        return (this == SUCCESS || newValue == null) ? success() : Result.success(newValue, message);
    }

    /**
     * as same as mapValue method. but just replace value.
     * @param newValue
     * @return
     * @param <RT>
     */
    public <RT> Result<RT, E> replaceValue(RT newValue) {
        if (isError()) {
            return (Result<RT, E>) this;
        }
        if (newValue == value || (this == SUCCESS && newValue == null)) return (Result<RT, E>) this;
        return Result.success(newValue, message);
    }

    /**
     * transform value if success.
     * @param f function
     * @return result
     * @param <RT>
     */
    public <RT> Result<RT, E> mapValue(Function<T, RT> f) {
        if (isError()) {
            return (Result<RT, E>) this;
        }
        RT apply = f.apply(value);
        return justReplaceValue(apply);
    }

    /**
     * transform error if failed
     * @param f function
     * @return result
     * @param <RE>
     */
    public <RE> Result<T, RE> mapError(Function<E, RE> f) {
        if (isError()) {
            return Result.failure(f.apply(error), message);
        }
        return (Result<T, RE>) this;
    }

    /**
     * replace error to string. if error is throwable,
     * then will just use Throwable.getMessage() and will not use param function.
     * @param f covert function, will not call if error is Throwable.
     * @return
     */
    public Result<T, String> replaceErrorString(Function<E, String> f) {
        if (isError()) {
            String str = null;
            if (error instanceof Throwable) {
                str = ((Throwable) error).getMessage();
            } else {
                str = f.apply(error);
            }
            return Result.failure(str);
        }
        return (Result<T, String>) this;
    }

    /**
     * transform result
     * @param rt transform result if success
     * @param re transform error if failed
     * @return the result
     * @param <RT>
     * @param <RE>
     */
    public <RT, RE> Result<RT, RE> map(Function<T, RT> rt, Function<E, RE> re) {
        if (isError()) {
            return Result.failure(re.apply(error), message);
        }
        RT apply = rt.apply(value);
        return justReplaceValue(apply);
    }

    /**
     * if success then use value.toString else error.toString
     * if you want custom message please use map method;
     * @return
     */
    @Override
    public String toString() {
        return isError() ? String.format("Result Error { %s }", error) : String.format("Result { %s }", value);
    }

    public static <T, E> Result<T, E> of(T value, E error) {
        return new Result<T, E>(null, value, error);
    }

    public static <T, E> Result<T, E> of(T value, E error, String message) {
        return new Result<T, E>(message, value, error);
    }

    public static <T, E> Result<T, E> success() {
        return (Result<T, E>) SUCCESS;
    }

    public static <T, E> Result<T, E> success(T value) {
        if (value == null) return success();
        return of(value, null);
    }

    public static <T, E> Result<T, E> success(T value, String message) {
        if (value == null && message == null) return success();
        return of(value, null, message);
    }

    public static <T, E> Result<T, E> failure(E error) {
        assert error != null;
        return of(null, error);
    }

    public static <T, E> Result<T, E> failure(E error, String message) {
        assert error != null;
        return of(null, error, message);
    }
}
