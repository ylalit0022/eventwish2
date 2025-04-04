package com.ds.eventwish.resourse;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentResourceBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

// Stub Palette class
class Palette {
    private final Bitmap bitmap;

    private Palette(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public static Builder from(Bitmap bitmap) {
        return new Builder(bitmap);
    }

    public Swatch getDominantSwatch() {
        return new Swatch(Color.WHITE, 1);
    }
    
    public int getDominantColor(int defaultColor) {
        Swatch swatch = getDominantSwatch();
        return swatch != null ? swatch.getRgb() : defaultColor;
    }

    public static class Builder {
        private final Bitmap bitmap;

        public Builder(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public Builder generate(PaletteAsyncListener listener) {
            listener.onGenerated(new Palette(bitmap));
            return this;
        }

        public Palette generate() {
            return new Palette(bitmap);
        }
    }

    public static class Swatch {
        private final int color;
        private final int population;

        public Swatch(int color, int population) {
            this.color = color;
            this.population = population;
        }

        public int getRgb() {
            return color;
        }

        public int getPopulation() {
            return population;
        }
    }

    public interface PaletteAsyncListener {
        void onGenerated(Palette palette);
    }
}

public class ResourceFragment extends Fragment {
    private static final String TAG = "ResourceFragment";
    private FragmentResourceBinding binding;
    private ResourceViewModel viewModel;
    private String shortCode;
    private BottomNavigationView bottomNav;
    private boolean isFullScreenMode = true;
    private boolean isBottomNavVisible = false;
    private Handler autoHideHandler = new Handler();
    private Runnable autoHideRunnable;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ResourceFragment created");
        viewModel = new ViewModelProvider(this).get(ResourceViewModel.class);
        
        // Initialize auto-hide runnable
        autoHideRunnable = () -> {
            if (isBottomNavVisible && isFullScreenMode) {
                hideBottomNav();
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
        binding = FragmentResourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: View created");

        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.VISIBLE);
        }

        // Get shortCode from deep link
        if (getArguments() != null) {
            shortCode = getArguments().getString("shortCode");
            Log.d(TAG, "onViewCreated: Received shortCode from arguments: " + shortCode);
            
            if (shortCode != null && !shortCode.isEmpty()) {
                // Trim any whitespace that might be in the shortCode
                shortCode = shortCode.trim();
                
                // Check if the shortCode is URL-encoded and decode if necessary
                if (shortCode.contains("%")) {
                    try {
                        String decoded = java.net.URLDecoder.decode(shortCode, "UTF-8");
                        Log.d(TAG, "onViewCreated: Decoded URL-encoded shortCode from " + shortCode + " to " + decoded);
                        shortCode = decoded;
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding shortCode", e);
                    }
                }
                
                // Ensure shortCode doesn't start with "wish/" which could happen with deep links
                if (shortCode.startsWith("wish/")) {
                    shortCode = shortCode.substring(5); // Remove "wish/" prefix
                    Log.d(TAG, "onViewCreated: Removed 'wish/' prefix, shortCode=" + shortCode);
                }
                
                // Remove any forward slashes that might be at the beginning
                while (shortCode.startsWith("/")) {
                    shortCode = shortCode.substring(1);
                    Log.d(TAG, "onViewCreated: Removed leading slash, shortCode=" + shortCode);
                }
                
                Log.d(TAG, "onViewCreated: Final shortCode to load=" + shortCode);
                loadWish();
            } else {
                Log.e(TAG, "onViewCreated: shortCode is null or empty in arguments");
                showError("Invalid wish code");
            }
        } else {
            Log.e(TAG, "onViewCreated: No arguments received");
            showError("No wish code provided");
        }

        setupWebView();
        setupObservers();
        setupRetryButton();
        setupFullScreenToggle();
        setupTouchListener();
        
        // Enable full screen mode by default
        enableFullScreenMode();
    }
    
    private boolean isComingFromExternalSource() {
        // Check if we're coming from an external source (social media)
        if (getActivity() != null && getActivity().getIntent() != null) {
            String action = getActivity().getIntent().getAction();
            return Intent.ACTION_VIEW.equals(action);
        }
        return false;
    }
    
    private void setupFullScreenToggle() {
        // Setup full screen toggle button
        binding.fullscreenToggle.setOnClickListener(v -> {
            if (isFullScreenMode) {
                disableFullScreenMode();
            } else {
                enableFullScreenMode();
            }
        });
        
        // Set the correct icon for fullscreen mode (since we start in fullscreen)
        binding.fullscreenToggle.setImageResource(R.drawable.ic_fullscreen_exit);
        
        // Show the toggle button by default
        binding.fullscreenToggle.setVisibility(View.VISIBLE);
    }
    
    private void enableFullScreenMode() {
        isFullScreenMode = true;
        binding.fullscreenToggle.setImageResource(R.drawable.ic_fullscreen_exit);
        
        // Hide bottom navigation
        hideBottomNav();
        
        // Hide app bar if it exists
        if (getActivity() instanceof MainActivity) {
            View appBar = getActivity().findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }
        }
        
        // Expand webview to full screen
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.webView.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        binding.webView.setLayoutParams(params);
        
        // Schedule auto-hide for UI elements
        delayedHide();
    }
    
    private void disableFullScreenMode() {
        isFullScreenMode = false;
        binding.fullscreenToggle.setImageResource(R.drawable.ic_fullscreen);
        
        // Show bottom navigation
        showBottomNav();
        
        // Show app bar if it exists
        if (getActivity() instanceof MainActivity) {
            View appBar = getActivity().findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
        }
        
        // Restore webview margins
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.webView.getLayoutParams();
        int margin = getResources().getDimensionPixelSize(R.dimen.standard_margin);
        params.setMargins(margin, margin, margin, margin);
        binding.webView.setLayoutParams(params);
        
        // Remove any pending auto-hide callbacks
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }
    
    private void showBottomNav() {
        if (bottomNav != null && !isBottomNavVisible) {
            bottomNav.setVisibility(View.VISIBLE);
            isBottomNavVisible = true;
        }
    }
    
    private void hideBottomNav() {
        if (bottomNav != null && isBottomNavVisible) {
            bottomNav.setVisibility(View.GONE);
            isBottomNavVisible = false;
        }
    }
    
    private void delayedHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MILLIS);
    }
    
    private void setupTouchListener() {
        // Set up touch listener to show/hide UI elements on tap
        binding.webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFullScreenMode) {
                    // Toggle visibility of UI elements
                    if (isBottomNavVisible) {
                        hideBottomNav();
                    } else {
                        showBottomNav();
                        // Auto-hide after delay
                        delayedHide();
                    }
                }
            }
            // Return false to allow the WebView to handle the touch event as well
            return false;
        });
        
        // Set up touch listener for the entire fragment view
        binding.getRoot().setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFullScreenMode) {
                    // Toggle visibility of UI elements
                    if (isBottomNavVisible) {
                        hideBottomNav();
                    } else {
                        showBottomNav();
                        // Auto-hide after delay
                        delayedHide();
                    }
                }
            }
            // Return false to allow other views to handle the touch event
            return false;
        });
    }

    private void setupWebView() {
        Log.d(TAG, "setupWebView: Configuring WebView");
        if (binding != null && binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDomStorageEnabled(true);
            
            // Set up WebView client with background color extraction
            binding.webView.setWebViewClient(new SafeWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    
                    // Show fullscreen toggle once content is loaded
                    binding.fullscreenToggle.setVisibility(View.VISIBLE);
                    
                    // Capture the WebView as a bitmap to extract dominant color
                    binding.webView.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(binding.webView.getDrawingCache());
                    binding.webView.setDrawingCacheEnabled(false);
                    
                    // Extract dominant color using Palette API
                    extractDominantColor(bitmap);
                }
            });
            
            Log.d(TAG, "setupWebView: WebView configured successfully");
        } else {
            Log.e(TAG, "setupWebView: binding or webView is null");
        }
    }
    
    private void extractDominantColor(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            try {
                Palette.from(bitmap).generate(palette -> {
                    if (palette != null) {
                        // Get the dominant color
                        int dominantColor = palette.getDominantSwatch().getRgb();
                        
                        // Make it lighter for better readability
                        int lightColor = ColorUtils.blendARGB(dominantColor, Color.WHITE, 0.7f);
                        
                        // Apply the color to the background
                        binding.getRoot().setBackgroundColor(lightColor);
                        
                        // Also apply to content layout
                        binding.contentLayout.setBackgroundColor(lightColor);
                        
                        Log.d(TAG, "Applied extracted background color");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error extracting color", e);
            }
        }
    }

    private void setupRetryButton() {
        if (binding != null && binding.retryLayout != null) {
            binding.retryLayout.retryButton.setOnClickListener(v -> {
                if (shortCode != null) {
                    Log.d(TAG, "Retry button clicked, attempting to reload wish with shortCode=" + shortCode);
                    binding.retryLayout.getRoot().setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.VISIBLE);
                    loadWish();
                } else {
                    Log.e(TAG, "Cannot retry: shortCode is null");
                    showError("Cannot retry: invalid wish code");
                }
            });
        }
    }

    private void setupObservers() {
        Log.d(TAG, "setupObservers: Setting up observers");
        viewModel.getWish().observe(getViewLifecycleOwner(), wishResponse -> {
            Log.d(TAG, "Wish response received: " + (wishResponse != null ? "not null" : "null"));
            if (wishResponse != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.retryLayout.getRoot().setVisibility(View.GONE);
                binding.contentLayout.setVisibility(View.VISIBLE);
                
                if (wishResponse.getTemplate() != null) {
                    Log.d(TAG, "Setting up WebView content for wish with template id: " + 
                          (wishResponse.getTemplate().getId() != null ? wishResponse.getTemplate().getId() : "null"));
                    setupWebViewContent(wishResponse);
                    
                    // Show fullscreen toggle
                    binding.fullscreenToggle.setVisibility(View.VISIBLE);
                    
                    // Always enable full screen mode by default
                    enableFullScreenMode();
                } else {
                    Log.e(TAG, "Template is null in wish response");
                    showError("Invalid wish template");
                    binding.retryLayout.getRoot().setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Log.e(TAG, "Error observed: " + error);
                binding.progressBar.setVisibility(View.GONE);
                binding.retryLayout.getRoot().setVisibility(View.VISIBLE);
                binding.retryLayout.errorText.setText(error);
                binding.contentLayout.setVisibility(View.GONE);
                showError(error);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && isAdded()) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    binding.retryLayout.getRoot().setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupWebViewContent(WishResponse wishResponse) {
        if (binding != null && binding.webView != null && wishResponse != null) {
            binding.contentLayout.setVisibility(View.VISIBLE);
            String html = wishResponse.getCustomizedHtml();
            String css = wishResponse.getTemplate().getCssContent();
            String js = wishResponse.getTemplate().getJsContent();

            // Add background color matching to the CSS
            css += "\nbody { background-color: transparent !important; }\n";

            String fullHtml = String.format(
                "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0, minimum-scale=0.5\">" +
                        "<style>%s</style>" +
                        "</head>" +
                        "<body>%s<script>%s</script></body>" +
                "</html>",
                css, html, js
            );
            binding.webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
            
            // Make WebView background transparent to match fragment background
            binding.webView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void showError(String message) {
        if (isAdded() && binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove any pending callbacks
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Restore bottom navigation visibility
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
        
        // Restore app bar if it exists
        if (getActivity() instanceof MainActivity) {
            View appBar = getActivity().findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
        }
        
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
            binding.webView.destroy();
        }
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.getWish().removeObservers(this);
        viewModel.getError().removeObservers(this);
        viewModel.isLoading().removeObservers(this);
    }

    private static class SafeWebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return true; // Prevent navigation
        }
    }

    /**
     * Load the wish using the current shortCode
     */
    private void loadWish() {
        if (shortCode != null && !shortCode.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.retryLayout.getRoot().setVisibility(View.GONE);
            
            Log.d(TAG, "loadWish: Loading wish with shortCode=" + shortCode);
            viewModel.loadWish(shortCode);
        } else {
            Log.e(TAG, "loadWish: Cannot load wish with null or empty shortCode");
            showError("Invalid wish code");
        }
    }
}