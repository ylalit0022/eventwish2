package com.ds.eventwish.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.View;

public class TemplateOverlayHelper {
    // Server-side placeholders for dynamic content
    private static final String USER_NAME_PLACEHOLDER = "{{userName}}";
    private static final String USER_PHOTO_PLACEHOLDER = "{{userPhoto}}";
    private static final String USER_ID_PLACEHOLDER = "{{userId}}";
    private static final String SHOW_PHOTO_PLACEHOLDER = "{{showPhoto}}";
    private static final String SHOW_NAME_PLACEHOLDER = "{{showName}}";
    private static final String POSITION_PLACEHOLDER = "{{position}}";
    private static final String THEME_PLACEHOLDER = "{{theme}}";
    private static final String DEFAULT_PHOTO_URL = "https://api.dicebear.com/7.x/avatars/svg?seed=";

    private static final String TAG = "TemplateOverlayHelper";

    /**
     * Creates a WebView with the overlay using server-provided templates
     */
    public static WebView createOverlayWebView(
            @NonNull Context context,
            @NonNull String serverHtmlTemplate,
            @NonNull String serverCssTemplate,
            @Nullable String serverJsTemplate,
            @Nullable String userName,
            @Nullable String photoUrl,
            @Nullable String userId) {
            
        Log.d(TAG, "🎨 Creating overlay WebView:");
        Log.d(TAG, "   • HTML Template length: " + (serverHtmlTemplate != null ? serverHtmlTemplate.length() : "null"));
        Log.d(TAG, "   • CSS Template length: " + (serverCssTemplate != null ? serverCssTemplate.length() : "null"));
        Log.d(TAG, "   • JS Template length: " + (serverJsTemplate != null ? serverJsTemplate.length() : "null"));
        Log.d(TAG, "   • User Name: " + userName);
        Log.d(TAG, "   • User Photo: " + photoUrl);
        Log.d(TAG, "   • User ID: " + userId);
        
        WebView webView = new WebView(context);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Configure WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(serverJsTemplate != null);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Handle null values
        String finalUserName = TextUtils.isEmpty(userName) ? "Anonymous User" : userName;
        String finalPhotoUrl = TextUtils.isEmpty(photoUrl) ? 
            DEFAULT_PHOTO_URL + (userId != null ? userId : "default") : photoUrl;

        // Replace placeholders in server template
        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'><style>" +
                     serverCssTemplate +
                     "</style>";
        
        // Add JavaScript if provided
        if (serverJsTemplate != null) {
            html += "<script type='text/javascript'>" +
                    serverJsTemplate +
                    "</script>";
        }
        
        html += "</head><body>" +
                serverHtmlTemplate
                    .replace(USER_NAME_PLACEHOLDER, finalUserName)
                    .replace(USER_PHOTO_PLACEHOLDER, finalPhotoUrl)
                    .replace(USER_ID_PLACEHOLDER, userId != null ? userId : "default") +
                "</body></html>";

        Log.d(TAG, "📄 Generated HTML document:\n" + html);
        
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        // Add WebView client for logging
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "🌐 WebView started loading");
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "✅ WebView finished loading");
                
                // Inject CSS to fix potential rendering issues
                String fixCss = "body { margin: 0; padding: 0; width: 100%; overflow-x: hidden; } " +
                              "img { max-width: 100%; height: auto; } " +
                              "* { -webkit-text-size-adjust: none; }";
                
                String js = "javascript:(function() { " +
                           "var style = document.createElement('style'); " +
                           "style.type = 'text/css'; " +
                           "style.innerHTML = '" + fixCss + "'; " +
                           "document.head.appendChild(style); " +
                           "})()";
                
                view.evaluateJavascript(js, null);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "❌ WebView error: " + description);
            }
        });
        
        return webView;
    }

    /**
     * Template data structure that matches server response
     */
    public static class TemplateData {
        public String htmlTemplate;  // HTML structure with placeholders
        public String cssTemplate;   // CSS styles for overlay
        public String jsTemplate;    // Optional JavaScript for interactivity
    }
} 