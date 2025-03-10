package com.ds.eventwish.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Utility class for handling WebView link clicks
 */
public class WebViewLinkHandler {
    private static final String TAG = "WebViewLinkHandler";
    
    /**
     * Set up WebView link handling
     * @param webView The WebView to set up
     * @param context The context
     */
    public static void setupWebViewLinkHandling(WebView webView, Context context) {
        if (webView == null || context == null) {
            Log.e(TAG, "WebView or context is null");
            return;
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "WebView link clicked: " + url);
                
                // Check if the URL is an EventWish URL
                if (LinkChooserUtil.isEventWishUrl(url)) {
                    // Handle EventWish URLs with DeepLinkHandler
                    Uri uri = Uri.parse(url);
                    if (DeepLinkHandler.handleDeepLink(context, new android.content.Intent(android.content.Intent.ACTION_VIEW, uri))) {
                        return true;
                    }
                    
                    // If DeepLinkHandler couldn't handle it, let the WebView handle it
                    return false;
                } else {
                    // For external URLs, show a chooser dialog
                    LinkChooserUtil.openUrlWithChooser(context, url, "Open with");
                    return true;
                }
            }
        });
    }
} 