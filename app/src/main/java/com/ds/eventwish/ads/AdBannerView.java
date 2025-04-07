package com.ds.eventwish.ads;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.ads.AdConstants;

/**
 * Custom view for displaying banner ads
 * This view serves as a placeholder for now without actually loading real ads
 * It will be extended later to support the Google Mobile Ads SDK
 */
public class AdBannerView extends FrameLayout {
    private static final String TAG = "AdBannerView";
    
    private String adType = AdConstants.AdType.BANNER;
    private boolean isTestMode = true;
    private boolean isEnabled = true;
    
    private TextView adStatusLabel;
    private FrameLayout adContainer;
    
    private long startTime;
    private String adUnitId;
    
    private AdMobManager adManager;
    
    private boolean isLoaded = false;
    private boolean hasError = false;
    private int errorCode = 0;
    
    private AdListener adListener;
    
    // Constructors
    public AdBannerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }
    
    public AdBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public AdBannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    /**
     * Initialize the view
     * @param context Context
     * @param attrs AttributeSet
     */
    private void init(Context context, AttributeSet attrs) {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.view_ad_banner, this, true);
        
        // Get references to child views
        adStatusLabel = findViewById(R.id.ad_status_label);
        adContainer = findViewById(R.id.ad_container);
        
        // Parse attributes
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdBannerView);
            isTestMode = a.getBoolean(R.styleable.AdBannerView_testMode, true);
            isEnabled = a.getBoolean(R.styleable.AdBannerView_enabled, true);
            adType = a.getString(R.styleable.AdBannerView_adType);
            if (adType == null) {
                adType = AdConstants.AdType.BANNER;
            }
            a.recycle();
        }
        
        // Initialize the AdMobManager
        try {
            adManager = AdMobManager.getInstance();
        } catch (IllegalStateException e) {
            // AdMobManager not initialized yet, will initialize later
            Log.w(TAG, "AdMobManager not initialized yet: " + e.getMessage());
        }
        
        // Set up the view
        if (!isEnabled) {
            setVisibility(GONE);
        } else {
            setupAdView();
        }
    }
    
    /**
     * Set up the ad view
     */
    private void setupAdView() {
        Log.d(TAG, "Setting up ad view with type: " + adType + ", testMode: " + isTestMode);
        
        try {
            // Clear existing views
            removeAllViews();
            
            if (adManager != null) {
                // Use real AdMob implementation if available
                // This would be the real implementation
                Log.d(TAG, "Using real AdMob implementation");
                // To be replaced with actual implementation
                simulateAdLoading(); 
            } else {
                // Use simulation for testing
                Log.d(TAG, "Using simulated ad for testing");
                simulateAdLoading();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ad view", e);
            displayErrorView(e.getMessage());
        }
    }
    
    /**
     * Display error view
     */
    private void displayErrorView(String errorMessage) {
        // Clear existing views
        removeAllViews();
        
        // Create error view
        TextView errorView = new TextView(getContext());
        errorView.setText("Error loading ad: " + errorMessage);
        errorView.setTextSize(14);
        errorView.setTextColor(0xFFFF0000);
        errorView.setPadding(16, 16, 16, 16);
        errorView.setGravity(Gravity.CENTER);
        
        // Add to layout
        addView(errorView);
        
        // Set error state
        hasError = true;
        errorCode = 101; // Generic error
        
        // Notify listener
        if (adListener != null) {
            adListener.onAdFailedToLoad(errorCode);
        }
    }
    
    /**
     * Update the status label
     * @param status Status text
     */
    private void updateStatusLabel(String status) {
        if (adStatusLabel != null) {
            adStatusLabel.setText(status);
        }
    }
    
    /**
     * Enable or disable the ad view
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (isEnabled) {
            setupAdView();
        } else {
            setVisibility(GONE);
        }
    }
    
    /**
     * Set test mode
     * @param testMode True to enable test mode, false to disable
     */
    public void setTestMode(boolean testMode) {
        isTestMode = testMode;
        setupAdView();
    }
    
    /**
     * Set ad type
     * @param adType Ad type (e.g., AdConstants.AdType.BANNER)
     */
    public void setAdType(String adType) {
        this.adType = adType;
        setupAdView();
    }
    
    /**
     * Set ad listener for lifecycle events
     */
    public void setAdListener(AdListener listener) {
        this.adListener = listener;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupAdView();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        // Track engagement when view is detached
        if (adManager != null && adUnitId != null) {
            long durationSeconds = (System.currentTimeMillis() - startTime) / 1000; // Convert to seconds
            int duration = (int) Math.min(durationSeconds, Integer.MAX_VALUE);
            adManager.trackEngagement(adUnitId, "view", duration);
        }
    }
    
    /**
     * Simulate the ad loading process
     * This is just for testing purposes
     */
    private void simulateAdLoading() {
        // Show loading state
        TextView loadingText = new TextView(getContext());
        loadingText.setText("Loading Ad...");
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setPadding(16, 16, 16, 16);
        addView(loadingText);
        
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Random success or failure (80% success rate in test mode)
            boolean success = isTestMode || Math.random() > 0.2;
            
            if (success) {
                // Remove loading text
                removeAllViews();
                
                // Create ad content
                createAdContent();
                
                // Set loaded state
                isLoaded = true;
                
                // Notify listener
                if (adListener != null) {
                    adListener.onAdLoaded();
                }
                
                // Simulate impression after a short delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (adListener != null) {
                        adListener.onAdImpression();
                    }
                }, 500);
            } else {
                // Failure case
                hasError = true;
                errorCode = 104; // Ad not available
                
                // Remove loading text
                removeAllViews();
                
                // Show error message
                TextView errorText = new TextView(getContext());
                errorText.setText("Ad failed to load (Error code: " + errorCode + ")");
                errorText.setGravity(Gravity.CENTER);
                errorText.setPadding(16, 16, 16, 16);
                addView(errorText);
                
                // Notify listener
                if (adListener != null) {
                    adListener.onAdFailedToLoad(errorCode);
                }
            }
        }, isTestMode ? 1500 : 3000); // Faster in test mode
    }
    
    /**
     * Create the ad content
     */
    private void createAdContent() {
        TextView adContent = new TextView(getContext());
        adContent.setText("This is a " + (isTestMode ? "TEST " : "") + "Banner Ad");
        adContent.setTextSize(18);
        adContent.setGravity(Gravity.CENTER);
        adContent.setPadding(16, 32, 16, 32);
        adContent.setBackgroundColor(0xFF4285F4); // Google blue
        adContent.setTextColor(0xFFFFFFFF); // White text
        
        // Make clickable
        adContent.setOnClickListener(v -> {
            Log.d(TAG, "Ad clicked");
            
            // Notify listener
            if (adListener != null) {
                adListener.onAdClicked();
                adListener.onAdOpened();
                
                // Simulate closing after interaction
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (adListener != null) {
                        adListener.onAdClosed();
                    }
                }, 500);
            }
        });
        
        addView(adContent);
    }
    
    /**
     * Check if the ad is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Check if there was an error loading the ad
     */
    public boolean hasError() {
        return hasError;
    }
    
    /**
     * Get the error code
     */
    public int getErrorCode() {
        return errorCode;
    }
    
    /**
     * Listener interface for ad lifecycle events
     */
    public interface AdListener {
        /**
         * Called when an ad is loaded
         */
        void onAdLoaded();
        
        /**
         * Called when an ad fails to load
         * @param errorCode The error code
         */
        void onAdFailedToLoad(int errorCode);
        
        /**
         * Called when an ad is opened (e.g., a user clicks on the ad)
         */
        void onAdOpened();
        
        /**
         * Called when an ad is clicked
         */
        void onAdClicked();
        
        /**
         * Called when an ad is closed (e.g., user returns from landing page)
         */
        void onAdClosed();
        
        /**
         * Called when an ad impression is recorded
         */
        void onAdImpression();
    }
} 