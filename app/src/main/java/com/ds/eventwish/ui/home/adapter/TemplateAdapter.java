package com.ds.eventwish.ui.home.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.data.repository.EngagementRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple adapter for horizontal template lists showing recommended templates
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {
    private static final String TAG = "HorizontalTemplateAdapter";
    
    private final List<Template> templates = new ArrayList<>();
    private final Set<String> recommendedTemplateIds = new HashSet<>();
    private OnTemplateClickListener listener;
    private CategoryIconRepository categoryIconRepository;
    private EngagementRepository engagementRepository;
    
    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }
    
    public TemplateAdapter(OnTemplateClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = templates.get(position);
        holder.bind(template);
    }
    
    @Override
    public int getItemCount() {
        return templates.size();
    }
    
    public void setCategoryIconRepository(CategoryIconRepository repository) {
        this.categoryIconRepository = repository;
    }
    
    public void setEngagementRepository(EngagementRepository repository) {
        this.engagementRepository = repository;
    }
    
    /**
     * Mark templates as viewed
     */
    public void markAsViewed(String templateId) {
        // Nothing to do here - simplified implementation
    }
    
    /**
     * Update the adapter with new templates
     */
    public void updateTemplates(List<Template> newTemplates) {
        templates.clear();
        if (newTemplates != null) {
            templates.addAll(newTemplates);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Submit a list of templates with recommended IDs
     */
    public void submitListWithRecommendations(List<Template> newTemplates, Set<String> recommendedIds) {
        recommendedTemplateIds.clear();
        if (recommendedIds != null) {
            recommendedTemplateIds.addAll(recommendedIds);
        }
        
        updateTemplates(newTemplates);
    }
    
    public class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView categoryText;
        private final ImageView templateImage;
        private final ImageView categoryIcon;
        private final LinearLayout recommendedBadge;
        private final TextView newBadge;
        private final CardView cardView;
        
        TemplateViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            templateImage = itemView.findViewById(R.id.template_image);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            newBadge = itemView.findViewById(R.id.newBadge);
            cardView = (CardView) itemView;
        }
        
        void bind(Template template) {
            // Set template data
            titleText.setText(template.getTitle());
            categoryText.setText(template.getCategory());
            
            // Always show recommended badge in horizontal list
            recommendedBadge.setVisibility(View.VISIBLE);
            
            // Hide new badge
            newBadge.setVisibility(View.GONE);
            
            // Set card elevation to highlight
            cardView.setCardElevation(8);
            
            // Load thumbnail
            if (template.getThumbnailUrl() != null && !template.getThumbnailUrl().isEmpty()) {
                Log.d(TAG, "Loading thumbnail: " + template.getThumbnailUrl());
                
                RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image);
                
                Glide.with(itemView.getContext())
                    .load(template.getThumbnailUrl())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(templateImage);
            } else {
                templateImage.setImageResource(R.drawable.placeholder_image);
            }
            
            // Load category icon
            loadCategoryIcon(template);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Track engagement safely without disrupting user experience
                    try {
                        if (engagementRepository != null) {
                            engagementRepository.trackTemplateView(
                                template.getId(),
                                template.getCategory(),
                                "direct"
                            );
                            Log.d(TAG, "Horizontal template view tracked: " + template.getId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error tracking template engagement", e);
                    }
                    
                    // Notify listener of click event
                    listener.onTemplateClick(template);
                }
            });
        }
        
        /**
         * Load the category icon for a template
         */
        private void loadCategoryIcon(Template template) {
            if (categoryIconRepository != null && template.getCategory() != null) {
                String iconUrl = categoryIconRepository.getCategoryIconUrl(template.getCategory());
                
                if (iconUrl != null && !iconUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .placeholder(R.drawable.ic_category)
                        .error(R.drawable.ic_category)
                        .into(categoryIcon);
                } else {
                    categoryIcon.setImageResource(R.drawable.ic_category);
                }
            } else {
                categoryIcon.setImageResource(R.drawable.ic_category);
            }
        }
    }
}
