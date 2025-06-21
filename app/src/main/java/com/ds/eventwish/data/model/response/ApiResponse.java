package com.ds.eventwish.data.model.response;

import com.google.gson.annotations.SerializedName;

/**
 * Generic API response class that can handle any data type
 * @param <T> The type of data contained in the response
 */
public class ApiResponse<T> {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("data")
    private T data;

    public ApiResponse() {
        // Required empty constructor for Gson
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
} 