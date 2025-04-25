package com.ds.eventwish.ads;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.R;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

/**
 * Helper class to manage native ad loading and display.
 * Handles the lifecycle of native ads and provides a simple interface for activities/fragments.
 */
public class NativeAdHelper {
    private static final String TAG = "NativeAdHelper";

    private final Context context;
    private final ViewGroup adContainer;
    private final ProgressBar progressBar;
    private NativeAd currentNativeAd;
    private NativeAdView nativeAdView;

    /**
     * Creates a new NativeAdHelper instance.
     *
     * @param context Context to use for inflating views
     * @param adContainer ViewGroup to display the ad in
     * @param progressBar ProgressBar to show during loading
     */
    public NativeAdHelper(@NonNull Context context, @NonNull ViewGroup adContainer, @Nullable ProgressBar progressBar) {
        this.context = context;
        this.adContainer = adContainer;
        this.progressBar = progressBar;
    }

    /**
     * Loads and displays a native ad.
     */
    public void loadAd() {
        showLoading(true);
        
        AdMobManager.getInstance().loadNativeAd(new AdMobManager.NativeAdCallback() {
            @Override
            public void onAdLoaded(NativeAd nativeAd) {
                Log.d(TAG, "Native ad loaded successfully");
                displayNativeAd(nativeAd);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load native ad: " + message);
                showLoading(false);
                // Optionally show error state
                // adContainer.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Displays the loaded native ad.
     *
     * @param nativeAd The native ad to display
     */
    private void displayNativeAd(NativeAd nativeAd) {
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
        }
        currentNativeAd = nativeAd;

        // Inflate native ad layout
        nativeAdView = (NativeAdView) LayoutInflater.from(context)
                .inflate(R.layout.native_ad_layout, null);
        
        // Add to container
        adContainer.removeAllViews();
        adContainer.addView(nativeAdView);

        // Populate the native ad view
        if (AdMobManager.getInstance().populateNativeAdView(nativeAdView)) {
            showLoading(false);
            adContainer.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "Failed to populate native ad view");
            cleanup();
        }
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show True to show loading, false to hide
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (!show) {
            adContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Cleans up resources when the ad is no longer needed.
     * Should be called in onDestroy() of the hosting component.
     */
    public void cleanup() {
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }
        adContainer.removeAllViews();
        nativeAdView = null;
    }

    /**
     * Creates a FrameLayout that can be used as an ad container.
     *
     * @param context Context to create the view with
     * @return A FrameLayout suitable for displaying native ads
     */
    public static FrameLayout createAdContainer(Context context) {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }
} 