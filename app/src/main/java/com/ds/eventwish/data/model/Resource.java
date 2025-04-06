package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic class that holds a value with its loading status.
 * @param <T> Type of the resource data
 */
public class Resource<T> {
    
    /**
     * Status of a resource that is provided to the UI
     */
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
    
    @NonNull
    private final Status status;
    
    @Nullable
    private final T data;
    
    @Nullable
    private final String message;
    
    private final boolean stale;
    
    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message, boolean stale) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.stale = stale;
    }
    
    /**
     * Creates a successful resource with data
     * @param data The data to be wrapped
     * @param <T> Type of the data
     * @return A Resource in success state
     */
    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null, false);
    }
    
    /**
     * Creates a successful resource with data and stale flag
     * @param data The data to be wrapped
     * @param stale Whether the data is stale
     * @param <T> Type of the data
     * @return A Resource in success state
     */
    public static <T> Resource<T> success(@Nullable T data, boolean stale) {
        return new Resource<>(Status.SUCCESS, data, null, stale);
    }
    
    /**
     * Creates an error resource with an error message
     * @param msg The error message
     * @param data The data to be wrapped (can be null)
     * @param <T> Type of the data
     * @return A Resource in error state
     */
    public static <T> Resource<T> error(String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg, false);
    }
    
    /**
     * Creates an error resource with an error message and stale flag
     * @param msg The error message
     * @param data The data to be wrapped (can be null)
     * @param stale Whether the data is stale
     * @param <T> Type of the data
     * @return A Resource in error state
     */
    public static <T> Resource<T> error(String msg, @Nullable T data, boolean stale) {
        return new Resource<>(Status.ERROR, data, msg, stale);
    }
    
    /**
     * Creates a loading resource
     * @param data The data to be wrapped (can be null)
     * @param <T> Type of the data
     * @return A Resource in loading state
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null, false);
    }
    
    /**
     * Creates a loading resource with stale flag
     * @param data The data to be wrapped (can be null)
     * @param stale Whether the data is stale
     * @param <T> Type of the data
     * @return A Resource in loading state
     */
    public static <T> Resource<T> loading(@Nullable T data, boolean stale) {
        return new Resource<>(Status.LOADING, data, null, stale);
    }
    
    /**
     * Get the status of the resource
     * @return Status
     */
    @NonNull
    public Status getStatus() {
        return status;
    }
    
    /**
     * Get the data wrapped by this resource
     * @return Data
     */
    @Nullable
    public T getData() {
        return data;
    }
    
    /**
     * Get the error message
     * @return Error message
     */
    @Nullable
    public String getMessage() {
        return message;
    }
    
    /**
     * Check if the data is stale
     * @return True if stale, false otherwise
     */
    public boolean isStale() {
        return stale;
    }
    
    /**
     * Check if the resource is in success state
     * @return True if success, false otherwise
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * Check if the resource is in error state
     * @return True if error, false otherwise
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    /**
     * Check if the resource is in loading state
     * @return True if loading, false otherwise
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }
    
    /**
     * Check if the resource has data
     * @return True if has data, false otherwise
     */
    public boolean hasData() {
        return data != null;
    }
    
    @Override
    public String toString() {
        return "Resource{" +
                "status=" + status +
                ", data=" + data +
                ", message='" + message + '\'' +
                ", stale=" + stale +
                '}';
    }
} 