package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.ErrorHandler;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing category icons with improved caching, offline support, and error handling
 */
public class CategoryIconRepository {
    private static final String TAG = "CategoryIconRepository";
    
    // Add BASE_URL field
    private static final String BASE_URL = "https://eventwish2.onrender.com/api/";
    
    // Cache keys
    private static final String CACHE_KEY_CATEGORY_ICONS = "category_icons";
    
    // Cache expiration times - increase from 7 days to 30 days
    private static final long CACHE_EXPIRATION_ICONS = TimeUnit.DAYS.toMillis(30); // 30 days
    
    // Memory cache expiration - new field
    private static final long MEMORY_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(24); // 24 hours
    
    // In-memory URL cache to avoid repeated lookups
    private static final int URL_CACHE_SIZE = 100;
    private final LruCache<String, String> urlCache = new LruCache<>(URL_CACHE_SIZE);
    
    // Set of normalized categories to improve search performance
    private final Set<String> normalizedCategories = new HashSet<>();
    
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
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private volatile Call<CategoryIconResponse> currentCall;
    private volatile long lastMemoryCacheRefresh = 0; // Track when memory cache was last refreshed

    // Network retry constants
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 1.5;
    private int currentRetryCount = 0;
    private long currentBackoffMs = INITIAL_BACKOFF_MS;

    /**
     * Get the singleton instance with Context
     * @param context Application context
     * @return The singleton instance
     */
    public static synchronized CategoryIconRepository getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "❌ Cannot initialize CategoryIconRepository with null context");
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        if (instance == null) {
            instance = new CategoryIconRepository(context.getApplicationContext());
            Log.d(TAG, "✅ Created new CategoryIconRepository instance with context");
        } else {
            // Update the context if needed
            if (instance.context == null && context != null) {
                Log.d(TAG, "🔄 Updating existing CategoryIconRepository instance with new context");
                instance = new CategoryIconRepository(context.getApplicationContext());
            }
        }
        return instance;
    }
    
    /**
     * Legacy getInstance method - always use getInstance(Context) instead
     * @return The singleton instance or null if not initialized
     * @deprecated Use getInstance(Context) to ensure proper initialization
     */
    @Deprecated
    public static synchronized CategoryIconRepository getInstance() {
        // Try to get application context as fallback
        Context appContext = null;
        try {
            appContext = com.ds.eventwish.EventWishApplication.getAppContext();
            if (appContext != null) {
                Log.d(TAG, "⚠️ Using application context from EventWishApplication");
                return getInstance(appContext);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Could not get application context", e);
        }
        
        if (instance != null) {
            Log.w(TAG, "⚠️ Using existing instance without refreshing context");
            return instance;
        }
        
        Log.e(TAG, "❌ No context available for CategoryIconRepository initialization");
        throw new IllegalStateException("Failed to initialize CategoryIconRepository - context required");
    }
    
    /**
     * Private constructor with improved error handling
     * @param context Application context (can be null in fallback scenario)
     */
    private CategoryIconRepository(Context context) {
        // Store application context if available
        this.context = context != null ? context.getApplicationContext() : null;
        
        // Initialize dependencies with null checks
        if (context != null) {
            // Normal initialization with context
            this.resourceRepository = ResourceRepository.getInstance(context);
            this.apiService = ApiClient.getClient();
            this.executors = AppExecutors.getInstance();
            this.networkUtils = NetworkUtils.getInstance(context);
            this.gson = new GsonBuilder()
                    .serializeNulls()
                    .create();
            this.errorHandler = ErrorHandler.getInstance(context);
            
            Log.d(TAG, "🏗️ CategoryIconRepository fully initialized with context");
        } else {
            // Fallback initialization - minimal functionality
            this.resourceRepository = null;
            this.apiService = null;
            this.executors = AppExecutors.getInstance();
            this.networkUtils = null;
            this.gson = new Gson();
            this.errorHandler = null;
            
            Log.w(TAG, "⚠️ CategoryIconRepository initialized with minimal functionality (no context)");
        }
        
        // Initialize category icons list
        categoryIcons.setValue(new ArrayList<>());
        
        // Add fallback icons immediately
        addFallbackIcons();
    }

    /**
     * Get category icons as LiveData
     * @return LiveData with list of category icons
     */
    public LiveData<List<CategoryIcon>> getCategoryIcons() {
        if (!isInitialized.get()) {
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
     * Get a category icon by category name with improved search algorithms
     * @param category The category name
     * @return The CategoryIcon object or null if not found
     */
    @Nullable
    public CategoryIcon getCategoryIconByCategory(String category) {
        if (category == null) {
            Log.w(TAG, "⚠️ getCategoryIconByCategory called with null category");
            return null;
        }
        
        // Normalize the category name for consistent lookup
        String normalizedCategory = normalizeCategory(category);
        
        // Quick return from exact match
        CategoryIcon icon = categoryIconMap.get(normalizedCategory);
        if (icon != null) {
            Log.d(TAG, "✅ Found exact match icon for category: '" + normalizedCategory + "'");
            return icon;
        }
        
        // Try to initialize if needed
        if (!isInitialized.get() || categoryIconMap.isEmpty()) {
            Log.d(TAG, "🔄 Icons not initialized yet, loading category icons");
            
            // Synchronous loading for real-time requests
            synchronized (this) {
                loadCategoryIconsSync();
                
                // Check again after loading
                icon = categoryIconMap.get(normalizedCategory);
                if (icon != null) {
                    Log.d(TAG, "✅ Found icon after sync loading for category: '" + normalizedCategory + "'");
                    return icon;
                }
            }
        }
        
        // Try fuzzy matching if exact match failed
        icon = findBestMatchingIcon(normalizedCategory);
        if (icon != null) {
            // Cache this match for future lookups
            categoryIconMap.put(normalizedCategory, icon);
            Log.d(TAG, "🔍 Found fuzzy match for category: '" + normalizedCategory + 
                  "' → '" + icon.getCategory() + "'");
            return icon;
        }
        
        // If still not found, add a generic fallback for this specific category
        Log.d(TAG, "⚠️ No icon found for category: '" + normalizedCategory + "', adding generic fallback");
        String fallbackUrl = generateFallbackUrl(normalizedCategory);
        CategoryIcon fallbackIcon = new CategoryIcon(normalizedCategory, category, fallbackUrl);
        categoryIconMap.put(normalizedCategory, fallbackIcon);
        
        return fallbackIcon;
    }

    /**
     * Load category icons synchronously for immediate needs
     */
    private synchronized void loadCategoryIconsSync() {
        // Check if we already have icons in memory
        if (isInitialized.get() && !categoryIconMap.isEmpty()) {
            Log.d(TAG, "✅ Icons already loaded in memory");
            return;
        }
        
        // Try to load from cache
        Resource<String> cachedResource = resourceRepository.getResource(CACHE_KEY_CATEGORY_ICONS);
        if (cachedResource != null && cachedResource.getData() != null) {
            try {
                JsonObject cachedData = gson.fromJson(cachedResource.getData(), JsonObject.class);
                Type listType = new TypeToken<List<CategoryIcon>>(){}.getType();
                List<CategoryIcon> icons = gson.fromJson(cachedData.getAsJsonArray("data"), listType);
                processCategoryIcons(icons);
                Log.d(TAG, "✅ Loaded " + icons.size() + " icons from cache synchronously");
                return;
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading from cache synchronously: " + e.getMessage(), e);
                // Fall through to fallbacks
            }
        }
        
        // Add default fallbacks if nothing else worked
        addFallbackIcons();
        isInitialized.set(true);
        
        // Trigger async loading for future requests
        executors.networkIO().execute(() -> loadCategoryIcons());
    }

    /**
     * Cancel current API call
     */
    private void cancelCurrentCall() {
        Call<CategoryIconResponse> call = currentCall;
        if (call != null && !call.isCanceled()) {
            call.cancel();
            Log.d(TAG, "🛑 Cancelled ongoing category icon API call");
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
     * Load category icons from repository - public method for manual refresh
     */
    public void loadCategoryIcons() {
        // Check for minimal initialization
        if (context == null || resourceRepository == null || apiService == null) {
            Log.w(TAG, "⚠️ Cannot load category icons - missing context or dependencies");
            addFallbackIcons();
            isInitialized.set(true);
            return;
        }
        
        // Prevent concurrent API requests
        if (Boolean.TRUE.equals(loading.getValue())) {
            Log.d(TAG, "Already loading category icons");
            return;
        }
        
        // Set loading state
        loading.setValue(true);
        
        // Execute in background thread
        executors.diskIO().execute(() -> {
            try {
                Log.d(TAG, "🔄 Loading category icons");
                loadFromCache();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading category icons", e);
                error.postValue("Error loading category icons: " + e.getMessage());
                loading.postValue(false);
                
                // Always ensure we have at least fallback icons
                addFallbackIcons();
                isInitialized.set(true);
            }
        });
    }

    /**
     * Try to find the best matching icon for a category with fuzzy matching
     */
    @Nullable
    private CategoryIcon findBestMatchingIcon(String category) {
        if (category == null || category.isEmpty() || categoryIconMap.isEmpty()) {
            return null;
        }
        
        // Special case for "All" category - it's important to handle this consistently
        if ("all".equalsIgnoreCase(category) || "all".equalsIgnoreCase(category.trim())) {
            CategoryIcon allIcon = categoryIconMap.get("all");
            if (allIcon != null) {
                Log.d(TAG, "✅ Found exact match for 'All' category");
                return allIcon;
            }
        }
        
        CategoryIcon bestMatch = null;
        int bestScore = 0;
        
        // First try direct substring matching (most reliable)
        for (Map.Entry<String, CategoryIcon> entry : categoryIconMap.entrySet()) {
            String key = entry.getKey();
            
            // Skip empty keys
            if (key == null || key.isEmpty()) continue;
            
            // Direct substring matches are highest priority
            if (key.contains(category) || category.contains(key)) {
                int matchLength = Math.min(key.length(), category.length());
                int currentScore = 100 + matchLength; // Prioritize substring matches
                
                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestMatch = entry.getValue();
                }
            }
        }
        
        // If we got a good substring match, return it
        if (bestScore > 100) {
            return bestMatch;
        }
        
        // Try word-by-word matching for multi-word categories
        String[] categoryWords = category.split("\\s+");
        if (categoryWords.length > 1) {
            for (Map.Entry<String, CategoryIcon> entry : categoryIconMap.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) continue;
                
                String[] keyWords = key.split("\\s+");
                int matchedWords = 0;
                
                for (String categoryWord : categoryWords) {
                    if (categoryWord.length() < 3) continue; // Skip short words
                    
                    for (String keyWord : keyWords) {
                        if (keyWord.length() < 3) continue; // Skip short words
                        
                        if (keyWord.contains(categoryWord) || categoryWord.contains(keyWord)) {
                            matchedWords++;
                            break;
                        }
                    }
                }
                
                if (matchedWords > 0) {
                    int currentScore = matchedWords * 50; // 50 points per matched word
                    if (currentScore > bestScore) {
                        bestScore = currentScore;
                        bestMatch = entry.getValue();
                    }
                }
            }
        }
        
        return bestMatch;
    }

    /**
     * Normalize category name for consistent comparisons
     */
    private String normalizeCategory(String category) {
        if (category == null) return "";
        return category.toLowerCase(Locale.US).trim();
    }
    
    /**
     * Generate a fallback URL for a category
     */
    private String generateFallbackUrl(String category) {
        // Generic Material Design category icon as default fallback
        return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/category/materialicons/24dp/2x/category_black_24dp.png";
    }

    /**
     * Process category icons and update caches
     */
    private void processCategoryIcons(List<CategoryIcon> icons) {
        if (icons == null) {
            icons = new ArrayList<>();
        }
        
        Log.d(TAG, "🔄 Processing " + icons.size() + " category icons");
        
        // Clear existing map and rebuild it with new icons
        categoryIconMap.clear();
        normalizedCategories.clear();
        urlCache.evictAll();
        
        // Add all icons to the map
        for (CategoryIcon icon : icons) {
            if (icon.getCategory() != null) {
                String key = normalizeCategory(icon.getCategory());
                String url = icon.getCategoryIcon();
                
                if (url != null && !url.isEmpty()) {
                    Log.d(TAG, "📝 Adding icon for category: '" + key + "' with URL: " + url);
                    categoryIconMap.put(key, icon);
                    normalizedCategories.add(key);
                    urlCache.put(key, url);
                } else {
                    Log.w(TAG, "⚠️ Skipping icon with empty URL for category: '" + key + "'");
                }
            } else {
                Log.w(TAG, "⚠️ Skipping icon with null category");
            }
        }
        
        // Add some common fallback icons for categories that might be missing
        // Only add if we don't already have these categories
        addFallbackIcons();
        
        // Mark as initialized
        isInitialized.set(true);
        
        // Update last refresh time
        lastMemoryCacheRefresh = System.currentTimeMillis();
        
        // Update LiveData value
        List<CategoryIcon> updatedList = new ArrayList<>(categoryIconMap.values());
        categoryIcons.postValue(updatedList);
        
        Log.d(TAG, "✅ Category icons processed, total: " + categoryIconMap.size());
    }
    
    /**
     * Get just the icon URL for a category (light operation for RecyclerView)
     * @param category Category name
     * @return URL string or null if not found
     */
    public String getCategoryIconUrl(String category) {
        if (category == null || category.isEmpty()) {
            Log.w(TAG, "⚠️ getCategoryIconUrl called with null/empty category");
            return null;
        }
        
        // Normalize category name (lowercase, trimmed)
        String normalizedCategory = normalizeCategory(category);
        
        // Check URL cache first (fastest)
        String cachedUrl = urlCache.get(normalizedCategory);
        if (cachedUrl != null) {
            Log.d(TAG, "🚀 URL cache hit for category: '" + normalizedCategory + "'");
            return cachedUrl;
        }
        
        // Check memory cache next
        CategoryIcon icon = categoryIconMap.get(normalizedCategory);
        if (icon != null && icon.getCategoryIcon() != null && !icon.getCategoryIcon().isEmpty()) {
            String url = icon.getCategoryIcon();
            // Cache the URL for future quick lookups
            urlCache.put(normalizedCategory, url);
            Log.d(TAG, "✅ Found exact match icon URL for category: '" + normalizedCategory + "'");
            return url;
        }
        
        // If not found with exact match, try fuzzy matching
        CategoryIcon matchedIcon = findBestMatchingIcon(normalizedCategory);
        if (matchedIcon != null && matchedIcon.getCategoryIcon() != null) {
            String url = matchedIcon.getCategoryIcon();
            // Cache the URL and icon for future lookups
            urlCache.put(normalizedCategory, url);
            categoryIconMap.put(normalizedCategory, matchedIcon);
            Log.d(TAG, "🔍 Found fuzzy match icon URL for category: '" + normalizedCategory + "'");
            return url;
        }
        
        // If still not found, initialize if needed and check again
        if (!isInitialized.get() || categoryIconMap.isEmpty()) {
            Log.d(TAG, "🔄 Repository not initialized yet, trying sync load for: '" + normalizedCategory + "'");
            
            // Try to initialize synchronously for this request
            loadCategoryIconsSync();
            
            // Check if we have it now after initialization
            icon = categoryIconMap.get(normalizedCategory);
            if (icon != null && icon.getCategoryIcon() != null) {
                String url = icon.getCategoryIcon();
                urlCache.put(normalizedCategory, url);
                Log.d(TAG, "✅ Found icon after initialization for: '" + normalizedCategory + "'");
                return url;
            }
        }
        
        // If all else fails, provide a generic fallback URL
        String fallbackUrl = generateFallbackUrl(normalizedCategory);
        Log.d(TAG, "⚠️ Using fallback icon URL for category: '" + normalizedCategory + "'");
        
        // Cache the fallback for future lookups
        urlCache.put(normalizedCategory, fallbackUrl);
        CategoryIcon fallbackIcon = new CategoryIcon(normalizedCategory, category, fallbackUrl);
        categoryIconMap.put(normalizedCategory, fallbackIcon);
        
        return fallbackUrl;
    }
    
    /**
     * Refresh category icons to ensure they're loaded
     * This method can be called from UI when icons aren't showing properly
     */
    public void refreshCategoryIcons() {
        // Check if memory cache needs refresh
        long currentTime = System.currentTimeMillis();
        boolean shouldRefreshMemoryCache = (currentTime - lastMemoryCacheRefresh) > MEMORY_CACHE_EXPIRATION;
        
        if (isInitialized.get() && !shouldRefreshMemoryCache && !categoryIconMap.isEmpty()) {
            Log.d(TAG, "✅ Skip refresh - memory cache is recent and icons are already loaded");
            return;
        }
        
        Log.d(TAG, "🔄 Refreshing category icons");
        
        // Reset state for full refresh
        if (shouldRefreshMemoryCache) {
            categoryIconMap.clear();
            normalizedCategories.clear();
            urlCache.evictAll();
            lastMemoryCacheRefresh = currentTime;
        }
        
        // Cancel any ongoing requests
        cancelCurrentCall();
        
        // Load from cache first, then from network if needed
        loadCategoryIcons();
    }
    
    /**
     * Clear all caches (for testing or when needed)
     */
    public void clearCaches() {
        Log.d(TAG, "🧹 Clearing all category icon caches");
        categoryIconMap.clear();
        normalizedCategories.clear();
        urlCache.evictAll();
        isInitialized.set(false);
        lastMemoryCacheRefresh = 0;
        
        // Clear ResourceRepository cache as well
        resourceRepository.deleteResourceByKey(CACHE_KEY_CATEGORY_ICONS);
    }

    /**
     * Add fallback icons for common categories
     */
    private void addFallbackIcons() {
        // Add some common fallbacks
        if (!categoryIconMap.containsKey("all")) {
            CategoryIcon allIcon = new CategoryIcon("all", "All", 
                "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png");
            categoryIconMap.put("all", allIcon);
            Log.d(TAG, "✅ Added fallback icon for 'All' category");
        }
        
        if (!categoryIconMap.containsKey("birthday")) {
            CategoryIcon birthdayIcon = new CategoryIcon("birthday", "Birthday", 
                "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/cake/materialicons/24dp/2x/baseline_cake_black_24dp.png");
            categoryIconMap.put("birthday", birthdayIcon);
            Log.d(TAG, "✅ Added fallback icon for 'Birthday' category");
        }
        
        if (!categoryIconMap.containsKey("wedding")) {
            CategoryIcon weddingIcon = new CategoryIcon("wedding", "Wedding", 
                "https://raw.githubusercontent.com/google/material-design-icons/master/png/places/cake/materialicons/24dp/2x/baseline_cake_black_24dp.png");
            categoryIconMap.put("wedding", weddingIcon);
            Log.d(TAG, "✅ Added fallback icon for 'Wedding' category");
        }
        
        // Add more common categories - expanded fallbacks
        String[][] fallbackCategories = {
            {"holiday", "Holiday", "https://raw.githubusercontent.com/google/material-design-icons/master/png/notification/event_note/materialicons/24dp/2x/baseline_event_note_black_24dp.png"},
            {"christmas", "Christmas", "https://raw.githubusercontent.com/google/material-design-icons/master/png/maps/local_florist/materialicons/24dp/2x/baseline_local_florist_black_24dp.png"},
            {"anniversary", "Anniversary", "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/date_range/materialicons/24dp/2x/baseline_date_range_black_24dp.png"},
            {"graduation", "Graduation", "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/school/materialicons/24dp/2x/baseline_school_black_24dp.png"},
            {"baby", "Baby", "https://raw.githubusercontent.com/google/material-design-icons/master/png/image/child_care/materialicons/24dp/2x/baseline_child_care_black_24dp.png"},
            {"invitation", "Invitation", "https://raw.githubusercontent.com/google/material-design-icons/master/png/content/mail/materialicons/24dp/2x/baseline_mail_black_24dp.png"},
            {"festival", "Festival", "https://raw.githubusercontent.com/google/material-design-icons/master/png/places/festival/materialicons/24dp/2x/baseline_festival_black_24dp.png"}
        };
        
        for (String[] categoryInfo : fallbackCategories) {
            String key = normalizeCategory(categoryInfo[0]);
            if (!categoryIconMap.containsKey(key)) {
                CategoryIcon icon = new CategoryIcon(key, categoryInfo[1], categoryInfo[2]);
                categoryIconMap.put(key, icon);
                Log.d(TAG, "✅ Added additional fallback icon for '" + categoryInfo[1] + "' category");
            }
        }
    }
    
    /**
     * Load icons from cache
     */
    private void loadFromCache() {
        try {
            Resource<String> cachedResource = resourceRepository.getResource(CACHE_KEY_CATEGORY_ICONS);
            if (cachedResource != null && cachedResource.getData() != null) {
                try {
                    JsonObject cachedData = JsonParser.parseString(cachedResource.getData()).getAsJsonObject();
                    Type listType = new TypeToken<List<CategoryIcon>>(){}.getType();
                    List<CategoryIcon> icons = gson.fromJson(cachedData.getAsJsonArray("data"), listType);
                    
                    Log.d(TAG, "Loaded " + icons.size() + " icons from cache");
                    
                    // Process icons from cache
                    processCategoryIcons(icons);
                    
                    // Skip network call if cache is fresh
                    if (isCacheExpired()) {
                        Log.d(TAG, "Cache expired, loading from network");
                        loadFromNetwork();
                    } else {
                        Log.d(TAG, "Using fresh cache, skipping network call");
                        loading.postValue(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing cache: " + e.getMessage(), e);
                    loadFromNetwork();
                }
            } else {
                Log.d(TAG, "No cache found, loading from network");
                loadFromNetwork();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading from cache: " + e.getMessage(), e);
            loadFromNetwork();
        }
    }
    
    /**
     * Load icons from network
     */
    private void loadFromNetwork() {
        if (!networkUtils.isNetworkAvailable()) {
            Log.w(TAG, "Network unavailable, using fallbacks");
            addFallbackIcons();
            isInitialized.set(true);
            loading.postValue(false);
            return;
        }
        
        Log.d(TAG, "📡 Loading category icons from network: " + BASE_URL + "categoryIcons");
        
        // Create API call
        Call<CategoryIconResponse> call = apiService.getCategoryIcons();
        setCurrentCall(call);
        
        // Execute API call
        call.enqueue(new Callback<CategoryIconResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryIconResponse> call, 
                                   @NonNull Response<CategoryIconResponse> response) {
                // Log raw response for debugging
                try {
                    Log.d(TAG, "📥 Raw API response: Code=" + response.code() + ", Message=" + response.message());
                    
                    // Enhanced logging for troubleshooting
                    if (response.code() != 200) {
                        Log.w(TAG, "⚠️ Non-200 response code: " + response.code());
                    }
                    
                    Log.d(TAG, "📥 Response headers: " + response.headers());
                    
                    // Try to extract raw response body for debugging
                    if (response.body() != null) {
                        String bodyJson = gson.toJson(response.body());
                        Log.d(TAG, "📄 Response body (first 500 chars): " + 
                            (bodyJson.length() > 500 ? bodyJson.substring(0, 500) + "..." : bodyJson));
                    } else if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            Log.d(TAG, "📄 Error body: " + errorJson);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error logging response", e);
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    CategoryIconResponse iconResponse = response.body();
                    
                    Log.d(TAG, "📦 API response details: success=" + iconResponse.isSuccess() + 
                          ", message=" + iconResponse.getMessage() + 
                          ", data size=" + (iconResponse.getData() != null ? iconResponse.getData().size() : "null"));
                    
                    if (iconResponse.isSuccess() && iconResponse.getData() != null) {
                        // Process the icons
                        List<CategoryIcon> icons = iconResponse.getData();
                        Log.d(TAG, "✅ Loaded " + icons.size() + " icons from API");
                        
                        // Print the first few icons for debugging
                        if (icons.size() > 0) {
                            int samplesToLog = Math.min(3, icons.size());
                            for (int i = 0; i < samplesToLog; i++) {
                                CategoryIcon icon = icons.get(i);
                                Log.d(TAG, "📎 Icon sample " + (i+1) + ": " + icon.getCategory() + 
                                      " -> " + icon.getCategoryIcon());
                            }
                        }
                        
                        // Process the icons
                        processCategoryIcons(icons);
                        
                        // Save to cache
                        saveToCache(iconResponse);
                    } else if (iconResponse.getData() != null) {
                        // Sometimes the API returns data without success flag
                        List<CategoryIcon> icons = iconResponse.getData();
                        if (icons != null && !icons.isEmpty()) {
                            Log.d(TAG, "✅ Loaded " + icons.size() + " icons from API (without success flag)");
                            
                            // Print the first few icons for debugging
                            int samplesToLog = Math.min(3, icons.size());
                            for (int i = 0; i < samplesToLog; i++) {
                                CategoryIcon icon = icons.get(i);
                                Log.d(TAG, "📎 Icon sample " + (i+1) + ": " + icon.getCategory() + 
                                      " -> " + icon.getCategoryIcon());
                            }
                            
                            processCategoryIcons(icons);
                            saveToCache(iconResponse);
                        } else {
                            Log.w(TAG, "⚠️ API returned empty icons list or missing success flag");
                            addFallbackIcons();
                        }
                    } else {
                        // Handle API success but with error data
                        String errorMessage = iconResponse.getMessage() != null ? 
                            iconResponse.getMessage() : "Unknown error loading category icons";
                        Log.e(TAG, "❌ " + errorMessage, null);
                        error.postValue(errorMessage);
                        addFallbackIcons();
                    }
                } else if (response.code() == 404) {
                    // Handle 404 errors specifically - the endpoint might have changed
                    String errorMessage = "Category icons endpoint not found (404)";
                    Log.e(TAG, "❌ " + errorMessage);
                    error.postValue(errorMessage);
                    
                    // Always ensure we have fallbacks
                    addFallbackIcons();
                } else {
                    // Handle unsuccessful response
                    String errorMessage = "Error loading category icons: " + response.code();
                    Log.e(TAG, "❌ " + errorMessage, null);
                    error.postValue(errorMessage);
                    
                    // Always ensure we have fallbacks
                    addFallbackIcons();
                    
                    // Try to parse error body for more details
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                            
                            // Try to extract message from error JSON if possible
                            try {
                                JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                                if (errorJson.has("message")) {
                                    String message = errorJson.get("message").getAsString();
                                    Log.e(TAG, "Error message from response: " + message);
                                    error.postValue(message);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body JSON", e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
                
                // Update loading state
                loading.postValue(false);
                currentCall = null;
            }
            
            @Override
            public void onFailure(@NonNull Call<CategoryIconResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "🛑 API call cancelled");
                } else {
                    // Handle error
                    String errorMessage = "Network error: " + t.getMessage();
                    Log.e(TAG, "❌ " + errorMessage, t);
                    error.postValue(errorMessage);
                    
                    // Extended logging for network errors
                    if (t instanceof java.net.SocketTimeoutException) {
                        Log.e(TAG, "Socket timeout - server might be slow or unreachable");
                    } else if (t instanceof java.net.UnknownHostException) {
                        Log.e(TAG, "Unknown host - check internet connection or DNS settings");
                    } else if (t instanceof java.io.IOException) {
                        Log.e(TAG, "IO Exception - possible network issue");
                    }
                    
                    // Try to retry if appropriate
                    if (shouldRetry() && !isInitialized.get()) {
                        Log.d(TAG, "🔄 Retrying network request (attempt " + currentRetryCount + ")");
                        retryWithBackoff();
                    } else {
                        // Add fallbacks if we've exhausted retries
                        addFallbackIcons();
                        loading.postValue(false);
                    }
                }
                
                currentCall = null;
            }
        });
    }
    
    /**
     * Save response to cache
     */
    private void saveToCache(CategoryIconResponse response) {
        executors.diskIO().execute(() -> {
            try {
                String data = gson.toJson(response);
                resourceRepository.saveResource(CACHE_KEY_CATEGORY_ICONS, data);
                Log.d(TAG, "Saved category icons to cache");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving to cache: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Check if we should retry the request
     */
    private boolean shouldRetry() {
        return currentRetryCount < MAX_RETRIES && networkUtils.isNetworkAvailable();
    }
    
    /**
     * Retry with exponential backoff
     */
    private void retryWithBackoff() {
        currentRetryCount++;
        
        // Calculate backoff time
        long backoffTime = (long) (currentBackoffMs * Math.pow(BACKOFF_MULTIPLIER, currentRetryCount - 1));
        
        Log.d(TAG, "Retrying in " + backoffTime + "ms (attempt " + currentRetryCount + " of " + MAX_RETRIES + ")");
        
        // Schedule retry
        new android.os.Handler().postDelayed(this::loadFromNetwork, backoffTime);
    }

    /**
     * Check if the cache is expired
     */
    private boolean isCacheExpired() {
        long currentTime = System.currentTimeMillis();
        long cacheTime = lastMemoryCacheRefresh + CACHE_EXPIRATION_ICONS;
        return currentTime > cacheTime;
    }
}