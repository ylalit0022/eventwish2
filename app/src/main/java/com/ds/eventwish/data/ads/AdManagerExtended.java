package com.ds.eventwish.data.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Extended manager class for handling all ad-related operations
 */
public class AdManagerExtended {
    private static final String TAG = "AdManagerExtended";
    
    // Ad unit IDs (replace with your actual ad unit IDs in production)
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"; // Test ad unit ID
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ad unit ID
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ad unit ID
    public static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; // Test ad unit ID
    //public static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; // Test ad unit ID

    public static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"; // Test ad unit ID

    private static AdManagerExtended instance;
    private final Context context;
    
    // App open ad
    private AppOpenAd appOpenAd;
    private boolean isAppOpenAdLoading = false;
    private final MutableLiveData<Boolean> appOpenAdLoaded = new MutableLiveData<>(false);
    
    /**
     * Private constructor
     * @param context The application context
     */
    private AdManagerExtended(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK initialized");
        });
    }
    
    /**
     * Get the singleton instance
     * @param context The context
     * @return The AdManagerExtended instance
     */
    public static synchronized AdManagerExtended getInstance(Context context) {
        if (instance == null) {
            instance = new AdManagerExtended(context);
        }
        return instance;
    }
    
    /**
     * Get an ad request
     * @return The ad request
     */
    public static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }
    
    /**
     * Load an interstitial ad
     * @param callback Callback to be invoked when the ad is loaded or fails to load
     */
    public void loadInterstitialAd(InterstitialAdCallback callback) {
        Log.d(TAG, "Loading interstitial ad");
        
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded successfully");
                        if (callback != null) {
                            callback.onInterstitialAdLoaded(interstitialAd);
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        if (callback != null) {
                            callback.onInterstitialAdFailedToLoad(loadAdError);
                        }
                    }
                });
    }
    
    /**
     * Load a rewarded ad
     * @param callback Callback to be invoked when the ad is loaded or fails to load
     */
    public void loadRewardedAd(RewardedAdCallback callback) {
        Log.d(TAG, "Loading rewarded ad");
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, getAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        Log.d(TAG, "Rewarded ad loaded successfully");
                        if (callback != null) {
                            callback.onRewardedAdLoaded(rewardedAd);
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        if (callback != null) {
                            callback.onRewardedAdFailedToLoad(loadAdError);
                        }
                    }
                });
    }
    
    /**
     * Load an app open ad
     */
    public void loadAppOpenAd() {
        if (isAppOpenAdLoading || appOpenAd != null) {
            return;
        }
        
        isAppOpenAdLoading = true;
        appOpenAdLoaded.setValue(false);
        
        Log.d(TAG, "Loading app open ad");
        
        AppOpenAd.load(context, APP_OPEN_AD_UNIT_ID, getAdRequest(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        Log.d(TAG, "App open ad loaded successfully");
                        appOpenAd = ad;
                        isAppOpenAdLoading = false;
                        appOpenAdLoaded.setValue(true);
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage());
                        isAppOpenAdLoading = false;
                        appOpenAdLoaded.setValue(false);
                    }
                });
    }
    
    /**
     * Show an app open ad
     * @param activity The activity
     * @param callback Callback to be invoked when the ad is shown or dismissed
     */
    public void showAppOpenAd(Activity activity, FullScreenContentCallback callback) {
        if (appOpenAd == null) {
            Log.d(TAG, "App open ad not loaded yet");
            loadAppOpenAd();
            if (callback != null) {
                callback.onAdFailedToShowFullScreenContent(
                        new AdError(0, "App open ad not loaded yet", "AdManagerExtended"));
            }
            return;
        }
        
        Log.d(TAG, "Showing app open ad");
        
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                appOpenAd = null;
                appOpenAdLoaded.setValue(false);
                loadAppOpenAd();
                if (callback != null) {
                    callback.onAdDismissedFullScreenContent();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                appOpenAd = null;
                appOpenAdLoaded.setValue(false);
                loadAppOpenAd();
                if (callback != null) {
                    callback.onAdFailedToShowFullScreenContent(adError);
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed successfully");
                if (callback != null) {
                    callback.onAdShowedFullScreenContent();
                }
            }
        });
        
        appOpenAd.show(activity);
    }
    
    /**
     * Get the app open ad loaded LiveData
     * @return The app open ad loaded LiveData
     */
    public LiveData<Boolean> getAppOpenAdLoadedLiveData() {
        return appOpenAdLoaded;
    }
    
    /**
     * Callback for interstitial ads
     */
    public interface InterstitialAdCallback {
        /**
         * Called when the interstitial ad is loaded
         * @param interstitialAd The interstitial ad
         */
        void onInterstitialAdLoaded(InterstitialAd interstitialAd);
        
        /**
         * Called when the interstitial ad fails to load
         * @param loadAdError The load ad error
         */
        void onInterstitialAdFailedToLoad(LoadAdError loadAdError);
    }
    
    /**
     * Callback for rewarded ads
     */
    public interface RewardedAdCallback {
        /**
         * Called when the rewarded ad is loaded
         * @param rewardedAd The rewarded ad
         */
        void onRewardedAdLoaded(RewardedAd rewardedAd);
        
        /**
         * Called when the rewarded ad fails to load
         * @param loadAdError The load ad error
         */
        void onRewardedAdFailedToLoad(LoadAdError loadAdError);
        
        /**
         * Called when the user earns a reward
         * @param amount The amount of the reward
         * @param type The type of the reward
         */
        default void onRewardEarned(int amount, String type) {}
    }
} 