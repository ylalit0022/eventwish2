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
<<<<<<< HEAD
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
=======
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel for sponsored ads
 */
public class SponsoredAdViewModel extends ViewModel {
    private static final String TAG = "SponsoredAdViewModel";
    
    private final SponsoredAdRepository repository;
    
<<<<<<< HEAD
    // Rotation support
    private final LocalRotationManager rotationManager;
    private final Map<String, MutableLiveData<SponsoredAd>> rotatingAdsMap = new HashMap<>();
    private boolean isRotationActive = false;
    
    // Track when last refreshed
    private long lastAdRefreshTime = 0;
    private static final long MAX_CACHE_LIFETIME_MS = TimeUnit.MINUTES.toMillis(5); // Refresh every 5 minutes at most
    
    public SponsoredAdViewModel(@NonNull Application application) {
        super(application);
        repository = SponsoredAdRepository.getInstance(application);
        rotationManager = new LocalRotationManager(application, repository);
        
        // Initialize ads when ViewModel is created
        forceRefreshAds();
    }
    
    /**
     * Fetch sponsored ads from the repository
     */
    public void fetchSponsoredAds() {
        Log.d(TAG, "Fetching sponsored ads from repository");
        repository.getSponsoredAds();
    }
    
    /**
     * Force refresh ads from network
     */
    public void forceRefreshAds() {
        Log.d(TAG, "Forcing refresh of sponsored ads from network");
        repository.forceRefreshNow();
        lastAdRefreshTime = System.currentTimeMillis();
    }
    
    /**
     * Get sponsored ads for a specific location and select the best one to display
     * @param location The location to get ads for (e.g., "home_bottom")
     * @return LiveData containing the selected ad
     */
    public LiveData<SponsoredAd> getAdForLocation(String location) {
        Log.d(TAG, "Getting sponsored ad for location: " + location);
        
        // Always force refresh if it's been more than MAX_CACHE_LIFETIME_MS
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdRefreshTime > MAX_CACHE_LIFETIME_MS) {
            Log.d(TAG, "Cache lifetime exceeded, forcing refresh");
            forceRefreshAds();
        }
        
        LiveData<List<SponsoredAd>> adsForLocation = repository.getAdsByLocation(location);
        
        // Add source to mediator LiveData
        selectedAdLiveData.addSource(adsForLocation, ads -> {
            if (ads != null && !ads.isEmpty()) {
                Log.d(TAG, "Found " + ads.size() + " ads for location: " + location);
                
                // Filter only active ads first
                List<SponsoredAd> activeAds = new ArrayList<>();
                for (SponsoredAd ad : ads) {
                    if (ad.isStatus()) {
                        activeAds.add(ad);
                    } else {
                        Log.d(TAG, "Filtering out inactive ad: " + ad.getId());
                    }
                }
                
                if (activeAds.isEmpty()) {
                    Log.d(TAG, "No active ads found for location: " + location + ", forcing refresh");
                    selectedAdLiveData.setValue(null);
                    adLoadedLiveData.setValue(false);
                    // Force refresh to get latest status
                    forceRefreshAds();
                    return;
                }
                
                // Select best ad based on priority or random if multiple with same priority
                SponsoredAd selectedAd = selectBestAd(activeAds);
                
                // Double-check selected ad status
                if (selectedAd != null && selectedAd.isStatus()) {
                    selectedAdLiveData.setValue(selectedAd);
                    adLoadedLiveData.setValue(true);
                    
                    Log.d(TAG, "Selected ad for display: " + selectedAd.getId() + 
                          ", title: " + selectedAd.getTitle() + 
                          ", priority: " + selectedAd.getPriority() +
                          ", status: " + selectedAd.isStatus() +
                          ", location: " + location);
                } else {
                    Log.w(TAG, "Selected ad is null or inactive, forcing refresh");
                    selectedAdLiveData.setValue(null);
                    adLoadedLiveData.setValue(false);
                    forceRefreshAds();
                }
            } else {
                selectedAdLiveData.setValue(null);
                adLoadedLiveData.setValue(false);
                Log.d(TAG, "No ads available for location: " + location);
            }
        });
        
        return selectedAdLiveData;
    }
    
    /**
     * Get LiveData for rotating ads at a specific location
     * @param location The location to get rotating ads for
     * @return LiveData containing the rotating ad
     */
    public LiveData<SponsoredAd> getRotatingAd(String location) {
        if (!rotatingAdsMap.containsKey(location)) {
            rotatingAdsMap.put(location, new MutableLiveData<>());
        }
        
        return rotatingAdsMap.get(location);
    }
    
    /**
     * Start ad rotation for a location
     * @param location The location to rotate ads for
     */
    public void startRotation(String location) {
        if (!rotatingAdsMap.containsKey(location)) {
            rotatingAdsMap.put(location, new MutableLiveData<>());
        }
        
        Log.d(TAG, "Starting ad rotation for location: " + location);
        isRotationActive = true;
        
        rotationManager.startRotation(location, new LocalRotationManager.RotationCallback() {
            @Override
            public void onAdRotated(SponsoredAd ad) {
                Log.d(TAG, "Ad rotated: " + ad.getId() + " for location: " + location);
                MutableLiveData<SponsoredAd> liveData = rotatingAdsMap.get(location);
                if (liveData != null) {
                    liveData.postValue(ad);
                }
            }
        });
    }
    
    /**
     * Stop ad rotation
     */
    public void stopRotation() {
        if (isRotationActive) {
            Log.d(TAG, "Stopping ad rotation");
            rotationManager.stopRotation();
            isRotationActive = false;
        }
    }
    
    /**
     * Set custom rotation interval (default is 20 minutes)
     * @param intervalMs Interval in milliseconds
     */
    public void setRotationInterval(long intervalMs) {
        Log.d(TAG, "Setting rotation interval to " + intervalMs + " ms");
        rotationManager.setRotationInterval(intervalMs);
    }
    
    /**
     * Set rotation interval in minutes for ease of use
     * @param minutes Interval in minutes
     */
    public void setRotationIntervalMinutes(int minutes) {
        setRotationInterval(TimeUnit.MINUTES.toMillis(minutes));
    }
    
    /**
     * Check if rotation is currently active
     * @return True if rotation is active
     */
    public boolean isRotationActive() {
        return isRotationActive;
    }
    
    /**
     * Select the best ad from a list based on priority and validity
     * @param ads List of ads to select from
     * @return The selected ad or null if none valid
     */
    private SponsoredAd selectBestAd(List<SponsoredAd> ads) {
        if (ads == null || ads.isEmpty()) {
            Log.d(TAG, "No ads provided for selection");
            return null;
        }
        
        SponsoredAd bestAd = null;
        int highestPriority = -1;
        Date now = new Date();
        
        Log.d(TAG, "Selecting best ad from " + ads.size() + " candidates");
        
        // Find ads with highest priority that are currently active
        for (SponsoredAd ad : ads) {
            if (!ad.isStatus()) {
                Log.d(TAG, "Skipping inactive ad: " + ad.getId());
                continue; // Skip inactive ads
            }
            
            // Check if ad is within its date range
            if (ad.getStartDate() != null && ad.getStartDate().after(now)) {
                Log.d(TAG, "Skipping ad not started yet: " + ad.getId() + ", starts: " + ad.getStartDate());
                continue; // Ad not started yet
            }
            
            if (ad.getEndDate() != null && ad.getEndDate().before(now)) {
                Log.d(TAG, "Skipping expired ad: " + ad.getId() + ", ended: " + ad.getEndDate());
                continue; // Ad expired
            }
            
            Log.d(TAG, "Considering ad: " + ad.getId() + ", priority: " + ad.getPriority() + 
                   ", current highest: " + highestPriority);
                   
            if (ad.getPriority() > highestPriority) {
                highestPriority = ad.getPriority();
                bestAd = ad;
                Log.d(TAG, "New highest priority ad: " + ad.getId() + ", priority: " + ad.getPriority());
=======
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
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
            }
        }
        
        // If we found a match, use it
        if (bestMatch != null) {
            adsByLocation.get(location).setValue(adCache.get(bestMatch));
            Log.d(TAG, "Using fallback ad from location '" + bestMatch + "' for requested location: " + location);
        } else {
<<<<<<< HEAD
            Log.w(TAG, "No valid ads found among " + ads.size() + " candidates");
        }
        
        return bestAd;
    }
    
    /**
     * Handle impression for a sponsored ad
     * @param ad The ad that was viewed
     */
    public void handleAdImpression(SponsoredAd ad) {
        if (ad == null || ad.getId() == null) {
            Log.e(TAG, "Cannot record impression for null ad or ad without ID");
            return;
        }
        
        try {
            Log.d(TAG, "Recording impression for ad: " + ad.getId() + 
                      ", title: " + ad.getTitle() + 
                      ", location: " + ad.getLocation());
                      
            // Record the impression (this will send immediately to server when network is available)
            repository.recordImpression(ad.getId());
            
            // Track via analytics
            AnalyticsUtils.getInstance().trackAdImpression(
                ad.getId(),
                ad.getTitle(),
                ad.getLocation()
            );
            
            Log.d(TAG, "Impression recorded successfully for ad: " + ad.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error handling ad impression", e);
        }
    }
    
    /**
     * Handle click on a sponsored ad
     * @param ad The ad that was clicked
     * @param context Context for launching intent
     * @return true if the click was handled successfully
     */
    public boolean handleAdClick(SponsoredAd ad, Context context) {
        if (ad == null || context == null) {
            return false;
        }
        
        try {
            // Record the click
            repository.recordClick(ad.getId());
            
            // Track via analytics
            AnalyticsUtils.getInstance().trackAdClick(ad.getId(), ad.getTitle(), ad.getLocation());
            
            // Open the redirect URL
            String url = ad.getRedirectUrl();
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error handling ad click", e);
            return false;
=======
            Log.d(TAG, "No suitable ad found for location: " + location);
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
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
        
        repository.getSponsoredAdsWithCallback(new SponsoredAdRepository.SponsoredAdCallback() {
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
            
            // Skip frequency capped ads
            if (ad.isFrequencyCapped()) {
                Log.d(TAG, "Skipping frequency capped ad: " + ad.getId());
                continue;
            }
            
            // Normalize location
            location = location.toLowerCase().trim();
            
            if (!adsByLocationTemp.containsKey(location)) {
                adsByLocationTemp.put(location, new ArrayList<>());
            }
            adsByLocationTemp.get(location).add(ad);
        }
        
        // Second pass: apply weighted distribution for each location
        for (Map.Entry<String, List<SponsoredAd>> entry : adsByLocationTemp.entrySet()) {
            String location = entry.getKey();
            List<SponsoredAd> locationAds = entry.getValue();
            
            if (locationAds.isEmpty()) {
                continue;
            }
            
            // Use weighted algorithm similar to server implementation
            if (locationAds.size() > 1) {
                // Calculate total priority
                int totalPriority = 0;
                for (SponsoredAd ad : locationAds) {
                    totalPriority += ad.getPriority();
                }
                
                // Apply weighted score with randomization factor
                Random random = new Random();
                for (SponsoredAd ad : locationAds) {
                    // Calculate weighted score: 
                    // - Base priority percentage + randomness factor scaled by priority
                    float priorityWeight = totalPriority > 0 ? 
                        (float) ad.getPriority() / totalPriority : 1f;
                    float randomFactor = random.nextFloat() * 0.4f; // Random factor between 0-0.4
                    float weightedScore = priorityWeight + 
                        (randomFactor * (ad.getPriority() / 10f)); // Scale randomness by priority
                    
                    ad.setWeightedScore(weightedScore);
                    
                    Log.d(TAG, "Ad " + ad.getId() + 
                          " priority: " + ad.getPriority() + 
                          " weight: " + priorityWeight + 
                          " random: " + randomFactor + 
                          " score: " + weightedScore);
                }
                
                // Sort by weighted score, higher scores first
                locationAds.sort((a, b) -> Float.compare(b.getWeightedScore(), a.getWeightedScore()));
                
                Log.d(TAG, "Sorted " + locationAds.size() + " ads by weighted score for location " + location);
            } else {
                // Only one ad available, no need for weighted algorithm
                locationAds.get(0).setWeightedScore(1.0f);
            }
            
            // Use the top-ranked ad after weighted sort
            SponsoredAd selectedAd = locationAds.get(0);
            
            // Update cache
            adCache.put(location, selectedAd);
            
            // Update LiveData if we have observers
            if (adsByLocation.containsKey(location)) {
                adsByLocation.get(location).setValue(selectedAd);
            }
            
            Log.d(TAG, "Selected ad " + 
                (selectedAd.getId() != null ? selectedAd.getId() : "null") + 
                " with priority " + selectedAd.getPriority() + 
                " and weighted score " + selectedAd.getWeightedScore() +
                " for location " + location);
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
        
        // Get ad location with null check
        String location = ad.getLocation() != null ? ad.getLocation() : "unknown";
        
        // Record click in factory for local stats
        SponsoredAdManagerFactory.getInstance().recordClick(location);
        
        // Get ad ID with null check
        String adId = ad.getId() != null ? ad.getId() : "unknown";
        
        // Send click to server (only if ID is not "unknown")
        if (!"unknown".equals(adId)) {
            repository.recordAdClick(adId, new SponsoredAdRepository.SponsoredAdCallback() {
                @Override
                public void onSuccess(@NonNull List<SponsoredAd> ads) {
                    Log.d(TAG, "Successfully recorded click for ad: " + adId);
                }
                
                @Override
                public void onError(@NonNull String message) {
                    Log.e(TAG, "Error recording click for ad " + adId + ": " + message);
                }
            });
        } else {
            Log.w(TAG, "Skipping server click recording for ad with null ID");
        }
        
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
        
        // Get ad location with null check
        String location = ad.getLocation() != null ? ad.getLocation() : "unknown";
        
        // Record impression in factory for local stats
        SponsoredAdManagerFactory.getInstance().recordImpression(location);
        
        // Get ad ID with null check
        String adId = ad.getId() != null ? ad.getId() : "unknown";
        
        // Send impression to server (only if ID is not "unknown")
        if (!"unknown".equals(adId)) {
            repository.recordAdImpression(adId, new SponsoredAdRepository.SponsoredAdCallback() {
                @Override
                public void onSuccess(@NonNull List<SponsoredAd> ads) {
                    Log.d(TAG, "Successfully recorded impression for ad: " + adId);
                }
                
                @Override
                public void onError(@NonNull String message) {
                    Log.e(TAG, "Error recording impression for ad " + adId + ": " + message);
                }
            });
        } else {
            Log.w(TAG, "Skipping server impression recording for ad with null ID");
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        Log.d(TAG, "SponsoredAdViewModel cleared");
    }
    
    /**
     * Get the next ad for rotation (for preloading)
     * @param location The location for which to get the next ad
     * @param excludeIds Set of ad IDs to exclude (e.g., currently displayed ad)
     * @return The next ad or null if none available
     */
    public SponsoredAd getNextAdForRotation(String location, Set<String> excludeIds) {
        if (repository == null) return null;
        
        try {
            // Get the next rotation ad synchronously
            return repository.getNextRotationAdSync(location, excludeIds);
        } catch (Exception e) {
            Log.e(TAG, "Error getting next ad for rotation: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopRotation();
    }
} 