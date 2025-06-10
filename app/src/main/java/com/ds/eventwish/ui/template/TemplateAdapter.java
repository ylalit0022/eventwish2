package com.ds.eventwish.ui.template;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * Adapter for displaying templates in a RecyclerView
 */
public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    
    private static final String TAG = "TemplateAdapter";
    
    private final OnTemplateInteractionListener listener;
    
    /**
     * Interface for handling template interactions
     */
    public interface OnTemplateInteractionListener {
        void onTemplateClick(Template template);
        void onTemplateLike(Template template);
        void onTemplateFavorite(Template template);
    }
    
    /**
     * Constructor
     *
     * @param listener Listener for template interactions
     */
    public TemplateAdapter(OnTemplateInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        // Enable stable IDs to prevent blinking during updates
        setHasStableIds(true);
    }
    
    /**
     * Provide stable item IDs to prevent unnecessary rebinding
     */
    @Override
    public long getItemId(int position) {
        Template template = getItem(position);
        // Use ID as stable identifier or position as fallback
        return template.getId() != null ? template.getId().hashCode() : position;
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
        Template template = getItem(position);
        holder.bind(template, listener);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            // No payload, do a full rebind
            onBindViewHolder(holder, position);
        } else {
            // Has payload, only update like/favorite state
            Template template = getItem(position);
            holder.updateInteractionState(template);
        }
    }
    
    /**
     * ViewHolder for template items
     */
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView templateImage;
        private final TextView templateName;
        private final ImageView likeIcon;
        private final ImageView favoriteIcon;
        private final TextView likeCountText;
        private final TextView favoriteCountText;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            templateImage = itemView.findViewById(R.id.template_image);
            templateName = itemView.findViewById(R.id.titleText);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            favoriteCountText = itemView.findViewById(R.id.favoriteCountText);
        }
        
        public void bind(Template template, OnTemplateInteractionListener listener) {
            templateName.setText(template.getName());
            
            // Load image with Glide
            Glide.with(templateImage.getContext())
                .load(template.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .centerCrop()
                .into(templateImage);
            
            // Set up like button and count
            boolean isLiked = template.isLiked();
            long likeCount = template.getLikeCount();
            
            Log.d(TAG, "Setting up like button for template: " + template.getId() + 
                  ", current state: " + isLiked + ", count: " + likeCount);
            
            likeIcon.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            likeCountText.setText(String.valueOf(Math.max(0, likeCount)));
            
            likeIcon.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Like button clicked for template: " + template.getId());
                    // Disable button temporarily to prevent double-clicks
                    likeIcon.setEnabled(false);
                    
                    // Update UI immediately for better feedback
                    boolean newLikeState = !template.isLiked();
                    template.setLiked(newLikeState);
                    likeIcon.setImageResource(newLikeState ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                    
                    // Update count immediately
                    long currentCount = Math.max(0, template.getLikeCount());
                    long newCount = newLikeState ? Math.max(1, currentCount + 1) : Math.max(0, currentCount - 1);
                    template.setLikeCount(newCount);
                    likeCountText.setText(String.valueOf(newCount));
                    
                    // Add animation for visual feedback
                    likeIcon.startAnimation(AnimationUtils.loadAnimation(likeIcon.getContext(), 
                        R.anim.like_button_animation));
                    
                    // Call listener
                    listener.onTemplateLike(template);
                    
                    // Re-enable button after a short delay
                    v.postDelayed(() -> {
                        likeIcon.setEnabled(true);
                    }, 500);
                }
            });
            
            // Set up favorite button and count
            boolean isFavorited = template.isFavorited();
            long favoriteCount = template.getFavoriteCount();
            
            Log.d(TAG, "Setting up favorite button for template: " + template.getId() + 
                  ", current state: " + isFavorited + ", count: " + favoriteCount);
            
            favoriteIcon.setImageResource(isFavorited ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            favoriteCountText.setText(String.valueOf(Math.max(0, favoriteCount)));
            
            favoriteIcon.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Favorite button clicked for template: " + template.getId());
                    // Disable button temporarily to prevent double-clicks
                    favoriteIcon.setEnabled(false);
                    
                    // Update UI immediately for better feedback
                    boolean newFavoriteState = !template.isFavorited();
                    template.setFavorited(newFavoriteState);
                    favoriteIcon.setImageResource(newFavoriteState ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                    
                    // Update count immediately
                    long currentCount = Math.max(0, template.getFavoriteCount());
                    long newCount = newFavoriteState ? Math.max(1, currentCount + 1) : Math.max(0, currentCount - 1);
                    template.setFavoriteCount(newCount);
                    favoriteCountText.setText(String.valueOf(newCount));
                    
                    // Add animation for visual feedback
                    favoriteIcon.startAnimation(AnimationUtils.loadAnimation(favoriteIcon.getContext(), 
                        R.anim.favorite_button_animation));
                    
                    // Call listener
                    listener.onTemplateFavorite(template);
                    
                    // Re-enable button after a short delay
                    v.postDelayed(() -> {
                        favoriteIcon.setEnabled(true);
                    }, 500);
                }
            });
            
            // Set click listener for the whole item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Template clicked: " + template.getId());
                    listener.onTemplateClick(template);
                }
            });
        }
        
        public void updateInteractionState(Template template) {
            // Update like button and count
            boolean isLiked = template.isLiked();
            long likeCount = Math.max(0, template.getLikeCount());
            
            boolean wasLiked = likeIcon.getDrawable().getConstantState().equals(
                likeIcon.getContext().getDrawable(R.drawable.ic_heart_filled).getConstantState());
                
            if (isLiked != wasLiked) {
                // Only animate if state actually changed
                likeIcon.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                likeIcon.startAnimation(AnimationUtils.loadAnimation(likeIcon.getContext(), 
                    R.anim.like_button_animation));
            }
            
            // Always update the count as it might have changed even if the state didn't
            likeCountText.setText(String.valueOf(likeCount));
            
            // Update favorite button and count
            boolean isFavorited = template.isFavorited();
            long favoriteCount = Math.max(0, template.getFavoriteCount());
            
            boolean wasFavorited = favoriteIcon.getDrawable().getConstantState().equals(
                favoriteIcon.getContext().getDrawable(R.drawable.ic_bookmark_filled).getConstantState());
                
            if (isFavorited != wasFavorited) {
                // Only animate if state actually changed
                favoriteIcon.setImageResource(isFavorited ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                favoriteIcon.startAnimation(AnimationUtils.loadAnimation(favoriteIcon.getContext(), 
                    R.anim.favorite_button_animation));
            }
            
            // Always update the count as it might have changed even if the state didn't
            favoriteCountText.setText(String.valueOf(favoriteCount));
            
            Log.d(TAG, "Partially updated template " + template.getId() + 
                " interaction state - Liked: " + isLiked + " (" + likeCount + "), " +
                "Favorited: " + isFavorited + " (" + favoriteCount + ")");
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    private static final DiffUtil.ItemCallback<Template> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<Template>() {
            @Override
            public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.getId().equals(newItem.getId());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                // Compare all properties except like/favorite status and like count
                // This ensures that like/favorite changes don't trigger position changes
                return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getImageUrl().equals(newItem.getImageUrl()) &&
                    oldItem.getCategoryId().equals(newItem.getCategoryId()) &&
                    // Include like/favorite state to detect changes for payload
                    oldItem.isLiked() == newItem.isLiked() &&
                    oldItem.isFavorited() == newItem.isFavorited() &&
                    oldItem.getLikeCount() == newItem.getLikeCount();
            }
            
            @Override
            public Object getChangePayload(@NonNull Template oldItem, @NonNull Template newItem) {
                // Return a payload when only like/favorite state changes
                // This allows partial rebinding for better performance
                if (oldItem.isLiked() != newItem.isLiked() ||
                    oldItem.isFavorited() != newItem.isFavorited() ||
                    oldItem.getLikeCount() != newItem.getLikeCount()) {
                    return true; // Just a marker that only interaction states changed
                }
                return null; // Default behavior for full rebinding
            }
        };
} 