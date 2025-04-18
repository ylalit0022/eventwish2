package com.ds.eventwish.ui.home.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.data.repository.EngagementRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;

/**
 * Enhanced adapter for showing templates with section headers and visual enhancements for recommended templates
 */
public class RecommendedTemplateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "RecommendedAdapter";
    
    // View types for different items
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_TEMPLATE = 1;
    
    // Data
    private final List<Object> items = new ArrayList<>();
    private final Set<String> recommendedTemplateIds = new HashSet<>();
    private final Set<String> newTemplateIds = new HashSet<>();
    
    // Dependencies
    private final TemplateClickListener clickListener;
    private CategoryIconRepository categoryIconRepository;
    private EngagementRepository engagementRepository;
    
    /**
     * Interface for template click events
     */
    public interface TemplateClickListener {
        void onTemplateClick(Template template);
    }
    
    /**
     * Header item for section separation
     */
    public static class SectionHeader {
        private final String title;
        private final String description;
        
        public SectionHeader(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * ViewHolder for section headers
     */
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.headerTitle);
            descriptionTextView = itemView.findViewById(R.id.headerDescription);
        }
        
        public void bind(SectionHeader header) {
            titleTextView.setText(header.getTitle());
            if (header.getDescription() != null && !header.getDescription().isEmpty()) {
                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(header.getDescription());
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * ViewHolder for template items
     */
    public static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView templateImage;
        private final TextView titleText;
        private final TextView categoryText;
        private final ImageView categoryIcon;
        private final TextView newBadge;
        private final LinearLayout recommendedBadge;
        private final CardView cardView;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            newBadge = itemView.findViewById(R.id.newBadge);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            cardView = (CardView) itemView;
        }
        
        public void bind(Template template, Set<String> recommendedIds, Set<String> newIds, TemplateClickListener listener) {
            // Set basic info
            titleText.setText(template.getTitle());
            if (template.getCategory() != null) {
                categoryText.setText(template.getCategory());
                categoryText.setVisibility(View.VISIBLE);
                categoryIcon.setVisibility(View.VISIBLE);
            } else {
                categoryText.setVisibility(View.GONE);
                categoryIcon.setVisibility(View.GONE);
            }
            
            // Check if this template is recommended
            boolean isRecommended = recommendedIds.contains(template.getId()) || template.isRecommended();
            
            // Special styling for recommended templates
            if (isRecommended) {
                recommendedBadge.setVisibility(View.VISIBLE);
                cardView.setCardBackgroundColor(0xFFFFF8E1); // Light amber background
                cardView.setCardElevation(8f); // Increased elevation
            } else {
                recommendedBadge.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(0xFFFFFFFF); // White background
                cardView.setCardElevation(4f); // Normal elevation
            }
            
            // Show NEW badge if needed
            newBadge.setVisibility(newIds.contains(template.getId()) ? View.VISIBLE : View.GONE);
            
            // Load image
            String imageUrl = template.getThumbnailUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(templateImage.getContext())
                    .load(imageUrl)
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Image load failed: " + imageUrl);
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(templateImage);
            } else {
                templateImage.setImageResource(R.drawable.placeholder_image);
            }
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
        }
    }
    
    /**
     * Constructor
     */
    public RecommendedTemplateAdapter(TemplateClickListener listener) {
        this.clickListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof SectionHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_TEMPLATE;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template, parent, false);
            return new TemplateViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((SectionHeader) items.get(position));
        } else if (holder instanceof TemplateViewHolder) {
            ((TemplateViewHolder) holder).bind(
                (Template) items.get(position),
                recommendedTemplateIds,
                newTemplateIds,
                clickListener
            );
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Update the adapter with a new list of templates, automatically organizing them into sections
     */
    public void updateTemplates(List<Template> templates) {
        items.clear();
        
        if (templates == null || templates.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        
        // Split templates into recommended and regular
        List<Template> recommendedTemplates = new ArrayList<>();
        List<Template> regularTemplates = new ArrayList<>();
        
        for (Template template : templates) {
            if (recommendedTemplateIds.contains(template.getId()) || template.isRecommended()) {
                recommendedTemplates.add(template);
            } else {
                regularTemplates.add(template);
            }
        }
        
        // Add recommended section if we have recommended templates
        if (!recommendedTemplates.isEmpty()) {
            items.add(new SectionHeader("Recommended for You", 
                "Personalized recommendations based on your preferences"));
            items.addAll(recommendedTemplates);
        }
        
        // Add regular templates
        if (!regularTemplates.isEmpty()) {
            items.add(new SectionHeader("All Templates", 
                recommendedTemplates.isEmpty() ? "" : "Browse all available templates"));
            items.addAll(regularTemplates);
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * Set recommended template IDs
     */
    public void setRecommendedTemplateIds(Set<String> ids) {
        recommendedTemplateIds.clear();
        if (ids != null) {
            recommendedTemplateIds.addAll(ids);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Set new template IDs for showing the NEW badge
     */
    public void setNewTemplateIds(Set<String> ids) {
        Log.d(TAG, "Setting new template IDs: " + (ids != null ? ids.size() : 0));
        
        // Check if an update is needed
        if (ids == null && newTemplateIds.isEmpty()) {
            return; // No change needed
        }
        
        if (ids == null && !newTemplateIds.isEmpty()) {
            // Clear all IDs
            newTemplateIds.clear();
            notifyDataSetChanged();
            return;
        }
        
        // Check for actual changes
        boolean hasChanges = false;
        
        // Check if any IDs were added
        for (String id : ids) {
            if (!newTemplateIds.contains(id)) {
                hasChanges = true;
                break;
            }
        }
        
        // Check if any IDs were removed
        if (!hasChanges && newTemplateIds.size() != ids.size()) {
            hasChanges = true;
        }
        
        // Only update if there are actual changes
        if (hasChanges) {
            newTemplateIds.clear();
            newTemplateIds.addAll(ids);
            
            // Log the IDs for debugging
            if (!newTemplateIds.isEmpty()) {
                Log.d(TAG, "New template IDs: " + String.join(", ", newTemplateIds));
            }
            
            // Update the adapter
            notifyDataSetChanged();
        }
    }
    
    /**
     * Mark a template as viewed (no longer new)
     */
    public void markAsViewed(String templateId) {
        if (newTemplateIds.contains(templateId)) {
            newTemplateIds.remove(templateId);
            
            // Find the position of this template and update it
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof Template) {
                    Template template = (Template) items.get(i);
                    if (template.getId().equals(templateId)) {
                        notifyItemChanged(i);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Set the CategoryIconRepository for loading category icons
     */
    public void setCategoryIconRepository(CategoryIconRepository repository) {
        this.categoryIconRepository = repository;
    }
    
    /**
     * Set the EngagementRepository for tracking
     */
    public void setEngagementRepository(EngagementRepository repository) {
        this.engagementRepository = repository;
    }
    
    /**
     * Load a category icon
     */
    private void loadCategoryIcon(Template template, ImageView imageView) {
        if (template.getCategory() == null || categoryIconRepository == null) {
            imageView.setImageResource(R.drawable.ic_category);
            return;
        }
        
        String iconUrl = categoryIconRepository.getCategoryIconUrl(template.getCategory());
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Glide.with(imageView.getContext())
                .load(iconUrl)
                .placeholder(R.drawable.ic_category)
                .error(R.drawable.ic_category)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_category);
        }
    }
    
    /**
     * Submit a list of templates and mark recommended ones
     */
    public void submitListWithRecommendations(List<Template> templates, Set<String> recommendedIds) {
        setRecommendedTemplateIds(recommendedIds);
        updateTemplates(templates);
    }
    
    /**
     * Get item at position
     */
    public Object getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }
} 