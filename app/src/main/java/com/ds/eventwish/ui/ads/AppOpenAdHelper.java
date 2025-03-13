package com.ds.eventwish.ui.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.ds.eventwish.data.ads.AdManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;

/**
 * Helper class for loading and displaying app open ads
 * Implements Application.ActivityLifecycleCallbacks to show ads when app comes to foreground
 */
public class AppOpenAdHelper implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "AppOpenAdHelper";
    
    private final AdManager adManager;
    private final Application application;
    
    private Activity currentActivity;
    private boolean isShowingAd = false;
    private boolean isAppInForeground = false;
    private long lastAdDisplayTime = 0;
    private static final long MIN_AD_DISPLAY_INTERVAL = 4 * 60 * 60 * 1000; // 4 hours in milliseconds
    
    /**
     * Constructor
     */
    public AppOpenAdHelper(Application application) {
        this.application = application;
        this.adManager = AdManager.getInstance(application);
        
        // Register activity lifecycle callbacks
        application.registerActivityLifecycleCallbacks(this);
        
        // Register process lifecycle observer
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        
        // Load an app open ad
        loadAppOpenAd();
    }
    
    /**
     * Load an app open ad
     */
    public void loadAppOpenAd() {
        Log.d(TAG, "Loading app open ad");
        adManager.loadAppOpenAd();
    }
    
    /**
     * Show an app open ad
     */
    public void showAppOpenAd() {
        // Don't show ad if already showing
        if (isShowingAd) {
            Log.d(TAG, "App open ad is already showing");
            return;
        }
        
        // Don't show ad if no activity is available
        if (currentActivity == null) {
            Log.d(TAG, "No activity available to show app open ad");
            return;
        }
        
        // Don't show ad if not enough time has passed since the last ad
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdDisplayTime < MIN_AD_DISPLAY_INTERVAL) {
            Log.d(TAG, "Not enough time has passed since the last app open ad");
            return;
        }
        
        // Don't show ad if user has ad-free experience
        if (adManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not showing app open ad");
            return;
        }
        
        // Show the ad
        Log.d(TAG, "Attempting to show app open ad");
        isShowingAd = true;
        adManager.showAppOpenAd(currentActivity, new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                isShowingAd = false;
                lastAdDisplayTime = System.currentTimeMillis();
                loadAppOpenAd(); // Load the next ad
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                isShowingAd = false;
                loadAppOpenAd(); // Load the next ad
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed successfully");
                lastAdDisplayTime = System.currentTimeMillis();
            }
        });
    }
    
    /**
     * Check if app open ad is showing
     */
    public boolean isShowingAd() {
        return isShowingAd;
    }
    
    /**
     * Called when an activity is created
     */
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // Not used
    }
    
    /**
     * Called when an activity is started
     */
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Update the current activity
        currentActivity = activity;
    }
    
    /**
     * Called when an activity is resumed
     */
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // Update the current activity
        currentActivity = activity;
    }
    
    /**
     * Called when an activity is paused
     */
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Not used
    }
    
    /**
     * Called when an activity is stopped
     */
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // Not used
    }
    
    /**
     * Called when an activity is saved
     */
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // Not used
    }
    
    /**
     * Called when an activity is destroyed
     */
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Clear the current activity if it's being destroyed
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
    
    /**
     * Called when the app goes to the foreground
     */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App foregrounded");
        if (!isAppInForeground) {
            isAppInForeground = true;
            // Show the ad when app comes to foreground
            showAppOpenAd();
        }
    }
    
    /**
     * Called when the app goes to the background
     */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App backgrounded");
        isAppInForeground = false;
    }
} 