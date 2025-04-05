package com.ds.eventwish.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapter for displaying category items in horizontal RecyclerView
 * with optimized icon loading and caching
 */
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoriesAdapter";
    private static final int MAX_VISIBLE_CATEGORIES = 8;
    
    // Drawable cache to avoid frequent image loading
    private static final int ICON_CACHE_SIZE = 20;
    private final LruCache<String, String> iconUrlCache = new LruCache<>(ICON_CACHE_SIZE);
    
    // Track failed icon loads to avoid repeated failures
    private final Set<String> failedIconUrls = new HashSet<>();
    
    // Categories data 
    private final List<String> categories = new ArrayList<>();
    private final List<String> visibleCategories = new ArrayList<>();
    
    // Selection state
    private int selectedPosition = 0;
    
    // Callbacks
    private OnCategoryClickListener listener;
    private OnMoreClickListener moreClickListener;
    
    // Dependencies
    private final CategoryIconRepository categoryIconRepository;
    private final Context context;
    
    // State flags
    private final AtomicBoolean categoryIconsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private boolean preventCategoryChanges = false;
    
    // Reusable request options
    private final RequestOptions iconRequestOptions;

    /**
     * Callback for category selection events
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(String category, int position);
    }

    /**
     * Callback for "More" button clicks to show additional categories
     */
    public interface OnMoreClickListener {
        void onMoreClick(List<String> remainingCategories);
    }

    /**
     * Default constructor with context
     * @param context The context for loading resources
     */
    public CategoriesAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.categoryIconRepository = CategoryIconRepository.getInstance(this.context);
        
        // Initialize Glide request options for reuse
        this.iconRequestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .override(80, 80); // Small icon size optimized for category items
                
        Log.d(TAG, "🏗️ CategoriesAdapter initialized with context");
    }
    
    /**
     * Legacy default constructor - avoid using
     * @deprecated Use CategoriesAdapter(Context) instead
     */
    @Deprecated
    public CategoriesAdapter() {
        // Try to get application context as a fallback
        Context appContext = null;
        try {
            appContext = com.ds.eventwish.EventWishApplication.getAppContext();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting application context", e);
        }
        
        this.context = appContext;
        this.categoryIconRepository = (appContext != null) 
            ? CategoryIconRepository.getInstance(appContext)
            : CategoryIconRepository.getInstance();
            
        // Initialize Glide request options
        this.iconRequestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .override(80, 80);
                
        Log.w(TAG, "⚠️ Using deprecated constructor without context - icon loading may fail");
    }

    /**
     * Set the category click listener
     */
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    /**
     * Set the "More" button click listener
     */
    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.moreClickListener = listener;
    }

    /**
     * Update adapter with new categories
     * @param newCategories List of category names to display
     */
    public void updateCategories(List<String> newCategories) {
        if (newCategories == null) {
            Log.w(TAG, "⚠️ Received null category list");
            return;
        }
        
        // Skip updates if we're preventing changes or a refresh is in progress
        if (preventCategoryChanges) {
            Log.d(TAG, "🔒 Category changes prevented, skipping update");
            return;
        }
        
        if (refreshInProgress.get()) {
            Log.d(TAG, "🔄 Refresh already in progress, skipping update");
            return;
        }
        
        // Check if categories actually changed to avoid unnecessary updates
        if (categoriesEqual(categories, newCategories)) {
            Log.d(TAG, "✅ Categories unchanged, skipping update");
            return;
        }
        
        // Set refresh flag to prevent concurrent updates
        refreshInProgress.set(true);
        
        try {
            Log.d(TAG, "🔄 Updating categories: " + newCategories.size() + " categories");
            
            // Clear and update the categories list
            categories.clear();
            categories.addAll(newCategories);
            
            // Update visible categories based on mode
            if (moreClickListener == null) {
                // Bottom sheet mode - show all categories
                visibleCategories.clear();
                visibleCategories.addAll(categories);
                Log.d(TAG, "📋 Bottom sheet mode: showing all " + categories.size() + " categories");
            } else {
                // Main adapter mode - apply visible categories logic
                updateVisibleCategories();
                Log.d(TAG, "📋 Main adapter mode: showing " + visibleCategories.size() + " of " + categories.size() + " categories");
            }
            
            // Initialize category icons if not already done
            if (!categoryIconsInitialized.get()) {
                categoryIconRepository.refreshCategoryIcons();
                categoryIconsInitialized.set(true);
            }
            
            // Notify to update the UI
            notifyDataSetChanged();
        } finally {
            // Reset refresh flag
            refreshInProgress.set(false);
        }
    }
    
    /**
     * Check if two category lists are equal in content and order
     */
    private boolean categoriesEqual(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Prevent category changes during operations like scrolling or loading more data
     * @param prevent Whether to prevent category updates
     */
    public void preventCategoryChanges(boolean prevent) {
        this.preventCategoryChanges = prevent;
        Log.d(TAG, "Category changes " + (prevent ? "prevented" : "allowed"));
    }

    /**
     * Update visible categories list from the full list
     */
    private void updateVisibleCategories() {
        String selectedCategory = selectedPosition < visibleCategories.size() ? 
                visibleCategories.get(selectedPosition) : null;
                
        visibleCategories.clear();
        
        // Always include "All" category if it exists
        if (categories.contains("All")) {
            visibleCategories.add("All");
        }
        
        // Add the selected category if not "All" and not already included
        if (selectedCategory != null && !selectedCategory.equals("All") && 
                categories.contains(selectedCategory) && 
                !visibleCategories.contains(selectedCategory)) {
            visibleCategories.add(selectedCategory);
        }
        
        // Create remaining categories list excluding already added ones
        // Important: preserve original order from 'categories' list
        List<String> remainingCategories = new ArrayList<>();
        for (String category : categories) {
            if (!visibleCategories.contains(category)) {
                remainingCategories.add(category);
            }
        }
        
        // Calculate remaining slots
        int remainingSlots = MAX_VISIBLE_CATEGORIES - 1 - visibleCategories.size();
        
        // Add other categories (preserving original order)
        for (int i = 0; i < Math.min(remainingSlots, remainingCategories.size()); i++) {
            visibleCategories.add(remainingCategories.get(i));
        }
        
        // Add "More" if necessary
        if (remainingCategories.size() > remainingSlots) {
            visibleCategories.add("More");
        }
    }

    /**
     * Update the selected position and refresh affected items
     */
    public void setSelectedPosition(int position) {
        if (position < 0 || position >= visibleCategories.size()) {
            Log.w(TAG, "⚠️ Invalid position: " + position + ", ignoring selection");
            return;
        }
        
        // Only update if position changed
        if (selectedPosition != position) {
            // Set prevention flag temporarily during selection change
            boolean wasPreventingChanges = preventCategoryChanges;
            preventCategoryChanges = true;
            
            try {
                int previousPosition = selectedPosition;
                selectedPosition = position;
                
                // Only refresh affected items instead of the whole list
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                Log.d(TAG, "🔄 Selection changed from " + previousPosition + " to " + selectedPosition);
            } finally {
                // Restore previous prevention state
                preventCategoryChanges = wasPreventingChanges;
            }
        }
    }

    /**
     * Update the selected category
     * @param category The name of the category to select
     */
    public void updateSelectedCategory(String category) {
        if (category == null) {
            // Default to first position (usually "All")
            setSelectedPosition(0);
            return;
        }
        
        // Check if this category is already in the visible list
        int existingPosition = visibleCategories.indexOf(category);
        if (existingPosition >= 0) {
            // Just update the selection without changing the list order
            setSelectedPosition(existingPosition);
            Log.d(TAG, "📌 Selected existing category: " + category + " at position " + existingPosition);
            return;
        }
        
        // At this point, the category isn't in the visible list
        Log.d(TAG, "🔄 Category not in visible list, updating: " + category);
        
        // Save old selected position for UI update
        String oldSelected = selectedPosition < visibleCategories.size() ? 
                visibleCategories.get(selectedPosition) : null;
                
        // Create a copy of the current visible list to detect changes
        List<String> oldVisibleList = new ArrayList<>(visibleCategories);
        
        // Preserve original categories and their order
        List<String> originalCategories = new ArrayList<>(categories);
        
        // Update visible categories list to include the selected category
        updateVisibleCategoriesWithSelected(category);
        
        // Find the new position of the selected category
        int newPosition = visibleCategories.indexOf(category);
        if (newPosition >= 0) {
            selectedPosition = newPosition;
            
            // Compare visible lists to determine refresh approach
            boolean minorChange = oldVisibleList.size() == visibleCategories.size() && 
                    Math.abs(oldVisibleList.indexOf(oldSelected) - newPosition) <= 2;
                    
            if (minorChange) {
                // Just update the selection appearance for minor changes
                notifyItemChanged(oldVisibleList.indexOf(oldSelected));
                notifyItemChanged(selectedPosition);
                Log.d(TAG, "🔄 Minor update for category selection");
            } else {
                // Full refresh for significant changes in the visible list
                notifyDataSetChanged();
                Log.d(TAG, "🔄 Full refresh of categories adapter");
            }
        } else {
            Log.e(TAG, "⚠️ Selected category not in visible list after update!");
        }
    }
    
    /**
     * Update visible categories ensuring a specific category is visible
     */
    private void updateVisibleCategoriesWithSelected(String category) {
        visibleCategories.clear();
        
        // Always include "All" category if it exists
        if (categories.contains("All")) {
            visibleCategories.add("All");
        }
        
        // Add the selected category if not "All" and not already included
        if (!category.equals("All") && categories.contains(category) &&
                !visibleCategories.contains(category)) {
            visibleCategories.add(category);
        }
        
        // Create remaining categories list excluding already added ones
        // Important: preserve original order from 'categories' list
        List<String> remainingCategories = new ArrayList<>();
        for (String cat : categories) {
            if (!visibleCategories.contains(cat)) {
                remainingCategories.add(cat);
            }
        }
        
        // Calculate remaining slots
        int remainingSlots = MAX_VISIBLE_CATEGORIES - 1 - visibleCategories.size();
        
        // Add other categories (preserving original order)
        for (int i = 0; i < Math.min(remainingSlots, remainingCategories.size()); i++) {
            visibleCategories.add(remainingCategories.get(i));
        }
        
        // Add "More" if necessary
        if (remainingCategories.size() > remainingSlots) {
            visibleCategories.add("More");
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = visibleCategories.get(position);
        boolean isMore = moreClickListener != null && "More".equals(category);
        boolean isSelected = position == selectedPosition && !isMore;

        // Bind the data to the ViewHolder
        holder.bind(category, isSelected);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (isMore && moreClickListener != null) {
                // Calculate remaining categories
                List<String> remainingCategories = new ArrayList<>(categories);
                for (String visibleCategory : visibleCategories) {
                    if (!"More".equals(visibleCategory)) {
                        remainingCategories.remove(visibleCategory);
                    }
                }
                moreClickListener.onMoreClick(remainingCategories);
                Log.d(TAG, "👆 'More' clicked, showing " + remainingCategories.size() + " additional categories");
            } else if (listener != null && !isMore) {
                setSelectedPosition(position);
                listener.onCategoryClick(category, position);
                Log.d(TAG, "👆 Category clicked: '" + category + "' at position " + position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleCategories.size();
    }

    /**
     * Get the list of all categories
     */
    public List<String> getAllCategories() {
        return new ArrayList<>(categories);
    }
    
    /**
     * Get currently visible categories
     */
    public List<String> getVisibleCategories() {
        return new ArrayList<>(visibleCategories);
    }
    
    /**
     * ViewHolder for category items
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final ImageView categoryIconView;
        private final LinearLayout linearLayout;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView;
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryIconView = itemView.findViewById(R.id.categoryIcon);
        }

        void bind(String category, boolean isSelected) {
            // Set the category name
            categoryName.setText(category);
            
            // Default icon resource
            int iconResId = R.drawable.ic_category_icon;
            
            // Special handling for "More" button
            if ("More".equals(category)) {
                categoryIconView.setImageResource(R.drawable.ic_more);
                categoryIconView.setColorFilter(
                        itemView.getContext().getColor(R.color.text_primary));
                categoryName.setTextColor(
                        itemView.getContext().getColor(R.color.text_primary));
                linearLayout.setSelected(false);
                return;
            }

            // Try to get cached URL first for better performance
            String cachedUrl = iconUrlCache.get(category);
            if (cachedUrl != null) {
                loadIconWithGlide(cachedUrl, category, iconResId, isSelected);
                return;
            }
            
            // Get icon from repository if not in cache
            String iconUrl = getCategoryIconUrl(category);
            if (iconUrl != null && !failedIconUrls.contains(iconUrl)) {
                // Cache the URL for future use
                iconUrlCache.put(category, iconUrl);
                loadIconWithGlide(iconUrl, category, iconResId, isSelected);
            } else {
                // Use default icon
                categoryIconView.setImageResource(iconResId);
                setItemStyles(isSelected);
                Log.d(TAG, "🖼️ Using default icon for category: '" + category + "'");
            }
        }
        
        /**
         * Set selection styles for item
         */
        private void setItemStyles(boolean isSelected) {
            // Set background based on selection state
            linearLayout.setSelected(isSelected);
            
            // Set text and icon colors based on selection state
            int colorRes = isSelected ? R.color.black : R.color.text_primary;
            int color = itemView.getContext().getColor(colorRes);
            
            categoryName.setTextColor(color);
            categoryIconView.setColorFilter(color);
        }
        
        /**
         * Load icon using Glide with optimized settings
         */
        private void loadIconWithGlide(String iconUrl, String category, int fallbackIconResId, boolean isSelected) {
            // Log icon loading attempt with full details
            Log.d(TAG, "🔄 Loading icon for '" + category + "' from URL: " + iconUrl);
            
            // Special handling for "All" category which is critical
            if ("All".equalsIgnoreCase(category) || "all".equalsIgnoreCase(category)) {
                Log.d(TAG, "⭐ Special handling for 'All' category: " + iconUrl);
            }
            
            // Set current styles immediately while loading
            setItemStyles(isSelected);
            
            // Apply additional validation
            if (iconUrl == null || iconUrl.trim().isEmpty()) {
                Log.e(TAG, "❌ Empty icon URL for category '" + category + "', using fallback");
                categoryIconView.setImageResource(fallbackIconResId);
                
                // For the All category specifically, use a known working fallback
                if ("All".equalsIgnoreCase(category) || "all".equalsIgnoreCase(category)) {
                    String fallback = "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png";
                    Log.d(TAG, "⭐ Using reliable fallback for 'All' category: " + fallback);
                    loadDirectIconUrl(fallback, fallbackIconResId, isSelected);
                }
                return;
            }
            
            // If context is null, use fallback icon
            if (itemView.getContext() == null) {
                Log.e(TAG, "❌ Null context for loading icon for '" + category + "', using fallback");
                categoryIconView.setImageResource(fallbackIconResId);
                return;
            }
            
            try {
                // Set placeholder immediately
                categoryIconView.setImageResource(fallbackIconResId);
                
                // Prepare enhanced request options
                RequestOptions enhancedOptions = iconRequestOptions
                    .placeholder(fallbackIconResId)
                    .error(fallbackIconResId)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized
                    .timeout(8000); // 8s timeout
                
                // Load icon with Glide
                Glide.with(itemView.getContext().getApplicationContext()) // Use application context for safety
                    .load(iconUrl)
                    .apply(enhancedOptions)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            // Mark as failed to avoid repeated failures
                            failedIconUrls.add(iconUrl);
                            Log.e(TAG, "❌ Failed to load icon for '" + category + "': " +
                                    (e != null ? e.getMessage() : "unknown error"));
                            
                            // Try to load a fallback URL after failure
                            String fallbackUrl = generateFallbackUrl(category);
                            if (!iconUrl.equals(fallbackUrl)) {
                                // Prevent infinite recursion
                                loadIconWithGlide(fallbackUrl, category, fallbackIconResId, isSelected);
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                      Target<Drawable> target, DataSource dataSource,
                                                      boolean isFirstResource) {
                            // Log successful load and cache source
                            Log.d(TAG, "✅ Loaded icon for '" + category + "' successfully" + 
                                (dataSource == DataSource.MEMORY_CACHE ? " (from memory cache)" : 
                                dataSource == DataSource.LOCAL ? " (from disk cache)" : 
                                " (from network)"));
                            return false;
                        }
                    })
                    .into(categoryIconView);
            } catch (Exception e) {
                // Handle any exceptions during loading
                Log.e(TAG, "❌ Exception loading icon for '" + category + "': " + e.getMessage(), e);
                categoryIconView.setImageResource(fallbackIconResId);
            }
        }
        
        /**
         * Load an icon directly from URL as backup method
         */
        private void loadDirectIconUrl(String iconUrl, int fallbackIconResId, boolean isSelected) {
            try {
                // Set placeholder immediately  
                categoryIconView.setImageResource(fallbackIconResId);
                
                // Set styles
                setItemStyles(isSelected);
                
                // Use direct loading with simpler options
                Glide.with(itemView.getContext().getApplicationContext())
                    .load(iconUrl)
                    .placeholder(fallbackIconResId)
                    .error(fallbackIconResId)
                    .into(categoryIconView);
                
                Log.d(TAG, "⭐ Direct icon loading attempt for URL: " + iconUrl);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error with direct icon loading: " + e.getMessage(), e);
                categoryIconView.setImageResource(fallbackIconResId);
            }
        }
    }

    /**
     * Get icon from repository if not in cache
     */
    private String getCategoryIconUrl(String category) {
        if (category == null) {
            return null;
        }
        
        try {
            // Add special debug for All category
            if ("All".equalsIgnoreCase(category) || "all".equalsIgnoreCase(category)) {
                Log.d(TAG, "⭐ Getting icon URL for special category 'All'");
            }
            
            // Check URL cache first for fast return
            String normalizedCategory = normalizeCategory(category);
            String cachedUrl = iconUrlCache.get(normalizedCategory);
            if (cachedUrl != null) {
                Log.d(TAG, "🚀 URL cache hit for category: '" + category + "'");
                return cachedUrl;
            }
            
            // Handle "All" category specially
            if ("all".equalsIgnoreCase(normalizedCategory)) {
                String allFallbackUrl = "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png";
                Log.d(TAG, "⭐ Using reliable 'All' category icon: " + allFallbackUrl);
                iconUrlCache.put("all", allFallbackUrl);
                return allFallbackUrl;
            }
            
            // If repository is not properly initialized, force refresh
            if (categoryIconRepository == null) {
                Log.e(TAG, "❌ CategoryIconRepository is null for category: '" + category + "'");
                return generateFallbackUrl(category);
            }
            
            // Try to get icon URL from repository
            String iconUrl = categoryIconRepository.getCategoryIconUrl(category);
            
            if (iconUrl != null && !iconUrl.isEmpty()) {
                // Cache the URL for future use
                iconUrlCache.put(normalizedCategory, iconUrl);
                Log.d(TAG, "🖼️ Found icon URL for category: '" + category + "': " + iconUrl);
                return iconUrl;
            } else {
                Log.w(TAG, "⚠️ No icon URL found for category: '" + category + "'");
                
                // Try to refresh icons and try again
                if (!categoryIconsInitialized.getAndSet(true)) {
                    categoryIconRepository.refreshCategoryIcons();
                    
                    // Retry after refresh
                    iconUrl = categoryIconRepository.getCategoryIconUrl(category);
                    if (iconUrl != null && !iconUrl.isEmpty()) {
                        // Cache the URL for future use
                        iconUrlCache.put(normalizedCategory, iconUrl);
                        Log.d(TAG, "🔄 Found icon URL after refresh for category: '" + category + "': " + iconUrl);
                        return iconUrl;
                    }
                }
                
                // If all else fails, generate a fallback URL
                String fallbackUrl = generateFallbackUrl(category);
                iconUrlCache.put(normalizedCategory, fallbackUrl);
                return fallbackUrl;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting icon URL for category: '" + category + "'", e);
            String fallbackUrl = generateFallbackUrl(category);
            iconUrlCache.put(normalizeCategory(category), fallbackUrl);
            return fallbackUrl;
        }
    }
    
    /**
     * Normalize a category name for consistent lookup
     */
    private String normalizeCategory(String category) {
        if (category == null) return "";
        return category.toLowerCase().trim();
    }
    
    /**
     * Generate a fallback URL for category icons
     */
    private String generateFallbackUrl(String category) {
        if (category == null) {
            return null;
        }
        
        // Try to match common categories with fixed URLs
        String normalized = normalizeCategory(category);
        
        // Common category mappings
        if (normalized.equals("all")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png";
        } else if (normalized.contains("birthday")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/cake/materialicons/24dp/2x/baseline_cake_black_24dp.png";
        } else if (normalized.contains("wedding")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/places/cake/materialicons/24dp/2x/baseline_cake_black_24dp.png";
        } else if (normalized.contains("holiday") || normalized.contains("festival")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/notification/event_note/materialicons/24dp/2x/baseline_event_note_black_24dp.png";
        } else if (normalized.contains("christmas")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/maps/local_florist/materialicons/24dp/2x/baseline_local_florist_black_24dp.png";
        } else if (normalized.contains("anniversary")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/date_range/materialicons/24dp/2x/baseline_date_range_black_24dp.png";
        } else if (normalized.contains("baby")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/image/child_care/materialicons/24dp/2x/baseline_child_care_black_24dp.png";
        } else if (normalized.contains("invitation")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/content/mail/materialicons/24dp/2x/baseline_mail_black_24dp.png";
        } else if (normalized.contains("graduation")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/school/materialicons/24dp/2x/baseline_school_black_24dp.png";
        }
        
        // Generic material design icon as last resort
        return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/category/materialicons/24dp/2x/baseline_category_black_24dp.png";
    }
    
    /**
     * Clear the icon caches
     */
    public void clearIconCaches() {
        iconUrlCache.evictAll();
        failedIconUrls.clear();
        Log.d(TAG, "🧹 Cleared icon caches");
    }

    /**
     * Set whether to prevent category changes (method alias for compatibility)
     * @param prevent Whether to prevent category updates
     */
    public void setPreventCategoryChanges(boolean prevent) {
        preventCategoryChanges(prevent);
    }
}
