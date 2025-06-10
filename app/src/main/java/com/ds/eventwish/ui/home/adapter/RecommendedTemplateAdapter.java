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
import java.util.Collections;
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
    
    // Add this constant at the top of the class
    private static final int TAG_ADAPTER = R.id.tag_adapter;
    
    /**
     * Interface for template click events
     */
    public interface TemplateClickListener {
        void onTemplateClick(Template template);
        void onTemplateLike(Template template);
        void onTemplateFavorite(Template template);
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
        private final ImageView likeIcon;
        private final ImageView favoriteIcon;
        private final TextView likeCountText;
        private final TextView favoriteCountText;
        private final View interactionButtonsLayout;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            newBadge = itemView.findViewById(R.id.newBadge);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            cardView = itemView.findViewById(R.id.cardView);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            favoriteCountText = itemView.findViewById(R.id.favoriteCountText);
            interactionButtonsLayout = itemView.findViewById(R.id.interactionButtonsLayout);
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
            
            // Update like icon state and count
            likeIcon.setImageResource(template.isLiked() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            likeIcon.setColorFilter(template.isLiked() ? 
                itemView.getContext().getColor(R.color.colorAccent) : 
                itemView.getContext().getColor(R.color.colorControlNormal));
            
            // Set like count, ensuring it's never negative
            long likeCount = Math.max(0, template.getLikeCount());
            likeCountText.setText(String.valueOf(likeCount));
            Log.d(TAG, "Setting like count for template " + template.getId() + ": " + likeCount);
            
            likeIcon.setOnClickListener(v -> {
                if (listener != null && v.isEnabled()) {
                    // Prevent multiple rapid clicks
                    v.setEnabled(false);
                    
                    // Provide haptic feedback
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    
                    // Immediately update UI for better user feedback
                    boolean newLikeState = !template.isLiked();
                    template.setLiked(newLikeState);
                    likeIcon.setImageResource(newLikeState ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                    likeIcon.setColorFilter(newLikeState ? 
                        itemView.getContext().getColor(R.color.colorAccent) : 
                        itemView.getContext().getColor(R.color.colorControlNormal));
                    
                    // Update count immediately
                    long newCount = newLikeState ? 
                        Math.max(1, likeCount + 1) : 
                        Math.max(0, likeCount - 1);
                    likeCountText.setText(String.valueOf(newCount));
                    
                    // Only trigger like action
                    listener.onTemplateLike(template);
                    
                    // Re-enable after a delay to prevent rapid clicks
                    v.postDelayed(() -> v.setEnabled(true), 1000);
                }
            });
            
            // Update favorite icon state and count
            favoriteIcon.setImageResource(template.isFavorited() ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
            favoriteIcon.setColorFilter(template.isFavorited() ? 
                itemView.getContext().getColor(R.color.colorAccent) : 
                itemView.getContext().getColor(R.color.colorControlNormal));
            
            // Set favorite count, ensuring it's never negative
            long favoriteCount = Math.max(0, template.getFavoriteCount());
            favoriteCountText.setText(String.valueOf(favoriteCount));
            Log.d(TAG, "Setting favorite count for template " + template.getId() + ": " + favoriteCount);
            
            favoriteIcon.setOnClickListener(v -> {
                if (listener != null && v.isEnabled()) {
                    // Prevent multiple rapid clicks
                    v.setEnabled(false);
                    
                    // Provide haptic feedback
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    
                    // Immediately update UI for better user feedback
                    boolean newFavoriteState = !template.isFavorited();
                    template.setFavorited(newFavoriteState);
                    favoriteIcon.setImageResource(newFavoriteState ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
                    favoriteIcon.setColorFilter(newFavoriteState ? 
                        itemView.getContext().getColor(R.color.colorAccent) : 
                        itemView.getContext().getColor(R.color.colorControlNormal));
                    
                    // Update count immediately
                    long newCount = newFavoriteState ? 
                        Math.max(1, favoriteCount + 1) : 
                        Math.max(0, favoriteCount - 1);
                    favoriteCountText.setText(String.valueOf(newCount));
                    
                    // Only trigger favorite action
                    listener.onTemplateFavorite(template);
                    
                    // Re-enable after a delay to prevent rapid clicks
                    v.postDelayed(() -> v.setEnabled(true), 1000);
                }
            });
            
            // Load template image
            String imageUrl = template.getPreviewUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(templateImage.getContext())
                    .load(imageUrl)
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                      Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Failed to load image for template: " + template.getId(), e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                         Target<Drawable> target, DataSource dataSource,
                                                         boolean isFirstResource) {
                                Log.d(TAG, "Image loaded successfully for template: " + template.getId());
                                return false;
                            }
                        })
                        .into(templateImage);
            } else {
                templateImage.setImageResource(R.drawable.placeholder_image);
            }
            
            // Check if this template should show the NEW badge
            boolean isNew = newIds != null && template.getId() != null && newIds.contains(template.getId());
            
            // Check if this template is recommended
            boolean isRecommended = (recommendedIds != null && 
                                    template.getId() != null && 
                                    recommendedIds.contains(template.getId())) || 
                                    template.isRecommended();
            
            // Show NEW badge with higher priority than recommended
            newBadge.setVisibility(isNew ? View.VISIBLE : View.GONE);
            
            // Special styling for templates
            if (isNew) {
                recommendedBadge.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(0xFFF3E5F5); // Light purple background
                cardView.setCardElevation(8f);
            } else if (isRecommended) {
                recommendedBadge.setVisibility(View.VISIBLE);
                cardView.setCardBackgroundColor(0xFFFFF8E1); // Light amber background
                cardView.setCardElevation(8f);
            } else {
                recommendedBadge.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(0xFFFFFFFF); // White background
                cardView.setCardElevation(4f);
            }
            
            // Set click listener for the card
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });

            // Prevent click propagation from interaction buttons
            interactionButtonsLayout.setOnClickListener(v -> {
                // Consume the click event
                Log.d(TAG, "Interaction buttons layout clicked, preventing propagation");
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
        // Set the adapter as a tag on the view holder
        holder.itemView.setTag(TAG_ADAPTER, this);
        
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
     * Update adapter with new templates
     * @param templates List of templates to display
     */
    public void updateTemplates(List<Template> templates) {
        if (templates == null) {
            Log.w(TAG, "Received null templates list");
            return;
        }
        
        Log.d(TAG, "Updating adapter with " + templates.size() + " templates");
        
        // Sort templates by creation date (newest first)
        List<Template> sortedTemplates = new ArrayList<>(templates);
        Collections.sort(sortedTemplates, (t1, t2) -> {
            long time1 = t1.getCreatedAtTimestamp();
            long time2 = t2.getCreatedAtTimestamp();
            // Sort in descending order (newest first)
            return Long.compare(time2, time1);
        });
        
        Log.d(TAG, "Sorted " + sortedTemplates.size() + " templates by creation date (newest first)");
        
        // Clear existing items
        items.clear();
        
        // Split templates into recommended and regular, maintaining the sorted order
        List<Template> recommendedTemplates = new ArrayList<>();
        List<Template> regularTemplates = new ArrayList<>();
        
        for (Template template : sortedTemplates) {
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
        
        // Add regular templates section
        if (!regularTemplates.isEmpty()) {
            items.add(new SectionHeader("All Templates", 
                recommendedTemplates.isEmpty() ? "Choose from our collection of templates" : "Browse all available templates"));
            items.addAll(regularTemplates);
        }
        
        // Notify adapter of changes
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
        
        if (ids == null) {
            // Clear all IDs if null is passed
            if (!newTemplateIds.isEmpty()) {
                newTemplateIds.clear();
                notifyDataSetChanged();
            }
            return;
        }
        
        // Create a copy of the new set to avoid modification issues
        Set<String> newIds = new HashSet<>(ids);
        
        // If there's no change, return early
        if (newTemplateIds.size() == newIds.size() && newTemplateIds.containsAll(newIds)) {
            Log.d(TAG, "No change in new template IDs, skipping update");
            return;
        }
        
        // Update the set with new IDs
        newTemplateIds.clear();
        newTemplateIds.addAll(newIds);
        
        // Log the IDs for debugging
        if (!newTemplateIds.isEmpty()) {
            Log.d(TAG, "New template IDs updated: " + String.join(", ", newTemplateIds));
        }
        
        // Find positions of affected items and update them individually
        // This is more efficient than notifyDataSetChanged()
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Template) {
                Template template = (Template) items.get(i);
                if (template.getId() != null && 
                    (newTemplateIds.contains(template.getId()) || 
                     ids.contains(template.getId()))) {
                    notifyItemChanged(i);
                }
            }
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