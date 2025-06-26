package com.ds.eventwish.ui.test;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ActivityVideoOverlayTestBinding;
import com.ds.eventwish.utils.TemplateOverlayHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VideoOverlayTestActivity extends AppCompatActivity {
    private ActivityVideoOverlayTestBinding binding;
    private ExoPlayer player;
    private WebView overlayWebView;
    private FirebaseAuth firebaseAuth;
    private boolean showPhoto = true;
    private boolean showName = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoOverlayTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        setupVideoPlayer();
        setupOverlayControls();
        updateOverlay();
    }

    private void setupVideoPlayer() {
        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        binding.videoPlayerView.setPlayer(player);

        // Load a sample video
        String videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void setupOverlayControls() {
        // Setup switches for photo and name visibility
        binding.switchPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showPhoto = isChecked;
            updateOverlay();
        });

        binding.switchName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showName = isChecked;
            updateOverlay();
        });
    }

    private void updateOverlay() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove existing overlay if any
        if (overlayWebView != null) {
            binding.videoContainer.removeView(overlayWebView);
        }

        // Create overlay WebView
        overlayWebView = TemplateOverlayHelper.createOverlayWebView(
            this,
            "<div class='user-overlay'><img src='{{userPhoto}}' class='photo'/><span class='name'>{{userName}}</span></div>",  // HTML template
            ".user-overlay { position: absolute; bottom: 20px; left: 20px; background: rgba(0,0,0,0.6); padding: 8px; border-radius: 24px; } .photo { width: 40px; height: 40px; border-radius: 50%; } .name { color: white; margin-left: 8px; }",  // CSS template
            null,  // No JS needed
            currentUser != null ? currentUser.getDisplayName() : "Anonymous User",
            currentUser != null ? currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null : null,
            currentUser != null ? currentUser.getUid() : null
        );

        // Add overlay to container
        binding.videoContainer.addView(overlayWebView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
} 