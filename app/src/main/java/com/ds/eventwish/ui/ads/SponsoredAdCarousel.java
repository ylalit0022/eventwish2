package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewpager2.widget.ViewPager2;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.repository.SponsoredAdRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

/**
 * Component for displaying a carousel of sponsored ads with auto-scrolling
 */
public class SponsoredAdCarousel extends FrameLayout {
    private static final String TAG = "SponsoredAdCarousel";
    
    // UI Components
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar loadingView;
    private TextView errorView;
    private ConstraintLayout container;
    
    // Data
    private SponsoredAdRepository repository;
    private SponsoredAdPagerAdapter adapter;
    private SponsoredAdViewModel viewModel;
    
    // Auto-scrolling
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoScrollRunnable = new AutoScrollRunnable();
    private boolean isAutoScrollEnabled = true;
    private static final long AUTO_SCROLL_INTERVAL_MS = 5000; // 5 seconds
    
    // Mediator for connecting TabLayout with ViewPager2
    private TabLayoutMediator tabMediator;
    
    public SponsoredAdCarousel(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public SponsoredAdCarousel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public SponsoredAdCarousel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        try {
            LayoutInflater.from(context).inflate(R.layout.sponsored_ad_carousel, this, true);
            
            // Initialize UI components
            viewPager = findViewById(R.id.ad_viewpager);
            tabLayout = findViewById(R.id.pager_indicator);
            loadingView = findViewById(R.id.carousel_loading);
            errorView = findViewById(R.id.carousel_error);
            container = findViewById(R.id.carousel_container);
            
            // Ensure ViewPager2 has fixed height
            if (viewPager != null) {
                ViewGroup.LayoutParams lp = viewPager.getLayoutParams();
                if (lp != null) {
                    lp.height = getResources().getDimensionPixelSize(R.dimen.sponsored_ad_height);
                    viewPager.setLayoutParams(lp);
                }
                
                // ViewPager2 requires direct child views to use match_parent
                viewPager.setLayoutParams(new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(R.dimen.sponsored_ad_height)
                ));
            }
            
            // Setup adapter
            adapter = new SponsoredAdPagerAdapter(context);
            viewPager.setAdapter(adapter);
            
            // Connect TabLayout with ViewPager2
            tabMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                // No title needed for indicator dots
            });
            tabMediator.attach();
            
            // Hide until we have ads
            showLoading(true);
            
            // Get repository 
            repository = new SponsoredAdRepository();
            
            // Setup page change listener to pause auto-scroll during manual scrolling
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrollStateChanged(int state) {
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        pauseAutoScroll();
                    } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        resumeAutoScroll();
                    }
                }
                
                @Override
                public void onPageSelected(int position) {
                    // Track impression for currently visible ad
                    trackImpressionForCurrentAd();
                }
            });
            
            // Set click listener for ads
            adapter.setOnAdClickListener(ad -> {
                if (viewModel != null && ad != null) {
                    viewModel.handleAdClick(ad, context);
                    Log.d(TAG, "Ad clicked: " + ad.getId());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SponsoredAdCarousel: " + e.getMessage(), e);
            setVisibility(GONE);
        }
    }
    
    /**
     * Load ads from a specific location
     * @param location The location identifier for filtering ads
     */
    public void loadAds(String location) {
        try {
            if (getContext() instanceof LifecycleOwner) {
                LifecycleOwner lifecycleOwner = (LifecycleOwner) getContext();
                
                // Show loading state
                showLoading(true);
                // Ensure container and main view are visible
                setVisibility(VISIBLE);
                
                // Observe all ads from repository
                repository.getSponsoredAdsForLocation(location).observe(lifecycleOwner, ads -> {
                    try {
                        // Hide loading view
                        showLoading(false);
                        
                        if (ads != null && !ads.isEmpty()) {
                            // Filter frequency capped ads
                            List<SponsoredAd> eligibleAds = filterFrequencyCappedAds(ads);
                            
                            if (!eligibleAds.isEmpty()) {
                                // Update adapter with filtered ads
                                adapter.setAds(eligibleAds);
                                
                                // Update tab count
                                tabLayout.setVisibility(eligibleAds.size() > 1 ? View.VISIBLE : View.GONE);
                                
                                // Always ensure carousel is visible when ads are loaded
                                setVisibility(VISIBLE);
                                container.setVisibility(VISIBLE);
                                viewPager.setVisibility(VISIBLE);
                                
                                // Start auto-scrolling if we have multiple ads
                                if (eligibleAds.size() > 1) {
                                    startAutoScroll();
                                }
                                
                                // Track impressions for the first visible ad
                                trackImpressionForCurrentAd();
                                
                                Log.d(TAG, "Loaded " + eligibleAds.size() + " ads for carousel and set to VISIBLE");
                            } else {
                                // No eligible ads after filtering
                                setVisibility(GONE);
                                Log.d(TAG, "No eligible ads after frequency cap filtering, set to GONE");
                            }
                        } else {
                            // No ads available
                            setVisibility(GONE);
                            Log.d(TAG, "No ads available for location: " + location + ", set to GONE");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating ads in carousel: " + e.getMessage());
                        // Don't hide the view on error if we have content
                        if (adapter == null || adapter.getItemCount() == 0) {
                            setVisibility(GONE);
                        }
                    }
                });
                
                // Observe loading state
                repository.getLoadingState().observe(lifecycleOwner, isLoading -> {
                    try {
                        showLoading(isLoading != null && isLoading);
                        // Don't hide the view while loading
                        if (isLoading != null && isLoading) {
                            setVisibility(VISIBLE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating loading state: " + e.getMessage());
                    }
                });
                
                // Observe errors
                repository.getError().observe(lifecycleOwner, errorMessage -> {
                    try {
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            showError(errorMessage);
                            // Don't hide the view on error if we have content
                            if (adapter == null || adapter.getItemCount() == 0) {
                                setVisibility(GONE);
                            }
                        } else {
                            errorView.setVisibility(GONE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing error message: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading ads: " + e.getMessage());
            // Don't hide the view on error if we have content
            if (adapter == null || adapter.getItemCount() == 0) {
                setVisibility(GONE);
            }
        }
    }
    
    /**
     * Filter out frequency capped ads
     * @param ads List of ads to filter
     * @return Filtered list of eligible ads
     */
    private List<SponsoredAd> filterFrequencyCappedAds(List<SponsoredAd> ads) {
        // Create a new list for eligible ads
        List<SponsoredAd> eligibleAds = new java.util.ArrayList<>();
        
        for (SponsoredAd ad : ads) {
            if (!ad.isFrequencyCapped()) {
                eligibleAds.add(ad);
            }
        }
        
        return eligibleAds;
    }
    
    /**
     * Track impression for the currently visible ad
     */
    private void trackImpressionForCurrentAd() {
        try {
            if (viewPager != null && adapter != null && viewModel != null) {
                int currentPosition = viewPager.getCurrentItem();
                if (currentPosition >= 0 && currentPosition < adapter.getItemCount()) {
                    SponsoredAd currentAd = null;
                    
                    try {
                        // Safely get the current ad
                        if (adapter instanceof SponsoredAdPagerAdapter) {
                            currentAd = ((SponsoredAdPagerAdapter) adapter).getAdAt(currentPosition);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting current ad: " + e.getMessage());
                    }
                    
                    if (currentAd != null) {
                        viewModel.trackImpression(currentAd);
                        Log.d(TAG, "Tracked impression for ad at position " + currentPosition);
                        
                        // Ensure carousel remains visible after tracking
                        if (getVisibility() != VISIBLE) {
                            setVisibility(VISIBLE);
                            Log.d(TAG, "Reset visibility to VISIBLE after tracking impression");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking impression: " + e.getMessage());
            // Don't change visibility on tracking error
        }
    }
    
    /**
     * Start auto-scrolling
     */
    public void startAutoScroll() {
        if (!isAutoScrollEnabled) return;
        
        pauseAutoScroll(); // Remove any existing callbacks
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_INTERVAL_MS);
    }
    
    /**
     * Pause auto-scrolling
     */
    public void pauseAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }
    
    /**
     * Resume auto-scrolling
     */
    public void resumeAutoScroll() {
        if (!isAutoScrollEnabled) return;
        
        pauseAutoScroll(); // Remove any existing callbacks
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_INTERVAL_MS);
    }
    
    /**
     * Enable or disable auto-scrolling
     * @param enabled True to enable, false to disable
     */
    public void setAutoScrollEnabled(boolean enabled) {
        isAutoScrollEnabled = enabled;
        if (enabled) {
            startAutoScroll();
        } else {
            pauseAutoScroll();
        }
    }
    
    /**
     * Show loading state
     * @param isLoading True to show loading, false to hide
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingView.setVisibility(VISIBLE);
            errorView.setVisibility(GONE);
        } else {
            loadingView.setVisibility(GONE);
        }
    }
    
    /**
     * Show error message
     * @param message Error message to display
     */
    private void showError(String message) {
        loadingView.setVisibility(GONE);
        errorView.setVisibility(VISIBLE);
        errorView.setText(message);
    }
    
    /**
     * Auto-scroll runnable for advancing to the next ad
     */
    private class AutoScrollRunnable implements Runnable {
        @Override
        public void run() {
            if (viewPager != null && adapter != null && adapter.getItemCount() > 1) {
                int currentItem = viewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % adapter.getItemCount();
                
                viewPager.setCurrentItem(nextItem, true);
                
                // Track impression for the newly visible ad
                trackImpressionForCurrentAd();
                
                // Schedule next auto-scroll
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS);
            }
        }
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAutoScrollEnabled && adapter != null && adapter.getItemCount() > 1) {
            startAutoScroll();
        }
        
        // Force visibility after attachment
        if (adapter != null && adapter.getItemCount() > 0) {
            setVisibility(VISIBLE);
            // Use post for making sure visibility is set after layout
            post(() -> setVisibility(VISIBLE));
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pauseAutoScroll();
        
        // Detach TabLayoutMediator to prevent memory leaks
        if (tabMediator != null) {
            tabMediator.detach();
        }
    }
    
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        
        // If we have content but view is hidden, force show it after a delay
        if (visibility != VISIBLE && adapter != null && adapter.getItemCount() > 0) {
            Log.d(TAG, "Visibility changed to " + visibility + " but we have content, will restore");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (getVisibility() != VISIBLE) {
                    Log.d(TAG, "Restoring visibility after change");
                    setVisibility(VISIBLE);
                }
            }, 500);
        }
    }
    
    /**
     * Initialize the carousel with a location and lifecycle owner
     * @param location The location identifier for filtering ads
     * @param lifecycleOwner Lifecycle owner for observing LiveData
     * @param viewModelStoreOwner ViewModelStoreOwner for getting ViewModel
     */
    public void initialize(String location, LifecycleOwner lifecycleOwner, ViewModelStoreOwner viewModelStoreOwner) {
        try {
            // Make sure carousel is visible during initialization
            setVisibility(VISIBLE);
            
            // Get view model from factory
            this.viewModel = SponsoredAdManagerFactory.getInstance().getViewModel(viewModelStoreOwner);
            
            // Register with factory for analytics tracking - use this as a View, not specifically a SponsoredAdView
            SponsoredAdManagerFactory.getInstance().registerAdView(location, this);
            
            // Set location as tag for identification
            setTag(location);
            
            // Load ads for this location
            loadAds(location);
            
            Log.d(TAG, "Initialized carousel with location: " + location);
            
            // Set a delayed check to ensure the carousel is still visible after initialization
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (getVisibility() != VISIBLE && adapter != null && adapter.getItemCount() > 0) {
                    Log.d(TAG, "Reset carousel visibility after initialization");
                    setVisibility(VISIBLE);
                }
            }, 2000); // Check after 2 seconds
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize sponsored ad carousel: " + e.getMessage());
            // Only hide if we don't have content
            if (adapter == null || adapter.getItemCount() == 0) {
                setVisibility(GONE);
            }
        }
    }
    
    /**
     * Cleanup resources when view is being destroyed
     */
    public void cleanup() {
        pauseAutoScroll();
        
        // Unregister from factory
        String location = getTag() != null ? getTag().toString() : null;
        if (location != null) {
            SponsoredAdManagerFactory.getInstance().unregisterAdView(location);
        }
        
        // Detach TabLayoutMediator to prevent memory leaks
        if (tabMediator != null) {
            tabMediator.detach();
        }
        
        // Remove callbacks to prevent memory leaks
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {});
        }
    }
} 