package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.SponsoredAdDao;
import com.ds.eventwish.data.local.entity.SponsoredAdEntity;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Debug utility for ad impression tracking
 * This class helps diagnose issues with ad impression tracking
 */
public class AdImpressionDebugger {
    private static final String TAG = "AdImpressionDebugger";
    private static final String PREFS_NAME = "sponsored_ad_tracking";
    
    private final Context context;
    private final AppDatabase database;
    private final SponsoredAdDao sponsoredAdDao;
    private final AdSessionManager sessionManager;
    private final ApiService apiService;
    private final InternetConnectivityChecker connectivityChecker;
    
    /**
     * Create a new debugger instance
     */
    public AdImpressionDebugger(Context context) {
        Log.d(TAG, "Initializing AdImpressionDebugger");
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.sponsoredAdDao = database.sponsoredAdDao();
        this.sessionManager = initializeSessionManager(context);
        this.apiService = ApiClient.getClient();
        this.connectivityChecker = InternetConnectivityChecker.getInstance(context);
        
        Log.d(TAG, "AdImpressionDebugger initialized successfully");
    }
    
    /**
     * Initialize the session manager with proper error handling
     */
    private AdSessionManager initializeSessionManager(Context context) {
        try {
            AdSessionManager manager = AdSessionManager.getInstance(context);
            
            // Enable debug mode for session manager
            if (manager != null) {
                manager.setDebugMode(true);
                Log.d(TAG, "AdSessionManager initialized successfully and debug mode enabled");
                return manager;
            } else {
                Log.e(TAG, "Failed to get AdSessionManager instance");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdSessionManager: " + e.getMessage());
        }
        
        // Create a new instance as fallback - use getInstance instead of constructor
        Log.d(TAG, "Creating new AdSessionManager instance as fallback");
        return AdSessionManager.getInstance(context);
    }
    
    /**
     * Get AdImpressionDebugger instance
     */
    public static AdImpressionDebugger getInstance() {
        Context appContext = EventWishApplication.getAppContext();
        if (appContext == null) {
            Log.e(TAG, "Application context is null, cannot create AdImpressionDebugger");
            throw new IllegalStateException("Application context is null");
        }
        return new AdImpressionDebugger(appContext);
    }
    
    /**
     * Run diagnostics on the ad impression tracking system
     */
    public void runDiagnostics() {
        Log.d(TAG, "======== AD IMPRESSION TRACKING DIAGNOSTICS ========");
        
        // Check AdSessionManager
        checkAdSessionManager();
        
        // Check local database
        checkLocalDatabase();
        
        // Check shared preferences
        checkSharedPreferences();
        
        // Check server communication
        checkServerCommunication();
        
        Log.d(TAG, "======== END DIAGNOSTICS ========");
    }
    
    /**
     * Check AdSessionManager state
     */
    private void checkAdSessionManager() {
        Log.d(TAG, "--- AdSessionManager Diagnostics ---");
        Log.d(TAG, "Current session ID: " + sessionManager.getCurrentSessionId());
        
        // Dump all session data
        sessionManager.dumpSessionData();
        
        boolean hasImpressions = sessionManager.hasTrackedImpressions();
        Log.d(TAG, "Has tracked impressions: " + hasImpressions);
    }
    
    /**
     * Check local database for ad entities
     */
    private void checkLocalDatabase() {
        Log.d(TAG, "--- Local Database Diagnostics ---");
        
        try {
            // Get all ads from database
            List<SponsoredAdEntity> entities = sponsoredAdDao.getAllAds();
            
            if (entities == null || entities.isEmpty()) {
                Log.d(TAG, "No sponsored ads found in local database");
                return;
            }
            
            Log.d(TAG, "Found " + entities.size() + " sponsored ads in local database");
            
            // Track impression counts
            int totalImpressions = 0;
            Map<Integer, Integer> countDistribution = new HashMap<>();
            
            for (SponsoredAdEntity entity : entities) {
                int impressions = entity.getImpressionCount();
                totalImpressions += impressions;
                
                // Count distribution
                Integer currentCount = countDistribution.getOrDefault(impressions, 0);
                countDistribution.put(impressions, currentCount + 1);
                
                // Log details for each ad
                Log.d(TAG, "Ad ID: " + entity.getId() + 
                      ", Title: " + entity.getTitle() + 
                      ", Impressions: " + impressions + 
                      ", Last impression: " + (entity.getLastImpressionTime() > 0 ? 
                                               new java.util.Date(entity.getLastImpressionTime()) : "never"));
            }
            
            // Log summary
            Log.d(TAG, "Total impressions in database: " + totalImpressions);
            Log.d(TAG, "Impression count distribution:");
            for (Map.Entry<Integer, Integer> entry : countDistribution.entrySet()) {
                Log.d(TAG, "  " + entry.getKey() + " impressions: " + entry.getValue() + " ads");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking local database", e);
        }
    }
    
    /**
     * Check shared preferences for tracking data
     */
    private void checkSharedPreferences() {
        Log.d(TAG, "--- SharedPreferences Diagnostics ---");
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();
            
            if (allEntries.isEmpty()) {
                Log.d(TAG, "No tracking data found in SharedPreferences");
                return;
            }
            
            Log.d(TAG, "Found " + allEntries.size() + " entries in SharedPreferences");
            
            // Count impression keys
            int impressionKeys = 0;
            
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                Log.d(TAG, "  " + key + ": " + value);
                
                if (key.startsWith("impression_")) {
                    impressionKeys++;
                }
            }
            
            Log.d(TAG, "Found " + impressionKeys + " impression tracking entries");
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking SharedPreferences", e);
        }
    }
    
    /**
     * Check server communication for ad impressions
     */
    private void checkServerCommunication() {
        Log.d(TAG, "--- Server Communication Diagnostics ---");
        
        try {
            // Get connection status
            boolean isConnected = connectivityChecker.isNetworkAvailable();
            Log.d(TAG, "Network connectivity: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));
            
            if (!isConnected) {
                Log.d(TAG, "Cannot check server - no network connectivity");
                return;
            }
            
            // Get a sample ad from database
            SponsoredAdEntity entity = sponsoredAdDao.getRandomAd();
            
            if (entity == null) {
                Log.d(TAG, "No ads found in database to check server communication");
                return;
            }
            
            // Try to get current impression count from server
            String adId = entity.getId();
            String deviceId = DeviceUtils.getDeviceId(context);
            
            Log.d(TAG, "Checking server impression count for ad: " + adId);
            Log.d(TAG, "Local impression count: " + entity.getImpressionCount());
            
            // Make a synchronous API call to check status (purely for debugging)
            Call<JsonObject> call = apiService.recordSponsoredAdImpression(adId, deviceId);
            try {
                Response<JsonObject> response = call.execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    Log.d(TAG, "Server response: " + body);
                    
                    if (body.has("impressionCount")) {
                        int serverCount = body.get("impressionCount").getAsInt();
                        Log.d(TAG, "Server impression count: " + serverCount);
                        
                        if (serverCount != entity.getImpressionCount()) {
                            Log.d(TAG, "DISCREPANCY DETECTED: Local count = " + entity.getImpressionCount() + 
                                  ", Server count = " + serverCount);
                        } else {
                            Log.d(TAG, "Counts match between local and server");
                        }
                    } else {
                        Log.d(TAG, "Server did not return impression count");
                    }
                } else {
                    Log.d(TAG, "Server error: " + response.code());
                    if (response.errorBody() != null) {
                        Log.d(TAG, "Error body: " + response.errorBody().string());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error communicating with server", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking server communication", e);
        }
    }
    
    /**
     * Force synchronization of impressions with server
     * WARNING: This will trigger an impression count for testing purposes
     */
    public void forceSyncWithServer() {
        Log.d(TAG, "Forcing synchronization with server...");
        
        try {
            // Get a sample ad from database
            SponsoredAdEntity entity = sponsoredAdDao.getRandomAd();
            
            if (entity == null) {
                Log.d(TAG, "No ads found in database to force sync");
                return;
            }
            
            String adId = entity.getId();
            String deviceId = DeviceUtils.getDeviceId(context);
            
            Log.d(TAG, "Force syncing impression for ad: " + adId);
            
            // Make a synchronous API call
            Call<JsonObject> call = apiService.recordSponsoredAdImpression(adId, deviceId);
            try {
                Response<JsonObject> response = call.execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    Log.d(TAG, "Server response: " + body);
                    
                    if (body.has("impressionCount")) {
                        int serverCount = body.get("impressionCount").getAsInt();
                        Log.d(TAG, "Server impression count after sync: " + serverCount);
                        
                        // Update local database
                        entity.setImpressionCount(serverCount);
                        sponsoredAdDao.update(entity);
                        Log.d(TAG, "Updated local database to match server count");
                    }
                } else {
                    Log.d(TAG, "Server error during sync: " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error during force sync", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during force sync", e);
        }
    }
    
    /**
     * Clear all impression tracking data for testing
     */
    public void clearAllImpressionData() {
        Log.d(TAG, "Clearing all impression tracking data...");
        
        try {
            // Clear session data
            sessionManager.clearAllTrackedImpressions();
            Log.d(TAG, "Cleared session tracking data");
            
            // Clear local database impression counts
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    List<SponsoredAdEntity> entities = sponsoredAdDao.getAllAds();
                    if (entities != null) {
                        for (SponsoredAdEntity entity : entities) {
                            entity.setImpressionCount(0);
                            entity.setLastImpressionTime(0);
                            sponsoredAdDao.update(entity);
                        }
                        Log.d(TAG, "Reset impression counts for " + entities.size() + " ads in database");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing database impression counts", e);
                }
            });
            
            // Clear shared preferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            Map<String, ?> allEntries = prefs.getAll();
            for (String key : allEntries.keySet()) {
                if (key.startsWith("impression_")) {
                    editor.remove(key);
                }
            }
            
            editor.apply();
            Log.d(TAG, "Cleared impression keys from SharedPreferences");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing impression data", e);
        }
    }
} 