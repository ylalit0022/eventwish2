package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (loading.getValue()) {
            return;
        }
        
        loading.postValue(true);
        
        // Get current user for like/favorite status
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // Try to sign in anonymously
            auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "Anonymous sign in successful, user ID: " + result.getUser().getUid());
                    // Reload templates after successful sign in
                    loadTemplatesInternal(forceRefresh, result.getUser());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Anonymous sign in failed", e);
                    // Continue without user authentication
                    loadTemplatesInternal(forceRefresh, null);
                });
        } else {
            // User already signed in
            loadTemplatesInternal(forceRefresh, auth.getCurrentUser());
        }
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
                        
                        // Log all template IDs for debugging with their timestamps
                        for (Template template : newTemplates) {
                            Log.d(TAG, "Template: " + template.getTitle() + 
                                  ", ID: " + template.getId() + 
                                  ", Created: " + template.getCreatedAt() +
                                  ", Timestamp: " + template.getCreatedAtTimestamp());
                        }
                    }

                    if (forceRefresh) {
                        // For a force refresh, use only the sorted new templates
                        currentList = new ArrayList<>(newTemplates);
                    } else {
                        // For pagination, we need to merge and re-sort all templates
                        currentList.addAll(newTemplates);
                        
                        // Re-sort the entire list to ensure newest templates are always at the top
                        Collections.sort(currentList, (t1, t2) -> {
                            long time1 = t1.getCreatedAtTimestamp();
                            long time2 = t2.getCreatedAtTimestamp();
                            // Sort in descending order (newest first)
                            return Long.compare(time2, time1);
                        });
                        
                        Log.d(TAG, "Re-sorted complete list of " + currentList.size() + " templates by creation date");
                    }
                    
                    // Create final copy for lambda
                    final List<Template> finalCurrentList = currentList;
                    
                    // Post the sorted list to LiveData
                    templates.postValue(finalCurrentList);

                    // Check like/favorite status if user is logged in
                    if (user != null && newTemplates != null && !newTemplates.isEmpty()) {
                        Log.d(TAG, "Starting interaction check for " + newTemplates.size() + " templates with user: " + user.getUid());
                        
                        // Log initial states
                        for (Template template : newTemplates) {
                            Log.d(TAG, String.format("Initial state - Template: %s, Liked: %b, Favorited: %b", 
                                template.getId(), template.isLiked(), template.isFavorited()));
                        }
                        
                        checkInteractionStates(newTemplates, user.getUid())
                            .addOnSuccessListener(updatedTemplates -> {
                                Log.d(TAG, "Successfully checked interaction states");
                                
                                // Log updated states
                                for (Template template : updatedTemplates) {
                                    Log.d(TAG, String.format("Updated state - Template: %s, Liked: %b, Favorited: %b", 
                                        template.getId(), template.isLiked(), template.isFavorited()));
                                }
                                
                                // Update like/favorite status in the already posted list
                                templates.postValue(finalCurrentList);
                                Log.d(TAG, "Posted updated templates to LiveData");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to check interaction states", e);
                            });
                    } else {
                        Log.d(TAG, "Skipping interaction check - User: " + (user != null ? user.getUid() : "null") + 
                              ", Templates: " + (newTemplates != null ? newTemplates.size() : "null"));
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

        db.collection("users").document(user.getUid())
            .collection("likes").document(templateId)
            .get()
            .addOnSuccessListener(doc -> likeState.setValue(doc.exists()))
            .addOnFailureListener(e -> likeState.setValue(false));

        return likeState;
    }

    public LiveData<Boolean> getFavoriteState(String templateId) {
        MutableLiveData<Boolean> favoriteState = new MutableLiveData<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            favoriteState.setValue(false);
            return favoriteState;
        }

        db.collection("users").document(user.getUid())
            .collection("favorites").document(templateId)
            .get()
            .addOnSuccessListener(doc -> favoriteState.setValue(doc.exists()))
            .addOnFailureListener(e -> favoriteState.setValue(false));

        return favoriteState;
    }

    public LiveData<Integer> getLikeCount(String templateId) {
        MutableLiveData<Integer> likeCount = new MutableLiveData<>();
        
        db.collection("templates").document(templateId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("likeCount")) {
                    Long count = doc.getLong("likeCount");
                    likeCount.setValue(count != null ? count.intValue() : 0);
                } else {
                    likeCount.setValue(0);
                }
            })
            .addOnFailureListener(e -> likeCount.setValue(0));

        return likeCount;
    }

    public void toggleLike(String templateId, boolean newState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            error.postValue("User must be logged in");
            return;
        }

        String userId = user.getUid();
        if (newState) {
            // Add to likes collection
            db.collection("users").document(userId)
                .collection("likes").document(templateId)
                .set(new HashMap<>())
                .addOnSuccessListener(v -> {
                    // Increment template like count
                    db.collection("templates").document(templateId)
                        .update("likeCount", com.google.firebase.firestore.FieldValue.increment(1));
                })
                .addOnFailureListener(e -> error.postValue("Failed to update like state"));
        } else {
            // Remove from likes collection
            db.collection("users").document(userId)
                .collection("likes").document(templateId)
                .delete()
                .addOnSuccessListener(v -> {
                    // Decrement template like count
                    db.collection("templates").document(templateId)
                        .update("likeCount", com.google.firebase.firestore.FieldValue.increment(-1));
                })
                .addOnFailureListener(e -> error.postValue("Failed to update like state"));
        }
    }

    public void toggleFavorite(String templateId, boolean newState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            error.postValue("User must be logged in");
            return;
        }

        String userId = user.getUid();
        if (newState) {
            // Add to favorites collection
            db.collection("users").document(userId)
                .collection("favorites").document(templateId)
                .set(new HashMap<>())
                .addOnFailureListener(e -> error.postValue("Failed to update favorite state"));
        } else {
            // Remove from favorites collection
            db.collection("users").document(userId)
                .collection("favorites").document(templateId)
                .delete()
                .addOnFailureListener(e -> error.postValue("Failed to update favorite state"));
        }
    }

    private Task<List<Template>> checkInteractionStates(List<Template> templates, String userId) {
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (Template template : templates) {
            String templateId = template.getId();
            if (templateId == null) {
                Log.e(TAG, "Template ID is null, skipping interaction check");
                continue;
            }

            // Check like status
            DocumentReference likeRef = db.collection("users")
                .document(userId)
                .collection("likes")
                .document(templateId);

            Task<Void> likeTask = likeRef.get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isLiked = task.getResult().exists();
                        template.setLiked(isLiked);
                        Log.d(TAG, "Template " + templateId + " like status: " + isLiked);
                    } else {
                        Log.e(TAG, "Failed to get like status for template " + templateId, task.getException());
                    }
                    return null;
                });
            tasks.add(likeTask);
            
            // Check favorite status
            DocumentReference favoriteRef = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(templateId);

            Task<Void> favoriteTask = favoriteRef.get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isFavorited = task.getResult().exists();
                        template.setFavorited(isFavorited);
                        Log.d(TAG, "Template " + templateId + " favorite status: " + isFavorited);
                    } else {
                        Log.e(TAG, "Failed to get favorite status for template " + templateId, task.getException());
                    }
                    return null;
                });
            tasks.add(favoriteTask);

            // Also check the template document for counts
            DocumentReference templateRef = db.collection("templates").document(templateId);
            Task<Void> templateTask = templateRef.get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        Long likeCount = doc.getLong("likeCount");
                        Long favoriteCount = doc.getLong("favoriteCount");
                        
                        if (likeCount != null) {
                            template.setLikeCount(likeCount.intValue());
                            Log.d(TAG, "Template " + templateId + " like count: " + likeCount);
                        }
                        if (favoriteCount != null) {
                            template.setFavoriteCount(favoriteCount.intValue());
                            Log.d(TAG, "Template " + templateId + " favorite count: " + favoriteCount);
                        }
                    } else {
                        Log.e(TAG, "Failed to get counts for template " + templateId, task.getException());
                    }
                    return null;
                });
            tasks.add(templateTask);
        }
        
        return Tasks.whenAll(tasks)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Successfully checked all interaction states");
                } else {
                    Log.e(TAG, "Failed to check some interaction states", task.getException());
                }
                return templates;
            });
    }
}
