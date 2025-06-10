package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manages authentication state persistence using SharedPreferences
 */
public class AuthStateManager {
    private static final String TAG = "AuthStateManager";
    private static final String PREF_NAME = "auth_state";
    
    // Preference keys
    private static final String KEY_USER_AUTHENTICATED = "user_authenticated";
    private static final String KEY_AUTH_TIMESTAMP = "auth_timestamp";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "display_name";
    
    // Singleton instance
    private static volatile AuthStateManager instance;
    
    // SharedPreferences instance
    private final SharedPreferences prefs;
    
    /**
     * Get the singleton instance of AuthStateManager
     * @param context Application context
     * @return AuthStateManager instance
     */
    public static AuthStateManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthStateManager.class) {
                if (instance == null) {
                    instance = new AuthStateManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private AuthStateManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "AuthStateManager initialized");
    }
    
    /**
     * Set the user as authenticated
     * @param userId User ID
     * @param email User email
     * @param displayName User display name
     */
    public void setAuthenticated(String userId, String email, String displayName) {
        try {
            long timestamp = System.currentTimeMillis();
            prefs.edit()
                .putBoolean(KEY_USER_AUTHENTICATED, true)
                .putLong(KEY_AUTH_TIMESTAMP, timestamp)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .apply();
            
            Log.d(TAG, "User authenticated: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error setting authenticated state", e);
        }
    }
    
    /**
     * Clear authentication state
     */
    public void clearAuthentication() {
        try {
            prefs.edit()
                .putBoolean(KEY_USER_AUTHENTICATED, false)
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_DISPLAY_NAME)
                .apply();
            
            Log.d(TAG, "Authentication state cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing authentication state", e);
        }
    }
    
    /**
     * Check if the user is authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return prefs.getBoolean(KEY_USER_AUTHENTICATED, false);
    }
    
    /**
     * Get the user ID
     * @return User ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get the user email
     * @return User email
     */
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }
    
    /**
     * Get the user display name
     * @return User display name
     */
    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }
    
    /**
     * Get the authentication timestamp
     * @return Authentication timestamp
     */
    public long getAuthTimestamp() {
        return prefs.getLong(KEY_AUTH_TIMESTAMP, 0);
    }
    
    /**
     * Check if the authentication is expired
     * @param expirationTimeMs Expiration time in milliseconds
     * @return true if expired, false otherwise
     */
    public boolean isAuthExpired(long expirationTimeMs) {
        long timestamp = getAuthTimestamp();
        long currentTime = System.currentTimeMillis();
        return (currentTime - timestamp) > expirationTimeMs;
    }
} 