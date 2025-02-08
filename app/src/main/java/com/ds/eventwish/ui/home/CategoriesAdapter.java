package com.ds.eventwish.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        notifyDataSetChanged();
    }

    private void updateVisibleCategories() {
        visibleCategories.clear();
        if (categories.size() <= MAX_VISIBLE_CATEGORIES) {
            visibleCategories.addAll(categories);
        } else {
            visibleCategories.addAll(categories.subList(0, MAX_VISIBLE_CATEGORIES - 1));
            visibleCategories.add("More"); // Add "More" as the last item
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
        
        holder.bind(category, isSelected);
        
        holder.itemView.setOnClickListener(v -> {
            if (isMore && moreClickListener != null) {
                List<String> remainingCategories = categories.subList(MAX_VISIBLE_CATEGORIES - 1, categories.size());
                moreClickListener.onMoreClick(remainingCategories);
            } else if (listener != null) {
                listener.onCategoryClick(category, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleCategories.size();
    }

    // Update selected category from bottom sheet
    public void updateSelectedCategory(String category) {
        int newPosition = categories.indexOf(category);
        if (newPosition >= 0) {
            // If category is in visible list, update selection
            int visiblePosition = visibleCategories.indexOf(category);
            if (visiblePosition >= 0) {
                setSelectedPosition(visiblePosition);
            } else {
                // Replace last visible category before "More" with selected category
                visibleCategories.set(MAX_VISIBLE_CATEGORIES - 2, category);
                setSelectedPosition(MAX_VISIBLE_CATEGORIES - 2);
            }
        }
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView categoryName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            categoryName = itemView.findViewById(R.id.categoryName);
        }

        void bind(String category, boolean isSelected) {
            categoryName.setText(category);
            cardView.setCardBackgroundColor(isSelected ? 
                itemView.getContext().getColor(R.color.primary) : 
                Color.WHITE);
            categoryName.setTextColor(isSelected ? 
                Color.WHITE : 
                itemView.getContext().getColor(R.color.black));
        }
    }
}
