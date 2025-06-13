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
import com.ds.eventwish.data.db.AppDatabase;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.utils.AppExecutors;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for template data
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

    private AppDatabase appDatabase;

    // Constants for template state sync 
    private static final long MIN_SYNC_INTERVAL_MS = 30000; // 30 seconds between syncs
    private static final long SERVER_SYNC_INTERVAL_MS = 300000; // 5 minutes between server syncs
    private long lastSyncTimestamp = 0;
    private long lastServerSyncTimestamp = 0;

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
        this.apiService = com.ds.eventwish.data.remote.ApiClient.getClient();
        
        // Initialize Room database for persistent storage
        this.appDatabase = AppDatabase.getInstance(applicationContext);
        
        // Load categories from preferences if available
        loadCategoriesFromPrefs();
        
        templates.postValue(new ArrayList<>());
        categories.postValue(new HashMap<>());
        // Initialize with default categories
        ensureDefaultCategories(null);
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

    /**
     * Load templates from server or cache
     * @param forceRefresh Whether to force a refresh from server
     */
    public void loadTemplates(boolean forceRefresh) {
        loading.postValue(true);
        
        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        // Load templates with user context if available
        loadTemplatesInternal(forceRefresh, user);
    }

    private void loadTemplatesInternal(boolean forceRefresh, FirebaseUser user) {
        // If we're already loading and this is not a forced refresh, skip
        if (loading.getValue() != null && loading.getValue() && !forceRefresh) {
            return;
        }
        
        // Set loading flag
        loading.postValue(true);
        
        // Construct API request parameters
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("limit", String.valueOf(PAGE_SIZE));
        
        if (currentCategory != null && !currentCategory.isEmpty()) {
            params.put("category", currentCategory);
        }
        
        // Check if we should use cached data or fetch from API
        if (!forceRefresh && templates.getValue() != null && !templates.getValue().isEmpty()) {
            // We have cached data, use it
            List<Template> cachedTemplates = templates.getValue();
            Log.d(TAG, "Using cached templates: " + cachedTemplates.size());
            
            // Apply local interaction states on background thread
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    if (user != null) {
                        List<Template> updatedTemplates = applyLocalInteractionStates(cachedTemplates, user.getUid());
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            templates.setValue(updatedTemplates);
                            loading.setValue(false);
                        });
                    } else {
                        // Even without a user, we can still update loading state on main thread
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            loading.setValue(false);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error applying local states", e);
                    // Update loading state on main thread
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        loading.setValue(false);
                    });
                }
            });
            return;
        }
        
        // Get templates from API
        Call<TemplateResponse> call;
        
        if (currentCategory != null && !currentCategory.isEmpty()) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }
        
        setCurrentCall(call);
        
        Log.d(TAG, "Fetching templates from API: page=" + currentPage + ", category=" + (currentCategory == null ? "all" : currentCategory));
        
        currentCall.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                // Process response on a background thread to avoid main thread database operations
                AppExecutors.getInstance().networkIO().execute(() -> {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            final TemplateResponse templateResponse = response.body();
                            List<Template> fetchedTemplates = templateResponse.getTemplates();
                            
                            Log.d(TAG, "Fetched templates: " + (fetchedTemplates != null ? fetchedTemplates.size() : 0));
                            
                                        // Update pagination state
            hasMorePages = templateResponse.isHasMore();
                            
                            // Process templates and update view on main thread
                            if (fetchedTemplates != null && !fetchedTemplates.isEmpty()) {
                                // Update templates in database on background thread
                                try {
                                    insertAll(fetchedTemplates, false);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving templates to database", e);
                                }
                                
                                // Apply local interaction states
                                final List<Template> processedTemplates;
                                if (user != null) {
                                    processedTemplates = applyLocalInteractionStates(fetchedTemplates, user.getUid());
                                } else {
                                    processedTemplates = fetchedTemplates;
                                }
                                
                                // Update UI on main thread
                                AppExecutors.getInstance().mainThread().execute(() -> {
                                    templates.setValue(processedTemplates);
                                    loading.setValue(false);
                                    error.setValue(null);
                                });
                            } else {
                                // No templates returned, update UI on main thread
                                AppExecutors.getInstance().mainThread().execute(() -> {
                                    templates.setValue(new ArrayList<>());
                                    loading.setValue(false);
                                    error.setValue(null);
                                });
                            }
                        } else {
                            // API error, update UI on main thread
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                error.setValue("Error loading templates: " + 
                                            (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                                loading.setValue(false);
                            });
                        }
                    } catch (Exception e) {
                        // Handle exceptions on main thread
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            error.setValue("Exception: " + e.getMessage());
                            loading.setValue(false);
                        });
                        Log.e(TAG, "Error processing template response", e);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                // Process failure on background thread
                AppExecutors.getInstance().networkIO().execute(() -> {
                    try {
                        if (call.isCanceled()) {
                            Log.d(TAG, "Template call was canceled");
                            
                            // Update UI on main thread
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                loading.setValue(false);
                            });
                            return;
                        }
                        
                        Log.e(TAG, "Failed to fetch templates", t);
                        
                        // Try to get templates from database as fallback
                        List<Template> savedTemplates = new ArrayList<>();
                        
                        try {
                            if (appDatabase != null) {
                                if (currentCategory != null && !currentCategory.isEmpty()) {
                                    savedTemplates = appDatabase.templateDao().getTemplatesByCategorySync(currentCategory);
                                } else {
                                    savedTemplates = appDatabase.templateDao().getAllTemplatesSync();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading templates from database", e);
                        }
                        
                        // Apply local interaction states
                        final List<Template> processedTemplates;
                        if (user != null && !savedTemplates.isEmpty()) {
                            processedTemplates = applyLocalInteractionStates(savedTemplates, user.getUid());
                        } else {
                            processedTemplates = savedTemplates;
                        }
                        
                        // Create final reference for lambda
                        final List<Template> finalProcessedTemplates = processedTemplates;
                        final String errorMsg = "Failed to fetch templates: " + t.getMessage() + 
                                         (finalProcessedTemplates.isEmpty() ? "" : " (Using cached data)");
                        
                        // Update UI on main thread
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            if (!finalProcessedTemplates.isEmpty()) {
                                templates.setValue(finalProcessedTemplates);
                                Log.d(TAG, "Using " + finalProcessedTemplates.size() + " cached templates as fallback");
                            }
                            
                            error.setValue(errorMsg);
                            loading.setValue(false);
                        });
                    } catch (Exception e) {
                        // Handle exceptions on main thread
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            error.setValue("Exception during error handling: " + e.getMessage());
                            loading.setValue(false);
                        });
                        Log.e(TAG, "Error handling template call failure", e);
                    }
                });
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

    /**
     * Toggle like status for a template
     * @param templateId The template ID
     * @param newState The new like state (true for liked, false for unliked)
     * @return Task representing the operation
     */
    public Task<Boolean> toggleLike(String templateId, boolean newState) {
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        
        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || templateId == null || templateId.isEmpty()) {
            tcs.setResult(false);
            return tcs.getTask();
        }
        
        String userId = user.getUid();
        
        // Update the template in memory first
        updateTemplateStateLocally(templateId, newState, null);

        // Process on background thread
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Store action locally first for offline support
                storeLikeActionLocally(templateId, userId, newState);
                
                // Get user's token for authentication
                user.getIdToken(true)
                    .addOnSuccessListener(getTokenResult -> {
                        String token = getTokenResult.getToken();
                        String authToken = "Bearer " + token;
                        
                        // Use the logged in user's token for authentication
                        Call<JsonObject> call;
                        if (newState) {
                            call = apiService.likeTemplate(
                                userId,
                                templateId,
                                authToken
                            );
                        } else {
                            call = apiService.unlikeTemplate(
                                userId,
                                templateId,
                                authToken
                            );
                        }
                        
                        // Execute the call
                        call.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                // Process response on background thread
                                AppExecutors.getInstance().diskIO().execute(() -> {
                                    if (response.isSuccessful() && response.body() != null) {
                                        try {
                                            JsonObject result = response.body();
                                            boolean success = result.has("success") && 
                                                result.get("success").getAsBoolean();
                                            
                                            if (success) {
                                                // Update Room database with the new state
                                                updateRoomState(templateId, newState, null);
                                                
                                                // Set task result
                                                tcs.setResult(true);
                                                return;
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing like response", e);
                                        }
                                    }
                                    
                                    // If we got here, something went wrong
                                    Log.e(TAG, "Like operation failed - status code: " + 
                                        response.code() + ", message: " + response.message());
                                    
                                    // Return result but don't revert the UI change
                                    // The user already sees the change, and we've stored it locally
                                    tcs.setResult(false);
                                });
                            }
                            
                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                // Process failure on background thread
                                AppExecutors.getInstance().diskIO().execute(() -> {
                                    Log.e(TAG, "Failed to toggle like: " + t.getMessage(), t);
                                    
                                    // Return result but don't revert the UI change
                                    // The user already sees the change, and we've stored it locally
                                    tcs.setResult(false);
                                });
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get user token", e);
                        tcs.setResult(false);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error in toggleLike", e);
                tcs.setResult(false);
            }
        });
        
        return tcs.getTask();
    }
    
    /**
     * Toggle favorite status for a template
     * @param templateId The template ID
     * @param newState The new favorite state (true for favorited, false for unfavorited)
     * @return Task representing the operation
     */
    public Task<Boolean> toggleFavorite(String templateId, boolean newState) {
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        
        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || templateId == null || templateId.isEmpty()) {
            tcs.setResult(false);
            return tcs.getTask();
        }
        
        String userId = user.getUid();
        
        // Update the template in memory first
        updateTemplateStateLocally(templateId, null, newState);

        // Process on background thread
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Store action locally first for offline support
                storeFavoriteActionLocally(templateId, userId, newState);
                
                // Get user's token for authentication
                user.getIdToken(true)
                    .addOnSuccessListener(getTokenResult -> {
                        String token = getTokenResult.getToken();
                        String authToken = "Bearer " + token;
                        
                        // Use the logged in user's token for authentication
                        Call<JsonObject> call;
                        if (newState) {
                            call = apiService.addToFavorites(
                                userId,
                                templateId,
                                authToken
                            );
                        } else {
                            call = apiService.removeFromFavorites(
                                userId,
                                templateId,
                                authToken
                            );
                        }
                        
                        // Execute the call
                        call.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                // Process response on background thread
                                AppExecutors.getInstance().diskIO().execute(() -> {
                                    if (response.isSuccessful() && response.body() != null) {
                                        try {
                                            JsonObject result = response.body();
                                            boolean success = result.has("success") && 
                                                result.get("success").getAsBoolean();
                                            
                                            if (success) {
                                                // Update Room database with the new state
                                                updateRoomState(templateId, null, newState);
                                                
                                                // Set task result
                                                tcs.setResult(true);
                                                return;
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing favorite response", e);
                                        }
                                    }
                                    
                                    // If we got here, something went wrong
                                    Log.e(TAG, "Favorite operation failed - status code: " + 
                                        response.code() + ", message: " + response.message());
                                    
                                    // Return result but don't revert the UI change
                                    // The user already sees the change, and we've stored it locally
                                    tcs.setResult(false);
                                });
                            }
                            
                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                // Process failure on background thread
                                AppExecutors.getInstance().diskIO().execute(() -> {
                                    Log.e(TAG, "Failed to toggle favorite: " + t.getMessage(), t);
                                    
                                    // Return result but don't revert the UI change
                                    // The user already sees the change, and we've stored it locally
                                    tcs.setResult(false);
                                });
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get user token", e);
                        tcs.setResult(false);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error in toggleFavorite", e);
                tcs.setResult(false);
            }
        });
        
        return tcs.getTask();
    }
    
    /**
     * Update Room database with template state
     * @param templateId Template ID
     * @param isLiked Like state or null if not changing
     * @param isFavorited Favorite state or null if not changing
     */
    private void updateRoomState(String templateId, Boolean isLiked, Boolean isFavorited) {
        if (templateId == null || (isLiked == null && isFavorited == null) || appDatabase == null) {
            return;
        }
        
        // Always use background thread for database operations
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // First check if the template exists
                Template template = appDatabase.templateDao().getTemplateByIdSync(templateId);
                
                if (template != null) {
                    // Update like state if provided
                    if (isLiked != null) {
                        appDatabase.templateDao().updateLikeState(templateId, isLiked);
                        Log.d(TAG, "Updated like state in Room database: templateId=" + templateId + ", isLiked=" + isLiked);
                    }
                    
                    // Update favorite state if provided
                    if (isFavorited != null) {
                        appDatabase.templateDao().updateFavoriteState(templateId, isFavorited);
                        Log.d(TAG, "Updated favorite state in Room database: templateId=" + templateId + ", isFavorited=" + isFavorited);
                    }
                } else {
                    // Template doesn't exist in database yet, try to fetch it from API and save
                    Log.d(TAG, "Template " + templateId + " not found in database, fetching from API");
                    
                    try {
                        // Synchronous API call on background thread is OK
                        Response<Template> response = apiService.getTemplateById(templateId).execute();
                        
                        if (response.isSuccessful() && response.body() != null) {
                            Template fetchedTemplate = response.body();
                            
                            // Set the states before saving
                            if (isLiked != null) {
                                fetchedTemplate.setLiked(isLiked);
                            }
                            
                            if (isFavorited != null) {
                                fetchedTemplate.setFavorited(isFavorited);
                            }
                            
                            // Save to database
                            appDatabase.templateDao().insert(fetchedTemplate);
                            Log.d(TAG, "Fetched and saved template " + templateId + " to database with states");
                        } else {
                            Log.w(TAG, "Failed to fetch template " + templateId + " from API: " + 
                                  (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching template " + templateId + " from API", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating Room state for template " + templateId, e);
            }
        });
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
     * Store like action locally for offline support
     * @param templateId Template ID
     * @param userId User ID
     * @param isLike True for like, false for unlike
     */
    private void storeLikeActionLocally(String templateId, String userId, boolean isLike) {
        if (templateId == null || userId == null || applicationContext == null) {
            return;
        }
        
        // Store in SharedPreferences first for faster access
        try {
            // Create a JSON object with the action data
            JsonObject actionData = new JsonObject();
            actionData.addProperty("templateId", templateId);
            actionData.addProperty("userId", userId);
            actionData.addProperty("action", isLike ? "like" : "unlike");
            actionData.addProperty("timestamp", System.currentTimeMillis());
            
            String jsonStr = new Gson().toJson(actionData);
            
            // Store in SharedPreferences
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(templateId + "_" + userId, jsonStr);
            editor.apply();
            
            // Also store in a user-specific set for faster lookup
            SharedPreferences userPrefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> likedIds = userPrefs.getStringSet("liked_" + userId, new HashSet<>());
            Set<String> updatedLikedIds = new HashSet<>(likedIds != null ? likedIds : new HashSet<>());
            
            if (isLike) {
                updatedLikedIds.add(templateId);
            } else {
                updatedLikedIds.remove(templateId);
            }
            
            userPrefs.edit().putStringSet("liked_" + userId, updatedLikedIds).apply();
            
            // Also update in Room database for persistence
            updateRoomState(templateId, isLike, null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error storing like action locally", e);
        }
    }
    
    /**
     * Store favorite action locally for offline support
     * @param templateId Template ID
     * @param userId User ID
     * @param isFavorite True for favorite, false for unfavorite
     */
    private void storeFavoriteActionLocally(String templateId, String userId, boolean isFavorite) {
        if (templateId == null || userId == null || applicationContext == null) {
            return;
        }
        
        // Store in SharedPreferences first for faster access
        try {
            // Create a JSON object with the action data
            JsonObject actionData = new JsonObject();
            actionData.addProperty("templateId", templateId);
            actionData.addProperty("userId", userId);
            actionData.addProperty("action", isFavorite ? "favorite" : "unfavorite");
            actionData.addProperty("timestamp", System.currentTimeMillis());
            
            String jsonStr = new Gson().toJson(actionData);
            
            // Store in SharedPreferences
            SharedPreferences prefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(templateId + "_" + userId, jsonStr);
            editor.apply();
            
            // Also store in a user-specific set for faster lookup
            SharedPreferences userPrefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> favoritedIds = userPrefs.getStringSet("favorited_" + userId, new HashSet<>());
            Set<String> updatedFavoritedIds = new HashSet<>(favoritedIds != null ? favoritedIds : new HashSet<>());
            
            if (isFavorite) {
                updatedFavoritedIds.add(templateId);
            } else {
                updatedFavoritedIds.remove(templateId);
            }
            
            userPrefs.edit().putStringSet("favorited_" + userId, updatedFavoritedIds).apply();
            
            // Also update in Room database for persistence
            updateRoomState(templateId, null, isFavorite);
            
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
        
        // First try to get data from SharedPreferences (fast, can be done on main thread)
        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            
            // First try to load likes from SharedPreferences
            Set<String> likedIds = prefs.getStringSet("liked_" + userId, new HashSet<>());
            if (likedIds != null && !likedIds.isEmpty()) {
                Log.d(TAG, "Found " + likedIds.size() + " liked templates in SharedPreferences");
                for (Template template : templates) {
                    if (likedIds.contains(template.getId())) {
                        template.setLiked(true);
                    }
                }
            }
            
            // Then try to load favorites from SharedPreferences
            Set<String> favoritedIds = prefs.getStringSet("favorited_" + userId, new HashSet<>());
            if (favoritedIds != null && !favoritedIds.isEmpty()) {
                Log.d(TAG, "Found " + favoritedIds.size() + " favorited templates in SharedPreferences");
                for (Template template : templates) {
                    if (favoritedIds.contains(template.getId())) {
                        template.setFavorited(true);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading template states from SharedPreferences", e);
        }
        
        // Try to get data from Room database (more accurate but needs background thread)
        if (appDatabase == null) {
            Log.w(TAG, "Room database not initialized, skipping database operations");
            return templates;
        }
        
        // Create a copy of the templates list to avoid ConcurrentModificationException
        List<Template> resultTemplates = new ArrayList<>(templates);
        
        // Use CountDownLatch to make this synchronous but still off the main thread
        CountDownLatch latch = new CountDownLatch(1);
        
        // Use a thread-safe list to collect results from background thread
        AtomicReference<List<Template>> updatedTemplatesRef = new AtomicReference<>(resultTemplates);
        
        // Process database operations on background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Get all liked templates from database
                List<Template> likedTemplates = appDatabase.templateDao().getLikedTemplatesSync();
                if (likedTemplates != null && !likedTemplates.isEmpty()) {
                    Log.d(TAG, "Found " + likedTemplates.size() + " liked templates in Room database");
                    
                    // Create a set of liked template IDs for faster lookup
                    Set<String> likedIdsSet = new HashSet<>();
                    for (Template template : likedTemplates) {
                        likedIdsSet.add(template.getId());
                    }
                    
                    // Update liked status for all templates
                    for (Template template : resultTemplates) {
                        if (likedIdsSet.contains(template.getId())) {
                            template.setLiked(true);
                        }
                    }
                }
                
                // Get all favorited templates from database
                List<Template> favoritedTemplates = appDatabase.templateDao().getFavoritedTemplatesSync();
                if (favoritedTemplates != null && !favoritedTemplates.isEmpty()) {
                    Log.d(TAG, "Found " + favoritedTemplates.size() + " favorited templates in Room database");
                    
                    // Create a set of favorited template IDs for faster lookup
                    Set<String> favoritedIdsSet = new HashSet<>();
                    for (Template template : favoritedTemplates) {
                        favoritedIdsSet.add(template.getId());
                    }
                    
                    // Update favorited status for all templates
                    for (Template template : resultTemplates) {
                        if (favoritedIdsSet.contains(template.getId())) {
                            template.setFavorited(true);
                        }
                    }
                }
                
                // Update the atomic reference with the updated templates
                updatedTemplatesRef.set(resultTemplates);
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading template states from Room database", e);
            } finally {
                // Signal that background work is complete
                latch.countDown();
            }
        });
        
        try {
            // Wait for database operations to complete
            // Use a timeout to prevent deadlock
            boolean completed = latch.await(500, TimeUnit.MILLISECONDS);
            if (!completed) {
                Log.w(TAG, "Database operations timed out, returning partially updated templates");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for database operations", e);
            Thread.currentThread().interrupt();
        }
        
        // Return the updated templates
        return updatedTemplatesRef.get();
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

    /**
     * Sync template states with local storage at app startup
     * This should be called when the app starts to ensure template states are properly loaded
     * @param userId Current user ID
     */
    public void syncTemplateStatesAtStartup(String userId) {
        if (userId == null || applicationContext == null) {
            Log.d(TAG, "Cannot sync template states: userId=" + (userId == null ? "null" : userId) +
                  ", applicationContext=" + (applicationContext == null ? "null" : "available"));
            return;
        }

        // Implement debounce mechanism - avoid syncing too frequently
        long currentTimeMs = System.currentTimeMillis();
        if (currentTimeMs - lastSyncTimestamp < MIN_SYNC_INTERVAL_MS) {
            Log.d(TAG, "Skipping template state sync - called too frequently (last sync was " + 
                  (currentTimeMs - lastSyncTimestamp) + "ms ago)");
            return;
        }
        
        // Update the last sync timestamp
        lastSyncTimestamp = currentTimeMs;
        
        // Use LiveData observer to ensure we sync when templates are actually loaded
        LiveData<List<Template>> templatesLiveData = getTemplates();
        
        // Check if templates are already loaded to avoid unnecessary observer
        List<Template> currentTemplates = templatesLiveData.getValue();
        if (currentTemplates != null && !currentTemplates.isEmpty()) {
            // Templates are already loaded, apply states immediately
            performTemplateStateSync(userId, currentTemplates);
            
            // Also fetch states from server for cross-device sync, but only if it's been a while
            if (currentTimeMs - lastServerSyncTimestamp >= SERVER_SYNC_INTERVAL_MS) {
                fetchTemplateStatesFromServer(userId);
                lastServerSyncTimestamp = currentTimeMs;
            }
            return;
        }
        
        // Use a one-time observer to sync states when templates become available
        androidx.lifecycle.Observer<List<Template>> observer = new androidx.lifecycle.Observer<List<Template>>() {
            @Override
            public void onChanged(List<Template> templatesList) {
                if (templatesList != null && !templatesList.isEmpty()) {
                    // Templates are loaded, apply states
                    performTemplateStateSync(userId, templatesList);
                    
                    // Also fetch states from server for cross-device sync, but only if it's been a while
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastServerSyncTimestamp >= SERVER_SYNC_INTERVAL_MS) {
                        fetchTemplateStatesFromServer(userId);
                        lastServerSyncTimestamp = currentTime;
                    }
                    
                    // Remove observer to avoid multiple updates
                    templatesLiveData.removeObserver(this);
                }
            }
        };
        
        // Make sure observer is added on the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            templatesLiveData.observeForever(observer);
        } else {
            AppExecutors.getInstance().mainThread().execute(() -> {
                templatesLiveData.observeForever(observer);
            });
        }
        
        // Set a fallback to ensure we don't keep the observer forever
        AppExecutors.getInstance().mainThread().execute(() -> {
            new Handler().postDelayed(() -> {
                // Check if current templates exist
                List<Template> fallbackTemplates = templatesLiveData.getValue();
                if (fallbackTemplates != null && !fallbackTemplates.isEmpty()) {
                    // Templates exist, sync states and remove observer
                    performTemplateStateSync(userId, fallbackTemplates);
                    
                    // Also fetch states from server for cross-device sync, but only if it's been a while
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastServerSyncTimestamp >= SERVER_SYNC_INTERVAL_MS) {
                        fetchTemplateStatesFromServer(userId);
                        lastServerSyncTimestamp = currentTime;
                    }
                    
                    templatesLiveData.removeObserver(observer);
                }
            }, 10000); // 10 second timeout
        });
    }
    
    /**
     * Fetch template states from server for cross-device sync
     * @param userId Current user ID
     */
    private void fetchTemplateStatesFromServer(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot fetch template states: userId is null or empty");
            return;
        }
        
        // Get the current Firebase user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot fetch template states: not authenticated with Firebase");
            return;
        }
        
        Log.d(TAG, "Fetching template states from server for user: " + userId);
        
        // Use background thread for network operations
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Get a fresh token
                user.getIdToken(true)
                    .addOnSuccessListener(getTokenResult -> {
                        String token = getTokenResult.getToken();
                        String authToken = "Bearer " + token;
                        
                        // Fetch likes from server
                        fetchUserLikesFromServer(userId, authToken);
                        
                        // Fetch favorites from server
                        fetchUserFavoritesFromServer(userId, authToken);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get authentication token for template state sync", e);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching template states from server", e);
            }
        });
    }
    
    /**
     * Fetch user likes from server and sync to local storage
     * @param userId Current user ID
     * @param authToken Firebase authentication token
     */
    private void fetchUserLikesFromServer(String userId, String authToken) {
        if (userId == null || authToken == null) {
            Log.e(TAG, "Cannot fetch user likes: userId or authToken is null");
            return;
        }
        
        // Use enqueue instead of execute to avoid NetworkOnMainThreadException
        apiService.getUserLikes(userId, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("likes") && body.get("likes").isJsonArray()) {
                        // Process on background thread to avoid blocking UI
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                JsonArray likesArray = body.getAsJsonArray("likes");
                                Set<String> likedTemplateIds = new HashSet<>();
                                
                                for (JsonElement element : likesArray) {
                                    if (element.isJsonPrimitive()) {
                                        likedTemplateIds.add(element.getAsString());
                                        // Store the like action locally
                                        storeLikeActionLocally(element.getAsString(), userId, true);
                                    }
                                }
                                
                                Log.d(TAG, "Synced " + likedTemplateIds.size() + " liked templates from server");
                                
                                // Store the complete set in SharedPreferences
                                if (applicationContext != null) {
                                    SharedPreferences prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                                    prefs.edit().putStringSet("liked_" + userId, likedTemplateIds).apply();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user likes response", e);
                            }
                        });
                    } else {
                        Log.w(TAG, "Invalid response format for user likes");
                    }
                } else {
                    Log.w(TAG, "Failed to fetch user likes from server: " + 
                          (response.code() + " - " + (response.errorBody() != null ? 
                           response.errorBody().toString() : "Unknown error")));
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching user likes from server", t);
            }
        });
    }
    
    /**
     * Fetch user favorites from server and sync to local storage
     * @param userId Current user ID
     * @param authToken Firebase authentication token
     */
    private void fetchUserFavoritesFromServer(String userId, String authToken) {
        if (userId == null || authToken == null) {
            Log.e(TAG, "Cannot fetch user favorites: userId or authToken is null");
            return;
        }
        
        // Use enqueue instead of execute to avoid NetworkOnMainThreadException
        apiService.getUserFavorites(userId, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("favorites") && body.get("favorites").isJsonArray()) {
                        // Process on background thread to avoid blocking UI
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                JsonArray favoritesArray = body.getAsJsonArray("favorites");
                                Set<String> favoritedTemplateIds = new HashSet<>();
                                
                                for (JsonElement element : favoritesArray) {
                                    if (element.isJsonPrimitive()) {
                                        favoritedTemplateIds.add(element.getAsString());
                                        // Store the favorite action locally
                                        storeFavoriteActionLocally(element.getAsString(), userId, true);
                                    }
                                }
                                
                                Log.d(TAG, "Synced " + favoritedTemplateIds.size() + " favorited templates from server");
                                
                                // Store the complete set in SharedPreferences
                                if (applicationContext != null) {
                                    SharedPreferences prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                                    prefs.edit().putStringSet("favorited_" + userId, favoritedTemplateIds).apply();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user favorites response", e);
                            }
                        });
                    } else {
                        Log.w(TAG, "Invalid response format for user favorites");
                    }
                } else {
                    Log.w(TAG, "Failed to fetch user favorites from server: " + 
                          (response.code() + " - " + (response.errorBody() != null ? 
                           response.errorBody().toString() : "Unknown error")));
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching user favorites from server", t);
            }
        });
    }

    /**
     * Private helper method to actually perform the template state sync
     * @param userId Current user ID
     * @param templatesList List of templates to sync states for
     */
    private void performTemplateStateSync(String userId, List<Template> templatesList) {
        if (templatesList == null || templatesList.isEmpty()) {
            Log.d(TAG, "No templates to sync");
            return;
        }
        
        Log.d(TAG, "Syncing template states for " + templatesList.size() + " templates");
        
        // Use AppExecutors to run the database operations on a background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Get liked templates from Room database
                List<String> likedTemplateIds = new ArrayList<>();
                if (appDatabase != null) {
                    List<Template> likedTemplates = appDatabase.templateDao().getLikedTemplatesSync();
                    if (likedTemplates != null && !likedTemplates.isEmpty()) {
                        for (Template template : likedTemplates) {
                            likedTemplateIds.add(template.getId());
                        }
                        Log.d(TAG, "Found " + likedTemplateIds.size() + " liked templates in Room database");
                    }
                }
                
                // Get favorited templates from Room database
                List<String> favoritedTemplateIds = new ArrayList<>();
                if (appDatabase != null) {
                    List<Template> favoritedTemplates = appDatabase.templateDao().getFavoritedTemplatesSync();
                    if (favoritedTemplates != null && !favoritedTemplates.isEmpty()) {
                        for (Template template : favoritedTemplates) {
                            favoritedTemplateIds.add(template.getId());
                        }
                        Log.d(TAG, "Found " + favoritedTemplateIds.size() + " favorited templates in Room database");
                    }
                }
                
                // Get like actions from SharedPreferences
                Map<String, Boolean> likeActions = new HashMap<>();
                SharedPreferences likePrefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
                Map<String, ?> allLikes = likePrefs.getAll();
                
                for (Map.Entry<String, ?> entry : allLikes.entrySet()) {
                    try {
                        String jsonStr = (String) entry.getValue();
                        JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                        
                        if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                            String templateId = actionData.get("templateId").getAsString();
                            String storedUserId = actionData.get("userId").getAsString();
                            String action = actionData.get("action").getAsString();
                            
                            // Only apply if this is for the current user
                            if (userId.equals(storedUserId)) {
                                boolean isLike = "like".equals(action);
                                likeActions.put(templateId, isLike);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing locally stored like action", e);
                    }
                }
                
                // Get favorite actions from SharedPreferences
                Map<String, Boolean> favoriteActions = new HashMap<>();
                SharedPreferences favoritePrefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
                Map<String, ?> allFavorites = favoritePrefs.getAll();
                
                for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                    try {
                        String jsonStr = (String) entry.getValue();
                        JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                        
                        if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                            String templateId = actionData.get("templateId").getAsString();
                            String storedUserId = actionData.get("userId").getAsString();
                            String action = actionData.get("action").getAsString();
                            
                            // Only apply if this is for the current user
                            if (userId.equals(storedUserId)) {
                                boolean isFavorite = "favorite".equals(action);
                                favoriteActions.put(templateId, isFavorite);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing locally stored favorite action", e);
                    }
                }
                
                // Apply states to templates
                final List<Template> updatedTemplates = new ArrayList<>();
                
                for (Template template : templatesList) {
                    String templateId = template.getId();
                    
                    // Apply like state
                    if (likedTemplateIds.contains(templateId) || Boolean.TRUE.equals(likeActions.get(templateId))) {
                        template.setLiked(true);
                    } else if (likeActions.containsKey(templateId)) {
                        template.setLiked(likeActions.get(templateId));
                    }
                    
                    // Apply favorite state
                    if (favoritedTemplateIds.contains(templateId) || Boolean.TRUE.equals(favoriteActions.get(templateId))) {
                        template.setFavorited(true);
                    } else if (favoriteActions.containsKey(templateId)) {
                        template.setFavorited(favoriteActions.get(templateId));
                    }
                    
                    updatedTemplates.add(template);
                }
                
                // Update LiveData on main thread
                AppExecutors.getInstance().mainThread().execute(() -> {
                    templates.setValue(updatedTemplates);
                    Log.d(TAG, "Template states synced at app startup for " + updatedTemplates.size() + " templates");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error syncing template states", e);
            }
        });
    }

    // no change needed

    /**
     * Helper method to get like actions from SharedPreferences
     */
    private Map<String, Boolean> getLikeActionsFromPrefs(String userId) {
        Map<String, Boolean> likeActions = new HashMap<>();
        try {
            SharedPreferences likePrefs = applicationContext.getSharedPreferences("pending_like_actions", Context.MODE_PRIVATE);
            Map<String, ?> allLikes = likePrefs.getAll();
            
            for (Map.Entry<String, ?> entry : allLikes.entrySet()) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                        String templateId = actionData.get("templateId").getAsString();
                        String storedUserId = actionData.get("userId").getAsString();
                        String action = actionData.get("action").getAsString();
                        
                        // Only apply if this is for the current user
                        if (userId.equals(storedUserId)) {
                            boolean isLike = "like".equals(action);
                            likeActions.put(templateId, isLike);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing locally stored like action", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading like actions from SharedPreferences", e);
        }
        return likeActions;
    }

    /**
     * Helper method to get favorite actions from SharedPreferences
     */
    private Map<String, Boolean> getFavoriteActionsFromPrefs(String userId) {
        Map<String, Boolean> favoriteActions = new HashMap<>();
        try {
            SharedPreferences favoritePrefs = applicationContext.getSharedPreferences("pending_favorite_actions", Context.MODE_PRIVATE);
            Map<String, ?> allFavorites = favoritePrefs.getAll();
            
            for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JsonObject actionData = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    if (actionData.has("templateId") && actionData.has("userId") && actionData.has("action")) {
                        String templateId = actionData.get("templateId").getAsString();
                        String storedUserId = actionData.get("userId").getAsString();
                        String action = actionData.get("action").getAsString();
                        
                        // Only apply if this is for the current user
                        if (userId.equals(storedUserId)) {
                            boolean isFavorite = "favorite".equals(action);
                            favoriteActions.put(templateId, isFavorite);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing locally stored favorite action", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading favorite actions from SharedPreferences", e);
        }
        return favoriteActions;
    }
}
