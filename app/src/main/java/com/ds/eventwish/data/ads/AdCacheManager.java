package com.ds.eventwish.data.ads;

import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages caching of ads to improve loading performance.
 * Maintains queues of preloaded interstitial and rewarded ads.
 */
public class AdCacheManager {
    private static final String TAG = "AdCacheManager";
    
    // Queues for caching ads
    private final Queue<InterstitialAd> interstitialAdCache;
    private final Queue<RewardedAd> rewardedAdCache;
    
    // Singleton instance
    private static AdCacheManager instance;
    
    // Private constructor for singleton pattern
    private AdCacheManager() {
        interstitialAdCache = new LinkedList<>();
        rewardedAdCache = new LinkedList<>();
    }
    
    /**
     * Gets the singleton instance of AdCacheManager.
     * @return the singleton instance
     */
    public static synchronized AdCacheManager getInstance() {
        if (instance == null) {
            instance = new AdCacheManager();
        }
        return instance;
    }
    
    /**
     * Adds an interstitial ad to the cache.
     * @param interstitialAd the interstitial ad to cache
     */
    public synchronized void cacheInterstitialAd(InterstitialAd interstitialAd) {
        if (interstitialAd == null) {
            Log.w(TAG, "Attempted to cache null interstitial ad");
            return;
        }
        
        // Ensure we don't exceed the maximum cache size
        if (interstitialAdCache.size() >= AdConstants.MAX_INTERSTITIAL_CACHE_SIZE) {
            Log.d(TAG, "Interstitial ad cache full, removing oldest ad");
            interstitialAdCache.poll(); // Remove the oldest ad
        }
        
        interstitialAdCache.offer(interstitialAd);
        Log.d(TAG, "Interstitial ad cached. Cache size: " + interstitialAdCache.size());
    }
    
    /**
     * Gets an interstitial ad from the cache.
     * @return the next available interstitial ad, or null if the cache is empty
     */
    public synchronized InterstitialAd getInterstitialAd() {
        InterstitialAd ad = interstitialAdCache.poll();
        Log.d(TAG, "Interstitial ad retrieved from cache. Remaining cache size: " + interstitialAdCache.size());
        return ad;
    }
    
    /**
     * Adds a rewarded ad to the cache.
     * @param rewardedAd the rewarded ad to cache
     */
    public synchronized void cacheRewardedAd(RewardedAd rewardedAd) {
        if (rewardedAd == null) {
            Log.w(TAG, "Attempted to cache null rewarded ad");
            return;
        }
        
        // Ensure we don't exceed the maximum cache size
        if (rewardedAdCache.size() >= AdConstants.MAX_REWARDED_CACHE_SIZE) {
            Log.d(TAG, "Rewarded ad cache full, removing oldest ad");
            rewardedAdCache.poll(); // Remove the oldest ad
        }
        
        rewardedAdCache.offer(rewardedAd);
        Log.d(TAG, "Rewarded ad cached. Cache size: " + rewardedAdCache.size());
    }
    
    /**
     * Gets a rewarded ad from the cache.
     * @return the next available rewarded ad, or null if the cache is empty
     */
    public synchronized RewardedAd getRewardedAd() {
        RewardedAd ad = rewardedAdCache.poll();
        Log.d(TAG, "Rewarded ad retrieved from cache. Remaining cache size: " + rewardedAdCache.size());
        return ad;
    }
    
    /**
     * Checks if there is an interstitial ad available in the cache.
     * @return true if there is at least one interstitial ad in the cache, false otherwise
     */
    public synchronized boolean hasInterstitialAd() {
        return !interstitialAdCache.isEmpty();
    }
    
    /**
     * Checks if there is a rewarded ad available in the cache.
     * @return true if there is at least one rewarded ad in the cache, false otherwise
     */
    public synchronized boolean hasRewardedAd() {
        return !rewardedAdCache.isEmpty();
    }
    
    /**
     * Gets the current size of the interstitial ad cache.
     * @return the number of interstitial ads in the cache
     */
    public synchronized int getInterstitialCacheSize() {
        return interstitialAdCache.size();
    }
    
    /**
     * Gets the current size of the rewarded ad cache.
     * @return the number of rewarded ads in the cache
     */
    public synchronized int getRewardedCacheSize() {
        return rewardedAdCache.size();
    }
    
    /**
     * Clears all cached ads.
     * Should be called when the app is being destroyed or when ads need to be refreshed.
     */
    public synchronized void clearCache() {
        interstitialAdCache.clear();
        rewardedAdCache.clear();
        Log.d(TAG, "Ad cache cleared");
    }
} 