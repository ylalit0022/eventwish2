package com.ds.eventwish.ui.ads;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.ads.AdManagerExtended;
import com.ds.eventwish.data.repository.AdRepository;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;

/**
 * ViewModel for ad-related operations.
 * Follows the MVVM pattern used throughout the app.
 */
public class AdViewModel extends AndroidViewModel {
    private static final String TAG = "AdViewModel";
    
    private final AdRepository adRepository;
    private final AdManagerExtended adManagerExtended;
    
    // LiveData for ad loading states
    private final MutableLiveData<Boolean> interstitialAdLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> rewardedAdLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> appOpenAdLoaded = new MutableLiveData<>(false);
    
    // Ad instances
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    
    // Rewarded ad state
    private boolean rewardedAdEarned = false;
    private long rewardedAdExpiry = 0;
    
    /**
     * Constructor for AdViewModel.
     * @param application the application
     */
    public AdViewModel(@NonNull Application application) {
        super(application);
        adRepository = new AdRepository(application);
        adManagerExtended = AdManagerExtended.getInstance(application);
        
        // Observe app open ad loading state
        adManagerExtended.getAppOpenAdLoadedLiveData().observeForever(isLoaded -> {
            appOpenAdLoaded.setValue(isLoaded);
            Log.d(TAG, "App open ad loaded state changed: " + isLoaded);
        });
    }
    
    /**
     * Creates and loads a banner ad.
     * @param adSize the size of the banner ad
     * @return the AdView
     */
    public AdView createBannerAd(AdSize adSize) {
        return adRepository.createBannerAd(adSize);
    }
    
    /**
     * Adds a banner ad to the specified container.
     * @param container the container to add the banner ad to
     * @param adSize the size of the banner ad
     * @return the AdView that was added, or null if the user has an ad-free experience
     */
    public AdView addBannerToContainer(ViewGroup container, AdSize adSize) {
        return adRepository.addBannerToContainer(container, adSize);
    }
    
    /**
     * Preloads an interstitial ad and adds it to the cache.
     */
    public void preloadInterstitialAd() {
        interstitialAdLoading.setValue(true);
        
        adManagerExtended.loadInterstitialAd(new AdManagerExtended.InterstitialAdCallback() {
            @Override
            public void onInterstitialAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                interstitialAdLoading.setValue(false);
                Log.d(TAG, "Interstitial ad loaded successfully");
            }
            
            @Override
            public void onInterstitialAdFailedToLoad(LoadAdError loadAdError) {
                interstitialAd = null;
                interstitialAdLoading.setValue(false);
                Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
            }
        });
    }
    
    /**
     * Shows an interstitial ad if one is available and conditions are met.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showInterstitialAd(Activity activity, AdManagerExtended.InterstitialAdCallback callback) {
        if (interstitialAd == null) {
            Log.d(TAG, "Interstitial ad not loaded yet");
            preloadInterstitialAd();
            if (callback != null) {
                callback.onInterstitialAdFailedToLoad(null);
            }
            return false;
        }
        
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                interstitialAd = null;
                preloadInterstitialAd();
                if (callback != null) {
                    // Don't call onInterstitialAdLoaded here as it triggers another ad
                    // Just notify that the ad was dismissed
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                interstitialAd = null;
                preloadInterstitialAd();
                if (callback != null) {
                    callback.onInterstitialAdFailedToLoad(new LoadAdError(
                            adError.getCode(),
                            adError.getMessage(),
                            adError.getDomain(),
                            null,
                            null));
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed successfully");
            }
        });
        
        interstitialAd.show(activity);
        return true;
    }
    
    /**
     * Preloads a rewarded ad and adds it to the cache.
     */
    public void preloadRewardedAd() {
        rewardedAdLoading.setValue(true);
        
        adManagerExtended.loadRewardedAd(new AdManagerExtended.RewardedAdCallback() {
            @Override
            public void onRewardedAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                rewardedAdLoading.setValue(false);
                Log.d(TAG, "Rewarded ad loaded successfully");
            }
            
            @Override
            public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
                rewardedAd = null;
                rewardedAdLoading.setValue(false);
                Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
            }
        });
    }
    
    /**
     * Shows a rewarded ad if one is available.
     * @param activity the activity to show the ad in
     * @param callback callback for ad events
     * @return true if an ad was shown, false otherwise
     */
    public boolean showRewardedAd(Activity activity, AdManagerExtended.RewardedAdCallback callback) {
        if (rewardedAd == null) {
            Log.d(TAG, "Rewarded ad not loaded yet");
            preloadRewardedAd();
            if (callback != null) {
                callback.onRewardedAdFailedToLoad(null);
            }
            return false;
        }
        
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed");
                rewardedAd = null;
                preloadRewardedAd(); // This is fine - preload for next time
                if (callback != null) {
                    // Don't call onRewardedAdLoaded here as it might trigger another ad
                    // Just notify that the ad was dismissed
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                preloadRewardedAd();
                if (callback != null) {
                    callback.onRewardedAdFailedToLoad(new LoadAdError(
                            adError.getCode(),
                            adError.getMessage(),
                            adError.getDomain(),
                            null,
                            null));
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed successfully");
            }
        });
        
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
            
            // Set reward earned state
            rewardedAdEarned = true;
            // Set expiry time (e.g., 24 hours from now)
            rewardedAdExpiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
            
            if (callback != null) {
                callback.onRewardEarned(rewardItem.getAmount(), rewardItem.getType());
            }
        });
        
        return true;
    }
    
    /**
     * Gets the LiveData for interstitial ad loading state.
     * @return the LiveData for interstitial ad loading state
     */
    public LiveData<Boolean> getInterstitialAdLoading() {
        return interstitialAdLoading;
    }
    
    /**
     * Gets the LiveData for rewarded ad loading state.
     * @return the LiveData for rewarded ad loading state
     */
    public LiveData<Boolean> getRewardedAdLoading() {
        return rewardedAdLoading;
    }
    
    /**
     * Gets the LiveData for app open ad loaded state.
     * @return the LiveData for app open ad loaded state
     */
    public LiveData<Boolean> getAppOpenAdLoaded() {
        return appOpenAdLoaded;
    }
    
    /**
     * Checks if the user has earned a reward from a rewarded ad.
     * @return true if the user has earned a reward, false otherwise
     */
    public boolean hasRewardedAdEarned() {
        // Check if the reward has expired
        if (rewardedAdEarned && System.currentTimeMillis() > rewardedAdExpiry) {
            rewardedAdEarned = false;
        }
        return rewardedAdEarned;
    }
    
    /**
     * Gets the expiry time of the rewarded ad benefit.
     * @return the expiry timestamp in milliseconds
     */
    public long getRewardedAdExpiry() {
        return rewardedAdExpiry;
    }
    
    /**
     * Checks if the user has an ad-free experience.
     * @return true if the user has an ad-free experience, false otherwise
     */
    public boolean isAdFree() {
        return adRepository.isAdFree();
    }
    
    /**
     * Sets the ad-free status for the user.
     * @param adFree true if the user should have an ad-free experience, false otherwise
     */
    public void setAdFree(boolean adFree) {
        adRepository.setAdFree(adFree);
    }
    
    /**
     * Cleans up resources when the ViewModel is cleared.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        adRepository.cleanup();
        interstitialAd = null;
        rewardedAd = null;
    }
} 