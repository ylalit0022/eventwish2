package com.ds.eventwish.data.ads;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages ad-related preferences and settings.
 * Uses SharedPreferences to store user preferences related to ads.
 */
public class AdPreferenceManager {
    private static final String PREF_NAME = "ad_preferences";
    private static final String KEY_AD_FREE = "ad_free";
    private static final String KEY_LAST_INTERSTITIAL_SHOWN = "last_interstitial_shown";
    private static final String KEY_INTERSTITIAL_COUNT = "interstitial_count";
    private static final String KEY_REWARDED_AD_EARNED = "rewarded_ad_earned";
    private static final String KEY_REWARDED_AD_EXPIRY = "rewarded_ad_expiry";
    
    private final SharedPreferences preferences;
    
    public AdPreferenceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Checks if the user has an ad-free experience (premium user).
     * @return true if the user has an ad-free experience, false otherwise
     */
    public boolean isAdFree() {
        return preferences.getBoolean(KEY_AD_FREE, false);
    }
    
    /**
     * Sets the ad-free status for the user.
     * @param adFree true if the user should have an ad-free experience, false otherwise
     */
    public void setAdFree(boolean adFree) {
        preferences.edit().putBoolean(KEY_AD_FREE, adFree).apply();
    }
    
    /**
     * Gets the timestamp of when the last interstitial ad was shown.
     * @return the timestamp in milliseconds
     */
    public long getLastInterstitialShownTime() {
        return preferences.getLong(KEY_LAST_INTERSTITIAL_SHOWN, 0);
    }
    
    /**
     * Sets the timestamp of when the last interstitial ad was shown.
     * @param timestamp the timestamp in milliseconds
     */
    public void setLastInterstitialShownTime(long timestamp) {
        preferences.edit().putLong(KEY_LAST_INTERSTITIAL_SHOWN, timestamp).apply();
    }
    
    /**
     * Gets the count of actions since the last interstitial ad was shown.
     * @return the count of actions
     */
    public int getInterstitialCount() {
        return preferences.getInt(KEY_INTERSTITIAL_COUNT, 0);
    }
    
    /**
     * Increments the count of actions since the last interstitial ad was shown.
     * @return the new count
     */
    public int incrementInterstitialCount() {
        int count = getInterstitialCount() + 1;
        preferences.edit().putInt(KEY_INTERSTITIAL_COUNT, count).apply();
        return count;
    }
    
    /**
     * Resets the count of actions since the last interstitial ad was shown.
     */
    public void resetInterstitialCount() {
        preferences.edit().putInt(KEY_INTERSTITIAL_COUNT, 0).apply();
    }
    
    /**
     * Checks if the user has earned a reward from a rewarded ad.
     * @return true if the user has earned a reward, false otherwise
     */
    public boolean hasRewardedAdEarned() {
        return preferences.getBoolean(KEY_REWARDED_AD_EARNED, false) && 
               System.currentTimeMillis() < getRewardedAdExpiry();
    }
    
    /**
     * Sets the rewarded ad earned status for the user.
     * @param earned true if the user has earned a reward, false otherwise
     * @param durationMillis the duration in milliseconds for which the reward is valid
     */
    public void setRewardedAdEarned(boolean earned, long durationMillis) {
        preferences.edit()
                .putBoolean(KEY_REWARDED_AD_EARNED, earned)
                .putLong(KEY_REWARDED_AD_EXPIRY, System.currentTimeMillis() + durationMillis)
                .apply();
    }
    
    /**
     * Gets the expiry time of the rewarded ad benefit.
     * @return the expiry timestamp in milliseconds
     */
    public long getRewardedAdExpiry() {
        return preferences.getLong(KEY_REWARDED_AD_EXPIRY, 0);
    }
    
    /**
     * Increments the count of ad displays.
     * This is used for tracking ad impressions.
     */
    public void incrementAdDisplayCount() {
        // Get the current count
        String KEY_AD_DISPLAY_COUNT = "ad_display_count";
        int count = preferences.getInt(KEY_AD_DISPLAY_COUNT, 0);
        
        // Increment and save
        preferences.edit().putInt(KEY_AD_DISPLAY_COUNT, count + 1).apply();
    }
} 