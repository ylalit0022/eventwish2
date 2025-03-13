package com.ds.eventwish.data.repository;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.lifecycle.LiveData;

import com.ds.eventwish.data.ads.AdManager;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

/**
 * Repository for ad-related operations.
 * Follows the repository pattern used throughout the app.
 */
public class AdRepository {
    private final AdManager adManager;
    
    /**
     * Constructor for AdRepository.
     * @param context the application context
     */
    public AdRepository(Context context) {
        adManager = AdManager.getInstance(context);
    }
    
    /**
     * Creates and loads a banner ad.
     * @param adSize the size of the banner ad
     * @return the AdView
     */
    public AdView createBannerAd(AdSize adSize) {
        return adManager.createBannerAd(adSize);
    }
    
    /**
     * Adds a banner ad to the specified container.
     * @param container the container to add the banner ad to
     * @param adSize the size of the banner ad
     * @return the AdView that was added, or null if the user has an ad-free experience
     */
    public AdView addBannerToContainer(ViewGroup container, AdSize adSize) {
        return adManager.addBannerToContainer(container, adSize);
    }
    
    /**
     * Preloads an interstitial ad and adds it to the cache.
     */
    public void preloadInterstitialAd() {
        adManager.preloadInterstitialAd();
    }
    
    /**
     * Shows an interstitial ad if one is available and conditions are met.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showInterstitialAd(Activity activity, AdManager.InterstitialAdCallback callback) {
        return adManager.showInterstitialAd(activity, callback);
    }
    
    /**
     * Preloads a rewarded ad and adds it to the cache.
     */
    public void preloadRewardedAd() {
        adManager.preloadRewardedAd();
    }
    
    /**
     * Shows a rewarded ad if one is available.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showRewardedAd(Activity activity, AdManager.RewardedAdCallback callback) {
        return adManager.showRewardedAd(activity, callback);
    }
    
    /**
     * Gets the LiveData for interstitial ad loading status.
     * @return the LiveData
     */
    public LiveData<Boolean> getInterstitialAdLoading() {
        return adManager.getInterstitialAdLoading();
    }
    
    /**
     * Gets the LiveData for rewarded ad loading status.
     * @return the LiveData
     */
    public LiveData<Boolean> getRewardedAdLoading() {
        return adManager.getRewardedAdLoading();
    }
    
    /**
     * Checks if the user has an ad-free experience.
     * @return true if the user has an ad-free experience, false otherwise
     */
    public boolean isAdFree() {
        return adManager.isAdFree();
    }
    
    /**
     * Sets the ad-free status for the user.
     * @param adFree true if the user should have an ad-free experience, false otherwise
     */
    public void setAdFree(boolean adFree) {
        adManager.setAdFree(adFree);
    }
    
    /**
     * Checks if the user has earned a reward from a rewarded ad.
     * @return true if the user has earned a reward, false otherwise
     */
    public boolean hasRewardedAdEarned() {
        return adManager.hasRewardedAdEarned();
    }
    
    /**
     * Gets the expiry time of the rewarded ad benefit.
     * @return the expiry timestamp in milliseconds
     */
    public long getRewardedAdExpiry() {
        return adManager.getRewardedAdExpiry();
    }
    
    /**
     * Cleans up resources when the app is being destroyed.
     */
    public void cleanup() {
        adManager.cleanup();
    }
} 