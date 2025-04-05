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
import com.ds.eventwish.ui.home.Category;

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
                boolean sameImage = (oldItem.getImageUrl() == null && newItem.getImageUrl() == null) ||
                        (oldItem.getImageUrl() != null && oldItem.getImageUrl().equals(newItem.getImageUrl()));
                
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
     * Update the selected category
     * @param categoryId ID of the selected category, can be null for "All"
     */
    public void updateSelectedCategory(String categoryId) {
        if ((selectedCategoryId == null && categoryId == null) ||
            (selectedCategoryId != null && selectedCategoryId.equals(categoryId))) {
            // No change, return
            return;
        }
        
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
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
            
            // Load image with enhanced Glide configuration for better caching
            if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
                String imageUrl = category.getImageUrl();
                
                // Log image loading for debugging
                Log.d(TAG, "Loading category icon for: " + category.getName() + " from URL: " + imageUrl);
                
                // Use a placeholder while loading to maintain layout stability
                imageView.setImageResource(R.drawable.ic_category);
                
                // Enhanced Glide request with better caching and error handling
                RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_category)
                    .error(R.drawable.ic_category)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized image
                    .skipMemoryCache(false) // Use memory cache
                    .override(200, 200) // Consistent size
                    .centerCrop(); // Center crop for consistent appearance
                
                try {
                    Glide.with(context.getApplicationContext()) // Use application context to avoid memory leaks
                        .load(imageUrl)
                        .apply(options)
                        .timeout(10000) // 10-second timeout
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, 
                                                    Object model, 
                                                    Target<android.graphics.drawable.Drawable> target, 
                                                    boolean isFirstResource) {
                                Log.e(TAG, "Failed to load image for " + category.getName() + 
                                    ": " + (e != null ? e.getMessage() : "unknown error"));
                                
                                // Fallback to category default icon
                                imageView.setImageResource(R.drawable.ic_category);
                                return true; // We've handled the error
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                        Object model, 
                                                        Target<android.graphics.drawable.Drawable> target, 
                                                        DataSource dataSource, 
                                                        boolean isFirstResource) {
                                Log.d(TAG, "Successfully loaded image for " + category.getName() + 
                                    " (from " + dataSource.name() + ")");
                                return false; // Let Glide handle setting the resource
                            }
                        })
                        .into(imageView);
                } catch (Exception e) {
                    Log.e(TAG, "Error while loading category icon: " + e.getMessage(), e);
                    imageView.setImageResource(R.drawable.ic_category);
                }
            } else {
                // Use default icon if no URL is available
                Log.d(TAG, "No image URL for category: " + category.getName() + ", using default icon");
                imageView.setImageResource(R.drawable.ic_category);
            }
            
            // Check if this category is selected - improved logic for handling "All" category
            boolean isSelected;
            if (selectedCategoryId == null) {
                // When selectedCategoryId is null, the "All" category should be selected
                isSelected = category.getId() == null;
            } else {
                // When selectedCategoryId is not null, match by ID
                isSelected = selectedCategoryId.equals(category.getId());
            }
            
            // Update the UI to reflect selection state
            container.setSelected(isSelected);
            
            // Apply additional styling for selected state
            if (isSelected) {
                // Change text color for selected category
                textView.setTextColor(context.getResources().getColor(R.color.colorPrimary, null));
                
                // Change icon tint for selected category
                imageView.setColorFilter(context.getResources().getColor(R.color.colorPrimary, null));
            } else {
                // Default text color for unselected categories
                textView.setTextColor(context.getResources().getColor(R.color.black, null));
                
                // Default icon tint for unselected categories
                imageView.setColorFilter(context.getResources().getColor(R.color.purple_500, null));
            }
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onCategoryClickListener != null) {
                    onCategoryClickListener.onCategoryClick(category, position);
                }
            });
        }
    }
} 