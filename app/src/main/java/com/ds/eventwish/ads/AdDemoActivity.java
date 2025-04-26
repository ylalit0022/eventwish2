package com.ds.eventwish.ads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.ads.core.AdConstants;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.nativead.MediaView;

public class AdDemoActivity extends AppCompatActivity {
    private static final String TAG = "AdDemoActivity";
    
    private AdMobRepository adMobRepository;
    private TextView statusText;
    private Button showAdButton;
    private Spinner adTypeSpinner;
    private String selectedAdType = AdConstants.AdType.INTERSTITIAL; // Default type
    private InterstitialAd interstitialAd;
    private NativeAd nativeAd;
    private FrameLayout nativeAdContainer;
    private boolean isInitialized = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_demo);
        Log.d(TAG, "Initializing AdDemoActivity");
        
        // Initialize repository first
        ApiService apiService = ApiClient.getClient();
        adMobRepository = new AdMobRepository(this, apiService);
        
        // Initialize UI elements
        initializeViews();
        
        // Post initialization with a delay to ensure views are ready
        mainHandler.postDelayed(this::completeInitialization, 100);
    }

    private void initializeViews() {
        try {
            statusText = findViewById(R.id.statusText);
            showAdButton = findViewById(R.id.showAdButton);
            adTypeSpinner = findViewById(R.id.adTypeSpinner);
            nativeAdContainer = findViewById(R.id.nativeAdContainer);
            
            if (statusText == null || showAdButton == null || 
                adTypeSpinner == null || nativeAdContainer == null) {
                Log.e(TAG, "Failed to initialize one or more views");
                return;
            }
            
            // Set initial UI state
            showAdButton.setEnabled(false);
            adTypeSpinner.setEnabled(false);
            updateStatus("Initializing...");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void completeInitialization() {
        try {
            if (!validateViews()) {
                Log.e(TAG, "Views not properly initialized");
                updateStatus("Initialization failed: Views not found");
                return;
            }
            
            setupAdTypeSpinner();
            setupShowAdButton();
            
            // Enable UI elements
            showAdButton.setEnabled(true);
            adTypeSpinner.setEnabled(true);
            
            isInitialized = true;
            Log.d(TAG, "Initialization completed successfully");
            
            // Initial UI update and ad fetch
            updateUIForAdType();
            fetchAdUnit();
        } catch (Exception e) {
            Log.e(TAG, "Error in completeInitialization: " + e.getMessage(), e);
            updateStatus("Initialization failed: " + e.getMessage());
        }
    }

    private boolean validateViews() {
        if (statusText == null) {
            Log.e(TAG, "Status text view is null");
            return false;
        }
        if (showAdButton == null) {
            Log.e(TAG, "Show ad button is null");
            return false;
        }
        if (adTypeSpinner == null) {
            Log.e(TAG, "Ad type spinner is null");
            return false;
        }
        if (nativeAdContainer == null) {
            Log.e(TAG, "Native ad container is null");
            return false;
        }
        return true;
    }

    private void setupAdTypeSpinner() {
        if (adTypeSpinner == null) {
            Log.e(TAG, "AdTypeSpinner is null");
            return;
        }

        String[] adTypes = {
            AdConstants.AdType.INTERSTITIAL,  // Default first
            AdConstants.AdType.NATIVE,
            AdConstants.AdType.APP_OPEN,
            AdConstants.AdType.BANNER,
            AdConstants.AdType.REWARDED
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, adTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adTypeSpinner.setAdapter(adapter);

        // Set initial selection
        for (int i = 0; i < adTypes.length; i++) {
            if (adTypes[i].equals(selectedAdType)) {
                adTypeSpinner.setSelection(i);
                break;
            }
        }

        adTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitialized) {
                    Log.d(TAG, "Ignoring selection, not yet initialized");
                    return;
                }
                
                String newAdType = adTypes[position];
                Log.d(TAG, "Selected ad type: " + newAdType);
                
                if (!newAdType.equals(selectedAdType)) {
                    selectedAdType = newAdType;
                    mainHandler.post(() -> {
                        cleanupCurrentAd();
                        updateUIForAdType();
                        fetchAdUnit();
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateUIForAdType() {
        if (!isInitialized) {
            Log.w(TAG, "Cannot update UI, not fully initialized");
            return;
        }

        mainHandler.post(() -> {
            try {
                if (!validateViews()) {
                    Log.e(TAG, "Views not valid during UI update");
                    return;
                }

                boolean showButton = selectedAdType.equals(AdConstants.AdType.INTERSTITIAL) || 
                                  selectedAdType.equals(AdConstants.AdType.REWARDED);
                showAdButton.setVisibility(showButton ? View.VISIBLE : View.GONE);
                nativeAdContainer.setVisibility(selectedAdType.equals(AdConstants.AdType.NATIVE) ? View.VISIBLE : View.GONE);
                
                Log.d(TAG, "UI updated for ad type: " + selectedAdType);
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI for ad type: " + e.getMessage(), e);
            }
        });
    }

    private void cleanupCurrentAd() {
        mainHandler.post(() -> {
            try {
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity is finishing or destroyed, skipping cleanup");
                    return;
                }

                if (interstitialAd != null) {
                    interstitialAd = null;
                }
                
                if (nativeAd != null) {
                    nativeAd.destroy();
                    nativeAd = null;
                }
                
                if (nativeAdContainer != null) {
                    nativeAdContainer.removeAllViews();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in cleanupCurrentAd: " + e.getMessage(), e);
            }
        });
    }

    private void setupShowAdButton() {
        showAdButton.setOnClickListener(v -> {
            if (interstitialAd != null) {
                Log.d(TAG, "Showing interstitial ad");
                interstitialAd.show(this);
            } else {
                Log.w(TAG, "Interstitial ad not loaded");
                updateStatus(getString(R.string.ad_demo_ad_failed_to_show, "Ad not loaded"));
            }
        });
    }

    private void fetchAdUnit() {
        Log.d(TAG, "Fetching ad unit for type: " + selectedAdType);
        updateStatus(getString(R.string.ad_demo_loading_ad));
        showAdButton.setEnabled(false);

        // First try to fetch from server
        adMobRepository.fetchAdUnit(selectedAdType, new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                // Log the raw response details
                Log.d(TAG, "=== Server Response Details ===");
                Log.d(TAG, "Ad Unit Type: " + selectedAdType);
                Log.d(TAG, "Ad Unit Code: " + adUnit.getAdUnitCode());
                Log.d(TAG, "Ad Name: " + adUnit.getAdName());
                Log.d(TAG, "Status: " + adUnit.isStatus());
                if (adUnit.getTargetingCriteria() != null) {
                    Log.d(TAG, "Targeting Criteria: " + adUnit.getTargetingCriteria().toString());
                }
                if (adUnit.getParameters() != null) {
                    Log.d(TAG, "Parameters: " + adUnit.getParameters().toString());
                }
                Log.d(TAG, "Can Show: " + adUnit.isCanShow());
                if (adUnit.getReason() != null) {
                    Log.d(TAG, "Reason: " + adUnit.getReason());
                }
                if (adUnit.getNextAvailable() != null) {
                    Log.d(TAG, "Next Available: " + adUnit.getNextAvailable());
                }
                Log.d(TAG, "=== End Server Response ===");

                Log.d(TAG, "Successfully fetched ad unit from server: " + adUnit.getAdUnitCode());
                loadAd(adUnit.getAdUnitCode());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "=== Server Error Details ===");
                Log.e(TAG, "Error Type: Server Response Error");
                Log.e(TAG, "Ad Type Requested: " + selectedAdType);
                Log.e(TAG, "Error Message: " + error);
                Log.e(TAG, "=== End Server Error ===");

                // On server error, try to get from local database
                checkLocalAdUnit();
            }
        });
    }

    private void checkLocalAdUnit() {
        Log.d(TAG, "=== Checking Local Database ===");
        Log.d(TAG, "Requested Ad Type: " + selectedAdType);
        
        adMobRepository.getActiveAdUnitByType(selectedAdType).observe(this, adUnit -> {
            if (adUnit != null) {
                Log.d(TAG, "=== Local Database Result ===");
                Log.d(TAG, "Found Active Ad Unit:");
                Log.d(TAG, "Ad Unit ID: " + adUnit.getAdUnitId());
                Log.d(TAG, "Ad Type: " + adUnit.getAdType());
                Log.d(TAG, "Status: " + (adUnit.getStatus() == 1 ? "Active" : "Inactive"));
                Log.d(TAG, "Can Show: " + adUnit.isCanShow());
                if (adUnit.getReason() != null) {
                    Log.d(TAG, "Reason: " + adUnit.getReason());
                }
                if (adUnit.getNextAvailable() != null) {
                    Log.d(TAG, "Next Available: " + adUnit.getNextAvailable());
                }
                Log.d(TAG, "=== End Local Database Result ===");
                
                loadAd(adUnit.getAdUnitId());
            } else {
                Log.w(TAG, "=== Local Database Result ===");
                Log.w(TAG, "No active ad unit found for type: " + selectedAdType);
                Log.w(TAG, "=== End Local Database Result ===");
                
                updateStatus(getString(R.string.ad_demo_ad_failed, "No ad unit available"));
            }
        });
    }

    private void loadAd(String adUnitId) {
        Log.d(TAG, "=== Loading Ad ===");
        Log.d(TAG, "Ad Type: " + selectedAdType);
        Log.d(TAG, "Ad Unit ID: " + adUnitId);
        
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            Log.e(TAG, "Invalid ad unit ID");
            updateStatus("Failed to load ad: Invalid ad unit ID");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        Log.d(TAG, "Created ad request: " + adRequest.toString());

        switch (selectedAdType) {
            case AdConstants.AdType.INTERSTITIAL:
                Log.d(TAG, "Loading interstitial ad...");
                loadInterstitialAd(adUnitId, adRequest);
                break;
                
            case AdConstants.AdType.NATIVE:
                Log.d(TAG, "Loading native ad...");
                loadNativeAd(adUnitId, adRequest);
                break;
                
            case AdConstants.AdType.APP_OPEN:
                Log.i(TAG, "App Open ads are managed by AppOpenManager");
                Log.i(TAG, "Ad Unit ID: " + adUnitId);
                updateStatus("App Open ad will show on next app launch\n\n" +
                           "Note: App Open ads are managed automatically by AppOpenManager\n" +
                           "Check AppOpenManager.java for implementation details");
                break;
                
            case AdConstants.AdType.BANNER:
                Log.i(TAG, "Banner ads must be implemented in layout");
                updateStatus("Banner ads must be added to layout directly");
                showBannerInstructions();
                break;
                
            case AdConstants.AdType.REWARDED:
                Log.i(TAG, "Rewarded ads implementation pending");
                updateStatus("Rewarded ads coming soon!\n\nTo implement rewarded ads:\n1. Load using RewardedAd.load()\n2. Show when ready\n3. Handle rewards");
                showRewardedInstructions();
                break;
        }
        Log.d(TAG, "=== End Loading Ad ===");
    }

    private void showBannerInstructions() {
        String instructions = "To implement banner ads:\n" +
                            "1. Add AdView to your layout\n" +
                            "2. Set ad unit ID\n" +
                            "3. Call adView.loadAd()";
        Log.i(TAG, instructions.replace('\n', ' '));
    }

    private void showRewardedInstructions() {
        String instructions = "To implement rewarded ads:\n" +
                            "1. Use RewardedAd.load()\n" +
                            "2. Set onUserEarnedRewardListener\n" +
                            "3. Show ad when ready\n" +
                            "4. Grant reward on completion";
        Log.i(TAG, instructions.replace('\n', ' '));
    }

    private void loadInterstitialAd(String adUnitId, AdRequest adRequest) {
        Log.d(TAG, "=== Loading Interstitial Ad ===");
        Log.d(TAG, "Ad Unit ID: " + adUnitId);
        Log.d(TAG, "Ad Request: " + adRequest.toString());
        
        InterstitialAd.load(this, adUnitId, adRequest, 
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    Log.d(TAG, "=== Interstitial Ad Loaded ===");
                    Log.d(TAG, "Ad Unit ID: " + adUnitId);
                    Log.d(TAG, "Ad Response Info: " + ad.getResponseInfo());
                    Log.d(TAG, "=== End Interstitial Ad Load ===");
                    
                    interstitialAd = ad;
                    showAdButton.setEnabled(true);
                    updateStatus(getString(R.string.ad_demo_ad_loaded));
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e(TAG, "=== Interstitial Ad Load Failed ===");
                    Log.e(TAG, "Ad Unit ID: " + adUnitId);
                    Log.e(TAG, "Error Code: " + loadAdError.getCode());
                    Log.e(TAG, "Error Domain: " + loadAdError.getDomain());
                    Log.e(TAG, "Error Message: " + loadAdError.getMessage());
                    Log.e(TAG, "Response Info: " + loadAdError.getResponseInfo());
                    Log.e(TAG, "=== End Interstitial Ad Load Error ===");
                    
                    interstitialAd = null;
                    showAdButton.setEnabled(false);
                    updateStatus(getString(R.string.ad_demo_ad_failed, loadAdError.getMessage()));
                }
            });
    }

    private void loadNativeAd(String adUnitId, AdRequest adRequest) {
        Log.d(TAG, "=== Loading Native Ad ===");
        Log.d(TAG, "Ad Unit ID: " + adUnitId);
        Log.d(TAG, "Ad Request: " + adRequest.toString());
        
        if (!isInitialized) {
            Log.w(TAG, "Cannot load native ad, not fully initialized");
            updateStatus("Cannot load ad: Not initialized");
            return;
        }

        if (nativeAdContainer == null) {
            Log.e(TAG, "Native ad container is null");
            updateStatus("Failed to load ad: Container not initialized");
            return;
        }

        try {
            Log.d(TAG, "Starting native ad load for unit: " + adUnitId);
            com.google.android.gms.ads.AdLoader adLoader = new com.google.android.gms.ads.AdLoader.Builder(this, adUnitId)
                .forNativeAd(nativeAd -> {
                    if (isFinishing() || isDestroyed()) {
                        nativeAd.destroy();
                        return;
                    }
                    
                    try {
                        if (this.nativeAd != null) {
                            this.nativeAd.destroy();
                        }
                        
                        this.nativeAd = nativeAd;
                        
                        if (!validateViews()) {
                            Log.e(TAG, "Views not valid during native ad display");
                            updateStatus("Failed to display ad: Views not initialized");
                            return;
                        }
                        
                        NativeAdView adView = (NativeAdView) getLayoutInflater()
                            .inflate(R.layout.native_ad_layout, null);
                            
                        if (adView == null) {
                            Log.e(TAG, "Failed to inflate native ad layout");
                            updateStatus("Failed to display ad: Layout error");
                            return;
                        }
                        
                        populateNativeAdView(nativeAd, adView);
                        
                        mainHandler.post(() -> {
                            nativeAdContainer.removeAllViews();
                            nativeAdContainer.addView(adView);
                            nativeAdContainer.setVisibility(View.VISIBLE);
                            updateStatus("Ad loaded successfully");
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error displaying native ad: " + e.getMessage(), e);
                        updateStatus("Failed to display ad: " + e.getMessage());
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.e(TAG, "Failed to load native ad: " + loadAdError.getMessage());
                        updateStatus("Failed to load ad: " + loadAdError.getMessage());
                    }
                })
                .build();

            adLoader.loadAd(adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error creating native ad loader: " + e.getMessage(), e);
            updateStatus("Failed to load ad: " + e.getMessage());
        }
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);

        // Set other ad assets
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStoreView(adView.findViewById(R.id.ad_store));

        // Set the headline text
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        // Set the body text
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        // Set the call to action
        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        // Set the app icon
        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        // Set the star rating
        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        // Set the advertiser
        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // Set the price
        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        // Set the store
        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        // Register the native ad view
        adView.setNativeAd(nativeAd);
    }

    private void updateStatus(String message) {
        mainHandler.post(() -> {
            try {
                Log.d(TAG, "Status update: " + message);
                if (statusText != null) {
                    // Support multiline status messages
                    statusText.setText(message);
                    // Make text selectable for copying implementation instructions
                    statusText.setTextIsSelectable(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating status: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "AdDemoActivity destroyed");
        isInitialized = false;
        cleanupCurrentAd();
        super.onDestroy();
    }
} 