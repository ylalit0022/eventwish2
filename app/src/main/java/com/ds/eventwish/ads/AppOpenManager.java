package com.ds.eventwish.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
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

/**
 * Handles app open ads for the application.
 * Shows ads when the app is brought to foreground.
 */
public class AppOpenManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AppOpenManager";
    private static final long TIMEOUT_DURATION_MILLIS = 4 * 3600 * 1000; // 4 hours

    private final Application application;
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long loadTime = 0;

    @Nullable private Activity currentActivity;

    public AppOpenManager(Application application) {
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /** Request an ad */
    public void fetchAd() {
        if (isLoadingAd || isAdAvailable()) {
            return;
        }

        isLoadingAd = true;
        AdMobManager.getInstance().loadAppOpenAd(new AdMobManager.AppOpenAdCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                Log.d(TAG, "App open ad loaded successfully");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load app open ad: " + message);
                isLoadingAd = false;
            }
        });
    }

    /** Shows the ad if one isn't already showing. */
    public void showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable() && currentActivity != null) {
            Log.d(TAG, "Will show ad.");

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
                        }
                    };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } else {
            Log.d(TAG, "Can't show ad: showing=" + isShowingAd + " available=" + isAdAvailable() + " activity=" + currentActivity);
            fetchAd();
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
} 