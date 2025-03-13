package com.ds.eventwish.ui.ads;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.lifecycle.LifecycleOwner;

import com.ds.eventwish.data.ads.AdManagerExtended;

/**
 * Helper class for interstitial ads
 */
public class InterstitialAdHelper {
    private static final String TAG = "InterstitialAdHelper";
    
    private final Activity activity;
    private final AdViewModel adViewModel;
    private final LifecycleOwner lifecycleOwner;
    
    /**
     * Constructor
     * @param activity The activity
     * @param adViewModel The ad view model
     * @param lifecycleOwner The lifecycle owner
     */
    public InterstitialAdHelper(Activity activity, AdViewModel adViewModel, LifecycleOwner lifecycleOwner) {
        this.activity = activity;
        this.adViewModel = adViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }
    
    /**
     * Show an interstitial ad
     * @param onAdDismissed Callback to be invoked when the ad is dismissed
     */
    public void showInterstitialAd(Runnable onAdDismissed) {
        Log.d(TAG, "Showing interstitial ad");
        
        // Check if ad is still loading
        Observer<Boolean> adLoadingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading != null && !isLoading) {
                    // Ad is loaded, show it
                    // Remove the observer to prevent multiple callbacks
                    adViewModel.getInterstitialAdLoading().removeObserver(this);
                    
                    adViewModel.showInterstitialAd(activity, new AdManagerExtended.InterstitialAdCallback() {
                        @Override
                        public void onInterstitialAdLoaded(com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                            if (onAdDismissed != null) {
                                onAdDismissed.run();
                            }
                        }
                        
                        @Override
                        public void onInterstitialAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                            Log.e(TAG, "Interstitial ad failed to load");
                            if (onAdDismissed != null) {
                                onAdDismissed.run();
                            }
                        }
                    });
                } else {
                    // Ad is still loading, wait for it
                    Log.d(TAG, "Interstitial ad is still loading");
                }
            }
        };
        
        adViewModel.getInterstitialAdLoading().observe(lifecycleOwner, adLoadingObserver);
    }
} 