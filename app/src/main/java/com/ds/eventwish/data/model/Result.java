package com.ds.eventwish.data.model;

/**
 * A generic class that holds a result with its loading status.
 * @param <T> The type of data
 */
public class Result<T> {
    private final Status status;
    private final T data;
    private final String error;
    private final boolean stale;

    private Result(Status status, T data, String error, boolean stale) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.stale = stale;
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }
    
    public boolean isStale() {
        return stale;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null, false);
    }
    
    public static <T> Result<T> success(T data, boolean stale) {
        return new Result<>(Status.SUCCESS, data, null, stale);
    }

    public static <T> Result<T> error(String error) {
        return new Result<>(Status.ERROR, null, error, false);
    }
    
    public static <T> Result<T> error(String error, boolean stale) {
        return new Result<>(Status.ERROR, null, error, stale);
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null, false);
    }

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}
