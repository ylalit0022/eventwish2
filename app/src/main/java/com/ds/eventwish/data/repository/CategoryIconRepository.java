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
    
    // Cache expiration times
    private static final long CACHE_EXPIRATION_ICONS = TimeUnit.DAYS.toMillis(7); // 7 days
    
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

    /**
     * Get the singleton instance of CategoryIconRepository
     * @return CategoryIconRepository instance
     */
    public static synchronized CategoryIconRepository getInstance() {
        if (instance == null) {
            Context appContext = com.ds.eventwish.EventWishApplication.getInstance();
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
        this.apiService = ApiClient.getInstance().getApiService();
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
            Log.d(TAG, "Found icon for category: " + category);
            return categoryIconMap.get(lowerCategory);
        }
        
        // If not found and we haven't loaded icons yet, load them
        if (!isInitialized) {
            Log.d(TAG, "Icons not initialized yet, loading category icons");
            loadCategoryIcons();
            
            // Check again after loading
            if (categoryIconMap.containsKey(lowerCategory)) {
                Log.d(TAG, "Found icon for category after loading: " + category);
                return categoryIconMap.get(lowerCategory);
            }
        }
        
        // If still not found, add a generic fallback for this specific category
        Log.d(TAG, "No icon found for category: " + category + ", adding generic fallback");
        String fallbackUrl = "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/category/materialicons/24dp/2x/baseline_category_black_24dp.png";
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
        if (loading.getValue()) {
            return;
        }
        
        loading.setValue(true);
        Log.d(TAG, "Loading category icons with offline-first approach");
        
        try {
            // Use ResourceRepository to load the resource
            LiveData<Resource<JsonObject>> resourceLiveData = resourceRepository.getResource(
                ResourceRepository.RESOURCE_TYPE_CATEGORY_ICON,
                CACHE_KEY_CATEGORY_ICONS,
                !isInitialized // Force refresh if not initialized
            );
            
            // Observe the resource
            MediatorLiveData<Resource<JsonObject>> mediator = new MediatorLiveData<>();
            mediator.addSource(resourceLiveData, resource -> {
                if (resource.isSuccess()) {
                    // Process the successful response
                    JsonObject data = resource.getData();
                    if (data != null) {
                        try {
                            Type listType = new TypeToken<List<CategoryIcon>>(){}.getType();
                            List<CategoryIcon> icons = gson.fromJson(data.getAsJsonArray("data"), listType);
                            processCategoryIcons(icons);
                            
                            // If data is stale, show a message
                            if (resource.isStale()) {
                                error.setValue("You're offline. Showing cached icons that may be outdated.");
                            } else {
                                error.setValue(null);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing category icons response", e);
                            error.setValue("Error parsing category icons data");
                            // Add fallback icons since parsing failed
                            addFallbackIcons();
                        }
                    } else {
                        // Add fallback icons since data is null
                        addFallbackIcons();
                    }
                    loading.setValue(false);
                } else if (resource.isError()) {
                    // Handle error
                    error.setValue(resource.getMessage());
                    loading.setValue(false);
                    // Add fallback icons since there was an error
                    addFallbackIcons();
                } else {
                    // Still loading, wait for result
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading category icons: " + e.getMessage());
            loading.setValue(false);
            // Continue with app initialization even if category icons fail to load
            isInitialized = true;
        }
        
        // Also load from network to ensure we have the latest data
        loadCategoryIconsFromNetwork();
    }
    
    /**
     * Load category icons from network
     */
    private void loadCategoryIconsFromNetwork() {
        Log.d(TAG, "Loading category icons from network");
        
        Call<CategoryIconResponse> call = apiService.getCategoryIcons();
        setCurrentCall(call);
        call.enqueue(new Callback<CategoryIconResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryIconResponse> call, @NonNull Response<CategoryIconResponse> response) {
                loading.setValue(false);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        CategoryIconResponse iconResponse = response.body();
                        List<CategoryIcon> icons = iconResponse.getData();
                        
                        if (icons == null || icons.isEmpty()) {
                            Log.w(TAG, "Received empty category icons list or null");
                            // Add fallback icons since we got an empty response
                            addFallbackIcons();
                            return;
                        }
                        
                        // Cache the response using ResourceRepository
                        JsonObject jsonObject = gson.toJsonTree(iconResponse).getAsJsonObject();
                        
                        // Save resource
                        resourceRepository.saveResource(
                            ResourceRepository.RESOURCE_TYPE_CATEGORY_ICON,
                            CACHE_KEY_CATEGORY_ICONS,
                            jsonObject,
                            null,
                            null,
                            new java.util.Date(System.currentTimeMillis() + CACHE_EXPIRATION_ICONS)
                        );
                        
                        // Process the icons
                        processCategoryIcons(icons);
                        
                    } else {
                        String errorMsg = "Failed to load category icons";
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
                                ErrorHandler.ErrorSeverity.LOW);
                        
                        // Add fallback icons since API call failed
                        addFallbackIcons();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing category icons response: " + e.getMessage(), e);
                    error.setValue("Error parsing category icons: " + e.getMessage());
                    errorHandler.handleError(
                            ErrorHandler.ErrorType.PARSING_ERROR,
                            "Error parsing category icons: " + e.getMessage(),
                            ErrorHandler.ErrorSeverity.LOW);
                    
                    // Add fallback icons since parsing failed
                    addFallbackIcons();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoryIconResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Call was canceled");
                    loading.setValue(false);
                    return;
                }
                
                loading.setValue(false);
                
                String errorMsg = NetworkErrorHandler.getErrorMessage(context, t);
                error.setValue(errorMsg);
                NetworkErrorHandler.handleNetworkError(context, t, errorHandler);
                
                // Add fallback icons since API call failed
                addFallbackIcons();
            }
        });
    }
    
    /**
     * Process category icons and update LiveData
     * @param icons List of category icons to process
     */
    private void processCategoryIcons(List<CategoryIcon> icons) {
        if (icons == null || icons.isEmpty()) {
            Log.w(TAG, "Received empty category icons list or null");
            return;
        }
        
        categoryIcons.setValue(icons);
        
        // Update our map for quick lookup
        categoryIconMap.clear();
        for (CategoryIcon icon : icons) {
            if (icon != null && icon.getCategory() != null) {
                categoryIconMap.put(icon.getCategory().toLowerCase(), icon);
                Log.d(TAG, "Added category icon: " + icon.getCategory() + " -> " + icon.getCategoryIcon());
            }
        }
        
        // Add fallback icons for common categories
        addFallbackIcons();
        
        isInitialized = true;
        Log.d(TAG, "Successfully loaded " + icons.size() + " category icons");
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