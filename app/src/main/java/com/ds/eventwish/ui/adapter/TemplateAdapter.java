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
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.TemplateInteractionManager;
import com.ds.eventwish.databinding.ItemTemplateBinding;
import com.ds.eventwish.ui.template.WebViewLazyLoader;
import com.ds.eventwish.utils.UserDataManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.core.content.ContextCompat;

public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> 
        implements WebViewLazyLoader.LoadCallback {

    private static final String TAG = "TemplateAdapter";
    private final Context context;
    private final TemplateClickListener clickListener;
    private final TemplateInteractionManager interactionManager;
    private final UserDataManager userDataManager;
    private final WebViewLazyLoader lazyLoader;
    
    // Click debouncing
    private final Map<String, Long> lastClickTimes = new HashMap<>();
    private static final long CLICK_DEBOUNCE_MS = 500;

    public interface TemplateClickListener {
        void onTemplateClick(Template template);
        void onLikeClick(Template template);
        void onFavoriteClick(Template template);
        void onShareClick(Template template);
    }

    public TemplateAdapter(Context context, TemplateClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.clickListener = clickListener;
        this.interactionManager = TemplateInteractionManager.getInstance();
        this.userDataManager = UserDataManager.getInstance(context);
        this.lazyLoader = new WebViewLazyLoader(context);
    }
    
    /**
     * Set up lazy loading for RecyclerView
     */
    public void setupLazyLoading(RecyclerView recyclerView) {
        lazyLoader.setupLazyLoading(recyclerView, this);
    }

    @NonNull
                    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTemplateBinding binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new TemplateViewHolder(binding);
                    }
                    
                    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = getItem(position);
        if (template != null) {
            holder.bind(template);
        }
                    }
                    
                    @Override
    public void onViewRecycled(@NonNull TemplateViewHolder holder) {
        super.onViewRecycled(holder);
        // Clear WebView content when view is recycled
        if (holder.template != null) {
            lazyLoader.clearTemplate(holder.binding.templateImage, holder.template.getId());
        }
    }

    // WebViewLazyLoader.LoadCallback implementation
                @Override
    public void onLoadStart(String templateId) {
        // Optional: Show loading indicator
                }
                
                @Override
    public void onLoadComplete(String templateId) {
        // Optional: Hide loading indicator
                }
                
                @Override
    public void onLoadError(String templateId, String error) {
        // Optional: Show error state
    }

    public class TemplateViewHolder extends RecyclerView.ViewHolder 
            implements WebViewLazyLoader.TemplateViewHolderInterface {
        final ItemTemplateBinding binding;
        Template template;

        public TemplateViewHolder(ItemTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        public Template getTemplate() {
            return template;
        }

        void bind(Template template) {
            this.template = template;
            
            // Store template in item view tag for lazy loader access
            itemView.setTag(template);
            
            // Set basic template information
            binding.titleText.setText(template.getTitle() != null ? template.getTitle() : "");
            
            // Set interaction counts
            updateInteractionCounts();
            
            // Set interaction states
            updateInteractionStates();
            
            // Show/hide badges
            updateBadges();
            
            // Set click listeners
            setupClickListeners();
            
            // WebView will be loaded by lazy loader based on visibility
        }
        
        private void updateInteractionCounts() {
            if (template == null) return;
            
            // Format and set like count
            String likeCount = formatCount(template.getLikes());
            binding.likeCountText.setText(likeCount);
            
            // Format and set share count with "shares" text
            String shareCount = formatCount(template.getShares());
            binding.shareCountText.setText(shareCount);
            
            // Format and set favorite count
            String favoriteCount = formatCount(template.getFavorites());
            binding.favoriteCountText.setText(favoriteCount);
        }
        
        private void updateInteractionStates() {
            if (template == null) return;
            
            // Update like state
            updateLikeState();
            
            // Update favorite state  
            updateFavoriteState();
        }
        
        private void updateLikeState() {
            // Use the Template object's state instead of TemplateInteractionManager
            boolean isLiked = template.isLiked();
            
            if (isLiked) {
                binding.likeIcon.setImageResource(R.drawable.ic_heart_filled);
                binding.likeIcon.setColorFilter(Color.parseColor("#E91E63"));
            } else {
                binding.likeIcon.setImageResource(R.drawable.ic_heart_outline);
                binding.likeIcon.clearColorFilter();
            }
            
            // Restore normal alpha (in case it was in loading state)
            binding.likeIcon.setAlpha(1.0f);
            
            Log.d(TAG, "Updated like state for template " + template.getId() + ": " + isLiked);
        }
        
        private void updateFavoriteState() {
            // Use the Template object's state instead of TemplateInteractionManager
            boolean isFavorited = template.isFavorited();
            
            if (isFavorited) {
                binding.favoriteIcon.setImageResource(R.drawable.ic_bookmark_filled);
                binding.favoriteIcon.setColorFilter(Color.parseColor("#FF9800"));
            } else {
                binding.favoriteIcon.setImageResource(R.drawable.ic_bookmark_outline);
                binding.favoriteIcon.clearColorFilter();
            }
            
            // Restore normal alpha (in case it was in loading state)
            binding.favoriteIcon.setAlpha(1.0f);
            
            Log.d(TAG, "Updated favorite state for template " + template.getId() + ": " + isFavorited);
        }
        
        private void updateBadges() {
            if (template == null) return;
            
            // Show NEW badge for templates created in last 7 days
            if (template.getCreatedAt() != null) {
                long daysSinceCreation = (new Date().getTime() - template.getCreatedAt().getTime()) / (1000 * 60 * 60 * 24);
                binding.newBadge.setVisibility(daysSinceCreation <= 7 ? 
                    android.view.View.VISIBLE : android.view.View.GONE);
            } else {
                binding.newBadge.setVisibility(android.view.View.GONE);
            }
            
            // Show RECOMMENDED badge for featured templates
            binding.recommendedBadge.setVisibility(template.isFeatured() ? 
                android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        private void setupClickListeners() {
            if (template == null || clickListener == null) return;
            
            // Configure WebView settings
            binding.templateImage.getSettings().setJavaScriptEnabled(true);
            binding.templateImage.getSettings().setAllowFileAccess(false);
            binding.templateImage.getSettings().setDomStorageEnabled(true);
            binding.templateImage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Make the WebView clickable and focusable
            binding.templateImage.setClickable(true);
            binding.templateImage.setFocusable(true);
            binding.templateImage.setEnabled(true);
            
            // Set WebView click listener
            binding.templateImage.setOnClickListener(v -> {
                if (isClickAllowed(template.getId())) {
                    Log.d(TAG, "Template WebView clicked, navigating to detail. Template ID: " + template.getId());
                    clickListener.onTemplateClick(template);
                }
            });
            
            // Make the entire card clickable for better UX
            binding.cardView.setClickable(true);
            binding.cardView.setFocusable(true);
            binding.cardView.setOnClickListener(v -> {
                if (isClickAllowed(template.getId())) {
                    Log.d(TAG, "Template card clicked, navigating to detail. Template ID: " + template.getId());
                    clickListener.onTemplateClick(template);
                }
            });
            
            // Like click
            binding.likeIcon.setOnClickListener(v -> {
                if (isClickAllowed("like_" + template.getId())) {
                    Log.d(TAG, "Like button clicked for template " + template.getId() + ", current state: " + template.isLiked());
                    
                    animateLikeButton();
                    
                    // Show loading state
                    binding.likeIcon.setAlpha(0.5f);
                    
                    // Notify the click listener for backend update - no optimistic update
                    clickListener.onLikeClick(template);
                }
            });
            
            // Favorite click
            binding.favoriteIcon.setOnClickListener(v -> {
                if (isClickAllowed("favorite_" + template.getId())) {
                    Log.d(TAG, "Favorite button clicked for template " + template.getId() + ", current state: " + template.isFavorited());
                    
                    animateFavoriteButton();
                    
                    // Show loading state
                    binding.favoriteIcon.setAlpha(0.5f);
                    
                    // Notify the click listener for backend update - no optimistic update
                    clickListener.onFavoriteClick(template);
                }
            });
            
            // Share click
            binding.shareIcon.setOnClickListener(v -> {
                if (isClickAllowed("share_" + template.getId())) {
                    clickListener.onShareClick(template);
                }
            });
        }
        
        private boolean isClickAllowed(String action) {
            long currentTime = System.currentTimeMillis();
            Long lastClickTime = lastClickTimes.get(action);
            
            if (lastClickTime == null || (currentTime - lastClickTime) > CLICK_DEBOUNCE_MS) {
                lastClickTimes.put(action, currentTime);
                return true;
            }
            
            return false;
        }
        
        private void animateLikeButton() {
            binding.likeIcon.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(100)
                .withEndAction(() -> 
                    binding.likeIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

        private void animateFavoriteButton() {
            binding.favoriteIcon.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .withEndAction(() -> 
                    binding.favoriteIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

        // Share button animation removed since share icon is no longer clickable
    }

    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
            } else {
            return String.valueOf(count);
           }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (lazyLoader != null) {
            lazyLoader.cleanup();
        }
        lastClickTimes.clear();
    }

    private static final DiffUtil.ItemCallback<Template> DIFF_CALLBACK = new DiffUtil.ItemCallback<Template>() {
        @Override
        public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
            return oldItem.equals(newItem);
        }
    };
} 