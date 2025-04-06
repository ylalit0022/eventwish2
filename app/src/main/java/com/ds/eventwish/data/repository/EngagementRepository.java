package com.ds.eventwish.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // Failure tracking to avoid overwhelming the server
    private int failureCounter = 0;
    private static final int MAX_FAILURES = 5;
    private boolean endpointMissing = false;
    
    // Rate limiting for category visits - prevent duplicate visits
    private final Map<String, Long> lastCategoryVisitTime = new ConcurrentHashMap<>();
    private final Map<String, Long> lastTemplateViewTime = new ConcurrentHashMap<>();
    private static final long MINIMUM_VISIT_INTERVAL_MS = 60000; // 1 minute between visits to same category
    private static final long MINIMUM_TEMPLATE_VIEW_INTERVAL_MS = 300000; // 5 minutes between views of same template
    
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
        
        // Initialize failure tracking
        this.failureCounter = 0;
        this.endpointMissing = false;
        
        Log.d(TAG, "EngagementRepository initialized");
        
        // Schedule cleanup of old engagement data
        cleanupOldEngagementData();
        
        // Schedule regular cleanup of rate limiting maps
        scheduleRateLimitMapCleanup();
    }
    
    /**
     * Schedule a regular cleanup of rate limiting maps to prevent memory leaks
     */
    private void scheduleRateLimitMapCleanup() {
        // Run cleanup every hour
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cleanupRateLimitMaps();
                // Schedule next cleanup
                new Handler(Looper.getMainLooper()).postDelayed(this, TimeUnit.HOURS.toMillis(1));
            }
        }, TimeUnit.HOURS.toMillis(1));
    }
    
    /**
     * Clean up rate limit maps by removing old entries
     */
    private void cleanupRateLimitMaps() {
        try {
            long currentTime = System.currentTimeMillis();
            int categoryCount = 0;
            int templateCount = 0;
            
            // Remove old category visit entries (older than 24 hours)
            Iterator<Map.Entry<String, Long>> categoryIterator = lastCategoryVisitTime.entrySet().iterator();
            while (categoryIterator.hasNext()) {
                Map.Entry<String, Long> entry = categoryIterator.next();
                if (currentTime - entry.getValue() > TimeUnit.HOURS.toMillis(24)) {
                    categoryIterator.remove();
                    categoryCount++;
                }
            }
            
            // Remove old template view entries (older than 24 hours)
            Iterator<Map.Entry<String, Long>> templateIterator = lastTemplateViewTime.entrySet().iterator();
            while (templateIterator.hasNext()) {
                Map.Entry<String, Long> entry = templateIterator.next();
                if (currentTime - entry.getValue() > TimeUnit.HOURS.toMillis(24)) {
                    templateIterator.remove();
                    templateCount++;
                }
            }
            
            Log.d(TAG, "Cleaned up rate limit maps - removed " + categoryCount + 
                  " old category entries and " + templateCount + " old template entries");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up rate limit maps", e);
        }
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
        
        // Check if we've recently tracked this template to prevent duplicates
        Long lastViewTime = lastTemplateViewTime.get(templateId);
        long currentTime = System.currentTimeMillis();
        
        if (lastViewTime != null && currentTime - lastViewTime < MINIMUM_TEMPLATE_VIEW_INTERVAL_MS) {
            Log.d(TAG, "Skipping duplicate template view for: " + templateId + 
                  " (category: " + category + ") - last view was " + (currentTime - lastViewTime) + "ms ago");
            return;
        }
        
        // Update the last view time
        lastTemplateViewTime.put(templateId, currentTime);
        
        // Validate source
        String validSource = validateSource(source);
        
        // Create engagement data
        final EngagementData data = new EngagementData(
            EngagementData.TYPE_TEMPLATE_VIEW, 
            templateId, 
            category, 
            durationMs,
            engagementScore,
            validSource
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
     * Track user visit to a category
     * @param category Category name
     * @param source Source of visit (e.g. direct, recommendation)
     */
    public void trackCategoryVisit(String category, String source) {
        if (category == null || category.isEmpty()) {
            Log.w(TAG, "Cannot track visit - category is null or empty");
            return;
        }
        
        // Check if we've recently tracked this category to prevent duplicates
        Long lastVisitTime = lastCategoryVisitTime.get(category);
        long currentTime = System.currentTimeMillis();
        
        if (lastVisitTime != null && currentTime - lastVisitTime < MINIMUM_VISIT_INTERVAL_MS) {
            Log.d(TAG, "Skipping duplicate category visit to: " + category + 
                  " - last visit was " + (currentTime - lastVisitTime) + "ms ago");
            return;
        }
        
        // Update the last visit time
        lastCategoryVisitTime.put(category, currentTime);
        
        // Validate source to ensure it's a valid enum value accepted by the server
        String validSource = validateSource(source);
        
        // Create engagement data with validated source
        final EngagementData data = new EngagementData(category, validSource);
        
        // Save to local database
        executors.diskIO().execute(() -> {
            engagementDataDao.insert(data);
            Log.d(TAG, "Recorded category visit to: " + category + " from: " + validSource);
        });
            
        // Try to sync immediately if network available
            if (networkUtils.isNetworkAvailable()) {
            executors.networkIO().execute(() -> {
                boolean success = syncEngagement(data);
                if (!success) {
                    // If sync failed, mark for later sync
                    data.setSyncPending(true);
                    executors.diskIO().execute(() -> engagementDataDao.update(data));
                    
                    // Also try a direct API call as backup - but avoid making duplicate requests
                    if (currentTime - lastVisitTime > MINIMUM_VISIT_INTERVAL_MS * 2) {
                        directUpdateUserActivity(category);
                    }
                }
            });
        } else {
            // Mark for later sync
            data.setSyncPending(true);
            executors.diskIO().execute(() -> engagementDataDao.update(data));
        }
    }
    
    /**
     * Validate and normalize source to ensure it's an allowed enum value
     * @param source The source string from the user interface
     * @return A valid source value for the API
     */
    private String validateSource(String source) {
        // If source is null or empty, default to direct
        if (source == null || source.isEmpty()) {
            return EngagementData.SOURCE_DIRECT;
        }
        
        // Check if source is already a valid enum value
        if (source.equals(EngagementData.SOURCE_DIRECT) || 
            source.equals(EngagementData.SOURCE_RECOMMENDATION) ||
            source.equals(EngagementData.SOURCE_SEARCH) ||
            source.equals(EngagementData.SOURCE_HISTORY)) {
            return source;
        }
        
        // Map common UI strings to valid enum values
        if (source.contains("direct") || source.equals("bottomsheet") || 
            source.equals("main") || source.equals("click")) {
            return EngagementData.SOURCE_DIRECT;
        } else if (source.contains("rec") || source.contains("suggestion")) {
            return EngagementData.SOURCE_RECOMMENDATION;
        } else if (source.contains("search")) {
            return EngagementData.SOURCE_SEARCH;
        } else if (source.contains("history")) {
            return EngagementData.SOURCE_HISTORY;
        }
        
        // Default to direct for any unrecognized source
        return EngagementData.SOURCE_DIRECT;
    }
    
    /**
     * Make a direct API call to update user activity with a category visit
     * This is a backup method that avoids the usual rate limiting
     * @param category The category that was visited
     */
    private void directUpdateUserActivity(String category) {
        if (!networkUtils.isNetworkAvailable() || !userRepository.isUserRegistered()) {
            return;
        }
        
        try {
            // Create request body with valid source value from the allowed enum
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("deviceId", userRepository.getDeviceId());
            requestBody.put("category", category);
            requestBody.put("timestamp", System.currentTimeMillis());
            
            // Use only standard source values that are allowed by the server's enum
            // SOURCE_DIRECT = "direct" is one of the predefined values
            requestBody.put("source", EngagementData.SOURCE_DIRECT);
            
            // Add request ID to avoid duplicate detection but don't include in source
            requestBody.put("requestId", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
            
            // Call the API directly
            apiService.updateUserActivity(requestBody).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Direct API call for category visit succeeded: " + category);
                    } else {
                        Log.w(TAG, "Direct API call for category visit failed: " + response.code() + 
                             ", category: " + category);
                        
                        // Log more details about the error
                        try {
                            if (response.errorBody() != null) {
                                Log.e(TAG, "Error body: " + response.errorBody().string());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                }
                
                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error in direct API call for category visit: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in direct API call", e);
        }
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
                
                // Send to server
                apiService.syncEngagementData(requestBody).enqueue(new Callback<JsonObject>() {
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
     * @return true if the sync was successful or queued, false if it failed
     */
    public boolean syncEngagement(EngagementData data) {
        if (!networkUtils.isNetworkAvailable()) {
            Log.d(TAG, "Network not available, marking for later sync: " + data.getId());
            data.setSyncPending(true);
            executors.diskIO().execute(() -> engagementDataDao.update(data));
            return false;
        }
        
        if (!userRepository.isUserRegistered()) {
            Log.d(TAG, "User not registered, cannot sync: " + data.getId());
            return false;
        }
        
        // Check if we've reached the retry limit for this session
        if (shouldSkipServerSync()) {
            Log.d(TAG, "Skipping server sync due to previous failures - storing locally only");
            // Still mark as synced locally to avoid repeated attempts
            executors.diskIO().execute(() -> {
                data.setSynced(true);
                engagementDataDao.update(data);
            });
            return true; // Consider this a "success" since we've handled it appropriately
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
        
        // Add a unique identifier to ensure this request is not considered a duplicate
        requestBody.put("requestId", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        
        // Use a CountDownLatch to make this call synchronous
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        
        // Send to server
        try {
        apiService.recordEngagement(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, 
                                   @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Mark as synced
                    executors.diskIO().execute(() -> {
                        data.setSynced(true);
                        engagementDataDao.update(data);
                            Log.d(TAG, "Engagement synced to server: " + data.getId() + ", category: " + data.getCategory());
                    });
                    // Reset failure counter on success
                    resetFailureCounter();
                        success.set(true);
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
                            success.set(true); // Still consider this a "success" for the caller
                    } else {
                        incrementFailureCounter();
                            Log.e(TAG, "Failed to sync engagement: " + response.code() + 
                                ", category: " + data.getCategory() +
                                ", details: " + (response.errorBody() != null ? response.errorBody().toString() : ""));
                            
                            // Log more details about the error
                            try {
                                if (response.errorBody() != null) {
                                    Log.e(TAG, "Error body: " + response.errorBody().string());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                    }
                    latch.countDown();
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                incrementFailureCounter();
                    Log.e(TAG, "Error syncing engagement: " + t.getMessage());
                    if (t.getCause() != null) {
                        Log.e(TAG, "Cause: " + t.getCause().getMessage());
                    }
                    latch.countDown();
                }
            });
            
            // Wait for the response with a timeout
            try {
                boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (!completed) {
                    Log.e(TAG, "Sync timed out, will retry later for: " + data.getCategory());
                    return false;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Sync interrupted", e);
                Thread.currentThread().interrupt();
                return false;
            }
            
            return success.get();
        } catch (Exception e) {
            Log.e(TAG, "Exception during sync", e);
            return false;
        }
    }
    
    /**
     * Reset the failure counter when a sync succeeds
     */
    private void resetFailureCounter() {
        this.failureCounter = 0;
    }
    
    /**
     * Increment the failure counter when a sync fails
     */
    private void incrementFailureCounter() {
        this.failureCounter++;
        Log.d(TAG, "Sync failure counter: " + this.failureCounter);
    }
    
    /**
     * Check if we should skip server sync based on failure counter
     * @return true if we should skip server sync
     */
    private boolean shouldSkipServerSync() {
        return this.endpointMissing || this.failureCounter >= MAX_FAILURES;
    }
    
    /**
     * Mark API endpoint as missing (404)
     */
    private void handleApiEndpointMissing() {
        this.endpointMissing = true;
        Log.w(TAG, "API endpoint is missing (404), will skip future sync attempts");
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
    
    /**
     * Sync all pending engagements to the server
     * Should be called periodically or when network becomes available
     * @return The number of successfully synced records
     */
    public int syncPendingEngagements() {
        if (!networkUtils.isNetworkAvailable() || !userRepository.isUserRegistered()) {
            Log.d(TAG, "Cannot sync pending engagements - network unavailable or user not registered");
            return 0;
        }
        
        try {
            // Get all pending engagements
            List<EngagementData> pendingEngagements = engagementDataDao.getPendingEngagements();
            
            if (pendingEngagements.isEmpty()) {
                Log.d(TAG, "No pending engagements to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pendingEngagements.size() + " pending engagements");
            
            int successCount = 0;
            for (EngagementData data : pendingEngagements) {
                // Try to sync each engagement with a small delay between
                if (syncEngagement(data)) {
                    successCount++;
                    
                    // Add a small delay to prevent overwhelming the server
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Sync interrupted", e);
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Successfully synced " + successCount + " out of " + pendingEngagements.size() + " engagements");
            return successCount;
        } catch (Exception e) {
            Log.e(TAG, "Error syncing pending engagements", e);
            return 0;
        }
    }
    
    /**
     * Check if there are any pending engagements that need to be synced
     * @return true if there are pending engagements
     */
    public boolean hasPendingEngagements() {
        try {
            int count = engagementDataDao.getPendingEngagementCount();
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking for pending engagements", e);
            return false;
        }
    }
} 