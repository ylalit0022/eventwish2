package com.ds.eventwish.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced logging utility for EventWish application with visual indicators, 
 * filtering capabilities, and performance tracking
 */
public class LogUtils {
    // Logging state
    private static final AtomicBoolean VERBOSE_LOGGING = new AtomicBoolean(false);
    private static final AtomicBoolean VISUAL_INDICATORS = new AtomicBoolean(true);
    private static final Map<String, Long> timers = new HashMap<>();
    
    // Emoji indicators for different log categories
    private static final String ICON_NETWORK = "🌐";
    private static final String ICON_DATABASE = "💾";
    private static final String ICON_UI = "🖼️";
    private static final String ICON_CATEGORY = "📂";
    private static final String ICON_TEMPLATE = "📝";
    private static final String ICON_SUCCESS = "✅";
    private static final String ICON_ERROR = "❌";
    private static final String ICON_WARNING = "⚠️";
    private static final String ICON_INFO = "ℹ️";
    private static final String ICON_DEBUG = "🔍";
    private static final String ICON_LIFECYCLE = "♻️";
    private static final String ICON_PAGING = "📄";
    private static final String ICON_PERF = "⚡";
    private static final String ICON_CACHE = "🔄";
    
    /**
     * Log categories for consistent tagging
     */
    public enum Category {
        NETWORK(ICON_NETWORK),
        DATABASE(ICON_DATABASE), 
        UI(ICON_UI),
        CATEGORY(ICON_CATEGORY),
        TEMPLATE(ICON_TEMPLATE),
        LIFECYCLE(ICON_LIFECYCLE),
        PAGING(ICON_PAGING),
        PERFORMANCE(ICON_PERF),
        CACHE(ICON_CACHE);
        
        private final String icon;
        
        Category(String icon) {
            this.icon = icon;
        }
        
        public String getIcon() {
            return icon;
        }
    }
    
    /**
     * Enable or disable verbose logging
     * @param enabled whether verbose logging should be enabled
     */
    public static void setVerboseLogging(boolean enabled) {
        VERBOSE_LOGGING.set(enabled);
        d("LogUtils", "Verbose logging " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Enable or disable visual indicators (emojis) in log messages
     * @param enabled whether visual indicators should be enabled
     */
    public static void setVisualIndicators(boolean enabled) {
        VISUAL_INDICATORS.set(enabled);
    }
    
    /**
     * Debug log with category indicator
     */
    public static void d(@NonNull String tag, @NonNull Category category, @NonNull String message) {
        if (shouldLog()) {
            Log.d(tag, formatMessage(category.getIcon(), message));
        }
    }
    
    /**
     * Error log with category indicator
     */
    public static void e(@NonNull String tag, @NonNull Category category, @NonNull String message, @Nullable Throwable e) {
        Log.e(tag, formatMessage(ICON_ERROR + " " + category.getIcon(), message), e);
    }
    
    /**
     * Warning log with category indicator
     */
    public static void w(@NonNull String tag, @NonNull Category category, @NonNull String message) {
        Log.w(tag, formatMessage(ICON_WARNING + " " + category.getIcon(), message));
    }
    
    /**
     * Info log with category indicator
     */
    public static void i(@NonNull String tag, @NonNull Category category, @NonNull String message) {
        Log.i(tag, formatMessage(ICON_INFO + " " + category.getIcon(), message));
    }
    
    /**
     * Basic debug log without category
     */
    public static void d(@NonNull String tag, @NonNull String message) {
        if (shouldLog()) {
            Log.d(tag, formatMessage(ICON_DEBUG, message));
        }
    }
    
    /**
     * Basic error log without category
     */
    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        Log.e(tag, formatMessage(ICON_ERROR, message), e);
    }
    
    /**
     * Basic warning log without category
     */
    public static void w(@NonNull String tag, @NonNull String message) {
        Log.w(tag, formatMessage(ICON_WARNING, message));
    }
    
    /**
     * Basic info log without category
     */
    public static void i(@NonNull String tag, @NonNull String message) {
        Log.i(tag, formatMessage(ICON_INFO, message));
    }
    
    /**
     * Log success message
     */
    public static void success(@NonNull String tag, @NonNull String message) {
        Log.d(tag, formatMessage(ICON_SUCCESS, message));
    }
    
    /**
     * Format message with visual indicator if enabled
     */
    private static String formatMessage(String icon, String message) {
        return VISUAL_INDICATORS.get() ? icon + " " + message : message;
    }
    
    /**
     * Check if we should log based on current settings
     */
    public static boolean shouldLog() {
        return VERBOSE_LOGGING.get() || com.ds.eventwish.BuildConfig.DEBUG;
    }
    
    /**
     * Start timing an operation for performance tracking
     * @param operationName unique name to identify the operation
     */
    public static void startTimer(@NonNull String tag, @NonNull String operationName) {
        timers.put(tag + ":" + operationName, System.currentTimeMillis());
        if (shouldLog()) {
            d(tag, Category.PERFORMANCE, "⏱️ Started timing: " + operationName);
        }
    }
    
    /**
     * End timing an operation and log the elapsed time
     * @param operationName the operation name used in startTimer
     * @return elapsed time in milliseconds
     */
    public static long endTimer(@NonNull String tag, @NonNull String operationName) {
        String key = tag + ":" + operationName;
        Long startTime = timers.remove(key);
        
        if (startTime == null) {
            w(tag, Category.PERFORMANCE, "⏱️ No timer found for: " + operationName);
            return -1;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        d(tag, Category.PERFORMANCE, String.format("⏱️ %s completed in %d ms", operationName, elapsed));
        return elapsed;
    }
    
    /**
     * Log a paging event
     */
    public static void paging(@NonNull String tag, int page, int totalItems) {
        if (shouldLog()) {
            d(tag, Category.PAGING, String.format("📄 Page %d loaded, total items: %d", page, totalItems));
        }
    }
    
    /**
     * Log a caching event
     */
    public static void cache(@NonNull String tag, @NonNull String operation, @NonNull String key) {
        if (shouldLog()) {
            d(tag, Category.CACHE, String.format("🔄 Cache %s: %s", operation, key));
        }
    }
    
    /**
     * Log a network event
     */
    public static void network(@NonNull String tag, @NonNull String operation, @NonNull String endpoint) {
        if (shouldLog()) {
            d(tag, Category.NETWORK, String.format("🌐 %s: %s", operation, endpoint));
        }
    }
    
    /**
     * Log a template operation
     */
    public static void template(@NonNull String tag, @NonNull String operation, @Nullable String templateId) {
        if (shouldLog()) {
            String idInfo = templateId != null ? " [ID: " + templateId + "]" : "";
            d(tag, Category.TEMPLATE, String.format("📝 %s%s", operation, idInfo));
        }
    }
    
    /**
     * Log a category operation
     */
    public static void category(@NonNull String tag, @NonNull String operation, @Nullable String categoryName) {
        if (shouldLog()) {
            String catInfo = categoryName != null ? " '" + categoryName + "'" : "";
            d(tag, Category.CATEGORY, String.format("📂 %s%s", operation, catInfo));
        }
    }
    
    /**
     * Log a lifecycle event
     */
    public static void lifecycle(@NonNull String tag, @NonNull String state, @Nullable String detail) {
        if (shouldLog()) {
            String detailInfo = detail != null ? ": " + detail : "";
            d(tag, Category.LIFECYCLE, String.format("♻️ %s%s", state, detailInfo));
        }
    }
} 