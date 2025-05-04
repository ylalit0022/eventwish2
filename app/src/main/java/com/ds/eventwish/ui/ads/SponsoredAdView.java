package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewTreeObserver;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.Priority;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.AdSessionManager;
import com.ds.eventwish.EventWishApplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom view for displaying sponsored ads in the UI with improved state handling
 */
public class SponsoredAdView extends FrameLayout {
    private static final String TAG = "SponsoredAdView";
    private static final int FADE_DURATION = 300; // Animation duration in ms
    private static final long IMPRESSION_CHECK_DELAY_MS = 500; // Delay before checking visibility
    private static final long IMPRESSION_MIN_VISIBLE_TIME_MS = 1000; // Minimum time ad must be visible
    private static final float MIN_VISIBILITY_PERCENT = 50f; // At least 50% of the view must be visible
    private static final int MAX_IMAGE_RETRY_COUNT = 3; // Maximum number of retries for image loading
    private static final float CROSSFADE_ROTATION_DURATION_MS = 400; // Slightly longer fade for rotation
    private static final boolean PRELOAD_NEXT_ROTATION = true; // Preload next rotation ad image
    
    // Debug mode settings
    private static boolean DEBUG_MODE = false; // Can be toggled at runtime
    private static final boolean ENABLE_VERBOSE_LOGS = false; // For extremely detailed logs
    private static final boolean LOG_METRICS = true; // For performance metrics
    private static final boolean LOG_VERBOSE = false; // For verbose logging
    
    // Error recovery settings
    private static final int MAX_RETRY_ATTEMPTS = 5; // Maximum retry attempts for operations
    private static final long BASE_RETRY_DELAY_MS = 1000; // Base delay for exponential backoff (1s)
    private static final long MAX_RETRY_DELAY_MS = 60000; // Maximum delay for exponential backoff (1min)
    
    // UI Components
    private CardView cardContainer;
    private ImageView adImage;
    private TextView adTitle;
    private TextView adDescription;
    private TextView sponsoredLabel;
    private ProgressBar loadingView;
    private LinearLayout errorContainer;
    private Button retryButton;
    
    // State
    private SponsoredAdViewModel viewModel;
    private SponsoredAd currentAd;
    private String location;
    private ViewState currentState = ViewState.LOADING;
    private InternetConnectivityChecker connectivityChecker;
    private AtomicBoolean impressionTracked = new AtomicBoolean(false);
    private AtomicBoolean adFullyLoaded = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());
    private AtomicBoolean isVisibleToUser = new AtomicBoolean(false);
    private long visibleSince = 0;
    private Runnable impressionCheckRunnable;
    private AtomicBoolean enableRotation = new AtomicBoolean(false);
    private SponsoredAd previousAd;
    private ImageView previousAdImage;
    private AtomicBoolean isRotationInitialized = new AtomicBoolean(false);
    
    // Add new fields for visibility tracking
    private final Rect tempVisibleRect = new Rect();
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    private ViewTreeObserver.OnDrawListener drawListener;
    
    // Add new fields for image loading optimization
    private AtomicInteger imageLoadRetryCount = new AtomicInteger(0);
    private long lastImageLoadAttempt = 0;
    private static final long IMAGE_RETRY_DELAY_MS = 2000; // Wait 2 seconds before retry
    
    // Add new fields for rotation optimization
    private SponsoredAd nextRotationAd = null;
    private AtomicBoolean isPreloadingNextAd = new AtomicBoolean(false);
    private AppExecutors executors;
    
    // Add new fields for error recovery
    private AtomicInteger operationRetryCount = new AtomicInteger(0);
    private AtomicBoolean isInErrorRecovery = new AtomicBoolean(false);
    
    // Thread-safe collections
    private final Set<String> shownAdsSet = Collections.synchronizedSet(new HashSet<>());
    
    // Add a new field to track if we're waiting for initial ad
    private boolean initialAdLoaded = false;
    
    // Enum to represent the different states of the view
    private enum ViewState {
        LOADING,
        LOADED,
        ERROR,
        GONE
    }
    
    public SponsoredAdView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public SponsoredAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public SponsoredAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.view_sponsored_ad, this, true);
        
        // Find views
        cardContainer = root.findViewById(R.id.sponsored_ad_container);
        adImage = root.findViewById(R.id.sponsored_ad_image);
        adTitle = root.findViewById(R.id.sponsored_ad_title);
        adDescription = root.findViewById(R.id.sponsored_ad_description);
        sponsoredLabel = root.findViewById(R.id.sponsored_label);
        loadingView = root.findViewById(R.id.sponsored_ad_loading);
        errorContainer = root.findViewById(R.id.sponsored_ad_error_container);
        retryButton = root.findViewById(R.id.sponsored_ad_retry_button);
        
        // Initialize connectivity checker
        connectivityChecker = InternetConnectivityChecker.getInstance(context);
        
        // Initialize executors
        executors = AppExecutors.getInstance();
        
        // Create impression check runnable
        impressionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkForValidImpression();
            }
        };
        
        // Make EVERYTHING invisible initially
        cardContainer.setVisibility(GONE);
        adImage.setVisibility(GONE);
        adTitle.setVisibility(GONE);
        adDescription.setVisibility(GONE);
        sponsoredLabel.setVisibility(GONE);
        loadingView.setVisibility(GONE);
        errorContainer.setVisibility(GONE);
        
        // Set initial state to GONE - hide everything initially
        setState(ViewState.GONE);
        
        // Set initial view visibility to GONE to prevent any placeholder from showing
        setVisibility(GONE);
        
        // Set click listener on the container
        cardContainer.setOnClickListener(v -> {
            if (viewModel != null && currentAd != null) {
                Log.d(TAG, "Ad clicked: " + currentAd.getId() + ", redirectUrl: " + currentAd.getRedirectUrl());
                
                // Show a ripple effect for feedback
                cardContainer.setPressed(true);
                handler.postDelayed(() -> cardContainer.setPressed(false), 200);
                
                // Handle click in ViewModel
                viewModel.handleAdClick(currentAd, getContext());
            }
        });
        
        // Set retry button click listener
        retryButton.setOnClickListener(v -> {
            if (viewModel != null) {
                refreshAds();
            }
        });
    }
    
    /**
     * Check if the current ad impression should be tracked based on visibility duration
     */
    private void checkForValidImpression() {
        if (currentAd == null || !adFullyLoaded.get() || impressionTracked.get() || !isVisibleToUser.get()) {
            logDebug("Skipping impression tracking: ad=" + (currentAd != null ? currentAd.getId() : "null") + 
                  ", loaded=" + adFullyLoaded.get() + ", tracked=" + impressionTracked.get() + 
                  ", visible=" + isVisibleToUser.get());
            return;
        }
        
        // Check actual visibility using Rect calculation
        if (!isActuallyVisibleOnScreen()) {
            Log.d(TAG, "Ad not actually visible on screen based on visibility percentage");
            return;
        }
        
        long now = System.currentTimeMillis();
        long visibleTime = now - visibleSince;
        
        // Only track impression if the ad has been visible for at least the minimum time
        if (visibleTime >= IMPRESSION_MIN_VISIBLE_TIME_MS) {
            Log.d(TAG, "Ad visible for " + visibleTime + "ms, tracking impression");
            trackImpression();
        } else {
            // Check again after a delay
            long remainingTime = IMPRESSION_MIN_VISIBLE_TIME_MS - visibleTime;
            Log.d(TAG, "Ad visible for " + visibleTime + "ms, checking again in " + remainingTime + "ms");
            handler.postDelayed(impressionCheckRunnable, remainingTime);
        }
    }
    
    /**
     * Accurately determine if the view is actually visible on screen
     * @return true if the view is visible based on percentage threshold
     */
    private boolean isActuallyVisibleOnScreen() {
        // Get the visible portion of the view
        boolean isVisible = getGlobalVisibleRect(tempVisibleRect);
        int visibleArea = tempVisibleRect.width() * tempVisibleRect.height();
        int totalArea = getWidth() * getHeight();
        
        // Calculate visibility percentage (avoid division by zero)
        float visibilityPercentage = (totalArea > 0) ? (visibleArea * 100f / totalArea) : 0f;
        
        Log.d(TAG, "VISIBILITY CHECK: isVisible=" + isVisible + 
              ", visibleArea=" + visibleArea + 
              ", totalArea=" + totalArea + 
              ", visiblePercent=" + visibilityPercentage + "%" + 
              ", threshold=" + MIN_VISIBILITY_PERCENT + "%");
              
        // Consider visible if the visible percentage exceeds the threshold
        return isVisible && visibilityPercentage >= MIN_VISIBILITY_PERCENT;
    }
    
    /**
     * Track an impression only once per ad
     */
    private synchronized void trackImpression() {
        if (currentAd == null || impressionTracked.get() || !adFullyLoaded.get() || viewModel == null) {
            logDebug("Skipping trackImpression: " + 
                  (currentAd == null ? "null ad" : 
                  impressionTracked.get() ? "already tracked" : 
                  !adFullyLoaded.get() ? "not fully loaded" : "null viewModel"));
            return;
        }
        
        // Track metrics before impression if enabled
        if (LOG_METRICS) {
            logMetrics("pre_impression", currentAd);
        }
        
        // SIMPLIFIED APPROACH: Use AdSessionManager to track impressions only once per app session
        AdSessionManager sessionManager = EventWishApplication.getInstance().getAdSessionManager();
        if (sessionManager == null) {
            Log.e(TAG, "Cannot track impression - AdSessionManager is null");
            return;
        }
        
        String adId = currentAd.getId();
        
        // Check if this ad was already tracked in the current session
        if (sessionManager.isImpressionTracked(adId)) {
            Log.d(TAG, "IMPRESSION: Ad " + adId + " already tracked in this session, skipping");
            return;
        }
        
        // Mark the impression as tracked in the current session
        sessionManager.trackImpression(adId);
        
        // Mark as tracked to avoid duplicate tracking
        impressionTracked.set(true);
        
        Log.d(TAG, "IMPRESSION: Tracked impression for ad " + adId + 
              " with title: " + currentAd.getTitle() + 
              ", at location: " + currentAd.getLocation());
        
        if (LOG_VERBOSE) {
            // Dump session data for debugging
            sessionManager.dumpSessionData();
        }
        
        // Send to server via ViewModel - IMPORTANT: This is the part that needs to happen immediately 
        try {
            Log.d(TAG, "IMPRESSION: Sending impression to server via ViewModel for ad: " + 
                 currentAd.getId() + ", title: " + currentAd.getTitle());
            // This will send immediately to server when network is available
            viewModel.handleAdImpression(currentAd);
            
            if (LOG_METRICS) {
                logMetrics("post_impression", currentAd);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking impression", e);
        }
    }
    
    /**
     * Log structured debug information
     * @param message Debug message to log
     */
    private void logDebug(String message) {
        if (DEBUG_MODE) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Log detailed verbose information (only in extra verbose mode)
     * @param message Verbose message to log
     */
    private void logVerbose(String message) {
        if (DEBUG_MODE && ENABLE_VERBOSE_LOGS) {
            Log.v(TAG, message);
        }
    }
    
    /**
     * Log comprehensive metrics about ad operations
     * @param operation The operation being performed
     * @param ad The ad being processed
     */
    private void logMetrics(String operation, SponsoredAd ad) {
        if (!LOG_METRICS) return;
        
        // Create structured log data
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("operation", operation);
        metrics.put("timestamp", System.currentTimeMillis());
        
        // Ad details
        if (ad != null) {
            metrics.put("ad_id", ad.getId());
            metrics.put("ad_title", ad.getTitle());
            metrics.put("ad_priority", ad.getPriority());
            metrics.put("ad_location", location);
        } else {
            metrics.put("ad_id", "null");
        }
        
        // View state
        metrics.put("view_state", currentState.name());
        metrics.put("visibility", getVisibility());
        metrics.put("is_shown", isShown());
        metrics.put("dimensions", getWidth() + "x" + getHeight());
        metrics.put("actual_visibility", isActuallyVisibleOnScreen());
        
        // Tracking state
        metrics.put("impression_tracked", impressionTracked.get());
        metrics.put("ad_fully_loaded", adFullyLoaded.get());
        metrics.put("is_visible_to_user", isVisibleToUser.get());
        metrics.put("rotation_enabled", enableRotation.get());
        
        // Log the metrics
        if (DEBUG_MODE) {
            Log.d(TAG, "METRICS: " + metrics);
        }
    }
    
    /**
     * Initialize the view with a location and lifecycle owner
     * @param location The location identifier for this ad view
     * @param lifecycleOwner LifecycleOwner to observe LiveData
     * @param viewModelStoreOwner ViewModelStoreOwner to get ViewModel
     */
    public void initialize(String location, LifecycleOwner lifecycleOwner, ViewModelStoreOwner viewModelStoreOwner) {
        this.location = location;
        Log.d(TAG, "Initializing sponsored ad view for location: " + location);
        
        // Initially hide the entire view - IMPORTANT: keep hidden until we confirm a valid ad
        setVisibility(GONE);
        initialAdLoaded = false;
        
        try {
            // Get ViewModel from the factory
            this.viewModel = SponsoredAdManagerFactory.getInstance().getViewModel(viewModelStoreOwner);
            
            // Register this view with the factory
            SponsoredAdManagerFactory.getInstance().registerAdView(location, this);
            Log.d(TAG, "Registered ad view for location: " + location + " with AdManagerFactory");
            
            // Force refresh from server to get latest status
            viewModel.forceRefreshAds();
            
            // Observe ad for the specified location
            viewModel.getAdForLocation(location).observe(lifecycleOwner, ad -> {
                if (ad != null) {
                    Log.d(TAG, "Received ad from ViewModel for location: " + location + 
                          ", id: " + ad.getId() + ", title: " + ad.getTitle() + 
                          ", status: " + ad.isStatus());
                    
                    // First, check status before doing anything else
                    if (!ad.isStatus()) {
                        Log.d(TAG, "Ad status is false, keeping view completely hidden: " + ad.getId());
                        setVisibility(GONE);
                        initialAdLoaded = true; // Mark as loaded so we don't show placeholders
                        return;
                    }
                    
                    // If we receive a different ad than before, reset tracking state
                    if (currentAd == null || !currentAd.getId().equals(ad.getId())) {
                        resetTrackingState();
                    }
                    
                    // Only now do we start loading the ad - view still hidden until image loads
                    loadAd(ad);
                    initialAdLoaded = true;
                } else {
                    Log.d(TAG, "No ad received from ViewModel for location: " + location);
                    setVisibility(GONE);
                    initialAdLoaded = true; // Mark as loaded so we won't show placeholders
                }
            });
            
            // Observe loading state but DON'T show anything during loading
            viewModel.getLoadingState().observe(lifecycleOwner, isLoading -> {
                Log.d(TAG, "Ad loading state changed: " + isLoading + " for location: " + location);
                
                // If we're loading and haven't received any ad yet, keep hidden
                if (isLoading && !initialAdLoaded) {
                    // Keep everything hidden during initial load
                    setVisibility(GONE);
                }
            });
            
            // Observe errors
            viewModel.getError().observe(lifecycleOwner, error -> {
                if (error != null && !error.isEmpty()) {
                    Log.e(TAG, "Error loading sponsored ad: " + error + " for location: " + location);
                    setVisibility(GONE);
                    initialAdLoaded = true; // Mark as loaded so we don't show placeholders
                    
                    // Try to force refresh when we get an error in case status changed
                    handler.postDelayed(() -> {
                        if (viewModel != null) {
                            viewModel.forceRefreshAds();
                        }
                    }, 5000); // Wait 5 seconds before retrying
                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize sponsored ad view: " + e.getMessage(), e);
            setVisibility(GONE);
            initialAdLoaded = true; // Mark as loaded so we don't show placeholders
        }
    }
    
    /**
     * Initialize the view with a location, lifecycle owner and rotation settings
     * @param location The location identifier for this ad view
     * @param lifecycleOwner LifecycleOwner to observe LiveData
     * @param viewModelStoreOwner ViewModelStoreOwner to get ViewModel
     * @param enableRotation Whether to enable rotation
     * @param rotationIntervalMinutes Minutes between rotations
     */
    public void initialize(String location, LifecycleOwner lifecycleOwner, 
                           ViewModelStoreOwner viewModelStoreOwner,
                           boolean enableRotation, int rotationIntervalMinutes) {
        // First initialize with standard parameters
        initialize(location, lifecycleOwner, viewModelStoreOwner);
        
        // Then apply rotation settings if requested
        if (enableRotation) {
            this.enableRotation(enableRotation);
            this.setRotationIntervalMinutes(rotationIntervalMinutes);
            Log.d(TAG, "Initialized with rotation enabled, interval: " + rotationIntervalMinutes + " minutes");
        }
    }
    
    /**
     * Enable/disable rotation with performance optimization
     * @param enable Whether to enable rotation
     */
    public void enableRotation(boolean enable) {
        if (enable == enableRotation.get()) return; // No change
        
        this.enableRotation.set(enable);
        Log.d(TAG, "Setting rotation to " + enable);
        
        if (viewModel != null) {
            if (enable) {
                if (!isRotationInitialized.get()) {
                    // Set up rotation LiveData observation
                    LifecycleOwner lifecycleOwner = getLifecycleOwner();
                    if (lifecycleOwner != null) {
                        Log.d(TAG, "Starting rotation for location: " + location);
                        observeRotatingAd(lifecycleOwner);
                        viewModel.startRotation(location);
                        isRotationInitialized.set(true);
                        
                        // Start preloading next rotation ad
                        preloadNextRotationAd();
                    } else {
                        Log.e(TAG, "Cannot start rotation: lifecycleOwner is null");
                    }
                } else {
                    // Already initialized, just start rotation again
                    Log.d(TAG, "Resuming rotation for location: " + location);
                    viewModel.startRotation(location);
                    
                    // Start preloading next rotation ad
                    preloadNextRotationAd();
                }
            } else {
                // Stop rotation
                Log.d(TAG, "Stopping rotation for location: " + location);
                viewModel.stopRotation();
                
                // Cancel any preloading
                isPreloadingNextAd.set(false);
            }
        }
    }

    /**
     * Set rotation interval in minutes
     * @param minutes Minutes between rotations (5-1440)
     */
    public void setRotationIntervalMinutes(int minutes) {
        if (minutes < 1) minutes = 1;
        if (minutes > 1440) minutes = 1440; // Max 24 hours
        
        Log.d(TAG, "Setting rotation interval to " + minutes + " minutes for location: " + location);
        if (viewModel != null) {
            viewModel.setRotationIntervalMinutes(minutes);
            
            // Restart rotation if it was running
            if (enableRotation.get() && isRotationInitialized.get()) {
                Log.d(TAG, "Restarting rotation with new interval");
                viewModel.startRotation(location);
            }
        }
    }

    /**
     * Get the lifecycle owner from the context
     */
    private LifecycleOwner getLifecycleOwner() {
        Context context = getContext();
        if (context instanceof LifecycleOwner) {
            return (LifecycleOwner) context;
        }
        return null;
    }

    /**
     * Observe the rotating ad LiveData
     */
    private void observeRotatingAd(LifecycleOwner lifecycleOwner) {
        if (viewModel == null || location == null) {
            Log.e(TAG, "Cannot observe rotating ads - viewModel or location is null");
            return;
        }
        
        Log.d(TAG, "Setting up observer for rotating ads at location: " + location);
        viewModel.getRotatingAd(location).observe(lifecycleOwner, ad -> {
            if (ad != null) {
                Log.d(TAG, "Received rotating ad: " + ad.getId() + 
                      ", title: " + ad.getTitle() + 
                      ", priority: " + ad.getPriority() +
                      ", impressions: " + ad.getImpressionCount());
                handleRotatedAd(ad);
            } else {
                Log.d(TAG, "Received null rotating ad");
            }
        });
    }

    /**
     * Handle rotation of ads with smooth transitions
     * @param ad The new ad to rotate to
     */
    private void handleRotatedAd(SponsoredAd ad) {
        if (ad == null) {
            Log.w(TAG, "Received null ad for rotation, ignoring");
            setVisibility(GONE);
            return;
        }
        
        // Check ad status - hide completely if status is false
        if (!ad.isStatus()) {
            Log.d(TAG, "Rotated ad status is false, hiding view completely: " + ad.getId());
            setVisibility(GONE);
            return;
        }
        
        // Make sure the view is visible since we have a valid ad
        setVisibility(VISIBLE);
        
        if (currentAd != null && ad.getId().equals(currentAd.getId())) {
            Log.d(TAG, "Received same ad for rotation, ignoring: " + ad.getId());
            return;
        }
        
        Log.d(TAG, "Handling rotated ad: " + ad.getId() + " - " + ad.getTitle());
        
        // Keep track of the previous ad for crossfade animation
        previousAd = currentAd;
        
        // Store copy of current image view for crossfade
        if (adImage != null && adImage.getDrawable() != null) {
            try {
                // Get parent to ensure it exists before adding a view
                if (cardContainer != null) {
                    // Create a new image view for the old image for crossfade effect
                    previousAdImage = new ImageView(getContext());
                    previousAdImage.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    previousAdImage.setScaleType(adImage.getScaleType());
                    previousAdImage.setImageDrawable(adImage.getDrawable());
                    previousAdImage.setAlpha(1.0f);
                    
                    // Insert at same position as current image view
                    int index = cardContainer.indexOfChild(adImage);
                    if (index >= 0) {
                        cardContainer.addView(previousAdImage, index);
                        Log.d(TAG, "Added previous image view for crossfade transition");
                    } else {
                        cardContainer.addView(previousAdImage);
                        Log.d(TAG, "Added previous image view (at end) for crossfade transition");
                    }
                    
                    // Hide current image initially for smooth loading transition
                    adImage.setAlpha(0.0f);
                    
                    // Start optimized crossfade after new image loads
                    loadImageOptimized(ad.getImageUrl(), adImage);
                    Log.d(TAG, "Started loading new image for rotation");
                    
                    // Update current ad reference
                    currentAd = ad;
                    
                    // Reset tracking state
                    resetTrackingState();
                    
                    // Start preloading next rotation ad for upcoming rotations
                    preloadNextRotationAd();
                    
                } else {
                    Log.e(TAG, "Cannot add previous image view: cardContainer is null");
                    // Fallback to standard loading
                    currentAd = ad;
                    loadImageOptimized(ad.getImageUrl(), adImage);
                    resetTrackingState();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up crossfade animation: " + e.getMessage());
                // Fallback to standard loading
                currentAd = ad;
                loadImageOptimized(ad.getImageUrl(), adImage);
                resetTrackingState();
            }
        } else {
            // No current image, just load the new one
            Log.d(TAG, "No current image to crossfade from, loading new image directly");
            currentAd = ad;
            loadImageOptimized(ad.getImageUrl(), adImage);
            resetTrackingState();
        }
    }
    
    /**
     * Reset tracking state when a new ad is loaded
     */
    private void resetTrackingState() {
        impressionTracked.set(false);
        adFullyLoaded.set(false);
        isVisibleToUser.set(false);
        visibleSince = 0;
        handler.removeCallbacks(impressionCheckRunnable);
    }
    
    /**
     * Update the view state and UI accordingly
     * @param state The new state to set
     */
    private void setState(ViewState state) {
        // Skip if state hasn't changed
        if (state == currentState) return;
        
        // Track state change for debugging
        logDebug("State changing from " + currentState + " to " + state);
        
        // Update current state
        ViewState previousState = currentState;
        currentState = state;
        
        // If current ad has status=false or is null, force GONE state regardless of requested state
        if (currentAd == null || !currentAd.isStatus()) {
            if (state != ViewState.GONE) {
                Log.d(TAG, "Forcing GONE state because ad is null or status is false");
                state = ViewState.GONE;
                currentState = ViewState.GONE;
                setVisibility(GONE);
            }
        }
        
        // Update UI based on new state
        switch (state) {
            case LOADING:
                // Show loading indicator
                if (loadingView != null) {
                    loadingView.setVisibility(VISIBLE);
                }
                // Hide error container
                if (errorContainer != null) {
                    errorContainer.setVisibility(GONE);
                }
                break;
                
            case LOADED:
                // Hide loading indicator
                if (loadingView != null) {
                    loadingView.setVisibility(GONE);
                }
                // Hide error container
                if (errorContainer != null) {
                    errorContainer.setVisibility(GONE);
                }
                // Reset error recovery state on successful load
                operationRetryCount.set(0);
                isInErrorRecovery.set(false);
                break;
                
            case ERROR:
                // Hide entire view on error
                setVisibility(GONE);
                // Hide loading indicator
                if (loadingView != null) {
                    loadingView.setVisibility(GONE);
                }
                // Hide error container too - don't show errors
                if (errorContainer != null) {
                    errorContainer.setVisibility(GONE);
                }
                break;
                
            case GONE:
                // Hide everything
                setVisibility(GONE);
                if (loadingView != null) {
                    loadingView.setVisibility(GONE);
                }
                if (errorContainer != null) {
                    errorContainer.setVisibility(GONE);
                }
                break;
        }
        
        // Track state change metrics
        if (LOG_METRICS) {
            Map<String, Object> stateMetrics = new HashMap<>();
            stateMetrics.put("previous_state", previousState.name());
            stateMetrics.put("new_state", state.name());
            stateMetrics.put("timestamp", System.currentTimeMillis());
            logDebug("STATE_METRICS: " + stateMetrics);
        }
    }
    
    /**
     * Called when the view becomes visible to the user
     */
    private void onBecameVisible() {
        // Set visible time
        isVisibleToUser.set(true);
        visibleSince = System.currentTimeMillis();
        Log.d(TAG, "TRACKING: View became visible at " + visibleSince);
        
        // Start impression tracking after minimum visible time
        handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
        Log.d(TAG, "TRACKING: Scheduled impression check in " + IMPRESSION_MIN_VISIBLE_TIME_MS + "ms");
        
        // Setup ViewTreeObserver listeners for accurate visibility tracking
        setupVisibilityTracking();
    }

    /**
     * Called when the fragment or activity pauses
     */
    public void onPause() {
        if (enableRotation.get()) {
            Log.d(TAG, "Pausing rotation in onPause");
            // Store that rotation was enabled, but pause it
            boolean wasRotating = enableRotation.get();
            enableRotation(false);
            enableRotation.set(wasRotating);
        }
    }

    /**
     * Called when the fragment or activity pauses
     * This should be called from the fragment's onPause method
     */
    public void handlePause() {
        if (enableRotation.get()) {
            Log.d(TAG, "Pausing rotation in handlePause");
            // Store that rotation was enabled, but pause it
            boolean wasRotating = enableRotation.get();
            enableRotation(false);
            enableRotation.set(wasRotating);
        }
    }

    /**
     * Called when the fragment or activity resumes
     * This should be called from the fragment's onResume method
     */
    public void handleResume() {
        Log.d(TAG, "TRACKING: handleResume called");
        
        // Ensure the ad view itself is visible
        setVisibility(VISIBLE);
        requestLayout(); // Force layout pass
        Log.d(TAG, "TRACKING: Set entire SponsoredAdView to VISIBLE in handleResume");
        
        // Post a delayed action to ensure UI is updated
        handler.postDelayed(() -> {
            // Double-check visibility after layout
            if (getVisibility() != VISIBLE) {
                Log.d(TAG, "TRACKING: View still not VISIBLE after delay, forcing VISIBLE");
                setVisibility(VISIBLE);
            }
            
            // Force show to parent if nested in another view
            ViewParent parent = getParent();
            if (parent instanceof View) {
                ((View) parent).setVisibility(VISIBLE);
                Log.d(TAG, "TRACKING: Set parent view VISIBLE");
            }
            
            // If we have an ad, ensure its visibility
            if (currentAd != null && adImage != null) {
                adImage.setVisibility(VISIBLE);
                adImage.setAlpha(1.0f);
                Log.d(TAG, "TRACKING: Forced adImage VISIBLE with alpha=1.0");
                
                // Force a refresh of the image using the optimized method
                String imageUrl = currentAd.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Log.d(TAG, "TRACKING: Forcing fresh image load in post-resume check");
                    // Reset retry count for this fresh load
                    imageLoadRetryCount.set(0);
                    lastImageLoadAttempt = System.currentTimeMillis();
                    loadImageOptimized(imageUrl, adImage);
                }
            }
        }, 100); // Short delay to allow layout pass to complete
        
        // Enable rotation if previously enabled
        if (enableRotation.get()) {
            Log.d(TAG, "TRACKING: Resuming rotation in handleResume");
            enableRotation(true);
        }
        
        // Ensure immediate visibility of current content
        if (currentAd != null) {
            Log.d(TAG, "TRACKING: Current ad is not null in handleResume: " + currentAd.getId());
            // Force current ad to reload image to ensure visibility
            if (adImage != null) {
                // First, make sure the image view is visible
                adImage.setVisibility(VISIBLE);
                adImage.setAlpha(1.0f); // Force full opacity
                Log.d(TAG, "TRACKING: Set adImage VISIBLE and alpha=1.0 in handleResume");
                
                // If we have a valid image URL, reload it with high priority
                if (currentAd.getImageUrl() != null && !currentAd.getImageUrl().isEmpty()) {
                    Log.d(TAG, "TRACKING: Reloading image in handleResume: " + currentAd.getImageUrl());
                    
                    // First clear any existing requests
                    try {
                        Context appContext = getContext().getApplicationContext();
                        if (appContext != null) {
                            Glide.with(appContext).clear(adImage);
                            Log.d(TAG, "TRACKING: Cleared existing Glide requests in handleResume");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "TRACKING: Error clearing image in handleResume: " + e.getMessage());
                    }
                    
                    // Short delay to ensure clear completes, then force reload with highest priority
                    handler.postDelayed(() -> {
                        try {
                            Log.d(TAG, "TRACKING: Executing delayed image reload in handleResume");
                            // Use application context to avoid memory leaks
                            Context appContext = getContext().getApplicationContext();
                            // Create a high-priority request for immediate loading
                            RequestOptions options = new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false)
                                .centerCrop()
                                .override(Target.SIZE_ORIGINAL)
                                .priority(Priority.IMMEDIATE); // Highest priority
                                
                                // Load directly into the image view
                                Glide.with(appContext)
                                    .load(currentAd.getImageUrl())
                                    .apply(options)
                                    .into(adImage);
                                
                            Log.d(TAG, "TRACKING: Image reload request completed in handleResume");
                        } catch (Exception e) {
                            Log.e(TAG, "TRACKING: Error reloading image in handleResume: " + e.getMessage());
                            // Fallback to standard loading
                            loadImageOptimized(currentAd.getImageUrl(), adImage);
                            Log.d(TAG, "TRACKING: Falling back to optimized image loading in handleResume");
                        }
                    }, 50);
                } else {
                    Log.w(TAG, "TRACKING: currentAd has null/empty imageUrl in handleResume");
                }
                
                // Always keep title and description hidden as requested
                if (adTitle != null) {
                    adTitle.setVisibility(GONE);
                    Log.d(TAG, "TRACKING: Set adTitle to GONE in handleResume");
                }
                if (adDescription != null) {
                    adDescription.setVisibility(GONE);
                    Log.d(TAG, "TRACKING: Set adDescription to GONE in handleResume");
                }
                
                // Ensure sponsored label is visible
                if (sponsoredLabel != null) {
                    sponsoredLabel.setVisibility(VISIBLE);
                    Log.d(TAG, "TRACKING: Set sponsoredLabel to VISIBLE in handleResume");
                }
            } else {
                Log.w(TAG, "TRACKING: adImage is null in handleResume");
            }
        } else {
            // No current ad, refresh from repository
            Log.d(TAG, "TRACKING: No current ad in handleResume, refreshing from repository");
            refreshAds();
        }
        
        // Update UI state to ensure proper visibility
        setState(ViewState.LOADED);
    }

    /**
     * Force reload the current ad
     * This is useful when returning to a fragment to ensure the ad is displayed correctly
     */
    public void forceReload() {
        if (currentAd != null) {
            Log.d(TAG, "TRACKING: Forcing reload of current ad: " + currentAd.getId());
            
            // Set loading state immediately
            setState(ViewState.LOADING);
            
            // Force this view and all parents to be visible
            forceViewVisible();
            
            // Clear any pending callbacks to avoid race conditions
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                Log.d(TAG, "TRACKING: Cleared all pending callbacks in forceReload");
            }
            
            // Reset image loading state
            imageLoadRetryCount.set(0);
            lastImageLoadAttempt = System.currentTimeMillis();
            
            // Use the optimized image loading method
            if (currentAd.getImageUrl() != null && !currentAd.getImageUrl().isEmpty()) {
                loadImageOptimized(currentAd.getImageUrl(), adImage);
            } else {
                Log.w(TAG, "Cannot force reload: null/empty image URL");
                setState(ViewState.ERROR);
            }
        } else {
            Log.w(TAG, "Cannot force reload: null ad");
            refreshAds();
        }
    }

    /**
     * Ensure text visibility states are correct
     */
    public void ensureTextVisible() {
        if (adTitle != null) {
            adTitle.setVisibility(GONE);
            Log.d(TAG, "TRACKING: Set adTitle to GONE in ensureTextVisible");
        }
        
        if (adDescription != null) {
            adDescription.setVisibility(GONE);
            Log.d(TAG, "TRACKING: Set adDescription to GONE in ensureTextVisible");
        }
        
        // Ensure sponsored label is visible
        if (sponsoredLabel != null) {
            sponsoredLabel.setVisibility(VISIBLE);
            Log.d(TAG, "TRACKING: Set sponsoredLabel to VISIBLE in ensureTextVisible");
        }
    }

    /**
     * Called when the view's lifecycle owner resumes
     * This combines both rotation and content refresh functionality
     */
    public void onResume() {
        Log.d(TAG, "onResume called");
        
        // Ensure the view itself is visible
        setVisibility(VISIBLE);
        
        // Enable rotation if previously enabled
        if (enableRotation.get()) {
            Log.d(TAG, "Resuming rotation in onResume");
            enableRotation(true);
        }
        
        // Similar to handleResume, ensure immediate visibility of current content
        if (currentAd != null) {
            // Force current ad to reload image to ensure visibility
            if (adImage != null) {
                adImage.setVisibility(VISIBLE);
                
                // If we have a valid image URL, reload it directly
                if (currentAd.getImageUrl() != null && !currentAd.getImageUrl().isEmpty()) {
                    Log.d(TAG, "Reloading image in onResume: " + currentAd.getImageUrl());
                    
                    // Use application context to avoid memory leaks
                    try {
                        Context appContext = getContext().getApplicationContext();
                        // Create a high-priority request for immediate loading
                        RequestOptions options = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .centerCrop()
                            .priority(Priority.IMMEDIATE); // Highest priority
                            
                        // Load directly into the image view
                        Glide.with(appContext)
                            .load(currentAd.getImageUrl())
                            .apply(options)
                            .into(adImage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reloading image in onResume: " + e.getMessage());
                        // Fallback to standard loading
                        loadImageOptimized(currentAd.getImageUrl(), adImage);
                    }
                }
                
                // Always keep title and description hidden as requested
                if (adTitle != null) adTitle.setVisibility(GONE);
                if (adDescription != null) adDescription.setVisibility(GONE);
                
                // Ensure sponsored label is visible
                if (sponsoredLabel != null) sponsoredLabel.setVisibility(VISIBLE);
            }
        } else {
            // No current ad, refresh from repository
            refreshAds();
        }
        
        // Update UI state
        setState(ViewState.LOADED);
    }

    /**
     * Reset impression tracking for the current ad
     */
    public void resetImpressionTracking() {
        Log.d(TAG, "Manually resetting impression tracking for ad: " + 
              (currentAd != null ? currentAd.getId() : "null"));
              
        impressionTracked.set(false);
        
        // Clear from session manager if we have a current ad
        if (currentAd != null) {
            try {
                AdSessionManager.getInstance(getContext()).resetTracking(currentAd.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error clearing impression tracking: " + e.getMessage());
            }
        }
        
        // If the ad is currently visible, this will allow for re-impression
        if (isVisibleToUser.get() && adFullyLoaded.get()) {
            Log.d(TAG, "Ad is currently visible, scheduling new impression check");
            handler.removeCallbacks(impressionCheckRunnable);
            visibleSince = System.currentTimeMillis();
            handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
        }
    }
    
    /**
     * Create a new impression session - use when app is restarted
     */
    public void createNewImpressionSession() {
        Log.d(TAG, "Creating new impression tracking session");
        
        try {
            // Use AdSessionManager to create a new session
            AdSessionManager.getInstance(getContext()).createNewSession();
            
            // Reset current tracking state to allow new impressions
            impressionTracked.set(false);
            
            // If the ad is currently visible, schedule a new impression check
            if (isVisibleToUser.get() && adFullyLoaded.get()) {
                handler.removeCallbacks(impressionCheckRunnable);
                visibleSince = System.currentTimeMillis();
                handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
                Log.d(TAG, "Ad is currently visible, scheduling new impression check");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating new impression session: " + e.getMessage());
        }
    }

    /**
     * Force reload and refresh the ad content completely
     * This is a convenience method that combines multiple operations:
     * 1. Reset impression tracking
     * 2. Fetch new ads from server
     * 3. Force reload of current ad
     */
    public void forceCompleteRefresh() {
        Log.d(TAG, "Performing complete forced refresh of ad");
        
        // 1. Reset impression tracking to allow for new impressions
        resetImpressionTracking();
        
        // 2. Reset tracking state and set loading state
        resetTrackingState();
        setState(ViewState.LOADING);
        
        // 3. Ensure visibility of the view and its parents
        forceViewVisible();
        
        // 4. If we have a view model, request fresh ads from server
        if (viewModel != null) {
            Log.d(TAG, "Requesting fresh ads from server");
            viewModel.fetchSponsoredAds();
        }
        
        // 5. If we have a current ad, also force reload its image
        if (currentAd != null && currentAd.getImageUrl() != null) {
            Log.d(TAG, "Force reloading image for current ad: " + currentAd.getId());
            try {
                // Get application context to avoid memory leaks
                Context appContext = getContext().getApplicationContext();
                if (appContext != null && adImage != null) {
                    // Clear existing requests and reset image
                    Glide.with(appContext).clear(adImage);
                    adImage.setImageResource(R.drawable.placeholder_image);
                    adImage.setVisibility(VISIBLE);
                    adImage.setAlpha(1.0f);
                    
                    // Load with highest priority
                    loadImageOptimized(currentAd.getImageUrl(), adImage);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reloading image in force refresh: " + e.getMessage());
            }
        }
        
        // 6. Double check after a delay to ensure visibility
        handler.postDelayed(() -> {
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
            }
            
            if (adImage != null) {
                adImage.setVisibility(VISIBLE);
                adImage.setAlpha(1.0f);
            }
            
            setState(ViewState.LOADED);
            ensureTextVisible();
            
            Log.d(TAG, "Completed force refresh checks");
        }, 500);
    }

    /**
     * Cleanup method to be called when the view is no longer needed
     * Implements comprehensive resource cleanup to prevent memory leaks
     */
    public void cleanup() {
        Log.d(TAG, "Performing comprehensive cleanup");
        
        // Unregister from factory if we have a location
        if (location != null) {
            try {
                SponsoredAdManagerFactory.getInstance().unregisterAdView(location);
                Log.d(TAG, "Unregistered ad view for location: " + location);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error during factory unregistration: " + e.getMessage());
            }
        }
        
        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Cleared all handler callbacks");
        }
        
        // Clear Glide resources
        try {
            Context context = getContext();
            if (context != null && adImage != null) {
                Glide.with(context.getApplicationContext()).clear(adImage);
                Log.d(TAG, "Cleared Glide resources for adImage");
            }
            
            if (context != null && previousAdImage != null) {
                Glide.with(context.getApplicationContext()).clear(previousAdImage);
                Log.d(TAG, "Cleared Glide resources for previousAdImage");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing Glide resources: " + e.getMessage());
        }
        
        // Stop rotation and clean up animation resources
        if (viewModel != null) {
            if (enableRotation.get()) {
                viewModel.stopRotation();
                Log.d(TAG, "Stopped rotation in ViewModel");
            }
            isRotationInitialized.set(false);
        }
        
        // Clear any animations
        if (adImage != null) {
            adImage.clearAnimation();
        }
        
        if (previousAdImage != null) {
            previousAdImage.clearAnimation();
            if (cardContainer != null) {
                cardContainer.removeView(previousAdImage);
                Log.d(TAG, "Removed previousAdImage from view hierarchy");
            }
        }
        
        // Remove visibility tracking
        removeVisibilityTracking();
        
        // Release references to objects that could cause leaks
        currentAd = null;
        previousAd = null;
        previousAdImage = null;
        nextRotationAd = null;
        connectivityChecker = null;
        viewModel = null;
        
        Log.d(TAG, "All resources cleaned up");
    }

    /**
     * Refresh ads from the server with error recovery
     */
    public void refreshAds() {
        if (viewModel != null) {
            // Reset tracking state
            resetTrackingState();
            
            // Show loading state
            setState(ViewState.LOADING);
            
            try {
                // Attempt to fetch new ads
                viewModel.fetchSponsoredAds();
                Log.d(TAG, "Refreshing ads from repository");
                
                // If we got here, reset retry count on successful operation
                if (operationRetryCount.get() > 0) {
                    Log.d(TAG, "Resetting retry count after successful operation");
                    operationRetryCount.set(0);
                }
            } catch (Exception e) {
                // Handle any exceptions during refresh
                Log.e(TAG, "Error refreshing ads: " + e.getMessage());
                handleError("Failed to refresh ads: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Cannot refresh ads: viewModel is null");
            handleError("Cannot refresh ads: viewModel is null");
        }
    }

    /**
     * Set the error state and initiate error recovery if needed
     * @param errorMessage Error message to display
     */
    private void handleError(String errorMessage) {
        Log.e(TAG, "Error: " + errorMessage);
        
        // Update UI to show error state
        setState(ViewState.ERROR);
        
        // Show error container if available
        if (errorContainer != null) {
            errorContainer.setVisibility(VISIBLE);
        }
        
        // Track metrics if enabled
        if (LOG_METRICS) {
            Map<String, Object> errorMetrics = new HashMap<>();
            errorMetrics.put("error_message", errorMessage);
            errorMetrics.put("retry_count", operationRetryCount.get());
            errorMetrics.put("timestamp", System.currentTimeMillis());
            Log.d(TAG, "ERROR_METRICS: " + errorMetrics);
        }
        
        // Initiate recovery if not already in progress
        if (!isInErrorRecovery.get() && operationRetryCount.get() < MAX_RETRY_ATTEMPTS) {
            recoverWithExponentialBackoff();
        } else if (operationRetryCount.get() >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Exceeded maximum retry attempts (" + MAX_RETRY_ATTEMPTS + "), giving up");
            operationRetryCount.set(0);
            isInErrorRecovery.set(false);
        }
    }

    /**
     * Implement exponential backoff for recovery from errors
     */
    private void recoverWithExponentialBackoff() {
        isInErrorRecovery.set(true);
        int currentRetry = operationRetryCount.incrementAndGet();
        
        // Calculate delay with exponential backoff: base * 2^retryCount
        long delay = (long) (BASE_RETRY_DELAY_MS * Math.pow(2, currentRetry - 1));
        
        // Cap at maximum delay
        delay = Math.min(delay, MAX_RETRY_DELAY_MS);
        
        Log.d(TAG, "Scheduling retry attempt " + currentRetry + 
              " of " + MAX_RETRY_ATTEMPTS + " in " + delay + "ms");
        
        // Schedule retry
        handler.postDelayed(() -> {
            Log.d(TAG, "Executing retry attempt " + currentRetry);
            isInErrorRecovery.set(false);
            
            // Hide error container
            if (errorContainer != null) {
                errorContainer.setVisibility(GONE);
            }
            
            // Retry operation
            refreshAds();
        }, delay);
    }

    /**
     * Toggle debug mode on or off
     * @param enableDebug Whether to enable debug mode
     */
    public static void setDebugMode(boolean enableDebug) {
        DEBUG_MODE = enableDebug;
        Log.i(TAG, "Debug mode set to: " + DEBUG_MODE);
    }
    
    /**
     * Check if debug mode is enabled
     * @return true if debug mode is enabled
     */
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    /**
     * Get the next ad for rotation from the ViewModel
     * @param location Location identifier
     * @param excludeIds Set of ad IDs to exclude
     * @return Next ad for rotation, or null if not available
     */
    private SponsoredAd getNextAdForRotation(String location, Set<String> excludeIds) {
        if (viewModel == null) return null;
        
        try {
            // Call the repository through ViewModel
            return viewModel.getNextAdForRotation(location, excludeIds);
        } catch (Exception e) {
            Log.e(TAG, "Error getting next ad for rotation: " + e.getMessage());
            return null;
        }
    }

    /**
     * Called when an ad image has loaded, with rotation transition if needed
     */
    protected void onAdImageLoaded() {
        Log.d(TAG, "TRACKING: onAdImageLoaded called" + (enableRotation.get() ? " with rotation enabled" : ""));
        
        // First check if we have a valid ad with status=true before showing anything
        if (currentAd == null || !currentAd.isStatus()) {
            Log.d(TAG, "Ad is null or status is false, keeping view hidden even though image loaded");
            setVisibility(GONE);
            return;
        }
        
        // Now we can safely show the view since we have a valid ad with image loaded
        setVisibility(VISIBLE);
        
        // Now we can safely show the container
        if (cardContainer != null) {
            cardContainer.setVisibility(VISIBLE);
        }
        
        // Also show the sponsored label
        if (sponsoredLabel != null) {
            sponsoredLabel.setVisibility(VISIBLE);
        }
        
        if (enableRotation.get() && previousAdImage != null) {
            Log.d(TAG, "TRACKING: Performing crossfade animation between ads");
            
            // Enhanced crossfade animation for rotations
            adImage.setAlpha(0f);
            adImage.setVisibility(VISIBLE);
            
            // Use optimized property animation instead of animate() for better performance
            ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(0f, 1f);
            fadeInAnimator.setDuration((long) CROSSFADE_ROTATION_DURATION_MS);
            fadeInAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            fadeInAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                if (adImage != null) {
                    adImage.setAlpha(value);
                }
            });
            fadeInAnimator.start();
            
            // Fade out old image simultaneously
            ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(1f, 0f);
            fadeOutAnimator.setDuration((long) CROSSFADE_ROTATION_DURATION_MS);
            fadeOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            fadeOutAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                if (previousAdImage != null) {
                    previousAdImage.setAlpha(value);
                }
            });
            fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cardContainer != null && previousAdImage != null) {
                        cardContainer.removeView(previousAdImage);
                        Log.d(TAG, "TRACKING: Removed previous ad image after animation");
                    }
                    previousAdImage = null;
                    previousAd = null;
                    
                    // Update text fields with new ad content
                    updateTextVisibility();
                }
            });
            fadeOutAnimator.start();
            
        } else {
            // Standard loading behavior for first load
            Log.d(TAG, "TRACKING: Performing standard visibility animation for first ad load");
            animateVisibility(adImage, true);
            
            // Update text fields with new ad content
            updateTextVisibility();
        }
        
        // Update UI state
        setState(ViewState.LOADED);
        adFullyLoaded.set(true);
        
        // Direct check for visibility to trigger impression tracking
        if (isShown() && getVisibility() == VISIBLE) {
            Log.d(TAG, "TRACKING: Ad is immediately visible, directly checking for impression");
            if (isActuallyVisibleOnScreen()) {
                // If the ad is already visible on screen, initiate tracking directly
                Log.d(TAG, "TRACKING: Ad is actually visible on screen, marking visible and starting timer");
                isVisibleToUser.set(true);
                visibleSince = System.currentTimeMillis();
                handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
                setupVisibilityTracking(); // Set up tracking for future visibility changes
            }
        } else {
            // Set up tracking for when the view becomes visible
            Log.d(TAG, "TRACKING: Ad not immediately visible, setting up visibility tracking");
            setupVisibilityTracking();
        }
    }

    /**
     * Preload the next rotation ad image for smoother transitions
     */
    private void preloadNextRotationAd() {
        if (!PRELOAD_NEXT_ROTATION || !enableRotation.get() || isPreloadingNextAd.get()) {
            return;
        }
        
        isPreloadingNextAd.set(true);
        
        // Get next rotation ad in background
        if (viewModel != null && location != null) {
            executors.diskIO().execute(() -> {
                try {
                    // Get ad rotation info from repository via view model
                    Set<String> excludeIds = new HashSet<>();
                    if (currentAd != null) {
                        excludeIds.add(currentAd.getId());
                    }
                    
                    // Get next ad candidate from repository
                    SponsoredAd nextAd = viewModel.getNextAdForRotation(location, excludeIds);
                    
                    if (nextAd != null && nextAd.getImageUrl() != null) {
                        // Store for later use
                        nextRotationAd = nextAd;
                        
                        // Preload image on main thread
                        handler.post(() -> {
                            Context context = getContext();
                            if (context != null) {
                                Log.d(TAG, "Preloading next rotation ad image: " + nextAd.getId());
                                
                                // Preload using Glide's preload capability
                                Glide.with(context.getApplicationContext())
                                    .load(nextAd.getImageUrl())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .priority(Priority.LOW) // Use low priority to not interfere with current loads
                                    .preload();
                            }
                            isPreloadingNextAd.set(false);
                        });
                    } else {
                        isPreloadingNextAd.set(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error preloading next rotation ad: " + e.getMessage());
                    isPreloadingNextAd.set(false);
                }
            });
        }
    }

    /**
     * Handle visibility changes for proper impression tracking
     */
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        
        Log.d(TAG, "onVisibilityChanged called: " + (visibility == VISIBLE ? "VISIBLE" : 
              visibility == INVISIBLE ? "INVISIBLE" : "GONE") + 
              ", isShown=" + isShown() + 
              ", changedView=" + changedView.getClass().getSimpleName());
        
        // Only proceed if it's us that changed
        if (changedView != this) return;
        
        if (visibility == VISIBLE && isShown()) {
            // Check if we're actually visible on screen
            if (!isVisibleToUser.get() && isActuallyVisibleOnScreen()) {
                Log.d(TAG, "TRACKING: View became visible in onVisibilityChanged");
                onBecameVisible();
            }
        } else {
            // If we're not visible, stop tracking impressions
            if (isVisibleToUser.get()) {
                Log.d(TAG, "TRACKING: View became invisible in onVisibilityChanged");
                isVisibleToUser.set(false);
                
                // Remove any pending impression checks
                if (handler != null) {
                    handler.removeCallbacks(impressionCheckRunnable);
                }
            }
        }
    }

    /**
     * Load ad data into the view 
     * @param ad The sponsored ad to display
     */
    private void loadAd(SponsoredAd ad) {
        if (ad == null) {
            Log.e(TAG, "Cannot load null ad");
            setVisibility(GONE);
            return;
        }
        
        // Check ad status - hide completely if status is false
        if (!ad.isStatus()) {
            Log.d(TAG, "Ad status is false, hiding view completely: " + ad.getId());
            setVisibility(GONE);
            return;
        }
        
        // Keep the view GONE until the image is fully loaded
        setVisibility(GONE);
        
        // Save current ad reference
        this.currentAd = ad;
        
        // Reset tracking state
        resetTrackingState();
        
        // Update labels but keep them hidden
        if (adTitle != null) {
            adTitle.setText(ad.getTitle());
            adTitle.setVisibility(GONE);
        }
        
        if (adDescription != null) {
            adDescription.setText(ad.getDescription());
            adDescription.setVisibility(GONE);
        }
        
        // Keep sponsored label hidden until image loads
        if (sponsoredLabel != null) {
            sponsoredLabel.setVisibility(GONE);
        }
        
        // Keep container hidden until image loads
        if (cardContainer != null) {
            cardContainer.setVisibility(GONE);
        }
        
        // Load image with optimized approach - view will be shown in onAdImageLoaded
        if (ad.getImageUrl() != null && !ad.getImageUrl().isEmpty()) {
            loadImageOptimized(ad.getImageUrl(), adImage);
        } else {
            Log.e(TAG, "Ad has no image URL: " + ad.getId());
            setVisibility(GONE);
        }
        
        // Update the container click listener
        if (cardContainer != null) {
            cardContainer.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.handleAdClick(ad, getContext());
                }
            });
        }
    }

    /**
     * Load image with optimized approach and retry mechanism
     * @param imageUrl The URL of the image to load
     * @param imageView The ImageView to load into
     */
    private void loadImageOptimized(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty() || imageView == null) {
            Log.e(TAG, "Cannot load image: " + (imageUrl == null ? "null URL" : 
                  imageView == null ? "null ImageView" : "empty URL"));
            setState(ViewState.ERROR);
            return;
        }
        
        Log.d(TAG, "Loading image: " + imageUrl);
        
        // Set loading state
        setState(ViewState.LOADING);
        
        // Get application context to prevent memory leaks
        Context appContext = getContext().getApplicationContext();
        
        // Calculate retry parameters
        int currentRetry = imageLoadRetryCount.getAndIncrement();
        boolean isRetry = currentRetry > 0;
        
        // Log retry information if applicable
        if (isRetry) {
            Log.d(TAG, "Retrying image load (attempt " + currentRetry + 
                  " of " + MAX_IMAGE_RETRY_COUNT + "): " + imageUrl);
        }
        
        try {
            // Configure Glide with optimized settings
            RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .priority(isRetry ? Priority.HIGH : Priority.NORMAL);
                
            // Add retry listener
            RequestListener<android.graphics.drawable.Drawable> requestListener = 
                new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                               Target<android.graphics.drawable.Drawable> target,
                                               boolean isFirstResource) {
                        Log.e(TAG, "Image load failed for " + imageUrl, e);
                        
                        // Record failure time
                        lastImageLoadAttempt = System.currentTimeMillis();
                        
                        // Retry if under max attempts
                        if (imageLoadRetryCount.get() <= MAX_IMAGE_RETRY_COUNT) {
                            Log.d(TAG, "Scheduling retry for image: " + imageUrl);
                            handler.postDelayed(() -> {
                                if (getContext() != null) {
                                    loadImageOptimized(imageUrl, imageView);
                                }
                            }, IMAGE_RETRY_DELAY_MS);
                            return true;
                        } else {
                            Log.e(TAG, "Exceeded max retries for image: " + imageUrl);
                            setState(ViewState.ERROR);
                            return false;
                        }
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                  Object model, Target<android.graphics.drawable.Drawable> target,
                                                  DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Image loaded successfully: " + imageUrl);
                        
                        // Reset retry count on success
                        imageLoadRetryCount.set(0);
                        
                        // Update state
                        adFullyLoaded.set(true);
                        setState(ViewState.LOADED);
                        
                        // Handle post-load actions
                        onAdImageLoaded();
                        return false;
                    }
                };
                
            // Load with Glide using transition and listener
            Glide.with(appContext)
                .load(imageUrl)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade(FADE_DURATION))
                .listener(requestListener)
                .into(imageView);
                
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            setState(ViewState.ERROR);
        }
    }

    /**
     * Setup detailed visibility tracking for the view
     */
    private void setupVisibilityTracking() {
        // Don't add duplicates
        removeVisibilityTracking();
        
        // Create global layout listener
        globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getVisibility() == VISIBLE && isShown()) {
                    boolean wasVisible = isVisibleToUser.get();
                    boolean isNowVisible = isActuallyVisibleOnScreen();
                    
                    // If visibility changed, handle it
                    if (!wasVisible && isNowVisible) {
                        Log.d(TAG, "TRACKING: View became visible in onGlobalLayout");
                        isVisibleToUser.set(true);
                        visibleSince = System.currentTimeMillis();
                        handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
                    } else if (wasVisible && !isNowVisible) {
                        Log.d(TAG, "TRACKING: View became invisible in onGlobalLayout");
                        isVisibleToUser.set(false);
                        handler.removeCallbacks(impressionCheckRunnable);
                    }
                }
            }
        };
        
        // Create draw listener for more precise tracking
        drawListener = new ViewTreeObserver.OnDrawListener() {
            @Override
            public void onDraw() {
                if (getVisibility() == VISIBLE && isShown() && !isVisibleToUser.get()) {
                    // Double-check if the view is actually visible on screen
                    if (isActuallyVisibleOnScreen()) {
                        Log.d(TAG, "TRACKING: View became visible in onDraw");
                        isVisibleToUser.set(true);
                        visibleSince = System.currentTimeMillis();
                        handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
                    }
                }
            }
        };
        
        // Add the listeners to the view tree observer
        ViewTreeObserver observer = getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(globalLayoutListener);
            observer.addOnDrawListener(drawListener);
            Log.d(TAG, "TRACKING: Visibility tracking set up");
        }
    }

    /**
     * Remove visibility tracking listeners to prevent memory leaks
     */
    private void removeVisibilityTracking() {
        // Remove callbacks first
        if (handler != null) {
            handler.removeCallbacks(impressionCheckRunnable);
            Log.d(TAG, "TRACKING: Removed impression check callbacks, set isVisibleToUser=false");
        }
        
        // Set invisible before removing listeners
        isVisibleToUser.set(false);
        
        // Remove layout listener
        if (globalLayoutListener != null) {
            ViewTreeObserver observer = getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnGlobalLayoutListener(globalLayoutListener);
            }
            globalLayoutListener = null;
        }
        
        // Remove draw listener
        if (drawListener != null) {
            ViewTreeObserver observer = getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnDrawListener(drawListener);
            }
            drawListener = null;
        }
        
        Log.d(TAG, "TRACKING: Visibility tracking removed");
    }

    /**
     * Force this view and all parent views to be visible
     */
    private void forceViewVisible() {
        // Make this view visible
        setVisibility(VISIBLE);
        
        // Force all parent views to be visible too
        ViewParent current = getParent();
        while (current instanceof View) {
            View parent = (View) current;
            if (parent.getVisibility() != VISIBLE) {
                parent.setVisibility(VISIBLE);
                Log.d(TAG, "TRACKING: Set parent view VISIBLE: " + parent.getClass().getSimpleName());
            }
            current = parent.getParent();
        }
        
        // Ensure card container is visible
        if (cardContainer != null) {
            cardContainer.setVisibility(VISIBLE);
        }
        
        // Ensure image is visible
        if (adImage != null) {
            adImage.setVisibility(VISIBLE);
            adImage.setAlpha(1.0f);
        }
        
        Log.d(TAG, "TRACKING: Force set all parent views to VISIBLE");
    }

    /**
     * Animate visibility changes smoothly
     * @param view The view to animate
     * @param visible Whether to make it visible or invisible
     */
    private void animateVisibility(final View view, final boolean visible) {
        if (view == null) return;
        
        // Cancel any ongoing animations
        view.clearAnimation();
        
        // Create fade animation
        AlphaAnimation animation = new AlphaAnimation(
            visible ? 0.0f : 1.0f,
            visible ? 1.0f : 0.0f
        );
        
        // Configure animation
        animation.setDuration(FADE_DURATION);
        animation.setFillAfter(true);
        
        // Add listener for cleanup
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (visible) {
                    view.setVisibility(VISIBLE);
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                if (!visible) {
                    view.setVisibility(GONE);
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed
            }
        });
        
        // Start animation
        view.startAnimation(animation);
    }

    /**
     * Update text field visibility based on current settings
     */
    private void updateTextVisibility() {
        // Hide title and description by default
        if (adTitle != null) {
            adTitle.setVisibility(GONE);
        }
        
        if (adDescription != null) {
            adDescription.setVisibility(GONE);
        }
        
        // Always show sponsored label
        if (sponsoredLabel != null) {
            sponsoredLabel.setVisibility(VISIBLE);
        }
    }

    /**
     * Set the view model for this ad view
     * @param viewModel The view model to use
     */
    public void setViewModel(SponsoredAdViewModel viewModel) {
        this.viewModel = viewModel;
        Log.d(TAG, "ViewModel set");
    }
    
    /**
     * Set the location for this ad view
     * @param location The location identifier
     */
    public void setLocation(String location) {
        this.location = location;
        Log.d(TAG, "Location set to: " + location);
    }

    /**
     * Add this method to force refresh the ad from the server
     * This should be called when the ad isn't showing but should be
     */
    public void forceRefreshFromServer() {
        Log.d(TAG, "Forcing refresh from server for location: " + location);
        
        // Keep view hidden until we get a valid ad
        setVisibility(GONE);
        
        if (viewModel != null) {
            // Force refresh directly from server
            viewModel.forceRefreshAds();
            
            // Reset our state
            currentAd = null;
            resetTrackingState();
            setState(ViewState.LOADING);
        } else {
            Log.e(TAG, "Cannot force refresh - viewModel is null");
        }
    }
} 