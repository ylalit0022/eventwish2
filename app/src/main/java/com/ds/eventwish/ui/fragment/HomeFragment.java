package com.ds.eventwish.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ds.eventwish.R;
import com.ds.eventwish.ui.ads.SponsoredAdView;

/**
 * Home Fragment demonstrating the usage of sponsored ad rotation
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    
    private SponsoredAdView sponsoredAdView;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Find sponsored ad view in layout
        sponsoredAdView = view.findViewById(R.id.sponsored_ad_view);
        
        // Initialize with rotation enabled and a 15-minute rotation interval
        sponsoredAdView.initialize(
            "home_bottom",   // Ad location identifier
            getViewLifecycleOwner(),  // Lifecycle owner for LiveData observation
            this,  // ViewModelStoreOwner for ViewModel acquisition
            true,  // Enable rotation
            15     // Rotate every 15 minutes
        );
        
        Log.d(TAG, "Initialized sponsored ad view with rotation enabled");
    }
    
    @Override
    public void onDestroyView() {
        // Clean up resources when view is destroyed
        if (sponsoredAdView != null) {
            sponsoredAdView.cleanup();
        }
        super.onDestroyView();
    }
}
