package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.repository.ResourceRepository;
import com.ds.eventwish.ui.common.ErrorDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import retrofit2.HttpException;

/**
 * Utility class for standardized error handling and user feedback
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    
    // Error types
    public enum ErrorType {
        NETWORK,
        SERVER,
        CLIENT,
        TIMEOUT,
        UNKNOWN,
        OFFLINE,
        RESOURCE_NOT_FOUND,
        RESOURCE_EXPIRED,
        RESOURCE_INVALID,
        PERMISSION_DENIED,
        AUTHENTICATION_REQUIRED,
        NETWORK_ERROR,
        SERVER_ERROR,
        SECURITY_ERROR,
        PARSING_ERROR
    }
    
    // Error severity levels
    public enum ErrorSeverity {
        INFO,       // Informational, doesn't affect functionality
        LOW,        // Low severity, minor issue
        WARNING,    // Warning, some functionality may be affected
        ERROR,      // Error, major functionality affected
        CRITICAL,   // Critical, app cannot function properly
        MEDIUM,     // Medium severity, between WARNING and ERROR
        HIGH        // High severity, between ERROR and CRITICAL
    }
    
    // Singleton instance
    private static volatile ErrorHandler instance;
    
    // Context
    private final Context context;
    
    // Error message cache to avoid showing the same error repeatedly
    private final Map<String, Long> errorCache = new HashMap<>();
    private static final long ERROR_CACHE_DURATION = 5000; // 5 seconds
    
    // Network utils
    private final NetworkUtils networkUtils;
    
    /**
     * Get the singleton instance of ErrorHandler
     * @param context Application context
     * @return ErrorHandler instance
     */
    public static ErrorHandler getInstance(Context context) {
        if (instance == null) {
            synchronized (ErrorHandler.class) {
                if (instance == null) {
                    instance = new ErrorHandler(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private ErrorHandler(Context context) {
        this.context = context;
        this.networkUtils = NetworkUtils.getInstance(context);
        Log.d(TAG, "ErrorHandler initialized");
    }
    
    /**
     * Handle an error and provide appropriate user feedback
     * @param activity Activity context for UI feedback
     * @param throwable Error that occurred
     * @param resourceType Type of resource being accessed (optional)
     * @param resourceKey Key of resource being accessed (optional)
     * @param fallbackMessageResId Fallback message resource ID
     * @return ErrorType that was handled
     */
    public ErrorType handleError(
            @Nullable FragmentActivity activity,
            @NonNull Throwable throwable,
            @Nullable String resourceType,
            @Nullable String resourceKey,
            @StringRes int fallbackMessageResId) {
        
        // Determine error type
        ErrorType errorType = determineErrorType(throwable);
        
        // Determine error severity
        ErrorSeverity severity = determineErrorSeverity(errorType, resourceType);
        
        // Get error message
        String errorMessage = getErrorMessage(errorType, throwable, resourceType, resourceKey, fallbackMessageResId);
        
        // Log error
        logError(errorType, severity, errorMessage, throwable, resourceType, resourceKey);
        
        // Show user feedback if activity is available
        if (activity != null && !activity.isFinishing()) {
            showUserFeedback(activity, errorType, severity, errorMessage);
        }
        
        return errorType;
    }
    
    /**
     * Handle an error with a custom message
     * @param activity Activity context for UI feedback
     * @param throwable Error that occurred
     * @param customMessage Custom error message
     * @return ErrorType that was handled
     */
    public ErrorType handleError(
            @Nullable FragmentActivity activity,
            @NonNull Throwable throwable,
            @NonNull String customMessage) {
        
        // Determine error type
        ErrorType errorType = determineErrorType(throwable);
        
        // Determine error severity
        ErrorSeverity severity = determineErrorSeverity(errorType, null);
        
        // Log error
        logError(errorType, severity, customMessage, throwable, null, null);
        
        // Show user feedback if activity is available
        if (activity != null && !activity.isFinishing()) {
            showUserFeedback(activity, errorType, severity, customMessage);
        }
        
        return errorType;
    }
    
    /**
     * Handle a resource error from ResourceRepository
     * @param activity Activity context for UI feedback
     * @param resource Resource object with error
     * @param resourceType Type of resource being accessed
     * @param fallbackMessageResId Fallback message resource ID
     * @return ErrorType that was handled
     */
    public <T> ErrorType handleResourceError(
            @Nullable FragmentActivity activity,
            @NonNull Resource<T> resource,
            @NonNull String resourceType,
            @StringRes int fallbackMessageResId) {
        
        // Determine error type based on error message
        ErrorType errorType = determineErrorTypeFromMessage(resource.getMessage());
        
        // Determine error severity
        ErrorSeverity severity = determineErrorSeverity(errorType, resourceType);
        
        // Get error message
        String errorMessage = resource.getMessage() != null ? 
                resource.getMessage() : 
                context.getString(fallbackMessageResId);
        
        // Log error
        logError(errorType, severity, errorMessage, null, resourceType, null);
        
        // Show user feedback if activity is available and we have data (don't show error if we have fallback data)
        if (activity != null && !activity.isFinishing() && (resource.getData() == null || severity == ErrorSeverity.CRITICAL)) {
            showUserFeedback(activity, errorType, severity, errorMessage);
        }
        
        return errorType;
    }
    
    /**
     * Determine the type of error based on the exception
     * @param throwable Error that occurred
     * @return ErrorType
     */
    private ErrorType determineErrorType(Throwable throwable) {
        if (throwable instanceof UnknownHostException || 
            throwable instanceof ConnectException) {
            return !networkUtils.isConnected() ? ErrorType.OFFLINE : ErrorType.NETWORK;
        } else if (throwable instanceof SocketTimeoutException || 
                  throwable instanceof TimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            if (code >= 500) {
                return ErrorType.SERVER;
            } else if (code == 404) {
                return ErrorType.RESOURCE_NOT_FOUND;
            } else if (code == 401 || code == 403) {
                return ErrorType.AUTHENTICATION_REQUIRED;
            } else {
                return ErrorType.CLIENT;
            }
        } else if (throwable instanceof IOException) {
            return !networkUtils.isConnected() ? ErrorType.OFFLINE : ErrorType.NETWORK;
        }
        
        return ErrorType.UNKNOWN;
    }
    
    /**
     * Determine the type of error based on the error message
     * @param errorMessage Error message
     * @return ErrorType
     */
    private ErrorType determineErrorTypeFromMessage(String errorMessage) {
        if (errorMessage == null) {
            return ErrorType.UNKNOWN;
        }
        
        String lowerCaseMessage = errorMessage.toLowerCase();
        
        if (lowerCaseMessage.contains("network") || 
            lowerCaseMessage.contains("connection") ||
            lowerCaseMessage.contains("connect")) {
            return !networkUtils.isConnected() ? ErrorType.OFFLINE : ErrorType.NETWORK;
        } else if (lowerCaseMessage.contains("timeout")) {
            return ErrorType.TIMEOUT;
        } else if (lowerCaseMessage.contains("server")) {
            return ErrorType.SERVER;
        } else if (lowerCaseMessage.contains("not found") || 
                  lowerCaseMessage.contains("404")) {
            return ErrorType.RESOURCE_NOT_FOUND;
        } else if (lowerCaseMessage.contains("expired")) {
            return ErrorType.RESOURCE_EXPIRED;
        } else if (lowerCaseMessage.contains("invalid")) {
            return ErrorType.RESOURCE_INVALID;
        } else if (lowerCaseMessage.contains("permission") || 
                  lowerCaseMessage.contains("denied")) {
            return ErrorType.PERMISSION_DENIED;
        } else if (lowerCaseMessage.contains("auth") || 
                  lowerCaseMessage.contains("login") ||
                  lowerCaseMessage.contains("401") ||
                  lowerCaseMessage.contains("403")) {
            return ErrorType.AUTHENTICATION_REQUIRED;
        }
        
        return ErrorType.UNKNOWN;
    }
    
    /**
     * Determine the severity of an error based on the error type and resource type
     * @param errorType Type of error
     * @param resourceType Type of resource being accessed
     * @return ErrorSeverity
     */
    private ErrorSeverity determineErrorSeverity(ErrorType errorType, @Nullable String resourceType) {
        // Critical resources that are essential for app functionality
        boolean isCriticalResource = resourceType != null && (
                resourceType.equals(ResourceRepository.RESOURCE_TYPE_TEMPLATE) ||
                resourceType.equals("user") ||
                resourceType.equals("config")
        );
        
        switch (errorType) {
            case OFFLINE:
                return isCriticalResource ? ErrorSeverity.ERROR : ErrorSeverity.WARNING;
                
            case NETWORK:
            case TIMEOUT:
                return isCriticalResource ? ErrorSeverity.ERROR : ErrorSeverity.WARNING;
                
            case SERVER:
                return ErrorSeverity.ERROR;
                
            case AUTHENTICATION_REQUIRED:
                return ErrorSeverity.CRITICAL;
                
            case RESOURCE_NOT_FOUND:
            case RESOURCE_EXPIRED:
                return isCriticalResource ? ErrorSeverity.ERROR : ErrorSeverity.WARNING;
                
            case RESOURCE_INVALID:
            case CLIENT:
                return ErrorSeverity.WARNING;
                
            case PERMISSION_DENIED:
                return ErrorSeverity.ERROR;
                
            case UNKNOWN:
            default:
                return isCriticalResource ? ErrorSeverity.ERROR : ErrorSeverity.WARNING;
        }
    }
    
    /**
     * Get an appropriate error message based on the error type
     * @param errorType Type of error
     * @param throwable Error that occurred
     * @param resourceType Type of resource being accessed
     * @param resourceKey Key of resource being accessed
     * @param fallbackMessageResId Fallback message resource ID
     * @return Error message
     */
    private String getErrorMessage(
            ErrorType errorType,
            @Nullable Throwable throwable,
            @Nullable String resourceType,
            @Nullable String resourceKey,
            @StringRes int fallbackMessageResId) {
        
        int messageResId;
        
        switch (errorType) {
            case OFFLINE:
                messageResId = R.string.error_offline;
                break;
                
            case NETWORK:
                messageResId = R.string.error_network;
                break;
                
            case TIMEOUT:
                messageResId = R.string.error_timeout;
                break;
                
            case SERVER:
                messageResId = R.string.error_server;
                break;
                
            case CLIENT:
                messageResId = R.string.error_client;
                break;
                
            case RESOURCE_NOT_FOUND:
                messageResId = R.string.error_resource_not_found;
                break;
                
            case RESOURCE_EXPIRED:
                messageResId = R.string.error_resource_expired;
                break;
                
            case RESOURCE_INVALID:
                messageResId = R.string.error_resource_invalid;
                break;
                
            case PERMISSION_DENIED:
                messageResId = R.string.error_permission_denied;
                break;
                
            case AUTHENTICATION_REQUIRED:
                messageResId = R.string.error_authentication_required;
                break;
                
            case UNKNOWN:
            default:
                messageResId = fallbackMessageResId;
                break;
        }
        
        try {
            String message = context.getString(messageResId);
            
            // Add resource type and key if available
            if (resourceType != null) {
                String resourceTypeFormatted = resourceType.substring(0, 1).toUpperCase() + resourceType.substring(1);
                if (resourceKey != null) {
                    message += " " + context.getString(R.string.error_resource_details, resourceTypeFormatted, resourceKey);
                } else {
                    message += " " + context.getString(R.string.error_resource_type, resourceTypeFormatted);
                }
            }
            
            return message;
        } catch (Exception e) {
            // If string resource is not found, use throwable message or fallback
            return throwable != null ? throwable.getMessage() : context.getString(fallbackMessageResId);
        }
    }
    
    /**
     * Log an error with appropriate details
     * @param errorType Type of error
     * @param severity Severity of error
     * @param errorMessage Error message
     * @param throwable Error that occurred
     * @param resourceType Type of resource being accessed
     * @param resourceKey Key of resource being accessed
     */
    private void logError(
            ErrorType errorType,
            ErrorSeverity severity,
            String errorMessage,
            @Nullable Throwable throwable,
            @Nullable String resourceType,
            @Nullable String resourceKey) {
        
        String logMessage = String.format(
                "Error [%s] [%s]: %s",
                errorType.name(),
                severity.name(),
                errorMessage
        );
        
        if (resourceType != null) {
            logMessage += String.format(" (Resource: %s", resourceType);
            if (resourceKey != null) {
                logMessage += ":" + resourceKey;
            }
            logMessage += ")";
        }
        
        switch (severity) {
            case CRITICAL:
            case ERROR:
                Log.e(TAG, logMessage, throwable);
                break;
                
            case WARNING:
                Log.w(TAG, logMessage, throwable);
                break;
                
            case INFO:
            default:
                Log.i(TAG, logMessage, throwable);
                break;
        }
    }
    
    /**
     * Show appropriate user feedback based on error severity
     * @param activity Activity context
     * @param errorType Type of error
     * @param severity Severity of error
     * @param errorMessage Error message
     */
    private void showUserFeedback(
            @NonNull FragmentActivity activity,
            ErrorType errorType,
            ErrorSeverity severity,
            String errorMessage) {
        
        // Check if we've shown this error recently
        String errorKey = errorType.name() + ":" + errorMessage;
        long now = System.currentTimeMillis();
        
        if (errorCache.containsKey(errorKey)) {
            long lastShown = errorCache.get(errorKey);
            if (now - lastShown < ERROR_CACHE_DURATION) {
                // Skip showing this error again so soon
                return;
            }
        }
        
        // Update error cache
        errorCache.put(errorKey, now);
        
        // Show appropriate feedback based on severity
        switch (severity) {
            case CRITICAL:
                // Show dialog for critical errors
                ErrorDialogFragment.newInstance(errorMessage, errorType)
                        .show(activity.getSupportFragmentManager(), "error_dialog");
                break;
                
            case ERROR:
                // Show snackbar for errors
                Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        errorMessage,
                        Snackbar.LENGTH_LONG
                ).show();
                break;
                
            case WARNING:
                // Show toast for warnings
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
                break;
                
            case INFO:
                // Log only for info
                break;
        }
    }
    
    /**
     * Clear the error cache
     */
    public void clearErrorCache() {
        errorCache.clear();
    }
    
    /**
     * Handle an error with specified type, message, and severity
     * @param type Type of error
     * @param message Error message
     * @param severity Severity of error
     */
    public void handleError(ErrorType type, String message, ErrorSeverity severity) {
        // Log error
        logError(type, severity, message, null, null, null);
        
        // We don't show UI feedback here since we don't have an activity context
        // This method is primarily used for logging errors from background operations
    }
} 