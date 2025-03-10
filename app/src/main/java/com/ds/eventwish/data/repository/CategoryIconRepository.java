package com.ds.eventwish.data.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing category icons
 */
public class CategoryIconRepository {
    private static final String TAG = "CategoryIconRepository";
    private static CategoryIconRepository instance;
    private final ApiService apiService;
    private final MutableLiveData<List<CategoryIcon>> categoryIcons = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final Map<String, CategoryIcon> categoryIconMap = new HashMap<>();
    private boolean isInitialized = false;

    private CategoryIconRepository() {
        apiService = ApiClient.getClient();
        categoryIcons.setValue(new ArrayList<>());
    }

    public static synchronized CategoryIconRepository getInstance() {
        if (instance == null) {
            instance = new CategoryIconRepository();
        }
        return instance;
    }

    public LiveData<List<CategoryIcon>> getCategoryIcons() {
        if (!isInitialized) {
            loadCategoryIcons();
        }
        return categoryIcons;
    }

    public LiveData<String> getError() {
        return error;
    }

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
     * Load category icons from the API
     */
    public void loadCategoryIcons() {
        if (loading.getValue()) {
            return;
        }
        
        loading.setValue(true);
        Log.d(TAG, "Loading category icons from API");
        
        Call<CategoryIconResponse> call = apiService.getCategoryIcons();
        call.enqueue(new Callback<CategoryIconResponse>() {
            @Override
            public void onResponse(Call<CategoryIconResponse> call, Response<CategoryIconResponse> response) {
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
                    } else {
                        error.setValue("Failed to load category icons");
                        Log.e(TAG, "Failed to load category icons: " + response.message());
                        // Add fallback icons since API call failed
                        addFallbackIcons();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing category icons response: " + e.getMessage(), e);
                    // Add fallback icons since parsing failed
                    addFallbackIcons();
                }
            }

            @Override
            public void onFailure(Call<CategoryIconResponse> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
                Log.e(TAG, "Error loading category icons: " + t.getMessage());
                // Add fallback icons since API call failed
                addFallbackIcons();
            }
        });
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
        
        // Load fresh data from API
        loadCategoryIcons();
        
        // Notify observers that we're refreshing
        loading.setValue(true);
    }
}