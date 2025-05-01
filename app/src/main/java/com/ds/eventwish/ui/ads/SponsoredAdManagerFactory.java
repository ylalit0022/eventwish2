package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for managing sponsored ad components
 */
public class SponsoredAdManagerFactory {
    private static final String TAG = "SponsoredAdFactory";
    
    private static SponsoredAdManagerFactory instance;
    private SponsoredAdViewModel viewModel;
    private static Context applicationContext;
    
    // Use ConcurrentHashMap for thread safety
    private final ConcurrentHashMap<String, WeakReference<SponsoredAdView>> adViews = new ConcurrentHashMap<>();
    
    // Track locations for analytics and debugging
    private final Map<String, Integer> locationImpressionCounts = new HashMap<>();
    private final Map<String, Integer> locationClickCounts = new HashMap<>();
    
    // Maximum number of locations to track to prevent memory leaks
    private static final int MAX_LOCATIONS = 50;
    
    private SponsoredAdManagerFactory() {
        Log.d(TAG, "Initializing SponsoredAdManagerFactory");
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
     * @param adView The ad view instance
     */
    public void registerAdView(String location, SponsoredAdView adView) {
        // Check for too many locations being tracked to prevent memory issues
        if (adViews.size() >= MAX_LOCATIONS) {
            // Remove least recently used locations (but keep statistics)
            pruneAdViewMap();
        }
        
        // Clean up any expired entries
        cleanupExpiredReferences();
        
        // Register the new ad view
        adViews.put(location, new WeakReference<>(adView));
        Log.d(TAG, "Registered AdView for location: " + location + ", active locations: " + adViews.size());
        
        // Initialize impression counter for new locations
        if (!locationImpressionCounts.containsKey(location)) {
            locationImpressionCounts.put(location, 0);
            locationClickCounts.put(location, 0);
        }
    }
    
    /**
     * Unregister an ad view for a specific location
     * @param location The location identifier
     */
    public void unregisterAdView(String location) {
        if (adViews.containsKey(location)) {
            adViews.remove(location);
            Log.d(TAG, "Unregistered AdView for location: " + location + ", remaining locations: " + adViews.size());
        }
    }
    
    /**
     * Record an impression for tracking purposes
     * @param location The location where the impression occurred
     */
    public void recordImpression(String location) {
        if (location != null) {
            int count = locationImpressionCounts.getOrDefault(location, 0);
            locationImpressionCounts.put(location, count + 1);
            Log.d(TAG, "Recorded impression for location: " + location + 
                 ", total impressions for this location: " + (count + 1));
        }
    }
    
    /**
     * Record a click for tracking purposes
     * @param location The location where the click occurred
     */
    public void recordClick(String location) {
        if (location != null) {
            int count = locationClickCounts.getOrDefault(location, 0);
            locationClickCounts.put(location, count + 1);
            Log.d(TAG, "Recorded click for location: " + location + 
                 ", total clicks for this location: " + (count + 1));
        }
    }
    
    /**
     * Get all active ad views
     * @return Map of locations to ad views
     */
    @NonNull
    public Map<String, SponsoredAdView> getActiveAdViews() {
        Map<String, SponsoredAdView> activeViews = new HashMap<>();
        
        for (Map.Entry<String, WeakReference<SponsoredAdView>> entry : adViews.entrySet()) {
            SponsoredAdView adView = entry.getValue().get();
            if (adView != null) {
                activeViews.put(entry.getKey(), adView);
            }
        }
        
        return activeViews;
    }
    
    /**
     * Get performance metrics for all ad locations
     * @return Map of locations to impression/click metrics
     */
    @NonNull
    public Map<String, Map<String, Integer>> getPerformanceMetrics() {
        Map<String, Map<String, Integer>> metrics = new HashMap<>();
        
        for (String location : locationImpressionCounts.keySet()) {
            Map<String, Integer> locationMetrics = new HashMap<>();
            locationMetrics.put("impressions", locationImpressionCounts.getOrDefault(location, 0));
            locationMetrics.put("clicks", locationClickCounts.getOrDefault(location, 0));
            metrics.put(location, locationMetrics);
        }
        
        return metrics;
    }
    
    /**
     * Refresh ads for all registered ad views
     */
    public void refreshAllAds() {
        // Clean up expired references first
        cleanupExpiredReferences();
        
        Log.d(TAG, "Refreshing ads for " + adViews.size() + " registered locations");
        
        // Request new ads from the server
        if (viewModel != null) {
            viewModel.fetchSponsoredAds();
        }
    }
    
    /**
     * Clean up any expired ad view references
     */
    private void cleanupExpiredReferences() {
        adViews.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
    
    /**
     * Prune the ad view map to prevent memory leaks
     * Removes oldest entries while keeping the most recent ones
     */
    private void pruneAdViewMap() {
        // If we have too many locations, remove 20% of them (least used)
        int toRemove = Math.max(1, adViews.size() / 5);
        Log.d(TAG, "Pruning " + toRemove + " ad view references to prevent memory issues");
        
        // Find the locations with fewest impressions to remove
        locationImpressionCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(toRemove)
            .forEach(entry -> adViews.remove(entry.getKey()));
    }
    
    /**
     * Reset all tracking data and clear view references
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