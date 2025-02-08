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
    private List<String> categories = new ArrayList<>();
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, int position);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void updateCategories(List<String> newCategories) {
        this.categories = new ArrayList<>(newCategories);
        notifyDataSetChanged();
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
        String category = categories.get(position);
        holder.bind(category, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView categoryName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            categoryName = itemView.findViewById(R.id.categoryName);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(categories.get(position), position);
                }
            });
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
