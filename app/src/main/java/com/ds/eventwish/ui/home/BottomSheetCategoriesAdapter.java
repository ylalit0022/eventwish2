package com.ds.eventwish.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Category;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the bottom sheet showing all categories
 */
public class BottomSheetCategoriesAdapter extends RecyclerView.Adapter<BottomSheetCategoriesAdapter.CategoryViewHolder> {
    private static final String TAG = "BottomSheetCategoriesAdapter";
    
    private final List<Category> categories = new ArrayList<>();
    private OnCategoryClickListener listener;
    private final Context context;
    private final CategoryIconRepository categoryIconRepository;
    private String selectedCategoryId = null;
    
    /**
     * Callback for when a category is clicked
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }
    
    /**
     * Constructor
     * @param context Context for resources
     */
    public BottomSheetCategoriesAdapter(Context context) {
        this.context = context;
        this.categoryIconRepository = CategoryIconRepository.getInstance(context);
    }
    
    /**
     * Set the click listener
     */
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    /**
     * Update the list of categories
     */
    public void updateCategories(List<Category> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Set the currently selected category
     */
    public void setSelectedCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bottom_sheet_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        
        // Bind data
        holder.bind(category);
        
        // Add animation for items
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(
                holder.itemView.getContext(),
                R.anim.item_animation_from_bottom));
        
        // Handle click
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onCategoryClick(category, adapterPosition);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    /**
     * ViewHolder for category items
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final TextView categoryCount;
        private final ImageView categoryIcon;
        private final ImageView selectIcon;
        private final LinearLayout container;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.categoryContainer);
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryCount = itemView.findViewById(R.id.categoryCount);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            selectIcon = itemView.findViewById(R.id.selectIcon);
        }
        
        void bind(Category category) {
            // Set category name
            categoryName.setText(category.getName());
            
            // Set count if available
            int count = category.getTemplateCount();
            if (count > 0) {
                categoryCount.setText(count + (count == 1 ? " template" : " templates"));
                categoryCount.setVisibility(View.VISIBLE);
            } else {
                categoryCount.setVisibility(View.GONE);
            }
            
            // Check if this is the selected category
            boolean isSelected = (selectedCategoryId == null && category.getId() == null) || 
                    (selectedCategoryId != null && selectedCategoryId.equals(category.getId()));
            
            // Show selection indicator if selected
            selectIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // Load category icon
            loadCategoryIcon(category);
        }
        
        private void loadCategoryIcon(Category category) {
            String iconUrl = null;
            
            // Try to get icon from the category
            if (category.getIcon() != null && category.getIcon().getCategoryIcon() != null) {
                iconUrl = category.getIcon().getCategoryIcon();
            }
            
            // If no icon in the category, try repository
            if (iconUrl == null || iconUrl.isEmpty()) {
                iconUrl = categoryIconRepository.getCategoryIconUrl(category.getName());
            }
            
            // If still no icon, use default
            if (iconUrl == null || iconUrl.isEmpty()) {
                categoryIcon.setImageResource(R.drawable.ic_category);
                return;
            }
            
            // Load icon with Glide
            Glide.with(context)
                .load(iconUrl)
                .apply(new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop())
                .into(categoryIcon);
        }
    }
} 