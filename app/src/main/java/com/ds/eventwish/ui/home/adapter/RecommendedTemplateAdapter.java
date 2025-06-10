package com.ds.eventwish.ui.home.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
import com.ds.eventwish.utils.NumberFormatter;

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
            
            // Set badges
            if (newIds != null && newIds.contains(template.getId())) {
                newBadge.setVisibility(View.VISIBLE);
            } else {
                newBadge.setVisibility(View.GONE);
            }
            
            if (recommendedIds != null && recommendedIds.contains(template.getId())) {
                recommendedBadge.setVisibility(View.VISIBLE);
            } else {
                recommendedBadge.setVisibility(View.GONE);
            }
            
            // Update like icon state and count
            updateLikeState(template.isLiked(), template.getLikeCount());
            
            // Set up like click listener with debounce
            likeIcon.setTag(R.id.tag_last_click_time, 0L);
            likeIcon.setOnClickListener(v -> {
                // Implement debounce to prevent rapid clicks
                long lastClickTime = (long) v.getTag(R.id.tag_last_click_time);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 1000) { // 1 second debounce
                    return;
                }
                v.setTag(R.id.tag_last_click_time, currentTime);
                
                // Provide haptic feedback
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                
                // Optimistic UI update
                boolean newLikeState = !template.isLiked();
                long newCount = newLikeState ? 
                    Math.max(1, template.getLikeCount() + 1) : 
                    Math.max(0, template.getLikeCount() - 1);
                
                // Update UI immediately
                updateLikeState(newLikeState, newCount);
                
                // Animate the like button
                animateLikeButton(newLikeState);
                
                // Update model
                template.setLiked(newLikeState);
                template.setLikeCount(newCount);
                
                // Notify listener
                if (listener != null) {
                    listener.onTemplateLike(template);
                }
            });
            
            // Update favorite icon state and count
            updateFavoriteState(template.isFavorited(), template.getFavoriteCount());
            
            // Set up favorite click listener with debounce
            favoriteIcon.setTag(R.id.tag_last_click_time, 0L);
            favoriteIcon.setOnClickListener(v -> {
                // Implement debounce to prevent rapid clicks
                long lastClickTime = (long) v.getTag(R.id.tag_last_click_time);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 1000) { // 1 second debounce
                    return;
                }
                v.setTag(R.id.tag_last_click_time, currentTime);
                
                // Provide haptic feedback
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                
                // Optimistic UI update
                boolean newFavoriteState = !template.isFavorited();
                long newCount = newFavoriteState ? 
                    Math.max(1, template.getFavoriteCount() + 1) : 
                    Math.max(0, template.getFavoriteCount() - 1);
                
                // Update UI immediately
                updateFavoriteState(newFavoriteState, newCount);
                
                // Animate the favorite button
                animateFavoriteButton(newFavoriteState);
                
                // Update model
                template.setFavorited(newFavoriteState);
                template.setFavoriteCount(newCount);
                
                // Notify listener
                if (listener != null) {
                    listener.onTemplateFavorite(template);
                }
            });
            
            // Set up card click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
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
                        .centerCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                                      Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Image load failed for template: " + template.getId(), e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                         Target<Drawable> target, DataSource dataSource,
                                                         boolean isFirstResource) {
                                return false;
                            }
                        })
                    .into(templateImage);
            } else {
                // Set placeholder if no image URL
                templateImage.setImageResource(R.drawable.placeholder_image);
            }
        }
        
        // Helper method to update like state
        private void updateLikeState(boolean isLiked, long likeCount) {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.ic_heart_filled);
                likeIcon.setColorFilter(android.graphics.Color.RED);
            } else {
                likeIcon.setImageResource(R.drawable.ic_heart_outline);
                likeIcon.setColorFilter(null);
            }
            
            // Update count text
            if (likeCount > 0) {
                likeCountText.setVisibility(View.VISIBLE);
                likeCountText.setText(NumberFormatter.formatCount(likeCount));
            } else {
                likeCountText.setVisibility(View.GONE);
            }
        }
        
        // Helper method to update favorite state
        private void updateFavoriteState(boolean isFavorited, long favoriteCount) {
            if (isFavorited) {
                favoriteIcon.setImageResource(R.drawable.ic_bookmark_filled);
            } else {
                favoriteIcon.setImageResource(R.drawable.ic_bookmark_outline);
            }
            
            // Update count text
            if (favoriteCount > 0) {
                favoriteCountText.setVisibility(View.VISIBLE);
                favoriteCountText.setText(NumberFormatter.formatCount(favoriteCount));
            } else {
                favoriteCountText.setVisibility(View.GONE);
            }
        }
        
        private void animateLikeButton(boolean liked) {
            // Scale animation
            likeIcon.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction(() -> 
                        likeIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start())
                    .start();
        }
        
        private void animateFavoriteButton(boolean favorited) {
            // Scale animation
            favoriteIcon.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction(() -> 
                        favoriteIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start())
                    .start();
        }
    }
    
    /**
     * Constructor
     */
    public RecommendedTemplateAdapter(TemplateClickListener listener) {
        this.clickListener = listener;
        // Enable stable IDs to prevent blinking during updates
        setHasStableIds(true);
    }
    
    /**
     * Provide stable item IDs to prevent unnecessary rebinding
     */
    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if (item instanceof Template) {
            Template template = (Template) item;
            // Use the template ID's hashCode as the stable ID
            return template.getId().hashCode();
        } else if (item instanceof SectionHeader) {
            // For headers, use a negative hash of the title to avoid conflicts with templates
            SectionHeader header = (SectionHeader) item;
            return -1 * (header.getTitle().hashCode());
        }
        // Fallback to position for other types
        return position;
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
    
    /**
     * Update a template at a specific position
     * @param position Position in the adapter
     * @param template Updated template
     */
    public void updateTemplateAtPosition(int position, Template template) {
        if (position < 0 || position >= items.size()) return;
        
        Object item = items.get(position);
        if (item instanceof Template) {
            // Replace the template at this position
            items.set(position, template);
            // Notify just this item changed
            notifyItemChanged(position);
        }
    }
} 