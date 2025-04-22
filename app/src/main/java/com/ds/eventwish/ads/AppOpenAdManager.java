package com.ds.eventwish.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.repository.AdMobRepository;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

/**
 * App Open Ad Manager for handling app open ads
 * Simplified implementation focused on core functionality
 */
public class AppOpenAdManager implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AppOpenAdManager";
    
    // Ad request timeout - ads older than this are considered expired
    private static final long AD_EXPIRATION_TIME_MILLIS = 3600000; // 1 hour
    
    private static AppOpenAdManager instance;
    private final AdMobRepository adMobRepository;
    
    // Live data for monitoring the ad state
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isShowingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    
    // The actual App Open Ad object
    private AppOpenAd appOpenAd = null;
    
    // Variables to track ad state
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long loadTime = 0;
    private String adUnitId = "";
    
    // Current activity - used for showing the ad
    private Activity currentActivity;
    
    /**
     * Private constructor
     */
    private AppOpenAdManager(Context context) {
        this.adMobRepository = AdMobRepository.getInstance(context);
    }
    
    /**
     * Initialize the AppOpenAdManager singleton
     * @param application Application instance
     */
    public static synchronized void init(@NonNull Application application) {
        if (instance == null) {
            instance = new AppOpenAdManager(application);
            
            // Register activity lifecycle callbacks
            application.registerActivityLifecycleCallbacks(instance);
            
            Log.d(TAG, "AppOpenAdManager initialized");
        }
    }
    
    /**
     * Get singleton instance
     * @return AppOpenAdManager instance
     */
    public static synchronized AppOpenAdManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppOpenAdManager must be initialized first. Call init(Application)");
        }
        return instance;
    }
    
    /**
     * Set the ad unit ID for App Open Ads
     * @param adUnitId The ad unit ID to use
     */
    public void setAdUnitId(String adUnitId) {
        Log.d(TAG, "Setting App Open ad unit ID: " + adUnitId);
        this.adUnitId = adUnitId;
    }
    
    /**
     * Load an App Open Ad
     */
    public void loadAd() {
        // Do not load ad if already loading or there's no ad unit ID
        if (isLoadingAd || adUnitId.isEmpty() || currentActivity == null) {
            Log.d(TAG, "Not loading ad: " + 
                 (isLoadingAd ? "Already loading" : 
                  adUnitId.isEmpty() ? "No ad unit ID set" : 
                  "No activity available"));
            return;
        }
        
        isLoadingAd = true;
        isLoadingLiveData.setValue(true);
        
        Log.d(TAG, "Starting to load app open ad with ID: " + adUnitId);
        
        AdRequest request = new AdRequest.Builder().build();
        
        // Load the ad asynchronously
        AppOpenAd.load(
            currentActivity,
            adUnitId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    Log.d(TAG, "App open ad loaded successfully");
                    appOpenAd = ad;
                    isLoadingAd = false;
                    isLoadingLiveData.setValue(false);
                    loadTime = (new Date()).getTime();
                    
                    // Set up full screen content callback
                    setupFullScreenContentCallback();
                }
                
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage());
                    
                    isLoadingAd = false;
                    isLoadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to load app open ad: " + loadAdError.getMessage());
                }
            });
    }
    
    /**
     * Set up full screen content callback for the ad
     */
    private void setupFullScreenContentCallback() {
        if (appOpenAd == null) return;
        
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed");
                
                // Reset state
                appOpenAd = null;
                isShowingAd = false;
                isShowingLiveData.setValue(false);
                
                // Load the next ad
                loadAd();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                
                // Reset state
                appOpenAd = null;
                isShowingAd = false;
                isShowingLiveData.setValue(false);
                errorLiveData.setValue("Failed to show app open ad: " + adError.getMessage());
                
                // Load the next ad
                loadAd();
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed fullscreen content");
                isShowingAd = true;
                isShowingLiveData.setValue(true);
                
                // Track impression if we have an ad unit ID
                if (!adUnitId.isEmpty()) {
                    AdMobManager.getInstance().trackImpression(adUnitId);
                }
            }
        });
    }
    
    /**
     * Check if an ad is available to show
     * @return True if an ad is available and not expired, false otherwise
     */
    private boolean isAdAvailable() {
        return appOpenAd != null && !isAdExpired();
    }
    
    /**
     * Check if the loaded ad is expired
     * @return True if ad is expired, false otherwise
     */
    private boolean isAdExpired() {
        long currentTime = (new Date()).getTime();
        return (currentTime - loadTime) > AD_EXPIRATION_TIME_MILLIS;
    }
    
    /**
     * Show the ad if one is available
     * @return True if ad was shown, false otherwise
     */
    public boolean showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable() && currentActivity != null) {
            Log.d(TAG, "Showing app open ad");
            appOpenAd.show(currentActivity);
            return true;
        } else {
            if (isShowingAd) {
                Log.d(TAG, "Not showing ad: Already showing an ad");
            } else if (!isAdAvailable()) {
                Log.d(TAG, "Not showing ad: No ad available");
                loadAd();
            } else if (currentActivity == null) {
                Log.d(TAG, "Not showing ad: No activity available");
            }
            return false;
        }
    }
    
    /**
     * Force show an App Open Ad (for testing)
     * Loads a new ad if none is available
     */
    public void forceShowAd(Activity activity) {
        currentActivity = activity;
        
        // If we have an ad, show it
        if (isAdAvailable()) {
            Log.d(TAG, "Force showing app open ad");
            appOpenAd.show(activity);
        } else {
            // Otherwise load a new one
            Log.d(TAG, "No ad available for force show, loading new ad");
            loadAd();
        }
    }
    
    /**
     * Check if ad is ready to show
     */
    public boolean isAdReadyToShow() {
        return !isShowingAd && isAdAvailable();
    }
    
    // =========================================================================
    // Activity Lifecycle Callbacks
    // =========================================================================
    
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // Not used
    }
    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Not used
    }
    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }
    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Not used
    }
    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // Not used
    }
    
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // Not used
    }
    
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
    
    /**
     * Get the load state live data
     * @return MutableLiveData containing loading state
     */
    public MutableLiveData<Boolean> getLoadingState() {
        return isLoadingLiveData;
    }
    
    /**
     * Get the show state live data
     * @return MutableLiveData containing showing state
     */
    public MutableLiveData<Boolean> getShowingState() {
        return isShowingLiveData;
    }
    
    /**
     * Get the error live data
     * @return MutableLiveData containing error messages
     */
    public MutableLiveData<String> getError() {
        return errorLiveData;
    }
} 