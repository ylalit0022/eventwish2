package com.ds.eventwish.config;

/**
 * Configuration for AdMob ads
 */
public class AdConfig {
    // Test device IDs for testing ads
    public static final String[] TEST_DEVICE_IDS = new String[] {
        "ABCDEF1234567890", // Add real test device IDs here
        "EMULATOR"
    };
    
    // Ad unit IDs for testing
    public static final String TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
    
    // Production ad unit IDs
    public static final String PROD_BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    public static final String PROD_APP_OPEN_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN";
    
    // Ad configuration
    public static final boolean USE_TEST_ADS = true; // Set to false for production
    
    // Get appropriate ad unit IDs based on test mode
    public static String getBannerAdUnitId() {
        return USE_TEST_ADS ? TEST_BANNER_AD_UNIT_ID : PROD_BANNER_AD_UNIT_ID;
    }
    
    public static String getInterstitialAdUnitId() {
        return USE_TEST_ADS ? TEST_INTERSTITIAL_AD_UNIT_ID : PROD_INTERSTITIAL_AD_UNIT_ID;
    }
    
    public static String getRewardedAdUnitId() {
        return USE_TEST_ADS ? TEST_REWARDED_AD_UNIT_ID : PROD_REWARDED_AD_UNIT_ID;
    }
    
    public static String getAppOpenAdUnitId() {
        return USE_TEST_ADS ? TEST_APP_OPEN_AD_UNIT_ID : PROD_APP_OPEN_AD_UNIT_ID;
    }
}
