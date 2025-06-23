package com.ds.eventwish.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.ApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Repository for managing creator profile data
 * Handles all server-side creator profile operations with caching and error handling
 */
public class CreatorProfileRepository {
    private static final String TAG = "CreatorProfileRepo";
    private static CreatorProfileRepository instance;
    
    private final ApiService apiService;
    
    // Cache for creator profiles to avoid repeated API calls
    private final Map<String, JsonObject> creatorProfileCache = new HashMap<>();
    
    private CreatorProfileRepository() {
        this.apiService = ApiClient.getClient();
    }
    
    public static synchronized CreatorProfileRepository getInstance() {
        if (instance == null) {
            instance = new CreatorProfileRepository();
        }
        return instance;
    }

    /**
     * Get creator profile for a single template
     * Uses caching to improve performance
     * 
     * @param templateId The template ID
     * @return LiveData containing the creator profile response
     */
    public LiveData<JsonObject> getCreatorProfile(String templateId) {
        MutableLiveData<JsonObject> result = new MutableLiveData<>();
        
        Log.d(TAG, "Fetching creator profile for template: " + templateId);
        
        // Check cache first
        if (creatorProfileCache.containsKey(templateId)) {
            Log.d(TAG, "Creator profile found in cache for template: " + templateId);
            JsonObject cachedResponse = creatorProfileCache.get(templateId);
            result.setValue(cachedResponse);
            return result;
        }
        
        // Make API call
        Call<JsonObject> call = apiService.getTemplateCreatorProfile(templateId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject creatorResponse = response.body();
                    Log.d(TAG, "Creator profile fetched successfully for template: " + templateId);
                    
                    // Cache the result if successful
                    if (creatorResponse.has("success") && creatorResponse.get("success").getAsBoolean()) {
                        creatorProfileCache.put(templateId, creatorResponse);
                        Log.d(TAG, "Creator profile cached for template: " + templateId);
                    }
                    
                    result.setValue(creatorResponse);
                } else {
                    Log.e(TAG, "Failed to fetch creator profile for template: " + templateId + 
                          ", Response code: " + response.code());
                    
                    // Create fallback response
                    JsonObject fallbackResponse = createFallbackResponse(templateId);
                    result.setValue(fallbackResponse);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error fetching creator profile for template: " + templateId, t);
                
                // Create fallback response
                JsonObject fallbackResponse = createFallbackResponse(templateId);
                result.setValue(fallbackResponse);
            }
        });
        
        return result;
    }

    /**
     * Get creator profiles for multiple templates in batch
     * More efficient than individual calls
     * 
     * @param templateIds List of template IDs
     * @return LiveData containing the batch response
     */
    public LiveData<JsonObject> getBatchCreatorProfiles(List<String> templateIds) {
        MutableLiveData<JsonObject> result = new MutableLiveData<>();
        
        Log.d(TAG, "Fetching batch creator profiles for " + templateIds.size() + " templates");
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("templateIds", templateIds);
        
        Call<JsonObject> call = apiService.getBatchCreatorProfiles(requestBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject batchResponse = response.body();
                    Log.d(TAG, "Batch creator profiles fetched successfully");
                    
                    // Cache individual profiles from batch response
                    if (batchResponse.has("success") && batchResponse.get("success").getAsBoolean()) {
                        if (batchResponse.has("data") && batchResponse.get("data").isJsonArray()) {
                            JsonArray dataArray = batchResponse.getAsJsonArray("data");
                            for (JsonElement element : dataArray) {
                                if (element.isJsonObject()) {
                                    JsonObject profileData = element.getAsJsonObject();
                                    if (profileData.has("templateId")) {
                                        String templateId = profileData.get("templateId").getAsString();
                                        
                                        // Create individual response for caching
                                        JsonObject individualResponse = new JsonObject();
                                        individualResponse.addProperty("success", true);
                                        individualResponse.add("data", profileData);
                                        
                                        creatorProfileCache.put(templateId, individualResponse);
                                    }
                                }
                            }
                            Log.d(TAG, "Cached " + dataArray.size() + " creator profiles from batch");
                        }
                    }
                    
                    result.setValue(batchResponse);
                } else {
                    Log.e(TAG, "Failed to fetch batch creator profiles, Response code: " + response.code());
                    
                    // Create fallback batch response
                    JsonObject fallbackResponse = createFallbackBatchResponse(templateIds);
                    result.setValue(fallbackResponse);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error fetching batch creator profiles", t);
                
                // Create fallback batch response
                JsonObject fallbackResponse = createFallbackBatchResponse(templateIds);
                result.setValue(fallbackResponse);
            }
        });
        
        return result;
    }

    /**
     * Get detailed statistics for a creator
     * 
     * @param userId The user/creator ID
     * @return LiveData containing the creator statistics
     */
    public LiveData<JsonObject> getCreatorStats(String userId) {
        MutableLiveData<JsonObject> result = new MutableLiveData<>();
        
        Log.d(TAG, "Fetching creator statistics for user: " + userId);
        
        Call<JsonObject> call = apiService.getCreatorStats(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Creator statistics fetched successfully for user: " + userId);
                    result.setValue(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch creator statistics for user: " + userId + 
                          ", Response code: " + response.code());
                    
                    // Create error response
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("success", false);
                    errorResponse.addProperty("message", "Failed to fetch creator statistics");
                    result.setValue(errorResponse);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error fetching creator statistics for user: " + userId, t);
                
                // Create error response
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("message", "Network error: " + t.getMessage());
                result.setValue(errorResponse);
            }
        });
        
        return result;
    }

    /**
     * Check template interactions health
     * 
     * @return LiveData containing the health status
     */
    public LiveData<JsonObject> getTemplateInteractionsHealth() {
        MutableLiveData<JsonObject> result = new MutableLiveData<>();
        
        Log.d(TAG, "Checking template interactions health");
        
        Call<JsonObject> call = apiService.getTemplateInteractionsHealth();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Template interactions health check successful");
                    result.setValue(response.body());
                } else {
                    Log.e(TAG, "Failed to get template interactions health, Response code: " + response.code());
                    
                    // Create error response
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("success", false);
                    errorResponse.addProperty("message", "Health check failed");
                    result.setValue(errorResponse);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error during health check", t);
                
                // Create error response
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("message", "Network error: " + t.getMessage());
                result.setValue(errorResponse);
            }
        });
        
        return result;
    }

    /**
     * Clear all cached creator profiles
     */
    public void clearCache() {
        creatorProfileCache.clear();
        Log.d(TAG, "Creator profile cache cleared");
    }

    /**
     * Clear cached creator profile for a specific template
     * 
     * @param templateId The template ID to clear from cache
     */
    public void clearCacheForTemplate(String templateId) {
        creatorProfileCache.remove(templateId);
        Log.d(TAG, "Creator profile cache cleared for template: " + templateId);
    }

    /**
     * Create a fallback response when API fails
     * 
     * @param templateId The template ID
     * @return Fallback JsonObject response
     */
    private JsonObject createFallbackResponse(String templateId) {
        Log.d(TAG, "Creating fallback response for template: " + templateId);
        
        JsonObject fallbackResponse = new JsonObject();
        fallbackResponse.addProperty("success", true);
        fallbackResponse.addProperty("message", "Using fallback creator profile");
        
        JsonObject data = new JsonObject();
        data.addProperty("templateId", templateId);
        data.addProperty("creatorSource", "fallback");
        
        JsonObject creatorProfile = new JsonObject();
        creatorProfile.addProperty("creatorName", "eventwish");
        creatorProfile.addProperty("creatorProfilePhoto", "");
        creatorProfile.addProperty("creatorId", "");
        creatorProfile.addProperty("generatedByUser", "eventwish");
        
        data.add("creatorProfile", creatorProfile);
        fallbackResponse.add("data", data);
        
        return fallbackResponse;
    }

    /**
     * Create a fallback batch response when API fails
     * 
     * @param templateIds List of template IDs
     * @return Fallback JsonObject batch response
     */
    private JsonObject createFallbackBatchResponse(List<String> templateIds) {
        Log.d(TAG, "Creating fallback batch response for " + templateIds.size() + " templates");
        
        JsonObject fallbackResponse = new JsonObject();
        fallbackResponse.addProperty("success", true);
        fallbackResponse.addProperty("message", "Using fallback creator profiles");
        
        JsonArray dataArray = new JsonArray();
        for (String templateId : templateIds) {
            JsonObject profileData = new JsonObject();
            profileData.addProperty("templateId", templateId);
            profileData.addProperty("creatorSource", "fallback");
            
            JsonObject creatorProfile = new JsonObject();
            creatorProfile.addProperty("creatorName", "eventwish");
            creatorProfile.addProperty("creatorProfilePhoto", "");
            creatorProfile.addProperty("creatorId", "");
            creatorProfile.addProperty("generatedByUser", "eventwish");
            
            profileData.add("creatorProfile", creatorProfile);
            dataArray.add(profileData);
        }
        
        fallbackResponse.add("data", dataArray);
        
        JsonObject summary = new JsonObject();
        summary.addProperty("totalRequested", templateIds.size());
        summary.addProperty("totalFound", templateIds.size());
        summary.addProperty("totalFallback", templateIds.size());
        fallbackResponse.add("summary", summary);
        
        return fallbackResponse;
    }
} 