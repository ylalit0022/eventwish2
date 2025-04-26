package com.ds.eventwish.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles app open ads for the application.
 * Shows ads when the app is brought to foreground.
 */
public class AppOpenManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AppOpenManager";
    private static final long TIMEOUT_DURATION_MILLIS = 4 * 3600 * 1000; // 4 hours
    private static final long AD_LOAD_TIMEOUT = 10000L; // 10 seconds
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY = 1000L; // 1 second

    private final Application application;
    private final Set<Class<? extends Activity>> disabledActivities;
    private final Handler timeoutHandler;
    
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private boolean isFirstLaunch = true;
    private long loadTime = 0;
    private int retryAttempt = 0;

    @Nullable private Activity currentActivity;

    public AppOpenManager(Application application) {
        this.application = application;
        this.disabledActivities = new HashSet<>();
        this.timeoutHandler = new Handler(Looper.getMainLooper());
        
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Adds an Activity class to the disabled list
     */
    public void disableAppOpenForActivity(Class<? extends Activity> activityClass) {
        disabledActivities.add(activityClass);
        Log.d(TAG, "Disabled app open ads for: " + activityClass.getSimpleName());
    }

    /**
     * Removes an Activity class from the disabled list
     */
    public void enableAppOpenForActivity(Class<? extends Activity> activityClass) {
        disabledActivities.remove(activityClass);
        Log.d(TAG, "Enabled app open ads for: " + activityClass.getSimpleName());
    }

    /**
     * Checks if app open ads are disabled for the current Activity
     */
    private boolean isAppOpenDisabled() {
        return currentActivity != null && disabledActivities.contains(currentActivity.getClass());
    }

    /** Request an ad with timeout and retry mechanism */
    public void fetchAd() {
        if (isLoadingAd || isAdAvailable()) {
            return;
        }

        isLoadingAd = true;
        retryAttempt++;

        // Set timeout for ad loading
        timeoutHandler.postDelayed(() -> {
            if (isLoadingAd) {
                Log.e(TAG, "Ad load timeout");
                isLoadingAd = false;
                retryWithBackoff();
            }
        }, AD_LOAD_TIMEOUT);

        AdMobManager.getInstance().loadAppOpenAd(new AdMobManager.AppOpenAdCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                timeoutHandler.removeCallbacksAndMessages(null);
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                retryAttempt = 0;
                Log.d(TAG, "App open ad loaded successfully");
            }

            @Override
            public void onError(String message) {
                timeoutHandler.removeCallbacksAndMessages(null);
                Log.e(TAG, "Failed to load app open ad: " + message);
                isLoadingAd = false;
                retryWithBackoff();
            }
        });
    }

    /**
     * Implements exponential backoff for retries
     */
    private void retryWithBackoff() {
        if (retryAttempt < MAX_RETRY_ATTEMPTS) {
            long delay = RETRY_DELAY * (1L << (retryAttempt - 1));
            Log.d(TAG, String.format("Scheduling retry attempt %d/%d in %d ms", 
                retryAttempt, MAX_RETRY_ATTEMPTS, delay));
            
            timeoutHandler.postDelayed(this::fetchAd, delay);
        } else {
            Log.e(TAG, "Max retry attempts reached");
            retryAttempt = 0;
        }
    }

    /** Shows the ad if one isn't already showing and conditions are met */
    public void showAdIfAvailable() {
        // Don't show ad if:
        // 1. Ad is already showing
        // 2. Ad is not available
        // 3. Current activity is null
        // 4. Current activity is in disabled list
        // 5. Not first launch (cold start)
        if (!isShowingAd && isAdAvailable() && currentActivity != null 
            && !isAppOpenDisabled() && isFirstLaunch) {
            
            Log.d(TAG, "Showing app open ad");

            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            Log.e(TAG, "Failed to show ad: " + adError.getMessage());
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                            isFirstLaunch = false; // Reset first launch flag after showing
                        }
                    };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } else {
            Log.d(TAG, String.format("Can't show ad: showing=%b available=%b activity=%s disabled=%b firstLaunch=%b",
                isShowingAd, isAdAvailable(), 
                currentActivity != null ? currentActivity.getClass().getSimpleName() : "null",
                isAppOpenDisabled(), isFirstLaunch));
            
            if (!isLoadingAd && !isAdAvailable()) {
                fetchAd();
            }
        }
    }

    /** Check if ad exists and is not expired */
    private boolean isAdAvailable() {
        return appOpenAd != null && !isAdExpired();
    }

    /** Check if ad has expired */
    private boolean isAdExpired() {
        return (new Date()).getTime() - loadTime > TIMEOUT_DURATION_MILLIS;
    }

    /** ActivityLifecycleCallback methods */
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
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
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        currentActivity = null;
    }

    /** LifecycleObserver method */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        showAdIfAvailable();
    }

    /** Clean up */
    public void destroy() {
        timeoutHandler.removeCallbacksAndMessages(null);
        appOpenAd = null;
        currentActivity = null;
    }
} 