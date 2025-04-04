package com.ds.eventwish.ui.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.ui.home.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying categories in a RecyclerView
 */
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    
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
        this.categories = categories != null ? categories : new ArrayList<>();
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
     * Update the data in the adapter
     * @param newCategories New list of categories
     */
    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
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
            
            // Load image with Glide instead of Picasso
            if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(category.getImageUrl())
                        .placeholder(R.drawable.ic_category)
                        .error(R.drawable.ic_category)
                        .into(imageView);
            } else {
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