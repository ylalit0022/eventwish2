package com.ds.eventwish.data.repository;

import android.content.Context;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            Context appContext = com.ds.eventwish.EventWishApplication.getInstance();
            instance = new TemplateRepository(appContext);
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private TemplateRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resourceRepository = ResourceRepository.getInstance(context);
        this.apiService = ApiClient.getInstance().getApiService();
        this.executors = AppExecutors.getInstance();
        this.networkUtils = NetworkUtils.getInstance(context);
        this.gson = new Gson();
        this.errorHandler = ErrorHandler.getInstance(context);
        
        // Initialize templates list
        templates.setValue(new ArrayList<>());
        
        Log.d(TAG, "TemplateRepository initialized with enhanced caching and offline support");
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
        
        // Use ResourceRepository to load the resource
        String resourceKey = currentCategory != null 
            ? CACHE_KEY_TEMPLATES_BY_CATEGORY + currentCategory + "_page_" + currentPage
            : CACHE_KEY_TEMPLATES + "_page_" + currentPage;
            
        // Create a resource request
        LiveData<Resource<JsonObject>> resourceLiveData = resourceRepository.getResource(
            ResourceRepository.RESOURCE_TYPE_TEMPLATE,
            resourceKey,
            forceRefresh
        );
        
        // Observe the resource
        MediatorLiveData<Resource<JsonObject>> mediator = new MediatorLiveData<>();
        mediator.addSource(resourceLiveData, resource -> {
            if (resource.isSuccess()) {
                // Process the successful response
                JsonObject data = resource.getData();
                if (data != null) {
                    try {
                        TemplateResponse templateResponse = gson.fromJson(data, TemplateResponse.class);
                        processTemplateResponse(templateResponse, forceRefresh);
                        
                        // If data is stale, show a message
                        if (resource.isStale()) {
                            error.setValue("You're offline. Showing cached data that may be outdated.");
                        } else {
                            error.setValue(null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing template response", e);
                        error.setValue("Error parsing template data");
                    }
                }
                loading.setValue(false);
            } else if (resource.isError()) {
                // Handle error
                error.setValue(resource.getMessage());
                loading.setValue(false);
            }
            // Loading state is handled by the resource
        });
        
        // If we're not using ResourceRepository directly, fall back to the old implementation
        if (currentCategory != null) {
            loadTemplatesFromNetwork(forceRefresh);
        } else {
            loadTemplatesFromNetwork(forceRefresh);
        }
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
                loading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    
                    // Cache the response using ResourceRepository
                    String resourceKey = currentCategory != null 
                        ? CACHE_KEY_TEMPLATES_BY_CATEGORY + currentCategory + "_page_" + currentPage
                        : CACHE_KEY_TEMPLATES + "_page_" + currentPage;
                        
                    JsonObject jsonObject = gson.toJsonTree(templateResponse).getAsJsonObject();
                    
                    // Save resource
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("page", String.valueOf(currentPage));
                    if (currentCategory != null) {
                        metadata.put("category", currentCategory);
                    }
                    
                    resourceRepository.saveResource(
                        ResourceRepository.RESOURCE_TYPE_TEMPLATE,
                        resourceKey,
                        jsonObject,
                        metadata,
                        null,
                        new java.util.Date(System.currentTimeMillis() + CACHE_EXPIRATION_TEMPLATES)
                    );
                    
                    // Also cache categories separately
                    if (templateResponse.getCategories() != null && !templateResponse.getCategories().isEmpty()) {
                        JsonObject categoriesJson = gson.toJsonTree(templateResponse.getCategories()).getAsJsonObject();
                        resourceRepository.saveResource(
                            ResourceRepository.RESOURCE_TYPE_CATEGORY,
                            CACHE_KEY_CATEGORIES,
                            categoriesJson,
                            null,
                            null,
                            new java.util.Date(System.currentTimeMillis() + CACHE_EXPIRATION_CATEGORIES)
                        );
                    }
                    
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
                    
                    error.setValue(errorMsg);
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
                    loading.setValue(false);
                    return;
                }
                
                loading.setValue(false);
                
                // Try to get from cache using ResourceRepository
                String resourceKey = currentCategory != null 
                    ? CACHE_KEY_TEMPLATES_BY_CATEGORY + currentCategory + "_page_" + currentPage
                    : CACHE_KEY_TEMPLATES + "_page_" + currentPage;
                    
                LiveData<Resource<JsonObject>> resourceLiveData = resourceRepository.getResource(
                    ResourceRepository.RESOURCE_TYPE_TEMPLATE,
                    resourceKey,
                    false
                );
                
                MediatorLiveData<Resource<JsonObject>> mediator = new MediatorLiveData<>();
                mediator.addSource(resourceLiveData, resource -> {
                    if (resource.isSuccess() && resource.getData() != null) {
                        try {
                            TemplateResponse templateResponse = gson.fromJson(resource.getData(), TemplateResponse.class);
                            processTemplateResponse(templateResponse, false);
                            error.setValue("Network error. Showing cached data.");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing cached template response", e);
                            String errorMsg = NetworkErrorHandler.getErrorMessage(context, t);
                            error.setValue(errorMsg);
                            NetworkErrorHandler.handleNetworkError(context, t, errorHandler);
                        }
                    } else {
                        String errorMsg = NetworkErrorHandler.getErrorMessage(context, t);
                        error.setValue(errorMsg);
                        NetworkErrorHandler.handleNetworkError(context, t, errorHandler);
                    }
                });
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
        
        templates.setValue(currentList);
        
        // Process categories
        Map<String, Integer> categoryMap = templateResponse.getCategories();
        if (categoryMap != null && !categoryMap.isEmpty()) {
            Log.d(TAG, "Categories received: " + categoryMap.size());
            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                Log.d(TAG, "Category: " + entry.getKey() + ", Count: " + entry.getValue());
            }
            categories.setValue(categoryMap);
        } else {
            Log.d(TAG, "Categories map is null or empty");
            // Try to get categories from ResourceRepository
            LiveData<Resource<JsonObject>> categoriesResource = resourceRepository.getResource(
                ResourceRepository.RESOURCE_TYPE_CATEGORY,
                CACHE_KEY_CATEGORIES,
                false
            );
            
            MediatorLiveData<Resource<JsonObject>> mediator = new MediatorLiveData<>();
            mediator.addSource(categoriesResource, resource -> {
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
        
        hasMorePages = templateResponse.isHasMore();
        currentPage++;
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
