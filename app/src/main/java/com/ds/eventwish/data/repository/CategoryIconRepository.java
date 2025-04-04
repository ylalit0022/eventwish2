package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.ErrorHandler;
import com.ds.eventwish.utils.NetworkErrorHandler;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing category icons with improved caching, offline support, and error handling
 */
public class CategoryIconRepository {
    private static final String TAG = "CategoryIconRepository";
    
    // Cache keys
    private static final String CACHE_KEY_CATEGORY_ICONS = "category_icons";
    
    // Cache expiration times - increase from 7 days to 30 days
    private static final long CACHE_EXPIRATION_ICONS = TimeUnit.DAYS.toMillis(30); // 30 days
    
    // Memory cache expiration - new field
    private static final long MEMORY_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(24); // 24 hours
    
    // Singleton instance
    private static volatile CategoryIconRepository instance;
    
    // Dependencies
    private final Context context;
    private final ResourceRepository resourceRepository;
    private final ApiService apiService;
    private final AppExecutors executors;
    private final NetworkUtils networkUtils;
    private final Gson gson;
    private final ErrorHandler errorHandler;
    
    // LiveData
    private final MutableLiveData<List<CategoryIcon>> categoryIcons = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    // State
    private final Map<String, CategoryIcon> categoryIconMap = new HashMap<>();
    private boolean isInitialized = false;
    private Call<CategoryIconResponse> currentCall;
    private long lastMemoryCacheRefresh = 0; // Track when memory cache was last refreshed

    // Network retry constants
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second
    private static final float BACKOFF_MULTIPLIER = 1.5f;
    private int currentRetryCount = 0;
    private long currentBackoffMs = INITIAL_BACKOFF_MS;

    /**
     * Get the singleton instance of CategoryIconRepository
     * @return CategoryIconRepository instance
     */
    public static synchronized CategoryIconRepository getInstance() {
        if (instance == null) {
            Context appContext = com.ds.eventwish.EventWishApplication.getAppContext();
            if (appContext == null) {
                throw new IllegalStateException("Cannot initialize CategoryIconRepository: Application context is null");
            }
            instance = new CategoryIconRepository(appContext);
        }
        return instance;
    }

    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private CategoryIconRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resourceRepository = ResourceRepository.getInstance(context);
        this.apiService = ApiClient.getClient();
        this.executors = AppExecutors.getInstance();
        this.networkUtils = NetworkUtils.getInstance(context);
        this.gson = new Gson();
        this.errorHandler = ErrorHandler.getInstance(context);
        
        // Initialize category icons list
        categoryIcons.setValue(new ArrayList<>());
        
        Log.d(TAG, "CategoryIconRepository initialized with enhanced caching and offline support");
    }

    /**
     * Get category icons as LiveData
     * @return LiveData with list of category icons
     */
    public LiveData<List<CategoryIcon>> getCategoryIcons() {
        if (!isInitialized) {
            loadCategoryIcons();
        }
        return categoryIcons;
    }

    /**
     * Get error as LiveData
     * @return LiveData with error message
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * Get loading state as LiveData
     * @return LiveData with loading state
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * Get a category icon by category name
     * @param category The category name
     * @return The CategoryIcon object or null if not found
     */
    public CategoryIcon getCategoryIconByCategory(String category) {
        if (category == null) {
            return null;
        }
        
        String lowerCategory = category.toLowerCase();
        
        // Check if we have the icon in our map
        if (categoryIconMap.containsKey(lowerCategory)) {
            CategoryIcon icon = categoryIconMap.get(lowerCategory);
            if (icon != null) {
                Log.d(TAG, "Found icon for category: " + category);
                return icon;
            }
        }
        
        // If not found and we haven't loaded icons yet, load them
        if (!isInitialized || categoryIconMap.isEmpty()) {
            Log.d(TAG, "Icons not initialized yet, loading category icons");
            loadCategoryIcons();
            
            // Check again after loading
            if (categoryIconMap.containsKey(lowerCategory)) {
                CategoryIcon icon = categoryIconMap.get(lowerCategory);
                if (icon != null) {
                    Log.d(TAG, "Found icon for category after loading: " + category);
                    return icon;
                }
            }
        }
        
        // If still not found, add a generic fallback for this specific category
        Log.d(TAG, "No icon found for category: " + category + ", adding generic fallback");
        String fallbackUrl = "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/category/materialicons/24dp/2x/category_black_24dp.png";
        CategoryIcon fallbackIcon = new CategoryIcon(lowerCategory, category, fallbackUrl);
        categoryIconMap.put(lowerCategory, fallbackIcon);
        
        return fallbackIcon;
    }

    /**
     * Cancel current API call
     */
    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    /**
     * Set current API call
     * @param call Call to set as current
     */
    private void setCurrentCall(Call<CategoryIconResponse> call) {
        cancelCurrentCall();
        currentCall = call;
    }

    /**
     * Load category icons with offline-first approach
     */
    public void loadCategoryIcons() {
        // First check the cache, then load from network if needed
        if (loading.getValue() != null && loading.getValue()) {
            Log.d(TAG, "Icons are already loading, skipping request");
            return;
        }
        
        loading.setValue(true);
        
        // Check if we need to refresh memory cache
        long currentTime = System.currentTimeMillis();
        boolean shouldRefreshMemoryCache = (currentTime - lastMemoryCacheRefresh) > MEMORY_CACHE_EXPIRATION;
        
        if (isInitialized && !shouldRefreshMemoryCache && !categoryIconMap.isEmpty()) {
            // If we already have icons in memory and don't need to refresh, use them directly
            Log.d(TAG, "Using category icons from memory cache, map size: " + categoryIconMap.size());
            List<CategoryIcon> iconList = new ArrayList<>(categoryIconMap.values());
            categoryIcons.setValue(iconList);
            loading.setValue(false);
            return;
        }
        
        // First try to load from cache
        loadFromCache();
    }

    /**
     * Load category icons from cache
     */
    private void loadFromCache() {
        executors.diskIO().execute(() -> {
            try {
                // Use proper method to get resource from cache - use LiveData version but extract value synchronously
                LiveData<Resource<JsonObject>> resourceLiveData = resourceRepository.getResource(
                    ResourceType.CATEGORY_ICON.getKey(),
                    CACHE_KEY_CATEGORY_ICONS,
                    false  // Don't force refresh
                );
                
                // Check in-memory cache first
                JsonObject cachedData = resourceRepository.getResourceSync(
                    ResourceType.CATEGORY_ICON.getKey(),
                    CACHE_KEY_CATEGORY_ICONS
                );

                if (cachedData != null && cachedData.has("data") && cachedData.get("data").isJsonArray()) {
                    try {
                        Type listType = new TypeToken<List<CategoryIcon>>(){}.getType();
                        List<CategoryIcon> icons = gson.fromJson(cachedData.getAsJsonArray("data"), listType);
                        
                        executors.mainThread().execute(() -> {
                            processCategoryIcons(icons);
                            
                            // Check if cache is old and needs background refresh
                            long cachedTimestamp = 0;
                            if (cachedData.has("timestamp") && !cachedData.get("timestamp").isJsonNull()) {
                                cachedTimestamp = cachedData.get("timestamp").getAsLong();
                            }
                            
                            long currentTime = System.currentTimeMillis();
                            boolean shouldRefreshCache = (currentTime - cachedTimestamp) > (CACHE_EXPIRATION_ICONS / 2);
                            
                            if (icons.isEmpty() || shouldRefreshCache) {
                                // If cached data is empty or old, load from network in background
                                Log.d(TAG, "Cache is empty or old, loading from network in background");
                                loadFromNetwork(0, INITIAL_BACKOFF_MS);
                            } else {
                                loading.setValue(false);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing cached category icons JSON", e);
                        executors.mainThread().execute(() -> {
                            // Cache parse error - load from network
                            loadFromNetwork(0, INITIAL_BACKOFF_MS);
                        });
                    }
                } else {
                    Log.d(TAG, "No valid cached data found for category icons");
                    executors.mainThread().execute(() -> {
                        // No cache - load from network
                        loadFromNetwork(0, INITIAL_BACKOFF_MS);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading category icons from cache", e);
                executors.mainThread().execute(() -> {
                    // Cache error - load from network
                    loadFromNetwork(0, INITIAL_BACKOFF_MS);
                });
            }
        });
    }

    /**
     * Load category icons from network
     * @param retryCount Current retry count
     * @param backoffMs Current backoff time in milliseconds
     */
    private void loadFromNetwork(int retryCount, long backoffMs) {
        // If we don't have network, add fallback icons and return
        if (!networkUtils.isConnected()) {
            Log.d(TAG, "No network connection, using fallback icons");
            addFallbackIcons();
            loading.setValue(false);
            return;
        }
        
        // Direct API call for faster response
        Log.d(TAG, "Loading category icons from network" + (retryCount > 0 ? " (retry " + retryCount + ")" : ""));
        cancelCurrentCall();
        Call<CategoryIconResponse> call = apiService.getCategoryIcons();
        setCurrentCall(call);
        
        call.enqueue(new Callback<CategoryIconResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryIconResponse> call, @NonNull Response<CategoryIconResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        CategoryIconResponse data = response.body();
                        
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            // Process and cache the response
                            processCategoryIcons(data.getData());
                            
                            // Cache the response for offline use
                            try {
                                JsonObject jsonObject = new JsonObject();
                                jsonObject.add("data", gson.toJsonTree(data.getData()));
                                
                                // Use proper method to cache resource
                                resourceRepository.saveResource(
                                    ResourceType.CATEGORY_ICON.getKey(),
                                    CACHE_KEY_CATEGORY_ICONS,
                                    jsonObject,
                                    null,  // No metadata
                                    null,  // No etag
                                    new Date(System.currentTimeMillis() + CACHE_EXPIRATION_ICONS)
                                );
                                
                                Log.d(TAG, "Category icons cached successfully");
                            } catch (Exception e) {
                                Log.e(TAG, "Error caching category icons", e);
                            }
                            
                            loading.setValue(false);
                        } else {
                            // No data received
                            Log.e(TAG, "Received empty category icons data");
                            handleNetworkFailure(retryCount, backoffMs, new Exception("Empty data received"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing category icons response", e);
                        handleNetworkFailure(retryCount, backoffMs, e);
                    }
                } else {
                    // API error
                    Log.e(TAG, "Error loading category icons: " + response.code());
                    handleNetworkFailure(retryCount, backoffMs, 
                        new Exception("API error: " + response.code()));
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<CategoryIconResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Category icons request was canceled");
                    loading.setValue(false);
                } else {
                    Log.e(TAG, "Failed to load category icons", t);
                    handleNetworkFailure(retryCount, backoffMs, t);
                }
            }
        });
    }

    /**
     * Handle network failure with retry mechanism
     * @param retryCount Current retry count
     * @param backoffMs Current backoff time in milliseconds
     * @param error Error that occurred
     */
    private void handleNetworkFailure(int retryCount, long backoffMs, Throwable error) {
        if (retryCount < MAX_RETRIES) {
            // Calculate next backoff duration with exponential increase
            final long nextBackoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            final int nextRetryCount = retryCount + 1;
            
            Log.d(TAG, "Scheduling retry " + nextRetryCount + " in " + backoffMs + "ms");
            
            // Use executor to schedule retry after backoff delay
            executors.mainThread().execute(() -> {
                // Schedule a retry with exponential backoff
                new android.os.Handler().postDelayed(() -> {
                    loadFromNetwork(nextRetryCount, nextBackoffMs);
                }, backoffMs);
            });
        } else {
            // Max retries reached, load from cache
            Log.e(TAG, "Max retries reached, using fallback icons", error);
            addFallbackIcons();
            loading.setValue(false);
        }
    }

    /**
     * Process retrieved category icons
     * @param icons List of category icons
     */
    private void processCategoryIcons(List<CategoryIcon> icons) {
        if (icons == null) {
            icons = new ArrayList<>();
        }
        
        Log.d(TAG, "Processing " + icons.size() + " category icons");
        
        // Clear existing map and rebuild it with new icons
        categoryIconMap.clear();
        
        // Add all icons to the map
        for (CategoryIcon icon : icons) {
            if (icon.getCategory() != null) {
                String key = icon.getCategory().toLowerCase();
                Log.d(TAG, "Adding icon for category: " + key);
                categoryIconMap.put(key, icon);
            }
        }
        
        // Add some common fallback icons for categories that might be missing
        // Only add if we don't already have these categories
        addFallbackIcons();
        
        // Mark as initialized
        isInitialized = true;
        
        // Update last refresh time
        lastMemoryCacheRefresh = System.currentTimeMillis();
        
        // Update LiveData value
        List<CategoryIcon> updatedList = new ArrayList<>(categoryIconMap.values());
        categoryIcons.setValue(updatedList);
        
        Log.d(TAG, "Category icons processed, total: " + categoryIconMap.size());
    }
    
    /**
     * Add fallback icons for common categories
     */
    private void addFallbackIcons() {
        // Add fallback icons for common categories if they don't exist in our map
        addFallbackIcon("all", "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_module/materialicons/24dp/2x/baseline_view_module_black_24dp.png");
        addFallbackIcon("birthday", "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/cake/materialicons/24dp/2x/baseline_cake_black_24dp.png");
        addFallbackIcon("wedding", "https://raw.githubusercontent.com/google/material-design-icons/master/png/places/church/materialicons/24dp/2x/baseline_church_black_24dp.png");
        addFallbackIcon("anniversary", "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/favorite/materialicons/24dp/2x/baseline_favorite_black_24dp.png");
        addFallbackIcon("graduation", "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/school/materialicons/24dp/2x/baseline_school_black_24dp.png");
        addFallbackIcon("holiday", "https://raw.githubusercontent.com/google/material-design-icons/master/png/places/beach_access/materialicons/24dp/2x/baseline_beach_access_black_24dp.png");
        addFallbackIcon("congratulations", "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/emoji_events/materialicons/24dp/2x/baseline_emoji_events_black_24dp.png");
        addFallbackIcon("cultural", "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/emoji_events/materialicons/24dp/2x/baseline_emoji_events_black_24dp.png");
        
        // Log the total number of icons after adding fallbacks
        Log.d(TAG, "Total category icons after adding fallbacks: " + categoryIconMap.size());
        
        // Log all available categories for debugging
        for (String category : categoryIconMap.keySet()) {
            Log.d(TAG, "Available category icon: " + category);
        }
    }
    
    /**
     * Add a fallback icon if it doesn't exist in our map
     */
    private void addFallbackIcon(String category, String iconUrl) {
        if (category == null || category.isEmpty()) {
            Log.w(TAG, "Attempted to add fallback icon with null or empty category");
            return;
        }
        
        String lowerCategory = category.toLowerCase();
        if (!categoryIconMap.containsKey(lowerCategory)) {
            CategoryIcon icon = new CategoryIcon(lowerCategory, category, iconUrl);
            categoryIconMap.put(lowerCategory, icon);
            Log.d(TAG, "Added fallback icon for category: " + category);
        }
    }
    
    /**
     * Refresh category icons from the API
     */
    public void refreshCategoryIcons() {
        Log.d(TAG, "Refreshing category icons");
        
        // Clear the initialized flag to force a reload
        isInitialized = false;
        
        // Clear existing data
        categoryIconMap.clear();
        
        // Clear cache using ResourceRepository
        resourceRepository.clearCache(ResourceType.CATEGORY_ICON, CACHE_KEY_CATEGORY_ICONS);
        
        // Load fresh data from API
        loadCategoryIcons();
        
        // Notify observers that we're refreshing
        loading.setValue(true);
    }
}