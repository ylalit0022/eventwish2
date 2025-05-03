package com.ds.eventwish.ui.ads;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.ds.eventwish.data.repository.SponsoredAdRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to manage all sponsored ads in the application
 */
public class SponsoredAdManagerFactory {
    private static final String TAG = "SponsoredAdManagerFactory";
    private static SponsoredAdManagerFactory instance;
    
    private final SponsoredAdRepository repository;
    private final Map<String, SponsoredAdView> adViewsMap;
    private Context applicationContext;
    
    private SponsoredAdManagerFactory(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.repository = SponsoredAdRepository.getInstance(context);
        this.adViewsMap = new HashMap<>();
        
        // Preload ads from server
        preloadAds();
    }
    
    /**
     * Initialize the factory with application context
     * @param context Application context
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new SponsoredAdManagerFactory(context);
            Log.d(TAG, "SponsoredAdManagerFactory initialized");
        }
    }
    
    /**
     * Get singleton instance
     * @return SponsoredAdManagerFactory instance
     */
    public static SponsoredAdManagerFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SponsoredAdManagerFactory not initialized. Call init() first");
        }
        return instance;
    }
    
    /**
     * Register a SponsoredAdView for management
     * @param location Location identifier
     * @param adView SponsoredAdView instance
     */
    public void registerAdView(String location, SponsoredAdView adView) {
        adViewsMap.put(location, adView);
        Log.d(TAG, "Registered ad view for location: " + location);
    }
    
    /**
     * Unregister a SponsoredAdView
     * @param location Location identifier
     */
    public void unregisterAdView(String location) {
        adViewsMap.remove(location);
        Log.d(TAG, "Unregistered ad view for location: " + location);
    }
    
    /**
     * Preload ads from server
     */
    private void preloadAds() {
        repository.getSponsoredAds();
        Log.d(TAG, "Preloading sponsored ads from server");
    }
    
    /**
     * Refresh all managed ad views
     */
    public void refreshAllAds() {
        preloadAds();
        for (SponsoredAdView adView : adViewsMap.values()) {
            adView.refreshAds();
        }
        Log.d(TAG, "Refreshing all sponsored ads (" + adViewsMap.size() + " views)");
    }
    
    /**
     * Get the repository instance
     * @return SponsoredAdRepository
     */
    public SponsoredAdRepository getRepository() {
        return repository;
    }
    
    /**
     * Create and register a ViewModel for a specific owner
     * @param viewModelStoreOwner ViewModelStoreOwner (Activity or Fragment)
     * @return SponsoredAdViewModel
     */
    public SponsoredAdViewModel getViewModel(ViewModelStoreOwner viewModelStoreOwner) {
        return new ViewModelProvider(viewModelStoreOwner).get(SponsoredAdViewModel.class);
    }
} 