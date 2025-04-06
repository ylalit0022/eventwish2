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
        private final TextView titleView;
        private final TextView descriptionView;
        
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.sectionTitle);
            descriptionView = itemView.findViewById(R.id.sectionDescription);
        }
        
        public void bind(SectionHeader header) {
            titleView.setText(header.getTitle());
            descriptionView.setText(header.getDescription());
        }
    }
    
    /**
     * ViewHolder for template items
     */
    public class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView categoryView;
        private final ImageView thumbnailView;
        private final ImageView categoryIconView;
        private final LinearLayout recommendedBadge;
        private final TextView newBadge;
        private final CardView cardView;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            titleView = itemView.findViewById(R.id.titleText);
            categoryView = itemView.findViewById(R.id.categoryText);
            thumbnailView = itemView.findViewById(R.id.template_image);
            categoryIconView = itemView.findViewById(R.id.categoryIcon);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            newBadge = itemView.findViewById(R.id.newBadge);
        }
        
        public void bind(Template template) {
            // Set text
            titleView.setText(template.getTitle());
            categoryView.setText(template.getCategory());
            
            // Check if template is recommended
            boolean isRecommended = template.isRecommended() || 
                                   recommendedTemplateIds.contains(template.getId());
            recommendedBadge.setVisibility(isRecommended ? View.VISIBLE : View.GONE);
            
            // Apply special styling for recommended templates
            if (isRecommended) {
                cardView.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorAccent, null));
                cardView.setCardElevation(8); // Increased elevation
            } else {
                cardView.setCardBackgroundColor(itemView.getContext().getResources().getColor(android.R.color.white, null));
                cardView.setCardElevation(4); // Default elevation
            }
            
            // Check if template is new
            boolean isNew = newTemplateIds.contains(template.getId());
            newBadge.setVisibility(isNew ? View.VISIBLE : View.GONE);
            
            // Load thumbnail
            if (template.getThumbnailUrl() != null && !template.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(template.getThumbnailUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(thumbnailView);
            } else {
                thumbnailView.setImageResource(R.drawable.placeholder_image);
            }
            
            // Load category icon
            loadCategoryIcon(template, categoryIconView);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTemplateClick(template);
                    
                    // Mark as viewed
                    if (isNew) {
                        markAsViewed(template.getId());
                    }
                    
                    // Track engagement
                    try {
                        if (engagementRepository != null) {
                            String source = isRecommended ? 
                                "recommendation" : "direct";
                            engagementRepository.trackTemplateView(
                                template.getId(),
                                template.getCategory(),
                                source
                            );
                            Log.d(TAG, "Tracked template view: " + template.getId() + 
                                  " (recommended: " + isRecommended + ")");
                        } else {
                            Log.w(TAG, "EngagementRepository not initialized - tracking skipped");
                        }
                    } catch (Exception e) {
                        // Prevent engagement tracking errors from disrupting the user experience
                        Log.e(TAG, "Error tracking template engagement", e);
                    }
                }
            });
        }
    }
    
    public RecommendedTemplateAdapter(TemplateClickListener listener) {
        this.clickListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof SectionHeader ? 
            VIEW_TYPE_HEADER : VIEW_TYPE_TEMPLATE;
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
            ((TemplateViewHolder) holder).bind((Template) items.get(position));
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
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
        newTemplateIds.clear();
        if (ids != null) {
            newTemplateIds.addAll(ids);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Mark a template as viewed (no longer new)
     */
    public void markAsViewed(String templateId) {
        if (newTemplateIds.contains(templateId)) {
            newTemplateIds.remove(templateId);
            
            // Find and update the specific item
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
     * Update templates with sections for recommended and regular templates
     */
    public void updateTemplates(List<Template> templates) {
        if (templates == null) {
            templates = new ArrayList<>();
        }
        
        // Clear existing items
        items.clear();
        
        // Separate recommended and regular templates
        List<Template> recommendedTemplates = new ArrayList<>();
        List<Template> regularTemplates = new ArrayList<>();
        
        for (Template template : templates) {
            boolean isRecommended = template.isRecommended() || 
                                   recommendedTemplateIds.contains(template.getId());
            if (isRecommended) {
                recommendedTemplates.add(template);
            } else {
                regularTemplates.add(template);
            }
        }
        
        // Add recommended section if we have any
        if (!recommendedTemplates.isEmpty()) {
            // Add section header
            items.add(new SectionHeader(
                "Recommended For You",
                "Personalized templates based on your preferences"
            ));
            
            // Add all recommended templates
            items.addAll(recommendedTemplates);
            
            Log.d(TAG, "Added recommended section with " + recommendedTemplates.size() + " templates");
        }
        
        // Add regular templates 
        if (!regularTemplates.isEmpty()) {
            // Add section header if we have both sections
            if (!recommendedTemplates.isEmpty()) {
                items.add(new SectionHeader(
                    "More Templates",
                    "Browse our full collection of templates"
                ));
            }
            
            // Add all regular templates
            items.addAll(regularTemplates);
            
            Log.d(TAG, "Added regular section with " + regularTemplates.size() + " templates");
        }
        
        // Notify adapter of changes
        notifyDataSetChanged();
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
} 