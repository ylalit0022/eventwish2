package com.ds.eventwish.ui.ads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.R;
import com.ds.eventwish.data.ads.AdManagerExtended;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;

/**
 * Activity for testing different ad types
 */
public class TestAdActivity extends AppCompatActivity {
    private static final String TAG = "TestAdActivity";
    private static final int RETRY_DELAY_MS = 5000; // 5 seconds
    
    private AdManagerExtended adManager;
    private AdViewModel adViewModel;
    private BannerAdFragment bannerAdFragment;
    private InterstitialAdHelper interstitialAdHelper;
    private RewardedAdHelper rewardedAdHelper;
    private NativeVideoAdHelper nativeVideoAdHelper;
    
    private FrameLayout nativeVideoAdContainer;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private Switch autoRetrySwitch;
    
    private boolean autoRetryEnabled = false;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ad);
        
        // Initialize ad manager
        adManager = AdManagerExtended.getInstance(this);
        
        // Initialize view model
        adViewModel = new ViewModelProvider(this).get(AdViewModel.class);
        
        // Initialize ad helpers
        bannerAdFragment = new BannerAdFragment();
        interstitialAdHelper = new InterstitialAdHelper(this, adViewModel, this);
        rewardedAdHelper = new RewardedAdHelper(this, adViewModel, this);
        nativeVideoAdHelper = new NativeVideoAdHelper(this);
        
        // Find views
        Button showBannerAdButton = findViewById(R.id.show_banner_ad_button);
        Button showInterstitialAdButton = findViewById(R.id.show_interstitial_ad_button);
        Button showRewardedAdButton = findViewById(R.id.show_rewarded_ad_button);
        Button showNativeVideoAdButton = findViewById(R.id.show_native_video_ad_button);
        Button showAppOpenAdButton = findViewById(R.id.show_app_open_ad_button);
        nativeVideoAdContainer = findViewById(R.id.native_video_ad_container);
        progressBar = findViewById(R.id.progress_bar);
        statusTextView = findViewById(R.id.status_text_view);
        autoRetrySwitch = findViewById(R.id.auto_retry_switch);
        
        // Set click listeners
        showBannerAdButton.setOnClickListener(v -> showBannerAd());
        showInterstitialAdButton.setOnClickListener(v -> showInterstitialAd());
        showRewardedAdButton.setOnClickListener(v -> showRewardedAd());
        showNativeVideoAdButton.setOnClickListener(v -> showNativeVideoAd());
        showAppOpenAdButton.setOnClickListener(v -> showAppOpenAd());
        
        // Set auto retry switch listener
        autoRetrySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoRetryEnabled = isChecked;
            updateStatus("Auto retry " + (autoRetryEnabled ? "enabled" : "disabled"));
            Toast.makeText(this, "Auto retry " + (autoRetryEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        
        // Add banner ad fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.banner_ad_container, bannerAdFragment)
                .commit();
        
        // Load ads
        updateStatus("Loading ads...");
        adViewModel.preloadInterstitialAd();
        adViewModel.preloadRewardedAd();
        loadNativeVideoAd();
        loadAppOpenAd();
        
        // Observe ad loading status
        adViewModel.getInterstitialAdLoading().observe(this, isLoading -> {
            if (isLoading != null && !isLoading) {
                updateStatus("Interstitial ad loaded");
            }
        });
        
        adViewModel.getRewardedAdLoading().observe(this, isLoading -> {
            if (isLoading != null && !isLoading) {
                updateStatus("Rewarded ad loaded");
            }
        });
        
        adManager.getAppOpenAdLoadedLiveData().observe(this, isLoaded -> {
            if (isLoaded != null && isLoaded) {
                updateStatus("App open ad loaded");
            }
        });
    }
    
    /**
     * Update status text
     */
    private void updateStatus(String status) {
        if (statusTextView != null) {
            statusTextView.setText(status);
            Log.d(TAG, status);
        }
    }
    
    /**
     * Show a banner ad
     */
    private void showBannerAd() {
        updateStatus("Showing banner ad");
        // The BannerAdFragment will load the ad automatically
        Toast.makeText(this, "Banner ad loaded", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show an interstitial ad
     */
    private void showInterstitialAd() {
        updateStatus("Showing interstitial ad");
        progressBar.setVisibility(View.VISIBLE);
        
        interstitialAdHelper.showInterstitialAd(() -> {
            updateStatus("Interstitial ad dismissed");
            progressBar.setVisibility(View.GONE);
            Toast.makeText(TestAdActivity.this, "Interstitial ad dismissed", Toast.LENGTH_SHORT).show();
            
            // Auto retry loading if enabled
            if (autoRetryEnabled) {
                scheduleAdRetry("interstitial");
            }
        });
    }
    
    /**
     * Show a rewarded ad
     */
    private void showRewardedAd() {
        updateStatus("Showing rewarded ad");
        progressBar.setVisibility(View.VISIBLE);
        
        rewardedAdHelper.showRewardedAd(new RewardedAdHelper.RewardCallback() {
            @Override
            public void onReward(int amount, String type) {
                updateStatus("User earned reward: " + amount + " " + type);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TestAdActivity.this, 
                        "Reward earned: " + amount + " " + type, 
                        Toast.LENGTH_SHORT).show();
                
                // Auto retry loading if enabled
                if (autoRetryEnabled) {
                    scheduleAdRetry("rewarded");
                }
            }
        });
    }
    
    /**
     * Load a native video ad
     */
    private void loadNativeVideoAd() {
        updateStatus("Loading native video ad");
        progressBar.setVisibility(View.VISIBLE);
        
        nativeVideoAdHelper.loadNativeVideoAd(new NativeVideoAdHelper.NativeVideoAdCallback() {
            @Override
            public void onNativeVideoAdLoaded() {
                updateStatus("Native video ad loaded successfully");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TestAdActivity.this, "Native video ad loaded", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onNativeVideoAdFailedToLoad(String errorMessage) {
                updateStatus("Native video ad failed to load: " + errorMessage);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TestAdActivity.this, "Native video ad failed to load", Toast.LENGTH_SHORT).show();
                
                // Auto retry loading if enabled
                if (autoRetryEnabled) {
                    scheduleAdRetry("native");
                }
            }
        });
    }
    
    /**
     * Show a native video ad
     */
    private void showNativeVideoAd() {
        updateStatus("Showing native video ad");
        nativeVideoAdHelper.populateNativeVideoAdView(nativeVideoAdContainer);
    }
    
    /**
     * Load an app open ad
     */
    private void loadAppOpenAd() {
        updateStatus("Loading app open ad");
        adManager.loadAppOpenAd();
    }
    
    /**
     * Show an app open ad
     */
    private void showAppOpenAd() {
        updateStatus("Showing app open ad");
        progressBar.setVisibility(View.VISIBLE);
        
        adManager.showAppOpenAd(this, new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                updateStatus("App open ad dismissed");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TestAdActivity.this, "App open ad dismissed", Toast.LENGTH_SHORT).show();
                
                // Auto retry loading if enabled
                if (autoRetryEnabled) {
                    scheduleAdRetry("appopen");
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                updateStatus("App open ad failed to show: " + adError.getMessage());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TestAdActivity.this, 
                        "App open ad failed to show: " + adError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                
                // Try to load a new ad
                if (autoRetryEnabled) {
                    scheduleAdRetry("appopen");
                } else {
                    loadAppOpenAd();
                }
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                updateStatus("App open ad showed successfully");
            }
        });
    }
    
    /**
     * Schedule an ad retry after a delay
     * @param adType The type of ad to retry loading
     */
    private void scheduleAdRetry(String adType) {
        updateStatus("Scheduling retry for " + adType + " ad in " + (RETRY_DELAY_MS / 1000) + " seconds");
        
        retryHandler.postDelayed(() -> {
            updateStatus("Auto retrying " + adType + " ad load");
            
            switch (adType) {
                case "interstitial":
                    adViewModel.preloadInterstitialAd();
                    break;
                case "rewarded":
                    adViewModel.preloadRewardedAd();
                    break;
                case "native":
                    loadNativeVideoAd();
                    break;
                case "appopen":
                    loadAppOpenAd();
                    break;
            }
        }, RETRY_DELAY_MS);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (nativeVideoAdHelper != null) {
            nativeVideoAdHelper.destroy();
        }
        
        // Remove any pending retry callbacks
        retryHandler.removeCallbacksAndMessages(null);
    }
} 