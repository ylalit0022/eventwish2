package com.ds.eventwish.data.ads;

/**
 * Constants for ad-related functionality.
 * Contains ad unit IDs and other ad-related constants.
 */
public class AdConstants {
    // Test ad unit IDs (replace with actual IDs in production)
    public static final String TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";
    public static final String TEST_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115";
    public static final String TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294";
    
    // Production ad unit IDs (to be replaced with actual IDs)
    public static final String PROD_BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_APP_OPEN_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    
    // Use test ads for development
    public static final boolean USE_TEST_ADS = true;
    
    // Get the appropriate ad unit ID based on the USE_TEST_ADS flag
    public static String getBannerAdUnitId() {
        return USE_TEST_ADS ? TEST_BANNER_AD_UNIT_ID : PROD_BANNER_AD_UNIT_ID;
    }
    
    public static String getInterstitialAdUnitId() {
        return USE_TEST_ADS ? TEST_INTERSTITIAL_AD_UNIT_ID : PROD_INTERSTITIAL_AD_UNIT_ID;
    }
    
    public static String getRewardedAdUnitId() {
        return USE_TEST_ADS ? TEST_REWARDED_AD_UNIT_ID : PROD_REWARDED_AD_UNIT_ID;
    }
    
    public static String getNativeAdUnitId() {
        return USE_TEST_ADS ? TEST_NATIVE_AD_UNIT_ID : PROD_NATIVE_AD_UNIT_ID;
    }
    
    public static String getNativeVideoAdUnitId() {
        return USE_TEST_ADS ? TEST_NATIVE_VIDEO_AD_UNIT_ID : PROD_NATIVE_VIDEO_AD_UNIT_ID;
    }
    
    public static String getAppOpenAdUnitId() {
        return USE_TEST_ADS ? TEST_APP_OPEN_AD_UNIT_ID : PROD_APP_OPEN_AD_UNIT_ID;
    }
    
    // Ad refresh intervals (in milliseconds)
    public static final long BANNER_REFRESH_INTERVAL = 60000; // 1 minute
    public static final long INTERSTITIAL_REFRESH_INTERVAL = 300000; // 5 minutes
    public static final long REWARDED_REFRESH_INTERVAL = 300000; // 5 minutes
    
    // Ad display thresholds
    public static final int INTERSTITIAL_DISPLAY_THRESHOLD = 3; // Show interstitial every 3 actions
    
    // Ad cache size
    public static final int MAX_INTERSTITIAL_CACHE_SIZE = 2;
    public static final int MAX_REWARDED_CACHE_SIZE = 2;
    
    // Ad timeout (in milliseconds)
    public static final long AD_LOAD_TIMEOUT = 30000; // 30 seconds
} 