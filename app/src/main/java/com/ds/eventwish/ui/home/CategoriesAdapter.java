package com.ds.eventwish.ui.home;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private static final int MAX_VISIBLE_CATEGORIES = 5;
    private List<String> categories = new ArrayList<>();
    private List<String> visibleCategories = new ArrayList<>();
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;
    private OnMoreClickListener moreClickListener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, int position);
    }

    public interface OnMoreClickListener {
        void onMoreClick(List<String> remainingCategories);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.moreClickListener = listener;
    }

    public void updateCategories(List<String> newCategories) {
        this.categories = new ArrayList<>(newCategories);
        updateVisibleCategories();
        Log.d("CategoriesAdapter", "Categories updated: " + categories.size() + " categories, " + visibleCategories.size() + " visible");
        for (int i = 0; i < visibleCategories.size(); i++) {
            Log.d("CategoriesAdapter", "Visible category " + i + ": " + visibleCategories.get(i));
        }
        notifyDataSetChanged();
    }

    private void updateVisibleCategories() {
        visibleCategories.clear();
        if (categories.size() <= MAX_VISIBLE_CATEGORIES) {
            visibleCategories.addAll(categories);
            Log.d("CategoriesAdapter", "All categories fit in visible list: " + categories.size());
        } else {
            // Always include "All" category
            if (categories.contains("All")) {
                visibleCategories.add("All");
                Log.d("CategoriesAdapter", "Added 'All' category to visible list");
            }
            
            // Add other categories up to MAX_VISIBLE_CATEGORIES - 1 (leaving space for "More")
            int remainingSlots = MAX_VISIBLE_CATEGORIES - 1 - visibleCategories.size();
            List<String> otherCategories = new ArrayList<>(categories);
            otherCategories.remove("All"); // Remove "All" as we've already added it
            
            for (int i = 0; i < Math.min(remainingSlots, otherCategories.size()); i++) {
                visibleCategories.add(otherCategories.get(i));
                Log.d("CategoriesAdapter", "Added category to visible list: " + otherCategories.get(i));
            }
            
            // Add "More" as the last item
            visibleCategories.add("More");
            Log.d("CategoriesAdapter", "Added 'More' category to visible list");
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
        boolean isMore = category.equals("More");
        boolean isSelected = position == selectedPosition && !isMore;
        
        Log.d("CategoriesAdapter", "Binding category at position " + position + ": " + category + ", selected: " + isSelected);
        holder.bind(category, isSelected);
        
        holder.itemView.setOnClickListener(v -> {
            Log.d("CategoriesAdapter", "Category clicked: " + category + " at position " + position);
            if (isMore && moreClickListener != null) {
                List<String> remainingCategories = categories.subList(MAX_VISIBLE_CATEGORIES - 1, categories.size());
                moreClickListener.onMoreClick(remainingCategories);
            } else if (listener != null) {
                int oldPosition = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
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
        int newPosition;
        if (category.equals("All")) {
            newPosition = 0; // "All" is always at position 0
        } else {
            newPosition = categories.indexOf(category);
        }
        
        if (newPosition >= 0) {
            // If category is in visible list, update selection
            int visiblePosition = visibleCategories.indexOf(category);
            if (visiblePosition >= 0) {
                setSelectedPosition(visiblePosition);
            } else {
                // Replace last visible category before "More" with selected category
                if (visibleCategories.size() > MAX_VISIBLE_CATEGORIES - 2) {
                    visibleCategories.set(MAX_VISIBLE_CATEGORIES - 2, category);
                    setSelectedPosition(MAX_VISIBLE_CATEGORIES - 2);
                }
            }
        }
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView categoryName;
        private final ImageView categoryIcon;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
        }

        void bind(String category, boolean isSelected) {
            categoryName.setText(category);

            cardView.setCardBackgroundColor(isSelected ?
                    itemView.getContext().getColor(R.color.black) :
                    Color.WHITE);

            categoryName.setTextColor(
                    itemView.getContext().getColor(isSelected ? R.color.white : R.color.text_primary)
            );

            categoryIcon.setColorFilter(
                    itemView.getContext().getColor(isSelected ? R.color.white : R.color.text_primary)
            );

        }
    }
}
