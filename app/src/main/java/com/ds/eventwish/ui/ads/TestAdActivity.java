package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdBannerView;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.data.model.ads.AdConstants;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.repository.AdMobRepository;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.util.SecureTokenManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for testing AdMob integration
 * This activity provides a UI to test various aspects of the AdMob integration
 */
public class TestAdActivity extends AppCompatActivity {
    private static final String TAG = "TestAdActivity";
    
    // Gson instance for JSON formatting
    private Gson gson;
    
    // UI components
    private TextView statusTextView;
    private TextView resultTextView;
    private SwitchCompat autoRetrySwitch;
    private Button showBannerAdButton;
    private Button showInterstitialAdButton;
    private Button showRewardedAdButton;
    private Button showNativeAdButton;
    private Button showNativeVideoAdButton;
    private FrameLayout bannerAdContainer;
    private FrameLayout nativeAdContainer;
    private FrameLayout nativeVideoAdContainer;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    
    // AdMob classes
    private AdMobRepository adMobRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ad);
        
        // Initialize the Gson instance
        gson = new Gson();
        
        // Set up UI views
        setupViews();
        
        // Initialize repository
        adMobRepository = AdMobRepository.getInstance(this);
        
        // Set up button listeners
        setupButtonListeners();
        
        // Test auth automatically
        if (getIntent().getBooleanExtra("auto_test", false)) {
            new Handler().postDelayed(() -> testAuth(), 500);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
    }
    
    private void setupViews() {
        statusTextView = findViewById(R.id.status_text_view);
        resultTextView = findViewById(R.id.result_text_view);
        autoRetrySwitch = findViewById(R.id.auto_retry_switch);
        showBannerAdButton = findViewById(R.id.show_banner_ad_button);
        showInterstitialAdButton = findViewById(R.id.show_interstitial_ad_button);
        showRewardedAdButton = findViewById(R.id.show_rewarded_ad_button);
        showNativeAdButton = findViewById(R.id.show_native_ad_button);
        showNativeVideoAdButton = findViewById(R.id.show_native_video_ad_button);
        bannerAdContainer = findViewById(R.id.banner_ad_container);
        nativeAdContainer = findViewById(R.id.native_ad_container);
        nativeVideoAdContainer = findViewById(R.id.native_video_ad_container);
        progressBar = findViewById(R.id.progress_bar);
        scrollView = findViewById(R.id.scroll_view);
    }
    
    private void setupButtonListeners() {
        // Auth test button
        findViewById(R.id.test_auth_button).setOnClickListener(v -> testAuth());
        
        // Banner ad button
        findViewById(R.id.show_banner_ad_button).setOnClickListener(v -> {
            updateStatus("Loading banner ad...");
            fetchAdUnits("banner");
        });
        
        // Interstitial ad button
        findViewById(R.id.show_interstitial_ad_button).setOnClickListener(v -> {
            updateStatus("Loading interstitial ad...");
            fetchAdUnits("interstitial");
        });
        
        // Rewarded ad button
        findViewById(R.id.show_rewarded_ad_button).setOnClickListener(v -> {
            updateStatus("Loading rewarded ad...");
            fetchAdUnits("rewarded");
        });
        
        // Native ad button
        findViewById(R.id.show_native_ad_button).setOnClickListener(v -> {
            updateStatus("Loading native ad...");
            fetchAdUnits("native");
        });
        
        // Native video ad button
        findViewById(R.id.show_native_video_ad_button).setOnClickListener(v -> {
            updateStatus("Loading native video ad...");
            fetchAdUnits("native_video");
        });
    }
    
    /**
     * Update the status message
     */
    private void updateStatus(String status) {
        runOnUiThread(() -> {
            statusTextView.setText(status);
            Log.d(TAG, "Status update: " + status);
        });
    }
    
    /**
     * Enable/disable all buttons
     */
    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.test_auth_button).setEnabled(enabled);
        findViewById(R.id.show_banner_ad_button).setEnabled(enabled);
        findViewById(R.id.show_interstitial_ad_button).setEnabled(enabled);
        findViewById(R.id.show_rewarded_ad_button).setEnabled(enabled);
        findViewById(R.id.show_native_ad_button).setEnabled(enabled);
        findViewById(R.id.show_native_video_ad_button).setEnabled(enabled);
    }
    
    /**
     * Test authentication with the server
     */
    private void testAuth() {
        updateStatus("Testing authentication...");
        progressBar.setVisibility(View.VISIBLE);
        
        // Test all signatures to find which ones work
        testAllSignatures();
    }
    
    /**
     * Test all potential signatures sequentially
     */
    private void testAllSignatures() {
        updateStatus("Testing all possible signatures...");
        progressBar.setVisibility(View.VISIBLE);
        
        // Clear any previous results
        resultTextView.setText("");
        
        // List of signatures to test
        List<String> signatures = new ArrayList<>();
        signatures.add(AdConstants.Signature.APP_SIGNATURE); // The known working signature
        signatures.add("app_sig_1");
        signatures.add("app_sig_2");
        signatures.add("dev_signature"); // Development test signature
        signatures.add("ew_dev_c1ce47afeff9fa8b7b1aa165562cb915"); // API key as signature
        
        // Start testing with the first signature
        testSignatureSequence(signatures, 0);
    }
    
    /**
     * Test signatures in sequence
     */
    private void testSignatureSequence(List<String> signatures, int index) {
        if (index >= signatures.size()) {
            // All signatures tested
            updateStatus("Completed testing all signatures.");
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        String signature = signatures.get(index);
        updateStatus("Testing signature " + (index + 1) + "/" + signatures.size() + ": " + signature);
        
        // Create headers with the signature to test
        Map<String, String> headers = new HashMap<>();
        headers.put(AdConstants.Headers.API_KEY, "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915");
        headers.put(AdConstants.Headers.DEVICE_ID, "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e");
        headers.put(AdConstants.Headers.APP_SIGNATURE, signature);
        
        // Log headers for debugging
        Log.d(TAG, "Testing with headers: " + gson.toJson(headers));
        
        // Call the API to test the signature
        adMobRepository.getApiService().getAdUnits(headers, null).enqueue(new Callback<com.ds.eventwish.data.model.response.AdMobResponse>() {
            @Override
            public void onResponse(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Response<com.ds.eventwish.data.model.response.AdMobResponse> response) {
                if (response.isSuccessful()) {
                    // This signature works!
                    String message = "✅ Signature WORKS: " + signature;
                    Log.d(TAG, message);
                    appendStatus(message);
                    
                    // Display response data
                    try {
                        String responseBody = gson.toJson(response.body());
                        Log.d(TAG, "Response body: " + responseBody);
                        appendStatus("\nResponse: " + (responseBody.length() > 100 ? 
                            responseBody.substring(0, 100) + "..." : responseBody));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response body", e);
                    }
                    
                    // Continue with next signature
                    new Handler().postDelayed(() -> {
                        testSignatureSequence(signatures, index + 1);
                    }, 1000);
                } else {
                    // This signature doesn't work
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        
                        if (response.code() == 401 && errorBody.contains("APP_SIGNATURE_INVALID")) {
                            String error = "❌ Signature INVALID: " + signature;
                            Log.e(TAG, error + " - " + errorBody);
                            appendStatus("\n" + error);
                        } else {
                            String error = "❌ Request failed: " + response.code();
                            Log.e(TAG, error + " - " + errorBody);
                            appendStatus("\n" + error + " - " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        appendStatus("\n❌ Request failed: " + response.code());
                    }
                    
                    // Continue with next signature
                    new Handler().postDelayed(() -> {
                        testSignatureSequence(signatures, index + 1);
                    }, 1000);
                }
            }
            
            @Override
            public void onFailure(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Throwable t) {
                String errorMsg = "❌ Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                appendStatus("\n" + errorMsg);
                
                // Continue with next signature after delay
                new Handler().postDelayed(() -> {
                    testSignatureSequence(signatures, index + 1);
                }, 2000); // Longer delay after network error
            }
        });
    }
    
    /**
     * Test a specific signature
     */
    private void testSignature(String signature) {
        updateStatus("Testing signature: " + signature);
        
        // Create headers with the signature to test
        Map<String, String> headers = new HashMap<>();
        headers.put(AdConstants.Headers.API_KEY, "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915");
        headers.put(AdConstants.Headers.DEVICE_ID, "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e");
        headers.put(AdConstants.Headers.APP_SIGNATURE, signature);
        
        // Log headers for debugging
        Log.d(TAG, "Testing with headers: " + gson.toJson(headers));
        
        // Call the API to test the signature
        adMobRepository.getApiService().getAdUnits(headers, null).enqueue(new Callback<com.ds.eventwish.data.model.response.AdMobResponse>() {
            @Override
            public void onResponse(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Response<com.ds.eventwish.data.model.response.AdMobResponse> response) {
                if (response.isSuccessful()) {
                    // This signature works!
                    String message = "✅ Signature WORKS: " + signature;
                    Log.d(TAG, message);
                    updateStatus(message);
                    
                    // Display response data
                    try {
                        String responseBody = gson.toJson(response.body());
                        Log.d(TAG, "Response body: " + responseBody);
                        updateStatus(message + "\n\nResponse: " + (responseBody.length() > 200 ? 
                            responseBody.substring(0, 200) + "..." : responseBody));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response body", e);
                    }
                    
                    // Refresh ad units with the working signature
                    adMobRepository.fetchAdUnits(null);
                    
                    progressBar.setVisibility(View.GONE);
                } else {
                    // This signature doesn't work
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        
                        if (response.code() == 401 && errorBody.contains("APP_SIGNATURE_INVALID")) {
                            String error = "❌ Signature INVALID: " + signature;
                            Log.e(TAG, error + " - " + errorBody);
                            updateStatus(error + "\n\nError: " + errorBody);
                        } else if (response.code() >= 500) {
                            String error = "❌ SERVER ERROR: " + response.code();
                            Log.e(TAG, error + " - " + errorBody);
                            updateStatus(error + "\n\nThe server is experiencing issues. Try again later.");
                        } else {
                            String error = "❌ Request failed: " + response.code();
                            Log.e(TAG, error + " - " + errorBody);
                            updateStatus(error + "\n\nError: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        updateStatus("❌ Request failed: " + response.code());
                    }
                    
                    progressBar.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onFailure(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Throwable t) {
                String errorMsg = "❌ Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                updateStatus(errorMsg + "\n\nEnsure you have internet connectivity and the server is available.");
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Append to the status text view
     */
    private void appendStatus(String status) {
        runOnUiThread(() -> {
            String currentText = resultTextView.getText().toString();
            resultTextView.setText(currentText + status);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }
    
    /**
     * Fetch ad units of a specific type from the server
     */
    private void fetchAdUnits(String adType) {
        Log.d(TAG, "Fetching ad units for type: " + adType);
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Fetching " + adType + " ad units...");
        
        // First check if we have ad units in the database
        adMobRepository.getAdUnitsByType(adType).observe(this, adUnitEntities -> {
            if (adUnitEntities != null && !adUnitEntities.isEmpty()) {
                Log.d(TAG, "Found " + adUnitEntities.size() + " ad units in database for type: " + adType);
                updateStatus("Found " + adUnitEntities.size() + " ad units in database");
                
                // Log and display the ad units from database
                StringBuilder dbDataLog = new StringBuilder();
                dbDataLog.append("Database Ad Units:\n");
                dbDataLog.append("Total ad units: ").append(adUnitEntities.size()).append("\n\n");
                
                // Filter for the requested ad type
                List<AdUnit> filteredAdUnits = new ArrayList<>();
                for (AdUnit unit : adUnitEntities) {
                    // Log each ad unit
                    Log.d(TAG, "Ad Unit from DB: " + gson.toJson(unit));
                    dbDataLog.append("Ad Unit ID: ").append(unit.getId())
                        .append("\nType: ").append(unit.getAdType())
                        .append("\nCode: ").append(unit.getAdUnitCode())
                        .append("\n\n");
                    
                    if (adType.equalsIgnoreCase(unit.getAdType())) {
                        filteredAdUnits.add(unit);
                    }
                }
                
                // Update UI with database data
                updateStatus(dbDataLog.toString());
                
                if (filteredAdUnits.isEmpty()) {
                    String error = "No " + adType + " ad units found in database. Fetching from server...";
                    Log.d(TAG, error);
                    appendStatus("\n" + error);
                    
                    // Fetch from server as fallback
                    fetchAdUnitsFromServer(adType);
                } else {
                    // Use the first matching ad unit
                    AdUnit adUnit = filteredAdUnits.get(0);
                    Log.d(TAG, "Selected ad unit from database: " + gson.toJson(adUnit));
                    appendStatus("\nSelected Ad Unit (Database):\n" +
                        "ID: " + adUnit.getId() + "\n" +
                        "Type: " + adUnit.getAdType() + "\n" +
                        "Code: " + adUnit.getAdUnitCode());
                    
                    // Check ad status before showing
                    checkAdStatus(adUnit);
                    progressBar.setVisibility(View.GONE);
                }
            } else {
                Log.d(TAG, "No ad units found in database. Fetching from server...");
                appendStatus("No ad units found in database. Fetching from server...");
                
                // Fetch from server if no database entries
                fetchAdUnitsFromServer(adType);
            }
        });
    }
    
    /**
     * Fetch ad units from the server when they aren't in the database
     */
    private void fetchAdUnitsFromServer(String adType) {
        updateStatus("Fetching " + adType + " ad units from server...");
        
        // Create headers for the request
        Map<String, String> headers = createRequestHeaders();
        
        // Call the API to get ad units
        adMobRepository.getApiService().getAdUnits(headers, null).enqueue(new Callback<com.ds.eventwish.data.model.response.AdMobResponse>() {
            @Override
            public void onResponse(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Response<com.ds.eventwish.data.model.response.AdMobResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Log full response for debugging
                        String rawResponse = gson.toJson(response.body());
                        Log.d(TAG, "Raw server response: " + rawResponse);
                        
                        List<AdUnit> adUnits = response.body().getData().getAdUnits();
                        if (adUnits != null && !adUnits.isEmpty()) {
                            // Save ad units to database for future use
                            List<AdUnitEntity> entities = new ArrayList<>();
                            for (AdUnit unit : adUnits) {
                                entities.add(AdUnitEntity.fromAdUnit(unit));
                            }
                            
                            // Save to database on a background thread
                            new Thread(() -> {
                                try {
                                    if (adType != null) {
                                        adMobRepository.getAdUnitDao().refreshAdUnitsByType(adType, entities);
                                    } else {
                                        adMobRepository.getAdUnitDao().refreshAdUnits(entities);
                                    }
                                    Log.d(TAG, "Saved " + entities.size() + " ad units to database");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving ad units to database", e);
                                }
                            }).start();
                            
                            // Log all received ad units
                            Log.d(TAG, "Received " + adUnits.size() + " ad units from server");
                            StringBuilder serverDataLog = new StringBuilder();
                            serverDataLog.append("Server Response:\n");
                            serverDataLog.append("Total ad units received: ").append(adUnits.size()).append("\n\n");
                            
                            // Filter for the requested ad type
                            List<AdUnit> filteredAdUnits = new ArrayList<>();
                            for (AdUnit unit : adUnits) {
                                // Log each ad unit
                                Log.d(TAG, "Ad Unit: " + gson.toJson(unit));
                                serverDataLog.append("Ad Unit ID: ").append(unit.getId())
                                    .append("\nType: ").append(unit.getAdType())
                                    .append("\nCode: ").append(unit.getAdUnitCode())
                                    .append("\n\n");
                                
                                if (adType.equalsIgnoreCase(unit.getAdType())) {
                                    filteredAdUnits.add(unit);
                                }
                            }
                            
                            // Update UI with server data
                            updateStatus(serverDataLog.toString());
                            
                            if (filteredAdUnits.isEmpty()) {
                                String error = "No " + adType + " ad units available on server";
                                Log.e(TAG, error);
                                appendStatus("\n" + error);
                            } else {
                                // Use the first matching ad unit
                                AdUnit adUnit = filteredAdUnits.get(0);
                                Log.d(TAG, "Selected ad unit for display: " + gson.toJson(adUnit));
                                appendStatus("\nSelected Ad Unit (Server):\n" +
                                    "ID: " + adUnit.getId() + "\n" +
                                    "Type: " + adUnit.getAdType() + "\n" +
                                    "Code: " + adUnit.getAdUnitCode());
                                
                                // Check ad status before showing
                                checkAdStatus(adUnit);
                            }
                        } else {
                            String error = "No ad units found in server response";
                            Log.e(TAG, error);
                            updateStatus(error + "\n\nRaw response: " + rawResponse);
                        }
                    } catch (Exception e) {
                        String error = "Error parsing server response: " + e.getMessage();
                        Log.e(TAG, error, e);
                        updateStatus(error + "\n\nRaw response: " + gson.toJson(response.body()));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        String error = "Server returned error code: " + response.code();
                        Log.e(TAG, error + " - " + errorBody);
                        updateStatus(error + "\n\nServer Error: " + errorBody + 
                            "\n\nRequest URL: " + call.request().url() +
                            "\nHeaders: " + gson.toJson(call.request().headers()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        updateStatus("Server error: " + response.code() + 
                            "\nRequest URL: " + call.request().url());
                    }
                }
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onFailure(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Throwable t) {
                String errorMsg = "Network error when getting ad units: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                updateStatus("Network Error:\n" + t.getMessage() + 
                    "\n\nRequest Details:" +
                    "\nURL: " + call.request().url() +
                    "\nHeaders: " + gson.toJson(call.request().headers()));
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Check ad status for a given ad unit
     */
    private void checkAdStatus(AdUnit adUnit) {
        Log.d(TAG, "Checking ad status for unit: " + adUnit.getId());
        progressBar.setVisibility(View.VISIBLE);
        
        // Create headers for the request
        Map<String, String> headers = createRequestHeaders();
        
        // Call status endpoint
        adMobRepository.getApiService().getAdStatus(headers, null).enqueue(new Callback<com.ds.eventwish.data.model.response.AdMobResponse>() {
            @Override
            public void onResponse(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Response<com.ds.eventwish.data.model.response.AdMobResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = gson.toJson(response.body());
                        Log.d(TAG, "Ad status response: " + responseBody);
                        
                        // Update UI with the status
                        updateStatus("Ad unit " + adUnit.getId() + " is ready to show\n" +
                                "Type: " + adUnit.getAdType() + "\n" +
                                "Ad ID: " + adUnit.getAdUnitCode());
                        
                        // Show the appropriate ad type
                        showAdByType(adUnit);
                    } catch (Exception e) {
                        String error = "Error parsing ad status response: " + e.getMessage();
                        Log.e(TAG, error, e);
                        updateStatus(error);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        String error = "Failed to get ad status: " + response.code();
                        Log.e(TAG, error + " - " + errorBody);
                        updateStatus(error + "\n\nError: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        updateStatus("Failed to get ad status: " + response.code());
                    }
                }
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onFailure(Call<com.ds.eventwish.data.model.response.AdMobResponse> call, Throwable t) {
                String errorMsg = "Network error when checking ad status: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                updateStatus(errorMsg);
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Show the appropriate ad type based on the ad unit
     */
    private void showAdByType(AdUnit adUnit) {
        String adType = adUnit.getAdType().toLowerCase();
        switch (adType) {
            case "banner":
                displayBannerAd(adUnit);
                break;
            case "interstitial":
                showInterstitialAd(adUnit);
                break;
            case "rewarded":
                showRewardedAd(adUnit);
                break;
            case "native":
                showNativeAd(adUnit);
                break;
            case "native_video":
                showNativeVideoAd(adUnit);
                break;
            default:
                updateStatus("Unsupported ad type: " + adType);
                break;
        }
    }
    
    /**
     * Display a banner ad in the container
     */
    private void displayBannerAd(AdUnit adUnit) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Displaying banner ad for unit: " + adUnit.getId());
                
                // Clear existing ads
                bannerAdContainer.removeAllViews();
                
                // Create and display test banner
                TextView dummyAdView = new TextView(this);
                dummyAdView.setText("TEST BANNER AD\n" + adUnit.getAdUnitCode());
                dummyAdView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                dummyAdView.setPadding(16, 16, 16, 16);
                dummyAdView.setTextColor(getResources().getColor(android.R.color.white));
                dummyAdView.setGravity(android.view.Gravity.CENTER);
                
                bannerAdContainer.addView(dummyAdView);
                bannerAdContainer.setVisibility(View.VISIBLE);
                
                updateStatus("Banner ad displayed for unit: " + adUnit.getId());
            } catch (Exception e) {
                String error = "Error displaying banner ad: " + e.getMessage();
                Log.e(TAG, error, e);
                updateStatus(error);
            }
        });
    }
    
    /**
     * Show an interstitial ad
     */
    private void showInterstitialAd(AdUnit adUnit) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Displaying interstitial ad for unit: " + adUnit.getId());
                
                // Create and show test interstitial dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("TEST INTERSTITIAL AD");
                builder.setMessage("Ad Unit: " + adUnit.getAdUnitCode());
                builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                builder.show();
                
                updateStatus("Interstitial ad displayed for unit: " + adUnit.getId());
            } catch (Exception e) {
                String error = "Error displaying interstitial ad: " + e.getMessage();
                Log.e(TAG, error, e);
                updateStatus(error);
            }
        });
    }
    
    /**
     * Show a rewarded ad
     */
    private void showRewardedAd(AdUnit adUnit) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Displaying rewarded ad for unit: " + adUnit.getId());
                
                // Create and show test rewarded dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("TEST REWARDED AD");
                builder.setMessage("Ad Unit: " + adUnit.getAdUnitCode() + "\n\nWatch the ad to earn a reward!");
                builder.setPositiveButton("Earn Reward", (dialog, which) -> {
                    // Simulate reward earned
                    updateStatus("Reward earned from ad unit: " + adUnit.getId());
                    dialog.dismiss();
                });
                builder.setNegativeButton("Skip", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                builder.show();
                
                updateStatus("Rewarded ad displayed for unit: " + adUnit.getId());
            } catch (Exception e) {
                String error = "Error displaying rewarded ad: " + e.getMessage();
                Log.e(TAG, error, e);
                updateStatus(error);
            }
        });
    }
    
    /**
     * Show a native ad
     */
    private void showNativeAd(AdUnit adUnit) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Displaying native ad for unit: " + adUnit.getId());
                
                // Clear existing ads
                nativeAdContainer.removeAllViews();
                
                // Create and display test native ad
                android.widget.LinearLayout nativeAdLayout = new android.widget.LinearLayout(this);
                nativeAdLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
                nativeAdLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                nativeAdLayout.setPadding(16, 16, 16, 16);
                
                TextView titleView = new TextView(this);
                titleView.setText("TEST NATIVE AD");
                titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                titleView.setTextColor(getResources().getColor(android.R.color.white));
                
                TextView descriptionView = new TextView(this);
                descriptionView.setText("Ad Unit: " + adUnit.getAdUnitCode());
                descriptionView.setTextColor(getResources().getColor(android.R.color.white));
                
                nativeAdLayout.addView(titleView);
                nativeAdLayout.addView(descriptionView);
                nativeAdContainer.addView(nativeAdLayout);
                nativeAdContainer.setVisibility(View.VISIBLE);
                
                updateStatus("Native ad displayed for unit: " + adUnit.getId());
            } catch (Exception e) {
                String error = "Error displaying native ad: " + e.getMessage();
                Log.e(TAG, error, e);
                updateStatus(error);
            }
        });
    }
    
    /**
     * Show a native video ad
     */
    private void showNativeVideoAd(AdUnit adUnit) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Displaying native video ad for unit: " + adUnit.getId());
                
                // Clear existing ads
                nativeVideoAdContainer.removeAllViews();
                
                // Create and display test native video ad
                android.widget.LinearLayout videoAdLayout = new android.widget.LinearLayout(this);
                videoAdLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
                videoAdLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
                videoAdLayout.setPadding(16, 16, 16, 16);
                
                TextView titleView = new TextView(this);
                titleView.setText("TEST NATIVE VIDEO AD");
                titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                titleView.setTextColor(getResources().getColor(android.R.color.white));
                
                TextView descriptionView = new TextView(this);
                descriptionView.setText("Ad Unit: " + adUnit.getAdUnitCode() + "\n\n[Video Player Placeholder]");
                descriptionView.setTextColor(getResources().getColor(android.R.color.white));
                
                videoAdLayout.addView(titleView);
                videoAdLayout.addView(descriptionView);
                nativeVideoAdContainer.addView(videoAdLayout);
                nativeVideoAdContainer.setVisibility(View.VISIBLE);
                
                updateStatus("Native video ad displayed for unit: " + adUnit.getId());
            } catch (Exception e) {
                String error = "Error displaying native video ad: " + e.getMessage();
                Log.e(TAG, error, e);
                updateStatus(error);
            }
        });
    }
    
    /**
     * Create request headers with all necessary authentication information
     */
    private Map<String, String> createRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        
        try {
            // Initialize SecureTokenManager with context
            SecureTokenManager.getInstance().init(getApplicationContext());
            
            // Get API key from SecureTokenManager
            String apiKey = SecureTokenManager.getInstance().getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915"; // Fallback to default
                Log.w(TAG, "Using fallback API key as none found in SecureTokenManager");
            } else {
                Log.d(TAG, "Using API key from SecureTokenManager: " + apiKey);
            }
            
            // Get device ID with fallback
            String deviceId = SecureTokenManager.getInstance().getDeviceId();
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e"; // Fallback device ID
                Log.w(TAG, "Using fallback device ID as none found in SecureTokenManager");
            }
            Log.d(TAG, "Using device ID: " + deviceId);
            
            // Get app signature
            String appSignature = AdConstants.Signature.APP_SIGNATURE; // Use constant from AdConstants
            Log.d(TAG, "Using app signature: " + appSignature);
            
            // Add headers only if values are not null
            if (apiKey != null) headers.put(AdConstants.Headers.API_KEY, apiKey);
            if (deviceId != null) headers.put(AdConstants.Headers.DEVICE_ID, deviceId);
            if (appSignature != null) headers.put(AdConstants.Headers.APP_SIGNATURE, appSignature);
            
            // Validate headers
            if (!headers.containsKey(AdConstants.Headers.API_KEY) || 
                !headers.containsKey(AdConstants.Headers.DEVICE_ID) || 
                !headers.containsKey(AdConstants.Headers.APP_SIGNATURE)) {
                throw new IllegalStateException("Missing required headers");
            }
            
            // Log the final headers
            Log.d(TAG, "Created request headers: " + gson.toJson(headers));
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating request headers", e);
            updateStatus("Error: Failed to create request headers - " + e.getMessage());
        }
        
        return headers;
    }
} 