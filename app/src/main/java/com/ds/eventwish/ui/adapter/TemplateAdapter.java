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
        holder.cardView.setOnClickListener(v -> {
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
        }
    }
} 