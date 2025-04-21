package com.ds.eventwish.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.facebook.shimmer.ShimmerFrameLayout;

/**
 * Adapter for displaying category items in horizontal RecyclerView
 * with optimized icon loading and caching
 */
public class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "CategoriesAdapter";
    private static final int MAX_VISIBLE_CATEGORIES = 5; // Fixed number of visible categories
    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    private static final long CACHE_EXPIRATION_MS = 30 * 60 * 1000; // 30 minutes
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final int MAX_RETRY_COUNT = 3;
    
    /**
     * Tracks failed icon loads with retry information
     */
    private static class RetryEntry {
        final String iconUrl;
        int retryCount;
        long nextRetryTime;
        
        RetryEntry(String iconUrl) {
            this.iconUrl = iconUrl;
            this.retryCount = 0;
            this.nextRetryTime = System.currentTimeMillis();
        }
        
        boolean shouldRetry() {
            return retryCount < MAX_RETRY_COUNT && System.currentTimeMillis() >= nextRetryTime;
        }
        
        void incrementRetry() {
            retryCount++;
            // Exponential backoff: delay = initial_delay * 2^retry_count
            long delay = INITIAL_RETRY_DELAY_MS * (1L << (retryCount - 1));
            nextRetryTime = System.currentTimeMillis() + delay;
        }
    }
    
    // Track failed icon loads with retry information
    private final Map<String, RetryEntry> failedIconLoads = new HashMap<>();
    
    /**
     * Cache entry that includes both the drawable and its expiration time
     */
    private static class CacheEntry {
        final WeakReference<Drawable> drawable;
        final long expirationTime;
        
        CacheEntry(Drawable drawable) {
            this.drawable = new WeakReference<>(drawable);
            this.expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_MS;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime || drawable.get() == null;
        }
    }
    
    // Cache loaded icons with weak references to allow GC
    private final Map<String, CacheEntry> iconCache = new HashMap<>();
    
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
    
    // Track if we're in pagination loading
    private boolean isPaginationLoading = false;
    private boolean isInitialLoading = false;

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
        
        Log.d(TAG, "📋 updateCategories called with " + newCategories.size() + " categories: " + newCategories);
        
        // Skip updates if we're preventing changes or a refresh is in progress
        if (preventCategoryChanges) {
            Log.d(TAG, "🔒 Category changes prevented, skipping update");
            return;
        }
        
        if (refreshInProgress.get()) {
            Log.d(TAG, "🔄 Refresh already in progress, skipping update");
            return;
        }

        // Clear icon cache if category list size changed significantly
        if (Math.abs(categories.size() - newCategories.size()) > 2) {
            clearIconCaches();
            Log.d(TAG, "🧹 Cleared icon cache due to significant category list change");
        }
        
        // Set refresh flag to prevent concurrent updates
        refreshInProgress.set(true);
        
        try {
            // Clear and update the categories list
            categories.clear();
            categories.addAll(newCategories);
            
            // Update visible categories based on mode
            updateVisibleCategories();
            
            notifyDataSetChanged();
            Log.d(TAG, "✅ Categories updated successfully");
        } finally {
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
                
        Log.d(TAG, "📋 Updating visible categories. Total categories: " + categories.size());
        
        visibleCategories.clear();
        
        // Always include "All" category first
        visibleCategories.add("All");
        
        // Create list of regular categories (excluding All and More)
        List<String> regularCategories = new ArrayList<>();
        for (String category : categories) {
            if (!category.equals("All") && !category.equals("More")) {
                regularCategories.add(category);
            }
        }
        
        // Sort regular categories alphabetically
        Collections.sort(regularCategories);
        
        // If selected category is not "All", ensure it's included first
        if (selectedCategory != null && !selectedCategory.equals("All")) {
            if (regularCategories.remove(selectedCategory)) {
                regularCategories.add(0, selectedCategory);
            }
        }
        
        // Add categories up to MAX_VISIBLE_CATEGORIES - 1 (leaving space for More if needed)
        int availableSlots = MAX_VISIBLE_CATEGORIES - visibleCategories.size();
        int categoriesToShow = Math.min(availableSlots, regularCategories.size());
        
        for (int i = 0; i < categoriesToShow; i++) {
            visibleCategories.add(regularCategories.get(i));
        }
        
        // Add "More" if there are additional categories
        if (regularCategories.size() > categoriesToShow) {
            visibleCategories.add("More");
        }
        
        Log.d(TAG, "📋 Visible categories: " + visibleCategories.size() + 
              " shown, " + Math.max(0, regularCategories.size() - categoriesToShow) + 
              " in More section");
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
        
        // Always include "All" category first
        if (categories.contains("All")) {
            visibleCategories.add("All");
        }
        
        // Add the selected category if not "All" and not already included
        if (!category.equals("All") && categories.contains(category) &&
                !visibleCategories.contains(category)) {
            visibleCategories.add(category);
        }
        
        // Create remaining categories list excluding already added ones
        List<String> remainingCategories = new ArrayList<>();
        for (String cat : categories) {
            if (!visibleCategories.contains(cat) && !cat.equals("All")) {
                remainingCategories.add(cat);
            }
        }
        
        // Calculate how many more categories we can add to reach 5 total
        int slotsRemaining = 5 - visibleCategories.size();
        
        // Add remaining categories up to the 5-category limit
        for (int i = 0; i < Math.min(slotsRemaining, remainingCategories.size()); i++) {
            visibleCategories.add(remainingCategories.get(i));
        }
        
        // Remove added categories from remainingCategories
        remainingCategories = remainingCategories.subList(
            Math.min(slotsRemaining, remainingCategories.size()),
            remainingCategories.size()
        );
        
        // Add "More" if there are remaining categories
        if (!remainingCategories.isEmpty()) {
            visibleCategories.add("More");
            Log.d(TAG, "📋 Added 'More' option for " + remainingCategories.size() + " additional categories");
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).shimmerLayout.startShimmer();
        } else if (holder instanceof CategoryViewHolder) {
            CategoryViewHolder categoryHolder = (CategoryViewHolder) holder;
            String category = visibleCategories.get(position);
            boolean isMore = "More".equals(category);
            boolean isSelected = position == selectedPosition && !isMore;

            // Bind the data to the ViewHolder
            categoryHolder.bind(category, isSelected);

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                if (isMore && moreClickListener != null) {
                    // Get remaining categories, excluding special categories and already visible ones
                    List<String> remainingCategories = new ArrayList<>();
                    for (String cat : categories) {
                        // Skip special categories and already visible ones
                        if (!cat.equals("All") && 
                            !cat.equals("More") && 
                            !visibleCategories.subList(0, visibleCategories.size() - 1).contains(cat)) {
                            remainingCategories.add(cat);
                        }
                    }
                    
                    if (!remainingCategories.isEmpty()) {
                        // Sort categories alphabetically for better readability in dialog
                        Collections.sort(remainingCategories);
                        moreClickListener.onMoreClick(remainingCategories);
                        Log.d(TAG, "👆 More clicked, showing " + remainingCategories.size() + 
                              " additional categories");
                    }
                } else if (listener != null && !isMore) {
                    setSelectedPosition(position);
                    listener.onCategoryClick(category, position);
                    Log.d(TAG, "👆 Category clicked: " + category + " at position " + position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return isInitialLoading ? 5 : visibleCategories.size(); // Show 5 shimmer items during initial load
    }

    @Override
    public int getItemViewType(int position) {
        return isPaginationLoading ? VIEW_TYPE_LOADING : VIEW_TYPE_CATEGORY;
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

            // Try to get icon URL
            String iconUrl = getCategoryIconUrl(category);
            if (iconUrl != null && !failedIconLoads.containsKey(iconUrl)) {
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
            
            // Set current styles immediately while loading
            setItemStyles(isSelected);
            
            // Apply additional validation
            if (iconUrl == null || iconUrl.trim().isEmpty()) {
                Log.e(TAG, "❌ Empty icon URL for category '" + category + "', using fallback");
                categoryIconView.setImageResource(fallbackIconResId);
                return;
            }
            
            // Check if this URL is in failed loads and handle retry logic
            RetryEntry retryEntry = failedIconLoads.get(iconUrl);
            if (retryEntry != null) {
                if (!retryEntry.shouldRetry()) {
                    Log.d(TAG, "⏳ Skipping load for '" + category + "', max retries exceeded or waiting for backoff");
                    categoryIconView.setImageResource(fallbackIconResId);
                    return;
                }
                retryEntry.incrementRetry();
                Log.d(TAG, "🔄 Retrying load for '" + category + "', attempt " + retryEntry.retryCount);
            }
            
            try {
                // Set placeholder immediately
                categoryIconView.setImageResource(fallbackIconResId);
                
                // Load icon with Glide
                Glide.with(context)
                    .load(iconUrl)
                    .apply(iconRequestOptions)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            if (retryEntry == null) {
                                failedIconLoads.put(iconUrl, new RetryEntry(iconUrl));
                            }
                            Log.e(TAG, "❌ Failed to load icon for '" + category + "': " +
                                    (e != null ? e.getMessage() : "unknown error") +
                                    (retryEntry != null ? ", retry " + retryEntry.retryCount + "/" + MAX_RETRY_COUNT : ""));
                            categoryIconView.setImageResource(fallbackIconResId);
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                      Target<Drawable> target, DataSource dataSource,
                                                      boolean isFirstResource) {
                            Log.d(TAG, "✅ Loaded icon for '" + category + "' successfully" + 
                                (dataSource == DataSource.MEMORY_CACHE ? " (from memory cache)" : 
                                dataSource == DataSource.LOCAL ? " (from disk cache)" : 
                                " (from network)"));
                            // Remove from failed loads if it was there
                            failedIconLoads.remove(iconUrl);
                            iconCache.put(iconUrl, new CacheEntry(resource));
                            return false;
                        }
                    })
                    .into(categoryIconView);
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception loading icon for '" + category + "': " + e.getMessage(), e);
                if (retryEntry == null) {
                    failedIconLoads.put(iconUrl, new RetryEntry(iconUrl));
                }
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
            CacheEntry cachedEntry = iconCache.get(normalizedCategory);
            if (cachedEntry != null) {
                if (!cachedEntry.isExpired() && cachedEntry.drawable.get() != null) {
                    Log.d(TAG, "🚀 Icon cache hit for category: '" + category + "'");
                    return category; // Return category as key since we have the icon cached
                } else {
                    // Remove expired entry
                    Log.d(TAG, "⌛ Removing expired cache entry for category: '" + category + "'");
                    iconCache.remove(normalizedCategory);
                }
            }
            
            // Handle "All" category specially
            if ("all".equalsIgnoreCase(normalizedCategory)) {
                String allFallbackUrl = "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png";
                Log.d(TAG, "⭐ Using reliable 'All' category icon: " + allFallbackUrl);
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
                        Log.d(TAG, "🔄 Found icon URL after refresh for category: '" + category + "': " + iconUrl);
                        return iconUrl;
                    }
                }
                
                // If all else fails, generate a fallback URL
                String fallbackUrl = generateFallbackUrl(category);
                return fallbackUrl;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting icon URL for category: '" + category + "'", e);
            return generateFallbackUrl(category);
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
        
        // Special case for "All" category since it's a core UI element
        String normalized = normalizeCategory(category);
        if (normalized.equals("all")) {
            return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/view_comfy/materialicons/24dp/2x/baseline_view_comfy_black_24dp.png";
        }
        
        // Use generic material design icon for all other categories
        // This URL will be replaced by the CategoryIconRepository's URL once available
        return "https://raw.githubusercontent.com/google/material-design-icons/master/png/action/category/materialicons/24dp/2x/baseline_category_black_24dp.png";
    }
    
    /**
     * Clear the icon caches
     */
    public void clearIconCaches() {
        iconCache.clear();
        failedIconLoads.clear();
        Log.d(TAG, "🧹 Cleared icon caches and retry tracking");
    }

    /**
     * Set whether to prevent category changes (method alias for compatibility)
     * @param prevent Whether to prevent category updates
     */
    public void setPreventCategoryChanges(boolean prevent) {
        preventCategoryChanges(prevent);
    }

    /**
     * Check if category icons have been initialized
     * @return true if categories have been initialized
     */
    public boolean isInitialized() {
        return categoryIconsInitialized.get();
    }

    /**
     * Get the currently selected position
     * @return The selected position, or 0 if none selected (for "All" category)
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    /**
     * Get the currently selected category 
     * @return The selected category, or "All" if none selected
     */
    public String getSelectedCategory() {
        if (selectedPosition >= 0 && selectedPosition < visibleCategories.size()) {
            return visibleCategories.get(selectedPosition);
        }
        return "All";
    }

    public void setLoading(boolean loading) {
        // Handle initial loading vs pagination loading
        if (!this.isInitialLoading && loading) {
            // This is the first loading state
            this.isInitialLoading = true;
            this.isPaginationLoading = false;
        } else if (this.isInitialLoading && !loading) {
            // Initial loading finished
            this.isInitialLoading = false;
        }
        
        if (this.isPaginationLoading != loading) {
            this.isPaginationLoading = loading;
            // Only show loading UI during pagination
            if (loading) {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        clearIconCaches();
        Log.d(TAG, "🧹 Cleaned up resources on detach");
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        LoadingViewHolder(View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
