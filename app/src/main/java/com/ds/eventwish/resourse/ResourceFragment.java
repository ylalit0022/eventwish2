package com.ds.eventwish.resourse;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.view.Window;
import android.app.Activity;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentResourceBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.utils.EdgeToEdgeManager;

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

public class ResourceFragment extends BaseFragment {
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
    private WishResponse currentWish;
    
    // Add AdMobManager reference
    private AdMobManager adMobManager;
    
    // Material 3 immersive experience
    private int originalStatusBarColor;
    private int originalNavigationBarColor;
    private boolean originalLightStatusBar;
    private boolean originalLightNavigationBar;

    // Add EdgeToEdgeManager reference
    private EdgeToEdgeManager edgeToEdgeManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Disable authentication requirement for ResourceFragment
        setRequiresAuthentication(false);
        Log.d(TAG, "Authentication requirement disabled for ResourceFragment");
        
        Log.d(TAG, "onCreate: ResourceFragment created");
        viewModel = new ViewModelProvider(this).get(ResourceViewModel.class);
        
        // Initialize auto-hide runnable
        autoHideRunnable = () -> {
            if (isBottomNavVisible && isFullScreenMode) {
                hideBottomNav();
            }
        };
        
        // Initialize AdMobManager
        try {
            adMobManager = AdMobManager.getInstance();
            Log.d(TAG, "AdMobManager initialized successfully");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error initializing AdMobManager: " + e.getMessage());
        }

        // Initialize EdgeToEdgeManager
        edgeToEdgeManager = EdgeToEdgeManager.getInstance();
        Log.d(TAG, "EdgeToEdgeManager initialized successfully");
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

        // Track screen view for analytics - Add try-catch to prevent crashes
        try {
            AnalyticsUtils.trackScreenView("ResourceFragment", ResourceFragment.class.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error tracking screen view: " + e.getMessage());
        }

        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.GONE); // Hide bottom nav for immersive experience
        }
        
        // Enable Material 3 immersive mode
        enableMaterial3ImmersiveMode();

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
        setupReuseButton();
        setupBackButton();
        
        // Enable full screen mode by default
        enableFullScreenMode();
        
        // Show interstitial ad after a short delay
        showInterstitialAdWithDelay();
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
        if (binding == null || binding.webView == null) return;
        
        // Setup touch listener for WebView to handle UI visibility in edge-to-edge mode
        binding.webView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Toggle system bar protection and UI visibility on touch
                if (isFullScreenMode) {
                    toggleSystemUIVisibility();
                }
            }
            return false; // Allow WebView to handle the touch normally
        });
        
        // Setup touch listener for root view as fallback
        binding.getRoot().setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFullScreenMode) {
                    toggleSystemUIVisibility();
                }
            }
            return false; // Allow normal touch handling
        });
        
        Log.d(TAG, "Edge-to-edge touch listeners setup for immersive experience");
    }
    
    /**
     * Toggle system UI visibility for immersive experience
     */
    private void toggleSystemUIVisibility() {
        if (binding == null) return;
        
        // Toggle system bar protection visibility
        boolean isProtectionVisible = binding.topSystemBarProtection != null && 
                                    binding.topSystemBarProtection.getVisibility() == View.VISIBLE;
        
        if (isProtectionVisible) {
            // Hide protection for full immersion
            if (binding.topSystemBarProtection != null) {
                binding.topSystemBarProtection.setVisibility(View.GONE);
            }
            if (binding.bottomSystemBarProtection != null) {
                binding.bottomSystemBarProtection.setVisibility(View.GONE);
            }
        } else {
            // Show protection for better readability
            if (binding.topSystemBarProtection != null) {
                binding.topSystemBarProtection.setVisibility(View.VISIBLE);
            }
            if (binding.bottomSystemBarProtection != null) {
                binding.bottomSystemBarProtection.setVisibility(View.VISIBLE);
            }
        }
        
        // Schedule auto-hide after a delay
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(() -> {
            if (binding != null && isFullScreenMode) {
                if (binding.topSystemBarProtection != null) {
                    binding.topSystemBarProtection.setVisibility(View.GONE);
                }
                if (binding.bottomSystemBarProtection != null) {
                    binding.bottomSystemBarProtection.setVisibility(View.GONE);
                }
            }
        }, AUTO_HIDE_DELAY_MILLIS);
    }

    private void setupWebView() {
        Log.d(TAG, "setupWebView: Configuring WebView");
        if (binding != null && binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDomStorageEnabled(true);
            
            // Ensure WebView is properly configured for edge-to-edge
            binding.webView.setFitsSystemWindows(false);
            
            // Set up WebView client with background color extraction
            binding.webView.setWebViewClient(new SafeWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    
                    // Show fullscreen toggle once content is loaded
                    binding.fullscreenToggle.setVisibility(View.VISIBLE);
                    
                    // Ensure system bar protections are visible
                    if (binding.topSystemBarProtection != null) {
                        binding.topSystemBarProtection.setVisibility(View.VISIBLE);
                    }
                    if (binding.bottomSystemBarProtection != null) {
                        binding.bottomSystemBarProtection.setVisibility(View.VISIBLE);
                    }
                    
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
                        
                        Log.d(TAG, "Extracted dominant color: " + Integer.toHexString(dominantColor));
                        
                        // Apply Material 3 dynamic theming
                        applyDynamicTheming(dominantColor);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error extracting color", e);
                // Apply default Material 3 theming on error
                applyDynamicTheming(Color.parseColor("#6200EE"));
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
        if (wishResponse == null) {
            Log.e(TAG, "setupWebViewContent: wishResponse is null");
            showError("Failed to load wish: empty response");
            return;
        }

        // Store the wish response for reuse functionality
        currentWish = wishResponse;
        
        // Show reuse button if we have valid template data
        if ((wishResponse.getTemplateId() != null && !wishResponse.getTemplateId().isEmpty()) || 
            (wishResponse.getTemplate() != null && wishResponse.getTemplate().getId() != null)) {
            binding.reuseTemplateButton.setVisibility(View.VISIBLE);
        }

        if (binding != null && binding.webView != null && wishResponse != null) {
            binding.contentLayout.setVisibility(View.VISIBLE);
            
            // Get content with null checks
            String html = wishResponse.getCustomizedHtml();
            String css = wishResponse.getCssContent(); // Try getting CSS directly first
            String js = wishResponse.getJsContent();   // Try getting JS directly first
            
            // If CSS/JS is empty in WishResponse, try getting from template
            if ((css == null || css.isEmpty()) && wishResponse.getTemplate() != null) {
                css = wishResponse.getTemplate().getCssContent();
                Log.d(TAG, "Using template CSS as fallback, length: " + (css != null ? css.length() : 0));
            }
            
            if ((js == null || js.isEmpty()) && wishResponse.getTemplate() != null) {
                js = wishResponse.getTemplate().getJsContent();
                Log.d(TAG, "Using template JS as fallback, length: " + (js != null ? js.length() : 0));
            }
            
            // Ensure content is not null
            html = html != null ? html : "";
            css = css != null ? css : "";
            js = js != null ? js : "";
            
            // Create final copies for use in lambda expressions
            final String finalHtml = html;
            final String finalCss = css;
            final String finalJs = js;
            
            // Log content lengths for debugging
            Log.d(TAG, "HTML content length: " + finalHtml.length());
            Log.d(TAG, "CSS content length: " + finalCss.length());
            Log.d(TAG, "JS content length: " + finalJs.length());
            
            // Add background color matching and edge-to-edge adjustments to the CSS
            final String finalCssWithBackground = finalCss + 
                "\nbody { background-color: transparent !important; margin: 0; padding: 0; }\n" +
                "html { height: 100%; overflow-x: hidden; }\n" +
                ".content-wrapper { padding-top: env(safe-area-inset-top); padding-bottom: env(safe-area-inset-bottom); }\n";
            
            // Better HTML construction with StringBuilder
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>\n");
            htmlBuilder.append("<html>\n");
            htmlBuilder.append("<head>\n");
            htmlBuilder.append("  <meta charset=\"UTF-8\">\n");
            htmlBuilder.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, viewport-fit=cover\">\n");
            
            // Add CSS with debug comment
            htmlBuilder.append("  <style type='text/css'>\n");
            htmlBuilder.append("    /* CSS Content Start */\n");
            htmlBuilder.append(finalCssWithBackground);
            htmlBuilder.append("\n    /* CSS Content End */\n");
            htmlBuilder.append("  </style>\n");
            
            // Add CSS validation helper
            htmlBuilder.append("  <script>\n");
            htmlBuilder.append("    function checkCssLoaded() {\n");
            htmlBuilder.append("      console.log('CSS Validation: Checking stylesheets...');\n");
            htmlBuilder.append("      var sheets = document.styleSheets;\n");
            htmlBuilder.append("      console.log('Found ' + sheets.length + ' stylesheets');\n");
            htmlBuilder.append("      return sheets.length > 0;\n");
            htmlBuilder.append("    }\n");
            htmlBuilder.append("    function testJs() {\n");
            htmlBuilder.append("      console.log('JavaScript is working!');\n");
            htmlBuilder.append("      return true;\n");
            htmlBuilder.append("    }\n");
            htmlBuilder.append("  </script>\n");
            htmlBuilder.append("</head>\n");
            htmlBuilder.append("<body>\n");
            
            // Wrap HTML content in a div with padding for safe areas
            htmlBuilder.append("<div class=\"content-wrapper\">\n");
            htmlBuilder.append(finalHtml);
            htmlBuilder.append("</div>\n");
            
            // Add JavaScript with debug comment and validation
            htmlBuilder.append("\n<script type='text/javascript'>\n");
            htmlBuilder.append("// JavaScript Content Start\n");
            htmlBuilder.append("console.log('Main JavaScript started executing');\n");
            htmlBuilder.append("document.addEventListener('DOMContentLoaded', function() {\n");
            htmlBuilder.append("  console.log('DOM fully loaded, running CSS check...');\n");
            htmlBuilder.append("  checkCssLoaded();\n");
            htmlBuilder.append("  testJs();\n");
            htmlBuilder.append("});\n\n");
            htmlBuilder.append(finalJs);
            htmlBuilder.append("\n// JavaScript Content End\n");
            htmlBuilder.append("</script>\n");
            
            htmlBuilder.append("</body>\n");
            htmlBuilder.append("</html>");
            
            String fullHtml = htmlBuilder.toString();
            
            // Enable JavaScript and other necessary settings
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            
            // Add console message handler to see JavaScript console logs
            binding.webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.d("WebView Console", consoleMessage.message() + " -- From line " +
                          consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                    return true;
                }
            });
            
            // Set up enhanced WebViewClient to handle page load events and inject fallbacks if needed
            binding.webView.setWebViewClient(new SafeWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    
                    // Show fullscreen toggle once content is loaded
                    binding.fullscreenToggle.setVisibility(View.VISIBLE);
                    
                    // Ensure system bar protections are visible for edge-to-edge design
                    if (binding.topSystemBarProtection != null) {
                        binding.topSystemBarProtection.setVisibility(View.VISIBLE);
                    }
                    if (binding.bottomSystemBarProtection != null) {
                        binding.bottomSystemBarProtection.setVisibility(View.VISIBLE);
                    }
                    
                    // Call test functions to verify CSS and JS are working
                    binding.webView.evaluateJavascript("checkCssLoaded()", 
                        value -> Log.d(TAG, "CSS loaded check: " + value));
                    
                    binding.webView.evaluateJavascript("testJs()", 
                        value -> Log.d(TAG, "JS test result: " + value));
                    
                    // Inject CSS as a fallback if needed
                    if (finalCss != null && !finalCss.isEmpty()) {
                        String cssEscaped = finalCss.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\"", "\\\"");
                        String injectCss = 
                            "var style = document.createElement('style');" +
                            "style.type = 'text/css';" +
                            "style.innerHTML = \"" + cssEscaped + "\";" +
                            "document.head.appendChild(style);" +
                            "console.log('CSS manually injected, length: " + finalCss.length() + "');" +
                            "true;";
                        
                        binding.webView.evaluateJavascript(injectCss, value -> 
                            Log.d(TAG, "CSS injection result: " + value));
                    }
                    
                    // Capture the WebView as a bitmap to extract dominant color
                    binding.webView.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(binding.webView.getDrawingCache());
                    binding.webView.setDrawingCacheEnabled(false);
                    
                    // Extract dominant color using Palette API
                    extractDominantColor(bitmap);
                }
            });
            
            // Load the HTML content into the WebView with a base URL
            binding.webView.loadDataWithBaseURL(
                "https://eventwish2.onrender.com/",
                fullHtml,
                "text/html",
                "UTF-8",
                null
            );
            
            // Make WebView background transparent and ensure it draws behind system bars
            binding.webView.setBackgroundColor(Color.TRANSPARENT);
            
            // Ensure WebView draws edge-to-edge
            binding.webView.setFitsSystemWindows(false);
        }
    }

    @Override
    protected void showError(String message) {
        if (isAdded() && binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove any pending callbacks
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Disable immersive mode and restore system UI
        disableMaterial3ImmersiveMode();
        
        // Cleanup EdgeToEdgeManager
        if (edgeToEdgeManager != null) {
            edgeToEdgeManager.disableEdgeToEdge();
            edgeToEdgeManager.cleanup();
        }
        
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
        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "WebView error: " + errorCode + " - " + description);
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

    private void setupReuseButton() {
        binding.reuseTemplateButton.setOnClickListener(v -> reuseTemplate());
        // Hide button initially until we have template data
        binding.reuseTemplateButton.setVisibility(View.GONE);
    }

    private void reuseTemplate() {
        if (currentWish == null) {
            Toast.makeText(requireContext(), getString(R.string.cannot_reuse_template_missing_data), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the template ID
        String templateId = null;
        if (currentWish.getTemplateId() != null) {
            templateId = currentWish.getTemplateId();
        } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getId() != null) {
            templateId = currentWish.getTemplate().getId();
        }
        
        if (templateId == null || templateId.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.cannot_reuse_template_missing_id), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Reusing template with ID: " + templateId);
        
        try {
            // Navigate to TemplateDetailFragment using action ID from nav_graph.xml
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("templateId", templateId);
            navController.navigate(R.id.action_resource_to_template_detail, args);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to template detail: " + e.getMessage(), e);
            Toast.makeText(requireContext(), getString(R.string.error_opening_template, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show an interstitial ad after a short delay to let the UI load first
     */
    private void showInterstitialAdWithDelay() {
        if (adMobManager == null) {
            try {
                // Try one more time to initialize the ad manager
                adMobManager = AdMobManager.getInstance();
                Log.d(TAG, "Re-initialized AdMobManager successfully");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot show interstitial ad: AdMobManager not available: " + e.getMessage());
                return;
            }
        }
        
        // Use a delay that's long enough for the fragment to fully initialize
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (!isAdded() || getActivity() == null) {
                    Log.e(TAG, "Cannot show interstitial ad: Fragment not attached to activity");
                    return;
                }
                
                Log.d(TAG, "Attempting to show interstitial ad");
                
                // Verify ad is ready first
                if (adMobManager.isInterstitialAdReady()) {
                    boolean adShown = adMobManager.showInterstitialAd(getActivity());
                    
                    if (adShown) {
                        Log.d(TAG, "Interstitial ad shown successfully");
                    } else {
                        Log.e(TAG, "Interstitial ad failed to show despite being ready");
                    }
                } else {
                    Log.d(TAG, "Interstitial ad not ready yet, will try again later");
                    
                    // Try again after a longer delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            if (isAdded() && getActivity() != null && adMobManager != null) {
                                boolean adShown = adMobManager.showInterstitialAd(getActivity());
                                Log.d(TAG, "Second attempt to show interstitial ad: " + (adShown ? "successful" : "failed"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in second attempt to show interstitial ad: " + e.getMessage());
                        }
                    }, 3000); // Try again after 3 seconds
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing interstitial ad: " + e.getMessage());
            }
        }, 2000); // 2 second initial delay
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Force dispatch analytics events to ensure they're sent to Firebase - Add try-catch to prevent crashes
        try {
            AnalyticsUtils.forceDispatchEvents();
            
            // Re-track screen view on resume to ensure it's recorded
            AnalyticsUtils.trackScreenView("ResourceFragment", ResourceFragment.class.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error in analytics tracking on resume: " + e.getMessage());
        }
    }
    
    /**
     * Setup back button click listener
     */
    private void setupBackButton() {
        if (binding != null && binding.backButton != null) {
            binding.backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked - navigating to HomeFragment");
                navigateToHome();
            });
        }
    }
    
    /**
     * Navigate to HomeFragment
     */
    private void navigateToHome() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_resource_to_home);
            Log.d(TAG, "Successfully navigated to HomeFragment");
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to HomeFragment", e);
            // Fallback: finish activity if navigation fails
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }
    
    /**
     * Enable Material 3 immersive mode with transparent status bar and edge-to-edge design
     * Following Android's edge-to-edge design guidelines
     */
    private void enableMaterial3ImmersiveMode() {
        Activity activity = getActivity();
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Store original values for restoration
        originalStatusBarColor = window.getStatusBarColor();
        originalNavigationBarColor = window.getNavigationBarColor();
        
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            originalLightStatusBar = windowInsetsController.isAppearanceLightStatusBars();
            originalLightNavigationBar = windowInsetsController.isAppearanceLightNavigationBars();
        }
        
        // Enable edge-to-edge design - content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false);
        
        // Set transparent system bars for immersive experience
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        
        // Configure system bar appearance for optimal contrast
        if (windowInsetsController != null) {
            // Use dark content for better visibility on light backgrounds
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
            
            // Set system bar behavior
            windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
        
        // Set window flags for navigation bar handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        
        // Make sure the WebView is properly configured for edge-to-edge
        if (binding != null && binding.webView != null) {
            // Ensure WebView draws behind system bars
            ViewCompat.setOnApplyWindowInsetsListener(binding.webView, (v, insets) -> {
                // Don't apply insets to WebView to allow it to draw edge-to-edge
                return insets;
            });
            
            // Make sure system bar protection views are visible
            if (binding.topSystemBarProtection != null) {
                binding.topSystemBarProtection.setVisibility(View.VISIBLE);
            }
            if (binding.bottomSystemBarProtection != null) {
                binding.bottomSystemBarProtection.setVisibility(View.VISIBLE);
            }
        }
        
        // Setup window insets handling for proper content padding
        setupWindowInsetsHandling();
        
        Log.d(TAG, "Material 3 immersive mode enabled with transparent system bars and edge-to-edge design");
    }
    
    /**
     * Disable Material 3 immersive mode and restore original system UI
     */
    private void disableMaterial3ImmersiveMode() {
        Activity activity = getActivity();
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Restore window insets behavior
        WindowCompat.setDecorFitsSystemWindows(window, true);
        
        // Restore system bar appearance
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(originalLightStatusBar);
            windowInsetsController.setAppearanceLightNavigationBars(originalLightNavigationBar);
        }
        
        // Restore original colors
        window.setStatusBarColor(originalStatusBarColor);
        window.setNavigationBarColor(originalNavigationBarColor);
        
        // Show bottom navigation
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
        
        // Hide system bar protection
        if (binding != null) {
            if (binding.topSystemBarProtection != null) {
                binding.topSystemBarProtection.setVisibility(View.GONE);
            }
            if (binding.bottomSystemBarProtection != null) {
                binding.bottomSystemBarProtection.setVisibility(View.GONE);
            }
        }
        
        // Remove window insets listener
        if (binding != null && binding.getRoot() != null) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), null);
        }
        
        Log.d(TAG, "Material 3 immersive mode disabled and original system UI restored");
    }
    
    /**
     * Apply dynamic theming based on WebView content color
     */
    private void applyDynamicTheming(int dominantColor) {
        Activity activity = getActivity();
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Create darker variation for status bar
        int statusBarColor = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.3f);
        
        // Apply to status bar only (keep it visible)
        window.setStatusBarColor(statusBarColor);
        
        // Keep navigation bar transparent
        window.setNavigationBarColor(Color.TRANSPARENT);
        
        // Apply lighter background to fragment content
        int lightColor = ColorUtils.blendARGB(dominantColor, Color.WHITE, 0.8f);
        if (binding != null) {
            binding.getRoot().setBackgroundColor(lightColor);
        }
        
        // Update window insets controller for proper contrast
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            // Determine if we need light or dark content based on status bar color
            boolean isLightBackground = ColorUtils.calculateLuminance(statusBarColor) > 0.5;
            windowInsetsController.setAppearanceLightStatusBars(isLightBackground);
            windowInsetsController.setAppearanceLightNavigationBars(isLightBackground);
        }
        
        Log.d(TAG, "Applied dynamic theming with status bar color: " + Integer.toHexString(statusBarColor));
    }
    
    /**
     * Setup window insets handling for edge-to-edge design
     * Ensures content is properly positioned relative to system bars
     */
    private void setupWindowInsetsHandling() {
        if (binding == null || binding.getRoot() == null) return;
        
        // Apply window insets to the root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            // Get system bar insets
            androidx.core.graphics.Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets displayCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            androidx.core.graphics.Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            
            // Combine all insets for proper edge-to-edge handling
            int topInset = Math.max(systemBarsInsets.top, displayCutoutInsets.top);
            int bottomInset = Math.max(Math.max(systemBarsInsets.bottom, displayCutoutInsets.bottom),
                                     Math.max(navigationBarsInsets.bottom, gestureInsets.bottom));
            int leftInset = Math.max(Math.max(systemBarsInsets.left, displayCutoutInsets.left),
                                   Math.max(navigationBarsInsets.left, gestureInsets.left));
            int rightInset = Math.max(Math.max(systemBarsInsets.right, displayCutoutInsets.right),
                                    Math.max(navigationBarsInsets.right, gestureInsets.right));
            
            Log.d(TAG, "Window insets - top: " + topInset + ", bottom: " + bottomInset + 
                      ", left: " + leftInset + ", right: " + rightInset);
            
            // Apply insets to critical UI elements that shouldn't be obscured
            applyInsetsToUI(topInset, bottomInset, leftInset, rightInset);
            
            // Show system bar protection when needed
            updateSystemBarProtection(topInset, bottomInset);
            
            // Return insets to allow other views to handle them
            return insets;
        });
    }
    
    /**
     * Apply window insets to UI elements that need to avoid system bars
     */
    private void applyInsetsToUI(int topInset, int bottomInset, int leftInset, int rightInset) {
        if (binding == null) return;
        
        // Apply top inset to back button
        if (binding.backButton != null) {
            ViewGroup.MarginLayoutParams backButtonParams = 
                (ViewGroup.MarginLayoutParams) binding.backButton.getLayoutParams();
            backButtonParams.topMargin = topInset + getResources().getDimensionPixelSize(R.dimen.standard_margin);
            backButtonParams.leftMargin = leftInset + getResources().getDimensionPixelSize(R.dimen.standard_margin);
            binding.backButton.setLayoutParams(backButtonParams);
        }
        
        // Apply bottom inset to floating action buttons
        if (binding.fullscreenToggle != null) {
            ViewGroup.MarginLayoutParams toggleParams = 
                (ViewGroup.MarginLayoutParams) binding.fullscreenToggle.getLayoutParams();
            toggleParams.bottomMargin = bottomInset + getResources().getDimensionPixelSize(R.dimen.standard_margin);
            toggleParams.rightMargin = rightInset + getResources().getDimensionPixelSize(R.dimen.standard_margin);
            binding.fullscreenToggle.setLayoutParams(toggleParams);
        }
        
        if (binding.reuseTemplateButton != null) {
            ViewGroup.MarginLayoutParams reuseParams = 
                (ViewGroup.MarginLayoutParams) binding.reuseTemplateButton.getLayoutParams();
            reuseParams.rightMargin = rightInset + getResources().getDimensionPixelSize(R.dimen.standard_margin);
            binding.reuseTemplateButton.setLayoutParams(reuseParams);
        }
        
        // WebView should draw edge-to-edge (no insets applied)
        // This allows content to draw behind system bars for immersive experience
    }
    
    /**
     * Update system bar protection visibility based on content and insets
     */
    private void updateSystemBarProtection(int topInset, int bottomInset) {
        if (binding == null) return;
        
        // Always show top protection for status bar when in edge-to-edge mode
        if (binding.topSystemBarProtection != null) {
            if (topInset > 0) {
                binding.topSystemBarProtection.setVisibility(View.VISIBLE);
                
                // Adjust protection height based on inset
                ViewGroup.LayoutParams topParams = binding.topSystemBarProtection.getLayoutParams();
                topParams.height = topInset + getResources().getDimensionPixelSize(R.dimen.gradient_protection_extra);
                binding.topSystemBarProtection.setLayoutParams(topParams);
                
                Log.d(TAG, "Top system bar protection enabled with height: " + topParams.height);
            } else {
                binding.topSystemBarProtection.setVisibility(View.GONE);
            }
        }
        
        // Always show bottom protection for navigation bar when in edge-to-edge mode
        if (binding.bottomSystemBarProtection != null) {
            if (bottomInset > 0) {
                binding.bottomSystemBarProtection.setVisibility(View.VISIBLE);
                
                // Adjust protection height based on inset
                ViewGroup.LayoutParams bottomParams = binding.bottomSystemBarProtection.getLayoutParams();
                bottomParams.height = bottomInset + getResources().getDimensionPixelSize(R.dimen.gradient_protection_extra);
                binding.bottomSystemBarProtection.setLayoutParams(bottomParams);
                
                Log.d(TAG, "Bottom system bar protection enabled with height: " + bottomParams.height);
            } else {
                binding.bottomSystemBarProtection.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Check if content is drawing behind status bar
     */
    private boolean isContentBehindStatusBar() {
        // In edge-to-edge mode, content always draws behind status bar
        return true;
    }
    
    /**
     * Check if content is drawing behind navigation bar
     */
    private boolean isContentBehindNavigationBar() {
        // In edge-to-edge mode, content always draws behind navigation bar
        return true;
    }
}