package com.ds.eventwish.ui.viewmodel;

import android.app.Activity;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.repository.AdMobRepository;
import com.ds.eventwish.data.repository.CoinsRepository;
import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.util.List;
import android.util.Log;

/**
 * ViewModel for managing ad-related data and operations
 */
public class AdViewModel extends AndroidViewModel {
    private static final String TAG = "AdViewModel";
    private final AdMobRepository adMobRepository;
    private final CoinsRepository coinsRepository;
    private final MutableLiveData<Boolean> isRewardedAdLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRewardedAdReady = new MutableLiveData<>(false);
    private final MutableLiveData<String> adError = new MutableLiveData<>("");
    private RewardedAd rewardedAd;
    private static final int REWARD_COINS = 10; // Number of coins for watching a rewarded ad

    public AdViewModel(Application application) {
        super(application);
        adMobRepository = AdMobRepository.getInstance(application);
        coinsRepository = CoinsRepository.getInstance(application);
    }
    
    /**
     * Get LiveData for tracking if a rewarded ad is currently loading
     */
    public LiveData<Boolean> getIsRewardedAdLoading() {
        return isRewardedAdLoading;
    }
    
    /**
     * Get LiveData for tracking if a rewarded ad is ready to show
     */
    public LiveData<Boolean> getIsRewardedAdReady() {
        return isRewardedAdReady;
    }
    
    /**
     * Get LiveData for tracking ad errors
     */
    public LiveData<String> getAdError() {
        return adError;
    }
    
    /**
     * Load a rewarded ad
     */
    public void loadRewardedAd() {
        if (isRewardedAdLoading.getValue() == Boolean.TRUE) {
            Log.d(TAG, "Ad already loading, skipping request");
            return;
        }
        
        isRewardedAdLoading.setValue(true);
        isRewardedAdReady.setValue(false);
        adError.setValue("");
        
        // Get the active rewarded ad unit ID
        adMobRepository.getActiveRewardedAdUnitId(new AdMobRepository.AdUnitCallback() {
            @Override
            public void onAdUnitReceived(String adUnitId) {
                if (adUnitId == null || adUnitId.isEmpty()) {
                    Log.e(TAG, "No valid rewarded ad unit ID available");
                    isRewardedAdLoading.postValue(false);
                    adError.postValue("Ad configuration error");
                    return;
                }
                
                Log.d(TAG, "Loading rewarded ad with unit ID: " + adUnitId);
                
                try {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    RewardedAd.load(getApplication(), adUnitId, adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                            Log.e(TAG, "Failed to load rewarded ad: " + loadAdError.getMessage());
                            isRewardedAdLoading.postValue(false);
                            isRewardedAdReady.postValue(false);
                            adError.postValue("Failed to load ad: " + loadAdError.getMessage());
                            rewardedAd = null;
                        }
                        
                        @Override
                        public void onAdLoaded(RewardedAd ad) {
                            Log.d(TAG, "Rewarded ad loaded successfully");
                            rewardedAd = ad;
                            isRewardedAdLoading.postValue(false);
                            isRewardedAdReady.postValue(true);
                            adError.postValue("");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading rewarded ad", e);
                    isRewardedAdLoading.postValue(false);
                    isRewardedAdReady.postValue(false);
                    adError.postValue("Error: " + e.getMessage());
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error getting rewarded ad unit ID", error);
                isRewardedAdLoading.postValue(false);
                adError.postValue("Ad configuration error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Interface for ad reward callbacks
     */
    public interface RewardCallback {
        void onRewardResult(boolean success);
    }
    
    /**
     * Show the rewarded ad to the user
     * @param activity The activity context
     * @param callback Callback to handle the reward result
     */
    public void showRewardedAd(Activity activity, RewardCallback callback) {
        if (rewardedAd == null) {
            Log.e(TAG, "Attempted to show ad but no ad is loaded");
            callback.onRewardResult(false);
            return;
        }
        
        Log.d(TAG, "Showing rewarded ad");
        
        // Track that the ad is no longer available after showing
        isRewardedAdReady.setValue(false);
        
        // Show the ad
        rewardedAd.show(activity, rewardItem -> {
            // User earned a reward
            int rewardAmount = rewardItem.getAmount();
            Log.d(TAG, "User earned reward: " + rewardAmount);
            
            // Add coins to the user's balance
            coinsRepository.addCoins(rewardAmount);
            
            // Force refresh coins LiveData to ensure UI updates
            coinsRepository.forceRefreshCoinsLiveData();
            
            // Double check that coins were actually added by logging current coins
            int currentCoins = coinsRepository.getCurrentCoins();
            Log.d(TAG, "Current coins after reward: " + currentCoins);
            
            // Add a second refresh after a short delay to ensure UI updates
            new android.os.Handler().postDelayed(() -> {
                Log.d(TAG, "Performing delayed refresh after reward");
                coinsRepository.forceRefreshCoinsLiveData();
            }, 300);
            
            // Notify callback of success
            callback.onRewardResult(true);
            
            // Preload the next ad
            loadRewardedAd();
        });
        
        // Set other callbacks
        rewardedAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed");
                // Preload the next ad
                loadRewardedAd();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                callback.onRewardResult(false);
                // Attempt to load a new ad
                loadRewardedAd();
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content");
                // Ad is shown, set to null so we don't use it again
                rewardedAd = null;
            }
        });
    }
}