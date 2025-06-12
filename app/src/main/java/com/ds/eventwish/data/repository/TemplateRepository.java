package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.Set;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing templates
 */
public class TemplateRepository {
    private static final String TAG = "TemplateRepository";
    private static final String COLLECTION_TEMPLATES = "templates";

    private static volatile TemplateRepository instance;
    private static Context applicationContext;

    private final FirebaseFirestore db;
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

    public static void init(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
    }

    public static TemplateRepository getInstance() {
        if (instance == null) {
            synchronized (TemplateRepository.class) {
                if (instance == null) {
                    if (applicationContext == null) {
                        throw new IllegalStateException("TemplateRepository must be initialized with init(Context) before calling getInstance()");
                    }
                    instance = new TemplateRepository();
                }
            }
        }
        return instance;
    }

    private TemplateRepository() {
        this.db = FirebaseFirestore.getInstance();
        apiService = ApiClient.getClient();
        templates.postValue(new ArrayList<>());
        categories.postValue(new HashMap<>());
        // Initialize with default categories
        ensureDefaultCategories(null);
        
        // Load categories if context is available
        if (applicationContext != null) {
            loadCategoriesFromPrefs();
        }
    }

    private void loadCategoriesFromPrefs() {
        if (applicationContext == null) {
            Log.e(TAG, "Cannot load categories from prefs: app context is null");
            return;
        }
        
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
        if (applicationContext == null || categoriesToSave == null) {
            return;
        }
        
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
        // Skip if already loading
        if (loading.getValue() != null && loading.getValue()) {
            Log.d(TAG, "Already loading templates, skipping duplicate request");
            return;
        }

        // Set loading state
        loading.postValue(true);
        
        // Clear error state when starting a new load
        error.postValue(null);

        // Reset pagination for new requests
        if (forceRefresh) {
            Log.d(TAG, "Clearing existing templates and resetting pagination");
            currentPage = 1;
            hasMorePages = true;
            
            // Only clear templates if we're not filtering by category or if this is the initial load
            if (currentCategory == null || templates.getValue() == null || templates.getValue().isEmpty()) {
                templates.postValue(new ArrayList<>());
            }
        }

        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        // Load templates with user context if available
        loadTemplatesInternal(forceRefresh, user);
    }

    private void loadTemplatesInternal(boolean forceRefresh, FirebaseUser user) {
        // If this is a forced refresh, reset pagination
        if (forceRefresh) {
            currentPage = 1;
            hasMorePages = true;
            
            // Only clear templates if we're not filtering by category or if this is the initial load
            if (currentCategory == null || templates.getValue() == null || templates.getValue().isEmpty()) {
                templates.postValue(new ArrayList<>());
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
                  ", forceRefresh: " + forceRefresh +
                  ", user: " + (user != null ? user.getUid() : "null"));

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
                    
                    // Get the new templates from the response
                    List<Template> newTemplates = templateResponse.getTemplates();
                    
                    // Sort the new templates by creation date (newest first)
                    if (newTemplates != null && !newTemplates.isEmpty()) {
                        Collections.sort(newTemplates, (t1, t2) -> {
                            long time1 = t1.getCreatedAtTimestamp();
                            long time2 = t2.getCreatedAtTimestamp();
                            // Sort in descending order (newest first)
                            return Long.compare(time2, time1);
                        });
                        
                        Log.d(TAG, "Sorted " + newTemplates.size() + " templates by creation date (newest first)");
                    }

                    final List<Template> finalList;
                    if (forceRefresh) {
                        // For a force refresh, use only the sorted new templates
                        finalList = new ArrayList<>(newTemplates);
                    } else {
                        // For pagination, we need to merge and maintain the current order
                        // Add new templates at the end
                        finalList = new ArrayList<>(currentList);
                        
                        // Add new templates that don't already exist in the list
                        for (Template newTemplate : newTemplates) {
                            boolean exists = false;
                            for (Template existingTemplate : finalList) {
                                if (existingTemplate.getId().equals(newTemplate.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                finalList.add(newTemplate);
                            }
                        }
                        
                        // Only sort if this is not a user interaction update
                        if (forceRefresh) {
                        // Re-sort the entire list to ensure newest templates are always at the top
                            Collections.sort(finalList, (t1, t2) -> {
                            long time1 = t1.getCreatedAtTimestamp();
                            long time2 = t2.getCreatedAtTimestamp();
                            // Sort in descending order (newest first)
                            return Long.compare(time2, time1);
                        });
                        
                            Log.d(TAG, "Re-sorted complete list of " + finalList.size() + " templates by creation date");
                        }
                    }
                    
                    // Check like/favorite status if user is logged in
                    if (user != null && newTemplates != null && !newTemplates.isEmpty()) {
                        Log.d(TAG, "Starting interaction check for " + newTemplates.size() + " templates with user: " + user.getUid());
                        
                        checkInteractionStates(newTemplates, user.getUid())
                            .addOnSuccessListener(updatedTemplates -> {
                                Log.d(TAG, "Successfully checked interaction states");
                                
                                // Create a new list with the same order as finalList
                                List<Template> updatedFinalList = new ArrayList<>(finalList);
                                
                                // Update states without changing positions
                                for (int i = 0; i < updatedFinalList.size(); i++) {
                                    Template template = updatedFinalList.get(i);
                                    
                                    // Find updated template with same ID
                                    for (Template updatedTemplate : updatedTemplates) {
                                        if (template.getId().equals(updatedTemplate.getId())) {
                                            // Update like/favorite status without changing position
                                            template.setLiked(updatedTemplate.isLiked());
                                            template.setFavorited(updatedTemplate.isFavorited());
                                            template.setLikeCount(updatedTemplate.getLikeCount());
                                            break;
                                        }
                                    }
                                }
                                
                                // Apply locally stored interactions after server data
List<Template> locallyUpdatedList = applyLocalInteractionStates(updatedFinalList, user.getUid());

// Post updated list without changing order
templates.postValue(locallyUpdatedList);
Log.d(TAG, "Posted updated templates to LiveData with server and local interaction states");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to check interaction states", e);
                                
                                // Even if server check fails, still apply local states
                                List<Template> locallyUpdatedList = applyLocalInteractionStates(finalList, user.getUid());
                                templates.postValue(locallyUpdatedList);
                                Log.d(TAG, "Posted locally updated templates to LiveData after server failure");
                            });
                    } else {
                        // No user or no templates, just post the list as is
                        if (user != null) {
                            // If we have a user but no new templates, still apply local states to the final list
                            List<Template> locallyUpdatedList = applyLocalInteractionStates(finalList, user.getUid());
                            templates.postValue(locallyUpdatedList);
                        } else {
                            templates.postValue(finalList);
                        }
                    }
                    
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
            SharedPreferences prefs = applicationContext != null ? 
                applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) : null;
            
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

    /**
     * Insert a list of templates into the database
     * @param templateList List of templates to insert
     * @param notifyNewTemplates Whether to notify observers about new templates
     */
    public void insertAll(List<Template> templateList, boolean notifyNewTemplates) {
        if (templateList == null || templateList.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Adding " + templateList.size() + " templates to the repository");
        
        // Sort the new templates by creation date (newest first)
        Collections.sort(templateList, (t1, t2) -> {
            long time1 = t1.getCreatedAtTimestamp();
            long time2 = t2.getCreatedAtTimestamp();
            // Sort in descending order (newest first)
            return Long.compare(time2, time1);
        });
        
        // Update the templates LiveData with the new templates
        List<Template> currentList = templates.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        // Add new templates to the current list
        List<Template> updatedList = new ArrayList<>(currentList);
        updatedList.addAll(0, templateList); // Add at the beginning to show newest first
        
        // Re-sort the entire list to ensure newest templates are always at the top
        Collections.sort(updatedList, (t1, t2) -> {
            long time1 = t1.getCreatedAtTimestamp();
            long time2 = t2.getCreatedAtTimestamp();
            // Sort in descending order (newest first)
            return Long.compare(time2, time1);
        });
        
        // Update the LiveData
        templates.postValue(updatedList);
        
        // Notify observers about new templates if requested
        if (notifyNewTemplates) {
            notifyNewTemplatesInserted(templateList);
        }
    }
    
    /**
     * Insert a list of templates into the database
     * @param templateList List of templates to insert
     */
    public void insertAll(List<Template> templateList) {
        insertAll(templateList, true); // Default to notifying about new templates
    }
    
    /**
     * Notify observers that new templates have been inserted
     * @param newTemplates The list of newly inserted templates
     */
    public void notifyNewTemplatesInserted(List<Template> newTemplates) {
        if (newTemplates == null || newTemplates.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Notifying observers about " + newTemplates.size() + " new templates");
        
        // Reset the lastCheckTime in SharedPreferences to force detection of new templates
        if (applicationContext != null) {
            SharedPreferences prefs = applicationContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            // Set to a time before the new templates were created
            long timeBeforeNewTemplates = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 1 day ago
            prefs.edit().putLong("last_check_time", timeBeforeNewTemplates).apply();
            Log.d(TAG, "Reset last_check_time to force detection of new templates");
        }
    }

    /**
     * Get templates for a specific category
     */
    @NonNull
    public Task<List<Template>> getTemplatesForCategory(String categoryId) {
        return db.collection(COLLECTION_TEMPLATES)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                QuerySnapshot snapshot = task.getResult();
                List<Template> templates = new ArrayList<>();

                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    templates.add(new Template(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("categoryId"),
                        doc.getString("imageUrl"),
                        Boolean.TRUE.equals(doc.getBoolean("isLiked")),
                        Boolean.TRUE.equals(doc.getBoolean("isFavorited")),
                        doc.getLong("likeCount") != null ? doc.getLong("likeCount").intValue() : 0
                    ));
                }

                return templates;
            });
    }

    /**
     * Get a template by ID
     */
    @NonNull
    public Task<Template> getTemplateById(String templateId) {
        return db.collection(COLLECTION_TEMPLATES)
            .document(templateId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                if (!doc.exists()) {
                    throw new IllegalStateException("Template not found: " + templateId);
                }

                return new Template(
                    doc.getId(),
                    doc.getString("name"),
                    doc.getString("categoryId"),
                    doc.getString("imageUrl"),
                    Boolean.TRUE.equals(doc.getBoolean("isLiked")),
                    Boolean.TRUE.equals(doc.getBoolean("isFavorited")),
                    doc.getLong("likeCount") != null ? doc.getLong("likeCount").intValue() : 0
                );
            });
    }

    /**
     * Update a template in the repository
     * @param template The template to update
     */
    public void updateTemplate(Template template) {
        if (template == null || template.getId() == null) {
            Log.e(TAG, "Cannot update template: template or template ID is null");
            return;
        }

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        Log.d(TAG, String.format("Starting template update - ID: %s, LikeChanged: %b, FavoriteChanged: %b", 
            template.getId(), 
            template.isLikeChanged(),
            template.isFavoriteChanged()));
        
        // Handle likes
        if (template.isLikeChanged()) {
            Log.d(TAG, String.format("Processing like change - TemplateID: %s, IsLiked: %b", 
                template.getId(), 
                template.isLiked()));
                
            firestoreManager.toggleLike(template.getId())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, String.format("Successfully toggled like - TemplateID: %s", template.getId()));
                    template.clearChangeFlags();
                    saveTemplateToCache(template);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, String.format("Failed to toggle like - TemplateID: %s", template.getId()), e);
                    template.clearChangeFlags();
                });
        }
        
        // Handle favorites
        if (template.isFavoriteChanged()) {
            Log.d(TAG, String.format("Processing favorite change - TemplateID: %s, IsFavorited: %b", 
                template.getId(), 
                template.isFavorited()));
                
            firestoreManager.toggleFavorite(template.getId())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, String.format("Successfully toggled favorite - TemplateID: %s", template.getId()));
                    template.clearChangeFlags();
                    saveTemplateToCache(template);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, String.format("Failed to toggle favorite - TemplateID: %s", template.getId()), e);
                    template.clearChangeFlags();
                });
        }
    }
    
    /**
     * Save template to local cache
     */
    private void saveTemplateToCache(Template template) {
        Log.d(TAG, String.format("Saving template to cache - ID: %s", template.getId()));
        if (applicationContext == null) return;
        
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences("template_cache", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            Gson gson = new Gson();
            String json = gson.toJson(template);
            editor.putString("template_" + template.getId(), json);
            editor.apply();
            
            Log.d(TAG, "Template saved to cache: " + template.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error saving template to cache", e);
        }
    }
    
    /**
     * Notify observers that templates have been updated
     * @param updatedTemplates The updated list of templates
     */
    public void notifyTemplatesUpdated(List<Template> updatedTemplates) {
        if (updatedTemplates == null) return;
        
        // Create a new list to avoid modification issues
        List<Template> newList = new ArrayList<>(updatedTemplates);
        
        // Post the update on the main thread
        templates.postValue(newList);
        
        // Log the update
        Log.d(TAG, "Templates updated, notifying observers. Count: " + newList.size());
    }

    public LiveData<Boolean> getLikeState(String templateId) {
        MutableLiveData<Boolean> likeState = new MutableLiveData<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            likeState.setValue(false);
            return likeState;
        }
        
        // Get Firebase token for API authorization
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                likeState.postValue(false);
                return;
            }
            
            String authToken = "Bearer " + tokenTask.getResult().getToken();
            String userId = user.getUid();
            
            // Get user's likes from MongoDB
            apiService.getUserLikes(userId, authToken).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("likes") && body.get("likes").isJsonArray()) {
                                // Check if templateId is in the likes array
                                boolean isLiked = false;
                                for (JsonElement element : body.getAsJsonArray("likes")) {
                                    if (element.isJsonPrimitive() && 
                                        templateId.equals(element.getAsString())) {
                                        isLiked = true;
                                        break;
                                    }
                                }
                                likeState.postValue(isLiked);
                                return;
                            }
                        }
                        // Default to false if any issues
                        likeState.postValue(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing like state response", e);
                        likeState.postValue(false);
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to get likes from MongoDB", t);
                    likeState.postValue(false);
                }
            });
        });

        return likeState;
    }

    public LiveData<Boolean> getFavoriteState(String templateId) {
        MutableLiveData<Boolean> favoriteState = new MutableLiveData<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            favoriteState.setValue(false);
            return favoriteState;
        }
        
        // Get Firebase token for API authorization
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                favoriteState.postValue(false);
                return;
            }
            
            String authToken = "Bearer " + tokenTask.getResult().getToken();
            String userId = user.getUid();
            
            // Get user's favorites from MongoDB
            apiService.getUserFavorites(userId, authToken).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("favorites") && body.get("favorites").isJsonArray()) {
                                // Check if templateId is in the favorites array
                                boolean isFavorited = false;
                                for (JsonElement element : body.getAsJsonArray("favorites")) {
                                    if (element.isJsonPrimitive() && 
                                        templateId.equals(element.getAsString())) {
                                        isFavorited = true;
                                        break;
                                    }
                                }
                                favoriteState.postValue(isFavorited);
                                return;
                            }
                        }
                        // Default to false if any issues
                        favoriteState.postValue(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing favorite state response", e);
                        favoriteState.postValue(false);
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to get favorites from MongoDB", t);
                    favoriteState.postValue(false);
                }
            });
        });

        return favoriteState;
    }

    public LiveData<Integer> getLikeCount(String templateId) {
        MutableLiveData<Integer> likeCount = new MutableLiveData<>();
        
        // Get the template from the API to get the latest like count
        apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
            @Override
            public void onResponse(Call<Template> call, Response<Template> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Template template = response.body();
                    // Ensure count is non-negative
                    long count = template.getLikeCount();
                    int safeCount = Math.max(0, (int)count);
                    likeCount.postValue(safeCount);
                    Log.d(TAG, "Got like count for template " + templateId + " from MongoDB: " + safeCount);
                } else {
                    // Default to 0 if API call fails
                    likeCount.postValue(0);
                    Log.d(TAG, "Failed to get template " + templateId + " from MongoDB, defaulting like count to 0");
                }
            }
            
            @Override
            public void onFailure(Call<Template> call, Throwable t) {
                // Default to 0 on error
                likeCount.postValue(0);
                Log.e(TAG, "Error getting template " + templateId + " from MongoDB for like count", t);
            }
        });

        return likeCount;
    }

    public Task<Boolean> toggleLike(String templateId, boolean newState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            error.postValue("User must be logged in");
            return Tasks.forException(new IllegalStateException("User must be logged in"));
        }

        String userId = user.getUid();
        
        // Create a TaskCompletionSource to return a Task
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        
        // Update UI immediately for better user experience - ensure it runs on main thread
Handler mainHandler = new Handler(Looper.getMainLooper());
mainHandler.post(() -> {
    updateTemplateStateLocally(templateId, newState, null);
    
    // Notify observers immediately for real-time UI updates
    List<Template> currentList = templates.getValue();
    if (currentList != null) {
        templates.postValue(new ArrayList<>(currentList));
    }
});
        
        // Force refresh the Firebase token to ensure it's not expired
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                
                // Store action locally instead of reverting UI
                storeLikeActionLocally(templateId, userId, newState);
                
                // For UI consistency, consider this a success
                tcs.setResult(newState);
                return;
            }
            
            String token = tokenTask.getResult().getToken();
            Log.d(TAG, "Got fresh Firebase token for API call: " + (token.length() > 10 ? token.substring(0, 10) + "..." : "invalid"));
            String authToken = "Bearer " + token;
            
            // Always use the likeTemplate API method regardless of newState
            // The server will handle adding or removing the like based on the current state
            Call<JsonObject> apiCall;
            
            if (newState) {
                // Like template using MongoDB API
                apiCall = apiService.likeTemplate(userId, templateId, authToken);
            } else {
                // For unlike, still use the unlikeTemplate endpoint but handle errors properly
                apiCall = apiService.unlikeTemplate(userId, templateId, authToken);
            }
            
            // Execute the API call
apiCall.enqueue(new Callback<JsonObject>() {
    @Override
    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG, "Successfully " + (newState ? "liked" : "unliked") + 
                  " template " + templateId + " in MongoDB");
            
            // If unliking, ensure it's removed from the user's liked templates in MongoDB
            if (!newState) {
                Log.d(TAG, "Template " + templateId + " removed from user's liked templates in MongoDB");
            }
            
            tcs.setResult(newState);
        } else {
                        // Get error body for debugging
                        String errorBody = "";
                        try {
                            errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        } catch (Exception e) {
                            errorBody = "Error reading error body: " + e.getMessage();
                        }
                        
                        // Check if it contains the Project Id error
                        boolean isProjectIdError = errorBody.contains("Unable to detect a Project Id");
                        boolean isEnumError = errorBody.contains("is not a valid enum value for path `action`");
                        
                        Log.e(TAG, "Failed to update like state in MongoDB: " + response.code() + 
                              "\nError body: " + errorBody);
                        
                        if (response.code() == 401 || isProjectIdError || isEnumError) {
                            // Known Firebase validation issue or enum validation issue - use local storage
                            Log.d(TAG, "Authentication error or validation error, storing " + 
                                  (newState ? "like" : "unlike") + " action locally");
                            
                            // Store action locally and pretend success
                            storeLikeActionLocally(templateId, userId, newState);
                            tcs.setResult(newState);
                        } else {
                            // Other server error - still try local storage but report partial success
                            Log.w(TAG, "Server error " + response.code() + ", falling back to local storage");
                            storeLikeActionLocally(templateId, userId, newState);
                            tcs.setResult(newState);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "API call failed when updating like state", t);
                    
                    // For network errors, store locally and maintain UI state
                    storeLikeActionLocally(templateId, userId, newState);
                    tcs.setResult(newState); // Pretend success to maintain UI state
                }
            });
        });
        
        return tcs.getTask();
    }

    public Task<Boolean> toggleFavorite(String templateId, boolean newState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            error.postValue("User must be logged in");
            return Tasks.forException(new IllegalStateException("User must be logged in"));
        }

        String userId = user.getUid();
        
        // Create a TaskCompletionSource to return a Task
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        
        // Update UI immediately for better user experience - ensure it runs on main thread
Handler mainHandler = new Handler(Looper.getMainLooper());
mainHandler.post(() -> {
    updateTemplateStateLocally(templateId, null, newState);
    
    // Notify observers immediately for real-time UI updates
    List<Template> currentList = templates.getValue();
    if (currentList != null) {
        templates.postValue(new ArrayList<>(currentList));
    }
});
        
        // Force refresh the Firebase token to ensure it's not expired
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                
                // Store action locally instead of reverting UI
                storeFavoriteActionLocally(templateId, userId, newState);
                
                // For UI consistency, consider this a success
                tcs.setResult(newState);
                return;
            }
            
            String token = tokenTask.getResult().getToken();
            Log.d(TAG, "Got fresh Firebase token for API call: " + (token.length() > 10 ? token.substring(0, 10) + "..." : "invalid"));
            String authToken = "Bearer " + token;
            
            // Always use the appropriate API method based on the requested state
            Call<JsonObject> apiCall;
            
            if (newState) {
                // Add to favorites using MongoDB API
                apiCall = apiService.addToFavorites(userId, templateId, authToken);
            } else {
                // Remove from favorites using MongoDB API
                apiCall = apiService.removeFromFavorites(userId, templateId, authToken);
            }
            
            // Execute the API call
apiCall.enqueue(new Callback<JsonObject>() {
    @Override
    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG, "Successfully " + (newState ? "added to favorites" : "removed from favorites") + 
                  " template " + templateId + " in MongoDB");
            
            // If unfavoriting, ensure it's removed from the user's favorited templates in MongoDB
            if (!newState) {
                Log.d(TAG, "Template " + templateId + " removed from user's favorites in MongoDB");
            }
            
            tcs.setResult(newState);
        } else {
                        // Get error body for debugging
                        String errorBody = "";
                        try {
                            errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        } catch (Exception e) {
                            errorBody = "Error reading error body: " + e.getMessage();
                        }
                        
                        // Check if it contains the Project Id error
                        boolean isProjectIdError = errorBody.contains("Unable to detect a Project Id");
                        boolean isEnumError = errorBody.contains("is not a valid enum value for path `action`");
                        
                        Log.e(TAG, "Failed to update favorite state in MongoDB: " + response.code() + 
                              "\nError body: " + errorBody);
                        
                        if (response.code() == 401 || isProjectIdError || isEnumError) {
                            // Known Firebase validation issue or enum validation issue - use local storage
                            Log.d(TAG, "Authentication error or validation error, storing " + 
                                  (newState ? "favorite" : "unfavorite") + " action locally");
                            
                            // Store action locally and pretend success
                            storeFavoriteActionLocally(templateId, userId, newState);
                            tcs.setResult(newState);
                        } else {
                            // Other server error - still try local storage but report partial success
                            Log.w(TAG, "Server error " + response.code() + ", falling back to local storage");
                            storeFavoriteActionLocally(templateId, userId, newState);
                            tcs.setResult(newState);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "API call failed when updating favorite state", t);
                    
                    // For network errors, store locally and maintain UI state
                    storeFavoriteActionLocally(templateId, userId, newState);
                    tcs.setResult(newState); // Pretend success to maintain UI state
                }
            });
        });
        
        return tcs.getTask();
    }

    /**
     * Update a template's state locally for immediate UI feedback
     */
    private void updateTemplateStateLocally(String templateId, Boolean isLiked, Boolean isFavorited) {
        List<Template> currentTemplates = templates.getValue();
        if (currentTemplates == null) {
            Log.w(TAG, "Cannot update template state locally: current templates list is null");
            return;
        }
        
        // Create a copy of the list to avoid modification issues
        List<Template> updatedTemplates = new ArrayList<>();
        
        // Flag to track if we found and updated the template
        boolean found = false;
        
        // Preserve exact order while updating the specific template
        for (Template template : currentTemplates) {
            // Check if this is the template we need to update
            if (templateId.equals(template.getId())) {
                // Only update the specific fields that changed
                if (isLiked != null) {
                    // Save previous state to determine if this is a change
                    boolean previousState = template.isLiked();
                    
                    // Update liked state
                    template.setLiked(isLiked);
                    
                    // Only update count if state actually changed
                    if (previousState != isLiked) {
                        // Get current count, ensuring it's at least 0
                        long currentCount = Math.max(0L, template.getLikeCount());
                        
                        // Calculate new count based on the new state, not the delta
                        // If liked, ensure at least 1; if unliked, ensure at least 0
                        long newCount = isLiked ? 
                            Math.max(1L, currentCount + 1L) : 
                            Math.max(0L, currentCount - 1L);
                        
                        template.setLikeCount(newCount);
                        Log.d(TAG, "Locally updated template " + templateId + 
                              " like state to " + isLiked + 
                              " with count: " + newCount);
                    }
                }
                
                if (isFavorited != null) {
                    // Save previous state to determine if this is a change
                    boolean previousState = template.isFavorited();
                    
                    // Update favorited state
                    template.setFavorited(isFavorited);
                    
                    // Only update count if state actually changed
                    if (previousState != isFavorited) {
                        // Get current count, ensuring it's at least 0
                        long currentCount = Math.max(0L, template.getFavoriteCount());
                        
                        // Calculate new count based on the new state, not the delta
                        // If favorited, ensure at least 1; if unfavorited, ensure at least 0
                        long newCount = isFavorited ? 
                            Math.max(1L, currentCount + 1L) : 
                            Math.max(0L, currentCount - 1L);
                        
                        template.setFavoriteCount(newCount);
                        Log.d(TAG, "Locally updated template " + templateId + 
                              " favorite state to " + isFavorited + 
                              " with count: " + newCount);
                    }
                }
                
                found = true;
            }
            
            // Add to our updated list (either original or modified)
            updatedTemplates.add(template);
        }
        
        // Only update LiveData if we actually found and modified the template
        if (found) {
            // Always use postValue for thread safety
            templates.postValue(updatedTemplates);
            Log.d(TAG, "Posted template state update for template " + templateId + 
                  (isLiked != null ? ", liked: " + isLiked : "") + 
                  (isFavorited != null ? ", favorited: " + isFavorited : ""));
        } else {
            Log.w(TAG, "Template " + templateId + " not found in current list, state not updated locally");
        }
    }

    private Task<List<Template>> checkInteractionStates(List<Template> templates, String userId) {
        TaskCompletionSource<List<Template>> tcs = new TaskCompletionSource<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User is null, cannot check interaction states");
            tcs.setResult(templates);
            return tcs.getTask();
        }
        
        // Force refresh the Firebase token to ensure it's not expired
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token for interaction check", tokenTask.getException());
                tcs.setResult(templates); // Return original templates
                return;
            }
            
            String token = tokenTask.getResult().getToken();
            Log.d(TAG, "Got fresh Firebase token for interaction check: " + (token.length() > 10 ? token.substring(0, 10) + "..." : "invalid"));
            String authToken = "Bearer " + token;
            
            // Parallel API calls for likes and favorites
            Call<JsonObject> likesCall = apiService.getUserLikes(userId, authToken);
            Call<JsonObject> favoritesCall = apiService.getUserFavorites(userId, authToken);
            
            // We don't need to modify OkHttp dispatcher as ApiClient
            // already configures it properly for concurrent requests
            
            // Execute likes call
            likesCall.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("likes") && body.get("likes").isJsonArray()) {
                                // Process likes
                                JsonArray likesArray = body.getAsJsonArray("likes");
                                Set<String> likedTemplateIds = new HashSet<>();
                                
                                for (JsonElement element : likesArray) {
                                    if (element.isJsonPrimitive()) {
                                        likedTemplateIds.add(element.getAsString());
                                    }
                                }
                                
                                // Update liked status for templates
                                for (Template template : templates) {
                                    String templateId = template.getId();
                                    if (templateId != null) {
                                        boolean isLiked = likedTemplateIds.contains(templateId);
                                        template.setLiked(isLiked);
                                        Log.d(TAG, "Template " + templateId + " like status from MongoDB: " + isLiked);
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to get likes from MongoDB: " + 
                                  (response.code() + " " + response.message()));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing likes response", e);
                    }
                    
                    // Continue with favorites regardless of likes result
                    processFavorites(templates, userId, authToken, tcs);
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to get likes from MongoDB", t);
                    // Continue with favorites regardless of likes failure
                    processFavorites(templates, userId, authToken, tcs);
                }
            });
        });
        
        return tcs.getTask();
    }
    
    private void processFavorites(List<Template> templates, String userId, String authToken, 
                                 TaskCompletionSource<List<Template>> tcs) {
        apiService.getUserFavorites(userId, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject body = response.body();
                        if (body.has("favorites") && body.get("favorites").isJsonArray()) {
                            // Process favorites
                            JsonArray favoritesArray = body.getAsJsonArray("favorites");
                            Set<String> favoritedTemplateIds = new HashSet<>();
                            
                            for (JsonElement element : favoritesArray) {
                                if (element.isJsonPrimitive()) {
                                    favoritedTemplateIds.add(element.getAsString());
                                }
                            }
                            
                            // Update favorited status for templates
                            for (Template template : templates) {
                                String templateId = template.getId();
                                if (templateId != null) {
                                    boolean isFavorited = favoritedTemplateIds.contains(templateId);
                                    template.setFavorited(isFavorited);
                                    Log.d(TAG, "Template " + templateId + " favorite status from MongoDB: " + isFavorited);
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to get favorites from MongoDB: " + 
                              (response.code() + " " + response.message()));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing favorites response", e);
                }
                
                // Apply locally stored interactions after server data
                List<Template> locallyUpdatedList = applyLocalInteractionStates(templates, userId);
                
                // Always set the result with locally updated list to ensure task completes
                tcs.setResult(locallyUpdatedList);
                Log.d(TAG, "Completed interaction check with MongoDB and applied local states for " + templates.size() + " templates");
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to get favorites from MongoDB", t);
                
                // Apply locally stored interactions even when server fails
                List<Template> locallyUpdatedList = applyLocalInteractionStates(templates, userId);
                tcs.setResult(locallyUpdatedList);
                
                Log.d(TAG, "Completed interaction check with MongoDB (with errors) for " + templates.size() + " templates");
                Log.d(TAG, "Applied local interaction states after server failure");
            }
        });
    }

    /**
     * Store like action locally when server is unavailable
     * @param templateId The template ID
     * @param userId The user ID
     * @param isLike True if liking, false if unliking
     */
    private void storeLikeActionLocally(String templateId, String userId, boolean isLike) {
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
            
            // Create a unique key for this action - use a consistent key format
            String key = userId + "_" + templateId;
            
            // Create a JSON string with the action details
            JsonObject actionData = new JsonObject();
            actionData.addProperty("templateId", templateId);
            actionData.addProperty("userId", userId);
            actionData.addProperty("action", isLike ? "like" : "unlike");
            actionData.addProperty("timestamp", System.currentTimeMillis());
            
            // Store in SharedPreferences
            prefs.edit().putString(key, actionData.toString()).apply();
            
            Log.d(TAG, "Stored " + (isLike ? "like" : "unlike") + " action locally for template " + 
                  templateId + " and user " + userId + " with key: " + key);
            
            // We could schedule a background job to sync these later
            // WorkManager.getInstance(applicationContext).enqueue(...);
        } catch (Exception e) {
            Log.e(TAG, "Error storing like action locally", e);
        }
    }

    /**
     * Store favorite action locally when server is unavailable
     * @param templateId The template ID
     * @param userId The user ID
     * @param isFavorite True if adding to favorites, false if removing
     */
    private void storeFavoriteActionLocally(String templateId, String userId, boolean isFavorite) {
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
            
            // Create a unique key for this action - use a consistent key format
            String key = userId + "_" + templateId;
            
            // Create a JSON string with the action details
            JsonObject actionData = new JsonObject();
            actionData.addProperty("templateId", templateId);
            actionData.addProperty("userId", userId);
            actionData.addProperty("action", isFavorite ? "favorite" : "unfavorite");
            actionData.addProperty("timestamp", System.currentTimeMillis());
            
            // Store in SharedPreferences
            prefs.edit().putString(key, actionData.toString()).apply();
            
            Log.d(TAG, "Stored " + (isFavorite ? "favorite" : "unfavorite") + " action locally for template " + 
                  templateId + " and user " + userId + " with key: " + key);
            
            // We could schedule a background job to sync these later
            // WorkManager.getInstance(applicationContext).enqueue(...);
        } catch (Exception e) {
            Log.e(TAG, "Error storing favorite action locally", e);
        }
    }

    /**
     * Load locally stored like/favorite actions and apply them to templates
     * This should be called after templates are loaded from the server
     * @param templates The list of templates to update
     * @param userId The current user ID
     * @return The updated list of templates
     */
    private List<Template> applyLocalInteractionStates(List<Template> templates, String userId) {
        if (templates == null || templates.isEmpty() || userId == null || applicationContext == null) {
            Log.d(TAG, "Cannot apply local interaction states: templates=" + (templates == null ? "null" : templates.size()) +
                  ", userId=" + (userId == null ? "null" : userId) +
                  ", applicationContext=" + (applicationContext == null ? "null" : "available"));
            return templates;
        }
        
        Log.d(TAG, "Applying local interaction states for " + templates.size() + " templates with userId: " + userId);
        
        try {
            // Create a map of template IDs to templates for quick lookup
            Map<String, Template> templateMap = new HashMap<>();
            for (Template template : templates) {
                if (template.getId() != null) {
                    templateMap.put(template.getId(), template);
                    Log.d(TAG, "Template before local state: id=" + template.getId() + 
                          ", liked=" + template.isLiked() + 
                          ", favorited=" + template.isFavorited());
                }
            }
            
            // Load likes from SharedPreferences
            SharedPreferences likePrefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
            Map<String, ?> allLikes = likePrefs.getAll();
            
            if (!allLikes.isEmpty()) {
                Log.d(TAG, "Found " + allLikes.size() + " locally stored like actions");
                
                // Process each stored like action
                for (Map.Entry<String, ?> entry : allLikes.entrySet()) {
                    try {
                        String jsonStr = (String) entry.getValue();
                        JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                        
                        if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                            String templateId = actionData.get("templateId").getAsString();
                            String storedUserId = actionData.get("userId").getAsString();
                            String action = actionData.get("action").getAsString();
                            
                            // Only apply if this is for the current user
                            if (userId.equals(storedUserId) && templateMap.containsKey(templateId)) {
                                Template template = templateMap.get(templateId);
                                boolean isLike = "like".equals(action);
                                
                                // Apply the action
                                template.setLiked(isLike);
                                
                                // Update like count
                                long currentCount = Math.max(0L, template.getLikeCount());
                                long newCount = isLike ? 
                                    Math.max(1L, currentCount) : 
                                    Math.max(0L, currentCount - 1L);
                                template.setLikeCount(newCount);
                                
                                Log.d(TAG, "Applied local like state for template " + templateId + ": " + isLike + 
                                      ", new like count: " + newCount);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing locally stored like action", e);
                    }
                }
            } else {
                Log.d(TAG, "No locally stored like actions found");
            }
            
            // Load favorites from SharedPreferences
            SharedPreferences favoritePrefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
            Map<String, ?> allFavorites = favoritePrefs.getAll();
            
            if (!allFavorites.isEmpty()) {
                Log.d(TAG, "Found " + allFavorites.size() + " locally stored favorite actions");
                
                // Process each stored favorite action
                for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                    try {
                        String jsonStr = (String) entry.getValue();
                        JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                        
                        if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                            String templateId = actionData.get("templateId").getAsString();
                            String storedUserId = actionData.get("userId").getAsString();
                            String action = actionData.get("action").getAsString();
                            
                            // Only apply if this is for the current user
                            if (userId.equals(storedUserId) && templateMap.containsKey(templateId)) {
                                Template template = templateMap.get(templateId);
                                boolean isFavorite = "favorite".equals(action);
                                
                                // Apply the action
                                template.setFavorited(isFavorite);
                                
                                // Update favorite count
                                long currentCount = Math.max(0L, template.getFavoriteCount());
                                long newCount = isFavorite ? 
                                    Math.max(1L, currentCount) : 
                                    Math.max(0L, currentCount - 1L);
                                template.setFavoriteCount(newCount);
                                
                                Log.d(TAG, "Applied local favorite state for template " + templateId + ": " + isFavorite + 
                                      ", new favorite count: " + newCount);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing locally stored favorite action", e);
                    }
                }
            } else {
                Log.d(TAG, "No locally stored favorite actions found");
            }
            
            // Log final states for debugging
            for (Template template : templates) {
                if (template.getId() != null) {
                    Log.d(TAG, "Template after local state: id=" + template.getId() + 
                          ", liked=" + template.isLiked() + 
                          ", favorited=" + template.isFavorited());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying local interaction states", e);
        }
        
        return templates;
    }

    /**
     * Get the user's most recently liked templates
     * 
     * @param limit Maximum number of templates to return
     * @return LiveData containing a list of the user's most recently liked templates
     */
    public LiveData<List<Template>> getMostRecentlyLikedTemplates(int limit) {
        MutableLiveData<List<Template>> likedTemplates = new MutableLiveData<>();
        likedTemplates.setValue(new ArrayList<>());
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Cannot get liked templates: user not logged in");
            return likedTemplates;
        }
        
        String userId = user.getUid();
        
        // Force refresh the Firebase token to ensure it's not expired
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                return;
            }
            
            String token = tokenTask.getResult().getToken();
            String authToken = "Bearer " + token;
            
            // Get user's liked templates from MongoDB
            apiService.getUserLikes(userId, authToken).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject body = response.body();
                        if (body.has("likes") && body.get("likes").isJsonArray()) {
                            JsonArray likesArray = body.getAsJsonArray("likes");
                            final List<String> likedTemplateIds = new ArrayList<>();
                            
                            for (JsonElement element : likesArray) {
                                if (element.isJsonPrimitive()) {
                                    likedTemplateIds.add(element.getAsString());
                                }
                            }
                            
                            if (likedTemplateIds.isEmpty()) {
                                Log.d(TAG, "User has no liked templates");
                                likedTemplates.postValue(new ArrayList<>());
                                return;
                            }
                            
                            // Create a copy of the list for limiting
                            final List<String> limitedLikedTemplateIds;
                            // Limit the number of templates to fetch
                            if (likedTemplateIds.size() > limit) {
                                limitedLikedTemplateIds = new ArrayList<>(likedTemplateIds.subList(0, limit));
                            } else {
                                limitedLikedTemplateIds = new ArrayList<>(likedTemplateIds);
                            }
                            
                            Log.d(TAG, "Fetching " + limitedLikedTemplateIds.size() + " liked templates");
                            
                            // Fetch template details for each liked template ID
                            List<Template> templates = new ArrayList<>();
                            AtomicInteger fetchCount = new AtomicInteger(0);
                            
                            for (String templateId : limitedLikedTemplateIds) {
                                final String finalTemplateId = templateId;
                                apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
                                    @Override
                                    public void onResponse(Call<Template> call, Response<Template> response) {
                                        if (response.isSuccessful() && response.body() != null) {
                                            Template template = response.body();
                                            template.setLiked(true); // Ensure it's marked as liked
                                            templates.add(template);
                                        }
                                        
                                        // Check if all templates have been fetched
                                        if (fetchCount.incrementAndGet() == limitedLikedTemplateIds.size()) {
                                            // Sort by most recently liked (assuming the order in the likes array)
                                            likedTemplates.postValue(templates);
                                            Log.d(TAG, "Posted " + templates.size() + " liked templates");
                                        }
                                    }
                                    
                                    @Override
                                    public void onFailure(Call<Template> call, Throwable t) {
                                        Log.e(TAG, "Failed to fetch template " + finalTemplateId, t);
                                        
                                        // Check if all templates have been fetched
                                        if (fetchCount.incrementAndGet() == limitedLikedTemplateIds.size()) {
                                            likedTemplates.postValue(templates);
                                            Log.d(TAG, "Posted " + templates.size() + " liked templates (with errors)");
                                        }
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to get user likes: " + response.code());
                        
                        // Try to get locally stored likes
                        getLikedTemplatesFromLocalStorage(userId, likedTemplates, limit);
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to get user likes", t);
                    
                    // Try to get locally stored likes
                    getLikedTemplatesFromLocalStorage(userId, likedTemplates, limit);
                }
            });
        });
        
        return likedTemplates;
    }

    /**
     * Get the user's most recently favorited templates
     * 
     * @param limit Maximum number of templates to return
     * @return LiveData containing a list of the user's most recently favorited templates
     */
    public LiveData<List<Template>> getMostRecentlyFavoritedTemplates(int limit) {
        MutableLiveData<List<Template>> favoritedTemplates = new MutableLiveData<>();
        favoritedTemplates.setValue(new ArrayList<>());
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Cannot get favorited templates: user not logged in");
            return favoritedTemplates;
        }
        
        String userId = user.getUid();
        
        // Force refresh the Firebase token to ensure it's not expired
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Failed to get Firebase token", tokenTask.getException());
                return;
            }
            
            String token = tokenTask.getResult().getToken();
            String authToken = "Bearer " + token;
            
            // Get user's favorited templates from MongoDB
            apiService.getUserFavorites(userId, authToken).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("favorites")) {
                                JsonArray favoritesArray = body.getAsJsonArray("favorites");
                                final List<String> favoritedTemplateIds = new ArrayList<>();
                                
                                for (JsonElement element : favoritesArray) {
                                    if (element.isJsonPrimitive()) {
                                        favoritedTemplateIds.add(element.getAsString());
                                    }
                                }
                                
                                if (favoritedTemplateIds.isEmpty()) {
                                    Log.d(TAG, "User has no favorited templates");
                                    favoritedTemplates.postValue(new ArrayList<>());
                                    return;
                                }
                                
                                // Create a copy of the list for limiting
                                final List<String> limitedFavoritedTemplateIds;
                                if (favoritedTemplateIds.size() > limit) {
                                    limitedFavoritedTemplateIds = favoritedTemplateIds.subList(0, limit);
                                } else {
                                    limitedFavoritedTemplateIds = favoritedTemplateIds;
                                }
                                
                                Log.d(TAG, "Found " + limitedFavoritedTemplateIds.size() + " favorited templates for user " + userId);
                                
                                // Fetch template details for each favorited template ID
                                List<Template> templates = new ArrayList<>();
                                AtomicInteger fetchCount = new AtomicInteger(0);
                                
                                for (String templateId : limitedFavoritedTemplateIds) {
                                    final String finalTemplateId = templateId;
                                    apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
                                        @Override
                                        public void onResponse(Call<Template> call, Response<Template> response) {
                                            if (response.isSuccessful() && response.body() != null) {
                                                Template template = response.body();
                                                template.setFavorited(true);
                                                templates.add(template);
                                                
                                                Log.d(TAG, "Fetched favorited template: " + template.getTitle());
                                                
                                                // If we've fetched all templates, update the LiveData
                                                if (fetchCount.incrementAndGet() == limitedFavoritedTemplateIds.size()) {
                                                    favoritedTemplates.postValue(templates);
                                                    Log.d(TAG, "Posted " + templates.size() + " favorited templates");
                                                }
                                            } else {
                                                Log.e(TAG, "Failed to fetch favorited template " + finalTemplateId);
                                                
                                                // Still increment the counter to ensure we complete
                                                if (fetchCount.incrementAndGet() == limitedFavoritedTemplateIds.size()) {
                                                    favoritedTemplates.postValue(templates);
                                                    Log.d(TAG, "Posted " + templates.size() + " favorited templates (some failed)");
                                                }
                                            }
                                        }
                                        
                                        @Override
                                        public void onFailure(Call<Template> call, Throwable t) {
                                            Log.e(TAG, "Failed to fetch favorited template " + finalTemplateId, t);
                                            
                                            // Still increment the counter to ensure we complete
                                            if (fetchCount.incrementAndGet() == limitedFavoritedTemplateIds.size()) {
                                                favoritedTemplates.postValue(templates);
                                                Log.d(TAG, "Posted " + templates.size() + " favorited templates (some failed)");
                                            }
                                        }
                                    });
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to get favorites from MongoDB: " + 
                                  (response.code() + " " + response.message()));
                            
                            // Try to get locally stored favorites
                            getFavoritedTemplatesFromLocalStorage(userId, favoritedTemplates, limit);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing favorites response", e);
                        
                        // Try to get locally stored favorites
                        getFavoritedTemplatesFromLocalStorage(userId, favoritedTemplates, limit);
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to get user favorites", t);
                    
                    // Try to get locally stored favorites
                    getFavoritedTemplatesFromLocalStorage(userId, favoritedTemplates, limit);
                }
            });
        });
        
        return favoritedTemplates;
    }

    /**
     * Get liked templates from local storage
     */
    private void getLikedTemplatesFromLocalStorage(String userId, MutableLiveData<List<Template>> likedTemplates, int limit) {
        if (applicationContext == null) {
            return;
        }
        
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
            Map<String, ?> allLikes = prefs.getAll();
            
            if (allLikes.isEmpty()) {
                return;
            }
            
            // Process locally stored likes
            List<String> likedTemplateIds = new ArrayList<>();
            
            for (Map.Entry<String, ?> entry : allLikes.entrySet()) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                        String templateId = actionData.get("templateId").getAsString();
                        String storedUserId = actionData.get("userId").getAsString();
                        String action = actionData.get("action").getAsString();
                        
                        // Only include if this is a like action for the current user
                        if (userId.equals(storedUserId) && "like".equals(action)) {
                            likedTemplateIds.add(templateId);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing locally stored like action", e);
                }
            }
            
            if (likedTemplateIds.isEmpty()) {
                return;
            }
            
            // Limit the number of templates to fetch
            if (likedTemplateIds.size() > limit) {
                likedTemplateIds = likedTemplateIds.subList(0, limit);
            }
            
            // Get templates from the current list
            List<Template> currentTemplates = templates.getValue();
            if (currentTemplates == null || currentTemplates.isEmpty()) {
                return;
            }
            
            // Find templates that match the liked template IDs
            List<Template> likedTemplatesList = new ArrayList<>();
            
            for (String templateId : likedTemplateIds) {
                for (Template template : currentTemplates) {
                    if (templateId.equals(template.getId())) {
                        template.setLiked(true);
                        likedTemplatesList.add(template);
                        break;
                    }
                }
            }
            
            if (!likedTemplatesList.isEmpty()) {
                likedTemplates.postValue(likedTemplatesList);
                Log.d(TAG, "Posted " + likedTemplatesList.size() + " locally stored liked templates");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting liked templates from local storage", e);
        }
    }

    /**
     * Get favorited templates from local storage
     */
    private void getFavoritedTemplatesFromLocalStorage(String userId, MutableLiveData<List<Template>> favoritedTemplates, int limit) {
        if (applicationContext == null) {
            return;
        }
        
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
            Map<String, ?> allFavorites = prefs.getAll();
            
            if (allFavorites.isEmpty()) {
                return;
            }
            
            // Process locally stored favorites
            List<String> favoritedTemplateIds = new ArrayList<>();
            
            for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                        String templateId = actionData.get("templateId").getAsString();
                        String storedUserId = actionData.get("userId").getAsString();
                        String action = actionData.get("action").getAsString();
                        
                        // Only include if this is a favorite action for the current user
                        if (userId.equals(storedUserId) && "favorite".equals(action)) {
                            favoritedTemplateIds.add(templateId);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing locally stored favorite action", e);
                }
            }
            
            if (favoritedTemplateIds.isEmpty()) {
                return;
            }
            
            // Limit the number of templates to fetch
            if (favoritedTemplateIds.size() > limit) {
                favoritedTemplateIds = favoritedTemplateIds.subList(0, limit);
            }
            
            // Get templates from the current list
            List<Template> currentTemplates = templates.getValue();
            if (currentTemplates == null || currentTemplates.isEmpty()) {
                return;
            }
            
            // Find templates that match the favorited template IDs
            List<Template> favoritedTemplatesList = new ArrayList<>();
            
            for (String templateId : favoritedTemplateIds) {
                for (Template template : currentTemplates) {
                    if (templateId.equals(template.getId())) {
                        template.setFavorited(true);
                        favoritedTemplatesList.add(template);
                        break;
                    }
                }
            }
            
            if (!favoritedTemplatesList.isEmpty()) {
                favoritedTemplates.postValue(favoritedTemplatesList);
                Log.d(TAG, "Posted " + favoritedTemplatesList.size() + " locally stored favorited templates");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting favorited templates from local storage", e);
        }
    }

    /**
     * Like a template
     * @param templateId ID of the template to like
     * @return Task representing the operation
     */
    public Task<Boolean> likeTemplate(String templateId) {
        return toggleLike(templateId, true);
    }
    
    /**
     * Unlike a template
     * @param templateId ID of the template to unlike
     * @return Task representing the operation
     */
    public Task<Boolean> unlikeTemplate(String templateId) {
        return toggleLike(templateId, false);
    }
    
    /**
     * Favorite a template
     * @param templateId ID of the template to favorite
     * @return Task representing the operation
     */
    public Task<Boolean> favoriteTemplate(String templateId) {
        return toggleFavorite(templateId, true);
    }
    
    /**
     * Unfavorite a template
     * @param templateId ID of the template to unfavorite
     * @return Task representing the operation
     */
    public Task<Boolean> unfavoriteTemplate(String templateId) {
        return toggleFavorite(templateId, false);
    }
}
