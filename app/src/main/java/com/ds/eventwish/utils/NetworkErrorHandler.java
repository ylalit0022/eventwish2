package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import retrofit2.HttpException;

/**
 * Utility class for handling network errors in a standardized way
 */
public class NetworkErrorHandler {
    private static final String TAG = "NetworkErrorHandler";
    
    // Error types
    public enum NetworkErrorType {
        CONNECTION_ERROR,
        TIMEOUT_ERROR,
        SERVER_ERROR,
        SSL_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Get the error type from a Throwable
     * @param throwable Throwable to analyze
     * @return NetworkErrorType
     */
    public static NetworkErrorType getErrorType(@NonNull Throwable throwable) {
        if (throwable instanceof ConnectException || throwable instanceof UnknownHostException) {
            return NetworkErrorType.CONNECTION_ERROR;
        } else if (throwable instanceof SocketTimeoutException) {
            return NetworkErrorType.TIMEOUT_ERROR;
        } else if (throwable instanceof HttpException) {
            return NetworkErrorType.SERVER_ERROR;
        } else if (throwable instanceof SSLException) {
            return NetworkErrorType.SSL_ERROR;
        } else {
            return NetworkErrorType.UNKNOWN_ERROR;
        }
    }
    
    /**
     * Get the error message from a Throwable
     * @param context Context for string resources
     * @param throwable Throwable to analyze
     * @return User-friendly error message
     */
    public static String getErrorMessage(@NonNull Context context, @NonNull Throwable throwable) {
        NetworkErrorType errorType = getErrorType(throwable);
        
        switch (errorType) {
            case CONNECTION_ERROR:
                return context.getString(R.string.error_no_connection);
            case TIMEOUT_ERROR:
                return context.getString(R.string.error_timeout);
            case SERVER_ERROR:
                if (throwable instanceof HttpException) {
                    HttpException httpException = (HttpException) throwable;
                    int code = httpException.code();
                    
                    if (code >= 500) {
                        return context.getString(R.string.error_server);
                    } else if (code == 404) {
                        return context.getString(R.string.error_not_found);
                    } else if (code == 401 || code == 403) {
                        return context.getString(R.string.error_unauthorized);
                    } else {
                        return context.getString(R.string.error_http, code);
                    }
                }
                return context.getString(R.string.error_server);
            case SSL_ERROR:
                return context.getString(R.string.error_ssl);
            case UNKNOWN_ERROR:
            default:
                return context.getString(R.string.error_unknown);
        }
    }
    
    /**
     * Get the error icon resource ID from a Throwable
     * @param throwable Throwable to analyze
     * @return Resource ID for error icon
     */
    public static int getErrorIconResource(@NonNull Throwable throwable) {
        NetworkErrorType errorType = getErrorType(throwable);
        
        switch (errorType) {
            case CONNECTION_ERROR:
                return R.drawable.ic_offline;
            case TIMEOUT_ERROR:
                return R.drawable.ic_timeout;
            case SERVER_ERROR:
                return R.drawable.ic_server_error;
            case SSL_ERROR:
                return R.drawable.ic_error;
            case UNKNOWN_ERROR:
            default:
                return R.drawable.ic_error;
        }
    }
    
    /**
     * Handle a network error
     * @param context Context for string resources
     * @param throwable Throwable to handle
     * @param errorHandler ErrorHandler for centralized error handling
     */
    public static void handleNetworkError(@NonNull Context context, @NonNull Throwable throwable, @NonNull ErrorHandler errorHandler) {
        NetworkErrorType errorType = getErrorType(throwable);
        String errorMessage = getErrorMessage(context, throwable);
        
        // Log the error
        Log.e(TAG, "Network error: " + errorMessage, throwable);
        
        // Determine error severity
        ErrorHandler.ErrorSeverity severity;
        ErrorHandler.ErrorType type;
        
        switch (errorType) {
            case CONNECTION_ERROR:
                severity = ErrorHandler.ErrorSeverity.MEDIUM;
                type = ErrorHandler.ErrorType.NETWORK_ERROR;
                break;
            case TIMEOUT_ERROR:
                severity = ErrorHandler.ErrorSeverity.MEDIUM;
                type = ErrorHandler.ErrorType.TIMEOUT;
                break;
            case SERVER_ERROR:
                severity = ErrorHandler.ErrorSeverity.HIGH;
                type = ErrorHandler.ErrorType.SERVER_ERROR;
                break;
            case SSL_ERROR:
                severity = ErrorHandler.ErrorSeverity.HIGH;
                type = ErrorHandler.ErrorType.SECURITY_ERROR;
                break;
            case UNKNOWN_ERROR:
            default:
                severity = ErrorHandler.ErrorSeverity.MEDIUM;
                type = ErrorHandler.ErrorType.UNKNOWN;
                break;
        }
        
        // Handle the error
        errorHandler.handleError(type, errorMessage, severity);
    }
    
    /**
     * Check if the error is recoverable
     * @param throwable Throwable to analyze
     * @return true if recoverable, false otherwise
     */
    public static boolean isRecoverableError(@NonNull Throwable throwable) {
        NetworkErrorType errorType = getErrorType(throwable);
        
        switch (errorType) {
            case CONNECTION_ERROR:
            case TIMEOUT_ERROR:
                return true;
            case SERVER_ERROR:
                if (throwable instanceof HttpException) {
                    HttpException httpException = (HttpException) throwable;
                    int code = httpException.code();
                    
                    // 5xx errors are server errors and might be recoverable
                    // 4xx errors are client errors and usually not recoverable
                    return code >= 500;
                }
                return false;
            case SSL_ERROR:
            case UNKNOWN_ERROR:
            default:
                return false;
        }
    }
    
    /**
     * Get the retry delay for a recoverable error
     * @param throwable Throwable to analyze
     * @param retryCount Current retry count
     * @return Delay in milliseconds before retry
     */
    public static long getRetryDelay(@NonNull Throwable throwable, int retryCount) {
        NetworkErrorType errorType = getErrorType(throwable);
        
        // Base delay in milliseconds
        long baseDelay;
        
        switch (errorType) {
            case CONNECTION_ERROR:
                baseDelay = 1000; // 1 second
                break;
            case TIMEOUT_ERROR:
                baseDelay = 2000; // 2 seconds
                break;
            case SERVER_ERROR:
                baseDelay = 5000; // 5 seconds
                break;
            case SSL_ERROR:
            case UNKNOWN_ERROR:
            default:
                baseDelay = 3000; // 3 seconds
                break;
        }
        
        // Apply exponential backoff with jitter
        double jitter = Math.random() * 0.3 + 0.85; // Random between 0.85 and 1.15
        return (long) (baseDelay * Math.pow(1.5, retryCount) * jitter);
    }
} 