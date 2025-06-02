package com.ds.eventwish.data.repository;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.utils.InternetConnectivityChecker;
import retrofit2.Response;
import java.io.IOException;
import java.net.SocketTimeoutException;

public abstract class BaseRepository {
    private static final String TAG = "BaseRepository";
    protected final InternetConnectivityChecker connectivityChecker;

    protected BaseRepository() {
        this.connectivityChecker = InternetConnectivityChecker.getInstance();
    }

    protected <T> void handleApiError(Response<T> response, MutableLiveData<String> errorLiveData) {
        String errorMessage;
        
        switch (response.code()) {
            case 401:
                errorMessage = "Unauthorized access. Please log in again.";
                break;
            case 403:
                errorMessage = "Access forbidden. Please check your permissions.";
                break;
            case 404:
                errorMessage = "Resource not found.";
                break;
            case 429:
                errorMessage = "Too many requests. Please try again later.";
                break;
            case 500:
                errorMessage = "Server error. Please try again later.";
                break;
            default:
                errorMessage = "Error: " + response.code();
                if (response.errorBody() != null) {
                    try {
                        errorMessage += " - " + response.errorBody().string();
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
        }
        
        Log.e(TAG, "API Error: " + errorMessage);
        errorLiveData.postValue(errorMessage);
    }

    protected void handleNetworkError(Throwable t, MutableLiveData<String> errorLiveData) {
        String errorMessage;
        
        if (t instanceof ApiClient.NoConnectivityException) {
            errorMessage = "No internet connection available.";
        } else if (t instanceof SocketTimeoutException) {
            errorMessage = "Request timed out. Please try again.";
        } else if (t instanceof IOException) {
            errorMessage = "Network error: " + t.getMessage();
        } else {
            errorMessage = "Unexpected error: " + t.getMessage();
        }
        
        Log.e(TAG, "Network Error: " + errorMessage, t);
        errorLiveData.postValue(errorMessage);
    }

    protected boolean isNetworkAvailable() {
        boolean isAvailable = connectivityChecker.isNetworkAvailable();
        if (!isAvailable) {
            Log.d(TAG, "Network is not available");
        }
        return isAvailable;
    }
} 