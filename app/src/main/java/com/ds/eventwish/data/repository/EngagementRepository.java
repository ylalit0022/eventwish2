package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.EngagementDataDao;
import com.ds.eventwish.data.model.EngagementData;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing user engagement data
 */
public class EngagementRepository {
    private static final String TAG = "EngagementRepository";
    
    // Constants
    private static final int MAX_SYNC_BATCH_SIZE = 50;
    private static final long MAX_ENGAGEMENT_AGE_DAYS = 90;
    
    // Singleton instance
    private static volatile EngagementRepository instance;
    
    // Dependencies
    private final Context context;
    private final EngagementDataDao engagementDataDao;
    private final ApiService apiService;
    private final AppExecutors executors;
    private final NetworkUtils networkUtils;
    private final UserRepository userRepository;
    private final Gson gson;
    
    /**
     * Get singleton instance of EngagementRepository
     * @param context Application context
     * @return EngagementRepository instance
     */
    public static synchronized EngagementRepository getInstance(Context context) {
        if (instance == null) {
            instance = new EngagementRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private EngagementRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.engagementDataDao = database.engagementDataDao();
        this.apiService = ApiClient.getClient();
        this.executors = AppExecutors.getInstance();
        this.networkUtils = NetworkUtils.getInstance(context);
        this.userRepository = UserRepository.getInstance(context);
        this.gson = new Gson();
        
        Log.d(TAG, "EngagementRepository initialized");
        
        // Schedule cleanup of old engagement data
        cleanupOldEngagementData();
    }
    
    /**
     * Track a template view with detailed engagement metrics
     * @param templateId Template ID
     * @param category Category name
     * @param durationMs View duration in milliseconds
     * @param engagementScore Engagement score (1-5)
     * @param source Source of engagement
     */
    public void trackTemplateEngagement(String templateId, String category,
                                        long durationMs, int engagementScore, String source) {
        if (templateId == null || category == null) {
            Log.w(TAG, "Cannot track engagement: templateId or category is null");
            return;
        }
        
        // Create engagement data
        final EngagementData data = new EngagementData(
            EngagementData.TYPE_TEMPLATE_VIEW, 
            templateId, 
            category, 
            durationMs,
            engagementScore,
            source
        );
        
        // Save locally in background thread
        executors.diskIO().execute(() -> {
            engagementDataDao.insert(data);
            Log.d(TAG, "Saved template engagement locally: " + templateId + ", category: " + category);
            
            // Also try to sync to server if online
            if (networkUtils.isNetworkAvailable()) {
                syncEngagement(data);
            }
        });
        
        // Also track with UserRepository for backward compatibility
        userRepository.trackTemplateView(templateId, category);
    }
    
    /**
     * Track a template view (simplified version)
     * @param templateId Template ID
     * @param category Category name
     * @param source Source of engagement
     */
    public void trackTemplateView(String templateId, String category, String source) {
        trackTemplateEngagement(templateId, category, 0, 2, source);
    }
    
    /**
     * Track a category visit
     * @param category Category name
     * @param source Source of engagement
     */
    public void trackCategoryVisit(String category, String source) {
        if (category == null) {
            Log.w(TAG, "Cannot track category visit: category is null");
            return;
        }
        
        // Create engagement data
        final EngagementData data = new EngagementData(category, source);
        
        // Save locally in background thread
        executors.diskIO().execute(() -> {
            engagementDataDao.insert(data);
            Log.d(TAG, "Saved category visit locally: " + category);
            
            // Also try to sync to server if online
            if (networkUtils.isNetworkAvailable()) {
                syncEngagement(data);
            }
        });
        
        // Also track with UserRepository for backward compatibility
        userRepository.updateUserActivity(category);
    }
    
    /**
     * Track template usage (e.g., template selected for editing)
     * @param templateId Template ID
     * @param category Category name
     */
    public void trackTemplateUsage(String templateId, String category) {
        if (templateId == null || category == null) {
            Log.w(TAG, "Cannot track usage: templateId or category is null");
            return;
        }
        
        // Create engagement data with higher score for actual usage
        final EngagementData data = new EngagementData(
            EngagementData.TYPE_TEMPLATE_USE, 
            templateId, 
            category, 
            EngagementData.SOURCE_DIRECT
        );
        
        // Save locally in background thread
        executors.diskIO().execute(() -> {
            engagementDataDao.insert(data);
            Log.d(TAG, "Saved template usage locally: " + templateId);
            
            // Also try to sync to server if online
            if (networkUtils.isNetworkAvailable()) {
                syncEngagement(data);
            }
        });
    }
    
    /**
     * Record explicit feedback (like/dislike)
     * @param templateId Template ID
     * @param category Category name
     * @param isLike true for like, false for dislike
     */
    public void recordFeedback(String templateId, String category, boolean isLike) {
        if (templateId == null || category == null) {
            Log.w(TAG, "Cannot record feedback: templateId or category is null");
            return;
        }
        
        // Create engagement data with appropriate type
        final EngagementData data = new EngagementData(
            isLike ? EngagementData.TYPE_EXPLICIT_LIKE : EngagementData.TYPE_EXPLICIT_DISLIKE,
            templateId,
            category,
            0,
            isLike ? 5 : 1,
            EngagementData.SOURCE_DIRECT
        );
        
        // Save locally in background thread
        executors.diskIO().execute(() -> {
            engagementDataDao.insert(data);
            Log.d(TAG, "Saved explicit " + (isLike ? "like" : "dislike") + 
                  " for template: " + templateId);
            
            // Also try to sync to server if online
            if (networkUtils.isNetworkAvailable()) {
                syncEngagement(data);
            }
        });
    }
    
    /**
     * Get category weights based on user activity
     * @return Map of category to weight (0.0 to 1.0)
     */
    public Map<String, Float> getCategoryWeights() {
        Map<String, Float> weights = new HashMap<>();
        
        executors.diskIO().execute(() -> {
            // Get the total count of all engagements first
            List<EngagementDataDao.CategoryCount> categoryCounts = 
                engagementDataDao.getCategoryWeights();
            
            int totalCount = 0;
            for (EngagementDataDao.CategoryCount count : categoryCounts) {
                totalCount += count.count;
            }
            
            // Calculate normalized weights (0.0 to 1.0)
            if (totalCount > 0) {
                for (EngagementDataDao.CategoryCount count : categoryCounts) {
                    float normalizedWeight = (float) count.count / totalCount;
                    weights.put(count.category, normalizedWeight);
                }
            }
        });
        
        return weights;
    }
    
    /**
     * Get recently viewed templates
     * @param limit Maximum number of templates to return
     * @return List of recently viewed template IDs
     */
    public List<String> getRecentlyViewedTemplates(int limit) {
        final List<String> result = new ArrayList<>();
        
        executors.diskIO().execute(() -> {
            List<EngagementData> recentViews = engagementDataDao.getByTypeLive(
                EngagementData.TYPE_TEMPLATE_VIEW).getValue();
            
            if (recentViews != null) {
                // Extract template IDs and avoid duplicates
                Set<String> uniqueIds = new HashSet<>();
                for (EngagementData data : recentViews) {
                    if (data.getTemplateId() != null && uniqueIds.size() < limit) {
                        uniqueIds.add(data.getTemplateId());
                    }
                    
                    if (uniqueIds.size() >= limit) {
                        break;
                    }
                }
                
                result.addAll(uniqueIds);
            }
        });
        
        return result;
    }
    
    /**
     * Get all engagement data
     * @return LiveData list of all engagement data
     */
    public LiveData<List<EngagementData>> getAllEngagementData() {
        return engagementDataDao.getAllLive();
    }
    
    /**
     * Sync all unsynced engagement data to server
     */
    public void syncUnsynced() {
        if (!networkUtils.isNetworkAvailable()) {
            Log.d(TAG, "Skipping sync: No network connection");
            return;
        }
        
        if (!userRepository.isUserRegistered()) {
            Log.d(TAG, "Skipping sync: User not registered");
            return;
        }
        
        // Skip if endpoint is known to be missing or too many failures
        if (shouldSkipServerSync()) {
            Log.d(TAG, "Skipping batch sync due to previous failures - will mark as synced locally");
            executors.diskIO().execute(() -> {
                List<EngagementData> unsynced = engagementDataDao.getUnsynced();
                if (!unsynced.isEmpty()) {
                    List<String> allIds = new ArrayList<>();
                    for (EngagementData data : unsynced) {
                        allIds.add(data.getId());
                    }
                    engagementDataDao.markAsSynced(allIds);
                    Log.d(TAG, "Marked " + allIds.size() + " engagement records as locally synced (server sync skipped)");
                }
            });
            return;
        }
        
        executors.diskIO().execute(() -> {
            List<EngagementData> unsynced = engagementDataDao.getUnsynced();
            
            if (unsynced.isEmpty()) {
                Log.d(TAG, "No unsynced engagement data to sync");
                return;
            }
            
            Log.d(TAG, "Syncing " + unsynced.size() + " unsynced engagement records");
            
            // Process in batches to avoid overloading the server
            int total = unsynced.size();
            int processed = 0;
            
            while (processed < total) {
                int batchSize = Math.min(MAX_SYNC_BATCH_SIZE, total - processed);
                List<EngagementData> batch = unsynced.subList(processed, processed + batchSize);
                
                // Convert to JSON array
                JsonObject requestBody = new JsonObject();
                requestBody.add("engagements", gson.toJsonTree(batch));
                
                // Add device ID
                String deviceId = userRepository.getDeviceId();
                if (deviceId != null) {
                    requestBody.addProperty("deviceId", deviceId);
                }
                
                // Create a list of IDs for this batch for marking as synced
                List<String> batchIds = new ArrayList<>();
                for (EngagementData data : batch) {
                    batchIds.add(data.getId());
                }
                
                // Get auth token - use empty string as we're using device ID for legacy authentication
                String authToken = "";
                
                // Send to server
                apiService.syncEngagementData(requestBody, authToken).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, 
                                           @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            // Mark as synced
                            executors.diskIO().execute(() -> {
                                engagementDataDao.markAsSynced(batchIds);
                                Log.d(TAG, "Marked " + batchIds.size() + " engagement records as synced");
                            });
                            // Reset failure counter on success
                            resetFailureCounter();
                        } else {
                            // Still mark as synced locally if we get a 404
                            if (response.code() == 404) {
                                handleApiEndpointMissing();
                                executors.diskIO().execute(() -> {
                                    engagementDataDao.markAsSynced(batchIds);
                                    Log.d(TAG, "Endpoint missing, marking batch as completed locally");
                                });
                            } else {
                                incrementFailureCounter();
                                Log.e(TAG, "Failed to sync engagement data: " + response.code());
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        incrementFailureCounter();
                        Log.e(TAG, "Error syncing engagement data", t);
                    }
                });
                
                processed += batchSize;
            }
        });
    }
    
    /**
     * Sync a single engagement data record to server
     * @param data Engagement data to sync
     */
    private void syncEngagement(EngagementData data) {
        if (!networkUtils.isNetworkAvailable() || !userRepository.isUserRegistered()) {
            return;
        }
        
        // Check if we've reached the retry limit for this session
        if (shouldSkipServerSync()) {
            Log.d(TAG, "Skipping server sync due to previous failures - storing locally only");
            // Still mark as synced locally to avoid repeated attempts
            executors.diskIO().execute(() -> {
                data.setSynced(true);
                engagementDataDao.update(data);
            });
            return;
        }
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", userRepository.getDeviceId());
        requestBody.put("type", data.getType());
        
        if (data.getTemplateId() != null) {
            requestBody.put("templateId", data.getTemplateId());
        }
        
        if (data.getCategory() != null) {
            requestBody.put("category", data.getCategory());
        }
        
        requestBody.put("timestamp", data.getTimestamp());
        requestBody.put("durationMs", data.getDurationMs());
        requestBody.put("engagementScore", data.getEngagementScore());
        
        if (data.getSource() != null) {
            requestBody.put("source", data.getSource());
        }
        
        // Get auth token - use empty string as we're using device ID for legacy authentication
        String authToken = "";
        
        // Send to server
        apiService.recordEngagement(requestBody, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, 
                                   @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Mark as synced
                    executors.diskIO().execute(() -> {
                        data.setSynced(true);
                        engagementDataDao.update(data);
                        Log.d(TAG, "Engagement synced to server: " + data.getId());
                    });
                    // Reset failure counter on success
                    resetFailureCounter();
                } else {
                    // Still mark as synced locally if we get a 404
                    // (indicates the endpoint doesn't exist on server)
                    if (response.code() == 404) {
                        handleApiEndpointMissing();
                        executors.diskIO().execute(() -> {
                            data.setSynced(true);
                            engagementDataDao.update(data);
                            Log.d(TAG, "Endpoint missing, marking sync as completed locally");
                        });
                    } else {
                        incrementFailureCounter();
                        Log.e(TAG, "Failed to sync engagement: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                incrementFailureCounter();
                Log.e(TAG, "Error syncing engagement", t);
            }
        });
    }
    
    // Track API failures to avoid repeated attempts
    private static final String PREF_ENGAGEMENT_FAILURES = "engagement_sync_failures";
    private static final String PREF_ENDPOINT_MISSING = "engagement_endpoint_missing";
    private static final int MAX_FAILURES_BEFORE_SKIP = 5;
    
    private void incrementFailureCounter() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("engagement_prefs", Context.MODE_PRIVATE);
        int failures = prefs.getInt(PREF_ENGAGEMENT_FAILURES, 0);
        prefs.edit().putInt(PREF_ENGAGEMENT_FAILURES, failures + 1).apply();
    }
    
    private void resetFailureCounter() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("engagement_prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_ENGAGEMENT_FAILURES, 0).apply();
    }
    
    private void handleApiEndpointMissing() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("engagement_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENDPOINT_MISSING, true).apply();
        Log.w(TAG, "Engagement tracking API endpoint not found on server, will store locally only");
    }
    
    private boolean shouldSkipServerSync() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("engagement_prefs", Context.MODE_PRIVATE);
        // Skip if endpoint has been confirmed missing or if we've had too many failures
        return prefs.getBoolean(PREF_ENDPOINT_MISSING, false) || 
               prefs.getInt(PREF_ENGAGEMENT_FAILURES, 0) >= MAX_FAILURES_BEFORE_SKIP;
    }
    
    /**
     * Cleanup old engagement data to prevent database bloat
     */
    private void cleanupOldEngagementData() {
        executors.diskIO().execute(() -> {
            try {
                // Calculate cutoff time (90 days ago)
                long cutoffTime = System.currentTimeMillis() - 
                    TimeUnit.DAYS.toMillis(MAX_ENGAGEMENT_AGE_DAYS);
                
                int deleted = engagementDataDao.deleteOlderThan(cutoffTime);
                
                if (deleted > 0) {
                    Log.d(TAG, "Cleaned up " + deleted + " old engagement records");
                }
            } catch (Exception e) {
                // Catch any database exceptions to prevent app crashes
                Log.e(TAG, "Error cleaning up old engagement data", e);
            }
        });
    }
    
    /**
     * Get list of the most recently engaged categories
     * @param limit Maximum number of categories to return
     * @return List of categories sorted by recent engagement
     */
    public List<String> getRecentCategories(int limit) {
        try {
            return engagementDataDao.getMostRecentCategories(limit);
        } catch (Exception e) {
            Log.e(TAG, "Error getting recent categories", e);
            return new ArrayList<>();
        }
    }
} 