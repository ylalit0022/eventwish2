package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.repository.SponsoredAdRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages ad rotation logic to ensure fair distribution of sponsored ads
 */
public class LocalRotationManager {
    private static final String TAG = "LocalRotationManager";
    private static final String PREF_NAME = "sponsored_ad_rotation";
    private static final String KEY_LAST_ROTATION = "last_rotation_timestamp";
    private static final String KEY_SHOWN_ADS = "shown_ads";
    private static final String KEY_ROTATION_LOCATION = "rotation_location";
    private static final long DEFAULT_ROTATION_INTERVAL_MS = TimeUnit.MINUTES.toMillis(20); // 20 minutes
    
    private final SharedPreferences prefs;
    private final Handler rotationHandler;
    private final SponsoredAdRepository repository;
    private long rotationIntervalMs;
    private Set<String> shownAdsInSession;
    private String currentLocation;
    private boolean isRotationActive = false;
    
    /**
     * Constructor and initialization
     */
    public LocalRotationManager(Context context, SponsoredAdRepository repository) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.rotationHandler = new Handler(Looper.getMainLooper());
        this.repository = repository;
        this.rotationIntervalMs = DEFAULT_ROTATION_INTERVAL_MS;
        this.shownAdsInSession = new LinkedHashSet<>();
        
        // Restore persisted state
        restoreState();
    }
    
    /**
     * Start rotation for a specific location
     */
    public void startRotation(String location, RotationCallback callback) {
        if (isRotationActive) {
            Log.d(TAG, "Rotation already active for location: " + currentLocation);
            
            // If location changed, update and restart
            if (!location.equals(currentLocation)) {
                Log.d(TAG, "Changing rotation location from " + currentLocation + " to " + location);
                stopRotation();
                this.currentLocation = location;
                prefs.edit().putString(KEY_ROTATION_LOCATION, location).apply();
                isRotationActive = true;
                scheduleNextRotation(callback);
            }
            return;
        }
        
        Log.d(TAG, "Starting rotation for location: " + location + 
              " with interval: " + (rotationIntervalMs / 1000) + " seconds");
        this.currentLocation = location;
        this.isRotationActive = true;
        
        // Save the location
        prefs.edit().putString(KEY_ROTATION_LOCATION, location).apply();
        
        // Schedule first rotation
        scheduleNextRotation(callback);
    }
    
    /**
     * Stop ongoing rotation
     */
    public void stopRotation() {
        Log.d(TAG, "Stopping rotation");
        isRotationActive = false;
        rotationHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Schedule the next ad rotation
     */
    private void scheduleNextRotation(RotationCallback callback) {
        if (!isRotationActive) {
            Log.d(TAG, "Rotation is not active, not scheduling next rotation");
            return;
        }
        
        // Calculate time until next rotation
        long lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0);
        long now = System.currentTimeMillis();
        long timeElapsed = now - lastRotation;
        
        // If enough time has passed, rotate immediately
        if (timeElapsed >= rotationIntervalMs || lastRotation == 0) {
            Log.d(TAG, "Time to rotate: elapsed=" + timeElapsed + "ms, threshold=" + rotationIntervalMs + "ms");
            rotateAd(callback);
        } else {
            // Otherwise, schedule for remaining time
            long delay = rotationIntervalMs - timeElapsed;
            Log.d(TAG, "Scheduling next rotation in " + delay + "ms");
            rotationHandler.postDelayed(() -> rotateAd(callback), delay);
        }
    }
    
    /**
     * Perform ad rotation
     */
    private void rotateAd(RotationCallback callback) {
        if (!isRotationActive) {
            Log.d(TAG, "Rotation is not active, skipping rotation");
            return;
        }
        
        // Update rotation timestamp
        long now = System.currentTimeMillis();
        long lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0);
        long timeSinceLastRotation = now - lastRotation;
        
        prefs.edit().putLong(KEY_LAST_ROTATION, now).apply();
        
        Log.d(TAG, "Rotating ad for location: " + currentLocation + 
              ", excluding " + shownAdsInSession.size() + " ads" +
              ", time since last rotation: " + (timeSinceLastRotation / 1000) + " seconds");
        
        // Get next ad for rotation
        repository.getNextRotationAd(currentLocation, shownAdsInSession)
            .observeForever(new Observer<SponsoredAd>() {
                @Override
                public void onChanged(SponsoredAd ad) {
                    repository.getNextRotationAd(currentLocation, shownAdsInSession)
                        .removeObserver(this);
                    
                    if (ad != null) {
                        Log.d(TAG, "Rotated to new ad: " + ad.getId() + 
                              " - " + ad.getTitle() + 
                              ", priority: " + ad.getPriority() + 
                              ", impressions: " + ad.getImpressionCount());
                        
                        // Record this ad as shown
                        shownAdsInSession.add(ad.getId());
                        persistShownAds();
                        
                        // Notify callback
                        callback.onAdRotated(ad);
                        
                        // Log next rotation time
                        long nextRotationTime = now + rotationIntervalMs;
                        Log.d(TAG, "Next rotation scheduled at: " + 
                              new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                  .format(new java.util.Date(nextRotationTime)) +
                              " (" + (rotationIntervalMs / 1000) + " seconds from now)");
                    } else {
                        Log.d(TAG, "No ad available for rotation, will try again later");
                        
                        // If we run out of ads, clear the shown list to start fresh
                        if (!shownAdsInSession.isEmpty()) {
                            Log.d(TAG, "Clearing shown ads list to get fresh ads");
                            shownAdsInSession.clear();
                            persistShownAds();
                        }
                    }
                    
                    // Schedule next rotation if still active
                    if (isRotationActive) {
                        rotationHandler.postDelayed(() -> rotateAd(callback), rotationIntervalMs);
                    }
                }
            });
    }
    
    /**
     * Save shown ads to preferences
     */
    private void persistShownAds() {
        // Keep only the last 20 shown ads
        List<String> recentAds = new ArrayList<>(shownAdsInSession);
        if (recentAds.size() > 20) {
            recentAds = recentAds.subList(recentAds.size() - 20, recentAds.size());
            shownAdsInSession = new LinkedHashSet<>(recentAds);
        }
        
        // Save to preferences
        Set<String> adSet = new HashSet<>(shownAdsInSession);
        prefs.edit().putStringSet(KEY_SHOWN_ADS, adSet).apply();
        
        Log.d(TAG, "Persisted " + adSet.size() + " shown ads to preferences");
    }
    
    /**
     * Restore state from preferences
     */
    private void restoreState() {
        Set<String> savedAds = prefs.getStringSet(KEY_SHOWN_ADS, null);
        currentLocation = prefs.getString(KEY_ROTATION_LOCATION, null);
        
        if (savedAds != null) {
            shownAdsInSession = new LinkedHashSet<>(savedAds);
            Log.d(TAG, "Restored " + shownAdsInSession.size() + " shown ads from preferences");
        }
        
        if (currentLocation != null) {
            Log.d(TAG, "Restored rotation location: " + currentLocation);
        }
    }
    
    /**
     * Reset rotation state
     */
    public void resetRotation() {
        shownAdsInSession.clear();
        prefs.edit()
            .remove(KEY_LAST_ROTATION)
            .remove(KEY_SHOWN_ADS)
            .apply();
        
        Log.d(TAG, "Reset rotation state");
    }
    
    /**
     * Set custom rotation interval
     */
    public void setRotationInterval(long intervalMs) {
        this.rotationIntervalMs = intervalMs;
        Log.d(TAG, "Set rotation interval to " + intervalMs + "ms");
    }
    
    /**
     * Get the list of shown ads
     */
    public Set<String> getShownAdsInSession() {
        return new HashSet<>(shownAdsInSession);
    }
    
    /**
     * Get the current rotation interval
     */
    public long getRotationInterval() {
        return rotationIntervalMs;
    }
    
    /**
     * Get the current rotation location
     */
    public String getCurrentLocation() {
        return currentLocation;
    }
    
    /**
     * Callback for rotation events
     */
    public interface RotationCallback {
        void onAdRotated(SponsoredAd ad);
    }
} 