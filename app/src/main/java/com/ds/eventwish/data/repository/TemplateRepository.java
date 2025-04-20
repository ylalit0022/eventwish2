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
import com.google.gson.JsonObject;

public class TemplateRepository {
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
        templates.setValue(new ArrayList<>());
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
     * Ensure we have at least default categories available
     */
    private void ensureDefaultCategories(Map<String, Integer> categoryMap) {
        if (categoryMap == null) {
            categoryMap = new HashMap<>();
        }
        
        // If we received categories from server, use them
        if (!categoryMap.isEmpty()) {
            android.util.Log.d("TemplateRepository", "Using server categories: " + categoryMap.size());
            categories.setValue(categoryMap);
            return;
        }
        
        // Only use defaults if we have no categories at all
        Map<String, Integer> currentCategories = categories.getValue();
        if (currentCategories == null || currentCategories.isEmpty()) {
            categoryMap.putAll(defaultCategories);
            android.util.Log.d("TemplateRepository", "Using default categories as fallback: " + defaultCategories.size());
            categories.setValue(categoryMap);
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
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

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
        
        android.util.Log.d("TemplateRepository", "Changing category from " + 
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
                templates.setValue(new ArrayList<>());
            }
        }
        
        // If we've already loaded all pages, don't make another request
        if (!hasMorePages && currentPage > 1) {
            loading.setValue(false);
            return;
        }
        
        // Log request details
        android.util.Log.d("TemplateRepository", "Loading templates - page: " + currentPage + 
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
                loading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentList = templates.getValue();
                    if (currentList == null) currentList = new ArrayList<>();
                    
                    if (forceRefresh) {
                        currentList = new ArrayList<>(templateResponse.getTemplates());
                    } else {
                        currentList.addAll(templateResponse.getTemplates());
                    }
                    
                    templates.setValue(currentList);
                    
                    // Handle categories with persistence
                    Map<String, Integer> categoryMap = templateResponse.getCategories();
                    if (categoryMap != null && !categoryMap.isEmpty()) {
                        android.util.Log.d("TemplateRepository", "Received " + categoryMap.size() + " categories from server");
                        categories.setValue(categoryMap);
                    }
                    
                    hasMorePages = templateResponse.isHasMore();
                    currentPage++;
                } else {
                    error.setValue("Failed to load templates");
                    // Only use default categories if we have none
                    if (categories.getValue() == null || categories.getValue().isEmpty()) {
                        ensureDefaultCategories(null);
                    }
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
                // Only use default categories if we have none
                if (categories.getValue() == null || categories.getValue().isEmpty()) {
                    ensureDefaultCategories(null);
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
        android.util.Log.d("TemplateRepository", "Clearing template cache");
        
        // Save current categories
        Map<String, Integer> currentCategories = categories.getValue();
        
        // Reset pagination
        currentPage = 1;
        hasMorePages = true;
        
        // Clear templates
        templates.setValue(new ArrayList<>());
        
        // Cancel any ongoing requests
        cancelCurrentCall();
        
        // Reset error state
        error.setValue(null);
        
        // Reset loading state
        loading.setValue(false);
        
        // Restore categories with defaults if needed
        ensureDefaultCategories(currentCategories);
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
                    result.setValue(template);
                    return result;
                }
            }
        }
        
        // Fetch from network
        apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
            @Override
            public void onResponse(Call<Template> call, Response<Template> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Template> call, Throwable t) {
                result.setValue(null);
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
            android.util.Log.d("TemplateRepository", "Notifying observers of " + currentCategories.size() + " existing categories");
            // Use setValue to trigger observer updates with existing data
            categories.setValue(new HashMap<>(currentCategories));
        } else {
            android.util.Log.d("TemplateRepository", "No categories available to notify observers");
            // Ensure we have at least default categories
            ensureDefaultCategories(null);
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
                    categories.setValue(categoryMap);
                    callback.onSuccess(categoryMap);
                } else {
                    // Only use defaults if we have no categories
                    if (categories.getValue() == null || categories.getValue().isEmpty()) {
                        Map<String, Integer> defaultCats = new HashMap<>(defaultCategories);
                        categories.setValue(defaultCats);
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
                    categories.setValue(defaultCats);
                    callback.onSuccess(defaultCats);
                } else {
                    callback.onError(t.getMessage());
                }
            }
        });
    }
}
