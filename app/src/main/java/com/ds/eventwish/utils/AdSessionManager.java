package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;

/**
 * Manages ad impressions sessions for the application
 * An ad session represents a single app launch-to-close cycle
 */
public class AdSessionManager {
    private static final String TAG = "AdSessionManager";
    private static final String PREFS_NAME = "sponsored_ad_tracking";
    private static final String KEY_APP_SESSION = "app_session";
    
    private static volatile AdSessionManager instance;
    private final Context applicationContext;
    private boolean debugMode = false;
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private AdSessionManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        
        // Create a new session immediately
        createNewSession();
    }
    
    /**
     * Get singleton instance
     */
    public static AdSessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AdSessionManager.class) {
                if (instance == null) {
                    instance = new AdSessionManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Enable or disable debug mode
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        if (enabled) {
            Log.d(TAG, "Debug mode enabled - logging will be verbose");
        }
    }
    
    /**
     * Log a debug message if debug mode is enabled
     */
    private void logDebug(String message) {
        if (debugMode) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Create a new impression session
     * This should be called when the app is started/resumed after being fully closed
     */
    public void createNewSession() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long sessionId = System.currentTimeMillis();
        prefs.edit().putLong(KEY_APP_SESSION, sessionId).apply();
        Log.d(TAG, "Created new ad impression session: " + sessionId);
        
        // Debug - dump existing session data
        if (debugMode) {
            dumpSessionData();
        }
    }
    
    /**
     * Get the current session ID
     */
    public long getCurrentSessionId() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_APP_SESSION, 0);
    }
    
    /**
     * Check if an ad has been tracked in the current session
     * @param adId The ad ID to check
     * @return true if already tracked in this session
     */
    public boolean isImpressionTracked(String adId) {
        if (adId == null) return false;
        
        long currentSession = getCurrentSessionId();
        if (currentSession == 0) {
            // No valid session, create a new one
            createNewSession();
            return false;
        }
        
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String impressionKey = "impression_" + adId + "_" + currentSession;
        boolean isTracked = prefs.getBoolean(impressionKey, false);
        
        logDebug("Checking if impression tracked for ad " + adId + 
                " in session " + currentSession + ": " + isTracked);
        
        return isTracked;
    }
    
    /**
     * Mark an ad as tracked in the current session
     * @param adId The ad ID to mark as tracked
     */
    public void trackImpression(String adId) {
        if (adId == null) return;
        
        long currentSession = getCurrentSessionId();
        if (currentSession == 0) {
            // No valid session, create a new one
            createNewSession();
            currentSession = getCurrentSessionId();
        }
        
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String impressionKey = "impression_" + adId + "_" + currentSession;
        prefs.edit().putBoolean(impressionKey, true).apply();
        Log.d(TAG, "Marked ad " + adId + " as tracked in session " + currentSession);
    }
    
    /**
     * Reset tracking for a specific ad in the current session
     * @param adId The ad ID to reset tracking for
     */
    public void resetTracking(String adId) {
        if (adId == null) return;
        
        long currentSession = getCurrentSessionId();
        if (currentSession == 0) return;
        
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String impressionKey = "impression_" + adId + "_" + currentSession;
        prefs.edit().remove(impressionKey).apply();
        Log.d(TAG, "Reset tracking for ad " + adId + " in session " + currentSession);
    }
    
    /**
     * Dump all session data for debugging
     */
    public void dumpSessionData() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long currentSession = prefs.getLong(KEY_APP_SESSION, 0);
        
        Log.d(TAG, "======== AD SESSION DATA DUMP ========");
        Log.d(TAG, "Current session ID: " + currentSession);
        
        // Count impressions
        int impressionCount = 0;
        
        // Get all keys
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Print all impression entries
            if (key.startsWith("impression_")) {
                Log.d(TAG, key + ": " + value);
                impressionCount++;
            }
        }
        
        Log.d(TAG, "Total impressions tracked: " + impressionCount);
        Log.d(TAG, "=====================================");
    }
    
    /**
     * Check if any tracked impressions exist in storage
     */
    public boolean hasTrackedImpressions() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Count impression keys
        Map<String, ?> allEntries = prefs.getAll();
        for (String key : allEntries.keySet()) {
            if (key.startsWith("impression_")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clear all tracked impressions data
     */
    public void clearAllTrackedImpressions() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Get all keys
        Map<String, ?> allEntries = prefs.getAll();
        int count = 0;
        
        for (String key : allEntries.keySet()) {
            if (key.startsWith("impression_")) {
                editor.remove(key);
                count++;
            }
        }
        
        editor.apply();
        Log.d(TAG, "Cleared " + count + " tracked impressions");
    }
    
    /**
     * Get the total number of tracked impressions in the current session
     * @return Count of tracked impressions
     */
    public int getTrackedImpressionCount() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long currentSession = prefs.getLong(KEY_APP_SESSION, 0);
        
        // Prefix for current session
        String sessionPrefix = "_" + currentSession;
        
        // Count impression keys for the current session
        int count = 0;
        Map<String, ?> allEntries = prefs.getAll();
        for (String key : allEntries.keySet()) {
            if (key.startsWith("impression_") && key.contains(sessionPrefix)) {
                count++;
            }
        }
        
        logDebug("Current session has " + count + " tracked impressions");
        return count;
    }
} 