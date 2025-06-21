package com.ds.eventwish.ui.home.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Category;
import com.ds.eventwish.data.repository.CategoryIconRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter for displaying categories in a RecyclerView
 */
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoriesAdapter";
    private final Context context;
    private List<Category> categories;
    private OnCategoryClickListener onCategoryClickListener;
    private OnMoreClickListener onMoreClickListener;
    private String selectedCategoryId; // Track the selected category ID
    
    // State flags for loading states
    private boolean isInitialLoading = false;
    private boolean isPaginationLoading = false;
    
    /**
     * Interface for handling category click events
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }
    
    public interface OnMoreClickListener {
        void onMoreClick(List<Category> remainingCategories);
    }
    
    /**
     * Constructor
     * @param context Context
     * @param categories List of categories
     * @param listener Click listener
     */
    public CategoriesAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = sortCategoriesStably(categories != null ? categories : new ArrayList<>());
        this.onCategoryClickListener = listener;
    }
    
    /**
     * Constructor with just context
     * @param context Context
     */
    public CategoriesAdapter(Context context) {
        this(context, new ArrayList<>(), null);
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.onCategoryClickListener = listener;
    }
    
    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.onMoreClickListener = listener;
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
        Category category = categories.get(position);
        holder.bind(category, position);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    /**
     * Update the data in the adapter with a stable sort to prevent position changes
     * @param newCategories New list of categories
     */
    public void updateCategories(List<Category> newCategories) {
        List<Category> sortedNewCategories = sortCategoriesStably(newCategories);
        
        // Use DiffUtil for smooth updates without full rebind
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return categories.size();
            }

            @Override
            public int getNewListSize() {
                return sortedNewCategories.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Category oldItem = categories.get(oldItemPosition);
                Category newItem = sortedNewCategories.get(newItemPosition);
                
                // Compare by ID, handling null for "All" category
                if (oldItem.getId() == null && newItem.getId() == null) {
                    return true;
                }
                return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Category oldItem = categories.get(oldItemPosition);
                Category newItem = sortedNewCategories.get(newItemPosition);
                
                // Compare all relevant fields
                boolean sameId = (oldItem.getId() == null && newItem.getId() == null) ||
                        (oldItem.getId() != null && oldItem.getId().equals(newItem.getId()));
                boolean sameName = oldItem.getName().equals(newItem.getName());
                boolean sameImage = (oldItem.getIcon() == null && newItem.getIcon() == null) ||
                        (oldItem.getIcon() != null && oldItem.getIcon().equals(newItem.getIcon()));
                
                return sameId && sameName && sameImage;
            }
        });
        
        this.categories = sortedNewCategories;
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * Sort categories in a stable manner to maintain consistent positions
     * @param categoriesList List of categories to sort
     * @return Sorted list of categories
     */
    private List<Category> sortCategoriesStably(List<Category> categoriesList) {
        List<Category> result = new ArrayList<>(categoriesList);
        
        // Always ensure "All" category is first
        List<Category> sortedList = new ArrayList<>();
        
        // Find and extract the "All" category
        Category allCategory = null;
        for (Category category : result) {
            if (category.getId() == null || "all".equalsIgnoreCase(category.getId())) {
                allCategory = category;
                break;
            }
        }
        
        // If found, remove it from the main list and add it first
        if (allCategory != null) {
            result.remove(allCategory);
            sortedList.add(allCategory);
        }
        
        // Sort the rest alphabetically by name for stable positioning
        Collections.sort(result, Comparator.comparing(Category::getName));
        
        // Add the sorted categories after the "All" category
        sortedList.addAll(result);
        
        return sortedList;
    }
    
    public List<Category> getVisibleCategories() {
        return new ArrayList<>(categories);
    }
    
    /**
     * Get the currently selected category ID
     * @return The ID of the selected category, null for "All" category
     */
    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }
    
    /**
     * Get the position of the currently selected category in the visible list
     * @return Position of the selected category, or -1 if not found
     */
    public int getSelectedPosition() {
        if (categories == null || categories.isEmpty()) {
            return -1;
        }
        
        // If selectedCategoryId is null, the "All" category is selected (typically at position 0)
        if (selectedCategoryId == null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == null) {
                    return i;
                }
            }
            return -1;
        }
        
        // Find the position of the selected category by ID
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            if (category.getId() != null && category.getId().equals(selectedCategoryId)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Prevent category changes temporarily
     * This is used to avoid UI flickering when updating categories
     */
    private boolean preventChanges = false;
    
    public void preventCategoryChanges(boolean prevent) {
        this.preventChanges = prevent;
    }
    
    /**
     * Set the selected position
     * @param position Position to select
     */
    public void setSelectedPosition(int position) {
        if (position >= 0 && position < categories.size()) {
            Category category = categories.get(position);
            updateSelectedCategory(category.getId());
        } else if (position == 0 && categories.isEmpty()) {
            // Default to null (All) if no categories
            updateSelectedCategory(null);
        }
    }
    
    /**
     * Get selected category by ID
     * @return The ID of the currently selected category
     */
    public String getSelectedCategory() {
        return selectedCategoryId;
    }
    
    /**
     * Update the selected category
     * @param categoryId ID of the selected category, can be null for "All"
     */
    public void updateSelectedCategory(String categoryId) {
        if ((selectedCategoryId == null && categoryId == null) ||
            (selectedCategoryId != null && selectedCategoryId.equals(categoryId))) {
            // No change, return
            return;
        }
        
        Log.d(TAG, "Updating selected category from " + 
              (selectedCategoryId != null ? selectedCategoryId : "All") + 
              " to " + (categoryId != null ? categoryId : "All"));
        
        this.selectedCategoryId = categoryId;
        
        // Only notify data set changed if we're not preventing changes
        if (!preventChanges) {
            notifyDataSetChanged();
        }
    }
    
    /**
     * ViewHolder for category items
     * Non-static to access adapter fields
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textView;
        private final LinearLayout container;
        
        CategoryViewHolder(View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
            imageView = itemView.findViewById(R.id.categoryIcon);
            textView = itemView.findViewById(R.id.categoryName);
        }
        
        void bind(final Category category, final int position) {
            textView.setText(category.getName());
            
            // Update selection state
            boolean isSelected = (selectedCategoryId == null && category.getId() == null) ||
                    (selectedCategoryId != null && selectedCategoryId.equals(category.getId()));
            
            // Apply selection styling
            container.setSelected(isSelected);
            textView.setSelected(isSelected);
            
            // Load category icon with improved error handling
            loadCategoryIcon(category);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onCategoryClickListener != null) {
                    onCategoryClickListener.onCategoryClick(category, position);
                }
            });
        }
        
        /**
         * Load the category icon with robust error handling
         */
        private void loadCategoryIcon(Category category) {
            // Default to placeholder
            imageView.setImageResource(R.drawable.ic_category);

            // Check all levels in the chain with detailed logging
            if (category == null) {
                Log.w(TAG, "Null category object in bind()");
                return;
            }
            
            Log.d(TAG, "Loading icon for category: " + category.getName());
            
            if (category.getIcon() == null) {
                Log.w(TAG, "Null icon object for category: " + category.getName());
                return;
            }
            
            Log.d(TAG, "Category icon object found: " + category.getIcon().toString());
            
            if (category.getIcon().getCategoryIcon() == null) {
                Log.w(TAG, "Null getCategoryIcon() for category: " + category.getName());
                return;
            }
            
            final String imageUrl = category.getIcon().getCategoryIcon();
            if (imageUrl.isEmpty()) {
                Log.w(TAG, "Empty icon URL for category: " + category.getName());
                return;
            }
            
            Log.d(TAG, "Loading icon URL: " + imageUrl + " for category: " + category.getName());
            
            // Enhanced Glide request with better error handling and caching
            RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.ic_category)
                .error(R.drawable.ic_category)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(15000); // 15 second timeout for slow connections
                
            Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, 
                                            Object model, 
                                            Target<android.graphics.drawable.Drawable> target, 
                                            boolean isFirstResource) {
                        String categoryName = category != null ? category.getName() : "unknown";
                        String categoryId = category != null ? category.getId() : "null";
                        
                        Log.w(TAG, "Failed to load icon for category: '" + categoryName + "' (ID: " + 
                              categoryId + "), URL: " + imageUrl, e);
                        
                        if (e != null) {
                            // Log detailed exception info
                            for (Throwable t : e.getRootCauses()) {
                                Log.w(TAG, "Root cause: " + t.getMessage(), t);
                            }
                        }
                        
                        // Try fallback loading directly if this was a specific type of error
                        // (like malformed URL, which we might be able to fix)
                        if (e != null && e.getRootCauses().size() > 0 && 
                            (e.getMessage().contains("URL") || e.getMessage().contains("http"))) {
                            // Try fixing common URL issues
                            final String fixedUrl = fixCommonUrlIssues(imageUrl);
                            if (!fixedUrl.equals(imageUrl)) {
                                Log.d(TAG, "Trying with fixed URL: " + fixedUrl);
                                Glide.with(context)
                                    .load(fixedUrl)
                                    .apply(requestOptions)
                                    .into(imageView);
                            }
                        }
                        
                        return false; // Let Glide set the error drawable
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                Object model, 
                                                Target<android.graphics.drawable.Drawable> target, 
                                                DataSource dataSource, 
                                                boolean isFirstResource) {
                        Log.d(TAG, "Successfully loaded icon for: " + 
                              (category != null ? category.getName() : "unknown") + 
                              ", source: " + dataSource.name());
                        return false;
                    }
                })
                .into(imageView);
        }
        
        /**
         * Attempt to fix common URL issues
         */
        private String fixCommonUrlIssues(String url) {
            if (url == null || url.isEmpty()) {
                return url;
            }
            
            // Add https:// if missing
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (url.startsWith("//")) {
                    return "https:" + url;
                } else {
                    return "https://" + url;
                }
            }
            
            // Fix double http issues
            if (url.contains("http://http://")) {
                return url.replace("http://http://", "http://");
            }
            if (url.contains("https://http://")) {
                return url.replace("https://http://", "http://");
            }
            if (url.contains("http://https://")) {
                return url.replace("http://https://", "https://");
            }
            if (url.contains("https://https://")) {
                return url.replace("https://https://", "https://");
            }
            
            return url;
        }
    }
    
    /**
     * Set loading state for the adapter
     * @param loading Whether the adapter is in loading state
     */
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
} 