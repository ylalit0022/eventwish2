package com.ds.eventwish.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import retrofit2.Response;

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
public class ApiResponse<T> {

    public final int code;
    @Nullable
    public final T body;
    @Nullable
    public final String errorMessage;
    @Nullable
    public final String errorBody;

    public ApiResponse(Throwable error) {
        code = 500;
        body = null;
        errorMessage = error.getMessage();
        errorBody = null;
    }

    public ApiResponse(Response<T> response) {
        code = response.code();
        if (response.isSuccessful()) {
            body = response.body();
            errorMessage = null;
            errorBody = null;
        } else {
            body = null;
            errorMessage = response.message();
            errorBody = response.errorBody() != null ? getErrorBodyString(response) : null;
        }
    }

    private String getErrorBodyString(Response<T> response) {
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            return "Error reading error body";
        }
    }

    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public boolean isClientError() {
        return code >= 400 && code < 500;
    }

    public boolean isServerError() {
        return code >= 500;
    }

    public boolean isNetworkError() {
        return code == 0 && errorMessage != null;
    }

    /**
     * Get the error message from the response
     * @return Error message or a default message if not available
     */
    public String getErrorMessage() {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        } else if (errorBody != null && !errorBody.isEmpty()) {
            return errorBody;
        } else {
            return "Error " + code;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ApiResponse{" +
                "code=" + code +
                ", body=" + body +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorBody='" + errorBody + '\'' +
                '}';
    }
} 