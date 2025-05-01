package com.ds.eventwish.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;
import com.ds.eventwish.ads.core.AdConstants;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;

import java.util.Date;

/**
 * Manager class for handling rewarded ads
 */
public class RewardedAdManager {
    private static final String TAG = "RewardedAdManager";
    private static final String AD_TYPE = "rewarded"; // Match server enum case
    
    private final Context context;
    private final AdMobRepository adMobRepository;
    private RewardedAd rewardedAd;
    private boolean isLoading = false;
    private boolean isShowingAd = false;
    private int retryAttempts = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long COOLDOWN_PERIOD_MS = 60 * 1000; // 1 minute cooldown period
    private long lastAdDisplayTime = 0;
    
    public interface RewardedAdCallback {
        void onAdLoaded();
        void onAdFailedToLoad(String error);
        void onAdOpened();
        void onAdClosed();
        void onUserEarnedReward(String type, int amount);
        void onAdShowFailed(String error);
    }
    
    public RewardedAdManager(Context context) {
        this.context = context.getApplicationContext();
        ApiService apiService = ApiClient.getClient();
        this.adMobRepository = new AdMobRepository(context.getApplicationContext(), apiService);
    }
    
    /**
     * Loads a rewarded ad.
     */
    public void loadAd(RewardedAdCallback callback) {
        if (isLoading) {
            Log.d(TAG, "Ad loading already in progress");
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Fetching rewarded ad unit from server...");
        
        adMobRepository.fetchAdUnit(AD_TYPE, new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                if (!adUnit.isStatus()) {
                    Log.d(TAG, "Rewarded ad unit is disabled on server");
                    isLoading = false;
                    retryAttempts = 0;
                    if (callback != null) {
                        callback.onAdFailedToLoad("Rewarded ads are currently disabled");
                    }
                    return;
                }
                
                Log.d(TAG, "Loading rewarded ad with unit ID: " + adUnit.getAdUnitCode());
                AdRequest adRequest = new AdRequest.Builder().build();
                RewardedAd.load(context,
                    adUnit.getAdUnitCode(),
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            Log.d(TAG, "Rewarded ad loaded successfully");
                            rewardedAd = ad;
                            isLoading = false;
                            retryAttempts = 0;
                            
                            // Set server-side verification options if needed
                            if (adUnit.getParameters() != null && adUnit.getParameters().containsKey("userId")) {
                                ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
                                    .setUserId(adUnit.getParameters().get("userId"))
                                    .build();
                                rewardedAd.setServerSideVerificationOptions(options);
                            }
                            
                            setupRewardedCallback(callback);
                            
                            if (callback != null) {
                                callback.onAdLoaded();
                            }
                        }
                        
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Log detailed error information
                            Log.e(TAG, "=================== REWARDED AD LOAD ERROR ===================");
                            Log.e(TAG, "Error Code: " + loadAdError.getCode());
                            Log.e(TAG, "Error Domain: " + loadAdError.getDomain());
                            Log.e(TAG, "Error Message: " + loadAdError.getMessage());
                            Log.e(TAG, "Response Info: " + (loadAdError.getResponseInfo() != null ? loadAdError.getResponseInfo().toString() : "null"));
                            Log.e(TAG, "Raw Response: " + loadAdError.toString());
                            Log.e(TAG, "=============================================================");
                            
                            // Construct a detailed error message
                            String errorMessage = "Error Code: " + loadAdError.getCode() +
                                    ", Message: " + loadAdError.getMessage() +
                                    ", Domain: " + loadAdError.getDomain();
                            
                            rewardedAd = null;
                            isLoading = false;
                            
                            // Retry logic for ad loading failures
                            if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                                retryAttempts++;
                                Log.d(TAG, "Retrying rewarded ad load (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                                loadAd(callback);
                            } else {
                                retryAttempts = 0;
                                if (callback != null) {
                                    callback.onAdFailedToLoad(errorMessage);
                                }
                            }
                        }
                    });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "=================== AD UNIT FETCH ERROR ===================");
                Log.e(TAG, "Raw Error: " + error);
                // Try to identify specific error types for more helpful messages
                String errorMessage;
                if (error.contains("SSLHandshakeException")) {
                    errorMessage = "SSL Error: " + error;
                    Log.e(TAG, "SSL Handshake Error: " + error);
                } else if (error.contains("SocketTimeoutException")) {
                    errorMessage = "Timeout Error: " + error;
                    Log.e(TAG, "Socket Timeout Error: " + error);
                } else if (error.contains("ConnectException")) {
                    errorMessage = "Connection Error: " + error;
                    Log.e(TAG, "Connection Error: " + error);
                } else if (error.contains("UnknownHostException")) {
                    errorMessage = "Unknown Host Error: " + error;
                    Log.e(TAG, "Unknown Host Error: " + error);
                } else {
                    errorMessage = "Server Error: " + error;
                    Log.e(TAG, "Unknown Server Error: " + error);
                }
                Log.e(TAG, "=========================================================");
                
                isLoading = false;
                
                // Retry logic for server errors
                if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                    retryAttempts++;
                    Log.d(TAG, "Retrying server fetch (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                    loadAd(callback);
                } else {
                    retryAttempts = 0;
                    if (callback != null) {
                        callback.onAdFailedToLoad(errorMessage);
                    }
                }
            }
        });
    }
    
    private void setupRewardedCallback(RewardedAdCallback callback) {
        if (rewardedAd == null) return;
        
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed");
                rewardedAd = null;
                isShowingAd = false;
                lastAdDisplayTime = System.currentTimeMillis();
                
                if (callback != null) {
                    callback.onAdClosed();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                isShowingAd = false;
                
                if (callback != null) {
                    callback.onAdShowFailed(adError.getMessage());
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed successfully");
                isShowingAd = true;
                
                if (callback != null) {
                    callback.onAdOpened();
                }
            }
        });
    }
    
    /**
     * Shows the loaded rewarded ad.
     *
     * @param activity The activity context to show the ad
     * @param callback Callback to handle ad events
     * @return true if ad was shown, false otherwise
     */
    public boolean showAd(Activity activity, OnUserEarnedRewardListener rewardListener, RewardedAdCallback callback) {
        // Check if in cooldown period
        if (isInCooldownPeriod()) {
            Log.d(TAG, "Cannot show ad: in cooldown period");
            if (callback != null) {
                callback.onAdShowFailed("Ad in cooldown period");
            }
            return false;
        }
        
        // Check if ad is loaded
        if (rewardedAd != null && !isShowingAd) {
            setupRewardedCallback(callback);
            
            // Set reward listener
            rewardedAd.show(activity, reward -> {
                Log.d(TAG, "User earned reward: " + reward.getAmount() + " " + reward.getType());
                
                if (callback != null) {
                    callback.onUserEarnedReward(reward.getType(), reward.getAmount());
                }
                
                if (rewardListener != null) {
                    rewardListener.onUserEarnedReward(reward);
                }
            });
            
            return true;
        } else {
            Log.d(TAG, "Rewarded ad not ready yet");
            
            if (!isLoading) {
                loadAd(callback); // Try to load next ad
            }
            
            if (callback != null) {
                callback.onAdShowFailed("Ad not ready");
            }
            
            return false;
        }
    }
    
    /**
     * Checks if a rewarded ad is loaded and ready to show.
     *
     * @return true if ad is ready, false otherwise
     */
    public boolean isAdLoaded() {
        return rewardedAd != null && !isShowingAd;
    }
    
    /**
     * Checks if the ad is currently in cooldown period.
     *
     * @return true if in cooldown period, false otherwise
     */
    public boolean isInCooldownPeriod() {
        return System.currentTimeMillis() - lastAdDisplayTime < COOLDOWN_PERIOD_MS;
    }
    
    /**
     * Get remaining cooldown time in milliseconds.
     *
     * @return Remaining cooldown time in milliseconds, 0 if not in cooldown
     */
    public long getRemainingCooldownMs() {
        long timeSinceLastAd = System.currentTimeMillis() - lastAdDisplayTime;
        long remainingTime = COOLDOWN_PERIOD_MS - timeSinceLastAd;
        return Math.max(0, remainingTime);
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        rewardedAd = null;
        isLoading = false;
        isShowingAd = false;
    }
} 