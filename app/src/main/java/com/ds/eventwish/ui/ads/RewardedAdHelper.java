package com.ds.eventwish.ui.ads;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.lifecycle.LifecycleOwner;

import com.ds.eventwish.data.ads.AdManagerExtended;

/**
 * Helper class for rewarded ads
 */
public class RewardedAdHelper {
    private static final String TAG = "RewardedAdHelper";
    
    private final Activity activity;
    private final AdViewModel adViewModel;
    private final LifecycleOwner lifecycleOwner;
    
    /**
     * Constructor
     * @param activity The activity
     * @param adViewModel The ad view model
     * @param lifecycleOwner The lifecycle owner
     */
    public RewardedAdHelper(Activity activity, AdViewModel adViewModel, LifecycleOwner lifecycleOwner) {
        this.activity = activity;
        this.adViewModel = adViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }
    
    /**
     * Show a rewarded ad
     * @param rewardCallback Callback to be invoked when the user earns a reward
     */
    public void showRewardedAd(RewardCallback rewardCallback) {
        Log.d(TAG, "Showing rewarded ad");
        
        // Check if ad is still loading
        Observer<Boolean> adLoadingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading != null && !isLoading) {
                    // Ad is loaded, show it
                    // Remove the observer to prevent multiple callbacks
                    adViewModel.getRewardedAdLoading().removeObserver(this);
                    
                    adViewModel.showRewardedAd(activity, new AdManagerExtended.RewardedAdCallback() {
                        @Override
                        public void onRewardedAdLoaded(com.google.android.gms.ads.rewarded.RewardedAd rewardedAd) {
                            Log.d(TAG, "Rewarded ad loaded");
                        }
                        
                        @Override
                        public void onRewardedAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                            Log.e(TAG, "Rewarded ad failed to load");
                        }
                        
                        @Override
                        public void onRewardEarned(int amount, String type) {
                            Log.d(TAG, "User earned reward: " + amount + " " + type);
                            if (rewardCallback != null) {
                                rewardCallback.onReward(amount, type);
                            }
                        }
                    });
                } else {
                    // Ad is still loading, wait for it
                    Log.d(TAG, "Rewarded ad is still loading");
                }
            }
        };
        
        adViewModel.getRewardedAdLoading().observe(lifecycleOwner, adLoadingObserver);
    }
    
    /**
     * Checks if the user has earned a reward from a rewarded ad.
     * @return true if the user has earned a reward, false otherwise
     */
    public boolean hasRewardedAdEarned() {
        return adViewModel.hasRewardedAdEarned();
    }
    
    /**
     * Gets the expiry time of the rewarded ad benefit.
     * @return the expiry timestamp in milliseconds
     */
    public long getRewardedAdExpiry() {
        return adViewModel.getRewardedAdExpiry();
    }
    
    /**
     * Callback for rewarded ads
     */
    public interface RewardCallback {
        /**
         * Called when the user earns a reward
         * @param amount The amount of the reward
         * @param type The type of the reward
         */
        void onReward(int amount, String type);
    }
} 