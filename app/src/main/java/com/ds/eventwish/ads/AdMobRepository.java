package com.ds.eventwish.ads;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.Date;

import com.ds.eventwish.R;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ds.eventwish.data.db.AppDatabase;
import com.ds.eventwish.data.local.dao.AdUnitDao;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.utils.AppExecutors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for handling AdMob ad unit fetching directly from server
 * No caching is implemented - all requests go directly to the server
 */
public class AdMobRepository {
    private static final String TAG = "AdMobRepository";
    private static final boolean DEBUG = true; // Enable detailed logging
    
    private final Context context;
    private final ApiService apiService;
    private final String apiKey = "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915";
    private final String appSignature = "app_sig_1";
    private final AdUnitDao adUnitDao;
    
    public interface AdUnitCallback {
        void onSuccess(AdUnit adUnit);
        void onError(String error);
    }
    
    public AdMobRepository(Context context, ApiService apiService) {
        Log.d(TAG, "Initializing AdMobRepository");
        this.context = context.getApplicationContext();
        this.apiService = apiService;
        this.adUnitDao = AppDatabase.getInstance(context).adUnitDao();
    }

    private void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    private void logError(String message, Throwable t) {
        Log.e(TAG, message, t);
    }

    private String getStringOrNull(JsonObject obj, String key) {
        try {
            JsonElement element = obj.get(key);
            String value = element != null && !element.isJsonNull() ? element.getAsString() : null;
            logDebug(String.format("Getting string value for key '%s': %s", key, value));
            return value;
            } catch (Exception e) {
            logError(String.format("Error getting string value for key '%s'", key), e);
            return null;
        }
    }

    private boolean getBooleanOrDefault(JsonObject obj, String key, boolean defaultValue) {
        try {
            JsonElement element = obj.get(key);
            boolean value = element != null && !element.isJsonNull() ? element.getAsBoolean() : defaultValue;
            logDebug(String.format("Getting boolean value for key '%s': %b (default: %b)", key, value, defaultValue));
            return value;
        } catch (Exception e) {
            logError(String.format("Error getting boolean value for key '%s', using default: %b", key, defaultValue), e);
            return defaultValue;
        }
    }

    private int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        try {
            JsonElement element = obj.get(key);
            int value = element != null && !element.isJsonNull() ? element.getAsInt() : defaultValue;
            logDebug(String.format("Getting int value for key '%s': %d (default: %d)", key, value, defaultValue));
            return value;
        } catch (Exception e) {
            logError(String.format("Error getting int value for key '%s', using default: %d", key, defaultValue), e);
            return defaultValue;
        }
    }

    /**
     * Fetch ad unit by type directly from server
     * @param adType Type of ad (app_open, banner, interstitial, rewarded)
     * @param callback Callback to handle response
     */
    public void fetchAdUnit(String adType, AdUnitCallback callback) {
        logDebug("=== Starting Ad Unit Fetch ===");
        logDebug(String.format("Fetching ad unit for type: %s", adType));
        
        // Get device ID - in real app, this should be properly generated and stored
        String deviceId = "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e";
        
        // Log request details
        logDebug("Request Details:");
        logDebug(String.format("- API Key: %s", apiKey));
        logDebug(String.format("- App Signature: %s", appSignature));
        logDebug(String.format("- Device ID: %s", deviceId));
        logDebug(String.format("- Ad Type: %s", adType));
        
        apiService.getAdUnits(adType, apiKey, appSignature, deviceId)
            .enqueue(new Callback<JsonObject>() {
            @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    logDebug("=== Server Response Received ===");
                    logDebug(String.format("Response Code: %d", response.code()));
                    logDebug(String.format("Response Message: %s", response.message()));
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String rawResponse = response.body().toString();
                        logDebug("=== Raw Response Analysis ===");
                        logDebug("Raw Response Body:");
                        logDebug(rawResponse);
                        logDebug("Response Headers:");
                        logDebug(response.headers().toString());
                        
                        try {
                            JsonObject responseBody = response.body();
                            logDebug("\nResponse Structure Analysis:");
                            logDebug("Root level keys:");
                            for (String key : responseBody.keySet()) {
                                JsonElement element = responseBody.get(key);
                                logDebug(String.format("- %s: %s (Type: %s)", 
                                    key, 
                                    element,
                                    element != null ? element.getClass().getSimpleName() : "null"));
                            }

                            JsonArray adUnitsArray = null;
                            if (responseBody.has("adUnits")) {
                                adUnitsArray = responseBody.getAsJsonArray("adUnits");
                                logDebug("Found adUnits array directly in response");
                            } else if (responseBody.has("data")) {
                                JsonElement dataElement = responseBody.get("data");
                                if (dataElement.isJsonObject() && dataElement.getAsJsonObject().has("adUnits")) {
                                    adUnitsArray = dataElement.getAsJsonObject().getAsJsonArray("adUnits");
                                    logDebug("Found adUnits array in data object");
                                }
                            }

                            if (adUnitsArray == null) {
                                String error = "Could not find adUnits array in response";
                                logError(error, null);
                                callback.onError(error);
                                return;
                            }

                            logDebug(String.format("Found %d ad units in response", adUnitsArray.size()));

                            // Find the ad unit matching the requested type (case insensitive)
                            JsonObject matchingAdUnit = null;
                            String normalizedRequestedType = adType.toLowerCase().trim();
                            
                            for (JsonElement element : adUnitsArray) {
                                if (!element.isJsonObject()) continue;
                                
                                JsonObject adUnitObj = element.getAsJsonObject();
                                String currentAdType = getStringOrNull(adUnitObj, "adType");
                                
                                if (currentAdType == null) continue;
                                
                                String normalizedCurrentType = currentAdType.toLowerCase().trim();
                                logDebug(String.format("Checking ad unit - Type: %s (normalized: %s)", 
                                    currentAdType, normalizedCurrentType));
                                
                                if (normalizedRequestedType.equals(normalizedCurrentType)) {
                                    matchingAdUnit = adUnitObj;
                                    logDebug("Found matching ad unit!");
                                    break;
                                }
                            }
                            
                            if (matchingAdUnit == null) {
                                String error = String.format("No ad unit found for type: %s", adType);
                                logError(error, null);
                                callback.onError(error);
                                return;
                            }
                            
                            logDebug("Found matching ad unit:");
                            for (String key : matchingAdUnit.keySet()) {
                                JsonElement value = matchingAdUnit.get(key);
                                logDebug(String.format("- %s: %s", key, value));
                            }
                            
                            // Parse the matching ad unit
                            String adName = getStringOrNull(matchingAdUnit, "adName");
                            String adUnitType = getStringOrNull(matchingAdUnit, "adType");
                            String adUnitCode = getStringOrNull(matchingAdUnit, "adUnitCode");
                            boolean status = getBooleanOrDefault(matchingAdUnit, "status", false);
                            int targetingPriority = getIntOrDefault(matchingAdUnit, "targetingPriority", 1);
                            
                            // Validate required fields
                            logDebug("Required Fields Validation:");
                            logDebug(String.format("- adName: %s", adName));
                            logDebug(String.format("- adUnitType: %s", adUnitType));
                            logDebug(String.format("- adUnitCode: %s", adUnitCode));
                            
                            if (adName == null || adUnitType == null || adUnitCode == null) {
                                String error = "Missing required fields in server response";
                                logError(error + String.format(" - adName: %s, adType: %s, adUnitCode: %s",
                                    adName, adUnitType, adUnitCode), null);
                                callback.onError(error);
                                return;
                            }
                            
                            logDebug("Creating AdUnit object with parsed data:");
                            logDebug(String.format("- Name: %s", adName));
                            logDebug(String.format("- Type: %s", adUnitType));
                            logDebug(String.format("- Code: %s", adUnitCode));
                            logDebug(String.format("- Status: %b", status));
                            logDebug(String.format("- Priority: %d", targetingPriority));
                            
                            AdUnit adUnit = new AdUnit(adName, adUnitType, adUnitCode, status);
                            adUnit.setTargetingPriority(targetingPriority);
                            
                            // Check if ad can be shown
                            boolean canShow = getBooleanOrDefault(matchingAdUnit, "canShow", true);
                            String reason = getStringOrNull(matchingAdUnit, "reason");
                            Date nextAvailable = null;
                            
                            if (matchingAdUnit.has("nextAvailable") && !matchingAdUnit.get("nextAvailable").isJsonNull()) {
                                try {
                                    nextAvailable = new Date(matchingAdUnit.get("nextAvailable").getAsLong());
                                    logDebug(String.format("Parsed nextAvailable date: %s", nextAvailable));
                                } catch (Exception e) {
                                    logError("Error parsing nextAvailable date", e);
                                }
                            }
                            
                            adUnit.setCanShow(canShow);
                            adUnit.setReason(reason);
                            adUnit.setNextAvailable(nextAvailable);
                            
                            logDebug("Ad Availability Details:");
                            logDebug(String.format("- Can Show: %b", canShow));
                            logDebug(String.format("- Reason: %s", reason));
                            logDebug(String.format("- Next Available: %s", nextAvailable));
                            
                            // Save to database
                            logDebug("Creating AdUnitEntity for database:");
                            AdUnitEntity entity = new AdUnitEntity();
                            entity.setId(adUnitCode);
                            entity.setAdName(adName);
                            entity.setAdType(adUnitType);
                            entity.setAdUnitCode(adUnitCode);
                            entity.setStatus(status ? 1 : 0);
                            entity.setTargetingPriority(targetingPriority);
                            entity.setCanShow(canShow);
                            entity.setReason(reason);
                            entity.setNextAvailable(nextAvailable != null ? nextAvailable.toString() : null);
                            
                            try {
                                // Move database operation to background thread
                                AppExecutors.getInstance().diskIO().execute(() -> {
                                    try {
                                        adUnitDao.insert(entity);
                                        logDebug("Successfully saved ad unit to database");
                                    } catch (Exception e) {
                                        logError("Error saving ad unit to database", e);
                                    }
                                });
                            } catch (Exception e) {
                                logError("Error scheduling database operation", e);
                            }
                            
                            logDebug("=== Ad Unit Fetch Completed Successfully ===");
                            callback.onSuccess(adUnit);
                        } catch (Exception e) {
                            String error = "Failed to parse ad unit data: " + e.getMessage();
                            logError(error, e);
                            callback.onError(error);
                        }
                    } else if (response.code() == 503) {
                        String error = context.getString(R.string.log_admob_server_error);
                        logError(error, null);
                        callback.onError(error);
                } else {
                        String error = String.format("Server error: %d - %s", 
                            response.code(), response.message());
                        logError(error, null);
                        callback.onError(error);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    logDebug("=================== NETWORK ERROR ===================");
                    logDebug("Request URL: " + call.request().url());
                    logDebug("Request Method: " + call.request().method());
                    logDebug("Request Headers: " + call.request().headers());
                    
                    // Examine the exception type for more specific information
                    String errorType = t.getClass().getSimpleName();
                    logDebug("Error Type: " + errorType);
                    logDebug("Error Message: " + t.getMessage());
                    
                    // Build detailed error message
                    StringBuilder detailedError = new StringBuilder();
                    detailedError.append("Network error [").append(errorType).append("]: ");
                    detailedError.append(t.getMessage());
                    
                    // Add request details to error message
                    detailedError.append(" | URL: ").append(call.request().url());
                    
                    // Log stack trace
                    logDebug("Stack Trace:");
                    for (StackTraceElement element : t.getStackTrace()) {
                        logDebug("  " + element.toString());
                        if (detailedError.length() < 1000) { // Limit size for callback
                            detailedError.append(" | ").append(element.toString());
                        }
                    }
                    
                    logDebug("=====================================================");
                    
                    String error = detailedError.toString();
                    logError(error, t);
                    callback.onError(error);
                }
        });
    }
    
    /**
     * Fetch all ad units directly from server
     * @param callback Callback to handle response
     */
    public void fetchAllAdUnits(AdUnitCallback callback) {
        Log.d(TAG, "Fetching all ad units");
        
        // Get device ID - in real app, this should be properly generated and stored
        String deviceId = "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e";
        
        // Log request details
        Log.d(TAG, context.getString(R.string.log_admob_headers, apiKey, appSignature, deviceId));
        
        apiService.getAllAdUnits(apiKey, appSignature, deviceId)
            .enqueue(new Callback<JsonObject>() {
            @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.d(TAG, context.getString(R.string.debug_response_code, response.code()));
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String rawResponse = response.body().toString();
                        Log.d(TAG, context.getString(R.string.debug_raw_response, rawResponse));
                        
                        try {
                            // Create AdUnit from first ad unit in response
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonObject firstAdUnit = data.getAsJsonArray("ad_units").get(0).getAsJsonObject();
                            
                            String adName = firstAdUnit.get("ad_name").getAsString();
                            String adType = firstAdUnit.get("ad_type").getAsString();
                            String adUnitCode = firstAdUnit.get("ad_unit_code").getAsString();
                            boolean status = firstAdUnit.get("status").getAsBoolean();
                            
                            AdUnit adUnit = new AdUnit(adName, adType, adUnitCode, status);
                            callback.onSuccess(adUnit);
                        } catch (Exception e) {
                            String error = "Failed to parse ad units data: " + e.getMessage();
                            Log.e(TAG, error, e);
                            callback.onError(error);
                        }
                    } else if (response.code() == 503) {
                        String error = context.getString(R.string.log_admob_server_error);
                        Log.e(TAG, error);
                        callback.onError(error);
                } else {
                        String error = context.getString(R.string.log_admob_response_error, 
                            response.code() + " " + response.message());
                        Log.e(TAG, error);
                        callback.onError(error);
                }
            }
            
            @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    String error = context.getString(R.string.log_admob_network_error, t.getMessage());
                    Log.e(TAG, error, t);
                    callback.onError(error);
            }
        });
    }
    
    public LiveData<AdUnitEntity> getActiveAdUnitByType(String type) {
        Log.d(TAG, String.format("Getting active ad unit for type: %s", type));
        return adUnitDao.getActiveAdUnitByType(type);
    }

    public LiveData<List<AdUnitEntity>> getAllActiveAdUnits() {
        Log.d(TAG, "Getting all active ad units");
        return adUnitDao.getAllActiveAdUnits();
    }

    public LiveData<List<AdUnitEntity>> getAvailableAdUnitsByType(String type) {
        Log.d(TAG, String.format("Getting available ad units for type: %s", type));
        return adUnitDao.getAvailableAdUnitsByType(type);
    }

    public void updateAdUnitStatus(String id, boolean canShow, String reason) {
        Log.d(TAG, String.format("Updating ad unit status - ID: %s, CanShow: %b, Reason: %s", 
            id, canShow, reason));
        AppExecutors.getInstance().diskIO().execute(() -> {
            adUnitDao.updateAdUnitStatus(id, canShow, reason);
        });
    }

    public void updateNextAvailable(String id, String nextAvailable) {
        Log.d(TAG, String.format("Updating ad unit next available time - ID: %s, NextAvailable: %s", 
            id, nextAvailable));
        AppExecutors.getInstance().diskIO().execute(() -> {
            adUnitDao.updateNextAvailable(id, nextAvailable);
        });
    }
} 