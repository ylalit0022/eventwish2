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

            // Enhanced JavaScript to extract background color from HTML and communicate it back to Android
            String colorExtractionJs = 
                "function extractBackgroundColor() {" +
                "  var bodyBg = window.getComputedStyle(document.body).backgroundColor;" +
                "  var htmlBg = window.getComputedStyle(document.documentElement).backgroundColor;" +
                "  var mainBg = 'transparent';" +
                "  var divBg = 'transparent';" +
                "  var contentBg = 'transparent';" +
                "  " +
                "  // Try to find main container elements" +
                "  var mainElement = document.querySelector('main') || document.querySelector('.container') || document.querySelector('.main-content');" +
                "  if (mainElement) {" +
                "    mainBg = window.getComputedStyle(mainElement).backgroundColor;" +
                "  }" +
                "  " +
                "  // Look for the first large div with a background color" +
                "  var divs = document.querySelectorAll('div');" +
                "  for (var i = 0; i < divs.length; i++) {" +
                "    if (divs[i].clientWidth > window.innerWidth * 0.8 && divs[i].clientHeight > window.innerHeight * 0.3) {" +
                "      var bgColor = window.getComputedStyle(divs[i]).backgroundColor;" +
                "      if (bgColor && bgColor !== 'transparent' && bgColor !== 'rgba(0, 0, 0, 0)') {" +
                "        divBg = bgColor;" +
                "        break;" +
                "      }" +
                "    }" +
                "  }" +
                "  " +
                "  // Check for content sections" +
                "  var contentElement = document.querySelector('.content') || document.querySelector('#content') || document.querySelector('article');" +
                "  if (contentElement) {" +
                "    contentBg = window.getComputedStyle(contentElement).backgroundColor;" +
                "  }" +
                "  " +
                "  // Also check for background-color CSS variables" +
                "  var rootStyles = window.getComputedStyle(document.documentElement);" +
                "  var bgVariable = rootStyles.getPropertyValue('--background-color') || rootStyles.getPropertyValue('--bg-color') || rootStyles.getPropertyValue('--page-bg');" +
                "  " +
                "  // Choose the most appropriate background color" +
                "  var bgColor = 'rgb(255, 255, 255)';" + // Default white
                "  if (bgVariable && bgVariable !== 'transparent' && bgVariable !== 'rgba(0, 0, 0, 0)' && bgVariable.trim() !== '') {" +
                "    bgColor = bgVariable.trim();" + 
                "  } else if (bodyBg && bodyBg !== 'transparent' && bodyBg !== 'rgba(0, 0, 0, 0)') {" +
                "    bgColor = bodyBg;" +
                "  } else if (htmlBg && htmlBg !== 'transparent' && htmlBg !== 'rgba(0, 0, 0, 0)') {" +
                "    bgColor = htmlBg;" +
                "  } else if (mainBg && mainBg !== 'transparent' && mainBg !== 'rgba(0, 0, 0, 0)') {" +
                "    bgColor = mainBg;" +
                "  } else if (divBg && divBg !== 'transparent' && divBg !== 'rgba(0, 0, 0, 0)') {" +
                "    bgColor = divBg;" +
                "  } else if (contentBg && contentBg !== 'transparent' && contentBg !== 'rgba(0, 0, 0, 0)') {" +
                "    bgColor = contentBg;" +
                "  }" +
                "  " +
                "  // Send the extracted color back to Android" +
                "  window.Android.onBackgroundColorExtracted(bgColor);" +
                "  " +
                "  // Also send page load complete event" +
                "  window.Android.onPageFullyLoaded();" +
                "}" +
                "window.addEventListener('load', function() {" +
                "  // First attempt immediately after load" +
                "  extractBackgroundColor();" +
                "  // Second attempt after a delay to ensure all styles are applied" +
                "  setTimeout(extractBackgroundColor, 500);" +
                "});";

            // Make body background transparent to see fragment background
            css += "\nbody, html { background-color: transparent !important; }\n";

            String fullHtml = String.format(
                "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0, minimum-scale=0.5\">" +
                        "<style>%s</style>" +
                        "</head>" +
                        "<body>%s" +
                        "<script>%s</script>" +
                        "<script>%s</script>" +
                        "</body>" +
                "</html>",
                css, html, js, colorExtractionJs
            );
            
            // Set up a JavaScript interface to receive the extracted color
            binding.webView.addJavascriptInterface(new WebViewJSInterface(), "Android");
            
            // Initially set a neutral background color
            binding.getRoot().setBackgroundColor(Color.rgb(245, 245, 245));
            binding.contentLayout.setBackgroundColor(Color.rgb(245, 245, 245));
            
            // Make WebView background transparent to match fragment background
            binding.webView.setBackgroundColor(Color.TRANSPARENT);
            
            // Load the HTML content
            binding.webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
        }
    }

    /**
     * JavaScript interface to receive the extracted background color from WebView
     */
    private class WebViewJSInterface {
        @android.webkit.JavascriptInterface
        public void onBackgroundColorExtracted(String colorValue) {
            Log.d(TAG, "Extracted background color from HTML: " + colorValue);
            // Parse the color and apply it on the UI thread
            try {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        int color = parseColorValue(colorValue);
                        applyExtractedColor(color);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error applying extracted color", e);
            }
        }
        
        @android.webkit.JavascriptInterface
        public void onPageFullyLoaded() {
            Log.d(TAG, "Page fully loaded in WebView");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Hide progress indicator if still visible
                    if (binding != null && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }
        
        // Enhanced color parser that handles various CSS color formats
        private int parseColorValue(String colorValue) {
            try {
                // Trim the input string
                colorValue = colorValue.trim();
                
                // Handle rgb(r, g, b) format
                if (colorValue.startsWith("rgb(")) {
                    String[] values = colorValue.substring(4, colorValue.length() - 1).split(",");
                    int r = Integer.parseInt(values[0].trim());
                    int g = Integer.parseInt(values[1].trim());
                    int b = Integer.parseInt(values[2].trim());
                    return Color.rgb(r, g, b);
                } 
                // Handle rgba(r, g, b, a) format
                else if (colorValue.startsWith("rgba(")) {
                    String[] values = colorValue.substring(5, colorValue.length() - 1).split(",");
                    int r = Integer.parseInt(values[0].trim());
                    int g = Integer.parseInt(values[1].trim());
                    int b = Integer.parseInt(values[2].trim());
                    float a = Float.parseFloat(values[3].trim());
                    return Color.argb((int)(a * 255), r, g, b);
                }
                // Handle hsl(h, s%, l%) format
                else if (colorValue.startsWith("hsl(")) {
                    return parseHslColor(colorValue);
                }
                // Handle hsla(h, s%, l%, a) format
                else if (colorValue.startsWith("hsla(")) {
                    return parseHslColor(colorValue);
                }
                // Handle hex format (#RGB or #RRGGBB)
                else if (colorValue.startsWith("#")) {
                    return Color.parseColor(colorValue);
                }
                // Handle named colors
                else if (colorValue.matches("[a-zA-Z]+")) {
                    try {
                        // Try to parse as a named color
                        java.lang.reflect.Field field = Color.class.getField(colorValue.toUpperCase());
                        return field.getInt(null);
                    } catch (Exception e) {
                        Log.w(TAG, "Named color not recognized: " + colorValue);
                        return Color.WHITE; // Default to white
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing color: " + colorValue, e);
            }
            // Default fallback color
            return Color.WHITE;
        }
        
        // Helper method to parse HSL color format
        private int parseHslColor(String hslColor) {
            try {
                // Extract the values from the HSL string
                String valuePart = hslColor.startsWith("hsla(") ? 
                    hslColor.substring(5, hslColor.length() - 1) : 
                    hslColor.substring(4, hslColor.length() - 1);
                
                String[] values = valuePart.split(",");
                float h = Float.parseFloat(values[0].trim()) / 360f; // Normalize to 0-1
                
                // Handle percentage values for saturation and lightness
                float s = values[1].trim().endsWith("%") ? 
                    Float.parseFloat(values[1].trim().substring(0, values[1].trim().length() - 1)) / 100f : 
                    Float.parseFloat(values[1].trim());
                
                float l = values[2].trim().endsWith("%") ? 
                    Float.parseFloat(values[2].trim().substring(0, values[2].trim().length() - 1)) / 100f : 
                    Float.parseFloat(values[2].trim());
                
                // Handle alpha channel if present
                float a = 1.0f;
                if (values.length > 3) {
                    a = Float.parseFloat(values[3].trim());
                }
                
                // Convert HSL to RGB
                float[] rgb = hslToRgb(h, s, l);
                return Color.argb((int)(a * 255), (int)(rgb[0] * 255), (int)(rgb[1] * 255), (int)(rgb[2] * 255));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing HSL color: " + hslColor, e);
                return Color.WHITE;
            }
        }
        
        // Helper method to convert HSL to RGB
        private float[] hslToRgb(float h, float s, float l) {
            float r, g, b;
            
            if (s == 0) {
                // Achromatic (grey)
                r = g = b = l;
            } else {
                float q = l < 0.5 ? l * (1 + s) : l + s - l * s;
                float p = 2 * l - q;
                r = hueToRgb(p, q, h + 1f/3f);
                g = hueToRgb(p, q, h);
                b = hueToRgb(p, q, h - 1f/3f);
            }
            
            return new float[]{r, g, b};
        }
        
        private float hueToRgb(float p, float q, float t) {
            if (t < 0) t += 1;
            if (t > 1) t -= 1;
            if (t < 1f/6f) return p + (q - p) * 6 * t;
            if (t < 1f/2f) return q;
            if (t < 2f/3f) return p + (q - p) * (2f/3f - t) * 6;
            return p;
        }
    }

    private void applyExtractedColor(int color) {
        if (binding != null) {
            try {
                Log.d(TAG, "Applying extracted color to fragment background");
                
                // Calculate color brightness to determine if we need to adjust it
                double brightness = calculateBrightness(color);
                
                // Adjust the color if it's too dark or too light
                if (brightness < 0.5) {
                    // Brighten dark colors for better readability
                    color = ColorUtils.blendARGB(color, Color.WHITE, 0.4f);
                } else if (brightness > 0.9) {
                    // Slightly tone down very bright colors
                    color = ColorUtils.blendARGB(color, Color.LTGRAY, 0.1f);
                }
                
                // Apply the color with a smooth transition animation (if we can get the current color)
                int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
                int currentColor = Color.WHITE; // Default starting color
                
                try {
                    // Get the current background color if possible
                    android.graphics.drawable.ColorDrawable background = 
                        (android.graphics.drawable.ColorDrawable) binding.getRoot().getBackground();
                    if (background != null) {
                        currentColor = background.getColor();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get current background color", e);
                }
                
                // Use ValueAnimator for smooth color transition
                android.animation.ValueAnimator colorAnimation = android.animation.ValueAnimator.ofObject(
                        new android.animation.ArgbEvaluator(), currentColor, color);
                colorAnimation.setDuration(duration);
                colorAnimation.addUpdateListener(animator -> {
                    int animatedColor = (int) animator.getAnimatedValue();
                    binding.getRoot().setBackgroundColor(animatedColor);
                    binding.contentLayout.setBackgroundColor(animatedColor);
                });
                colorAnimation.start();
                
                // Also update any navigation bars or UI elements to match the theme
                updateUIElementsForTheme(color, brightness);
            } catch (Exception e) {
                Log.e(TAG, "Error applying background color", e);
                // Fallback to a simple color application without animation
                binding.getRoot().setBackgroundColor(color);
                binding.contentLayout.setBackgroundColor(color);
            }
        }
    }

    /**
     * Calculate the perceived brightness of a color
     * @param color The color to analyze
     * @return Brightness value between 0 (darkest) and 1 (brightest)
     */
    private double calculateBrightness(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        // Using the formula for perceived brightness: 
        // (0.299*R + 0.587*G + 0.114*B) / 255
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    }

    /**
     * Update UI elements based on the theme color and brightness
     */
    private void updateUIElementsForTheme(int themeColor, double brightness) {
        // Determine if we should use light or dark text/icons based on background brightness
        boolean useDarkText = brightness > 0.6;
        int textColor = useDarkText ? Color.BLACK : Color.WHITE;
        int iconTint = useDarkText ? Color.DKGRAY : Color.WHITE;
        
        // Update fullscreen toggle icon tint
        binding.fullscreenToggle.setColorFilter(iconTint);
        
        // Update status bar color if we're in API 23+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && getActivity() != null) {
            getActivity().getWindow().setStatusBarColor(themeColor);
            
            // Set light or dark status bar icons based on background color
            View decorView = getActivity().getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (useDarkText) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
        
        // Update navigation bar color on API 26+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && getActivity() != null) {
            getActivity().getWindow().setNavigationBarColor(themeColor);
            
            // Set light or dark navigation bar icons based on background color
            View decorView = getActivity().getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (useDarkText) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
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