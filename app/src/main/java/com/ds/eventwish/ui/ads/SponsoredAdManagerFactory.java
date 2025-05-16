package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

/**
 * Factory for managing sponsored ad components
 */
public class SponsoredAdManagerFactory {
    private static final String TAG = "SponsoredAdFactory";
    
    private static SponsoredAdManagerFactory instance;
    private SponsoredAdViewModel viewModel;
    private static Context applicationContext;
    
    // Use ConcurrentHashMap for thread safety
    private final ConcurrentHashMap<String, WeakReference<View>> adViews = new ConcurrentHashMap<>();
    
<<<<<<< HEAD
    private SponsoredAdManagerFactory(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.repository = SponsoredAdRepository.getInstance(context);
        this.adViewsMap = new HashMap<>();
        
        // Preload ads from server
        preloadAds();
=======
    // Track locations for analytics and debugging
    private final Map<String, Integer> locationImpressionCounts = new HashMap<>();
    private final Map<String, Integer> locationClickCounts = new HashMap<>();
    
    // Maximum number of locations to track to prevent memory leaks
    private static final int MAX_LOCATIONS = 50;
    
    private SponsoredAdManagerFactory() {
        Log.d(TAG, "Initializing SponsoredAdManagerFactory");
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
    }
    
    /**
     * Initialize the SponsoredAdManagerFactory
     * @param context The application context
     */
    public static void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize SponsoredAdManagerFactory with null context");
            return;
        }
        
        applicationContext = context.getApplicationContext();
        Log.d(TAG, "SponsoredAdManagerFactory initialized with application context");
    }
    
    /**
     * Get singleton instance of the factory
     * @return SponsoredAdManagerFactory instance
     */
    public static synchronized SponsoredAdManagerFactory getInstance() {
        if (instance == null) {
            if (applicationContext == null) {
                Log.e(TAG, "SponsoredAdManagerFactory not initialized. Call init() first.");
                throw new IllegalStateException("SponsoredAdManagerFactory not initialized. Call init() first.");
            }
            instance = new SponsoredAdManagerFactory();
        }
        return instance;
    }
    
    /**
     * Get the ViewModel for sponsored ads
     * @param owner ViewModelStoreOwner to get ViewModel
     * @return SponsoredAdViewModel
     */
    public SponsoredAdViewModel getViewModel(ViewModelStoreOwner owner) {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(owner).get(SponsoredAdViewModel.class);
            Log.d(TAG, "Created new SponsoredAdViewModel instance");
        }
        return viewModel;
    }
    
    /**
     * Register an ad view for a specific location
     * @param location The location identifier
     * @param adView The view displaying the ad
     */
    public void registerAdView(String location, View adView) {
        if (location == null || location.isEmpty() || adView == null) {
            Log.w(TAG, "Cannot register null or empty location/view");
            return;
        }
        
        // Store weak reference to avoid memory leaks
        adViews.put(location, new WeakReference<>(adView));
        Log.d(TAG, "Registered ad view for location: " + location);
    }
    
    /**
     * Unregister an ad view for a specific location
     * @param location The location identifier
     */
    public void unregisterAdView(String location) {
        if (location == null || location.isEmpty()) {
            Log.w(TAG, "Cannot unregister ad view with null or empty location");
            return;
        }
        
        adViews.remove(location);
        Log.d(TAG, "Unregistered ad view for location: " + location);
    }
    
    /**
     * Record an impression for a specific location
     * @param location The location identifier
     */
    public void recordImpression(String location) {
        if (location != null) {
            int count = locationImpressionCounts.getOrDefault(location, 0) + 1;
            locationImpressionCounts.put(location, count);
            Log.d(TAG, "Recorded impression for location: " + location + ", total: " + count);
        }
    }
    
    /**
     * Record a click for a specific location
     * @param location The location identifier
     */
    public void recordClick(String location) {
        if (location != null) {
            int count = locationClickCounts.getOrDefault(location, 0) + 1;
            locationClickCounts.put(location, count);
            Log.d(TAG, "Recorded click for location: " + location + ", total: " + count);
        }
    }
    
    /**
     * Get impression count for a specific location
     * @param location The location identifier
     * @return Number of impressions
     */
    public int getImpressionCount(String location) {
        return locationImpressionCounts.getOrDefault(location, 0);
    }
    
    /**
     * Get click count for a specific location
     * @param location The location identifier
     * @return Number of clicks
     */
    public int getClickCount(String location) {
        return locationClickCounts.getOrDefault(location, 0);
    }
    
    /**
     * Get CTR (Click-Through Rate) for a specific location
     * @param location The location identifier
     * @return CTR as a percentage
     */
    public float getCTR(String location) {
        int impressions = getImpressionCount(location);
        int clicks = getClickCount(location);
        
        if (impressions > 0) {
            return (float) clicks / impressions * 100;
        }
        return 0;
    }
    
    /**
     * Get all locations with statistics
     * @return Map of location stats
     */
    public Map<String, Map<String, Object>> getLocationStats() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        for (String location : locationImpressionCounts.keySet()) {
            Map<String, Object> locationStats = new HashMap<>();
            locationStats.put("impressions", getImpressionCount(location));
            locationStats.put("clicks", getClickCount(location));
            locationStats.put("ctr", getCTR(location));
            
            stats.put(location, locationStats);
        }
        
        return stats;
    }
    
    /**
     * Clean up expired references to AdViews
     */
    private void cleanupExpiredReferences() {
        for (Map.Entry<String, WeakReference<View>> entry : adViews.entrySet()) {
            if (entry.getValue().get() == null) {
                adViews.remove(entry.getKey());
                Log.d(TAG, "Removed expired AdView reference for location: " + entry.getKey());
            }
        }
    }
    
    /**
     * Prune the AdView map to prevent memory issues
     */
    private void pruneAdViewMap() {
        if (adViews.size() <= MAX_LOCATIONS / 2) {
            return; // No need to prune
        }
        
        // Remove half of the entries
        int toRemove = adViews.size() - MAX_LOCATIONS / 2;
        int removed = 0;
        
        for (String location : adViews.keySet()) {
            adViews.remove(location);
            removed++;
            Log.d(TAG, "Pruned AdView for location: " + location + " to prevent memory issues");
            
            if (removed >= toRemove) {
                break;
            }
        }
    }
    
    /**
     * Reset all data (for testing)
     */
    public void reset() {
        adViews.clear();
        locationImpressionCounts.clear();
        locationClickCounts.clear();
        viewModel = null;
        Log.d(TAG, "SponsoredAdManagerFactory reset");
    }
    
    /**
     * Invalidate the factory instance (for testing or memory cleanup)
     */
    public static void invalidateInstance() {
        if (instance != null) {
            instance.reset();
            instance = null;
            Log.d(TAG, "SponsoredAdManagerFactory instance invalidated");
        }
    }
} 