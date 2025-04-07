package com.ds.eventwish.data.model.ads;

import android.content.Context;
import android.util.Log;

/**
 * Constants for AdMob integration
 */
public class AdConstants {
    private static final String TAG = "AdConstants";
    
    /**
     * Ad Types
     */
    public static class AdType {
        public static final String BANNER = "banner";
        public static final String INTERSTITIAL = "interstitial";
        public static final String REWARDED = "rewarded";
        public static final String NATIVE = "native";
        public static final String APP_OPEN = "app_open";
    }
    
    /**
     * Request settings
     */
    public static class RequestSettings {
        public static final long MIN_REQUEST_INTERVAL = 60000; // 1 minute
        public static final int MAX_RETRY_COUNT = 3;
        public static final long RETRY_BACKOFF_MS = 1000; // 1 second
    }
    
    /**
     * Test ad unit IDs
     */
    public static class TestAdUnits {
        public static final String BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111";
        public static final String INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712";
        public static final String REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917";
        public static final String NATIVE_ADVANCED_TEST_ID = "ca-app-pub-3940256099942544/2247696110";
        public static final String APP_OPEN_TEST_ID = "ca-app-pub-3940256099942544/9257395921";
    }
    
    /**
     * SharedPreferences keys
     */
    public static class Preferences {
        public static final String PREF_FILE = "admob_preferences";
        public static final String CACHED_AD_UNITS = "cached_ad_units";
        public static final String LAST_AD_FETCH_TIME = "last_ad_fetch_time";
        public static final String TEST_MODE_ENABLED = "test_mode_enabled";
        public static final String AD_FEATURE_ENABLED = "ad_feature_enabled";
        public static final String LAST_SYNC_TIME = "last_sync_time";
        public static final String STORED_ENGAGEMENT_DATA = "stored_engagement_data";
    }
    
    /**
     * HTTP Headers
     */
    public static class Headers {
        public static final String API_KEY = "x-api-key";
        public static final String DEVICE_ID = "x-device-id";
        public static final String APP_SIGNATURE = "x-app-signature";
    }
    
    /**
     * Signature constants
     */
    public static class Signature {
        // The secret key used for signing
        public static final String SECRET_KEY = "c1ce47afeff9fa8b7b1aa165562cb915b448007f8b5c863bac496b265b0518f3";
        
        // The working app signature - verified to work with the server
        public static final String APP_SIGNATURE = "app_sig_1";
        
        // App package name for signature generation
        public static final String APP_PACKAGE = "com.ds.eventwish";
    }
} 