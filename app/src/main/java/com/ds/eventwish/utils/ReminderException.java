package com.ds.eventwish.utils;

public class ReminderException extends Exception {
    public enum ErrorType {
        INVALID_REMINDER("Invalid reminder"),
        PERMISSION_DENIED("Permission denied"),
        SCHEDULING_ERROR("Failed to schedule reminder"),
        ALARM_MANAGER_ERROR("Alarm manager error"),
        WORK_MANAGER_ERROR("Work manager error"),
        SYSTEM_SERVICE_UNAVAILABLE("System service unavailable"),
        DATABASE_ERROR("Database error"),
        NOTIFICATION_ERROR("Failed to show notification"),
        UNKNOWN_ERROR("Unknown error");

        private final String defaultMessage;

        ErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    private final ErrorType errorType;
    private final String userFriendlyMessage;

    public ReminderException(ErrorType errorType, String message) {
        this(errorType, message, null);
    }

    public ReminderException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.userFriendlyMessage = getUserFriendlyMessage(errorType);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return getMessage();
    }

    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    private String getUserFriendlyMessage(ErrorType errorType) {
        switch (errorType) {
            case INVALID_REMINDER:
                return "The reminder is not valid. Please check your input and try again.";
            case PERMISSION_DENIED:
                return "Required permissions are not granted. Please grant the necessary permissions in Settings.";
            case SCHEDULING_ERROR:
                return "Failed to schedule the reminder. Please try again.";
            case ALARM_MANAGER_ERROR:
                return "Failed to set the alarm for this reminder. Please check your device settings.";
            case WORK_MANAGER_ERROR:
                return "Failed to schedule background work for this reminder. Please try again.";
            case SYSTEM_SERVICE_UNAVAILABLE:
                return "A required system service is not available. Please restart the app.";
            case DATABASE_ERROR:
                return "Unable to save the reminder. Please try again.";
            case NOTIFICATION_ERROR:
                return "Failed to show the notification. Please check your notification settings.";
            default:
                return "An unexpected error occurred. Please try again.";
        }
    }
}
