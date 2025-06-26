package com.ds.eventwish.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.ItemTemplateBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ds.eventwish.utils.VideoPlayerManager;
import com.ds.eventwish.utils.VideoVisibilityHelper;
import androidx.media3.ui.PlayerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> 
        implements VideoVisibilityHelper.VideoTemplateChecker, VideoPlayerManager.VideoPlaybackListener {
    private static final String TAG = "TemplateAdapter";
    private final OnTemplateClickListener listener;
    private Set<String> newTemplates = new HashSet<>();
    private VideoPlayerManager videoPlayerManager;
    private RecyclerView recyclerView;

    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }

    public TemplateAdapter(OnTemplateClickListener listener) {
        super(new DiffUtil.ItemCallback<Template>() {
            @Override
            public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }
    
    /**
     * Initialize video player manager and attach to RecyclerView
     */
    public void initializeVideoPlayer(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        if (recyclerView.getContext() != null) {
            this.videoPlayerManager = VideoPlayerManager.getInstance(recyclerView.getContext());
            this.videoPlayerManager.setPlaybackListener(this);
            Log.d(TAG, "Video player manager initialized");
        }
    }
    
    /**
     * Release video player resources
     */
    public void releaseVideoPlayer() {
        if (videoPlayerManager != null) {
            videoPlayerManager.release();
            videoPlayerManager = null;
        }
    }
    
    /**
     * Handle video visibility changes (called from scroll listener)
     */
    public void handleVideoVisibilityChange() {
        if (recyclerView == null || videoPlayerManager == null) {
            return;
        }
        
        // Find the most visible video and play it
        int mostVisibleVideoPosition = VideoVisibilityHelper.findMostVisibleVideoPosition(recyclerView);
        
        if (mostVisibleVideoPosition != -1) {
            Template template = getItem(mostVisibleVideoPosition);
            if (template != null && isVideoTemplate(template)) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(mostVisibleVideoPosition);
                if (viewHolder instanceof TemplateViewHolder) {
                    ((TemplateViewHolder) viewHolder).playVideoIfVisible();
                }
            }
        } else {
            // No video visible, pause current video
            videoPlayerManager.pauseCurrentVideo();
        }
    }
    
    /**
     * Pause video when app goes to background
     */
    public void pauseVideo() {
        if (videoPlayerManager != null) {
            videoPlayerManager.pauseCurrentVideo();
        }
    }
    
    /**
     * Resume video when app comes to foreground
     */
    public void resumeVideo() {
        if (videoPlayerManager != null && recyclerView != null) {
            handleVideoVisibilityChange();
        }
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
        try {
            Template template = getItem(position);
            if (template != null) {
                holder.bind(template);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    /**
     * Update the list of templates in the adapter
     * @param templates New list of templates
     */
    public void updateTemplates(List<Template> templates) {
        submitList(templates);
    }

    /**
     * Set the list of new templates to show the NEW badge
     * @param newTemplateIds Set of template IDs that are new
     */
    public void setNewTemplates(Set<String> newTemplateIds) {
        this.newTemplates = newTemplateIds != null ? new HashSet<>(newTemplateIds) : new HashSet<>();
        notifyDataSetChanged();
    }

    /**
     * Mark a template as viewed (no longer new)
     * @param templateId The ID of the template to mark as viewed
     */
    public void markAsViewed(String templateId) {
        if (newTemplates.contains(templateId)) {
            newTemplates.remove(templateId);
            // Find the position of this template and update it
            for (int i = 0; i < getItemCount(); i++) {
                Template template = getItem(i);
                if (template != null && template.getId().equals(templateId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ItemTemplateBinding binding;
        private Template currentTemplate;
        private boolean isVideoPlaying = false;

        TemplateViewHolder(ItemTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener in constructor to avoid creating new instances
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Template template = getItem(position);
                    if (template != null && listener != null) {
                        Log.d(TAG, "Template clicked: " + template.getId());
                        if (newTemplates.contains(template.getId())) {
                            markAsViewed(template.getId());
                        }
                        listener.onTemplateClick(template);
                    }
                }
            });
            
            // Set up video control listeners
            setupVideoControls();
        }
        
        private void setupVideoControls() {
            // Play button click listener
            if (binding.videoPlayButton != null) {
                binding.videoPlayButton.setOnClickListener(v -> {
                    if (currentTemplate != null && isVideoTemplate(currentTemplate)) {
                        playVideoIfVisible();
                    }
                });
            }
            
            // Mute button click listener
            if (binding.videoMuteButton != null) {
                binding.videoMuteButton.setOnClickListener(v -> {
                    if (videoPlayerManager != null) {
                        videoPlayerManager.toggleMute();
                        updateMuteButtonIcon();
                    }
                });
            }
        }
        
        /**
         * Play video if this view is visible enough
         */
        public void playVideoIfVisible() {
            if (currentTemplate == null || !isVideoTemplate(currentTemplate) || 
                videoPlayerManager == null || recyclerView == null) {
                return;
            }
            
            // Check if this view is visible enough for auto-play
            if (VideoVisibilityHelper.isViewVisibleForAutoPlay(itemView, recyclerView)) {
                String videoUrl = currentTemplate.getVideoUrl();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    
                    // Show loading indicator
                    showVideoLoading(true);
                    
                    // Hide play button when starting video
                    if (binding.videoPlayButton != null) {
                        binding.videoPlayButton.setVisibility(View.GONE);
                    }
                    
                    // Show mute button
                    if (binding.videoMuteButton != null) {
                        binding.videoMuteButton.setVisibility(View.VISIBLE);
                        updateMuteButtonIcon();
                    }
                    
                    // Start video playback
                    if (binding.videoPlayerView != null) {
                        videoPlayerManager.playVideo(
                            binding.videoPlayerView, 
                            videoUrl, 
                            currentTemplate.getId(), 
                            true
                        );
                        
                        // Hide thumbnail and show player view
                        if (binding.videoThumbnail != null) {
                            binding.videoThumbnail.setVisibility(View.GONE);
                        }
                        binding.videoPlayerView.setVisibility(View.VISIBLE);
                        
                        isVideoPlaying = true;
                        Log.d(TAG, "Started video playback for template: " + currentTemplate.getId());
                    }
                }
            }
        }
        
        /**
         * Stop video playback for this view
         */
        public void stopVideo() {
            if (videoPlayerManager != null && currentTemplate != null && isVideoPlaying) {
                videoPlayerManager.stopCurrentVideo();
                resetVideoUI();
                isVideoPlaying = false;
                Log.d(TAG, "Stopped video playback for template: " + currentTemplate.getId());
            }
        }
        
        /**
         * Reset video UI to initial state
         */
        private void resetVideoUI() {
            // Hide player view and show thumbnail
            if (binding.videoPlayerView != null) {
                binding.videoPlayerView.setVisibility(View.GONE);
            }
            if (binding.videoThumbnail != null) {
                binding.videoThumbnail.setVisibility(View.VISIBLE);
            }
            
            // Show play button
            if (binding.videoPlayButton != null) {
                binding.videoPlayButton.setVisibility(View.VISIBLE);
            }
            
            // Hide mute button
            if (binding.videoMuteButton != null) {
                binding.videoMuteButton.setVisibility(View.GONE);
            }
            
            // Hide loading indicator
            showVideoLoading(false);
        }
        
        /**
         * Show/hide video loading indicator
         */
        private void showVideoLoading(boolean show) {
            if (binding.videoLoadingIndicator != null) {
                binding.videoLoadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
        
        /**
         * Update mute button icon based on current volume
         */
        private void updateMuteButtonIcon() {
            if (binding.videoMuteButton != null && videoPlayerManager != null) {
                // This is a simplified implementation - you might want to add volume checking
                // For now, we'll just toggle between mute and unmute icons
                // You can enhance this by checking the actual volume from VideoPlayerManager
            }
        }

        void bind(Template template) {
            try {
                // Store current template reference
                this.currentTemplate = template;
                
                // Reset video state
                isVideoPlaying = false;
                resetVideoUI();
                
                // Set title
                binding.titleText.setText(template.getTitle());
                
                // Set category if available
                String category = template.getCategory();
                if (category != null && !category.isEmpty()) {
                    binding.categoryText.setVisibility(View.VISIBLE);
                    binding.categoryText.setText(category);
                } else {
                    binding.categoryText.setVisibility(View.GONE);
                }
                
                // Show NEW badge if this template is in the newTemplates set
                binding.newBadge.setVisibility(newTemplates.contains(template.getId()) ? 
                    View.VISIBLE : View.GONE);
                
                // Handle different template types
                String templateType = template.getTemplateType();
                if (templateType == null || templateType.isEmpty()) {
                    templateType = "image"; // Default to image if not specified
                }
                
                Log.d(TAG, "Binding template " + template.getId() + " with type: " + templateType);
                
                // Hide all template containers first
                binding.templateImage.setVisibility(View.GONE);
                binding.htmlTemplateContainer.setVisibility(View.GONE);
                binding.videoTemplateContainer.setVisibility(View.GONE);
                binding.templateTypeBadge.setVisibility(View.VISIBLE);
                
                switch (templateType.toLowerCase()) {
                    case "html":
                        setupHtmlTemplate(template);
                        break;
                    case "video":
                        setupVideoTemplate(template);
                        break;
                    case "image":
                    default:
                        setupImageTemplate(template);
                        break;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding template: " + e.getMessage());
                // Fallback to image template on error
                setupImageTemplate(template);
            }
        }
        
        private void setupImageTemplate(Template template) {
            Log.d(TAG, "Setting up image template: " + template.getId());
            
            // Show image container
            binding.templateImage.setVisibility(View.VISIBLE);
            binding.templateTypeBadge.setText("IMAGE");
            
            // Load image
            String imageUrl = template.getThumbnailUrl() != null ? template.getThumbnailUrl() : template.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.templateImage);
            } else {
                binding.templateImage.setImageResource(android.R.color.transparent);
            }
        }
        
        private void setupHtmlTemplate(Template template) {
            Log.d(TAG, "Setting up HTML template: " + template.getId());
            
            // Show HTML container
            binding.htmlTemplateContainer.setVisibility(View.VISIBLE);
            binding.templateTypeBadge.setText("HTML");
            
            // Set HTML template title
            binding.htmlTemplateTitle.setText(template.getTitle());
            
            // Note: htmlTemplateDescription view doesn't exist in the layout
            // Description functionality can be added later if needed
        }
        
        private void setupVideoTemplate(Template template) {
            Log.d(TAG, "Setting up video template: " + template.getId());
            
            // Show video container
            binding.videoTemplateContainer.setVisibility(View.VISIBLE);
            binding.templateTypeBadge.setText("VIDEO");
            
            // Reset video UI state
            resetVideoUI();
            
            // Load video thumbnail if available
            String thumbnailUrl = template.getThumbnailUrl();
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                    .load(thumbnailUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.videoThumbnail);
            } else {
                // Use a default video placeholder
                binding.videoThumbnail.setImageResource(android.R.color.transparent);
            }
            
            // Set video duration if available (you might need to add this field to Template model)
            // For now, we'll show a default duration
            binding.videoDuration.setText("0:30");
            
            // Check if this video should auto-play immediately
            if (recyclerView != null && VideoVisibilityHelper.isViewVisibleForAutoPlay(itemView, recyclerView)) {
                // Delay auto-play slightly to ensure UI is ready
                itemView.post(() -> playVideoIfVisible());
            }
        }
    }
    
    // VideoTemplateChecker interface implementation
    @Override
    public boolean isVideoTemplate(int position) {
        try {
            if (position >= 0 && position < getItemCount()) {
                Template template = getItem(position);
                return isVideoTemplate(template);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking video template at position " + position, e);
        }
        return false;
    }
    
    /**
     * Check if a template is a video template
     */
    private boolean isVideoTemplate(Template template) {
        if (template == null) {
            return false;
        }
        
        String templateType = template.getTemplateType();
        return "video".equalsIgnoreCase(templateType) && 
               template.getVideoUrl() != null && 
               !template.getVideoUrl().trim().isEmpty();
    }
    
    // VideoPlaybackListener interface implementation
    @Override
    public void onVideoStarted(String templateId) {
        Log.d(TAG, "Video started for template: " + templateId);
        // Update UI if needed - hide loading indicator
        updateVideoLoadingState(templateId, false);
    }
    
    @Override
    public void onVideoPaused(String templateId) {
        Log.d(TAG, "Video paused for template: " + templateId);
    }
    
    @Override
    public void onVideoError(String templateId, String error) {
        Log.e(TAG, "Video error for template " + templateId + ": " + error);
        // Reset video UI on error
        updateVideoErrorState(templateId);
    }
    
    @Override
    public void onVideoBuffering(String templateId, boolean isBuffering) {
        Log.d(TAG, "Video buffering for template " + templateId + ": " + isBuffering);
        updateVideoLoadingState(templateId, isBuffering);
    }
    
    /**
     * Update loading state for a specific template
     */
    private void updateVideoLoadingState(String templateId, boolean isLoading) {
        if (recyclerView == null) return;
        
        // Find the view holder for this template
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
            
            if (viewHolder instanceof TemplateViewHolder) {
                TemplateViewHolder templateHolder = (TemplateViewHolder) viewHolder;
                if (templateHolder.currentTemplate != null && 
                    templateHolder.currentTemplate.getId().equals(templateId)) {
                    templateHolder.showVideoLoading(isLoading);
                    break;
                }
            }
        }
    }
    
    /**
     * Update error state for a specific template
     */
    private void updateVideoErrorState(String templateId) {
        if (recyclerView == null) return;
        
        // Find the view holder for this template and reset its video UI
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
            
            if (viewHolder instanceof TemplateViewHolder) {
                TemplateViewHolder templateHolder = (TemplateViewHolder) viewHolder;
                if (templateHolder.currentTemplate != null && 
                    templateHolder.currentTemplate.getId().equals(templateId)) {
                    templateHolder.resetVideoUI();
                    templateHolder.isVideoPlaying = false;
                    break;
                }
            }
        }
    }
}
