package com.ds.eventwish.data.repository;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
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
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for templates with improved caching, offline support, and error handling
 */
public class TemplateRepository {
    private static final String TAG = "TemplateRepository";
    
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
    private static volatile TemplateRepository instance;
    
    // Dependencies
    private final Context context;
    private final ResourceRepository resourceRepository;
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
     * Get the singleton instance of TemplateRepository
     * @return TemplateRepository instance
     */
    public static synchronized TemplateRepository getInstance() {
        if (instance == null) {
            Context appContext = com.ds.eventwish.EventWishApplication.getAppContext();
            if (appContext == null) {
                Log.e(TAG, "Cannot initialize TemplateRepository: Application context is null");
                throw new IllegalStateException("Cannot initialize TemplateRepository: Application context is null");
            }
            instance = new TemplateRepository(appContext);
        }
        return instance;
    }
    
    /**
     * Static initialization method with explicit context
     * @param context The application context
     * @return The singleton instance
     */
    public static synchronized TemplateRepository init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize TemplateRepository: Context is null");
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        if (instance == null) {
            instance = new TemplateRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private TemplateRepository(Context context) {
        this.context = context.getApplicationContext();
        
        try {
            // Initialize dependencies safely
            resourceRepository = ResourceRepository.getInstance(context);
            
            // Initialize API Service with retry logic
            ApiService tempApiService = null;
            try {
                // First attempt to get API service
                tempApiService = ApiClient.getClient();
                Log.d(TAG, "Successfully obtained ApiService");
            } catch (Exception e) {
                Log.e(TAG, "Failed to get ApiService, will retry once", e);
                
                // Try one more time after a short delay
                boolean interrupted = false;
                try {
                    Thread.sleep(500);
                    tempApiService = ApiClient.getClient();
                    Log.d(TAG, "Successfully obtained ApiService on retry");
                } catch (InterruptedException ie) {
                    interrupted = true;
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Interrupted while waiting to retry ApiService initialization", ie);
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to get ApiService on retry", e2);
                }
                
                // If thread was interrupted, log it but continue with initialization
                if (interrupted) {
                    Log.w(TAG, "Thread was interrupted during ApiService initialization");
                }
            }
            
            // Assign the final result to the instance variable
            apiService = tempApiService;
            
            executors = AppExecutors.getInstance();
            networkUtils = NetworkUtils.getInstance(context);
            gson = new Gson();
            errorHandler = ErrorHandler.getInstance(context);
            
            // Initialize LiveData objects
            templates.setValue(new ArrayList<>());
            categories.setValue(new HashMap<>());
            loading.setValue(false);
            error.setValue(null);
            
            Log.d(TAG, "TemplateRepository initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error during TemplateRepository initialization", e);
            // Initialize minimum required components to avoid nulls
            if (templates.getValue() == null) templates.setValue(new ArrayList<>());
            if (categories.getValue() == null) categories.setValue(new HashMap<>());
            if (loading.getValue() == null) loading.setValue(false);
            
            // Re-throw to ensure proper handling upstream
            throw new IllegalStateException("Failed to initialize TemplateRepository", e);
        }
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
            Log.d(TAG, "Canceling current API call: " + 
                  (currentCall.request() != null ? currentCall.request().url() : "unknown URL"));
            currentCall.cancel();
            
            // Reset loading state when canceling
            loading.postValue(false);
        }
    }
    
    /**
     * Set current API call
     * @param call Call to set as current
     */
    private void setCurrentCall(Call<TemplateResponse> call) {
        if (call == null) {
            Log.e(TAG, "Cannot set null call as current call");
            return;
        }
        
        // Only cancel the current call if it's for a different request
        // This prevents unnecessary cancellations during retry attempts
        if (currentCall != null && !currentCall.isCanceled()) {
            try {
                if (currentCall.request() != null && call.request() != null && 
                    !currentCall.request().url().equals(call.request().url())) {
                    
                    Log.d(TAG, "Replacing current API call: " + 
                          (currentCall.request() != null ? currentCall.request().url() : "unknown URL") + 
                          " with new call: " + (call.request() != null ? call.request().url() : "unknown URL"));
                    
                    currentCall.cancel();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error comparing request URLs", e);
                // Just cancel to be safe
                currentCall.cancel();
            }
        }
        
        // Set the new call
        currentCall = call;
    }
    
    /**
     * Set category filter and load templates for that category
     * @param category Category to filter by, or null for all categories
     */
    public void setCategory(String category) {
        Log.d(TAG, "Setting category filter: " + (category != null ? category : "All"));
        
        String previousCategory = currentCategory;
        currentCategory = category;
        
        // Only reload if the category has changed
        if (!Objects.equals(previousCategory, currentCategory)) {
            // Reset pagination when changing categories
            currentPage = 1;
            hasMorePages = true;
            
            // Load templates with the new category
            loadTemplates(true);
        }
    }
    
    /**
     * Load templates with offline-first approach
     * @param forceRefresh Whether to force a refresh from network
     */
    public void loadTemplates(boolean forceRefresh) {
        Log.d(TAG, "loadTemplates called with forceRefresh=" + forceRefresh + 
              ", currentCategory=" + (currentCategory != null ? currentCategory : "All") + 
              ", currentPage=" + currentPage);
        
        // Set loading state
        loading.setValue(true);
        
        // First try to load from cache if not forcing refresh
        if (!forceRefresh) {
            String cacheKey = getCacheKey();
            resourceRepository.getResource(
                ResourceRepository.RESOURCE_TYPE_TEMPLATE,
                cacheKey,
                false
            ).observeForever(resource -> {
                if (resource.isSuccess() && resource.getData() != null) {
                    Log.d(TAG, "Using cached template data for category: " + 
                         (currentCategory != null ? currentCategory : "All") + 
                         ", found " + (resource.getData() != null ? resource.getData().size() : 0) + " templates");
                    // Process cached data
                    processTemplateResponse(gson.fromJson(resource.getData(), TemplateResponse.class), false);
                    loading.setValue(false);
                } else {
                    // No cache, load from network
                    Log.d(TAG, "No cache found for templates with key: " + cacheKey + ", loading from network");
                    loadTemplatesFromNetwork(forceRefresh);
                }
            });
        } else {
            // Bypass cache and load directly from network
            Log.d(TAG, "Force refresh requested, loading templates from network");
            loadTemplatesFromNetwork(forceRefresh);
        }
    }
    
    /**
     * Load templates from network
     * @param forceRefresh Whether this is a forced refresh
     */
    private void loadTemplatesFromNetwork(boolean forceRefresh) {
        loading.postValue(true);
        
        // If API service is not available, handle gracefully
        if (apiService == null) {
            Log.e(TAG, "Cannot load templates from network: API service is not available");
            loading.postValue(false);
            error.postValue("Network service unavailable. Check your connection and restart the app.");
            return;
        }
        
        Call<TemplateResponse> call;
        String normalizedCategory = normalizeCategory(currentCategory);
        
        if (currentCategory != null && !normalizedCategory.isEmpty()) {
            // Use distinct log format to clearly identify the API call being made
            Log.d(TAG, "🔍 API CALL: Getting templates for category: '" + currentCategory + 
                  "' (normalized: '" + normalizedCategory + "'), page: " + currentPage + 
                  ", pageSize: " + PAGE_SIZE);
            
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            // Use distinct log format to clearly identify the API call being made
            Log.d(TAG, "🔍 API CALL: Getting all templates, page: " + currentPage + 
                  ", pageSize: " + PAGE_SIZE);
            
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }
        
        setCurrentCall(call);
        
        currentCall.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(@NonNull Call<TemplateResponse> call, @NonNull Response<TemplateResponse> response) {
                loading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    
                    // Use consistent logging for successful responses
                    int templateCount = (templateResponse.getTemplates() != null) ? 
                                        templateResponse.getTemplates().size() : 0;
                    
                    if (currentCategory != null && !normalizedCategory.isEmpty()) {
                        Log.d(TAG, "✅ API SUCCESS: Received " + templateCount + 
                              " templates for category: '" + currentCategory + 
                              "', page: " + currentPage);
                    } else {
                        Log.d(TAG, "✅ API SUCCESS: Received " + templateCount + 
                              " templates for all categories, page: " + currentPage);
                    }
                    
                    // Cache the response
                    String cacheKey = getCacheKey();
                    executors.diskIO().execute(() -> {
                        resourceRepository.saveResource(
                            ResourceRepository.RESOURCE_TYPE_TEMPLATE,
                            cacheKey,
                            gson.toJsonTree(templateResponse).getAsJsonObject(),
                            null,
                            null,
                            new java.util.Date(System.currentTimeMillis() + CACHE_EXPIRATION_TEMPLATES)
                        );
                        
                        // Also cache categories separately
                        if (templateResponse.getCategories() != null && !templateResponse.getCategories().isEmpty()) {
                            resourceRepository.saveResource(
                                ResourceRepository.RESOURCE_TYPE_CATEGORY,
                                CACHE_KEY_CATEGORIES,
                                gson.toJsonTree(templateResponse.getCategories()).getAsJsonObject(),
                                null,
                                null,
                                new java.util.Date(System.currentTimeMillis() + CACHE_EXPIRATION_CATEGORIES)
                            );
                        }
                    });
                    
                    // Process the response
                    processTemplateResponse(templateResponse, forceRefresh);
                } else {
                    String errorMsg;
                    if (currentCategory != null && !normalizedCategory.isEmpty()) {
                        errorMsg = "Error loading templates for category '" + currentCategory + 
                                   "': HTTP " + response.code();
                    } else {
                        errorMsg = "Error loading templates: HTTP " + response.code();
                    }
                    
                    Log.e(TAG, "❌ API ERROR: " + errorMsg);
                    error.postValue(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TemplateResponse> call, @NonNull Throwable t) {
                // Don't show errors for canceled calls - they are expected during navigation
                if (call.isCanceled()) {
                    Log.d(TAG, "Template request was canceled - this is normal during navigation");
                    
                    // Just reset loading state and return without error
                    loading.postValue(false);
                    return;
                }
                
                // Handle IO cancellation exceptions separately
                if (t instanceof IOException && t.getMessage() != null && 
                    (t.getMessage().contains("Canceled") || 
                     t.getMessage().contains("Socket closed") || 
                     t.getMessage().contains("Connection reset") ||
                     "canceled".equalsIgnoreCase(t.getMessage()))) {
                    
                    Log.d(TAG, "Template request was canceled via IOException: " + t.getMessage());
                    loading.postValue(false);
                    return;
                }
                
                // Regular error handling for non-cancellation errors with improved logging
                String errorContext = currentCategory != null ? 
                                     "for category '" + currentCategory + "'" : 
                                     "for all categories";
                
                Log.e(TAG, "❌ NETWORK ERROR: Failed to load templates " + errorContext + 
                      ": " + t.getMessage(), t);
                
                // Consistent error messaging based on network state
                if (!networkUtils.isNetworkAvailable()) {
                    error.postValue("No internet connection. Showing cached data if available.");
                } else {
                    error.postValue("Network error: " + t.getMessage());
                }
                
                // Try to load from cache as a fallback with improved caching strategy
                String cacheKey = getCacheKey();
                Log.d(TAG, "🔄 CACHE FALLBACK: Attempting to load templates from cache with key: " + cacheKey);
                
                resourceRepository.getResource(
                    ResourceRepository.RESOURCE_TYPE_TEMPLATE,
                    cacheKey,
                    false
                ).observeForever(resource -> {
                    if (resource.isSuccess() && resource.getData() != null) {
                        // Successfully loaded from cache with improved logging
                        Log.d(TAG, "📦 CACHE HIT: Successfully loaded templates from cache, found " + 
                            (resource.getData() != null ? resource.getData().size() : 0) + " templates " + 
                            errorContext);
                            
                        processTemplateResponse(gson.fromJson(resource.getData(), TemplateResponse.class), false);
                        
                        // Keep error message to indicate we're showing cached data
                        error.postValue("Network error. Showing cached data.");
                    } else {
                        Log.e(TAG, "📦 CACHE MISS: No cached data available for templates " + errorContext);
                        
                        // If we're on the first page and have no templates, show empty UI
                        if (currentPage == 1 && (templates.getValue() == null || templates.getValue().isEmpty())) {
                            templates.postValue(new ArrayList<>());
                        }
                        
                        // Enhanced error message with more context
                        if (!networkUtils.isNetworkAvailable()) {
                            error.postValue("No internet connection and no cached data available " + 
                                           errorContext + ". Please check your connection and try again.");
                        } else {
                            error.postValue("Unable to load templates " + errorContext + 
                                           ". Please try again later.");
                        }
                    }
                });
                
                // Reset loading state
                loading.postValue(false);
            }
        });
    }
    
    /**
     * Process template response and update LiveData
     * @param response Response to process
     * @param forceRefresh Whether this is a forced refresh
     */
    private void processTemplateResponse(TemplateResponse response, boolean forceRefresh) {
        Log.d(TAG, "Processing template response for category: " + 
             (currentCategory != null ? currentCategory : "All"));
        
        if (response == null) {
            Log.e(TAG, "Template response is null");
            return;
        }
        
        List<Template> responseTemplates = response.getTemplates();
        Log.d(TAG, "Response contains " + (responseTemplates != null ? responseTemplates.size() : 0) + " templates");
        
        if (responseTemplates == null) {
            responseTemplates = new ArrayList<>();
            Log.w(TAG, "Response templates list is null, using empty list");
        }
        
        // Handle case where API returns empty templates list for a category
        if (responseTemplates.isEmpty() && currentCategory != null) {
            Log.w(TAG, "API returned empty templates list for category: " + currentCategory);
            
            // Set an empty list to avoid null pointer issues
            templates.setValue(new ArrayList<>());
            
            // Update hasMorePages flag to avoid infinite scrolling attempts
            hasMorePages = false;
            
            // Categories processing still continues below even for empty template list
        }
        
        // Debug log - output all template categories to identify possible case/formatting issues
        if (currentCategory != null && !responseTemplates.isEmpty()) {
            Log.d(TAG, "Current filter category ('" + currentCategory + "'). All template categories in response:");
            for (Template template : responseTemplates) {
                Log.d(TAG, "Template ID: " + template.getId() + 
                      ", Title: " + template.getTitle() + 
                      ", Category: '" + template.getCategory() + "'");
            }
        }
        
        // Filter templates by category if needed and the API didn't already filter
        List<Template> filteredTemplates = new ArrayList<>();
        if (currentCategory != null && !responseTemplates.isEmpty()) {
            String normalizedCategory = currentCategory.toLowerCase().trim();
            Log.d(TAG, "Filtering by normalized category: '" + normalizedCategory + "'");
            
            for (Template template : responseTemplates) {
                String templateCategory = template.getCategory();
                
                // Handle null or empty category
                if (templateCategory == null || templateCategory.isEmpty()) {
                    Log.d(TAG, "Template has null/empty category, skipping: " + template.getId());
                    continue;
                }
                
                // Normalize the template category for case-insensitive comparison
                String normalizedTemplateCategory = templateCategory.toLowerCase().trim();
                
                if (normalizedTemplateCategory.equals(normalizedCategory)) {
                    // Check for duplicates before adding
                    boolean isDuplicate = false;
                    for (Template existing : filteredTemplates) {
                        if (existing.getId().equals(template.getId())) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    
                    if (!isDuplicate) {
                        filteredTemplates.add(template);
                        Log.d(TAG, "Including template: " + template.getId() + " - " + template.getTitle() + 
                              " (category: " + template.getCategory() + ")");
                    } else {
                        Log.d(TAG, "Excluding duplicate template: " + template.getId());
                    }
                } else {
                    Log.d(TAG, "Excluding template: " + template.getId() + " - " + template.getTitle() + 
                          " (category: '" + templateCategory + "') - normalized: '" + normalizedTemplateCategory + 
                          "' - does not match filter: '" + normalizedCategory + "'");
                }
            }
            Log.d(TAG, "Filtered " + responseTemplates.size() + " templates to " + 
                 filteredTemplates.size() + " for category: " + currentCategory);
            
            // If we didn't find any templates for the category, try a more lenient approach
            if (filteredTemplates.isEmpty()) {
                Log.w(TAG, "No templates found using exact match for category '" + currentCategory + 
                      "'. Trying contains match...");
                
                // Try a more lenient "contains" match
                for (Template template : responseTemplates) {
                    String templateCategory = template.getCategory();
                    if (templateCategory != null) {
                        String normalizedTemplateCategory = templateCategory.toLowerCase().trim();
                        if (normalizedTemplateCategory.contains(normalizedCategory) || 
                            normalizedCategory.contains(normalizedTemplateCategory)) {
                            
                            // Check for duplicates before adding
                            boolean isDuplicate = false;
                            for (Template existing : filteredTemplates) {
                                if (existing.getId().equals(template.getId())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            
                            if (!isDuplicate) {
                                filteredTemplates.add(template);
                                Log.d(TAG, "Including template using contains match: " + template.getId() + 
                                      " - " + template.getTitle() + " (category: " + template.getCategory() + ")");
                            }
                        }
                    }
                }
                
                Log.d(TAG, "After contains matching, found " + filteredTemplates.size() + 
                      " templates for category: " + currentCategory);
            }
            
            // Use the filtered list if it's not empty
            if (!filteredTemplates.isEmpty()) {
                responseTemplates = filteredTemplates;
            } else if (!responseTemplates.isEmpty()) {
                Log.w(TAG, "No templates found for category: " + currentCategory + 
                      " even with lenient matching. Using all templates instead.");
            }
        }
        
        // Get current templates or create new list
        List<Template> currentTemplates = templates.getValue();
        if (currentTemplates == null) {
            currentTemplates = new ArrayList<>();
        }
        
        // Reset list if this is page 1 or forced refresh
        if (currentPage == 1 || forceRefresh) {
            Log.d(TAG, "Clearing existing templates for new data (page 1 or forced refresh)");
            currentTemplates.clear();
        }
        
        // Add new templates, avoiding duplicates
        if (!responseTemplates.isEmpty()) {
            if (forceRefresh || currentPage == 1) {
                // For page 1 or force refresh, use the new list directly
                currentTemplates = new ArrayList<>(responseTemplates);
                Log.d(TAG, "Setting new template list with " + currentTemplates.size() + " items (force refresh or first page)");
            } else {
                // For pagination, add new templates but avoid duplicates
                int prevSize = currentTemplates.size();
                Set<String> existingIds = new HashSet<>();
                
                // Build a set of existing IDs
                for (Template existing : currentTemplates) {
                    existingIds.add(existing.getId());
                }
                
                // Add only non-duplicate templates
                for (Template template : responseTemplates) {
                    if (!existingIds.contains(template.getId())) {
                        currentTemplates.add(template);
                        existingIds.add(template.getId());
                        Log.d(TAG, "Added new template: " + template.getId());
                    } else {
                        Log.d(TAG, "Skipped duplicate template: " + template.getId());
                    }
                }
                
                Log.d(TAG, "Added " + (currentTemplates.size() - prevSize) + " templates to existing list, new size: " + 
                      currentTemplates.size() + " (previous: " + prevSize + ")");
            }
            
            // Log all template IDs for debugging
            for (Template template : responseTemplates) {
                Log.d(TAG, "Template: " + template.getTitle() + 
                          ", ID: " + template.getId() + 
                          ", Category: " + template.getCategory() +
                          ", Created: " + template.getCreatedAt());
            }
        }
        
        // Debug log for category filter check
        if (currentCategory != null && !currentTemplates.isEmpty()) {
            boolean allMatch = true;
            for (Template template : currentTemplates) {
                String templateCategory = template.getCategory();
                String normalizedTemplateCategory = templateCategory != null ? templateCategory.toLowerCase().trim() : "";
                String normalizedCurrentCategory = currentCategory.toLowerCase().trim();
                
                if (templateCategory == null || normalizedTemplateCategory.isEmpty() || 
                    !normalizedCurrentCategory.equals(normalizedTemplateCategory)) {
                    allMatch = false;
                    Log.w(TAG, "Template category mismatch: expected '" + currentCategory + 
                              "', got '" + templateCategory + "' for template " + template.getId() + 
                              " (normalized: '" + normalizedCurrentCategory + "' vs '" + normalizedTemplateCategory + "')");
                }
            }
            if (allMatch) {
                Log.d(TAG, "All templates match the current category filter: '" + currentCategory + "'");
            } else {
                Log.w(TAG, "Some templates do not match the current category filter: '" + currentCategory + "'");
            }
        }
        
        templates.setValue(currentTemplates);
        
        // Process categories
        Map<String, Integer> categoryMap = response.getCategories();
        if (categoryMap != null && !categoryMap.isEmpty()) {
            Log.d(TAG, "Categories received: " + categoryMap.size());
            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                Log.d(TAG, "Category: " + entry.getKey() + ", Count: " + entry.getValue());
            }
            categories.setValue(categoryMap);
        } else {
            Log.d(TAG, "Categories map is null or empty");
            // Try to get categories from ResourceRepository or fallback options
            loadCategoriesFromCache();
        }
        
        hasMorePages = response.isHasMore();
        currentPage++;
    }
    
    /**
     * Load categories from cache or create mock ones if needed
     * Extracted to separate method to avoid duplicating code
     */
    private void loadCategoriesFromCache() {
        // Try to get categories from ResourceRepository
        ResourceRepository resourceRepository = ResourceRepository.getInstance(context);
        resourceRepository.getResource(
            ResourceRepository.RESOURCE_TYPE_CATEGORY,
            CACHE_KEY_CATEGORIES,
            false
        ).observeForever(resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                try {
                    Type type = new TypeToken<Map<String, Integer>>(){}.getType();
                    Map<String, Integer> cachedCategories = gson.fromJson(resource.getData(), type);
                    if (cachedCategories != null && !cachedCategories.isEmpty()) {
                        Log.d(TAG, "Using cached categories: " + cachedCategories.size());
                        categories.setValue(cachedCategories);
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing cached categories", e);
                }
            }
            
            // Add mock categories if none are returned from API or cache
            Map<String, Integer> mockCategories = new HashMap<>();
            mockCategories.put("Birthday", 10);
            mockCategories.put("Wedding", 8);
            mockCategories.put("Anniversary", 6);
            mockCategories.put("Graduation", 5);
            mockCategories.put("Holiday", 7);
            mockCategories.put("Congratulations", 4);
            Log.d(TAG, "Added mock categories: " + mockCategories.size());
            categories.setValue(mockCategories);
        });
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
        
        // Clear cache using ResourceRepository
        resourceRepository.clearCache(ResourceType.TEMPLATE);
        
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
        
        // Validate templateId
        if (templateId == null || templateId.trim().isEmpty()) {
            Log.e(TAG, "getTemplateById called with null or empty templateId");
            result.setValue(Resource.error("Template ID cannot be null or empty", null));
            return result;
        }
        
        Log.d(TAG, "Loading template by ID: " + templateId + ", forceRefresh: " + forceRefresh);
        
        // Use ResourceRepository to load the template
        LiveData<Resource<JsonObject>> source = resourceRepository.loadResource(
                ResourceType.TEMPLATE,
                templateId,
                forceRefresh);
        
        result.addSource(source, resource -> {
            if (resource.isSuccess()) {
                JsonObject data = resource.getData();
                if (data != null) {
                    try {
                        Template template = gson.fromJson(data, Template.class);
                        if (template != null) {
                            Log.d(TAG, "Successfully deserialized template: " + template.getId() + " - " + template.getTitle());
                            result.setValue(Resource.success(template, resource.isStale()));
                        } else {
                            Log.e(TAG, "Failed to deserialize template: null result after deserialization for ID: " + templateId);
                            result.setValue(Resource.error("Failed to process template data", null));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deserializing template: " + e.getMessage() + " for template ID: " + templateId, e);
                        
                        // Try to extract template data manually from the JsonObject as a fallback
                        try {
                            Template manualTemplate = new Template();
                            if (data.has("_id")) manualTemplate.setId(data.get("_id").getAsString());
                            if (data.has("title")) manualTemplate.setTitle(data.get("title").getAsString());
                            if (data.has("category")) manualTemplate.setCategory(data.get("category").getAsString());
                            
                            // Set other basic fields if available
                            if (data.has("htmlContent")) manualTemplate.setHtmlContent(data.get("htmlContent").getAsString());
                            if (data.has("cssContent")) manualTemplate.setCssContent(data.get("cssContent").getAsString());
                            if (data.has("jsContent")) manualTemplate.setJsContent(data.get("jsContent").getAsString());
                            if (data.has("previewUrl")) manualTemplate.setPreviewUrl(data.get("previewUrl").getAsString());
                            if (data.has("thumbnailUrl")) manualTemplate.setThumbnailUrl(data.get("thumbnailUrl").getAsString());

                            Log.d(TAG, "Manually extracted basic template data for ID: " + manualTemplate.getId());
                            result.setValue(Resource.success(manualTemplate, true)); // Mark as stale since it's partial data
                        } catch (Exception ex) {
                            Log.e(TAG, "Failed manual extraction fallback: " + ex.getMessage(), ex);
                            result.setValue(Resource.error("Failed to process template data: " + e.getMessage(), null));
                        }
                    }
                } else {
                    Log.e(TAG, "Template data is null for ID: " + templateId);
                    result.setValue(Resource.error("Template data is null", null));
                }
            } else if (resource.isError()) {
                Log.e(TAG, "Error loading template by ID " + templateId + ": " + resource.getMessage());
                result.setValue(Resource.error(resource.getMessage(), null));
            } else {
                // Still loading
                result.setValue(Resource.loading(null));
            }
        });
        
        return result;
    }
    
    /**
     * Clear templates and reset pagination
     */
    public void clearTemplates() {
        Log.d(TAG, "Clearing templates list");
        
        // Clear templates list
        templates.setValue(new ArrayList<>());
        
        // Reset pagination
        currentPage = 1;
        hasMorePages = true;
        
        // Cancel any ongoing calls
        cancelCurrentCall();
        
        // Clear error state
        error.setValue(null);
        
        Log.d(TAG, "Templates cleared, pagination reset, and errors cleared");
    }
    
    private String getCacheKey() {
        if (currentCategory != null) {
            return CACHE_KEY_TEMPLATES_BY_CATEGORY + currentCategory + "_page_" + currentPage;
        } else {
            return CACHE_KEY_TEMPLATES + "_page_" + currentPage;
        }
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        error.postValue(null);
    }
    
    /**
     * Set the loading state manually
     * @param isLoading Whether the repository is loading
     */
    public void setLoading(boolean isLoading) {
        loading.setValue(isLoading);
    }
    
    /**
     * Set an error message
     * @param errorMessage The error message to set
     */
    public void setErrorMessage(String errorMessage) {
        error.postValue(errorMessage);
    }
    
    /**
     * Normalize a category string for consistent comparison
     * @param category Category string to normalize
     * @return Normalized category string (lowercase, trimmed) or empty string if null
     */
    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toLowerCase();
    }
    
    /**
     * Get the current templates list synchronously
     * @return Current list of templates or empty list if none available
     */
    public List<Template> getTemplatesSync() {
        List<Template> currentTemplates = templates.getValue();
        if (currentTemplates == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(currentTemplates); // Return a copy to prevent modification
    }
}
