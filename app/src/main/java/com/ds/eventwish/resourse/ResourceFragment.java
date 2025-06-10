package com.ds.eventwish.resourse;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
        setupReuseButton();
        
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
            
            // Add background color matching to the CSS
            final String finalCssWithBackground = finalCss + "\nbody { background-color: transparent !important; }\n";
            
            // Better HTML construction with StringBuilder
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>\n");
            htmlBuilder.append("<html>\n");
            htmlBuilder.append("<head>\n");
            htmlBuilder.append("  <meta charset=\"UTF-8\">\n");
            htmlBuilder.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0, minimum-scale=0.5\">\n");
            
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
            
            // Add HTML Content
            htmlBuilder.append(finalHtml);
            
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
            
            // Make WebView background transparent to match fragment background
            binding.webView.setBackgroundColor(Color.TRANSPARENT);
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
}