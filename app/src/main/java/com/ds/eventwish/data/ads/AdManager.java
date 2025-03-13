package com.ds.eventwish.data.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main manager class for handling all ad-related operations.
 * Responsible for loading, showing, and caching ads.
 */
public class AdManager {
    private static final String TAG = "AdManager";
    
    // Ad unit IDs (replace with your actual ad unit IDs in production)
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"; // Test ad unit ID
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ad unit ID
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ad unit ID
    public static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; // Test ad unit ID
    public static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; // Test ad unit ID
    
    // Context and preferences
    private final Context context;
    private final AdPreferenceManager preferenceManager;
    
    // Ad cache manager
    private final AdCacheManager cacheManager;
    
    // Handler for main thread operations
    private final Handler mainHandler;
    
    // Executor for background tasks
    private final ScheduledExecutorService executor;
    
    // LiveData for ad loading status
    private final MutableLiveData<Boolean> interstitialAdLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> rewardedAdLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> appOpenAdLoaded = new MutableLiveData<>(false);
    private final Map<String, MutableLiveData<Boolean>> nativeAdLoadedMap = new HashMap<>();
    
    // Singleton instance
    private static AdManager instance;
    
    // Flag to track if AdMob is initialized
    private boolean isInitialized = false;
    
    // Timeout handlers
    private Runnable interstitialTimeoutRunnable;
    private Runnable rewardedTimeoutRunnable;
    
    // Ad instances
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AppOpenAd appOpenAd;
    private final Map<String, NativeAd> nativeAds = new HashMap<>();
    
    // App Open Ad timestamp
    private long appOpenAdLoadTime = 0;
    
    /**
     * Private constructor for singleton pattern.
     * @param context the application context
     */
    private AdManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferenceManager = new AdPreferenceManager(context);
        this.cacheManager = AdCacheManager.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newScheduledThreadPool(2);
        
        // Initialize AdMob
        initializeAdMob();
    }
    
    /**
     * Gets the singleton instance of AdManager.
     * @param context the context
     * @return the singleton instance
     */
    public static synchronized AdManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdManager(context);
        }
        return instance;
    }
    
    /**
     * Initializes the AdMob SDK.
     */
    private void initializeAdMob() {
        try {
            // Configure ad requests
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Arrays.asList(AdRequest.DEVICE_ID_EMULATOR))
                .build();
            MobileAds.setRequestConfiguration(configuration);
            
            // Initialize the Mobile Ads SDK
            MobileAds.initialize(context, this::onAdMobInitialized);
            
            Log.d(TAG, "AdMob initialization started");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMob: " + e.getMessage());
        }
    }
    
    /**
     * Callback for when AdMob is initialized.
     * @param initializationStatus the initialization status
     */
    private void onAdMobInitialized(InitializationStatus initializationStatus) {
        isInitialized = true;
        Log.d(TAG, "AdMob initialized successfully");
        
        // Start preloading ads
        preloadInterstitialAd();
        preloadRewardedAd();
        loadAppOpenAd();
    }
    
    /**
     * Creates a new AdRequest.
     * @return the AdRequest
     */
    private AdRequest createAdRequest() {
        return new AdRequest.Builder().build();
    }
    
    /**
     * Creates and loads a banner ad.
     * @param adSize the size of the banner ad
     * @return the AdView
     */
    public AdView createBannerAd(AdSize adSize) {
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not creating banner ad");
            return null;
        }
        
        Log.d(TAG, "Creating banner ad");
        AdView adView = new AdView(context);
        adView.setAdUnitId(AdConstants.getBannerAdUnitId());
        adView.setAdSize(adSize);
        
        // Set up ad listener
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
            }
            
            @Override
            public void onAdClosed() {
                Log.d(TAG, "Banner ad closed");
            }
        });
        
        // Load the ad
        adView.loadAd(createAdRequest());
        
        return adView;
    }
    
    /**
     * Adds a banner ad to the specified container.
     * @param container the container to add the banner ad to
     * @param adSize the size of the banner ad
     * @return the AdView that was added, or null if the user has an ad-free experience
     */
    public AdView addBannerToContainer(ViewGroup container, AdSize adSize) {
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not adding banner ad");
            return null;
        }
        
        // Create and load the banner ad
        AdView adView = createBannerAd(adSize);
        
        // Add the ad view to the container
        container.removeAllViews();
        container.addView(adView);
        
        // Schedule periodic refresh
        scheduleAdRefresh(adView);
        
        return adView;
    }
    
    /**
     * Schedules periodic refresh of a banner ad.
     * @param adView the AdView to refresh
     */
    private void scheduleAdRefresh(AdView adView) {
        executor.scheduleAtFixedRate(() -> {
            if (adView != null && !preferenceManager.isAdFree()) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Refreshing banner ad");
                    adView.loadAd(createAdRequest());
                });
            }
        }, AdConstants.BANNER_REFRESH_INTERVAL, AdConstants.BANNER_REFRESH_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Preloads an interstitial ad and adds it to the cache.
     */
    public void preloadInterstitialAd() {
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not preloading interstitial ad");
            return;
        }
        
        if (interstitialAdLoading.getValue() != null && interstitialAdLoading.getValue()) {
            Log.d(TAG, "Interstitial ad is already loading");
            return;
        }
        
        if (cacheManager.getInterstitialCacheSize() >= AdConstants.MAX_INTERSTITIAL_CACHE_SIZE) {
            Log.d(TAG, "Interstitial ad cache is full");
            return;
        }
        
        Log.d(TAG, "Preloading interstitial ad");
        interstitialAdLoading.setValue(true);
        
        // Set up timeout
        interstitialTimeoutRunnable = () -> {
            Log.w(TAG, "Interstitial ad load timed out");
            interstitialAdLoading.postValue(false);
        };
        mainHandler.postDelayed(interstitialTimeoutRunnable, AdConstants.AD_LOAD_TIMEOUT);
        
        // Load the interstitial ad
        InterstitialAd.load(context, AdConstants.getInterstitialAdUnitId(), createAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mainHandler.removeCallbacks(interstitialTimeoutRunnable);
                        Log.d(TAG, "Interstitial ad loaded successfully");
                        
                        // Add the ad to the cache
                        cacheManager.cacheInterstitialAd(interstitialAd);
                        
                        interstitialAdLoading.setValue(false);
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mainHandler.removeCallbacks(interstitialTimeoutRunnable);
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        
                        interstitialAdLoading.setValue(false);
                        
                        // Retry after a delay
                        mainHandler.postDelayed(() -> preloadInterstitialAd(), 60000); // Retry after 1 minute
                    }
                });
    }
    
    /**
     * Shows an interstitial ad if one is available and conditions are met.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showInterstitialAd(Activity activity, InterstitialAdCallback callback) {
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not showing interstitial ad");
            if (callback != null) {
                callback.onAdDismissed();
            }
            return false;
        }
        
        // Check if enough time has passed since the last interstitial ad was shown
        long lastShownTime = preferenceManager.getLastInterstitialShownTime();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShownTime < AdConstants.INTERSTITIAL_REFRESH_INTERVAL) {
            Log.d(TAG, "Not enough time has passed since the last interstitial ad was shown");
            if (callback != null) {
                callback.onAdDismissed();
            }
            return false;
        }
        
        // Check if the interstitial count threshold has been reached
        int count = preferenceManager.getInterstitialCount();
        if (count < AdConstants.INTERSTITIAL_DISPLAY_THRESHOLD) {
            Log.d(TAG, "Interstitial count threshold not reached: " + count + "/" + 
                    AdConstants.INTERSTITIAL_DISPLAY_THRESHOLD);
            preferenceManager.incrementInterstitialCount();
            if (callback != null) {
                callback.onAdDismissed();
            }
            return false;
        }
        
        // Get an interstitial ad from the cache
        InterstitialAd interstitialAd = cacheManager.getInterstitialAd();
        if (interstitialAd == null) {
            Log.d(TAG, "No interstitial ad available in cache, loading a new one");
            
            // Preload a new ad for next time
            preloadInterstitialAd();
            
            // Try to load an ad immediately and show it
            InterstitialAd.load(context, AdConstants.getInterstitialAdUnitId(), createAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d(TAG, "Interstitial ad loaded successfully, showing immediately");
                        
                        // Set up full screen content callback
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Interstitial ad dismissed");
                                
                                // Reset the interstitial count
                                preferenceManager.resetInterstitialCount();
                                
                                // Update the last shown time
                                preferenceManager.setLastInterstitialShownTime(System.currentTimeMillis());
                                
                                // Preload a new ad
                                preloadInterstitialAd();
                                
                                if (callback != null) {
                                    callback.onAdDismissed();
                                }
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                                
                                // Preload a new ad
                                preloadInterstitialAd();
                                
                                if (callback != null) {
                                    callback.onAdDismissed();
                                }
                            }
                            
                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Interstitial ad showed successfully");
                                
                                if (callback != null) {
                                    callback.onAdShown();
                                }
                            }
                        });
                        
                        // Show the ad
                        ad.show(activity);
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        
                        if (callback != null) {
                            callback.onAdDismissed();
                        }
                    }
                });
            
            return true;
        }
        
        Log.d(TAG, "Showing interstitial ad from cache");
        
        // Set up full screen content callback
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                
                // Reset the interstitial count
                preferenceManager.resetInterstitialCount();
                
                // Update the last shown time
                preferenceManager.setLastInterstitialShownTime(System.currentTimeMillis());
                
                // Preload a new ad
                preloadInterstitialAd();
                
                if (callback != null) {
                    callback.onAdDismissed();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                
                // Preload a new ad
                preloadInterstitialAd();
                
                if (callback != null) {
                    callback.onAdDismissed();
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed successfully");
                
                if (callback != null) {
                    callback.onAdShown();
                }
            }
        });
        
        // Show the ad
        interstitialAd.show(activity);
        
        return true;
    }
    
    /**
     * Preloads a rewarded ad and adds it to the cache.
     */
    public void preloadRewardedAd() {
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User has ad-free experience, not preloading rewarded ad");
            return;
        }
        
        if (rewardedAdLoading.getValue() != null && rewardedAdLoading.getValue()) {
            Log.d(TAG, "Rewarded ad is already loading");
            return;
        }
        
        if (cacheManager.getRewardedCacheSize() >= AdConstants.MAX_REWARDED_CACHE_SIZE) {
            Log.d(TAG, "Rewarded ad cache is full");
            return;
        }
        
        Log.d(TAG, "Preloading rewarded ad");
        rewardedAdLoading.setValue(true);
        
        // Set up timeout
        rewardedTimeoutRunnable = () -> {
            Log.w(TAG, "Rewarded ad load timed out");
            rewardedAdLoading.postValue(false);
        };
        mainHandler.postDelayed(rewardedTimeoutRunnable, AdConstants.AD_LOAD_TIMEOUT);
        
        // Load the rewarded ad
        RewardedAd.load(context, AdConstants.getRewardedAdUnitId(), createAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mainHandler.removeCallbacks(rewardedTimeoutRunnable);
                        Log.d(TAG, "Rewarded ad loaded successfully");
                        
                        // Add the ad to the cache
                        cacheManager.cacheRewardedAd(rewardedAd);
                        
                        rewardedAdLoading.setValue(false);
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mainHandler.removeCallbacks(rewardedTimeoutRunnable);
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        
                        rewardedAdLoading.setValue(false);
                        
                        // Retry after a delay
                        mainHandler.postDelayed(() -> preloadRewardedAd(), 60000); // Retry after 1 minute
                    }
                });
    }
    
    /**
     * Shows a rewarded ad if one is available.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showRewardedAd(Activity activity, RewardedAdCallback callback) {
        // Get a rewarded ad from the cache
        RewardedAd rewardedAd = cacheManager.getRewardedAd();
        if (rewardedAd == null) {
            Log.d(TAG, "No rewarded ad available in cache");
            
            // Preload a new ad for next time
            preloadRewardedAd();
            
            if (callback != null) {
                callback.onAdFailedToShow();
            }
            return false;
        }
        
        Log.d(TAG, "Showing rewarded ad");
        
        // Set up full screen content callback
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed");
                
                // Preload a new ad
                preloadRewardedAd();
                
                if (callback != null) {
                    callback.onAdDismissed();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                
                // Preload a new ad
                preloadRewardedAd();
                
                if (callback != null) {
                    callback.onAdFailedToShow();
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed successfully");
                
                if (callback != null) {
                    callback.onAdShown();
                }
            }
        });
        
        // Show the ad
        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                
                // Set the rewarded ad earned status
                preferenceManager.setRewardedAdEarned(true, 24 * 60 * 60 * 1000); // 24 hours
                
                if (callback != null) {
                    callback.onUserEarnedReward(rewardItem.getAmount(), rewardItem.getType());
                }
            }
        });
        
        return true;
    }
    
    /**
     * Gets the LiveData for interstitial ad loading status.
     * @return the LiveData
     */
    public LiveData<Boolean> getInterstitialAdLoading() {
        return interstitialAdLoading;
    }
    
    /**
     * Gets the LiveData for rewarded ad loading status.
     * @return the LiveData
     */
    public LiveData<Boolean> getRewardedAdLoading() {
        return rewardedAdLoading;
    }
    
    /**
     * Checks if the user has an ad-free experience.
     * @return true if the user has an ad-free experience, false otherwise
     */
    public boolean isAdFree() {
        return preferenceManager.isAdFree();
    }
    
    /**
     * Sets the ad-free status for the user.
     * @param adFree true if the user should have an ad-free experience, false otherwise
     */
    public void setAdFree(boolean adFree) {
        preferenceManager.setAdFree(adFree);
    }
    
    /**
     * Checks if the user has earned a reward from a rewarded ad.
     * @return true if the user has earned a reward, false otherwise
     */
    public boolean hasRewardedAdEarned() {
        return preferenceManager.hasRewardedAdEarned();
    }
    
    /**
     * Gets the expiry time of the rewarded ad benefit.
     * @return the expiry timestamp in milliseconds
     */
    public long getRewardedAdExpiry() {
        return preferenceManager.getRewardedAdExpiry();
    }
    
    /**
     * Cleans up resources when the app is being destroyed.
     */
    public void cleanup() {
        executor.shutdown();
        cacheManager.clearCache();
    }
    
    /**
     * Callback interface for interstitial ad events.
     */
    public interface InterstitialAdCallback {
        /**
         * Called when the ad is shown.
         */
        void onAdShown();
        
        /**
         * Called when the ad is dismissed.
         */
        void onAdDismissed();
    }
    
    /**
     * Callback interface for rewarded ad events.
     */
    public interface RewardedAdCallback {
        /**
         * Called when the ad is shown.
         */
        void onAdShown();
        
        /**
         * Called when the ad is dismissed.
         */
        void onAdDismissed();
        
        /**
         * Called when the ad fails to show.
         */
        void onAdFailedToShow();
        
        /**
         * Called when the user earns a reward.
         * @param amount the amount of the reward
         * @param type the type of the reward
         */
        void onUserEarnedReward(int amount, String type);
    }
    
    /**
     * Load an app open ad
     */
    public void loadAppOpenAd() {
        // Check if user is ad-free
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User is ad-free, skipping app open ad loading");
            return;
        }
        
        // Check if ad is already loaded and not expired
        if (isAppOpenAdAvailable()) {
            Log.d(TAG, "App open ad already loaded and not expired");
            appOpenAdLoaded.setValue(true);
            return;
        }
        
        // Load a new ad
        Log.d(TAG, "Loading app open ad with ad unit ID: " + AdConstants.getAppOpenAdUnitId());
        
        try {
            AppOpenAd.load(
                    context,
                    AdConstants.getAppOpenAdUnitId(),
                    createAdRequest(),
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(TAG, "App open ad loaded successfully");
                            appOpenAd = ad;
                            appOpenAdLoadTime = (new Date()).getTime();
                            appOpenAdLoaded.setValue(true);
                        }
                        
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage() + 
                                    ", error code: " + loadAdError.getCode() + 
                                    ", domain: " + loadAdError.getDomain());
                            appOpenAd = null;
                            appOpenAdLoaded.setValue(false);
                            
                            // Retry after a delay
                            mainHandler.postDelayed(() -> loadAppOpenAd(), 60000); // Retry after 1 minute
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception while loading app open ad: " + e.getMessage());
            appOpenAdLoaded.setValue(false);
        }
    }
    
    /**
     * Check if app open ad is available and not expired
     */
    private boolean isAppOpenAdAvailable() {
        if (appOpenAd == null) {
            return false;
        }
        
        // Check if ad has expired (4 hours)
        long currentTime = (new Date()).getTime();
        long adExpireTime = TimeUnit.HOURS.toMillis(4);
        return (currentTime - appOpenAdLoadTime) < adExpireTime;
    }
    
    /**
     * Show an app open ad
     */
    public void showAppOpenAd(Activity activity, FullScreenContentCallback callback) {
        // Check if user is ad-free
        if (preferenceManager.isAdFree()) {
            Log.d(TAG, "User is ad-free, skipping app open ad display");
            if (callback != null) {
                callback.onAdDismissedFullScreenContent();
            }
            return;
        }
        
        // Check if ad is loaded and not expired
        if (!isAppOpenAdAvailable()) {
            Log.d(TAG, "App open ad not loaded or expired, loading now");
            loadAppOpenAd();
            if (callback != null) {
                callback.onAdFailedToShowFullScreenContent(
                        new AdError(0, "App open ad not available", "com.ds.eventwish"));
            }
            return;
        }
        
        // Set callback
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                appOpenAd = null;
                appOpenAdLoaded.setValue(false);
                
                // Load a new ad for next time
                loadAppOpenAd();
                
                // Forward callback
                if (callback != null) {
                    callback.onAdDismissedFullScreenContent();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                appOpenAd = null;
                appOpenAdLoaded.setValue(false);
                
                // Load a new ad for next time
                loadAppOpenAd();
                
                // Forward callback
                if (callback != null) {
                    callback.onAdFailedToShowFullScreenContent(adError);
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed successfully");
                
                // Increment ad display count
                preferenceManager.incrementAdDisplayCount();
                
                // Forward callback
                if (callback != null) {
                    callback.onAdShowedFullScreenContent();
                }
            }
        });
        
        // Show the ad
        Log.d(TAG, "Showing app open ad");
        try {
            appOpenAd.show(activity);
        } catch (Exception e) {
            Log.e(TAG, "Exception while showing app open ad: " + e.getMessage());
            appOpenAd = null;
            appOpenAdLoaded.setValue(false);
            loadAppOpenAd();
            
            if (callback != null) {
                callback.onAdFailedToShowFullScreenContent(
                        new AdError(0, "Exception showing app open ad: " + e.getMessage(), "com.ds.eventwish"));
            }
        }
    }
    
    /**
     * Get LiveData for app open ad loaded status
     */
    public LiveData<Boolean> getAppOpenAdLoaded() {
        return appOpenAdLoaded;
    }
    
    /**
     * Get LiveData for native ad loaded status
     */
    public LiveData<Boolean> getNativeAdLoaded(String adUnitId) {
        if (!nativeAdLoadedMap.containsKey(adUnitId)) {
            nativeAdLoadedMap.put(adUnitId, new MutableLiveData<>(false));
        }
        return nativeAdLoadedMap.get(adUnitId);
    }
    
    /**
     * Get AdPreferenceManager
     */
    public AdPreferenceManager getAdPreferenceManager() {
        return preferenceManager;
    }
    
    /**
     * Callback interface for native ad loading
     */
    public interface NativeAdLoadCallback {
        void onNativeAdLoaded(NativeAd nativeAd);
        void onNativeAdFailedToLoad(LoadAdError loadAdError);
    }
    
    /**
     * Loads a native video ad.
     * @param adUnitId the ad unit ID to load
     * @param callback the callback to be notified when the ad is loaded or fails to load
     */
    public void loadNativeVideoAd(String adUnitId, NativeAdLoadCallback callback) {
        if (isAdFree()) {
            if (callback != null) {
                callback.onNativeAdFailedToLoad(new LoadAdError(0, "Ad-free mode enabled", "", null, null));
            }
            return;
        }
        
        Log.d(TAG, "Loading native video ad: " + adUnitId);
        
        // Set native ad options
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(new com.google.android.gms.ads.VideoOptions.Builder()
                        .setStartMuted(true)
                        .build())
                .build();
        
        // Create ad loader
        AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(nativeAd -> {
                    // Cache the native ad
                    nativeAds.put(adUnitId, nativeAd);
                    
                    // Update loading state
                    if (nativeAdLoadedMap.containsKey(adUnitId)) {
                        nativeAdLoadedMap.get(adUnitId).postValue(true);
                    }
                    
                    // Notify callback
                    if (callback != null) {
                        callback.onNativeAdLoaded(nativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Native video ad failed to load: " + loadAdError.getMessage());
                        
                        // Update loading state
                        if (nativeAdLoadedMap.containsKey(adUnitId)) {
                            nativeAdLoadedMap.get(adUnitId).postValue(false);
                        }
                        
                        // Notify callback
                        if (callback != null) {
                            callback.onNativeAdFailedToLoad(loadAdError);
                        }
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        
        // Load the ad
        adLoader.loadAd(createAdRequest());
    }
} 