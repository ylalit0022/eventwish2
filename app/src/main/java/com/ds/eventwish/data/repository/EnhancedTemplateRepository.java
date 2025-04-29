package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.ds.eventwish.data.cache.ResourceCache;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
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
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Enhanced repository for templates with improved caching, offline support, and error handling
 */
public class EnhancedTemplateRepository {
    private static final String TAG = "EnhancedTemplateRepo";
    
    // Cache keys
    private static final String CACHE_KEY_TEMPLATES = "templates";
    private static final String CACHE_KEY_TEMPLATES_BY_CATEGORY = "templates_category_";
    private static final String CACHE_KEY_CATEGORIES = "categories";
    
    // Cache expiration times
    private static final long CACHE_EXPIRATION_TEMPLATES = TimeUnit.HOURS.toMillis(1); // 1 hour
    private static final long CACHE_EXPIRATION_CATEGORIES = TimeUnit.DAYS.toMillis(1); // 1 day
    
    // Pagination
    private static final int PAGE_SIZE = 20;
    
    // Singleton instance
    private static volatile EnhancedTemplateRepository instance;
    
    // Dependencies
    private final Context context;
    private final ResourceRepository resourceRepository;
    private final ResourceCache resourceCache;
    private final ApiService apiService;
    private final AppExecutors executors;
    private final NetworkUtils networkUtils;
    private final Gson gson;
    private final ErrorHandler errorHandler;
    
    // LiveData
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    // State
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private String currentCategory = null;
    private Call<TemplateResponse> currentCall;
    
    /**
     * Get the singleton instance of EnhancedTemplateRepository
     * @param context Application context
     * @return EnhancedTemplateRepository instance
     */
    public static EnhancedTemplateRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (EnhancedTemplateRepository.class) {
                if (instance == null) {
                    if (context == null) {
                        throw new IllegalStateException("Context cannot be null when initializing EnhancedTemplateRepository");
                    }
                    // Make sure ResourceRepository is initialized first with context
                    ResourceRepository.getInstance(context);
                    instance = new EnhancedTemplateRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the singleton instance of EnhancedTemplateRepository
     * This method should only be called after initialization with context
     * @return EnhancedTemplateRepository instance
     * @throws IllegalStateException if getInstance(Context) has not been called first
     */
    public static EnhancedTemplateRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EnhancedTemplateRepository must be initialized with getInstance(Context) before calling getInstance()");
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private EnhancedTemplateRepository(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        // Use the parameterless getInstance() after ensuring initialization
        this.resourceRepository = ResourceRepository.getInstance();
        this.resourceCache = ResourceCache.getInstance(context);
        this.apiService = ApiClient.getClient();
        this.executors = AppExecutors.getInstance();
        this.networkUtils = NetworkUtils.getInstance(context);
        this.gson = new Gson();
        this.errorHandler = ErrorHandler.getInstance(context);
        
        // Initialize templates list
        templates.setValue(new ArrayList<>());
        
        Log.d(TAG, "EnhancedTemplateRepository initialized");
    }
    
    /**
     * Get templates as LiveData
     * @return LiveData with list of templates
     */
    public LiveData<List<Template>> getTemplates() {
        return templates;
    }
    
    /**
     * Get categories as LiveData
     * @return LiveData with map of categories and their counts
     */
    public LiveData<Map<String, Integer>> getCategories() {
        return categories;
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
     * Check if repository is currently loading
     * @return true if loading, false otherwise
     */
    public boolean isLoading() {
        return loading.getValue() != null && loading.getValue();
    }
    
    /**
     * Check if there are more pages to load
     * @return true if there are more pages, false otherwise
     */
    public boolean hasMorePages() {
        return hasMorePages;
    }
    
    /**
     * Get current category filter
     * @return Current category or null for all categories
     */
    public String getCurrentCategory() {
        return currentCategory;
    }
    
    /**
     * Cancel current API call
     */
    public void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }
    
    /**
     * Set current API call
     * @param call Call to set as current
     */
    private void setCurrentCall(Call<TemplateResponse> call) {
        cancelCurrentCall();
        currentCall = call;
    }
    
    /**
     * Set the current category filter
     * @param category The category to filter by, or null for all categories
     */
    public void setCategory(String category) {
        setCategory(category, true);
    }
    
    /**
     * Set the current category filter
     * @param category The category to filter by, or null for all categories
     * @param reload Whether to reload templates after setting the category
     */
    public void setCategory(String category, boolean reload) {
        if ((category == null && currentCategory == null) || 
            (category != null && category.equals(currentCategory))) {
            return;
        }
        
        Log.d(TAG, "Changing category from " + 
            (currentCategory != null ? currentCategory : "All") + " to " + 
            (category != null ? category : "All"));
            
        // Save current templates before changing category
        List<Template> currentTemplateList = templates.getValue();
        
        currentCategory = category;
        currentPage = 1;
        hasMorePages = true;
        
        if (reload) {
            loadTemplates(true);
        }
    }
    
    /**
     * Load templates with offline-first approach
     * @param forceRefresh Whether to force a refresh from network
     */
    public void loadTemplates(boolean forceRefresh) {
        if (loading.getValue()) {
            return;
        }
        
        loading.setValue(true);
        
        // If this is a forced refresh, reset pagination
        if (forceRefresh) {
            currentPage = 1;
            hasMorePages = true;
            
            // Only clear templates if we're not filtering by category or if this is the initial load
            if (currentCategory == null || templates.getValue() == null || templates.getValue().isEmpty()) {
                if (templates.getValue() != null) {
                    templates.getValue().clear();
                    // Notify observers of the empty list to clear the UI
                    templates.setValue(new ArrayList<>());
                } else {
                    templates.setValue(new ArrayList<>());
                }
            }
        }
        
        // If we've already loaded all pages, don't make another request
        if (!hasMorePages && currentPage > 1) {
            loading.setValue(false);
            return;
        }
        
        // Log request details
        Log.d(TAG, "Loading templates - page: " + currentPage + 
                  ", category: " + (currentCategory != null ? currentCategory : "All") + 
                  ", forceRefresh: " + forceRefresh);
        
        // First try to get from cache
        String cacheKey = getCacheKey();
        executors.diskIO().execute(() -> {
            TemplateResponse cachedResponse = resourceCache.get(cacheKey);
            
            if (cachedResponse != null && !forceRefresh) {
                Log.d(TAG, "Loaded templates from cache: " + cacheKey);
                processTemplateResponse(cachedResponse, forceRefresh);
                
                // If network is available and not metered, check for updates in background
                if (networkUtils.isConnected() && !networkUtils.isConnectionMetered()) {
                    refreshTemplatesInBackground();
                }
            } else {
                // If not in cache or force refresh, load from network
                if (networkUtils.isConnected()) {
                    loadTemplatesFromNetwork(forceRefresh);
                } else {
                    // If offline and no cache, return error
                    if (cachedResponse != null) {
                        Log.d(TAG, "Offline, using stale cached data: " + cacheKey);
                        processTemplateResponse(cachedResponse, forceRefresh);
                        error.postValue("You're offline. Showing cached data that may be outdated.");
                    } else {
                        Log.e(TAG, "Offline and no cached data available: " + cacheKey);
                        loading.postValue(false);
                        error.postValue("No internet connection and no cached data available.");
                        errorHandler.handleError(
                                ErrorHandler.ErrorType.OFFLINE,
                                "No internet connection and no cached data available.",
                                ErrorHandler.ErrorSeverity.MEDIUM);
                    }
                }
            }
        });
    }
    
    /**
     * Load templates from network
     * @param forceRefresh Whether this is a forced refresh
     */
    private void loadTemplatesFromNetwork(boolean forceRefresh) {
        Call<TemplateResponse> call;
        if (currentCategory != null) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        setCurrentCall(call);
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(@NonNull Call<TemplateResponse> call, @NonNull Response<TemplateResponse> response) {
                loading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    
                    // Cache the response
                    String cacheKey = getCacheKey();
                    executors.diskIO().execute(() -> {
                        resourceCache.put(cacheKey, templateResponse, CACHE_EXPIRATION_TEMPLATES);
                        
                        // Also cache categories separately
                        if (templateResponse.getCategories() != null && !templateResponse.getCategories().isEmpty()) {
                            resourceCache.put(CACHE_KEY_CATEGORIES, templateResponse.getCategories(), CACHE_EXPIRATION_CATEGORIES);
                        }
                    });
                    
                    // Process the response
                    processTemplateResponse(templateResponse, forceRefresh);
                } else {
                    String errorMsg = "Failed to load templates";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    
                    error.postValue(errorMsg);
                    errorHandler.handleError(
                            ErrorHandler.ErrorType.SERVER_ERROR,
                            errorMsg,
                            ErrorHandler.ErrorSeverity.MEDIUM);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TemplateResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Call was canceled");
                    loading.postValue(false);
                    return;
                }
                
                loading.postValue(false);
                
                // Try to get from cache as fallback
                String cacheKey = getCacheKey();
                executors.diskIO().execute(() -> {
                    TemplateResponse cachedResponse = resourceCache.get(cacheKey);
                    if (cachedResponse != null) {
                        Log.d(TAG, "Network request failed, using cache: " + cacheKey);
                        processTemplateResponse(cachedResponse, false);
                        error.postValue("Network error. Showing cached data.");
                    } else {
                        String errorMsg = NetworkErrorHandler.getErrorMessage(context, t);
                        error.postValue(errorMsg);
                        NetworkErrorHandler.handleNetworkError(context, t, errorHandler);
                    }
                });
            }
        });
    }
    
    /**
     * Refresh templates in background without updating UI loading state
     */
    private void refreshTemplatesInBackground() {
        Call<TemplateResponse> call;
        if (currentCategory != null) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(@NonNull Call<TemplateResponse> call, @NonNull Response<TemplateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    
                    // Cache the response
                    String cacheKey = getCacheKey();
                    executors.diskIO().execute(() -> {
                        resourceCache.put(cacheKey, templateResponse, CACHE_EXPIRATION_TEMPLATES);
                        
                        // Also cache categories separately
                        if (templateResponse.getCategories() != null && !templateResponse.getCategories().isEmpty()) {
                            resourceCache.put(CACHE_KEY_CATEGORIES, templateResponse.getCategories(), CACHE_EXPIRATION_CATEGORIES);
                        }
                    });
                    
                    // Update UI with new data
                    processTemplateResponse(templateResponse, false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TemplateResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Background refresh failed", t);
            }
        });
    }
    
    /**
     * Process template response and update LiveData
     * @param templateResponse Response to process
     * @param forceRefresh Whether this is a forced refresh
     */
    private void processTemplateResponse(TemplateResponse templateResponse, boolean forceRefresh) {
        List<Template> currentList = templates.getValue();
        if (currentList == null) currentList = new ArrayList<>();
        
        if (forceRefresh || currentPage == 1) {
            currentList = new ArrayList<>(templateResponse.getTemplates());
        } else {
            currentList.addAll(templateResponse.getTemplates());
        }
        
        // Log all template IDs for debugging
        for (Template template : templateResponse.getTemplates()) {
            Log.d(TAG, "Template: " + template.getTitle() + 
                      ", ID: " + template.getId() + 
                      ", Created: " + template.getCreatedAt());
        }
        
        templates.postValue(currentList);
        
        // Process categories
        Map<String, Integer> categoryMap = templateResponse.getCategories();
        if (categoryMap != null && !categoryMap.isEmpty()) {
            Log.d(TAG, "Categories received: " + categoryMap.size());
            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                Log.d(TAG, "Category: " + entry.getKey() + ", Count: " + entry.getValue());
            }
            categories.postValue(categoryMap);
        } else {
            Log.d(TAG, "Categories map is null or empty");
            // Try to get categories from cache
            executors.diskIO().execute(() -> {
                Map<String, Integer> cachedCategories = resourceCache.get(CACHE_KEY_CATEGORIES);
                if (cachedCategories != null && !cachedCategories.isEmpty()) {
                    Log.d(TAG, "Using cached categories: " + cachedCategories.size());
                    categories.postValue(cachedCategories);
                } else {
                    // Add mock categories if none are returned from API or cache
                    Map<String, Integer> mockCategories = new HashMap<>();
                    mockCategories.put("Birthday", 10);
                    mockCategories.put("Wedding", 8);
                    mockCategories.put("Anniversary", 6);
                    mockCategories.put("Graduation", 5);
                    mockCategories.put("Holiday", 7);
                    mockCategories.put("Congratulations", 4);
                    Log.d(TAG, "Added mock categories: " + mockCategories.size());
                    categories.postValue(mockCategories);
                }
            });
        }
        
        hasMorePages = templateResponse.isHasMore();
        currentPage++;
    }
    
    /**
     * Get cache key based on current state
     * @return Cache key
     */
    private String getCacheKey() {
        if (currentCategory != null) {
            return CACHE_KEY_TEMPLATES_BY_CATEGORY + currentCategory + "_page_" + currentPage;
        } else {
            return CACHE_KEY_TEMPLATES + "_page_" + currentPage;
        }
    }
    
    /**
     * Set the current page for pagination
     * @param page Page number
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
    
    /**
     * Get the current page
     * @return Current page number
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Clear the template cache and reset pagination
     */
    public void clearCache() {
        Log.d(TAG, "Clearing template cache");
        
        // Save current categories before clearing
        Map<String, Integer> currentCategories = categories.getValue();
        
        // Reset pagination
        currentPage = 1;
        hasMorePages = true;
        
        // Clear templates
        if (templates.getValue() != null) {
            templates.getValue().clear();
            templates.setValue(new ArrayList<>());
        } else {
            templates.setValue(new ArrayList<>());
        }
        
        // Cancel any ongoing requests
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        
        // Reset error state
        error.setValue(null);
        
        // Reset loading state
        loading.setValue(false);
        
        // Clear cache
        executors.diskIO().execute(() -> {
            // Clear all template caches
            for (int i = 1; i <= 10; i++) { // Clear up to 10 pages
                resourceCache.remove(CACHE_KEY_TEMPLATES + "_page_" + i);
                
                // Clear category-specific caches
                if (currentCategories != null) {
                    for (String category : currentCategories.keySet()) {
                        resourceCache.remove(CACHE_KEY_TEMPLATES_BY_CATEGORY + category + "_page_" + i);
                    }
                }
            }
        });
        
        // Restore categories if they were available
        if (currentCategories != null && !currentCategories.isEmpty()) {
            Log.d(TAG, "Restoring " + currentCategories.size() + " categories after cache clear");
            categories.setValue(currentCategories);
        }
    }
    
    /**
     * Get a single template by ID
     * @param templateId Template ID
     * @param forceRefresh Whether to force a refresh from network
     * @return LiveData with Resource containing the template
     */
    public LiveData<Resource<Template>> getTemplateById(String templateId, boolean forceRefresh) {
        MediatorLiveData<Resource<Template>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));
        
        // Use ResourceRepository to load the template
        LiveData<Resource<JsonObject>> source = resourceRepository.loadResource(
                ResourceType.TEMPLATE,
                templateId,
                forceRefresh);
        
        result.addSource(source, resource -> {
            if (resource.isSuccess()) {
                JsonObject data = resource.getData();
                if (data != null) {
                    Template template = gson.fromJson(data, Template.class);
                    result.setValue(Resource.success(template, resource.isStale()));
                } else {
                    result.setValue(Resource.error("Template data is null", null));
                }
            } else if (resource.isError()) {
                result.setValue(Resource.error(resource.getMessage(), null));
            } else {
                result.setValue(Resource.loading(null));
            }
        });
        
        return result;
    }
} 