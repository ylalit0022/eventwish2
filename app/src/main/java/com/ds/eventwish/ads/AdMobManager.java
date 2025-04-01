package com.ds.eventwish.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.ds.eventwish.analytics.AdAnalytics;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdMobManager implements LifecycleObserver {
    private static final String TAG = "AdMobManager";
    private static volatile AdMobManager instance;
    private final Context applicationContext;
    private final Map<String, RewardedAd> rewardedAds;
    private final Map<String, InterstitialAd> interstitialAds;
    private final Map<String, AppOpenAd> appOpenAds;
    private final Map<String, Long> lastAdShownTime;
    private final Map<String, Integer> dailyImpressionCount;
    private final Map<String, Long> cooldownEndTime;
    private final AtomicBoolean isShowingAd;
    private Activity currentActivity;
    private AppOpenAd appOpenAd;
    private boolean isLoadingAd;
    private boolean isShowingAppOpenAd;
    private AdAnalytics analytics;

    private AdMobManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.rewardedAds = new HashMap<>();
        this.interstitialAds = new HashMap<>();
        this.appOpenAds = new HashMap<>();
        this.lastAdShownTime = new HashMap<>();
        this.dailyImpressionCount = new HashMap<>();
        this.cooldownEndTime = new HashMap<>();
        this.isShowingAd = new AtomicBoolean(false);
        this.isLoadingAd = false;
        this.isShowingAppOpenAd = false;
        this.analytics = AdAnalytics.getInstance(context);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(applicationContext, initializationStatus -> {
            // Set test device IDs
            List<String> testDeviceIds = Arrays.asList("TEST-DEVICE-HASH");
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build();
            MobileAds.setRequestConfiguration(configuration);
        });
    }

    public static AdMobManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AdMobManager.class) {
                if (instance == null) {
                    instance = new AdMobManager(context);
                }
            }
        }
        return instance;
    }

    public void setCurrentActivity(Activity activity) {
        this.currentActivity = activity;
    }

    // App Open Ad Methods
    public void loadAppOpenAd() {
        if (isLoadingAd || isAdAvailable(appOpenAd)) {
            return;
        }

        isLoadingAd = true;
        AppOpenAdLoadCallback loadCallback = new AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.d(TAG, "App open ad loaded successfully");
                appOpenAd = ad;
                isLoadingAd = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage());
                analytics.trackLoadFailure("app_open", loadAdError);
                isLoadingAd = false;
                appOpenAd = null;
            }
        };

        AdRequest request = getAdRequest();
        AppOpenAd.load(
                applicationContext,
                "ca-app-pub-3940256099942544/3419835294", // Test ad unit ID
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_UP,
                loadCallback
        );
    }

    public void showAppOpenAdIfAvailable() {
        if (!isAdAvailable(appOpenAd) || isShowingAppOpenAd) {
            return;
        }

        isShowingAppOpenAd = true;
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                analytics.trackImpression("app_open");
                isShowingAppOpenAd = false;
                appOpenAd = null;
                loadAppOpenAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                analytics.trackShowFailure("app_open", adError);
                isShowingAppOpenAd = false;
                appOpenAd = null;
                loadAppOpenAd();
            }
        });

        appOpenAd.show(currentActivity);
    }

    // Interstitial Ad Methods
    public void loadInterstitialAd(String adUnitId) {
        if (interstitialAds.containsKey(adUnitId)) {
            return;
        }

        InterstitialAdLoadCallback loadCallback = new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                Log.d(TAG, "Interstitial ad loaded successfully");
                interstitialAds.put(adUnitId, ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                analytics.trackLoadFailure(adUnitId, loadAdError);
                interstitialAds.remove(adUnitId);
            }
        };

        InterstitialAd.load(
                applicationContext,
                adUnitId,
                getAdRequest(),
                loadCallback
        );
    }

    public void showInterstitialAd(String adUnitId, @Nullable OnAdClosedListener listener) {
        InterstitialAd ad = interstitialAds.get(adUnitId);
        if (ad == null || isShowingAd.get()) {
            if (listener != null) {
                listener.onAdClosed();
            }
            return;
        }

        isShowingAd.set(true);
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                analytics.trackImpression(adUnitId);
                isShowingAd.set(false);
                interstitialAds.remove(adUnitId);
                if (listener != null) {
                    listener.onAdClosed();
                }
                loadInterstitialAd(adUnitId);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                analytics.trackShowFailure(adUnitId, adError);
                isShowingAd.set(false);
                interstitialAds.remove(adUnitId);
                if (listener != null) {
                    listener.onAdClosed();
                }
                loadInterstitialAd(adUnitId);
            }
        });

        ad.show(currentActivity);
    }

    // Rewarded Ad Methods
    public void loadRewardedAd(String adUnitId) {
        if (rewardedAds.containsKey(adUnitId)) {
            return;
        }

        RewardedAdLoadCallback loadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "Rewarded ad loaded successfully");
                rewardedAds.put(adUnitId, ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                analytics.trackLoadFailure(adUnitId, loadAdError);
                rewardedAds.remove(adUnitId);
            }
        };

        RewardedAd.load(
                applicationContext,
                adUnitId,
                getAdRequest(),
                loadCallback
        );
    }

    public void showRewardedAd(String adUnitId, OnUserEarnedRewardListener rewardListener,
                             @Nullable OnAdClosedListener closedListener) {
        RewardedAd ad = rewardedAds.get(adUnitId);
        if (ad == null || isShowingAd.get()) {
            if (closedListener != null) {
                closedListener.onAdClosed();
            }
            return;
        }

        isShowingAd.set(true);
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed");
                analytics.trackImpression(adUnitId);
                isShowingAd.set(false);
                rewardedAds.remove(adUnitId);
                if (closedListener != null) {
                    closedListener.onAdClosed();
                }
                loadRewardedAd(adUnitId);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                analytics.trackShowFailure(adUnitId, adError);
                isShowingAd.set(false);
                rewardedAds.remove(adUnitId);
                if (closedListener != null) {
                    closedListener.onAdClosed();
                }
                loadRewardedAd(adUnitId);
            }
        });

        ad.show(currentActivity, rewardListener);
    }

    // Ad Availability Check Methods
    public boolean canShowAd(String adUnitId) {
        if (cooldownEndTime.containsKey(adUnitId)) {
            long now = System.currentTimeMillis();
            if (now < cooldownEndTime.get(adUnitId)) {
                return false;
            }
        }

        if (dailyImpressionCount.containsKey(adUnitId)) {
            if (dailyImpressionCount.get(adUnitId) >= 10) { // Max 10 impressions per day
                return false;
            }
        }

        if (lastAdShownTime.containsKey(adUnitId)) {
            long now = System.currentTimeMillis();
            if (now - lastAdShownTime.get(adUnitId) < 60000) { // Minimum 1 minute between ads
                return false;
            }
        }

        return true;
    }

    public void recordAdShown(String adUnitId) {
        long now = System.currentTimeMillis();
        lastAdShownTime.put(adUnitId, now);
        
        int count = dailyImpressionCount.getOrDefault(adUnitId, 0);
        dailyImpressionCount.put(adUnitId, count + 1);
    }

    public void startCooldown(String adUnitId, long durationMillis) {
        long now = System.currentTimeMillis();
        cooldownEndTime.put(adUnitId, now + durationMillis);
    }

    // Helper Methods
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    private boolean isAdAvailable(Object ad) {
        return ad != null;
    }

    // Lifecycle Methods
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (currentActivity != null) {
            showAppOpenAdIfAvailable();
        }
    }

    // Interfaces
    public interface OnAdClosedListener {
        void onAdClosed();
    }
} 