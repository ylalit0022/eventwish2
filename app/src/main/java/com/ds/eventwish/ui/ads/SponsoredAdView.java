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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;

/**
 * Custom view for displaying sponsored ads in the UI with improved state handling
 */
public class SponsoredAdView extends FrameLayout {
    private static final String TAG = "SponsoredAdView";
    private static final int FADE_DURATION = 300; // Animation duration in ms
    private static final long IMPRESSION_CHECK_DELAY_MS = 500; // Delay before checking visibility
    private static final long IMPRESSION_MIN_VISIBLE_TIME_MS = 1000; // Minimum time ad must be visible
    
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
    private boolean impressionTracked = false;
    private boolean adFullyLoaded = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isVisibleToUser = false;
    private long visibleSince = 0;
    private Runnable impressionCheckRunnable;
    private boolean enableRotation = false;
    private SponsoredAd previousAd;
    private ImageView previousAdImage;
    private boolean isRotationInitialized = false;
    
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
        
        // Create impression check runnable
        impressionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkForValidImpression();
            }
        };
        
        // Set initial state
        setState(ViewState.GONE);
        
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
        if (currentAd == null || !adFullyLoaded || impressionTracked || !isVisibleToUser) {
            Log.d(TAG, "Skipping impression tracking: ad=" + (currentAd != null ? currentAd.getId() : "null") + 
                  ", loaded=" + adFullyLoaded + ", tracked=" + impressionTracked + 
                  ", visible=" + isVisibleToUser);
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
     * Track an impression only once per ad
     */
    private void trackImpression() {
        if (currentAd == null || impressionTracked || !adFullyLoaded || viewModel == null) {
            Log.d(TAG, "Skipping trackImpression: " + 
                  (currentAd == null ? "null ad" : 
                  impressionTracked ? "already tracked" : 
                  !adFullyLoaded ? "not fully loaded" : "null viewModel"));
            return;
        }
        
        // Check if we've already tracked this ad in a previous session using SharedPreferences
        SharedPreferences prefs = getContext().getSharedPreferences("sponsored_ad_tracking", Context.MODE_PRIVATE);
        
        // Use daily key for better tracking granularity - only track once per day per ad
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24); // Days since epoch
        String impressionKey = "impression_view_" + currentAd.getId() + "_" + today;
        boolean alreadyTracked = prefs.getBoolean(impressionKey, false);
        
        if (alreadyTracked) {
            Log.d(TAG, "Impression for ad " + currentAd.getId() + " already tracked today, skipping");
            impressionTracked = true;
            return;
        }
        
        // Mark as tracked
        prefs.edit().putBoolean(impressionKey, true).apply();
        impressionTracked = true;
        
        // Track the impression
        Log.d(TAG, "Tracking impression for ad: " + currentAd.getId() + " in location: " + location);
        viewModel.handleAdImpression(currentAd);
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
        
        // Set loading state immediately
        setState(ViewState.LOADING);
        
        try {
            // Get ViewModel from the factory
            this.viewModel = SponsoredAdManagerFactory.getInstance().getViewModel(viewModelStoreOwner);
            
            // Register this view with the factory
            SponsoredAdManagerFactory.getInstance().registerAdView(location, this);
            Log.d(TAG, "Registered ad view for location: " + location + " with AdManagerFactory");
            
            // Observe ad for the specified location
            viewModel.getAdForLocation(location).observe(lifecycleOwner, ad -> {
                if (ad != null) {
                    Log.d(TAG, "Received ad from ViewModel for location: " + location + 
                          ", id: " + ad.getId() + ", title: " + ad.getTitle());
                    
                    // If we receive a different ad than before, reset tracking state
                    if (currentAd == null || !currentAd.getId().equals(ad.getId())) {
                        resetTrackingState();
                    }
                    
                    loadAd(ad);
                } else {
                    Log.d(TAG, "No ad received from ViewModel for location: " + location);
                    setState(ViewState.GONE);
                }
            });
            
            // Observe loading state for UX feedback
            viewModel.getLoadingState().observe(lifecycleOwner, isLoading -> {
                Log.d(TAG, "Ad loading state changed: " + isLoading + " for location: " + location);
                if (isLoading && currentState == ViewState.GONE) {
                    setState(ViewState.LOADING);
                }
            });
            
            // Observe errors
            viewModel.getError().observe(lifecycleOwner, error -> {
                if (error != null && !error.isEmpty()) {
                    Log.e(TAG, "Error loading sponsored ad: " + error + " for location: " + location);
                    setState(ViewState.ERROR);
                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize sponsored ad view: " + e.getMessage(), e);
            setState(ViewState.GONE);
        }
    }
    
    /**
     * Enable or disable ad rotation
     * @param enable Whether to enable rotation
     */
    public void enableRotation(boolean enable) {
        Log.d(TAG, "Setting rotation enabled: " + enable + " for location: " + location);
        this.enableRotation = enable;
        
        if (enable && viewModel != null && !isRotationInitialized) {
            // Start rotation
            Log.d(TAG, "Starting ad rotation for location: " + location);
            viewModel.startRotation(location);
            isRotationInitialized = true;
            
            // Observe rotating ad for this location
            if (getLifecycleOwner() != null) {
                observeRotatingAd(getLifecycleOwner());
            } else {
                Log.e(TAG, "Cannot observe rotating ads - no lifecycle owner available");
            }
        } else if (!enable && viewModel != null && isRotationInitialized) {
            // Stop rotation
            Log.d(TAG, "Stopping ad rotation");
            viewModel.stopRotation();
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
            if (enableRotation && isRotationInitialized) {
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
     * Handle a rotated ad
     */
    private void handleRotatedAd(SponsoredAd ad) {
        if (ad == null) {
            Log.e(TAG, "Cannot handle null rotated ad");
            return;
        }
        
        // Check if this is the same ad we're already displaying
        if (currentAd != null && ad.getId().equals(currentAd.getId())) {
            Log.d(TAG, "Skipping rotation - received the same ad: " + ad.getId());
            return;
        }
        
        Log.d(TAG, "Handling rotated ad: " + ad.getId() + ", previous: " + 
              (currentAd != null ? currentAd.getId() : "none"));
              
        // Save the current state as previous before updating
        if (adFullyLoaded && currentAd != null) {
            previousAd = currentAd;
            previousAdImage = adImage;
            
            // Make a copy of the current image
            if (previousAdImage != null && cardContainer != null) {
                Log.d(TAG, "Preparing for crossfade animation");
                // Keep the old image in place during transition
                try {
                    // Ensure old image stays where it is
                    previousAdImage.setLayoutParams(
                        new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up previous image for animation", e);
                }
            }
        }
        
        // Reset tracking state for the new ad
        resetTrackingState();
        
        // Create a new ImageView for the new ad
        adImage = new ImageView(getContext());
        adImage.setLayoutParams(
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );
        adImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        adImage.setAlpha(0f); // Start invisible for fade-in
        
        // Add new image to container
        if (cardContainer != null) {
            cardContainer.addView(adImage);
            Log.d(TAG, "Added new image view for ad: " + ad.getId());
        }
        
        // Update model reference
        currentAd = ad;
        
        // Load content
        loadAd(ad);
    }
    
    /**
     * Reset tracking state when a new ad is loaded
     */
    private void resetTrackingState() {
        impressionTracked = false;
        adFullyLoaded = false;
        isVisibleToUser = false;
        visibleSince = 0;
        handler.removeCallbacks(impressionCheckRunnable);
    }
    
    /**
     * Update the view state and UI accordingly
     * @param state The new state to set
     */
    private void setState(ViewState state) {
        // Skip if same state
        if (state == currentState) {
            return;
        }
        
        Log.d(TAG, "Changing ad view state from " + currentState + " to " + state + 
              (currentAd != null ? " for ad: " + currentAd.getId() : ""));
        
        currentState = state;
        
        // Update UI based on state - use animations for better UX
        switch (state) {
            case LOADING:
                // Show loading while hiding other elements
                animateVisibility(loadingView, true);
                animateVisibility(errorContainer, false);
                
                if (currentAd == null) {
                    // Only hide content if we don't have an ad yet
                    animateVisibility(adImage, false);
                    animateVisibility(sponsoredLabel, false);
                    animateVisibility(adTitle, false);
                    animateVisibility(adDescription, false);
                }
                setVisibility(VISIBLE);
                
                // Reset tracking state
                impressionTracked = false;
                adFullyLoaded = false;
                isVisibleToUser = false;
                break;
                
            case LOADED:
                // Hide loading and error, show content
                animateVisibility(loadingView, false);
                animateVisibility(errorContainer, false);
                animateVisibility(adImage, true);
                animateVisibility(sponsoredLabel, true);
                
                // Title and description visibility handled in loadAd()
                setVisibility(VISIBLE);
                
                adFullyLoaded = true;
                
                // Check for visibility after a delay to ensure layout is complete
                handler.removeCallbacks(impressionCheckRunnable);
                handler.postDelayed(() -> {
                    if (isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
                        onBecameVisible();
                    }
                }, IMPRESSION_CHECK_DELAY_MS);
                break;
                
            case ERROR:
                // Hide content and loading, show error
                animateVisibility(loadingView, false);
                animateVisibility(errorContainer, true);
                animateVisibility(adImage, false);
                animateVisibility(sponsoredLabel, false);
                animateVisibility(adTitle, false);
                animateVisibility(adDescription, false);
                setVisibility(VISIBLE);
                
                // Reset tracking state
                adFullyLoaded = false;
                isVisibleToUser = false;
                
                // Only enable retry button if we have network
                retryButton.setEnabled(connectivityChecker.isNetworkAvailable());
                break;
                
            case GONE:
                // Just hide the entire view immediately
                setVisibility(GONE);
                
                // Reset tracking state
                adFullyLoaded = false;
                isVisibleToUser = false;
                break;
        }
    }
    
    /**
     * Called when the view becomes visible to the user
     */
    private void onBecameVisible() {
        if (!adFullyLoaded || impressionTracked) {
            return;
        }
        
        Log.d(TAG, "Ad view became visible: " + (currentAd != null ? currentAd.getId() : "null") + 
              " at location: " + location);
              
        isVisibleToUser = true;
        visibleSince = System.currentTimeMillis();
        
        // Schedule impression check
        handler.removeCallbacks(impressionCheckRunnable);
        handler.postDelayed(impressionCheckRunnable, IMPRESSION_MIN_VISIBLE_TIME_MS);
    }
    
    /**
     * Animate the visibility of a view
     * @param view The view to animate
     * @param show Whether to show or hide the view
     */
    private void animateVisibility(View view, boolean show) {
        if (view == null) return;
        
        if (show) {
            if (view.getVisibility() != VISIBLE) {
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(FADE_DURATION);
                fadeIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        view.setVisibility(VISIBLE);
                    }
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {}
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                view.startAnimation(fadeIn);
            }
        } else {
            if (view.getVisibility() != GONE) {
                AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
                fadeOut.setDuration(FADE_DURATION);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(GONE);
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                view.startAnimation(fadeOut);
            }
        }
    }
    
    /**
     * Load and display a sponsored ad
     * @param ad The ad to display
     */
    private void loadAd(SponsoredAd ad) {
        if (ad == null) {
            Log.e(TAG, "Attempted to load null ad");
            setState(ViewState.ERROR);
            return;
        }
        
        this.currentAd = ad;
        Log.d(TAG, "Loading sponsored ad: id=" + ad.getId() + 
              ", title=" + ad.getTitle() + 
              ", imageUrl=" + ad.getImageUrl() + 
              ", location=" + location);
        
        // Set text content if available
        if (ad.getTitle() != null && !ad.getTitle().isEmpty()) {
            adTitle.setText(ad.getTitle());
            adTitle.setVisibility(VISIBLE);
        } else {
            adTitle.setVisibility(GONE);
        }
        
        if (ad.getDescription() != null && !ad.getDescription().isEmpty()) {
            adDescription.setText(ad.getDescription());
            adDescription.setVisibility(VISIBLE);
        } else {
            adDescription.setVisibility(GONE);
        }
        
        // Always set loading state first and use the listener to monitor loading
        setState(ViewState.LOADING);
        loadImageWithListener(ad.getImageUrl());
        
        Log.d(TAG, "Successfully displayed sponsored ad: " + ad.getTitle() + " at location: " + location);
    }
    
    /**
     * Load image with a listener to handle loading states
     */
    private void loadImageWithListener(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.w(TAG, "No image URL for ad: " + (currentAd != null ? currentAd.getId() : "null"));
            adImage.setImageResource(R.drawable.placeholder_image);
            setState(ViewState.LOADED);
            return;
        }
        
        Log.d(TAG, "Loading ad image from URL: " + imageUrl);
        
        RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .override(Target.SIZE_ORIGINAL) // Use original size to avoid resizing multiple times
            .encodeFormat(Bitmap.CompressFormat.WEBP) // More efficient format for caching
            .encodeQuality(90) // Good quality but smaller size
            .skipMemoryCache(false) // Use memory cache
            .dontAnimate(); // Skip animation for faster loading
        
        Glide.with(getContext())
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade(FADE_DURATION))
            .apply(options)
            .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                            Target<android.graphics.drawable.Drawable> target, 
                                            boolean isFirstResource) {
                    Log.e(TAG, "Failed to load ad image: " + imageUrl, e);
                    setState(ViewState.ERROR);
                    return false;
                }
                
                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                              Object model, 
                                              Target<android.graphics.drawable.Drawable> target, 
                                              DataSource dataSource, 
                                              boolean isFirstResource) {
                    Log.d(TAG, "Ad image loaded successfully: " + imageUrl + 
                          " (source: " + dataSource.name() + ", first resource: " + isFirstResource + ")");
                    // Important: Must set state to LOADED to ensure proper tracking
                    handler.post(() -> {
                        if (currentState != ViewState.LOADED) {
                            setState(ViewState.LOADED);
                        } else {
                            // Even if already LOADED, still check visibility for impression tracking
                            handler.postDelayed(() -> {
                                if (isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
                                    onBecameVisible();
                                }
                            }, IMPRESSION_CHECK_DELAY_MS);
                        }
                    });
                    return false;
                }
            })
            .into(adImage);
    }
    
    /**
     * Refresh ads from the server
     */
    public void refreshAds() {
        if (viewModel != null) {
            resetTrackingState();
            setState(ViewState.LOADING);
            viewModel.fetchSponsoredAds();
        }
    }
    
    /**
     * Cleanup method to be called when the view is no longer needed
     */
    public void cleanup() {
        if (location != null) {
            try {
                SponsoredAdManagerFactory.getInstance().unregisterAdView(location);
                Log.d(TAG, "Unregistered ad view for location: " + location);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error during cleanup: " + e.getMessage());
            }
        }
        
        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacks(impressionCheckRunnable);
        }
        
        // Clear references to help with garbage collection
        viewModel = null;
        connectivityChecker = null;
        
        if (viewModel != null && enableRotation) {
            viewModel.stopRotation();
            isRotationInitialized = false;
        }
        
        // Clear previous image if it exists
        if (previousAdImage != null) {
            if (cardContainer != null) {
                cardContainer.removeView(previousAdImage);
            }
            previousAdImage = null;
            previousAd = null;
        }
    }
    
    /**
     * Called when the view is being destroyed
     * This is a good place to perform final cleanup
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            // Make sure all resources are cleaned up
            cleanup();
            Log.d(TAG, "SponsoredAdView finalized");
        } finally {
            super.finalize();
        }
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        
        // Check connectivity state
        if (connectivityChecker != null && !connectivityChecker.isNetworkAvailable() && currentState == ViewState.ERROR) {
            // Update retry button state
            retryButton.setEnabled(false);
        }
        
        // Check if we need to track visibility
        if (currentState == ViewState.LOADED && adFullyLoaded && !impressionTracked) {
            handler.postDelayed(() -> {
                if (isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
                    onBecameVisible();
                }
            }, IMPRESSION_CHECK_DELAY_MS);
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        Log.d(TAG, "SponsoredAdView detached from window");
        
        // Remove all pending callbacks to prevent leaks
        handler.removeCallbacksAndMessages(null);
        
        // Reset visibility state
        isVisibleToUser = false;
        
        // Ensure Glide requests are cleaned up
        if (adImage != null) {
            Glide.with(getContext()).clear(adImage);
        }
        
        // Clean up any resources
        currentAd = null;
        
        // Explicitly call cleanup to ensure unregistration from factory
        cleanup();
    }
    
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        
        // If this view becomes visible and we have a loaded ad, check for impression tracking
        if (visibility == VISIBLE) {
            if (currentState == ViewState.LOADED && adFullyLoaded && !impressionTracked) {
                handler.postDelayed(() -> {
                    if (isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
                        onBecameVisible();
                    }
                }, IMPRESSION_CHECK_DELAY_MS);
            }
        } else {
            // View is not visible anymore
            isVisibleToUser = false;
            handler.removeCallbacks(impressionCheckRunnable);
        }
    }
    
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        
        // Handle window visibility changes (like when app goes to background)
        if (visibility != VISIBLE) {
            isVisibleToUser = false;
            handler.removeCallbacks(impressionCheckRunnable);
        } else if (currentState == ViewState.LOADED && adFullyLoaded && !impressionTracked) {
            handler.postDelayed(() -> {
                if (isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
                    onBecameVisible();
                }
            }, IMPRESSION_CHECK_DELAY_MS);
        }
    }
    
    /**
     * Handle when an ad image has loaded, with rotation transition if needed
     */
    protected void onAdImageLoaded() {
        Log.d(TAG, "Ad image loaded" + (enableRotation ? " with rotation enabled" : ""));
        
        if (enableRotation && previousAdImage != null) {
            Log.d(TAG, "Performing crossfade animation between ads");
            // Fade in new image
            adImage.animate()
                .alpha(1f)
                .setDuration(FADE_DURATION)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();
            
            // Fade out old image
            previousAdImage.animate()
                .alpha(0f)
                .setDuration(FADE_DURATION)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (cardContainer != null) {
                        cardContainer.removeView(previousAdImage);
                        Log.d(TAG, "Removed previous ad image after animation");
                    }
                    previousAdImage = null;
                    previousAd = null;
                })
                .start();
        } else {
            // Standard loading behavior for first load
            Log.d(TAG, "Performing standard visibility animation for first ad load");
            animateVisibility(adImage, true);
        }
        
        // Update the rest of the UI
        if (loadingView != null) {
            animateVisibility(loadingView, false);
        }
        
        adFullyLoaded = true;
        
        // Check for impressions when image loads
        if (isVisibleToUser) {
            onBecameVisible();
        }
    }
} 