package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;
import com.ds.eventwish.data.ads.AdManagerExtended;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

/**
 * Helper class for native video ads
 */
public class NativeVideoAdHelper {
    private static final String TAG = "NativeVideoAdHelper";
    
    private final Context context;
    private NativeAd nativeAd;
    
    /**
     * Constructor
     * @param context The context
     */
    public NativeVideoAdHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Load a native video ad
     * @param callback Callback to be invoked when the ad is loaded or fails to load
     */
    public void loadNativeVideoAd(NativeVideoAdCallback callback) {
        Log.d(TAG, "Loading native video ad");
        
        // Create video options
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        
        // Create native ad options
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        
        // Create ad loader
        AdLoader adLoader = new AdLoader.Builder(context, AdManagerExtended.NATIVE_AD_UNIT_ID)
                .forNativeAd(nativeAd -> {
                    Log.d(TAG, "Native video ad loaded");
                    this.nativeAd = nativeAd;
                    if (callback != null) {
                        callback.onNativeVideoAdLoaded();
                    }
                })
                .withNativeAdOptions(adOptions)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Native video ad failed to load: " + loadAdError.getMessage());
                        if (callback != null) {
                            callback.onNativeVideoAdFailedToLoad(loadAdError.getMessage());
                        }
                    }
                })
                .build();
        
        // Load ad
        adLoader.loadAd(AdManagerExtended.getAdRequest());
    }
    
    /**
     * Populate a native video ad view
     * @param container The container to populate
     */
    public void populateNativeVideoAdView(ViewGroup container) {
        if (nativeAd == null) {
            Log.e(TAG, "Native video ad not loaded yet");
            return;
        }
        
        // Clear the container
        container.removeAllViews();
        
        // Inflate the native ad view
        LayoutInflater inflater = LayoutInflater.from(context);
        NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.native_video_ad, container, false);
        
        // Set the media view
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);
        
        // Set other ad assets
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        
        // Set the headline
        if (nativeAd.getHeadline() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }
        
        // Set the body
        if (nativeAd.getBody() != null) {
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        } else {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        }
        
        // Set the call to action
        if (nativeAd.getCallToAction() != null) {
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        } else {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        }
        
        // Set the icon
        if (nativeAd.getIcon() != null) {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
        } else {
            adView.getIconView().setVisibility(View.GONE);
        }
        
        // Set the price
        if (nativeAd.getPrice() != null) {
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        } else {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        }
        
        // Set the store
        if (nativeAd.getStore() != null) {
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        } else {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        }
        
        // Set the star rating
        if (nativeAd.getStarRating() != null) {
            ((TextView) adView.getStarRatingView()).setText(nativeAd.getStarRating().toString());
        } else {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        }
        
        // Set the advertiser
        if (nativeAd.getAdvertiser() != null) {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
        } else {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        }
        
        // Register the native ad view
        adView.setNativeAd(nativeAd);
        
        // Add the ad view to the container
        container.addView(adView);
    }
    
    /**
     * Destroy the native ad
     */
    public void destroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }
    
    /**
     * Callback for native video ads
     */
    public interface NativeVideoAdCallback {
        /**
         * Called when the native video ad is loaded
         */
        void onNativeVideoAdLoaded();
        
        /**
         * Called when the native video ad fails to load
         * @param errorMessage The error message
         */
        void onNativeVideoAdFailedToLoad(String errorMessage);
    }
} 