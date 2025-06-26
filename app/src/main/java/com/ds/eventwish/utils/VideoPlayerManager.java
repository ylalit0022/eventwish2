package com.ds.eventwish.utils;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage video playback for templates in RecyclerView
 * Handles automatic play/pause based on visibility, memory management, and audio focus
 */
public class VideoPlayerManager {
    private static final String TAG = "VideoPlayerManager";
    private static VideoPlayerManager instance;
    
    private Context context;
    private ExoPlayer currentPlayer;
    private PlayerView currentPlayerView;
    private String currentVideoUrl;
    private String currentTemplateId;
    private boolean isPlayerReady = false;
    private boolean shouldAutoPlay = true;
    private Handler mainHandler;
    
    // Cache for video preparation states
    private Map<String, Boolean> videoPreparedCache = new HashMap<>();
    private Map<String, Long> videoPositionCache = new HashMap<>();
    
    // Listener interfaces
    public interface VideoPlaybackListener {
        void onVideoStarted(String templateId);
        void onVideoPaused(String templateId);
        void onVideoError(String templateId, String error);
        void onVideoBuffering(String templateId, boolean isBuffering);
    }
    
    private VideoPlaybackListener playbackListener;
    
    private VideoPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized VideoPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoPlayerManager(context);
        }
        return instance;
    }
    
    public static VideoPlayerManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("VideoPlayerManager must be initialized with context first");
        }
        return instance;
    }
    
    /**
     * Set the playback listener for video events
     */
    public void setPlaybackListener(VideoPlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    /**
     * Play video in the specified PlayerView
     * @param playerView The PlayerView to attach the player to
     * @param videoUrl The URL of the video to play
     * @param templateId The template ID for tracking
     * @param autoPlay Whether to start playing immediately
     */
    public void playVideo(@NonNull PlayerView playerView, @NonNull String videoUrl, 
                         @NonNull String templateId, boolean autoPlay) {
        
        if (videoUrl.equals(currentVideoUrl) && currentPlayer != null && currentPlayerView == playerView) {
            // Same video, just resume if needed
            if (autoPlay && !currentPlayer.isPlaying()) {
                currentPlayer.setPlayWhenReady(true);
                notifyVideoStarted(templateId);
            }
            return;
        }
        
        // Stop current video if different
        if (currentPlayer != null && !videoUrl.equals(currentVideoUrl)) {
            pauseCurrentVideo();
        }
        
        try {
            // Create new player if needed
            if (currentPlayer == null) {
                createPlayer();
            }
            
            // Cache current position if switching videos
            if (currentVideoUrl != null && currentPlayer != null) {
                videoPositionCache.put(currentVideoUrl, currentPlayer.getCurrentPosition());
            }
            
            // Set up new video
            currentPlayerView = playerView;
            currentVideoUrl = videoUrl;
            currentTemplateId = templateId;
            isPlayerReady = false;
            
            // Attach player to view
            playerView.setPlayer(currentPlayer);
            playerView.setUseController(false); // Hide controls for auto-play
            playerView.setVisibility(View.VISIBLE);
            
            // Prepare media
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
            currentPlayer.setMediaItem(mediaItem);
            currentPlayer.setPlayWhenReady(autoPlay);
            currentPlayer.prepare();
            
            // Restore position if available
            Long cachedPosition = videoPositionCache.get(videoUrl);
            if (cachedPosition != null) {
                currentPlayer.seekTo(cachedPosition);
            }
            
            Log.d(TAG, "Video prepared for template: " + templateId + ", autoPlay: " + autoPlay);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing video for template " + templateId, e);
            notifyVideoError(templateId, e.getMessage());
        }
    }
    
    /**
     * Pause the currently playing video
     */
    public void pauseCurrentVideo() {
        if (currentPlayer != null && currentPlayer.isPlaying()) {
            currentPlayer.setPlayWhenReady(false);
            if (currentTemplateId != null) {
                notifyVideoPaused(currentTemplateId);
            }
            Log.d(TAG, "Video paused for template: " + currentTemplateId);
        }
    }
    
    /**
     * Resume the current video if paused
     */
    public void resumeCurrentVideo() {
        if (currentPlayer != null && !currentPlayer.isPlaying() && isPlayerReady) {
            currentPlayer.setPlayWhenReady(true);
            if (currentTemplateId != null) {
                notifyVideoStarted(currentTemplateId);
            }
            Log.d(TAG, "Video resumed for template: " + currentTemplateId);
        }
    }
    
    /**
     * Stop and release the current video
     */
    public void stopCurrentVideo() {
        if (currentPlayer != null) {
            // Cache position before stopping
            if (currentVideoUrl != null) {
                videoPositionCache.put(currentVideoUrl, currentPlayer.getCurrentPosition());
            }
            
            currentPlayer.stop();
            if (currentPlayerView != null) {
                currentPlayerView.setPlayer(null);
                currentPlayerView = null;
            }
            currentVideoUrl = null;
            currentTemplateId = null;
            isPlayerReady = false;
            
            Log.d(TAG, "Video stopped and player detached");
        }
    }
    
    /**
     * Check if a video is currently playing
     */
    public boolean isPlaying() {
        return currentPlayer != null && currentPlayer.isPlaying();
    }
    
    /**
     * Check if the specified template is currently playing
     */
    public boolean isPlaying(String templateId) {
        return currentTemplateId != null && currentTemplateId.equals(templateId) && isPlaying();
    }
    
    /**
     * Get the currently playing template ID
     */
    @Nullable
    public String getCurrentTemplateId() {
        return currentTemplateId;
    }
    
    /**
     * Set whether videos should auto-play when visible
     */
    public void setAutoPlay(boolean autoPlay) {
        this.shouldAutoPlay = autoPlay;
    }
    
    /**
     * Check if auto-play is enabled
     */
    public boolean isAutoPlayEnabled() {
        return shouldAutoPlay;
    }
    
    /**
     * Release all resources
     */
    public void release() {
        if (currentPlayer != null) {
            currentPlayer.release();
            currentPlayer = null;
        }
        
        if (currentPlayerView != null) {
            currentPlayerView.setPlayer(null);
            currentPlayerView = null;
        }
        
        videoPreparedCache.clear();
        videoPositionCache.clear();
        currentVideoUrl = null;
        currentTemplateId = null;
        isPlayerReady = false;
        
        Log.d(TAG, "VideoPlayerManager resources released");
    }
    
    /**
     * Handle app going to background
     */
    public void onAppBackground() {
        pauseCurrentVideo();
        Log.d(TAG, "App went to background, video paused");
    }
    
    /**
     * Handle app coming to foreground
     */
    public void onAppForeground() {
        // Video will resume when it becomes visible again
        Log.d(TAG, "App came to foreground");
    }
    
    /**
     * Create and configure ExoPlayer
     */
    private void createPlayer() {
        if (currentPlayer != null) {
            return;
        }
        
        try {
            currentPlayer = new ExoPlayer.Builder(context)
                .build();
            
            // Set up player listeners
            currentPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_READY:
                            isPlayerReady = true;
                            if (currentTemplateId != null && currentPlayer.isPlaying()) {
                                notifyVideoStarted(currentTemplateId);
                            }
                            break;
                            
                        case Player.STATE_BUFFERING:
                            if (currentTemplateId != null) {
                                notifyVideoBuffering(currentTemplateId, true);
                            }
                            break;
                            
                        case Player.STATE_ENDED:
                            // Loop the video for continuous playback
                            if (currentPlayer != null) {
                                currentPlayer.seekTo(0);
                                currentPlayer.setPlayWhenReady(shouldAutoPlay);
                            }
                            break;
                            
                        case Player.STATE_IDLE:
                            isPlayerReady = false;
                            break;
                    }
                }
                
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                    if (currentTemplateId != null) {
                        notifyVideoError(currentTemplateId, error.getMessage());
                    }
                    isPlayerReady = false;
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (currentTemplateId != null) {
                        if (isPlaying) {
                            notifyVideoStarted(currentTemplateId);
                        } else {
                            notifyVideoPaused(currentTemplateId);
                        }
                    }
                }
            });
            
            // Configure for looping and muted playback (like Instagram Reels)
            currentPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            currentPlayer.setVolume(0f); // Start muted
            
            Log.d(TAG, "ExoPlayer created and configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating ExoPlayer", e);
            currentPlayer = null;
        }
    }
    
    /**
     * Toggle video mute state
     */
    public void toggleMute() {
        if (currentPlayer != null) {
            float currentVolume = currentPlayer.getVolume();
            currentPlayer.setVolume(currentVolume > 0 ? 0f : 1f);
            Log.d(TAG, "Video mute toggled, volume: " + currentPlayer.getVolume());
        }
    }
    
    /**
     * Set video volume
     */
    public void setVolume(float volume) {
        if (currentPlayer != null) {
            currentPlayer.setVolume(Math.max(0f, Math.min(1f, volume)));
        }
    }
    
    /**
     * Pause video (alias for pauseCurrentVideo for compatibility)
     */
    public void pauseVideo() {
        pauseCurrentVideo();
    }
    
    /**
     * Release player (alias for release for compatibility)
     */
    public void releasePlayer() {
        release();
    }
    
    // Notification methods
    private void notifyVideoStarted(String templateId) {
        if (playbackListener != null) {
            mainHandler.post(() -> playbackListener.onVideoStarted(templateId));
        }
    }
    
    private void notifyVideoPaused(String templateId) {
        if (playbackListener != null) {
            mainHandler.post(() -> playbackListener.onVideoPaused(templateId));
        }
    }
    
    private void notifyVideoError(String templateId, String error) {
        if (playbackListener != null) {
            mainHandler.post(() -> playbackListener.onVideoError(templateId, error));
        }
    }
    
    private void notifyVideoBuffering(String templateId, boolean isBuffering) {
        if (playbackListener != null) {
            mainHandler.post(() -> playbackListener.onVideoBuffering(templateId, isBuffering));
        }
    }
} 