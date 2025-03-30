package com.ds.eventwish.utils;

import android.util.Log;
import com.ds.eventwish.BuildConfig;

public class ApiConstants {
    private static final String TAG = "ApiConstants";
    
    // Make sure this matches exactly with the server's API_KEY
    public static final String API_KEY = BuildConfig.API_KEY;
    public static final String BASE_URL = "https://eventwish2.onrender.com/api/";
    
    // Add this for debugging
    public static final boolean DEBUG = true;
    
    static {
        // Log the API key for debugging (only first few characters)
        if (DEBUG) {
            Log.d(TAG, "Using API key: " + (API_KEY != null ? API_KEY.substring(0, Math.min(API_KEY.length(), 10)) + "..." : "null"));
        }
    }
    
    private ApiConstants() {
        // Private constructor to prevent instantiation
    }
}
