package com.ds.eventwish.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.databinding.ItemCategoryBinding;
import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private List<String> categories;
    private int selectedPosition = 0;

    public CategoriesAdapter() {
        this.categories = new ArrayList<>();
        // Add initial categories
        loadInitialCategories();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void loadMore() {
        // Add more categories
        int startPosition = categories.size();
        List<String> newCategories = getMoreCategories();
        categories.addAll(newCategories);
        notifyItemRangeInserted(startPosition, newCategories.size());
    }

    private void loadInitialCategories() {
        categories.add("All");
        categories.add("Birthday");
        categories.add("Anniversary");
        categories.add("Wedding");
        categories.add("Graduation");
        // Add more initial categories...
    }

    private List<String> getMoreCategories() {
        List<String> moreCategories = new ArrayList<>();
        moreCategories.add("Christmas");
        moreCategories.add("New Year");
        moreCategories.add("Valentine's");
        moreCategories.add("Easter");
        moreCategories.add("Halloween");
        return moreCategories;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String category, boolean isSelected) {
            binding.categoryChip.setText(category);
            binding.categoryChip.setChecked(isSelected);
            
            binding.categoryChip.setOnClickListener(v -> {
                // Handle category selection
                if (!isSelected) {
                    int oldPosition = ((CategoriesAdapter) getBindingAdapter()).selectedPosition;
                    ((CategoriesAdapter) getBindingAdapter()).selectedPosition = getAdapterPosition();
                    getBindingAdapter().notifyItemChanged(oldPosition);
                    getBindingAdapter().notifyItemChanged(getAdapterPosition());
                }
            });
        }
    }
}
