package com.ds.eventwish.ui.ads;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ds.eventwish.R;
import com.ds.eventwish.data.ads.AdManagerExtended;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

/**
 * Fragment for displaying banner ads
 */
public class BannerAdFragment extends Fragment {
    private static final String TAG = "BannerAdFragment";
    
    private FrameLayout adContainer;
    private AdView adView;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_banner_ad, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Find the ad container
        adContainer = view.findViewById(R.id.banner_ad_container);
        
        // Load the ad
        loadBannerAd();
    }
    
    /**
     * Load a banner ad
     */
    private void loadBannerAd() {
        Log.d(TAG, "Loading banner ad");
        
        // Create an ad view
        adView = new AdView(requireContext());
        adView.setAdUnitId(AdManagerExtended.BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        
        // Set ad listener
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
            }
            
            @Override
            public void onAdOpened() {
                Log.d(TAG, "Banner ad opened");
            }
            
            @Override
            public void onAdClosed() {
                Log.d(TAG, "Banner ad closed");
            }
            
            @Override
            public void onAdClicked() {
                Log.d(TAG, "Banner ad clicked");
            }
            
            @Override
            public void onAdImpression() {
                Log.d(TAG, "Banner ad impression recorded");
            }
        });
        
        // Add the ad view to the container
        adContainer.removeAllViews();
        adContainer.addView(adView);
        
        // Load the ad
        AdRequest adRequest = AdManagerExtended.getAdRequest();
        adView.loadAd(adRequest);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Destroy the ad view
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }
} 