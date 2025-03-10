package com.ds.eventwish.ui.home;

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
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoriesAdapter";
    private static final int MAX_VISIBLE_CATEGORIES = 8;
    private List<String> categories = new ArrayList<>();
    private List<String> visibleCategories = new ArrayList<>();
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;
    private OnMoreClickListener moreClickListener;
    private CategoryIconRepository categoryIconRepository;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, int position);
    }

    public interface OnMoreClickListener {
        void onMoreClick(List<String> remainingCategories);
    }

    public CategoriesAdapter() {
        this.categoryIconRepository = CategoryIconRepository.getInstance();
        Log.d(TAG, "CategoriesAdapter initialized with CategoryIconRepository");
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.moreClickListener = listener;
    }

    public void updateCategories(List<String> newCategories) {
        if (!categories.equals(newCategories)) {
            this.categories = new ArrayList<>(newCategories);
            if (moreClickListener == null) {
                // Bottom sheet mode - no need for visible categories logic
                visibleCategories = new ArrayList<>(categories);
            } else {
                // Main adapter mode - use visible categories logic
                updateVisibleCategories();
            }
            Log.d("CategoriesAdapter", "Categories updated: " + categories.size() + " categories, " + visibleCategories.size() + " visible");
            notifyDataSetChanged();
        }
    }

    private void updateVisibleCategories() {
        visibleCategories.clear();
        if (categories.size() <= MAX_VISIBLE_CATEGORIES) {
            visibleCategories.addAll(categories);
            Log.d("CategoriesAdapter", "All categories fit in visible list: " + categories.size());
        } else {
            // Always include "All" category if it exists
            if (categories.contains("All")) {
                visibleCategories.add("All");
                Log.d("CategoriesAdapter", "Added 'All' category to visible list");
            }

            // Create a list of other categories excluding "All"
            List<String> otherCategories = new ArrayList<>(categories);
            otherCategories.remove("All");

            // If we have a selected category that's not "All", ensure it's included
            String selectedCategory = selectedPosition < categories.size() ?
                    categories.get(selectedPosition) : null;
            if (selectedCategory != null && !selectedCategory.equals("All") &&
                    otherCategories.contains(selectedCategory)) {
                visibleCategories.add(selectedCategory);
                otherCategories.remove(selectedCategory);
                Log.d("CategoriesAdapter", "Added selected category to visible list: " + selectedCategory);
            }

            // Calculate remaining slots for other categories
            int remainingSlots = MAX_VISIBLE_CATEGORIES - 1 - visibleCategories.size();

            // Add other categories up to the remaining slots
            for (int i = 0; i < Math.min(remainingSlots, otherCategories.size()); i++) {
                visibleCategories.add(otherCategories.get(i));
                Log.d("CategoriesAdapter", "Added category to visible list: " + otherCategories.get(i));
            }

            // Add "More" as the last item if we have more categories
            if (otherCategories.size() > remainingSlots) {
                visibleCategories.add("More");
                Log.d("CategoriesAdapter", "Added 'More' category to visible list");
            }
        }
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
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
        boolean isMore = moreClickListener != null && category.equals("More");
        boolean isSelected = position == selectedPosition && !isMore;

        Log.d("CategoriesAdapter", "Binding category at position " + position + ": " + category + ", selected: " + isSelected);
        holder.bind(category, isSelected);

        holder.itemView.setOnClickListener(v -> {
            Log.d("CategoriesAdapter", "Category clicked: " + category + " at position " + position);
            if (isMore && moreClickListener != null) {
                List<String> remainingCategories = categories.subList(MAX_VISIBLE_CATEGORIES - 1, categories.size());
                moreClickListener.onMoreClick(remainingCategories);
            } else if (listener != null && !isMore) {
                setSelectedPosition(position);
                listener.onCategoryClick(category, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleCategories.size();
    }

    // Getter for visible categories
    public List<String> getVisibleCategories() {
        return new ArrayList<>(visibleCategories);
    }

    // Update selected category from bottom sheet
    public void updateSelectedCategory(String category) {
        Log.d(TAG, "Updating selected category to: " + (category != null ? category : "All"));
        
        // Find the position of the category in the visible categories list
        if (category == null) {
            // Select "All" category (usually at position 0)
            for (int i = 0; i < visibleCategories.size(); i++) {
                if (visibleCategories.get(i).equals("All")) {
                    setSelectedPosition(i);
                    return;
                }
            }
            // If "All" not found, select position 0
            if (!visibleCategories.isEmpty()) {
                setSelectedPosition(0);
            }
        } else {
            // Find the category in the visible categories
            for (int i = 0; i < visibleCategories.size(); i++) {
                if (visibleCategories.get(i).equals(category)) {
                    setSelectedPosition(i);
                    return;
                }
            }
            
            // If category not found in visible categories, update visible categories
            if (categories.contains(category)) {
                // Add the category to visible categories if it exists in all categories
                Log.d(TAG, "Selected category not in visible list, updating visible categories");
                updateVisibleCategories();
                notifyDataSetChanged();
                
                // Now find the position again
                for (int i = 0; i < visibleCategories.size(); i++) {
                    if (visibleCategories.get(i).equals(category)) {
                        setSelectedPosition(i);
                        return;
                    }
                }
            }
        }
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final ImageView categoryIconView;
        private LinearLayout linearLayout;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView;
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryIconView = itemView.findViewById(R.id.categoryIcon);
        }

        void bind(String category, boolean isSelected) {
            categoryName.setText(category);

            // Set the appropriate icon based on category
            int iconResId = R.drawable.ic_category_icon;

            // Get the category icon from repository
            CategoryIcon categoryIconObj = getCategoryIcon(category);
            if (categoryIconObj != null && categoryIconObj.getCategoryIcon() != null) {
                String iconUrl = categoryIconObj.getCategoryIcon();
                Log.d(TAG, "Loading category icon for '" + category + "' from URL: " + iconUrl);
                
                // Load icon from URL using Glide
                Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .error(iconResId)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Failed to load icon for category '" + category + "': " +
                                        (e != null ? e.getMessage() : "unknown error"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                Log.d(TAG, "Successfully loaded icon for category '" + category + "'");
                                return false;
                            }
                        })
                        .into(categoryIconView);
            } else {
                Log.d(TAG, "No custom icon found for category '" + category + "', using default icon");
                categoryIconView.setImageResource(iconResId);
            }

            categoryIconView.setVisibility(View.VISIBLE);

            // Set background based on selection state
            linearLayout.setSelected(isSelected);

            // Set text and icon colors based on selection state
            categoryName.setTextColor(
                    itemView.getContext().getColor(isSelected ? R.color.black : R.color.text_primary)
            );

            categoryIconView.setColorFilter(
                    itemView.getContext().getColor(isSelected ? R.color.black : R.color.text_primary)
            );
        }
    }

    // Replace the findTemplateForCategory and getCategoryIconUrl methods with this single method
    private CategoryIcon getCategoryIcon(String category) {
        if (category == null) {
            Log.w(TAG, "Received null category");
            return null;
        }

        Log.d(TAG, "Fetching icon for category: " + category);
        
        // Get the icon from the repository
        CategoryIcon icon = categoryIconRepository.getCategoryIconByCategory(category.toLowerCase());
        
        if (icon != null) {
            Log.d(TAG, "Found icon for category '" + category + "': " + icon.getCategoryIcon());
            return icon;
        } else {
            Log.d(TAG, "No icon found for category: " + category);
            return null;
        }
    }

    /**
     * Get the list of all categories
     * @return The list of all categories
     */
    public List<String> getAllCategories() {
        return categories;
    }
}
