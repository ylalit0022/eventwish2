package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Template> templates;
    private final TemplateInteractionManager interactionManager;
    private OnItemClickListener onItemClickListener;
    private OnTemplateInteractionListener onTemplateInteractionListener;
    private static final long CLICK_DEBOUNCE_TIME = 800; // ms - longer debounce time for network operations
    private final Map<String, Long> lastClickTimes = new HashMap<>();
    private static final String TAG = "TemplateAdapter";

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
        
        if (template.isRecommended()) {
            holder.recommendedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.recommendedBadge.setVisibility(View.GONE);
        }
        
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
            boolean newLikeState = !template.isLiked();
            template.setLiked(newLikeState);
            
            Log.d(TAG, "Template " + template.getId() + " like toggled to: " + newLikeState);
            
            // Animate the like button
            animateLikeButton(holder.likeIcon, newLikeState);
            
            // Update UI
            updateLikeState(holder, newLikeState);
            
            // Show toast message
            Toast.makeText(context, newLikeState ? "Liked" : "Unliked", Toast.LENGTH_SHORT).show();
            
            // Update like count
            if (newLikeState) {
                long newCount = template.getLikeCount() + 1;
                template.setLikeCount(newCount);
                holder.likeCountText.setText(String.valueOf(newCount));
                holder.likeCountText.setVisibility(View.VISIBLE);
            } else {
                long newCount = Math.max(0, template.getLikeCount() - 1);
                template.setLikeCount(newCount);
                if (newCount > 0) {
                    holder.likeCountText.setText(String.valueOf(newCount));
                    holder.likeCountText.setVisibility(View.VISIBLE);
                } else {
                    holder.likeCountText.setVisibility(View.GONE);
                }
            }
            
            // Notify listener
            if (onTemplateInteractionListener != null) {
                onTemplateInteractionListener.onTemplateLiked(template, newLikeState);
            }
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

    public void setTemplates(ArrayList<Template> templates) {
        Log.d(TAG, "setTemplates: Received " + (templates != null ? templates.size() : 0) + " templates");
        
        if (templates == null) {
            Log.e(TAG, "setTemplates: Received null templates list");
            return;
        }
        
        if (templates.isEmpty()) {
            Log.d(TAG, "setTemplates: Received empty templates list");
        } else {
            Log.d(TAG, "First template ID: " + templates.get(0).getId() + 
                  ", Title: " + templates.get(0).getTitle() + 
                  ", Preview URL: " + templates.get(0).getPreviewUrl());
        }
        
        this.templates.clear();
        this.templates.addAll(templates);
        notifyDataSetChanged();
        
        Log.d(TAG, "setTemplates: Adapter updated with " + this.templates.size() + " templates");
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