package com.ds.eventwish.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class TemplateRepository {
    private static final String TAG = "TemplateRepository";
    private static TemplateRepository instance;
    private final ApiService apiService;
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private String currentCategory = null;
    private static final int PAGE_SIZE = 20;
    private Call<TemplateResponse> currentCall;
    private Context appContext;
    
    // Add constants for SharedPreferences
    private static final String PREF_NAME = "template_repository_prefs";
    private static final String PREF_CATEGORIES = "saved_categories";

    private Call<Template> currentTemplateCall;

    // Default categories with counts
    private final Map<String, Integer> defaultCategories = new HashMap<String, Integer>() {{
        put("Birthday", 10);
        put("Wedding", 8);
        put("Anniversary", 6);
        put("Holiday", 7);
        put("Party", 9);
    }};

    /**
     * Callback interface for category operations
     */
    public interface CategoriesCallback {
        void onSuccess(Map<String, Integer> categoryMap);
        void onError(String message);
    }

    private TemplateRepository() {
        apiService = ApiClient.getClient();
        templates.postValue(new ArrayList<>());
        categories.postValue(new HashMap<>());
        // Initialize with default categories
        ensureDefaultCategories(null);
    }

    /**
     * Initialize the repository with a context
     * @param context Application context
     * @return The repository instance
     */
    public static synchronized TemplateRepository init(Context context) {
        if (instance == null) {
            instance = new TemplateRepository();
            instance.appContext = context.getApplicationContext();
            instance.loadCategoriesFromPrefs();
        } else if (instance.appContext == null) {
            instance.appContext = context.getApplicationContext();
            instance.loadCategoriesFromPrefs();
        }
        return instance;
    }

    public static synchronized TemplateRepository getInstance() {
        if (instance == null) {
            instance = new TemplateRepository();
        }
        return instance;
    }

    /**
     * Load categories from SharedPreferences on initialization
     */
    private void loadCategoriesFromPrefs() {
        if (appContext == null) {
            Log.e(TAG, "Cannot load categories from prefs: app context is null");
            return;
        }
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String categoriesJson = prefs.getString(PREF_CATEGORIES, null);
            
            if (categoriesJson != null && !categoriesJson.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Integer>>(){}.getType();
                Map<String, Integer> savedCategories = gson.fromJson(categoriesJson, type);
                
                if (savedCategories != null && !savedCategories.isEmpty()) {
                    Log.d(TAG, "Loaded " + savedCategories.size() + " categories from SharedPreferences");
                    categories.postValue(savedCategories);
                    
                    // Remove the call to notifyCategoriesObservers to prevent infinite recursion
                    // notifyCategoriesObservers();
                } else {
                    Log.d(TAG, "No saved categories found in SharedPreferences");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading categories from SharedPreferences", e);
        }
    }
    
    /**
     * Save categories to SharedPreferences for persistence
     */
    private void saveCategoriesToPrefs(Map<String, Integer> categoriesToSave) {
        if (appContext == null || categoriesToSave == null) {
            return;
        }
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            Gson gson = new Gson();
            String categoriesJson = gson.toJson(categoriesToSave);
            
            editor.putString(PREF_CATEGORIES, categoriesJson);
            editor.apply();
            
            Log.d(TAG, "Saved " + categoriesToSave.size() + " categories to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving categories to SharedPreferences", e);
        }
    }

    /**
     * Ensure we have at least default categories available
     */
    private void ensureDefaultCategories(Map<String, Integer> categoryMap) {
        if (categoryMap == null) {
            categoryMap = new HashMap<>();
        }
        
        // If we received categories from server, use them
        if (!categoryMap.isEmpty()) {
            android.util.Log.d("TemplateRepository", "Using server categories: " + categoryMap.size());
            categories.postValue(categoryMap);
            return;
        }
        
        // Only use defaults if we have no categories at all
        Map<String, Integer> currentCategories = categories.getValue();
        if (currentCategories == null || currentCategories.isEmpty()) {
            categoryMap.putAll(defaultCategories);
            android.util.Log.d("TemplateRepository", "Using default categories as fallback: " + defaultCategories.size());
            categories.postValue(categoryMap);
        }
    }

    public LiveData<List<Template>> getTemplates() {
        return templates;
    }

    /**
     * Get templates synchronously without LiveData
     * @return A list of templates or empty list if none are loaded
     */
    public List<Template> getTemplatesSync() {
        List<Template> templateList = templates.getValue();
        return templateList != null ? new ArrayList<>(templateList) : new ArrayList<>();
    }

    public LiveData<Map<String, Integer>> getCategories() {
        return categories;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * Clear any current error state
     * This is useful when navigating back to a fragment where errors should be reset
     */
    public void clearError() {
        Log.d(TAG, "Clearing error state");
        error.postValue(null);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public boolean isLoading() {
        return loading.getValue() != null && loading.getValue();
    }

    public boolean hasMorePages() {
        return hasMorePages;
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled() && !currentCall.isExecuted()) {
            // Only cancel calls that haven't been executed yet or are still in progress
            Log.d(TAG, "Cancelling API call for templates");
            currentCall.cancel();
        }
    }

    private void setCurrentCall(Call<TemplateResponse> call) {
        // Cancel any existing call before setting a new one
        cancelCurrentCall();
        
        // Store the new call
        currentCall = call;
        
        // Debug logging
        Log.d(TAG, "Setting new API call for templates with category: " + 
            (currentCategory != null ? currentCategory : "All") + 
            ", page: " + currentPage);
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
        
        // Save current categories to ensure they're maintained
        Map<String, Integer> currentCategories = categories.getValue();
        
        currentCategory = category;
        currentPage = 1;
        hasMorePages = true;
        
        if (reload) {
            // Don't clear templates immediately to prevent UI flicker
            // templates.setValue(new ArrayList<>());
            
            loadTemplates(true);
            
            // After category change, make sure to notify observers of existing categories
            if (currentCategories != null && !currentCategories.isEmpty()) {
                Log.d(TAG, "Maintaining " + currentCategories.size() + " categories after category change");
                categories.postValue(new HashMap<>(currentCategories));
            }
        } else {
            // Even when not reloading, notify observers to maintain UI state
            notifyCategoriesObservers();
        }
    }

    public void loadTemplates(boolean forceRefresh) {
        if (loading.getValue()) {
            return;
        }
        
        loading.postValue(true);
        
        // If this is a forced refresh, reset pagination
        if (forceRefresh) {
            currentPage = 1;
            hasMorePages = true;
            
            // Only clear templates if we're not filtering by category or if this is the initial load
            if (currentCategory == null || templates.getValue() == null || templates.getValue().isEmpty()) {
                if (templates.getValue() != null) {
                    templates.getValue().clear();
                    // Notify observers of the empty list to clear the UI
                    templates.postValue(new ArrayList<>());
                } else {
                    templates.postValue(new ArrayList<>());
                }
            }
        }
        
        // If we've already loaded all pages, don't make another request
        if (!hasMorePages && currentPage > 1) {
            loading.postValue(false);
            return;
        }
        
        // Log request details
        Log.d(TAG, "Loading templates - page: " + currentPage + 
                  ", category: " + (currentCategory != null ? currentCategory : "All") + 
                  ", forceRefresh: " + forceRefresh);

        Call<TemplateResponse> call;
        if (currentCategory != null) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        setCurrentCall(call);
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                // Clear the current call reference if it matches this call
                if (currentCall != null && currentCall.equals(call)) {
                    currentCall = null;
                }
                
                // Skip processing if call was cancelled
                if (call.isCanceled()) {
                    Log.d(TAG, "Skipping response handling for cancelled call");
                    loading.postValue(false);
                    return;
                }
                
                loading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentList = templates.getValue();
                    if (currentList == null) currentList = new ArrayList<>();
                    
                    if (forceRefresh) {
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
                    
                    // Handle categories with persistence
                    Map<String, Integer> categoryMap = templateResponse.getCategories();
                    if (categoryMap == null) {
                        Log.d(TAG, "Categories map is null, using empty map");
                        categoryMap = new HashMap<>();
                    } else if (categoryMap.isEmpty()) {
                        Log.d(TAG, "Categories map is empty from server");
                    } else {
                        Log.d(TAG, "Categories received from server: " + categoryMap.size());
                        for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                            Log.d(TAG, "Category: " + entry.getKey() + ", Count: " + entry.getValue());
                        }
                        
                        // Save categories to SharedPreferences for persistence
                        saveCategoriesToPrefs(categoryMap);
                    }
                    
                    categories.postValue(categoryMap);
                    hasMorePages = templateResponse.isHasMore();
                    currentPage++;
                } else {
                    error.postValue("Failed to load templates");
                    // Only use default categories if we have none
                    if (categories.getValue() == null || categories.getValue().isEmpty()) {
                        ensureDefaultCategories(null);
                    }
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                // Clear the current call reference if it matches this call
                if (currentCall != null && currentCall.equals(call)) {
                    currentCall = null;
                }
                
                // Check if the call was cancelled
                if (call.isCanceled()) {
                    Log.d(TAG, "API call was cancelled - ignoring failure response");
                } else {
                    Log.e(TAG, "API call failed: " + t.getMessage(), t);
                    loading.postValue(false);
                    error.postValue(t.getMessage());
                    // Only use default categories if we have none
                    if (categories.getValue() == null || categories.getValue().isEmpty()) {
                        ensureDefaultCategories(null);
                    }
                }
            }
        });
    }

    /**
     * Set the current page for pagination
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
    
    /**
     * Get the current page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Clear the template cache and reset pagination
     */
    public void clearCache() {
        Log.d(TAG, "Clearing template cache");
        
        // Save current categories
        Map<String, Integer> currentCategories = categories.getValue();
        
        // Reset pagination
        currentPage = 1;
        hasMorePages = true;
        
        // Clear templates
        if (templates.getValue() != null) {
            templates.getValue().clear();
            templates.postValue(new ArrayList<>());
        } else {
            templates.postValue(new ArrayList<>());
        }
        
        // Cancel any ongoing requests
        cancelCurrentCall();
        
        // Reset error state
        error.postValue(null);
        
        // Reset loading state
        loading.postValue(false);
        
        // Restore categories with defaults if needed
        ensureDefaultCategories(currentCategories);
    }

    /**
     * Cancel any ongoing template detail request
     */
    public void cancelTemplateCall() {
        if (currentTemplateCall != null && !currentTemplateCall.isCanceled() && !currentTemplateCall.isExecuted()) {
            Log.d(TAG, "Cancelling template detail API call");
            currentTemplateCall.cancel();
        }
    }

    /**
     * Get a template by ID
     * @param templateId The template ID to fetch
     * @param forceRefresh Whether to force a refresh from network
     * @return LiveData with the template resource
     */
    public LiveData<Template> getTemplateById(String templateId, boolean forceRefresh) {
        MutableLiveData<Template> result = new MutableLiveData<>();
        
        // First check if we have it cached locally
        if (!forceRefresh && templates.getValue() != null) {
            for (Template template : templates.getValue()) {
                if (template.getId().equals(templateId)) {
                    result.postValue(template);
                    return result;
                }
            }
        }
        
        // Cancel any ongoing template detail request
        cancelTemplateCall();
        
        // Fetch from network
        Call<Template> call = apiService.getTemplateById(templateId);
        currentTemplateCall = call;
        
        call.enqueue(new Callback<Template>() {
            @Override
            public void onResponse(Call<Template> call, Response<Template> response) {
                // Clear the current template call reference if it matches this call
                if (currentTemplateCall != null && currentTemplateCall.equals(call)) {
                    currentTemplateCall = null;
                }
                
                // Skip processing if call was cancelled
                if (call.isCanceled()) {
                    Log.d(TAG, "Skipping template response handling for cancelled call");
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    Log.e(TAG, "Failed to get template by ID: " + templateId + ", response code: " + response.code());
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<Template> call, Throwable t) {
                // Clear the current template call reference if it matches this call
                if (currentTemplateCall != null && currentTemplateCall.equals(call)) {
                    currentTemplateCall = null;
                }
                
                // Check if the call was cancelled
                if (call.isCanceled()) {
                    Log.d(TAG, "Template API call was cancelled - ignoring failure response");
                } else {
                    Log.e(TAG, "Template API call failed: " + t.getMessage(), t);
                    result.postValue(null);
                }
            }
        });
        
        return result;
    }

    /**
     * Notify observers of existing categories without making a new request
     */
    public void notifyCategoriesObservers() {
        Map<String, Integer> currentCategories = categories.getValue();
        if (currentCategories != null && !currentCategories.isEmpty()) {
            Log.d(TAG, "Notifying observers of " + currentCategories.size() + " existing categories");
            // Use postValue to trigger observer updates with existing data
            categories.postValue(new HashMap<>(currentCategories));
        } else {
            // Add a guard to prevent infinite recursion
            SharedPreferences prefs = appContext != null ? 
                appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) : null;
            
            if (prefs != null && prefs.contains(PREF_CATEGORIES)) {
                // Try to load categories from SharedPreferences if no categories are available
                Log.d(TAG, "No categories available, attempting to load from SharedPreferences");
                loadCategoriesFromPrefs();
            } else {
                Log.d(TAG, "No categories available in memory or SharedPreferences");
                // Ensure we have at least default categories
                ensureDefaultCategories(null);
            }
        }
    }

    /**
     * Get categories with callback
     * @param callback Callback to receive the categories
     */
    public void getCategories(CategoriesCallback callback) {
        // Create headers map
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        // Make API call to get categories
        Call<List<JsonObject>> call = apiService.getCategories(headers);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Convert JsonObject list to Map<String, Integer>
                    Map<String, Integer> categoryMap = new HashMap<>();
                    for (JsonObject category : response.body()) {
                        String name = category.get("name").getAsString();
                        int count = category.get("count").getAsInt();
                        categoryMap.put(name, count);
                    }
                    
                    // Update categories immediately
                    categories.postValue(categoryMap);
                    callback.onSuccess(categoryMap);
                } else {
                    // Only use defaults if we have no categories
                    if (categories.getValue() == null || categories.getValue().isEmpty()) {
                        Map<String, Integer> defaultCats = new HashMap<>(defaultCategories);
                        categories.postValue(defaultCats);
                        callback.onSuccess(defaultCats);
                    } else {
                        callback.onError("Failed to load categories");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                // Only use defaults if we have no categories
                if (categories.getValue() == null || categories.getValue().isEmpty()) {
                    Map<String, Integer> defaultCats = new HashMap<>(defaultCategories);
                    categories.postValue(defaultCats);
                    callback.onSuccess(defaultCats);
                } else {
                    callback.onError(t.getMessage());
                }
            }
        });
    }
}
