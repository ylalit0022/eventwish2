package com.ds.eventwish.ui.ads;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ds.eventwish.R;

/**
 * Example fragment demonstrating usage of the SponsoredAdView
 */
public class SponsoredAdFragment extends Fragment {
    private SponsoredAdView sponsoredAdView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sponsored_ad, container, false);
        sponsoredAdView = root.findViewById(R.id.sponsored_ad_view);
        
        // Initialize the ad view with location "home_bottom"
        sponsoredAdView.initialize("home_bottom", getViewLifecycleOwner(), requireActivity());
        
        return root;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Optionally refresh ads when fragment becomes visible
        sponsoredAdView.refreshAds();
    }
} 