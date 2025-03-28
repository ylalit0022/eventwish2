package com.ds.eventwish.data.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class AppOpenAdHelper implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "AppOpenAdHelper";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; // Test ad unit ID
    
    private final Application application;
    private Activity currentActivity;
    private AppOpenAd appOpenAd = null;
    private boolean isShowingAd = false;
    private long loadTime = 0;
    private static final long TIMEOUT_DURATION_MILLIS = 3600000; // 1 hour
    
    /**
     * Constructor
     */
    public AppOpenAdHelper(Application application) {
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }
    
    /**
     * DefaultLifecycleObserver methods
     */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App moved to foreground, show ad if available
        showAdIfAvailable();
    }
    
    /**
     * Load an app open ad
     */
    public void loadAd() {
        // Don't load if already loaded and hasn't expired
        if (isAdAvailable()) {
            return;
        }
        
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(application, AD_UNIT_ID, request, 
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        Log.d(TAG, "App open ad loaded");
                        appOpenAd = ad;
                        loadTime = System.currentTimeMillis();
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }
    
    /**
     * Shows the ad if one is available
     */
    public void showAdIfAvailable() {
        // Only show ad if there is an ad available, no ad showing, and app is in foreground
        if (!isAdAvailable() || isShowingAd || currentActivity == null) {
            return;
        }
        
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                // Clear the ad and set isShowingAd to false
                appOpenAd = null;
                isShowingAd = false;
                // Load the next ad
                loadAd();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                // Clear the ad and set isShowingAd to false
                appOpenAd = null;
                isShowingAd = false;
                // Load the next ad
                loadAd();
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed");
                isShowingAd = true;
            }
        });
        
        isShowingAd = true;
        appOpenAd.show(currentActivity);
    }
    
    /**
     * Check if the ad is available
     * @return true if ad is available, false otherwise
     */
    private boolean isAdAvailable() {
        return appOpenAd != null && !isAdExpired();
    }
    
    /**
     * Check if the ad has expired
     * @return true if ad has expired, false otherwise
     */
    private boolean isAdExpired() {
        return System.currentTimeMillis() - loadTime > TIMEOUT_DURATION_MILLIS;
    }
    
    /**
     * ActivityLifecycleCallbacks methods
     */
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {}
    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Set the current activity to the started activity
        currentActivity = activity;
    }
    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }
    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {}
    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {}
    
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}
    
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Clear the current activity if it's the one being destroyed
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
} 