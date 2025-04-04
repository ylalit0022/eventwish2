package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.model.ServerTimeResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for managing time synchronization with server
 */
public class TimeUtils {
    private static final String TAG = "TimeUtils";
    
    // Difference between server and device time in milliseconds
    private static long serverTimeDiff = 0;
    
    // Last synced server date string
    private static String lastSyncDate = null;
    
    // Time when the last sync happened (device time)
    private static long lastSyncTimeMillis = 0;
    
    // Sync interval (sync every 30 minutes)
    private static final long SYNC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);
    
    // Retry intervals
    private static final long[] RETRY_INTERVALS_MS = {
        TimeUnit.SECONDS.toMillis(5),
        TimeUnit.SECONDS.toMillis(15),
        TimeUnit.SECONDS.toMillis(30),
        TimeUnit.MINUTES.toMillis(2)
    };
    
    // Current retry attempt
    private static int currentRetryAttempt = 0;
    
    // Maximum retry attempts
    private static final int MAX_RETRY_ATTEMPTS = RETRY_INTERVALS_MS.length;
    
    // Flag to indicate if sync is in progress
    private static boolean isSyncInProgress = false;
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "time_sync_prefs";
    private static final String KEY_SERVER_TIME_DIFF = "server_time_diff";
    private static final String KEY_LAST_SYNC_DATE = "last_sync_date";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    
    /**
     * Initialize TimeUtils from saved preferences
     * Should be called during app initialization
     */
    public static void initialize(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize TimeUtils with null context");
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        serverTimeDiff = prefs.getLong(KEY_SERVER_TIME_DIFF, 0);
        lastSyncDate = prefs.getString(KEY_LAST_SYNC_DATE, null);
        lastSyncTimeMillis = prefs.getLong(KEY_LAST_SYNC_TIME, 0);
        
        Log.d(TAG, "TimeUtils initialized with saved values: diff=" + serverTimeDiff + 
              "ms, lastSync=" + lastSyncDate);
        
        // Sync immediately if never synced or sync interval has passed
        if (lastSyncDate == null || System.currentTimeMillis() - lastSyncTimeMillis > SYNC_INTERVAL_MS) {
            syncWithServer();
        }
    }

    /**
     * Update the time difference between server and device
     * @param serverTimestamp Server timestamp in milliseconds
     * @param serverDate Formatted server date string
     */
    public static void syncWithServerTime(long serverTimestamp, String serverDate) {
        // Calculate difference (positive if server ahead, negative if behind)
        serverTimeDiff = serverTimestamp - System.currentTimeMillis();
        lastSyncDate = serverDate;
        lastSyncTimeMillis = System.currentTimeMillis();
        
        Log.d(TAG, "Time synced with server. Difference: " + serverTimeDiff + "ms");
        Log.d(TAG, "Server date: " + serverDate);
        
        // Reset retry counter on successful sync
        currentRetryAttempt = 0;
        isSyncInProgress = false;
        
        // Save to SharedPreferences
        Context context = EventWishApplication.getAppContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(KEY_SERVER_TIME_DIFF, serverTimeDiff);
            editor.putString(KEY_LAST_SYNC_DATE, lastSyncDate);
            editor.putLong(KEY_LAST_SYNC_TIME, lastSyncTimeMillis);
            editor.apply();
            
            Log.d(TAG, "Time sync data saved to preferences");
        } else {
            Log.e(TAG, "Could not save time sync data: application context is null");
        }
    }

    /**
     * Actively sync with server by making an API call
     * Includes retry mechanism for error handling
     */
    public static void syncWithServer() {
        if (isSyncInProgress) {
            Log.d(TAG, "Time sync already in progress, skipping");
            return;
        }
        
        isSyncInProgress = true;
        
        try {
            if (!isApiClientInitialized()) {
                Log.e(TAG, "ApiClient not initialized, rescheduling sync");
                retrySync();
                return;
            }
            
            ApiService apiService = ApiClient.getClient();
            Call<ServerTimeResponse> call = apiService.getServerTime();
            
            // Add request timestamp for more accurate sync
            long requestTime = System.currentTimeMillis();
            
            call.enqueue(new Callback<ServerTimeResponse>() {
                @Override
                public void onResponse(Call<ServerTimeResponse> call, Response<ServerTimeResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ServerTimeResponse timeResponse = response.body();
                        long responseTime = System.currentTimeMillis();
                        long networkDelay = (responseTime - requestTime) / 2; // Estimate one-way delay
                        
                        // Adjust server timestamp by estimated network delay
                        long adjustedServerTime = timeResponse.getTimestamp() + networkDelay;
                        
                        syncWithServerTime(adjustedServerTime, timeResponse.getFormatted());
                        Log.d(TAG, "Server time sync successful: " + timeResponse.getFormatted());
                    } else {
                        Log.e(TAG, "Failed to sync server time: " + response.code() + " " + response.message());
                        retrySync();
                    }
                }

                @Override
                public void onFailure(Call<ServerTimeResponse> call, Throwable t) {
                    Log.e(TAG, "Error syncing server time: " + t.getMessage(), t);
                    retrySync();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during server time sync: " + e.getMessage(), e);
            retrySync();
        }
    }
    
    /**
     * Check if API client is initialized
     */
    private static boolean isApiClientInitialized() {
        try {
            ApiClient.getClient();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Retry sync with exponential backoff
     */
    private static void retrySync() {
        if (currentRetryAttempt >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Max retry attempts reached for time sync");
            isSyncInProgress = false;
            return;
        }
        
        long retryDelay = RETRY_INTERVALS_MS[currentRetryAttempt];
        Log.d(TAG, "Scheduling time sync retry " + (currentRetryAttempt + 1) + 
                " in " + retryDelay + "ms");
        
        // Handler to retry after delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            currentRetryAttempt++;
            isSyncInProgress = false; // Reset flag before retrying
            syncWithServer();
        }, retryDelay);
    }

    /**
     * Get current time according to server
     * @return Date object with server time
     */
    public static Date getCurrentServerTime() {
        return new Date(System.currentTimeMillis() + serverTimeDiff);
    }

    /**
     * Get current server time in milliseconds since epoch
     * @return Current server time in milliseconds
     */
    public static long getCurrentServerTimeMillis() {
        return System.currentTimeMillis() + serverTimeDiff;
    }

    /**
     * Get current server time in ISO 8601 format
     * @return ISO 8601 formatted string in UTC timezone
     */
    public static String getCurrentServerTimeISO() {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(getCurrentServerTime());
    }

    /**
     * Get Calendar instance with server time
     * @return Calendar instance set to server time in UTC timezone
     */
    public static Calendar getServerCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(System.currentTimeMillis() + serverTimeDiff);
        return calendar;
    }

    /**
     * Add days to server time
     * @param days Number of days to add
     * @return Date with days added to server time
     */
    public static Date addDaysToServerTime(int days) {
        Calendar calendar = getServerCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    /**
     * Add months to server time
     * @param months Number of months to add
     * @return Date with months added to server time
     */
    public static Date addMonthsToServerTime(int months) {
        Calendar calendar = getServerCalendar();
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }

    /**
     * Check if time is synced with server
     * @return true if time is synced with server
     */
    public static boolean isTimeSynced() {
        return lastSyncDate != null;
    }

    /**
     * Check if sync is needed (if last sync was more than sync interval ago)
     * @return true if sync is needed
     */
    public static boolean isSyncNeeded() {
        return !isTimeSynced() || 
               System.currentTimeMillis() - lastSyncTimeMillis > SYNC_INTERVAL_MS;
    }

    /**
     * Get last sync date
     * @return Last sync date string
     */
    public static String getLastSyncDate() {
        return lastSyncDate;
    }

    /**
     * Get time difference with server in milliseconds
     * Positive if server ahead, negative if behind
     * @return Time difference in milliseconds
     */
    public static long getServerTimeDifference() {
        return serverTimeDiff;
    }
    
    /**
     * Format a date to RFC 3339 format
     * @param date Date to format
     * @return Formatted date string
     */
    public static String formatToRFC3339(Date date) {
        SimpleDateFormat rfc3339Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        rfc3339Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return rfc3339Format.format(date);
    }
    
    /**
     * Format date to a user-friendly format
     * @param date Date to format
     * @return Formatted date string
     */
    public static String formatDateForDisplay(Date date) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.US);
        return displayFormat.format(date);
    }
} 