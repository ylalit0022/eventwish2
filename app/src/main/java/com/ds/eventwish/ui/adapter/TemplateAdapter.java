package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.TemplateInteractionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.core.content.ContextCompat;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private final Context context;
    private ArrayList<Template> templates;
    private final TemplateInteractionManager interactionManager;
    private OnItemClickListener onItemClickListener;
    private OnTemplateInteractionListener onTemplateInteractionListener;
    private static final long CLICK_DEBOUNCE_TIME = 800; // ms - longer debounce time for network operations
    private final Map<String, Long> lastClickTimes = new HashMap<>();
    private static final String TAG = "TemplateAdapter";
    
    // Add debouncing for setTemplates to prevent excessive updates
    private long lastSetTemplatesTime = 0;
    private static final long SET_TEMPLATES_DEBOUNCE_TIME = 1000; // 1 second debounce

    public TemplateAdapter(Context context) {
        this.context = context;
        this.templates = new ArrayList<>();
        this.interactionManager = TemplateInteractionManager.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Template template = templates.get(position);
        
        Log.d(TAG, "onBindViewHolder: Binding template at position " + position + 
              ", ID: " + template.getId() + 
              ", Title: " + template.getTitle() +
              ", Preview URL: " + template.getPreviewUrl());
        
        // Set title and category
        holder.titleText.setText(template.getTitle());
        holder.categoryText.setText(template.getCategoryId());
        
        // Add detailed logging for time display debugging
        Log.d(TAG, "Template " + template.getId() + " time display: " +
              "createdAt=" + (template.getCreatedAt() != null ? template.getCreatedAt().toString() : "null") + 
              ", timestamp=" + template.getCreatedAtTimestamp());
        
        // Set creation time using actual template data
        String timeAgoText;
        if (template.getCreatedAt() != null) {
            timeAgoText = formatTimeAgo(template.getCreatedAt());
            Log.d(TAG, "Template " + template.getId() + " formatted time: " + timeAgoText);
        } else {
            timeAgoText = "recently added";
            Log.d(TAG, "Template " + template.getId() + " using fallback time (createdAt is null)");
        }
        
        // Create a SpannableString for the actual time
        SpannableString timeSpannable = new SpannableString("⏱️ " + timeAgoText.toUpperCase());
        timeSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, timeSpannable.length(), 0);
        timeSpannable.setSpan(new RelativeSizeSpan(1.2f), 0, timeSpannable.length(), 0);
        
        // Set the actual time text
        holder.timeText.setText(timeSpannable);
        holder.timeText.setVisibility(View.VISIBLE);
        holder.timeText.setTextColor(Color.WHITE);
        
        // Set the fallback time text with actual data too
        holder.fallbackTimeText.setText("⏱️ POSTED " + timeAgoText.toUpperCase());
        holder.fallbackTimeText.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Setting timeText to actual template time: " + timeAgoText);
        
        // Set like and favorite icons
        updateLikeState(holder, template.isLiked());
        updateFavoriteState(holder, template.isFavorited());
        
        // Set like count
        if (template.getLikeCount() > 0) {
            holder.likeCountText.setVisibility(View.VISIBLE);
            holder.likeCountText.setText(String.valueOf(template.getLikeCount()));
        } else {
            holder.likeCountText.setVisibility(View.GONE);
        }
        
        // Set badges
        if (isNewTemplate(template)) {
            holder.newBadge.setVisibility(View.VISIBLE);
        } else {
            holder.newBadge.setVisibility(View.GONE);
        }
        
        // Note: Recommendation system removed
        holder.recommendedBadge.setVisibility(View.GONE);
        
        // Load image
        Log.d(TAG, "Loading image for template " + template.getId() + " from URL: " + template.getPreviewUrl());
        Glide.with(holder.itemView.getContext())
                .load(template.getPreviewUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.templateImage);
                
        // Set click listeners
        holder.templateImage.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                Log.d(TAG, "Template clicked: " + template.getId());
                onItemClickListener.onItemClick(template);
            }
        });
        
        holder.likeIcon.setOnClickListener(v -> {
            Log.d(TAG, "=== LIKE BUTTON CLICKED ===");
            Log.d(TAG, "Template ID: " + template.getId());
            Log.d(TAG, "Current position: " + holder.getAdapterPosition());
            Log.d(TAG, "Layout position: " + holder.getLayoutPosition());
            Log.d(TAG, "Current liked state: " + template.isLiked());
            Log.d(TAG, "Current like count: " + template.getLikeCount());
            
            // Prevent rapid clicks
            if (!v.isEnabled()) {
                Log.w(TAG, "Like button disabled - preventing rapid clicks");
                return;
            }
            
            // Disable button temporarily to prevent rapid clicks
            v.setEnabled(false);
            v.postDelayed(() -> v.setEnabled(true), 1000);
            
            // Get current adapter position
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                Log.e(TAG, "Invalid adapter position - aborting like action");
                return;
            }
            
            Log.d(TAG, "Performing optimistic UI update at position: " + currentPosition);
            
            // Optimistic UI update - THIS MIGHT CAUSE JUMPING!
            boolean newLikedState = !template.isLiked();
            long newLikeCount = template.getLikeCount() + (newLikedState ? 1 : -1);
            
            Log.d(TAG, "Optimistic update: liked " + template.isLiked() + " -> " + newLikedState);
            Log.d(TAG, "Optimistic update: count " + template.getLikeCount() + " -> " + newLikeCount);
            
            // Update template state immediately for UI responsiveness
            template.setLiked(newLikedState);
            template.setLikeCount(Math.max(0, newLikeCount));
            
            // Update UI immediately - THIS MIGHT CAUSE JUMPING!
            Log.w(TAG, "Updating like button UI immediately - POTENTIAL JUMPING CAUSE!");
            
            // Animate the like button
            animateLikeButton(holder.likeIcon, newLikedState);
            
            // Update UI state
            updateLikeState(holder, newLikedState);
            
            // Update like count display
            if (template.getLikeCount() > 0) {
                holder.likeCountText.setText(String.valueOf(template.getLikeCount()));
                holder.likeCountText.setVisibility(View.VISIBLE);
                Log.d(TAG, "Like count updated to: " + template.getLikeCount());
            } else {
                holder.likeCountText.setVisibility(View.GONE);
                Log.d(TAG, "Like count hidden (count is 0)");
            }
            
            // Show toast message
            Toast.makeText(context, newLikedState ? "Liked" : "Unliked", Toast.LENGTH_SHORT).show();
            
            // Notify the listener
            if (onTemplateInteractionListener != null) {
                Log.d(TAG, "Notifying interaction listener of like action");
                onTemplateInteractionListener.onTemplateLiked(template, newLikedState);
            } else {
                Log.w(TAG, "No interaction listener set!");
            }
            
            Log.d(TAG, "=== LIKE BUTTON CLICK COMPLETED ===");
        });
        
        holder.favoriteIcon.setOnClickListener(v -> {
            boolean newFavoriteState = !template.isFavorited();
            template.setFavorited(newFavoriteState);
            
            Log.d(TAG, "Template " + template.getId() + " favorite toggled to: " + newFavoriteState);
            
            // Animate the favorite button
            animateFavoriteButton(holder.favoriteIcon, newFavoriteState);
            
            // Update UI
            updateFavoriteState(holder, newFavoriteState);
            
            // Show toast message
            Toast.makeText(context, newFavoriteState ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
            
            // Notify listener
            if (onTemplateInteractionListener != null) {
                onTemplateInteractionListener.onTemplateFavorited(template, newFavoriteState);
            }
        });
    }
    
    /**
     * Determines if a template is new based on its creation date
     * @param template The template to check
     * @return true if the template is new (less than 7 days old)
     */
    private boolean isNewTemplate(Template template) {
        if (template.getCreatedAt() == null) return false;
        
        long now = System.currentTimeMillis();
        long createdTime = template.getCreatedAt().getTime();
        long daysDiff = (now - createdTime) / (1000 * 60 * 60 * 24);
        
        return daysDiff < 7; // Template is new if less than 7 days old
    }
    
    private void animateLikeButton(ImageView likeIcon, boolean liked) {
        if (liked) {
            likeIcon.setImageResource(R.drawable.ic_heart_filled);
            likeIcon.setColorFilter(Color.RED);
        } else {
            likeIcon.setImageResource(R.drawable.ic_heart_outline);
            likeIcon.setColorFilter(null);
        }
        
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

    private void animateFavoriteButton(ImageView favoriteIcon, boolean favorited) {
        if (favorited) {
            favoriteIcon.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_bookmark_outline);
        }
        
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

    private void updateLikeState(ViewHolder holder, boolean liked) {
        if (liked) {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_filled);
            holder.likeIcon.setColorFilter(Color.RED);
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_outline);
            holder.likeIcon.setColorFilter(null);
        }
    }

    private void updateFavoriteState(ViewHolder holder, boolean favorited) {
        if (favorited) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_bookmark_outline);
        }
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    /**
     * Set the list of templates and update the adapter
     */
    public void setTemplates(List<Template> templates) {
        Log.d(TAG, "=== SET TEMPLATES CALLED ===");
        Log.d(TAG, "New templates count: " + (templates != null ? templates.size() : 0));
        Log.d(TAG, "Current templates count: " + (this.templates != null ? this.templates.size() : 0));
        
        if (templates == null) {
            Log.w(TAG, "Received null templates list");
            return;
        }
        
        // Debounce rapid setTemplates calls to prevent excessive updates
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSetTemplatesTime < SET_TEMPLATES_DEBOUNCE_TIME) {
            Log.d(TAG, "Debouncing setTemplates call - too soon since last update (" + 
                  (currentTime - lastSetTemplatesTime) + "ms ago)");
            return;
        }
        lastSetTemplatesTime = currentTime;
        
        // Log first few template IDs for debugging
        if (!templates.isEmpty()) {
            Log.d(TAG, "First template ID: " + templates.get(0).getId());
            if (templates.size() > 1) {
                Log.d(TAG, "Second template ID: " + templates.get(1).getId());
            }
        }
        
        // Check if this is the same data
        boolean isSameData = false;
        if (this.templates != null && this.templates.size() == templates.size()) {
            isSameData = true;
            for (int i = 0; i < templates.size(); i++) {
                if (!this.templates.get(i).getId().equals(templates.get(i).getId())) {
                    isSameData = false;
                    break;
                }
            }
        }
        
        Log.d(TAG, "Is same data: " + isSameData);
        
        if (isSameData) {
            Log.d(TAG, "Same template data detected - checking for state changes");
            // Check if any template states have changed
            boolean hasStateChanges = false;
            for (int i = 0; i < templates.size(); i++) {
                Template oldTemplate = this.templates.get(i);
                Template newTemplate = templates.get(i);
                
                if (oldTemplate.isLiked() != newTemplate.isLiked() ||
                    oldTemplate.isFavorited() != newTemplate.isFavorited() ||
                    oldTemplate.getLikeCount() != newTemplate.getLikeCount() ||
                    oldTemplate.getFavoriteCount() != newTemplate.getFavoriteCount()) {
                    
                    Log.d(TAG, "State change detected in template " + newTemplate.getId() + 
                          " at position " + i);
                    Log.d(TAG, "  Liked: " + oldTemplate.isLiked() + " -> " + newTemplate.isLiked());
                    Log.d(TAG, "  Like count: " + oldTemplate.getLikeCount() + " -> " + newTemplate.getLikeCount());
                    Log.d(TAG, "  Favorited: " + oldTemplate.isFavorited() + " -> " + newTemplate.isFavorited());
                    Log.d(TAG, "  Favorite count: " + oldTemplate.getFavoriteCount() + " -> " + newTemplate.getFavoriteCount());
                    
                    hasStateChanges = true;
                    
                    // Call notifyItemChanged for this specific position - THIS MIGHT CAUSE JUMPING!
                    Log.w(TAG, "Calling notifyItemChanged(" + i + ") - POTENTIAL JUMPING CAUSE!");
                    notifyItemChanged(i);
                }
            }
            
            if (hasStateChanges) {
                Log.w(TAG, "Template states updated with individual notifyItemChanged calls");
            } else {
                Log.d(TAG, "No state changes detected - skipping unnecessary updates");
            }
            
            // Update the data reference
            this.templates = new ArrayList<>(templates);
            return;
        }
        
        Log.d(TAG, "Different template data - performing full update");
        this.templates = new ArrayList<>(templates);
        
        Log.w(TAG, "Calling notifyDataSetChanged() - POTENTIAL JUMPING CAUSE!");
        notifyDataSetChanged();
        
        Log.d(TAG, "=== SET TEMPLATES COMPLETED ===");
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnTemplateInteractionListener(OnTemplateInteractionListener listener) {
        this.onTemplateInteractionListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Template template);
    }
    
    public interface OnTemplateInteractionListener {
        void onTemplateLiked(Template template, boolean liked);
        void onTemplateFavorited(Template template, boolean favorited);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView templateImage;
        final TextView titleText;
        final TextView categoryText;
        final ImageView categoryIcon;
        final ImageView likeIcon;
        final TextView likeCountText;
        final ImageView favoriteIcon;
        final TextView favoriteCountText;
        final TextView newBadge;
        final LinearLayout recommendedBadge;
        final TextView timeText;
        final TextView fallbackTimeText;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            favoriteCountText = itemView.findViewById(R.id.favoriteCountText);
            newBadge = itemView.findViewById(R.id.newBadge);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            timeText = itemView.findViewById(R.id.timeText);
            fallbackTimeText = itemView.findViewById(R.id.fallbackTimeText);
        }
    }

    /**
     * Formats a date into a social media style time ago string
     * @param date The date to format
     * @return A string like "2h ago", "3d ago", etc.
     */
    private String formatTimeAgo(Date date) {
        if (date == null) {
            Log.d(TAG, "formatTimeAgo: date is null, returning 'recently added'");
            return "recently added";
        }
        
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;
        
        Log.d(TAG, "formatTimeAgo: date=" + date + ", now=" + new Date(now) + ", diff=" + diff + "ms (" + (diff/1000) + " seconds)");
        
        // Check if date is in the future (server time might be ahead)
        if (diff < 0) {
            Log.d(TAG, "formatTimeAgo: date is in the future, using 'recently added'");
            return "recently added";
        }
        
        // Convert to seconds
        long seconds = diff / 1000;
        if (seconds < 60) {
            Log.d(TAG, "formatTimeAgo: returning 'just now' for " + seconds + " seconds");
            return "just now";
        }
        
        // Convert to minutes
        long minutes = seconds / 60;
        if (minutes < 60) {
            String result = minutes + "m ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + minutes + " minutes");
            return result;
        }
        
        // Convert to hours
        long hours = minutes / 60;
        if (hours < 24) {
            String result = hours + "h ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + hours + " hours");
            return result;
        }
        
        // Convert to days
        long days = hours / 24;
        if (days < 7) {
            String result = days + "d ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + days + " days");
            return result;
        }
        
        // Convert to weeks
        long weeks = days / 7;
        if (weeks < 4) {
            String result = weeks + "w ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + weeks + " weeks");
            return result;
        }
        
        // Convert to months
        long months = days / 30;
        if (months < 12) {
            String result = months + "mo ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + months + " months");
            return result;
        }
        
        // Convert to years
        long years = days / 365;
        String result = years + "y ago";
        Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + years + " years");
        return result;
    }

    private void verifyTimeTextVisibility(TextView timeText) {
        // Check if the view is visible and has valid dimensions
        boolean isVisible = timeText.getVisibility() == View.VISIBLE;
        int width = timeText.getWidth();
        int height = timeText.getHeight();
        String text = timeText.getText().toString();
        
        Log.d(TAG, "TimeText verification: " +
              "isVisible=" + isVisible + 
              ", width=" + width + 
              ", height=" + height + 
              ", text='" + text + "'" +
              ", parent=" + (timeText.getParent() != null ? timeText.getParent().getClass().getSimpleName() : "null"));
        
        // Check parent view
        if (timeText.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) timeText.getParent();
            Log.d(TAG, "Parent view: " +
                  "visibility=" + (parent.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT_VISIBLE") +
                  ", width=" + parent.getWidth() +
                  ", height=" + parent.getHeight() +
                  ", childCount=" + parent.getChildCount());
        }
        
        if (!isVisible) {
            Log.e(TAG, "TimeText is not visible!");
        } else if (width <= 0 || height <= 0) {
            Log.e(TAG, "TimeText has invalid dimensions: " + width + "x" + height);
        } else if (text.isEmpty()) {
            Log.e(TAG, "TimeText has empty text!");
        } else {
            Log.d(TAG, "TimeText appears to be displayed correctly");
        }
    }
} 