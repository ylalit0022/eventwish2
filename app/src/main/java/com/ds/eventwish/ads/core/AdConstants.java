package com.ds.eventwish.ads.core;

/**
 * Constants used throughout the ad system.
 * This class provides centralized access to all ad-related constants.
 */
public final class AdConstants {
    private AdConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Ad Types
     */
    public static final class AdType {
        public static final String APP_OPEN = "app_open";
        public static final String BANNER = "banner";
        public static final String INTERSTITIAL = "interstitial";
        public static final String NATIVE = "native";
        public static final String REWARDED = "rewarded";
        public static final String NATIVE_VIDEO = "native_video";
    }

    /**
     * Cache Configuration
     */
    public static final class Cache {
        public static final long EXPIRY_TIME = 3600000L; // 1 hour in milliseconds
        public static final int MAX_SIZE = 50; // Maximum number of ad units to cache
        public static final long MIN_REQUEST_INTERVAL = 5000L; // 5 seconds between requests
        public static final long REFRESH_INTERVAL = 300000L; // 5 minutes
    }

    /**
     * Retry Configuration
     */
    public static final class Retry {
        public static final int MAX_ATTEMPTS = 3;
        public static final long INITIAL_DELAY_MS = 1000L; // 1 second
        public static final long MAX_DELAY_MS = 10000L; // 10 seconds
        public static final double BACKOFF_MULTIPLIER = 2.0;
    }

    /**
     * Timeout Configuration
     */
    public static final class Timeout {
        public static final long AD_LOAD = 30000L; // 30 seconds
        public static final long AD_SHOW = 5000L; // 5 seconds
        public static final long INIT = 10000L; // 10 seconds
    }

    /**
     * Error Messages
     */
    public static final class Error {
        public static final String NOT_INITIALIZED = "AdManager not initialized";
        public static final String NO_AD_UNITS = "No ad units available";
        public static final String INVALID_AD_TYPE = "Invalid ad type";
        public static final String AD_LOAD_TIMEOUT = "Ad load timeout";
        public static final String AD_SHOW_TIMEOUT = "Ad show timeout";
        public static final String AD_ALREADY_SHOWING = "Ad is already showing";
        public static final String AD_NOT_READY = "Ad is not ready to show";
        public static final String SDK_NOT_INITIALIZED = "Ad SDK initialization failed";
        public static final String ADS_DISABLED = "Ad display is currently disabled";
        public static final String NULL_CONTEXT = "Invalid application context";
        public static final String NO_API_KEY = "Missing API configuration";
        public static final String NETWORK_ERROR = "Network error occurred";
        public static final String SERVER_ERROR = "Server error occurred";
        public static final String INVALID_RESPONSE = "Invalid response from server";
    }

    /**
     * Analytics Events
     */
    public static final class Events {
        public static final String AD_LOADED = "ad_loaded";
        public static final String AD_FAILED = "ad_failed";
        public static final String AD_SHOWN = "ad_shown";
        public static final String AD_CLICKED = "ad_clicked";
        public static final String AD_CLOSED = "ad_closed";
        public static final String AD_IMPRESSION = "ad_impression";
        public static final String AD_REVENUE = "ad_revenue";
        public static final String AD_REWARD_EARNED = "ad_reward_earned";
        public static final String AD_SKIPPED = "ad_skipped";
    }

    /**
     * Debug Tags
     */
    public static final class Tags {
        public static final String MANAGER = "AdManager";
        public static final String APP_OPEN = "AppOpenManager";
        public static final String INTERSTITIAL = "InterstitialManager";
        public static final String NATIVE = "NativeManager";
        public static final String BANNER = "BannerManager";
        public static final String REWARDED = "RewardedManager";
        public static final String CACHE = "AdCache";
        public static final String NETWORK = "AdNetwork";
    }

    /**
     * SharedPreferences Configuration
     */
    public static final class Prefs {
        public static final String FILE_NAME = "ad_prefs";
        public static final String CACHED_AD_UNITS = "cached_ad_units";
        public static final String LAST_FETCH_TIME = "last_ad_fetch_time";
        public static final String TEST_MODE = "test_mode_enabled";
        public static final String FEATURE_ENABLED = "ad_feature_enabled";
        public static final String LAST_SYNC = "last_sync_time";
        public static final String ENGAGEMENT_DATA = "stored_engagement_data";
        public static final String USER_CONSENT = "user_consent";
        public static final String GDPR_CONSENT = "gdpr_consent";
    }

    /**
     * HTTP Headers
     */
    public static final class Headers {
        public static final String API_KEY = "x-api-key";
        public static final String DEVICE_ID = "x-device-id";
        public static final String APP_SIGNATURE = "x-app-signature";
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String ACCEPT = "Accept";
    }

    /**
     * Test Ad Unit IDs
     */
    public static final class TestAds {
        // Test ad unit IDs from AdMob
        public static final String BANNER = "ca-app-pub-3940256099942544/6300978111";
        public static final String INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
        public static final String REWARDED = "ca-app-pub-3940256099942544/5224354917";
        public static final String NATIVE_ADVANCED = "ca-app-pub-3940256099942544/2247696110";
        public static final String APP_OPEN = "ca-app-pub-3940256099942544/3419835294";
    }

    /**
     * Ad Display Settings
     */
    public static final class Display {
        public static final int MIN_INTERVAL_BETWEEN_ADS = 60000; // 1 minute
        public static final int MAX_ADS_PER_SESSION = 10;
        public static final int BANNER_REFRESH_INTERVAL = 60000; // 1 minute
        public static final int NATIVE_REFRESH_INTERVAL = 300000; // 5 minutes
        public static final int INTERSTITIAL_INTERVAL = 180000; // 3 minutes
    }
} 