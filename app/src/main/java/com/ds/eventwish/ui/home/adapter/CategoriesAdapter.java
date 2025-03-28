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
    
    public void updateSelectedCategory(String categoryId) {
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
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onCategoryClickListener != null) {
                    onCategoryClickListener.onCategoryClick(category, position);
                }
            });
        }
    }
} 