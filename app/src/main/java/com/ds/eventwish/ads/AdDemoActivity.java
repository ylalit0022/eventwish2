package com.ds.eventwish.ads;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdDemoActivity extends AppCompatActivity {
    private static final String TAG = "AdDemoActivity";
    
    private AdMobRepository adMobRepository;
    private TextView statusText;
    private Button showAdButton;
    private Spinner adTypeSpinner;
    private String selectedAdType = "interstitial";
    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_demo);

        Log.d(TAG, "Initializing AdDemoActivity");
        
        ApiService apiService = ApiClient.getInstance();
        adMobRepository = new AdMobRepository(this, apiService);
        
        statusText = findViewById(R.id.statusText);
        showAdButton = findViewById(R.id.showAdButton);
        adTypeSpinner = findViewById(R.id.adTypeSpinner);

        setupAdTypeSpinner();
        setupShowAdButton();
        
        // Initial fetch
        fetchAdUnit();
    }

    private void setupAdTypeSpinner() {
        String[] adTypes = {"app_open", "banner", "interstitial", "rewarded"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, adTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adTypeSpinner.setAdapter(adapter);

        adTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAdType = adTypes[position];
                Log.d(TAG, "Selected ad type: " + selectedAdType);
                fetchAdUnit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
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
                Log.d(TAG, "Successfully fetched ad unit from server: " + adUnit.getAdUnitCode());
                loadInterstitialAd(adUnit.getAdUnitCode());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching ad unit from server: " + error);
                // On server error, try to get from local database
                checkLocalAdUnit();
            }
        });
    }

    private void checkLocalAdUnit() {
        Log.d(TAG, "Checking local database for ad unit");
        adMobRepository.getActiveAdUnitByType(selectedAdType).observe(this, adUnit -> {
            if (adUnit != null) {
                Log.d(TAG, "Found active ad unit in local database: " + adUnit.getAdUnitId());
                loadInterstitialAd(adUnit.getAdUnitId());
            } else {
                Log.w(TAG, "No active ad unit found in local database");
                updateStatus(getString(R.string.ad_demo_ad_failed, "No ad unit available"));
            }
        });
    }

    private void loadInterstitialAd(String adUnitId) {
        Log.d(TAG, "Loading interstitial ad with unit ID: " + adUnitId);
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, adUnitId, adRequest, 
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    Log.d(TAG, "Interstitial ad loaded successfully");
                    interstitialAd = ad;
                    showAdButton.setEnabled(true);
                    updateStatus(getString(R.string.ad_demo_ad_loaded));
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: " + loadAdError.getMessage());
                    interstitialAd = null;
                    showAdButton.setEnabled(false);
                    updateStatus(getString(R.string.ad_demo_ad_failed, loadAdError.getMessage()));
                }
            });
    }

    private void updateStatus(String message) {
        Log.d(TAG, "Status update: " + message);
        if (statusText != null) {
            statusText.setText(message);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "AdDemoActivity destroyed");
        super.onDestroy();
        interstitialAd = null;
    }
} 