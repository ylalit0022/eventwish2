package com.ds.eventwish.ui.template;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.utils.UserDataManager;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer class for template WebViews with user data placeholder replacement.
 * Handles HTML processing, user placeholder replacement, and WebView configuration
 */
public class TemplateWebViewRenderer {
    private static final String TAG = "TemplateWebViewRenderer";
    
    // HTML placeholder patterns
    private static final Pattern SENDER_NAME_PATTERN = Pattern.compile(
        "(?i)(<span[^>]*class=[\"']?[^\"']*sender-name[^\"']*[\"']?[^>]*>)\\[Your Name\\](</span>)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
        "(?i)(<img[^>]*src=[\"']?)\\[IMAGE_URL\\]([\"']?[^>]*>)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ALT_TEXT_PATTERN = Pattern.compile(
        "(?i)(alt=[\"']?)\\[([^\\]]+)\\]([\"']?)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Default placeholder image
    private static final String DEFAULT_USER_IMAGE = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNFMEUwRTAiLz4KPGNpcmNsZSBjeD0iMjAiIGN5PSIxNiIgcj0iNiIgZmlsbD0iIzk5OTk5OSIvPgo8cGF0aCBkPSJNMzAgMzJDMzAgMjYuNDc3MSAyNS41MjI5IDIyIDIwIDIyQzE0LjQ3NzEgMjIgMTAgMjYuNDc3MSAxMCAzMiIgZmlsbD0iIzk5OTk5OSIvPgo8L3N2Zz4K";
    
    // Threading
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Dependencies
    private final WeakReference<Context> contextRef;
    private final UserDataManager userDataManager;
    
    /**
     * Callback interface for render operations
     */
    public interface RenderCallback {
        void onRenderComplete();
        void onRenderError(String error);
        void onLoadingStateChanged(boolean isLoading);
    }
    
    public TemplateWebViewRenderer(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.userDataManager = UserDataManager.getInstance(context);
    }
    
    /**
     * Render template in WebView with user data integration
     */
    public void renderTemplate(WebView webView, Template template, RenderCallback callback) {
        if (webView == null || template == null) {
            if (callback != null) {
                callback.onRenderError("WebView or Template is null");
            }
            return;
        }
        
        // Configure WebView first
        configureWebView(webView, callback);
        
        // Process template on background thread
        backgroundExecutor.execute(() -> {
            try {
                String processedHtml = processTemplateWithUserData(template);
                
                mainHandler.post(() -> {
                    try {
                        if (callback != null) {
                            callback.onLoadingStateChanged(true);
                        }
                        
                        // Load the processed HTML
                        webView.loadDataWithBaseURL(
                            "file:///android_asset/",
                            processedHtml,
                            "text/html",
                            "UTF-8",
                            null
                        );
                        
                        Log.d(TAG, "Template loaded in WebView: " + template.getId());
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading HTML in WebView", e);
                        if (callback != null) {
                            callback.onRenderError("Failed to load HTML: " + e.getMessage());
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing template", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onRenderError("Failed to process template: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * Configure WebView settings for optimal template rendering
     */
    private void configureWebView(WebView webView, RenderCallback callback) {
        try {
            WebSettings settings = webView.getSettings();
            
            // Enable JavaScript and DOM storage
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(false);
            
            // Configure layout and viewport
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            settings.setDefaultTextEncodingName("UTF-8");
            
            // Enable image loading
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            
            // Disable zoom controls
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setSupportZoom(false);
            
            // Performance optimizations
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            
            // Security settings
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            
            // Set WebView client for handling page events
            webView.setWebViewClient(new TemplateWebViewClient(callback));
            
            // Set background and layer type
            webView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
            
            Log.d(TAG, "WebView configured for template rendering");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView", e);
            if (callback != null) {
                callback.onRenderError("Failed to configure WebView: " + e.getMessage());
            }
        }
    }
    
    /**
     * Process template HTML with user data placeholder replacement
     */
    private String processTemplateWithUserData(Template template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        
        String htmlContent = template.getHtmlContent();
        String cssContent = template.getCssContent();
        String jsContent = template.getJsContent();
        
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Template HTML content is empty");
        }
        
        // Get user data
        String userName = userDataManager.getDisplayNameWithFallback();
        String userPhoto = userDataManager.getCachedUserPhoto();
        
        Log.d(TAG, "Processing template with user data - Name: " + userName + 
                  ", Photo: " + (userPhoto != null ? "Available" : "Default"));
        
        // Replace user placeholders in HTML
        String processedHtml = replaceUserPlaceholders(htmlContent, userName, userPhoto, template);
        
        // Build complete HTML document
        return buildCompleteHtml(processedHtml, cssContent, jsContent, template);
    }
    
    /**
     * Replace user data placeholders in HTML content
     */
    private String replaceUserPlaceholders(String html, String userName, String userPhoto, Template template) {
        if (html == null) return "";
        
        String processedHtml = html;
        
        try {
            // Replace sender name placeholders
            if (userName != null && !userName.trim().isEmpty()) {
                Matcher senderMatcher = SENDER_NAME_PATTERN.matcher(processedHtml);
                processedHtml = senderMatcher.replaceAll("$1" + userName + "$2");
                
                // Also replace simple [Your Name] patterns
                processedHtml = processedHtml.replaceAll("(?i)\\[Your Name\\]", userName);
            }
            
            // Replace image URL placeholders
            String imageUrl = userPhoto != null ? userPhoto : DEFAULT_USER_IMAGE;
            Matcher imageMatcher = IMAGE_URL_PATTERN.matcher(processedHtml);
            processedHtml = imageMatcher.replaceAll("$1" + imageUrl + "$2");
            
            // Replace [IMAGE_URL] patterns
            processedHtml = processedHtml.replaceAll("(?i)\\[IMAGE_URL\\]", imageUrl);
            
            // Replace alt text placeholders
            if (template.getTitle() != null) {
                Matcher altMatcher = ALT_TEXT_PATTERN.matcher(processedHtml);
                processedHtml = altMatcher.replaceAll("$1" + template.getTitle() + "$3");
            }
            
            Log.d(TAG, "User placeholders replaced successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error replacing user placeholders", e);
            // Return original HTML if replacement fails
            return html;
        }
        
        return processedHtml;
    }
    
    /**
     * Build complete HTML document with CSS and JS
     */
    private String buildCompleteHtml(String htmlContent, String cssContent, String jsContent, Template template) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html lang=\"en\">");
        htmlBuilder.append("<head>");
        htmlBuilder.append("<meta charset=\"UTF-8\">");
        htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">");
        htmlBuilder.append("<title>").append(template.getTitle() != null ? template.getTitle() : "Template").append("</title>");
        
        // Add CSS content
        if (cssContent != null && !cssContent.trim().isEmpty()) {
            htmlBuilder.append("<style type=\"text/css\">");
            htmlBuilder.append(cssContent);
            htmlBuilder.append("</style>");
        }
        
        // Add responsive CSS for mobile optimization
        htmlBuilder.append("<style type=\"text/css\">");
        htmlBuilder.append("body { margin: 0; padding: 8px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }");
        htmlBuilder.append("img { max-width: 100%; height: auto; display: block; }");
        htmlBuilder.append("* { box-sizing: border-box; }");
        htmlBuilder.append(".sender-name { font-weight: bold; color: #333; }");
        htmlBuilder.append("</style>");
        
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");
        
        // Add HTML content
        htmlBuilder.append(htmlContent);
        
        // Add JavaScript content
        if (jsContent != null && !jsContent.trim().isEmpty()) {
            htmlBuilder.append("<script type=\"text/javascript\">");
            htmlBuilder.append(jsContent);
            htmlBuilder.append("</script>");
        }
        
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Custom WebViewClient for handling template rendering events
     */
    private static class TemplateWebViewClient extends WebViewClient {
        private final RenderCallback callback;
        
        public TemplateWebViewClient(RenderCallback callback) {
            this.callback = callback;
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (callback != null) {
                callback.onLoadingStateChanged(true);
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            
            // Inject additional optimizations
            String optimizationJs = 
                "javascript:(function() {" +
                "  document.body.style.width = '100%';" +
                "  document.body.style.margin = '0';" +
                "  document.body.style.padding = '8px';" +
                "  var imgs = document.getElementsByTagName('img');" +
                "  for(var i = 0; i < imgs.length; i++) {" +
                "    imgs[i].style.maxWidth = '100%';" +
                "    imgs[i].style.height = 'auto';" +
                "  }" +
                "})()";
            
            view.evaluateJavascript(optimizationJs, null);
            
            if (callback != null) {
                callback.onLoadingStateChanged(false);
                callback.onRenderComplete();
            }
        }
        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.e(TAG, "WebView error: " + description + " (Code: " + errorCode + ")");
            
            if (callback != null) {
                callback.onLoadingStateChanged(false);
                callback.onRenderError("WebView error: " + description);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up TemplateWebViewRenderer");
        
        if (!backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        contextRef.clear();
    }
    
    /**
     * Get user data manager instance
     */
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }
} 