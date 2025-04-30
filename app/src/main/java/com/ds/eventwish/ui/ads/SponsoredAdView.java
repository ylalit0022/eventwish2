package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;

/**
 * Custom view for displaying sponsored ads in the UI
 */
public class SponsoredAdView extends FrameLayout {
    private static final String TAG = "SponsoredAdView";
    
    private CardView cardContainer;
    private ImageView adImage;
    private TextView adTitle;
    private TextView adDescription;
    private TextView sponsoredLabel;
    
    private SponsoredAdViewModel viewModel;
    private SponsoredAd currentAd;
    private String location;
    
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
        
        cardContainer = root.findViewById(R.id.sponsored_ad_container);
        adImage = root.findViewById(R.id.sponsored_ad_image);
        adTitle = root.findViewById(R.id.sponsored_ad_title);
        adDescription = root.findViewById(R.id.sponsored_ad_description);
        sponsoredLabel = root.findViewById(R.id.sponsored_label);
        
        setVisibility(GONE); // Hide initially until ad is loaded
        
        // Set click listener on the container
        cardContainer.setOnClickListener(v -> {
            if (viewModel != null && currentAd != null) {
                viewModel.handleAdClick(currentAd, getContext());
            }
        });
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
                    loadAd(ad);
                } else {
                    Log.d(TAG, "No ad received from ViewModel for location: " + location);
                    setVisibility(GONE);
                }
            });
            
            // Observe loading state for UX feedback
            viewModel.getLoadingState().observe(lifecycleOwner, isLoading -> {
                Log.d(TAG, "Ad loading state changed: " + isLoading + " for location: " + location);
                // You could show a loading indicator here if needed
            });
            
            // Observe errors
            viewModel.getError().observe(lifecycleOwner, error -> {
                if (error != null && !error.isEmpty()) {
                    Log.e(TAG, "Error loading sponsored ad: " + error + " for location: " + location);
                    setVisibility(GONE);
                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize sponsored ad view: " + e.getMessage(), e);
            setVisibility(GONE);
        }
    }
    
    /**
     * Load and display a sponsored ad
     * @param ad The ad to display
     */
    private void loadAd(SponsoredAd ad) {
        if (ad == null) {
            Log.e(TAG, "Attempted to load null ad");
            setVisibility(GONE);
            return;
        }
        
        this.currentAd = ad;
        Log.d(TAG, "Loading sponsored ad: id=" + ad.getId() + 
              ", title=" + ad.getTitle() + 
              ", imageUrl=" + ad.getImageUrl() + 
              ", location=" + location);
        
        // Set text content
        adTitle.setText(ad.getTitle());
        adTitle.setVisibility(ad.getTitle() != null && !ad.getTitle().isEmpty() ? VISIBLE : GONE);
        
        adDescription.setText(ad.getDescription());
        adDescription.setVisibility(ad.getDescription() != null && !ad.getDescription().isEmpty() ? VISIBLE : GONE);
        
        // Load image with Glide
        if (ad.getImageUrl() != null && !ad.getImageUrl().isEmpty()) {
            Log.d(TAG, "Loading ad image from URL: " + ad.getImageUrl());
            Glide.with(getContext())
                .load(ad.getImageUrl())
                .apply(new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image))
                .into(adImage);
        } else {
            Log.w(TAG, "Ad has no image URL: " + ad.getId());
            adImage.setImageResource(R.drawable.placeholder_image);
        }
        
        // Show the view
        setVisibility(VISIBLE);
        
        Log.d(TAG, "Successfully displayed sponsored ad: " + ad.getTitle() + " at location: " + location);
    }
    
    /**
     * Refresh ads from the server
     */
    public void refreshAds() {
        if (viewModel != null) {
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
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error during cleanup: " + e.getMessage());
            }
        }
    }
} 