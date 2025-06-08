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
    
    /**
     * ViewHolder for template items
     */
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView templateImage;
        private final TextView templateName;
        private final ImageView likeIcon;
        private final ImageView favoriteIcon;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            templateImage = itemView.findViewById(R.id.template_image);
            templateName = itemView.findViewById(R.id.titleText);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
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
            
            // Set up like button
            boolean isLiked = template.isLiked();
            Log.d(TAG, "Setting up like button for template: " + template.getId() + ", current state: " + isLiked);
            likeIcon.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            likeIcon.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Like button clicked for template: " + template.getId());
                    // Disable button temporarily
                    likeIcon.setEnabled(false);
                    // Call listener without updating UI
                    listener.onTemplateLike(template);
                }
            });
            
            // Set up favorite button
            boolean isFavorited = template.isFavorited();
            Log.d(TAG, "Setting up favorite button for template: " + template.getId() + ", current state: " + isFavorited);
            favoriteIcon.setImageResource(isFavorited ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            favoriteIcon.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Favorite button clicked for template: " + template.getId());
                    // Disable button temporarily
                    favoriteIcon.setEnabled(false);
                    // Call listener without updating UI
                    listener.onTemplateFavorite(template);
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
                return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getImageUrl().equals(newItem.getImageUrl()) &&
                    oldItem.getCategoryId().equals(newItem.getCategoryId()) &&
                    oldItem.isLiked() == newItem.isLiked() &&
                    oldItem.isFavorited() == newItem.isFavorited() &&
                    oldItem.getLikeCount() == newItem.getLikeCount();
            }
        };
} 