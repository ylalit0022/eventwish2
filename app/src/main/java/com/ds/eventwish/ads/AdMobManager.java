package com.ds.eventwish.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.appopen.AppOpenAd;

import javax.net.ssl.SSLHandshakeException;

/**
 * Singleton manager class for handling AdMob ads in the application.
 * Handles initialization, loading, and displaying of different ad types.
 */
public class AdMobManager {
    private static final String TAG = "AdMobManager";
    private static AdMobManager instance;
    private Context context;
    private AdMobRepository repository;
    private InterstitialAd interstitialAd;
    private NativeAd nativeAd;
    private boolean isInitialized = false;
    private boolean isLoading = false;
    private int retryAttempts = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private AdMobManager() {
        // Private constructor to prevent direct instantiation
    }

    /**
     * Initializes the AdMob SDK and manager instance.
     * Must be called before getInstance().
     *
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        if (instance == null) {
            instance = new AdMobManager();
            instance.context = context.getApplicationContext();
            
            try {
                // Check if ApiClient is initialized
                if (!com.ds.eventwish.data.remote.ApiClient.isInitialized()) {
                    throw new IllegalStateException("ApiClient not initialized. Call ApiClient.init() first.");
                }
                
                // Create ApiService instance
                ApiService apiService = ApiClient.getClient();
                instance.repository = new AdMobRepository(context.getApplicationContext(), apiService);
                
                // Initialize the Mobile Ads SDK
                MobileAds.initialize(context, initializationStatus -> {
                    Log.d(TAG, "AdMob SDK Initialized");
                    instance.isInitialized = true;
                    // Start loading interstitial ad
                    instance.loadInterstitialAd();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during AdMobManager initialization: " + e.getMessage(), e);
                // Keep instance but mark as not initialized
                instance.isInitialized = false;
            }
        }
    }
    
    /**
     * Checks if AdMobManager is properly initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return instance != null && instance.isInitialized;
    }
    
    /**
     * Gets the singleton instance of AdMobManager.
     * Must call init() first.
     *
     * @return AdMobManager instance
     * @throws IllegalStateException if init() was not called
     */
    public static AdMobManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AdMobManager not initialized. Call init() first.");
        }
        return instance;
    }
    
    /**
     * Loads an interstitial ad from the server.
     */
    private void loadInterstitialAd() {
        if (isLoading || !isInitialized) {
            Log.d(TAG, "Skipping interstitial ad load: " + 
                (isLoading ? "Already loading" : "Not initialized"));
            return;
        }

        isLoading = true;
        Log.d(TAG, "Fetching interstitial ad unit from server...");
        
        repository.fetchAdUnit("interstitial", new AdMobRepository.AdUnitCallback() {
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
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            String errorMessage = context.getString(R.string.error_unknown);
                            Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                            interstitialAd = null;
                            isLoading = false;
                            
                            // Retry logic for ad loading failures
                            if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                                retryAttempts++;
                                Log.d(TAG, "Retrying interstitial ad load (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                                loadInterstitialAd();
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
                    loadInterstitialAd();
                } else {
                    Log.e(TAG, "Max retry attempts reached for server fetch");
                    retryAttempts = 0;
                }
            }
        });
    }

    /**
     * Sets up callbacks for interstitial ad events
     */
    private void setupInterstitialCallbacks() {
        if (interstitialAd == null) return;

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                interstitialAd = null;
                // Load the next interstitial ad
                loadInterstitialAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                interstitialAd = null;
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed successfully");
            }
        });
    }

    /**
     * Shows an interstitial ad if one is loaded.
     *
     * @param activity The activity context to show the ad
     * @return true if ad was shown, false otherwise
     */
    public boolean showInterstitialAd(Activity activity) {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            return true;
        } else {
            Log.d(TAG, "Interstitial ad not ready yet");
            loadInterstitialAd(); // Try to load next ad
            return false;
        }
    }

    /**
     * Checks if an interstitial ad is loaded and ready to show
     *
     * @return true if ad is ready, false otherwise
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }

    /**
     * Loads a native ad from the server.
     * 
     * @param callback Callback to be notified when the ad is loaded or fails
     */
    public void loadNativeAd(@NonNull NativeAdCallback callback) {
        if (isLoading || !isInitialized) {
            String errorMessage = context.getString(R.string.error_unknown);
            callback.onError(isLoading ? "Ad loading in progress" : errorMessage);
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Fetching native ad unit from server...");
        
        repository.fetchAdUnit("native", new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                if (!adUnit.isStatus()) {
                    Log.d(TAG, "Native ad unit is disabled on server");
                    isLoading = false;
                    callback.onError("Native ads are currently disabled");
                    return;
                }

                Log.d(TAG, "Loading native ad with unit ID: " + adUnit.getAdUnitCode());
                AdLoader adLoader = new AdLoader.Builder(context, adUnit.getAdUnitCode())
                    .forNativeAd(nativeAd -> {
                        AdMobManager.this.nativeAd = nativeAd;
                        isLoading = false;
                        callback.onAdLoaded(nativeAd);
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            String errorMessage = context.getString(R.string.error_unknown);
                            Log.e(TAG, "Native ad failed to load: " + loadAdError.getMessage());
                            isLoading = false;
                            callback.onError(errorMessage);
                        }
                    })
                    .build();

                adLoader.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onError(String error) {
                String errorMessage;
                if (error.contains("SSLHandshakeException")) {
                    errorMessage = context.getString(R.string.error_ssl);
                } else {
                    errorMessage = context.getString(R.string.error_unknown);
                }
                Log.e(TAG, "Failed to fetch native ad unit: " + errorMessage);
                isLoading = false;
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Populates a native ad view with the loaded native ad content.
     * 
     * @param nativeAdView The view to populate with ad content
     * @return true if ad was populated successfully, false otherwise
     */
    public boolean populateNativeAdView(NativeAdView nativeAdView) {
        if (nativeAd == null || nativeAdView == null) {
            Log.e(TAG, "Cannot populate native ad view: ad or view is null");
            return false;
        }

        // Set the media view
        com.google.android.gms.ads.nativead.MediaView mediaView = 
            nativeAdView.findViewById(R.id.ad_media);
        if (mediaView != null) {
            nativeAdView.setMediaView(mediaView);
        }

        // Set other ad assets
        TextView headlineView = nativeAdView.findViewById(R.id.ad_headline);
        if (headlineView != null) {
            headlineView.setText(nativeAd.getHeadline());
            nativeAdView.setHeadlineView(headlineView);
        }

        TextView bodyView = nativeAdView.findViewById(R.id.ad_body);
        if (bodyView != null) {
            bodyView.setText(nativeAd.getBody());
            nativeAdView.setBodyView(bodyView);
        }

        Button callToActionButton = nativeAdView.findViewById(R.id.ad_call_to_action);
        if (callToActionButton != null) {
            callToActionButton.setText(nativeAd.getCallToAction());
            nativeAdView.setCallToActionView(callToActionButton);
        }

        ImageView iconView = nativeAdView.findViewById(R.id.ad_app_icon);
        if (iconView != null) {
            if (nativeAd.getIcon() != null) {
                iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
                iconView.setVisibility(View.VISIBLE);
                } else {
                iconView.setVisibility(View.GONE);
            }
            nativeAdView.setIconView(iconView);
        }

        TextView advertiserView = nativeAdView.findViewById(R.id.ad_advertiser);
        if (advertiserView != null) {
            advertiserView.setText(nativeAd.getAdvertiser());
            nativeAdView.setAdvertiserView(advertiserView);
        }

        RatingBar ratingBar = nativeAdView.findViewById(R.id.ad_stars);
        if (ratingBar != null && nativeAd.getStarRating() != null) {
            ratingBar.setRating(nativeAd.getStarRating().floatValue());
            ratingBar.setVisibility(View.VISIBLE);
            nativeAdView.setStarRatingView(ratingBar);
        }

        // Associate the native ad with the view
        nativeAdView.setNativeAd(nativeAd);
        return true;
    }

    /**
     * Destroys the currently loaded native ad.
     */
    public void destroyNativeAd() {
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    /**
     * Checks if a native ad is currently loaded.
     *
     * @return true if ad is loaded, false otherwise
     */
    public boolean isNativeAdLoaded() {
        return nativeAd != null;
    }

    /**
     * Loads an app open ad from the server.
     * 
     * @param callback Callback to be notified when the ad is loaded or fails
     */
    public void loadAppOpenAd(@NonNull AppOpenAdCallback callback) {
        if (isLoading) {
            String errorMessage = context.getString(R.string.error_unknown);
            Log.e(TAG, "Cannot load app open ad: Ad loading in progress");
            callback.onError("Ad loading in progress");
            return;
        }
        
        if (!isInitialized()) {
            String errorMessage = context.getString(R.string.error_unknown);
            Log.e(TAG, "Cannot load app open ad: AdMob not initialized");
            callback.onError(errorMessage);
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Fetching app open ad unit from server...");
        
        // Try with multiple possible ad type variations - the server might be using a different naming convention
        repository.fetchAdUnit("app_open", new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                Log.d(TAG, "Successfully fetched app open ad unit: " + adUnit.getAdUnitCode());
                processAppOpenAdUnit(adUnit, callback);
            }

            @Override
            public void onError(String error) {
                // First variation failed, try "AppOpen"
                Log.d(TAG, "Failed to find ad unit with type 'app_open', trying 'AppOpen'...");
                repository.fetchAdUnit("AppOpen", new AdMobRepository.AdUnitCallback() {
                    @Override
                    public void onSuccess(AdUnit adUnit) {
                        Log.d(TAG, "Successfully fetched AppOpen ad unit: " + adUnit.getAdUnitCode());
                        processAppOpenAdUnit(adUnit, callback);
                    }

                    @Override
                    public void onError(String secondError) {
                        // Second variation failed, try "App Open"
                        Log.d(TAG, "Failed to find ad unit with type 'AppOpen', trying 'App Open'...");
                        repository.fetchAdUnit("App Open", new AdMobRepository.AdUnitCallback() {
                            @Override
                            public void onSuccess(AdUnit adUnit) {
                                Log.d(TAG, "Successfully fetched App Open ad unit: " + adUnit.getAdUnitCode());
                                processAppOpenAdUnit(adUnit, callback);
                            }

                            @Override
                            public void onError(String thirdError) {
                                // All variations failed, report the original error
                                Log.e(TAG, "All attempts to fetch app open ad unit failed. Error: " + error);
                                handleAdUnitError(error, callback);
                            }
                        });
                    }
                });
            }
        });
    }
    
    /**
     * Process the app open ad unit once found
     */
    private void processAppOpenAdUnit(AdUnit adUnit, AppOpenAdCallback callback) {
        if (!adUnit.isStatus()) {
            Log.d(TAG, "App open ad unit is disabled on server");
            isLoading = false;
            retryAttempts = 0;
            callback.onError("App open ads are currently disabled");
            return;
        }

        Log.d(TAG, "Loading app open ad with unit ID: " + adUnit.getAdUnitCode());
        AppOpenAd.load(context, 
            adUnit.getAdUnitCode(),
            new AdRequest.Builder().build(),
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully");
                    isLoading = false;
                    retryAttempts = 0;
                    callback.onAdLoaded(appOpenAd);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    String errorMessage = context.getString(R.string.error_unknown);
                    Log.e(TAG, "App open ad failed to load: " + loadAdError.getMessage());
                    isLoading = false;
                    
                    // Retry logic for ad loading failures
                    if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                        retryAttempts++;
                        Log.d(TAG, "Retrying app open ad load (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                        loadAppOpenAd(callback);
                    } else {
                        Log.e(TAG, "Max retry attempts reached for app open ad");
                        retryAttempts = 0;
                        callback.onError(errorMessage);
                    }
                }
            });
    }
    
    /**
     * Handle ad unit error with proper logging
     */
    private void handleAdUnitError(String error, AppOpenAdCallback callback) {
        String errorMessage;
        if (error.contains("SSLHandshakeException")) {
            errorMessage = context.getString(R.string.error_ssl);
        } else {
            errorMessage = context.getString(R.string.error_unknown);
        }
        Log.e(TAG, "Failed to fetch app open ad unit: " + errorMessage);
        isLoading = false;
        
        // Retry logic for server errors
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts++;
            Log.d(TAG, "Retrying server fetch (Attempt " + retryAttempts + "/" + MAX_RETRY_ATTEMPTS + ")");
            loadAppOpenAd(callback);
        } else {
            Log.e(TAG, "Max retry attempts reached for server fetch");
            retryAttempts = 0;
            callback.onError(errorMessage);
        }
    }
    
    /**
     * Callback interface for app open ad loading events.
     */
    public interface AppOpenAdCallback {
        void onAdLoaded(AppOpenAd appOpenAd);
        void onError(String message);
    }

    /**
     * Callback interface for native ad loading events.
     */
    public interface NativeAdCallback {
        void onAdLoaded(NativeAd nativeAd);
        void onError(String message);
    }
} 