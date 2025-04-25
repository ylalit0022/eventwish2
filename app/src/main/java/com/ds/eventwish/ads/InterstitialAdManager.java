package com.ds.eventwish.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import javax.net.ssl.SSLHandshakeException;

/**
 * Manager class for handling interstitial ads
 */
public class InterstitialAdManager {
    private static final String TAG = "InterstitialAdManager";
    private static final String AD_TYPE = "Interstitial"; // Match server enum case
    
    private final Context context;
    private final AdMobRepository adMobRepository;
    private InterstitialAd interstitialAd;
    private boolean isLoading = false;
    private int retryAttempts = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private InterstitialAdCallback callback;
    
    public interface InterstitialAdCallback {
        void onAdLoaded();
        void onAdFailedToLoad(String error);
        void onAdShown();
        void onAdDismissed();
        void onAdClicked();
        void onAdShowFailed(String error);
    }
    
    public InterstitialAdManager(Context context) {
        this.context = context.getApplicationContext();
        ApiService apiService = ApiClient.getClient();
        this.adMobRepository = new AdMobRepository(context.getApplicationContext(), apiService);
    }
    
    /**
     * Loads an interstitial ad.
     */
    public void loadAd(InterstitialAdCallback callback) {
        if (isLoading) {
            Log.d(TAG, "Ad loading already in progress");
            return;
        }
        
        this.callback = callback;
        isLoading = true;
        Log.d(TAG, "Fetching interstitial ad unit from server...");
        
        adMobRepository.fetchAdUnit(AD_TYPE, new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                if (!adUnit.isStatus()) {
                    Log.d(TAG, "Interstitial ad unit is disabled on server");
                    isLoading = false;
                    retryAttempts = 0;
                    return;
                }
                
                Log.d(TAG, "Loading interstitial ad with unit ID: " + adUnit.getAdUnitCode());
                AdRequest adRequest = new AdRequest.Builder().build();
                InterstitialAd.load(context,
                    adUnit.getAdUnitCode(),
                    adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            Log.d(TAG, "Interstitial ad loaded successfully");
                            interstitialAd = ad;
                            isLoading = false;
                            retryAttempts = 0;
                            setupInterstitialCallbacks();
                            if (callback != null) {
                                callback.onAdLoaded();
                            }
                        }
                        
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            String errorMessage = context.getString(R.string.error_unknown);
                            Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                            interstitialAd = null;
                            isLoading = false;
                            
                            if (callback != null) {
                                callback.onAdFailedToLoad(errorMessage);
                            }
                            
                            // Retry logic for ad loading failures
                            if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                                retryAttempts++;
                                Log.d(TAG, "Retrying interstitial ad load (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                                loadAd(callback);
                            } else {
                                Log.e(TAG, "Max retry attempts reached for interstitial ad");
                                retryAttempts = 0;
                            }
                        }
                    });
            }
            
            @Override
            public void onError(String error) {
                String errorMessage;
                if (error.contains("SSLHandshakeException")) {
                    errorMessage = context.getString(R.string.error_ssl);
                } else {
                    errorMessage = context.getString(R.string.error_unknown);
                }
                Log.e(TAG, "Failed to fetch interstitial ad unit: " + errorMessage);
                isLoading = false;
                
                // Retry logic for server errors
                if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                    retryAttempts++;
                    Log.d(TAG, "Retrying server fetch (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                    loadAd(callback);
                } else {
                    Log.e(TAG, "Max retry attempts reached for server fetch");
                    retryAttempts = 0;
                }
            }
        });
    }
    
    private void setupInterstitialCallbacks() {
        if (interstitialAd == null) return;
        
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                interstitialAd = null;
                if (callback != null) {
                    callback.onAdDismissed();
                }
                // Load the next interstitial ad
                loadAd(callback);
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                interstitialAd = null;
                if (callback != null) {
                    callback.onAdShowFailed(adError.getMessage());
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed successfully");
                if (callback != null) {
                    callback.onAdShown();
                }
            }

            @Override
            public void onAdClicked() {
                Log.d(TAG, "Interstitial ad clicked");
                if (callback != null) {
                    callback.onAdClicked();
                }
            }
        });
    }
    
    /**
     * Shows the loaded interstitial ad.
     *
     * @param activity The activity context to show the ad
     * @return true if ad was shown, false otherwise
     */
    public boolean showAd(Activity activity, InterstitialAdCallback callback) {
        this.callback = callback;
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            return true;
        } else {
            Log.d(TAG, "Interstitial ad not ready yet");
            loadAd(callback); // Try to load next ad
            return false;
        }
    }
    
    /**
     * Checks if an interstitial ad is loaded and ready to show.
     *
     * @return true if ad is ready, false otherwise
     */
    public boolean isAdLoaded() {
        return interstitialAd != null;
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        interstitialAd = null;
        isLoading = false;
        callback = null;
    }
} 