package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.SponsoredAdDao;
import com.ds.eventwish.data.local.entity.SponsoredAdEntity;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.model.response.SponsoredAdResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.DeviceUtils;
import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing sponsored ads from the API with local caching
 */
public class SponsoredAdRepository {
    private static final String TAG = "SponsoredAdRepository";
    private static final long CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(1); // Cache for 1 hour
    private static final long MIN_REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5); // Minimum 5 minutes between API refreshes
    private static final long IMPRESSION_THROTTLE_MS = TimeUnit.HOURS.toMillis(24); // Throttle impressions to once per day per ad
    private static final long RATE_LIMIT_BACKOFF_MS = TimeUnit.MINUTES.toMillis(60); // Respect server rate limits
    private static final int MAX_BATCH_SIZE = 10; // Maximum number of tracking events to batch
    private static final long BATCH_FLUSH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5); // Flush batch every 5 minutes
    private static final long MAX_BATCH_AGE_MS = TimeUnit.HOURS.toMillis(24); // Maximum age for cached events
    private static final int MAX_RETRY_ATTEMPTS = 3; // Maximum number of retry attempts
    
    private final ApiService apiService;
    private final SponsoredAdDao sponsoredAdDao;
    private final AppExecutors executors;
    private final MutableLiveData<List<SponsoredAd>> sponsoredAdsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final InternetConnectivityChecker connectivityChecker;
    
    // Track last impression time per ad to prevent duplicate tracking
    private final Map<String, Long> lastImpressionTimes = new HashMap<>();
    
    // Batch processing for tracking events
    private final List<TrackingEvent> pendingImpressions = Collections.synchronizedList(new ArrayList<>());
    private final List<TrackingEvent> pendingClicks = Collections.synchronizedList(new ArrayList<>());
    private final Handler batchHandler = new Handler(Looper.getMainLooper());
    private final Runnable batchProcessingRunnable = this::processPendingEvents;
    
    // Rate limiting
    private boolean isRateLimited = false;
    private long rateLimitExpiresAt = 0;
    private static final String PREF_NAME = "sponsored_ad_tracking";
    private static final String KEY_LAST_BATCH_FLUSH = "last_batch_flush";
    
    private boolean isInitialized = false;
    private long lastRefreshTime = 0;
    
    // Singleton instance
    private static volatile SponsoredAdRepository instance;
    
    // Add flag for forcing refresh
    private boolean forceRefresh = false;
    
    /**
     * Class to represent tracking events for batch processing
     */
    private static class TrackingEvent {
        final String adId;
        final String deviceId;
        final long timestamp;
        int retryCount = 0;
        
        TrackingEvent(String adId, String deviceId) {
            this.adId = adId;
            this.deviceId = deviceId;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Increment retry count
         * @return true if maximum retries not exceeded
         */
        boolean incrementRetry() {
            retryCount++;
            return retryCount <= MAX_RETRY_ATTEMPTS;
        }
        
        @Override
        public String toString() {
            return "TrackingEvent{" +
                   "adId='" + adId + '\'' +
                   ", timestamp=" + timestamp +
                   ", retryCount=" + retryCount +
                   '}';
        }
    }
    
    // Get singleton instance
    public static SponsoredAdRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SponsoredAdRepository.class) {
                if (instance == null) {
                    instance = new SponsoredAdRepository(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton
     */
    private SponsoredAdRepository(Context context) {
        this.apiService = ApiClient.getClient();
        this.sponsoredAdDao = AppDatabase.getInstance(context).sponsoredAdDao();
        this.executors = AppExecutors.getInstance();
        this.connectivityChecker = InternetConnectivityChecker.getInstance(context);
        
        Log.d(TAG, "SponsoredAdRepository initialized");
    }
    
    /**
     * Ensure the repository is initialized - clean expired cache entries
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Clean expired cache on background thread
        executors.diskIO().execute(() -> {
            long currentTime = System.currentTimeMillis();
            int deletedCount = sponsoredAdDao.deleteExpired(currentTime);
            Log.d(TAG, "Initialization complete. Deleted " + deletedCount + " expired cache entries");
            isInitialized = true;
            
            // Schedule batch processing
            scheduleBatchProcessing();
        });
    }
    
    /**
     * Schedule periodic batch processing with optimized flushing
     */
    private void scheduleBatchProcessing() {
        // Remove any existing callbacks
        batchHandler.removeCallbacks(batchProcessingRunnable);
        
        // Get the last flush time
        SharedPreferences prefs = EventWishApplication.getAppContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastFlushTime = prefs.getLong(KEY_LAST_BATCH_FLUSH, 0);
        long currentTime = System.currentTimeMillis();
        
        // If it's been too long since the last flush, do it immediately
        if (currentTime - lastFlushTime > BATCH_FLUSH_INTERVAL_MS) {
            Log.d(TAG, "Last batch flush was " + 
                  TimeUnit.MILLISECONDS.toMinutes(currentTime - lastFlushTime) + 
                  " minutes ago. Processing immediately.");
            batchHandler.post(batchProcessingRunnable);
        } else {
            // Otherwise schedule for regular interval
            long delayMs = BATCH_FLUSH_INTERVAL_MS - (currentTime - lastFlushTime);
            Log.d(TAG, "Scheduling next batch processing in " + 
                  TimeUnit.MILLISECONDS.toMinutes(delayMs) + " minutes");
            batchHandler.postDelayed(batchProcessingRunnable, delayMs);
        }
    }
    
    /**
     * Process pending tracking events with improved batching
     */
    private void processPendingEvents() {
        if (!connectivityChecker.isNetworkAvailable() || 
            (isRateLimited && System.currentTimeMillis() < rateLimitExpiresAt)) {
            // Reschedule if offline or rate limited
            Log.d(TAG, "Skipping batch processing due to " + 
                  (connectivityChecker.isNetworkAvailable() ? "rate limiting" : "connectivity") + 
                  " issues. Will try again later.");
            scheduleBatchProcessing();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Update the last flush time
        SharedPreferences prefs = EventWishApplication.getAppContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_BATCH_FLUSH, currentTime).apply();
        
        // Process impressions
        int impressionsProcessed = processImpressionBatch();
        
        // Process clicks
        int clicksProcessed = processClickBatch();
        
        // Log processing statistics
        if (impressionsProcessed > 0 || clicksProcessed > 0) {
            Log.d(TAG, "Batch processing completed: " + impressionsProcessed + 
                  " impressions and " + clicksProcessed + " clicks processed");
        }
        
        // Clean up stale events (older than MAX_BATCH_AGE_MS)
        cleanupStaleEvents(currentTime);
        
        // Reschedule
        scheduleBatchProcessing();
    }
    
    /**
     * Clean up stale events that are too old to process
     */
    private void cleanupStaleEvents(long currentTime) {
        int removedImpressions = 0;
        int removedClicks = 0;
        
        synchronized (pendingImpressions) {
            Iterator<TrackingEvent> iterator = pendingImpressions.iterator();
            while (iterator.hasNext()) {
                TrackingEvent event = iterator.next();
                if (currentTime - event.timestamp > MAX_BATCH_AGE_MS) {
                    iterator.remove();
                    removedImpressions++;
                }
            }
        }
        
        synchronized (pendingClicks) {
            Iterator<TrackingEvent> iterator = pendingClicks.iterator();
            while (iterator.hasNext()) {
                TrackingEvent event = iterator.next();
                if (currentTime - event.timestamp > MAX_BATCH_AGE_MS) {
                    iterator.remove();
                    removedClicks++;
                }
            }
        }
        
        if (removedImpressions > 0 || removedClicks > 0) {
            Log.d(TAG, "Cleaned up stale events: removed " + removedImpressions + 
                  " impressions and " + removedClicks + " clicks older than " + 
                  TimeUnit.MILLISECONDS.toHours(MAX_BATCH_AGE_MS) + " hours");
        }
    }
    
    /**
     * Process a batch of impression events
     * @return number of events processed
     */
    private int processImpressionBatch() {
        List<TrackingEvent> batchToProcess;
        
        synchronized (pendingImpressions) {
            if (pendingImpressions.isEmpty()) {
                return 0;
            }
            
            // Take up to MAX_BATCH_SIZE events
            int batchSize = Math.min(pendingImpressions.size(), MAX_BATCH_SIZE);
            batchToProcess = new ArrayList<>(pendingImpressions.subList(0, batchSize));
            
            // Remove processed events from the pending list
            for (int i = 0; i < batchSize; i++) {
                pendingImpressions.remove(0);
            }
        }
        
        if (batchToProcess.isEmpty()) {
            return 0;
        }
        
        Log.d(TAG, "Processing batch of " + batchToProcess.size() + " impression events");
        
        // Process each event
        int successCount = 0;
        for (TrackingEvent event : batchToProcess) {
            if (recordImpressionToServer(event.adId, event.deviceId)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * Process a batch of click events
     * @return number of events processed
     */
    private int processClickBatch() {
        List<TrackingEvent> batchToProcess;
        
        synchronized (pendingClicks) {
            if (pendingClicks.isEmpty()) {
                return 0;
            }
            
            // Take up to MAX_BATCH_SIZE events
            int batchSize = Math.min(pendingClicks.size(), MAX_BATCH_SIZE);
            batchToProcess = new ArrayList<>(pendingClicks.subList(0, batchSize));
            
            // Remove processed events from the pending list
            for (int i = 0; i < batchSize; i++) {
                pendingClicks.remove(0);
            }
        }
        
        if (batchToProcess.isEmpty()) {
            return 0;
        }
        
        Log.d(TAG, "Processing batch of " + batchToProcess.size() + " click events");
        
        // Process each event
        int successCount = 0;
        for (TrackingEvent event : batchToProcess) {
            if (recordClickToServer(event.adId, event.deviceId)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * Get all active sponsored ads from the server with caching
     * @return LiveData of sponsored ads list
     */
    public LiveData<List<SponsoredAd>> getSponsoredAds() {
        // Initialize repository if needed
        initialize();
        
        if (shouldRefreshFromNetwork() || forceRefresh) {
            refreshFromNetwork();
            // Reset force refresh flag
            forceRefresh = false;
        } else {
            loadFromCache();
        }
        
        return sponsoredAdsLiveData;
    }
    
    /**
     * Explicitly force a refresh from the network
     */
    public void refreshAds() {
        Log.d(TAG, "Forcing refresh of sponsored ads");
        refreshFromNetwork();
    }
    
    /**
     * Check if we should refresh the data from network
     */
    private boolean shouldRefreshFromNetwork() {
        // If force refresh is set, always refresh
        if (forceRefresh) {
            Log.d(TAG, "Forced refresh requested, refreshing from network");
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // If it's been longer than minimum refresh interval, refresh
        if (currentTime - lastRefreshTime > MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Min refresh interval elapsed, refreshing from network");
            return true;
        }
        
        // If network is not available, use cache
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, using cache");
            return false;
        }
        
        // Check if there are valid ads in cache
        executors.diskIO().execute(() -> {
            int validCount = sponsoredAdDao.countValid(currentTime);
            Log.d(TAG, "Found " + validCount + " valid ads in cache");
        });
        
        // Refresh at least once per app launch
        if (lastRefreshTime == 0) {
            Log.d(TAG, "First refresh since app launch, refreshing from network");
            return true;
        }
        
        return false;
    }
    
    /**
     * Load ads from local cache
     */
    private void loadFromCache() {
        executors.diskIO().execute(() -> {
            long currentTime = System.currentTimeMillis();
            List<SponsoredAdEntity> cachedEntities = sponsoredAdDao.getValidAds(currentTime);
            
            if (!cachedEntities.isEmpty()) {
                // Convert entities to models
                List<SponsoredAd> cachedAds = new ArrayList<>();
                for (SponsoredAdEntity entity : cachedEntities) {
                    cachedAds.add(entity.toModel());
                }
                
                Log.d(TAG, "Loaded " + cachedAds.size() + " sponsored ads from cache");
                sponsoredAdsLiveData.postValue(cachedAds);
            } else {
                Log.d(TAG, "No valid sponsored ads in cache");
                
                // Check if we're online and should fetch
                if (connectivityChecker.isNetworkAvailable() && shouldRefreshFromNetwork()) {
                    Log.d(TAG, "No cache but network available, refreshing from network");
                    refreshFromNetwork();
                }
            }
        });
    }
    
    /**
     * Refresh the data from the network
     */
    private void refreshFromNetwork() {
        Log.d(TAG, "Refreshing sponsored ads from network");
        loadingLiveData.setValue(true);
        
        // Prevent network calls if we're rate-limited
        if (isRateLimited && System.currentTimeMillis() < rateLimitExpiresAt) {
            Log.d(TAG, "Skipping network request due to rate limiting, expires in: " + 
                  (rateLimitExpiresAt - System.currentTimeMillis()) / 1000 + " seconds");
            loadingLiveData.setValue(false);
            errorLiveData.setValue("API rate limit exceeded. Please try again later.");
            return;
        }
        
        // If there's no network, load from cache and show message
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, loading from cache only");
            loadFromCache();
            loadingLiveData.setValue(false);
            errorLiveData.setValue("No internet connection. Showing cached ads.");
            return;
        }
        
        // Make the API call to get ads
        apiService.getSponsoredAds().enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                
                if (response.code() == 429) {
                    // Handle rate limiting
                    handleRateLimiting(response);
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    Log.d(TAG, "Sponsored ads API response: " + adResponse.toString());
                    
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        final List<SponsoredAd> ads = adResponse.getAds();
                        Log.d(TAG, "Loaded " + ads.size() + " sponsored ads from API");
                        
                        // Update LiveData
                        sponsoredAdsLiveData.postValue(ads);
                        
                        // Update cache on background thread
                        executors.diskIO().execute(() -> {
                            // Convert models to entities
                            List<SponsoredAdEntity> entities = new ArrayList<>();
                            for (SponsoredAd ad : ads) {
                                entities.add(new SponsoredAdEntity(ad, CACHE_DURATION_MS));
                            }
                            
                            // Replace all cached ads with new ones
                            sponsoredAdDao.replaceAll(entities);
                            Log.d(TAG, "Cached " + entities.size() + " sponsored ads");
                        });
                    } else {
                        String errorMsg = adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available";
                        errorLiveData.postValue(errorMsg);
                        Log.w(TAG, "Error loading sponsored ads: " + 
                               (adResponse.getError() != null ? adResponse.getError() : adResponse.getMessage()));
                    }
                } else {
                    String errorMsg = "Error fetching sponsored ads: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                            Log.e(TAG, "Error response body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                    
                    errorLiveData.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                String errorMsg = "Network error: " + t.getMessage();
                errorLiveData.postValue(errorMsg);
                Log.e(TAG, "Network error fetching sponsored ads", t);
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request headers: " + call.request().headers());
            }
        });
    }
    
    /**
     * Handle API rate limiting
     */
    private void handleRateLimiting(Response<?> response) {
        isRateLimited = true;
        rateLimitExpiresAt = System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MS;
        
        // Try to extract Retry-After header if available
        String retryAfter = response.headers().get("Retry-After");
        if (retryAfter != null && !retryAfter.isEmpty()) {
            try {
                // If Retry-After contains a number of seconds
                int seconds = Integer.parseInt(retryAfter.trim());
                rateLimitExpiresAt = System.currentTimeMillis() + (seconds * 1000L);
                Log.w(TAG, "Rate limited by server, retrying in " + seconds + " seconds (Retry-After header)");
            } catch (NumberFormatException e) {
                // If it's a HTTP date format, we'll just use our default backoff
                Log.w(TAG, "Rate limited by server, using default backoff of 60 minutes");
            }
        } else {
            Log.w(TAG, "Rate limited by server, retrying in 60 minutes (default backoff)");
        }
        
        String message = "Rate limited by server, retrying in " + (RATE_LIMIT_BACKOFF_MS / 60000) + " minutes";
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                message = "API rate limited: " + errorBody;
                Log.e(TAG, "Rate limiting error: " + errorBody);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing rate limit response", e);
        }
        
        errorLiveData.postValue(message);
        
        // Load from cache as fallback
        loadFromCache();
    }
    
    /**
     * Record an impression for a sponsored ad
     * @param adId The ID of the ad that was viewed
     */
    public void recordImpression(String adId) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            Log.e(TAG, "Cannot record impression - missing deviceId or adId");
            return;
        }
        
        Log.d(TAG, "IMPRESSION TRACKING: Starting impression recording process for ad: " + adId);
        
        // Check rate limiting for all API calls
        if (isRateLimited && System.currentTimeMillis() < rateLimitExpiresAt) {
            long remainingMinutes = (rateLimitExpiresAt - System.currentTimeMillis()) / 60000;
            Log.d(TAG, "IMPRESSION TRACKING: Skipping impression tracking - rate limited for " + remainingMinutes + " more minutes");
            
            // Still update local database
            updateLocalImpressionCount(adId);
            Log.d(TAG, "IMPRESSION TRACKING: Updated local database impression count only due to rate limiting");
            return;
        }
        
        // Create daily impression key - using just the date part ensures one impression per ad per day
        long currentTime = System.currentTimeMillis();
        String today = String.valueOf(currentTime / (1000 * 60 * 60 * 24)); // Convert to days since epoch
        SharedPreferences prefs = EventWishApplication.getAppContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        String impressionKey = "impression_" + adId + "_" + today;
        boolean alreadyTrackedToday = prefs.getBoolean(impressionKey, false);
        
        // Also check in-memory cache
        Long lastRecordedTime = lastImpressionTimes.get(adId);
        boolean recentlyTracked = lastRecordedTime != null && 
                                  (currentTime - lastRecordedTime) < IMPRESSION_THROTTLE_MS;
        
        if (alreadyTrackedToday || recentlyTracked) {
            Log.d(TAG, "IMPRESSION TRACKING: Throttling impression for ad: " + adId + 
                  " (already tracked today=" + alreadyTrackedToday + 
                  ", recently tracked=" + recentlyTracked + ", " +
                  (lastRecordedTime != null ? ((currentTime - lastRecordedTime) / 1000) + " seconds ago)" : "never tracked)"));
            return;
        }
        
        // Mark as tracked for today in persistent storage
        prefs.edit().putBoolean(impressionKey, true).apply();
        Log.d(TAG, "IMPRESSION TRACKING: Marked impression as tracked for today in SharedPreferences");
        
        // Update in-memory cache
        lastImpressionTimes.put(adId, currentTime);
        
        // Always update local database
        updateLocalImpressionCount(adId);
        Log.d(TAG, "IMPRESSION TRACKING: Updated local impression count in Room database");
        
        // IMPORTANT CHANGE: Send impression to server immediately if we have network
        if (connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "IMPRESSION TRACKING: Network available - sending impression directly to server");
            // Send directly to server
            boolean success = recordImpressionToServer(adId, deviceId);
            if (success) {
                Log.d(TAG, "IMPRESSION TRACKING: Successfully sent impression directly to server for ad: " + adId);
                // Don't return early - still add to batch queue as fallback in case API call fails
            }
        }
        
        // Add to pending batch (as fallback)
        TrackingEvent event = new TrackingEvent(adId, deviceId);
        synchronized (pendingImpressions) {
            pendingImpressions.add(event);
            Log.d(TAG, "IMPRESSION TRACKING: Added impression to batch queue as fallback. Queue size: " + pendingImpressions.size());
        }
        
        // If batch is full or we're online, process immediately
        if (pendingImpressions.size() >= MAX_BATCH_SIZE && connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "IMPRESSION TRACKING: Processing batch immediately (batch size: " + pendingImpressions.size() + ")");
            batchHandler.post(batchProcessingRunnable);
        } else {
            // Otherwise make sure a batch flush is scheduled
            Log.d(TAG, "IMPRESSION TRACKING: Scheduling batch processing for later");
            scheduleBatchProcessing();
        }
        
        // Diagnostic - dump impression queue
        synchronized (pendingImpressions) {
            Log.d(TAG, "IMPRESSION TRACKING: Current impression queue:");
            for (int i = 0; i < pendingImpressions.size(); i++) {
                TrackingEvent e = pendingImpressions.get(i);
                Log.d(TAG, "  Queue[" + i + "]: " + e.toString());
            }
        }
    }
    
    /**
     * Record an impression directly to the server
     * @return true if successful or scheduled for retry, false if failed permanently
     */
    private boolean recordImpressionToServer(String adId, String deviceId) {
        Log.d(TAG, "IMPRESSION TRACKING: Recording impression to server for ad: " + adId + " with deviceId: " + deviceId);
        
        final TrackingEvent event = new TrackingEvent(adId, deviceId);
        final boolean[] success = {false}; // Use array to allow modification in callback
        
        // Debug - show network connectivity status
        boolean isConnected = connectivityChecker.isNetworkAvailable();
        Log.d(TAG, "IMPRESSION TRACKING: Network connectivity status: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));
        
        // Get the API endpoint for impression tracking
        String endpoint = "sponsored-ads/viewed/" + adId;
        Log.d(TAG, "IMPRESSION TRACKING: Using API endpoint: " + endpoint);
        
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "IMPRESSION TRACKING: Server responded with code: " + response.code());
                
                // Check specifically for rate limiting response (429)
                if (response.code() == 429) {
                    handleRateLimiting(response);
                    Log.e(TAG, "IMPRESSION TRACKING: Rate limited by server when recording impression for ad: " + adId);
                    
                    // Add back to queue for future retry
                    synchronized (pendingImpressions) {
                        if (event.incrementRetry()) {
                            pendingImpressions.add(event);
                            Log.d(TAG, "IMPRESSION TRACKING: Re-queued impression for retry (attempt " + event.retryCount + 
                                  " of " + MAX_RETRY_ATTEMPTS + ")");
                        } else {
                            Log.e(TAG, "IMPRESSION TRACKING: Maximum retry attempts reached for impression: " + event);
                        }
                    }
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "IMPRESSION TRACKING: Successfully recorded impression for ad: " + adId + 
                          ", response: " + response.body().toString());
                    success[0] = true;
                    
                    // Log success metrics
                    try {
                        JsonObject body = response.body();
                        if (body.has("impressionCount")) {
                            int serverCount = body.get("impressionCount").getAsInt();
                            Log.d(TAG, "IMPRESSION TRACKING: Server now reports " + serverCount + 
                                  " impressions for ad: " + adId);
                        }
                        
                        if (body.has("message")) {
                            Log.d(TAG, "IMPRESSION TRACKING: Server message: " + body.get("message").getAsString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "IMPRESSION TRACKING: Error parsing success response", e);
                    }
                } else {
                    String errorMsg = "IMPRESSION TRACKING: Failed to record impression for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "IMPRESSION TRACKING: Error parsing impression error response", e);
                    }
                    Log.e(TAG, errorMsg);
                    
                    // Re-add to queue for server errors only
                    if (response.code() >= 500) {
                        synchronized (pendingImpressions) {
                            if (event.incrementRetry()) {
                                pendingImpressions.add(event);
                                Log.d(TAG, "IMPRESSION TRACKING: Re-queued impression for retry due to server error (attempt " + 
                                      event.retryCount + " of " + MAX_RETRY_ATTEMPTS + ")");
                                success[0] = true; // Consider as success since we're retrying
                            } else {
                                Log.e(TAG, "IMPRESSION TRACKING: Maximum retry attempts reached for impression: " + event);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "IMPRESSION TRACKING: Network error recording impression for ad: " + adId, t);
                Log.e(TAG, "IMPRESSION TRACKING: Impression request URL: " + call.request().url());
                Log.e(TAG, "IMPRESSION TRACKING: Error message: " + t.getMessage());
                
                // Add back to the queue for retry
                synchronized (pendingImpressions) {
                    if (event.incrementRetry()) {
                        pendingImpressions.add(event);
                        Log.d(TAG, "IMPRESSION TRACKING: Re-queued impression for retry due to network error (attempt " + 
                              event.retryCount + " of " + MAX_RETRY_ATTEMPTS + ")");
                        success[0] = true; // Consider as success since we're retrying
                    } else {
                        Log.e(TAG, "IMPRESSION TRACKING: Maximum retry attempts reached for impression: " + event);
                    }
                }
            }
        });
        
        return true; // Initial attempt is always considered successful
    }
    
    /**
     * Update the local database impression count
     */
    private void updateLocalImpressionCount(String adId) {
        if (adId == null) {
            Log.e(TAG, "IMPRESSION TRACKING: Cannot update local impression count - null adId");
            return;
        }
        
        try {
            Log.d(TAG, "IMPRESSION TRACKING: Updating local impression count for ad: " + adId);
            
            // Update in a background thread to avoid blocking
            executors.diskIO().execute(() -> {
                try {
                    // Get current entity from database
                    SponsoredAdEntity entity = sponsoredAdDao.getAdById(adId);
                    
                    if (entity != null) {
                        // Increment impression count
                        int currentCount = entity.getImpressionCount();
                        int newCount = currentCount + 1;
                        
                        // Update entity
                        entity.setImpressionCount(newCount);
                        entity.setLastImpressionTime(System.currentTimeMillis());
                        
                        // Save to database
                        sponsoredAdDao.update(entity);
                        
                        Log.d(TAG, "IMPRESSION TRACKING: Updated local database - Ad: " + adId + 
                              ", Old count: " + currentCount + ", New count: " + newCount);
                    } else {
                        Log.e(TAG, "IMPRESSION TRACKING: Ad not found in local database: " + adId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "IMPRESSION TRACKING: Error updating local impression count", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "IMPRESSION TRACKING: Error queueing local impression count update", e);
        }
    }
    
    /**
     * Record a click for a sponsored ad
     * @param adId The ID of the ad that was clicked
     */
    public void recordClick(String adId) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            Log.e(TAG, "Cannot record click - missing deviceId or adId");
            return;
        }
        
        // Check rate limiting for all API calls
        if (isRateLimited && System.currentTimeMillis() < rateLimitExpiresAt) {
            long remainingMinutes = (rateLimitExpiresAt - System.currentTimeMillis()) / 60000;
            Log.d(TAG, "Skipping click tracking - rate limited for " + remainingMinutes + " more minutes");
            
            // Still update local database even if rate limited
            updateLocalClickCount(adId);
            return;
        }
        
        // Always update local database first to ensure the click is recorded
        updateLocalClickCount(adId);
        
        // Add to pending batch
        TrackingEvent event = new TrackingEvent(adId, deviceId);
        synchronized (pendingClicks) {
            pendingClicks.add(event);
            Log.d(TAG, "Added click to batch queue. Queue size: " + pendingClicks.size());
        }
        
        // Clicks should be processed immediately if online
        if (connectivityChecker.isNetworkAvailable()) {
            batchHandler.post(batchProcessingRunnable);
        } else {
            // Otherwise make sure a batch flush is scheduled
            scheduleBatchProcessing();
        }
    }
    
    /**
     * Record a click directly to the server
     * @return true if successful or scheduled for retry, false if failed permanently
     */
    private boolean recordClickToServer(String adId, String deviceId) {
        Log.d(TAG, "Recording click for ad: " + adId + " with deviceId: " + deviceId);
        
        final TrackingEvent event = new TrackingEvent(adId, deviceId);
        final boolean[] success = {false}; // Use array to allow modification in callback
        
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Check for rate limiting status code (429)
                if (response.code() == 429) {
                    handleRateLimiting(response);
                    Log.e(TAG, "Rate limited by server when recording click for ad: " + adId);
                    
                    // Add back to queue for retry later
                    synchronized (pendingClicks) {
                        if (event.incrementRetry()) {
                            pendingClicks.add(event);
                            Log.d(TAG, "Re-queued click for retry (attempt " + event.retryCount + 
                                  " of " + MAX_RETRY_ATTEMPTS + ")");
                            success[0] = true; // Consider as success since we're retrying
                        } else {
                            Log.e(TAG, "Maximum retry attempts reached for click: " + event);
                        }
                    }
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully recorded click for ad: " + adId + 
                          ", response: " + response.body().toString());
                    
                    // Ensure click count is updated in database again
                    // This helps in case the earlier update had issues
                    updateLocalClickCount(adId);
                    success[0] = true;
                } else {
                    String errorMsg = "Failed to record click for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing click error response", e);
                    }
                    Log.e(TAG, errorMsg);
                    
                    // Re-add to queue for later retry
                    if (response.code() >= 500) {
                        synchronized (pendingClicks) {
                            if (event.incrementRetry()) {
                                pendingClicks.add(event);
                                Log.d(TAG, "Re-queued click for retry due to server error (attempt " + 
                                      event.retryCount + " of " + MAX_RETRY_ATTEMPTS + ")");
                                success[0] = true; // Consider as success since we're retrying
                            } else {
                                Log.e(TAG, "Maximum retry attempts reached for click: " + event);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error recording click for ad: " + adId, t);
                Log.e(TAG, "Click request URL: " + call.request().url());
                
                // Add back to the queue for retry
                synchronized (pendingClicks) {
                    if (event.incrementRetry()) {
                        pendingClicks.add(event);
                        Log.d(TAG, "Re-queued click for retry due to network error (attempt " + 
                              event.retryCount + " of " + MAX_RETRY_ATTEMPTS + ")");
                        success[0] = true; // Consider as success since we're retrying
                    } else {
                        Log.e(TAG, "Maximum retry attempts reached for click: " + event);
                    }
                }
            }
        });
        
        return true; // Initial attempt is always considered successful
    }
    
    /**
     * Update local click count in database
     */
    private void updateLocalClickCount(String adId) {
        executors.diskIO().execute(() -> {
            try {
                SponsoredAdEntity adEntity = sponsoredAdDao.getById(adId);
                if (adEntity != null) {
                    adEntity.setClickCount(adEntity.getClickCount() + 1);
                    sponsoredAdDao.update(adEntity);
                    Log.d(TAG, "Updated local click count for ad: " + adId + 
                          " to " + adEntity.getClickCount());
                } else {
                    Log.e(TAG, "Cannot update click - ad not found in cache: " + adId);
                    
                    // Try to fetch from network if the ad isn't in the cache
                    if (connectivityChecker.isNetworkAvailable()) {
                        Log.d(TAG, "Attempting to refresh ads to get missing ad: " + adId);
                        refreshFromNetwork();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating local click count for ad: " + adId, e);
            }
        });
    }
    
    /**
     * Get the loading state LiveData
     * @return LiveData of loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return loadingLiveData;
    }
    
    /**
     * Get the error LiveData
     * @return LiveData of error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    /**
     * Get sponsored ads for a specific location with caching
     * @param location The location identifier (e.g., "home_bottom")
     * @return LiveData containing filtered ads list
     */
    public LiveData<List<SponsoredAd>> getAdsByLocation(String location) {
        MediatorLiveData<List<SponsoredAd>> result = new MediatorLiveData<>();
        
        // If we have network, always refresh when requesting by location
        if (connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "Network available, refreshing ads for location: " + location);
            forceRefreshNow();
        }
        
        LiveData<List<SponsoredAd>> allAdsLiveData = getSponsoredAds();
        
        result.addSource(allAdsLiveData, ads -> {
            if (ads != null) {
                List<SponsoredAd> filteredAds = filterAdsByLocation(ads, location);
                Log.d(TAG, "Filtered " + ads.size() + " ads to " + filteredAds.size() + 
                      " for location: " + location);
                result.setValue(filteredAds);
                
                // If no ads found, try to refresh from network
                if (filteredAds.isEmpty() && connectivityChecker.isNetworkAvailable()) {
                    Log.d(TAG, "No ads found for location " + location + ", forcing refresh");
                    forceRefreshNow();
                }
            } else {
                result.setValue(null);
            }
        });
        
        return result;
    }
    
    /**
     * Filter ads by location with fallback strategies
     */
    private List<SponsoredAd> filterAdsByLocation(List<SponsoredAd> allAds, String location) {
        if (allAds == null || allAds.isEmpty() || location == null) {
            return new ArrayList<>();
        }
        
        Log.d(TAG, "Filtering " + allAds.size() + " ads for location '" + location + "'");
                List<SponsoredAd> filtered = new ArrayList<>();
                
                // Log all available locations for debugging
                StringBuilder availableLocations = new StringBuilder("Available ad locations: ");
        for (SponsoredAd ad : allAds) {
                    availableLocations.append("'").append(ad.getLocation()).append("'");
            if (allAds.indexOf(ad) < allAds.size() - 1) {
                        availableLocations.append(", ");
                    }
                }
                Log.d(TAG, availableLocations.toString());
                
                // Try different matching strategies if no exact match is found
                boolean foundExactMatch = false;
                
                // First pass: try exact match
        for (SponsoredAd ad : allAds) {
                    boolean exactMatch = location.equals(ad.getLocation()) && ad.isStatus();
                    if (exactMatch) {
                        foundExactMatch = true;
                        filtered.add(ad);
                        Log.d(TAG, "✓ Added ad to filtered list (exact match): " + ad.getId());
                    }
                }
                
                // If no exact match, try case-insensitive match
                if (!foundExactMatch) {
                    Log.d(TAG, "No exact location match found. Trying case-insensitive match...");
            for (SponsoredAd ad : allAds) {
                        boolean caseInsensitiveMatch = location.equalsIgnoreCase(ad.getLocation()) && ad.isStatus();
                        if (caseInsensitiveMatch) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (case-insensitive match): " + ad.getId());
                        }
                    }
                }
                
                // If still no match, check for partial match (e.g., "home" in "home_bottom")
                if (filtered.isEmpty()) {
                    Log.d(TAG, "No case-insensitive match found. Trying partial match...");
            for (SponsoredAd ad : allAds) {
                        String adLocation = ad.getLocation().toLowerCase();
                        String requestedLocation = location.toLowerCase();
                        
                        boolean partialMatch = (adLocation.contains(requestedLocation) || 
                                               requestedLocation.contains(adLocation)) && 
                                               ad.isStatus();
                        
                        if (partialMatch) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (partial match): " + ad.getId() + 
                                  ", ad location: '" + ad.getLocation() + "', requested: '" + location + "'");
                        }
                    }
                }
                
                // As a last resort, if no ads match any criteria, take the first active ad
        if (filtered.isEmpty() && !allAds.isEmpty()) {
                    Log.d(TAG, "No matching ads found. Taking first active ad as fallback...");
            for (SponsoredAd ad : allAds) {
                        if (ad.isStatus()) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (fallback): " + ad.getId() + 
                                  ", using location: '" + ad.getLocation() + "' instead of '" + location + "'");
                            break; // Just take the first one
                        }
                    }
                }
                
                Log.d(TAG, "Filtered " + filtered.size() + " ads for location: " + location);
        return filtered;
    }
    
    /**
     * Clear the cache
     */
    public void clearCache() {
        executors.diskIO().execute(() -> {
            sponsoredAdDao.deleteAll();
            Log.d(TAG, "Cleared sponsored ads cache");
        });
    }
    
    /**
     * Check if we have any valid ads in the cache
     * @return true if we have valid ads, false otherwise
     */
    public boolean hasCachedAds() {
        try {
            long currentTime = System.currentTimeMillis();
            return sponsoredAdDao.countValid(currentTime) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking cache status", e);
            return false;
        }
    }
    
    /**
     * Clean up resources when repository is no longer needed
     */
    public void cleanup() {
        // Process any pending events before cleanup
        processPendingEvents();
        
        // Remove callbacks
        batchHandler.removeCallbacks(batchProcessingRunnable);
        
        Log.d(TAG, "Repository cleaned up");
    }

    /**
     * Get the next ad for rotation, excluding previously shown ads
     */
    public LiveData<SponsoredAd> getNextRotationAd(String location, Set<String> excludeIds) {
        Log.d(TAG, "Getting next rotation ad for location: " + location + 
              ", excluding " + (excludeIds != null ? excludeIds.size() : 0) + " ads");
              
        MediatorLiveData<SponsoredAd> result = new MediatorLiveData<>();
        
        // First check cache
        executors.diskIO().execute(() -> {
            try {
                List<SponsoredAdEntity> entities;
                long currentTime = System.currentTimeMillis();
                
                if (excludeIds != null && !excludeIds.isEmpty()) {
                    List<String> excludeList = new ArrayList<>(excludeIds);
                    entities = sponsoredAdDao.getActiveAdsByLocationExcluding(
                        location, excludeList, currentTime);
                } else {
                    entities = sponsoredAdDao.getActiveAdsByLocation(location, currentTime);
                }
                
                if (entities != null && !entities.isEmpty()) {
                    Log.d(TAG, "Found " + entities.size() + " cached ads for rotation");
                    
                    // Apply weighted selection on main thread
                    executors.mainThread().execute(() -> {
                        SponsoredAdEntity selectedEntity = applyWeightedSelection(entities);
                        result.setValue(entityToModel(selectedEntity));
                        
                        // Refresh cache in background if needed
                        long cacheAge = System.currentTimeMillis() - selectedEntity.getLastFetchTime();
                        if (cacheAge > MIN_REFRESH_INTERVAL_MS) {
                            Log.d(TAG, "Cache is old (" + (cacheAge / 1000) + " seconds), refreshing from network");
                            refreshRotationAdsFromNetwork(location, excludeIds);
                        }
                    });
            } else {
                    Log.d(TAG, "No cached ads for rotation, fetching from network");
                    // Fetch from network
                    fetchRotationAdFromNetwork(location, excludeIds, result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting rotation ads from cache", e);
                // Fetch from network on error
                fetchRotationAdFromNetwork(location, excludeIds, result);
            }
        });
        
        return result;
    }

    /**
     * Apply weighted selection based on priority and previous impressions
     */
    private SponsoredAdEntity applyWeightedSelection(List<SponsoredAdEntity> entities) {
        if (entities.size() == 1) {
            return entities.get(0);
        }
        
        // Calculate total weight (priority * inverse of impression count)
        double totalWeight = 0;
        double[] weights = new double[entities.size()];
        
        for (int i = 0; i < entities.size(); i++) {
            SponsoredAdEntity entity = entities.get(i);
            
            // Higher priority and fewer impressions = higher weight
            // Formula: weight = priority * (1 / (1 + log(1 + impression_count)))
            double impressionFactor = 1.0 / (1 + Math.log(1 + entity.getImpressionCount()));
            weights[i] = entity.getPriority() * impressionFactor;
            totalWeight += weights[i];
        }
        
        Log.d(TAG, "Total weight: " + totalWeight + " for " + entities.size() + " ads");
        
        // Select based on weighted probability
        double random = Math.random() * totalWeight;
        double weightSum = 0;
        
        for (int i = 0; i < entities.size(); i++) {
            weightSum += weights[i];
            if (random <= weightSum) {
                Log.d(TAG, "Selected ad " + entities.get(i).getId() + " with weight " + 
                      weights[i] + "/" + totalWeight + " (random=" + random + ")");
                return entities.get(i);
            }
        }
        
        // Fallback to first ad (should rarely happen)
        Log.d(TAG, "Fallback to first ad due to weight calculation edge case");
        return entities.get(0);
    }

    /**
     * Convert SponsoredAdEntity to SponsoredAd model
     */
    private SponsoredAd entityToModel(SponsoredAdEntity entity) {
        if (entity == null) return null;
        return entity.toModel();
    }

    /**
     * Save ads to database for caching
     */
    private void saveAdsToDatabase(List<SponsoredAd> ads) {
        if (ads == null || ads.isEmpty()) {
            Log.d(TAG, "No ads to save to database");
            return;
        }

        Log.d(TAG, "Saving " + ads.size() + " ads to database");
        executors.diskIO().execute(() -> {
            try {
                // Convert models to entities
                List<SponsoredAdEntity> entities = new ArrayList<>();
                for (SponsoredAd ad : ads) {
                    entities.add(new SponsoredAdEntity(ad, CACHE_DURATION_MS));
                }
                
                // Insert into database
                sponsoredAdDao.insertAll(entities);
                Log.d(TAG, "Successfully saved " + entities.size() + " ads to database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving ads to database", e);
            }
        });
    }

    /**
     * Fetch rotation ad from network
     */
    private void fetchRotationAdFromNetwork(String location, Set<String> excludeIds, MediatorLiveData<SponsoredAd> result) {
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "No network available, cannot fetch rotation ads");
            result.postValue(null);
            return;
        }
        
        // Build query params
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("location", location);
        queryMap.put("limit", 5); // Get a few for local selection
        
        if (excludeIds != null && !excludeIds.isEmpty()) {
            queryMap.put("exclude", new ArrayList<>(excludeIds));
        }
        
        // Make API call to rotation endpoint
        apiService.getSponsoredAdsForRotation(queryMap).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SponsoredAd> ads = response.body().getAds();
                    
                    if (ads != null && !ads.isEmpty()) {
                        Log.d(TAG, "Fetched " + ads.size() + " rotation ads from network");
                        
                        // Save to database
                        saveAdsToDatabase(ads);
                        
                        // Select one ad using weighted selection
                        // (For simplicity, just do random selection here as server already applies weighting)
                        int randomIndex = (int)(Math.random() * ads.size());
                        result.postValue(ads.get(randomIndex));
                    } else {
                        Log.d(TAG, "No rotation ads returned from network");
                        result.postValue(null);
                    }
                } else {
                    Log.e(TAG, "Error fetching rotation ads: " + 
                          (response.code() + " - " + (response.errorBody() != null ? 
                          "Error body available" : "No error body")));
                    result.postValue(null);
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching rotation ads", t);
                result.postValue(null);
            }
        });
    }

    /**
     * Refresh rotation ads from network in background
     */
    private void refreshRotationAdsFromNetwork(String location, Set<String> excludeIds) {
        if (!connectivityChecker.isNetworkAvailable()) {
            return;
        }
        
        // Build query params
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("location", location);
        queryMap.put("limit", 10); // Get more for variety
        
        if (excludeIds != null && !excludeIds.isEmpty()) {
            queryMap.put("exclude", new ArrayList<>(excludeIds));
        }
        
        apiService.getSponsoredAdsForRotation(queryMap).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SponsoredAd> ads = response.body().getAds();
                    
                    if (ads != null && !ads.isEmpty()) {
                        Log.d(TAG, "Background refresh fetched " + ads.size() + " rotation ads");
                        saveAdsToDatabase(ads);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                Log.e(TAG, "Background refresh failed", t);
            }
        });
    }

    /**
     * Get the next rotation ad synchronously for preloading
     * @param location The location identifier
     * @param excludeIds Set of ad IDs to exclude
     * @return The next ad or null if none available
     */
    public SponsoredAd getNextRotationAdSync(String location, Set<String> excludeIds) {
        Log.d(TAG, "Getting next rotation ad synchronously for location: " + location +
              ", excluding: " + excludeIds.size() + " ads");
        
        try {
            // Query the database directly on the calling thread
            List<SponsoredAdEntity> entities = sponsoredAdDao.getAdsByLocationSync(location);
            
            if (entities == null || entities.isEmpty()) {
                Log.d(TAG, "No ads available for rotation at location: " + location);
                return null;
            }
            
            // Filter out excluded IDs
            if (excludeIds != null && !excludeIds.isEmpty()) {
                Iterator<SponsoredAdEntity> iterator = entities.iterator();
                while (iterator.hasNext()) {
                    SponsoredAdEntity entity = iterator.next();
                    if (excludeIds.contains(entity.getId())) {
                        iterator.remove();
                    }
                }
            }
            
            if (entities.isEmpty()) {
                Log.d(TAG, "No ads available after exclusion filter");
                return null;
            }
            
            // Apply weighted selection
            SponsoredAdEntity selected = applyWeightedSelection(entities);
            
            if (selected != null) {
                Log.d(TAG, "Selected next rotation ad: " + selected.getId() + 
                      ", priority: " + selected.getPriority());
                return entityToModel(selected);
            } else {
                Log.d(TAG, "No suitable ad found for next rotation");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting next rotation ad: " + e.getMessage());
            return null;
        }
    }

    /**
     * Force a refresh from the network next time getSponsoredAds is called
     */
    public void forceRefreshNextTime() {
        forceRefresh = true;
        Log.d(TAG, "Forced refresh scheduled for next getSponsoredAds call");
    }

    /**
     * Refresh ads forcefully right now
     */
    public void forceRefreshNow() {
        Log.d(TAG, "Forcing immediate refresh from network");
        forceRefresh = true;
        refreshFromNetwork();
    }
} 