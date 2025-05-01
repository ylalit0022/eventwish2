package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.repository.SponsoredAdRepository;
import com.ds.eventwish.utils.AnalyticsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel for sponsored ads
 */
public class SponsoredAdViewModel extends ViewModel {
    private static final String TAG = "SponsoredAdViewModel";
    
    private final SponsoredAdRepository repository;
    
    // LiveData for different locations
    private final Map<String, MutableLiveData<SponsoredAd>> adsByLocation = new HashMap<>();
    
    // LiveData for loading state and errors
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>("");
    
    // Cache for ads by location
    private final Map<String, SponsoredAd> adCache = new HashMap<>();
    
    // Cache expiration
    private long lastFetchTime = 0;
    private static final long CACHE_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(30); // 30 minutes
    
    // Track failed attempts to avoid excessive retries
    private int failedAttempts = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private boolean isRetryScheduled = false;
    
    public SponsoredAdViewModel() {
        repository = new SponsoredAdRepository();
        Log.d(TAG, "SponsoredAdViewModel created");
        
        // Initial fetch
        fetchSponsoredAds();
    }
    
    /**
     * Get ad for a specific location
     * @param location The location identifier
     * @return LiveData with SponsoredAd for the location
     */
    public LiveData<SponsoredAd> getAdForLocation(String location) {
        if (!adsByLocation.containsKey(location)) {
            MutableLiveData<SponsoredAd> newLiveData = new MutableLiveData<>();
            adsByLocation.put(location, newLiveData);
            
            // Check cache first
            if (adCache.containsKey(location)) {
                newLiveData.setValue(adCache.get(location));
                Log.d(TAG, "Returned cached ad for location: " + location);
            } else {
                // Try to find best matching ad based on location pattern
                findBestMatchingAd(location);
            }
            
            // If cache is expired, fetch fresh data
            if (System.currentTimeMillis() - lastFetchTime > CACHE_EXPIRATION_MS) {
                fetchSponsoredAds();
            }
        }
        
        return adsByLocation.get(location);
    }
    
    /**
     * Try to find the best matching ad for a location pattern
     * For example, if we have category_birthday but no exact match,
     * we might fall back to category_below or home_ad
     * 
     * @param location The requested location
     */
    private void findBestMatchingAd(String location) {
        // Early return if we don't have any ads cached
        if (adCache.isEmpty()) {
            Log.d(TAG, "No ads in cache to match for location: " + location);
            return;
        }
        
        // First try exact match
        if (adCache.containsKey(location)) {
            adsByLocation.get(location).setValue(adCache.get(location));
            Log.d(TAG, "Found exact match for location: " + location);
            return;
        }
        
        // Extract category parts if this is a category-based location
        String categoryPattern = null;
        if (location.startsWith("category_")) {
            categoryPattern = "category_";
        }
        
        // Look for fallback location matches
        String bestMatch = null;
        
        if (categoryPattern != null) {
            // Try to find any category ad
            for (String cachedLocation : adCache.keySet()) {
                if (cachedLocation.startsWith(categoryPattern)) {
                    bestMatch = cachedLocation;
                    break;
                }
            }
            
            // If no category match, try general locations
            if (bestMatch == null) {
                if (adCache.containsKey("category_below")) {
                    bestMatch = "category_below";
                } else if (adCache.containsKey("home_bottom")) {
                    bestMatch = "home_bottom";
                } else if (!adCache.isEmpty()) {
                    // Just use the first available ad
                    bestMatch = adCache.keySet().iterator().next();
                }
            }
        } else {
            // For non-category locations, try some common fallbacks
            if (adCache.containsKey("home_bottom")) {
                bestMatch = "home_bottom";
            } else if (adCache.containsKey("category_below")) {
                bestMatch = "category_below";
            } else if (!adCache.isEmpty()) {
                // Just use the first available ad
                bestMatch = adCache.keySet().iterator().next();
            }
        }
        
        // If we found a match, use it
        if (bestMatch != null) {
            adsByLocation.get(location).setValue(adCache.get(bestMatch));
            Log.d(TAG, "Using fallback ad from location '" + bestMatch + "' for requested location: " + location);
        } else {
            Log.d(TAG, "No suitable ad found for location: " + location);
        }
    }
    
    /**
     * Get loading state
     * @return LiveData with loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }
    
    /**
     * Get error
     * @return LiveData with error message
     */
    public LiveData<String> getError() {
        return error;
    }
    
    /**
     * Fetch sponsored ads from repository
     */
    public void fetchSponsoredAds() {
        // Skip if already loading or too many failed attempts
        if (loadingState.getValue() != null && loadingState.getValue()) {
            Log.d(TAG, "Already loading sponsored ads, skipping duplicate request");
            return;
        }
        
        if (failedAttempts >= MAX_RETRY_ATTEMPTS && isRetryScheduled) {
            Log.d(TAG, "Too many failed attempts (" + failedAttempts + "), skipping fetch");
            return;
        }
        
        loadingState.setValue(true);
        error.setValue("");
        
        repository.getSponsoredAds(new SponsoredAdRepository.SponsoredAdCallback() {
            @Override
            public void onSuccess(@NonNull List<SponsoredAd> ads) {
                Log.d(TAG, "Successfully fetched " + ads.size() + " sponsored ads");
                loadingState.setValue(false);
                failedAttempts = 0;
                isRetryScheduled = false;
                lastFetchTime = System.currentTimeMillis();
                
                // Clear old cache
                adCache.clear();
                
                // Process and organize ads
                processAds(ads);
            }
            
            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "Error fetching sponsored ads: " + message);
                loadingState.setValue(false);
                error.setValue(message);
                failedAttempts++;
                
                // Retry after delay if not too many attempts
                if (failedAttempts < MAX_RETRY_ATTEMPTS && !isRetryScheduled) {
                    isRetryScheduled = true;
                    // Add exponential backoff
                    long delayMs = (long) Math.pow(2, failedAttempts) * 1000;
                    
                    Log.d(TAG, "Scheduling retry in " + (delayMs / 1000) + " seconds (attempt " + failedAttempts + ")");
                    
                    // Since we can't use Handler directly in ViewModel, we'll use our own mechanism
                    new Thread(() -> {
                        try {
                            Thread.sleep(delayMs);
                            isRetryScheduled = false;
                            fetchSponsoredAds();
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Retry thread interrupted", e);
                        }
                    }).start();
                }
            }
        });
    }
    
    /**
     * Process and organize ads by location
     * @param ads List of sponsored ads from server
     */
    private void processAds(List<SponsoredAd> ads) {
        Map<String, List<SponsoredAd>> adsByLocationTemp = new HashMap<>();
        
        // First pass: organize ads by location
        for (SponsoredAd ad : ads) {
            String location = ad.getLocation();
            if (location == null || location.isEmpty()) {
                // Skip ads without a location
                continue;
            }
            
            // Normalize location
            location = location.toLowerCase().trim();
            
            if (!adsByLocationTemp.containsKey(location)) {
                adsByLocationTemp.put(location, new ArrayList<>());
            }
            adsByLocationTemp.get(location).add(ad);
        }
        
        // Second pass: select the highest priority ad for each location
        for (Map.Entry<String, List<SponsoredAd>> entry : adsByLocationTemp.entrySet()) {
            String location = entry.getKey();
            List<SponsoredAd> locationAds = entry.getValue();
            
            if (locationAds.isEmpty()) {
                continue;
            }
            
            // Sort by priority (higher number = higher priority)
            locationAds.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            
            // Use the highest priority ad
            SponsoredAd selectedAd = locationAds.get(0);
            
            // Update cache
            adCache.put(location, selectedAd);
            
            // Update LiveData if we have observers
            if (adsByLocation.containsKey(location)) {
                adsByLocation.get(location).setValue(selectedAd);
            }
            
            Log.d(TAG, "Selected ad " + selectedAd.getId() + " with priority " + 
                       selectedAd.getPriority() + " for location " + location);
        }
        
        // Third pass: update all LiveData objects that didn't get a direct match
        for (String location : adsByLocation.keySet()) {
            if (!adCache.containsKey(location)) {
                // Try to find a match
                findBestMatchingAd(location);
            }
        }
    }
    
    /**
     * Handle ad click
     * @param ad The ad that was clicked
     * @param context Context to open URL
     */
    public void handleAdClick(SponsoredAd ad, Context context) {
        if (ad == null || context == null) {
            Log.e(TAG, "Cannot handle click - ad or context is null");
            return;
        }
        
        // Record click in factory for local stats
        SponsoredAdManagerFactory.getInstance().recordClick(ad.getLocation());
        
        // Send click to server
        repository.recordAdClick(ad.getId(), new SponsoredAdRepository.SponsoredAdCallback() {
            @Override
            public void onSuccess(@NonNull List<SponsoredAd> ads) {
                Log.d(TAG, "Successfully recorded click for ad: " + ad.getId());
            }
            
            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "Error recording click for ad " + ad.getId() + ": " + message);
            }
        });
        
        // Open URL
        String redirectUrl = ad.getRedirectUrl();
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Opened URL for ad: " + redirectUrl);
            } catch (Exception e) {
                Log.e(TAG, "Error opening URL for ad: " + e.getMessage());
            }
        }
    }
    
    /**
     * Track impression for an ad
     * @param ad The ad that was viewed
     */
    public void trackImpression(SponsoredAd ad) {
        if (ad == null) {
            Log.e(TAG, "Cannot track impression - ad is null");
            return;
        }
        
        // Record impression in factory for local stats
        SponsoredAdManagerFactory.getInstance().recordImpression(ad.getLocation());
        
        // Send impression to server
        repository.recordAdImpression(ad.getId(), new SponsoredAdRepository.SponsoredAdCallback() {
            @Override
            public void onSuccess(@NonNull List<SponsoredAd> ads) {
                Log.d(TAG, "Successfully recorded impression for ad: " + ad.getId());
            }
            
            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "Error recording impression for ad " + ad.getId() + ": " + message);
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        Log.d(TAG, "SponsoredAdViewModel cleared");
    }
} 