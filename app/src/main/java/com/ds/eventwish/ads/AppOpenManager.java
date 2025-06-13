package com.ds.eventwish.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private static final int MAX_RETRY_CYCLES = 3; // Maximum number of full retry cycles
    private static final long RETRY_DELAY = 1000L; // 1 second
    private static final long MIN_COOLING_PERIOD = 10000L; // 10 second cooling period
    private static final long MAX_COOLING_PERIOD = 300000L; // 5 minute maximum cooling period

    private final Application application;
    private final Set<Class<? extends Activity>> disabledActivities;
    private final Handler timeoutHandler;
    
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private boolean isFirstLaunch = true;
    private long loadTime = 0;
    private int retryAttempt = 0;
    private int retryCycle = 0;
    private boolean permanentFailure = false;

    @Nullable private Activity currentActivity;

    public AppOpenManager(Application application) {
        this.application = application;
        this.disabledActivities = new HashSet<>();
        this.timeoutHandler = new Handler(Looper.getMainLooper());
        this.retryAttempt = 0; // Explicitly initialize retry counter
        this.retryCycle = 0; // Initialize cycle counter
        
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        
        Log.d(TAG, "AppOpenManager initialized");
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

    /**
     * Checks for network connectivity
     * @return true if connected, false otherwise
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return false;
            }
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Error checking network state", e);
            return false;
        }
    }

    /** Request an ad with timeout and retry mechanism */
    public void fetchAd() {
        // Skip if we're already loading or have a permanent failure
        if (isLoadingAd || isAdAvailable() || permanentFailure) {
            if (permanentFailure) {
                Log.d(TAG, "Skipping ad fetch due to permanent failure state");
            }
            return;
        }
        
        // Check network connectivity
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network not available, skipping ad fetch");
            // Schedule a retry after network might be available
            timeoutHandler.postDelayed(this::fetchAd, MIN_COOLING_PERIOD);
            return;
        }
        
        // Check if AdMobManager is properly initialized
        if (!AdMobManager.isInitialized()) {
            Log.e(TAG, "AdMobManager not initialized, cannot fetch ads");
            // Don't enter permanent failure state since this might be a temporary issue during app startup
            // Schedule a retry instead
            timeoutHandler.postDelayed(this::fetchAd, MIN_COOLING_PERIOD);
            return;
        }

        isLoadingAd = true;

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
                retryCycle = 0; // Reset cycle counter on success
                permanentFailure = false; // Clear permanent failure state on success
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
     * Reset the retry state to allow future attempts
     */
    public void resetFailureState() {
        Log.d(TAG, "Resetting ad failure state");
        permanentFailure = false;
        retryAttempt = 0;
        retryCycle = 0;
    }

    /**
     * Implements exponential backoff for retries with cycle limits
     */
    private void retryWithBackoff() {
        retryAttempt++;
        
        if (retryAttempt <= MAX_RETRY_ATTEMPTS) {
            long delay = RETRY_DELAY * (1L << (retryAttempt - 1));
            Log.d(TAG, String.format("Scheduling retry attempt %d/%d in %d ms (cycle %d/%d)", 
                retryAttempt, MAX_RETRY_ATTEMPTS, delay, retryCycle + 1, MAX_RETRY_CYCLES));
            
            timeoutHandler.postDelayed(() -> {
                Log.d(TAG, "Executing retry attempt " + retryAttempt);
                isLoadingAd = false; // Ensure loading flag is reset before retry
                fetchAd();
            }, delay);
        } else {
            // We've reached the max retry attempts for this cycle
            Log.e(TAG, "Max retry attempts reached for cycle " + (retryCycle + 1));
            retryAttempt = 0;
            retryCycle++;
            
            if (retryCycle >= MAX_RETRY_CYCLES) {
                // We've reached the maximum number of retry cycles
                Log.e(TAG, "Max retry cycles reached (" + MAX_RETRY_CYCLES + "). Entering permanent failure state.");
                permanentFailure = true;
                return;
            }
            
            // Calculate cooling period based on the retry cycle (increasing delay)
            long coolingPeriod = Math.min(
                MIN_COOLING_PERIOD * (1L << retryCycle), 
                MAX_COOLING_PERIOD
            );
            
            Log.d(TAG, String.format("Starting cooling period of %d ms before cycle %d/%d", 
                coolingPeriod, retryCycle + 1, MAX_RETRY_CYCLES));
                
            // Wait longer before trying again after max retries
            timeoutHandler.postDelayed(() -> {
                Log.d(TAG, "Attempting to load ad after cooling period (cycle " + (retryCycle + 1) + ")");
                isLoadingAd = false;
                fetchAd();
            }, coolingPeriod);
        }
    }

    /** Shows the ad if one isn't already showing and conditions are met */
    public void showAdIfAvailable() {
        // Don't show ad if:
        // 1. Ad is already showing
        // 2. Ad is not available
        // 3. Current activity is null
        // 4. Current activity is in disabled list
        // 5. In permanent failure state
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing, not showing another one");
            return;
        }
        
        if (permanentFailure) {
            Log.d(TAG, "In permanent failure state, not attempting to show ad");
            return;
        }
        
        if (!isAdAvailable()) {
            Log.d(TAG, "No ad available to show");
            if (!isLoadingAd && !permanentFailure) {
                Log.d(TAG, "No ad is loading, starting fetch");
                fetchAd();
            } else {
                Log.d(TAG, "Ad is currently loading or in permanent failure state, waiting for completion");
            }
            return;
        }
        
        if (currentActivity == null) {
            Log.d(TAG, "Current activity is null, cannot show ad");
            return;
        }
        
        if (isAppOpenDisabled()) {
            Log.d(TAG, "App open ads are disabled for " + currentActivity.getClass().getSimpleName());
            return;
        }
        
        Log.d(TAG, "Showing app open ad");

        FullScreenContentCallback fullScreenContentCallback =
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed");
                        appOpenAd = null;
                        isShowingAd = false;
                        fetchAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.e(TAG, "Failed to show ad: " + adError.getMessage());
                        isShowingAd = false;
                        appOpenAd = null; // Clear the ad reference on failure
                        fetchAd();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed successfully");
                        isShowingAd = true;
                    }
                };

        try {
            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } catch (Exception e) {
            Log.e(TAG, "Error showing app open ad: " + e.getMessage());
            isShowingAd = false;
            appOpenAd = null;
            fetchAd();
        }
    }

    /** Check if ad exists and is not expired */
    private boolean isAdAvailable() {
        boolean adExists = appOpenAd != null;
        boolean notExpired = !isAdExpired();
        boolean result = adExists && notExpired;
        
        if (adExists && !notExpired) {
            Log.d(TAG, "Ad exists but is expired, need to fetch a new one");
        } else if (!adExists) {
            Log.d(TAG, "No ad available");
        }
        
        return result;
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
        Log.d(TAG, "Destroying AppOpenManager");
        timeoutHandler.removeCallbacksAndMessages(null);
        appOpenAd = null;
        currentActivity = null;
        isLoadingAd = false;
        isShowingAd = false;
        
        // Clean up lifecycle observer
        try {
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        } catch (Exception e) {
            Log.e(TAG, "Error removing lifecycle observer", e);
        }
    }

    /**
     * Checks if the manager is in a permanent failure state
     * @return true if in permanent failure state, false otherwise
     */
    public boolean isInPermanentFailureState() {
        return permanentFailure;
    }
} 