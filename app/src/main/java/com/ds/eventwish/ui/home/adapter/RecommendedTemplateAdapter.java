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
import com.ds.eventwish.data.repository.CreatorProfileRepository;
import com.ds.eventwish.utils.NumberFormatter;
import com.google.gson.JsonObject;

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
    private CreatorProfileRepository creatorProfileRepository;
    
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
        private final ImageView shareIcon;
        private final TextView likeCountText;
        private final TextView favoriteCountText;
        private final TextView shareCountText;
        private final TextView timeText;
        private final TextView fallbackTimeText;
        private final ImageView profileImage;
        private final TextView usernameText;

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
            shareIcon = itemView.findViewById(R.id.shareIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            favoriteCountText = itemView.findViewById(R.id.favoriteCountText);
            shareCountText = itemView.findViewById(R.id.shareCountText);
            timeText = itemView.findViewById(R.id.timeText);
            fallbackTimeText = itemView.findViewById(R.id.fallbackTimeText);
            profileImage = itemView.findViewById(R.id.profileImage);
            usernameText = itemView.findViewById(R.id.usernameText);
        }
        
        public void bind(Template template, Set<String> recommendedIds, Set<String> newIds, TemplateClickListener listener, RecommendedTemplateAdapter adapter) {
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
            
            // Set creation time display
            String timeAgoText = adapter.formatTimeAgo(template.getCreatedAt());
            Log.d(TAG, "Template " + template.getId() + " formatted time: " + timeAgoText);
            Log.d(TAG, "Template " + template.getId() + " createdAt: " + (template.getCreatedAt() != null ? template.getCreatedAt().toString() : "null"));
            
            if (timeText != null) {
                String timeTextValue = "⏱️ " + timeAgoText;
                timeText.setText(timeTextValue);
                timeText.setVisibility(View.VISIBLE);
                Log.d(TAG, "Setting timeText to: " + timeTextValue);
            } else {
                Log.w(TAG, "timeText view is null!");
            }
            
            // Only show fallback for debugging purposes
            if (fallbackTimeText != null) {
                // Hide fallback by default - only show for debugging
                fallbackTimeText.setVisibility(View.GONE);
                Log.d(TAG, "Fallback time text hidden (for debugging only)");
            } else {
                Log.w(TAG, "fallbackTimeText view is null!");
            }
            
            // Set creator profile information
            setCreatorProfile(template);
            
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
            
            // Update share count display
            updateShareState(template);
            
            // Share icon is display-only, no click interaction needed
            shareIcon.setOnClickListener(null);
            shareIcon.setClickable(false);
            shareIcon.setFocusable(false);
            
            // Set up template image click listener
            templateImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
            
            // Remove card view click listener
            cardView.setOnClickListener(null);
            cardView.setClickable(false);
            cardView.setFocusable(false);
            
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
            
            // Update count text - always show count, even if 0
            likeCountText.setVisibility(View.VISIBLE);
            likeCountText.setText(NumberFormatter.formatCount(likeCount));
        }
        
        // Helper method to update favorite state
        private void updateFavoriteState(boolean isFavorited, long favoriteCount) {
            if (isFavorited) {
                favoriteIcon.setImageResource(R.drawable.ic_bookmark_filled);
            } else {
                favoriteIcon.setImageResource(R.drawable.ic_bookmark_outline);
            }
            
            // Update count text - always show count, even if 0
            favoriteCountText.setVisibility(View.VISIBLE);
            favoriteCountText.setText(NumberFormatter.formatCount(favoriteCount));
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
        
        // Helper method to update share state
        private void updateShareState(Template template) {
            long shareCount = template.getShareCount();
            // Update count text - always show count, even if 0
            shareCountText.setVisibility(View.VISIBLE);
            shareCountText.setText(template.getFormattedShareCount());
        }
        
        private void animateShareButton() {
            // Scale animation
            shareIcon.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction(() -> 
                        shareIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start())
                    .start();
        }
        
        /**
         * Set creator profile information using server-side API with fallback to app branding
         */
        private void setCreatorProfile(Template template) {
            if (profileImage == null || usernameText == null) {
                Log.w(TAG, "Profile views are null - profileImage: " + profileImage + ", usernameText: " + usernameText);
                return;
            }
            
            // Set default values immediately for better UX
            setDefaultCreatorProfile();
            
            // Get creator profile from server-side API
            if (template.getId() != null && !template.getId().trim().isEmpty()) {
                Log.d(TAG, "Fetching creator profile for template: " + template.getId());
                
                // Use the adapter's creator profile repository
                RecommendedTemplateAdapter adapter = getAdapterFromViewHolder();
                if (adapter != null && adapter.creatorProfileRepository != null) {
                    adapter.creatorProfileRepository.getCreatorProfile(template.getId())
                        .observeForever(response -> {
                            if (response != null) {
                                updateCreatorProfileUI(response, template.getId());
                            } else {
                                Log.w(TAG, "Failed to fetch creator profile for template: " + template.getId() + 
                                          ", using default profile");
                            }
                        });
                } else {
                    Log.w(TAG, "CreatorProfileRepository is not available, using default profile");
                }
            } else {
                Log.w(TAG, "Template ID is null or empty, using default profile");
            }
        }
        
        /**
         * Set default creator profile (app branding)
         */
        private void setDefaultCreatorProfile() {
            usernameText.setText("eventwish");
            profileImage.setImageResource(R.drawable.app_logo);
            Log.d(TAG, "Set default creator profile: eventwish with app logo");
        }
        
        /**
         * Update UI with creator profile information from server response
         */
        private void updateCreatorProfileUI(JsonObject response, String templateId) {
            try {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    if (response.has("data") && response.get("data").isJsonObject()) {
                        JsonObject data = response.getAsJsonObject("data");
                        
                        if (data.has("creatorProfile") && data.get("creatorProfile").isJsonObject()) {
                            JsonObject creatorProfile = data.getAsJsonObject("creatorProfile");
                            
                            // Set creator name with fallback priority
                            String displayName = "eventwish"; // Default fallback
                            if (creatorProfile.has("creatorName") && !creatorProfile.get("creatorName").isJsonNull()) {
                                String creatorName = creatorProfile.get("creatorName").getAsString();
                                if (creatorName != null && !creatorName.trim().isEmpty()) {
                                    displayName = creatorName.trim();
                                }
                            } else if (creatorProfile.has("generatedByUser") && !creatorProfile.get("generatedByUser").isJsonNull()) {
                                String generatedByUser = creatorProfile.get("generatedByUser").getAsString();
                                if (generatedByUser != null && !generatedByUser.trim().isEmpty()) {
                                    displayName = generatedByUser.trim();
                                }
                            }
                            usernameText.setText(displayName);
                            Log.d(TAG, "Updated username for template " + templateId + " to: " + displayName);
                            
                            // Set creator profile image
                            String profilePhotoUrl = null;
                            if (creatorProfile.has("creatorProfilePhoto") && !creatorProfile.get("creatorProfilePhoto").isJsonNull()) {
                                profilePhotoUrl = creatorProfile.get("creatorProfilePhoto").getAsString();
                            }
                            
                            if (profilePhotoUrl != null && !profilePhotoUrl.trim().isEmpty()) {
                                final String imageUrl = profilePhotoUrl.trim();
                                Log.d(TAG, "Loading creator profile image for template " + templateId + ": " + imageUrl);
                                
                                Glide.with(profileImage.getContext())
                                    .load(imageUrl)
                                    .apply(new RequestOptions()
                                        .placeholder(R.drawable.app_logo)
                                        .error(R.drawable.app_logo)
                                        .circleCrop()
                                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                                    .listener(new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                                                  Target<Drawable> target, boolean isFirstResource) {
                                            Log.w(TAG, "Failed to load creator profile image for template " + templateId + ": " + imageUrl, e);
                                            return false; // Let Glide handle the error (show error drawable)
                                        }

                                        @Override
                                        public boolean onResourceReady(Drawable resource, Object model,
                                                                     Target<Drawable> target, DataSource dataSource,
                                                                     boolean isFirstResource) {
                                            Log.d(TAG, "Successfully loaded creator profile image for template " + templateId);
                                            return false; // Let Glide handle the resource
                                        }
                                    })
                                    .into(profileImage);
                            } else {
                                // Fallback to app logo
                                profileImage.setImageResource(R.drawable.app_logo);
                                Log.d(TAG, "Using fallback app logo for template " + templateId);
                            }
                            
                            String creatorSource = data.has("creatorSource") ? data.get("creatorSource").getAsString() : "api";
                            Log.d(TAG, "Creator profile UI updated successfully - Source: " + creatorSource);
                        } else {
                            Log.w(TAG, "Creator profile object is missing for template " + templateId + ", using default");
                        }
                    } else {
                        Log.w(TAG, "Data object is missing for template " + templateId + ", using default");
                    }
                } else {
                    Log.w(TAG, "Creator profile response unsuccessful for template " + templateId + ", using default");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating creator profile UI for template " + templateId, e);
            }
        }
        
        /**
         * Get the adapter instance from the ViewHolder
         */
        private RecommendedTemplateAdapter getAdapterFromViewHolder() {
            Object tag = itemView.getTag(TAG_ADAPTER);
            if (tag instanceof RecommendedTemplateAdapter) {
                return (RecommendedTemplateAdapter) tag;
            }
            Log.w(TAG, "Failed to get adapter from ViewHolder tag");
            return null;
        }
    }
    
    /**
     * Constructor
     */
    public RecommendedTemplateAdapter(TemplateClickListener listener) {
        this.clickListener = listener;
        // Enable stable IDs to prevent blinking during updates
        setHasStableIds(true);
        
        // Initialize repositories
        this.creatorProfileRepository = CreatorProfileRepository.getInstance();
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
        
        try {
            if (holder instanceof HeaderViewHolder) {
                SectionHeader header = (SectionHeader) items.get(position);
                Log.d(TAG, "onBindViewHolder: Binding header at position " + position + ": " + header.getTitle());
                ((HeaderViewHolder) holder).bind(header);
            } else if (holder instanceof TemplateViewHolder) {
                Template template = (Template) items.get(position);
                Log.d(TAG, "onBindViewHolder: Binding template at position " + position + 
                      ", ID: " + template.getId() + 
                      ", Title: " + template.getTitle() +
                      ", Preview URL: " + template.getPreviewUrl());
                ((TemplateViewHolder) holder).bind(
                    template,
                    recommendedTemplateIds,
                    newTemplateIds,
                    clickListener,
                    this
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "onBindViewHolder: Error binding view at position " + position, e);
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
            Log.e(TAG, "updateTemplates: Received null templates list");
            return;
        }
        
        Log.d(TAG, "updateTemplates: Updating adapter with " + templates.size() + " templates");
        
        // Sort templates by creation date (newest first)
        List<Template> sortedTemplates = new ArrayList<>(templates);
        Collections.sort(sortedTemplates, (t1, t2) -> {
            long time1 = t1.getCreatedAtTimestamp();
            long time2 = t2.getCreatedAtTimestamp();
            // Sort in descending order (newest first)
            return Long.compare(time2, time1);
        });
        
        Log.d(TAG, "updateTemplates: Sorted " + sortedTemplates.size() + " templates by creation date (newest first)");
        
        // Clear existing items
        items.clear();
        
        // Split templates into recommended and regular, maintaining the sorted order
        List<Template> recommendedTemplates = new ArrayList<>();
        List<Template> regularTemplates = new ArrayList<>();
        
        for (Template template : sortedTemplates) {
            if (recommendedTemplateIds.contains(template.getId()) || template.isRecommended()) {
                recommendedTemplates.add(template);
                Log.d(TAG, "updateTemplates: Added template to recommended section: " + template.getId() + ", " + template.getTitle());
            } else {
                regularTemplates.add(template);
            }
        }
        
        // Add recommended section if we have recommended templates
        if (!recommendedTemplates.isEmpty()) {
            items.add(new SectionHeader("Recommended for You", 
                "Personalized recommendations based on your preferences"));
            items.addAll(recommendedTemplates);
            Log.d(TAG, "updateTemplates: Added recommended section with " + recommendedTemplates.size() + " templates");
        }
        
        // Add regular templates section
        if (!regularTemplates.isEmpty()) {
            items.add(new SectionHeader("All Templates", 
                recommendedTemplates.isEmpty() ? "Choose from our collection of templates" : "Browse all available templates"));
            items.addAll(regularTemplates);
            Log.d(TAG, "updateTemplates: Added regular templates section with " + regularTemplates.size() + " templates");
        }
        
        // Notify adapter of changes
        Log.d(TAG, "updateTemplates: Final items count: " + items.size() + " (including headers)");
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
        if (position >= 0 && position < items.size() && items.get(position) instanceof Template) {
            items.set(position, template);
            notifyItemChanged(position);
        }
    }
    
    /**
     * Formats a date into a social media style time ago string
     * @param date The date to format
     * @return A string like "2h ago", "3d ago", etc.
     */
    String formatTimeAgo(java.util.Date date) {
        if (date == null) {
            Log.d(TAG, "formatTimeAgo: date is null, returning 'recently added'");
            return "recently added";
        }
        
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;
        
        Log.d(TAG, "formatTimeAgo: date=" + date + ", now=" + new java.util.Date(now) + ", diff=" + diff + "ms (" + (diff/1000) + " seconds)");
        
        // Check if date is in the future (server time might be ahead)
        if (diff < 0) {
            Log.d(TAG, "formatTimeAgo: date is in the future, using 'recently added'");
            return "recently added";
        }
        
        // Convert to seconds
        long seconds = diff / 1000;
        if (seconds < 60) {
            return "just now";
        }
        
        // Convert to minutes
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        
        // Convert to hours
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        
        // Convert to days
        long days = hours / 24;
        if (days < 7) {
            return days + "d ago";
        }
        
        // Convert to weeks
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + "w ago";
        }
        
        // Convert to months (approximate)
        long months = days / 30;
        if (months < 12) {
            return months + "mo ago";
        }
        
        // Convert to years
        long years = days / 365;
        return years + "y ago";
    }
} 