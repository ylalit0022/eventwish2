package com.ds.eventwish.ui.ads;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.repository.SponsoredAdRepository;
import com.ds.eventwish.utils.AnalyticsUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel to handle sponsored ads in UI components
 */
public class SponsoredAdViewModel extends AndroidViewModel {
    private static final String TAG = "SponsoredAdViewModel";
    
    private final SponsoredAdRepository repository;
    private final MediatorLiveData<SponsoredAd> selectedAdLiveData = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> adLoadedLiveData = new MutableLiveData<>(false);
    
    // Rotation support
    private final LocalRotationManager rotationManager;
    private final Map<String, MutableLiveData<SponsoredAd>> rotatingAdsMap = new HashMap<>();
    private boolean isRotationActive = false;
    
    public SponsoredAdViewModel(@NonNull Application application) {
        super(application);
        repository = SponsoredAdRepository.getInstance(application);
        rotationManager = new LocalRotationManager(application, repository);
        
        // Initialize ads when ViewModel is created
        fetchSponsoredAds();
    }
    
    /**
     * Fetch sponsored ads from the repository
     */
    public void fetchSponsoredAds() {
        repository.getSponsoredAds();
    }
    
    /**
     * Get sponsored ads for a specific location and select the best one to display
     * @param location The location to get ads for (e.g., "home_bottom")
     * @return LiveData containing the selected ad
     */
    public LiveData<SponsoredAd> getAdForLocation(String location) {
        Log.d(TAG, "Getting sponsored ad for location: " + location);
        LiveData<List<SponsoredAd>> adsForLocation = repository.getAdsByLocation(location);
        
        // Add source to mediator LiveData
        selectedAdLiveData.addSource(adsForLocation, ads -> {
            if (ads != null && !ads.isEmpty()) {
                Log.d(TAG, "Found " + ads.size() + " ads for location: " + location);
                // Select best ad based on priority or random if multiple with same priority
                SponsoredAd selectedAd = selectBestAd(ads);
                selectedAdLiveData.setValue(selectedAd);
                adLoadedLiveData.setValue(true);
                
                // Log which ad was selected but DON'T track impression yet
                // Impression will be tracked when the ad is actually visible to the user
                if (selectedAd != null) {
                    Log.d(TAG, "Selected ad for display: " + selectedAd.getId() + 
                          ", title: " + selectedAd.getTitle() + 
                          ", priority: " + selectedAd.getPriority() +
                          ", location: " + location);
                } else {
                    Log.w(TAG, "No valid ad selected despite having " + ads.size() + " ads for location: " + location);
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
            }
        }
        
        // If multiple ads have the same highest priority, select one randomly
        if (bestAd != null) {
            final int topPriority = highestPriority;
            int count = 0;
            for (SponsoredAd ad : ads) {
                if (ad.isStatus() && ad.getPriority() == topPriority) {
                    count++;
                }
            }
            
            Log.d(TAG, "Found " + count + " ads with priority " + topPriority);
            
            if (count > 1) {
                // Multiple ads with same priority, pick randomly
                Random random = new Random();
                int randomIndex = random.nextInt(count);
                int currentIndex = 0;
                
                Log.d(TAG, "Selecting random ad from " + count + " with same priority, random index: " + randomIndex);
                
                for (SponsoredAd ad : ads) {
                    if (ad.isStatus() && ad.getPriority() == topPriority) {
                        if (currentIndex == randomIndex) {
                            bestAd = ad;
                            Log.d(TAG, "Randomly selected ad: " + ad.getId());
                            break;
                        }
                        currentIndex++;
                    }
                }
            }
        } else {
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
                      
            // Record the impression
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
        }
    }
    
    /**
     * Check if an ad is loaded and available
     * @return LiveData of ad loaded state
     */
    public LiveData<Boolean> isAdLoaded() {
        return adLoadedLiveData;
    }
    
    /**
     * Get loading state from repository
     * @return LiveData of loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return repository.getLoadingState();
    }
    
    /**
     * Get error state from repository
     * @return LiveData of error message
     */
    public LiveData<String> getError() {
        return repository.getError();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopRotation();
    }
} 