package com.ds.eventwish.utils;

public sealed class NetworkResult<T> {
    
    private NetworkResult() {} // Prevent direct instantiation
    
    public static final class Success<T> extends NetworkResult<T> {
        private final T data;
        
        public Success(T data) {
            this.data = data;
        }
        
        public T getData() {
            return data;
        }
    }
    
    public static final class Error<T> extends NetworkResult<T> {
        private final String message;
        
        public Error(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static final class Loading<T> extends NetworkResult<T> {
        private Loading() {} // Private constructor
    }
    
    // Static factory methods
    public static <T> NetworkResult<T> success(T data) {
        return new Success<>(data);
    }
    
    public static <T> NetworkResult<T> error(String message) {
        return new Error<>(message);
    }
    
    public static <T> NetworkResult<T> loading() {
        return new Loading<>();
    }
    
    // Helper methods to check the type
    public boolean isSuccess() {
        return this instanceof Success;
    }
    
    public boolean isError() {
        return this instanceof Error;
    }
    
    public boolean isLoading() {
        return this instanceof Loading;
    }
}
