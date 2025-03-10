package com.ds.eventwish.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Utility class for handling deep links
 */
public class DeepLinkHandler {
    private static final String TAG = "DeepLinkHandler";
    
    /**
     * Handle a deep link
     * @param context The context
     * @param intent The intent containing the deep link
     * @return true if the deep link was handled, false otherwise
     */
    public static boolean handleDeepLink(Context context, Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        
        // Get the URI from the intent
        Uri uri = intent.getData();
        if (uri == null) {
            return false;
        }
        
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        
        Log.d(TAG, "Handling deep link: " + uri);
        Log.d(TAG, "Scheme: " + scheme + ", Host: " + host + ", Path: " + path);
        
        // Handle EventWish deep links
        if ("eventwish".equals(scheme)) {
            return handleEventWishScheme(context, uri);
        }
        
        // Handle HTTP/HTTPS links to EventWish
        if (("http".equals(scheme) || "https".equals(scheme)) && 
                "eventwishes.onrender.com".equals(host)) {
            return handleEventWishWebLink(context, uri);
        }
        
        // Not an EventWish deep link
        return false;
    }
    
    /**
     * Handle an EventWish scheme deep link
     * @param context The context
     * @param uri The URI to handle
     * @return true if the deep link was handled, false otherwise
     */
    private static boolean handleEventWishScheme(Context context, Uri uri) {
        String host = uri.getHost();
        
        if ("wish".equals(host)) {
            // Handle wish links (e.g., eventwish://wish/abc123)
            String shortCode = uri.getLastPathSegment();
            if (shortCode != null && !shortCode.isEmpty()) {
                return openWishDetail(context, shortCode);
            }
        } else if ("festival".equals(host)) {
            // Handle festival links (e.g., eventwish://festival/abc123)
            String festivalId = uri.getLastPathSegment();
            if (festivalId != null && !festivalId.isEmpty()) {
                return openFestivalDetail(context, festivalId);
            }
        } else if ("template".equals(host)) {
            // Handle template links (e.g., eventwish://template/abc123)
            String templateId = uri.getLastPathSegment();
            if (templateId != null && !templateId.isEmpty()) {
                return openTemplateDetail(context, templateId);
            }
        }
        
        return false;
    }
    
    /**
     * Handle an EventWish web link
     * @param context The context
     * @param uri The URI to handle
     * @return true if the deep link was handled, false otherwise
     */
    private static boolean handleEventWishWebLink(Context context, Uri uri) {
        String path = uri.getPath();
        
        if (path == null) {
            return false;
        }
        
        if (path.startsWith("/wish/")) {
            // Handle wish links (e.g., https://eventwishes.onrender.com/wish/abc123)
            String shortCode = path.substring("/wish/".length());
            if (!shortCode.isEmpty()) {
                return openWishDetail(context, shortCode);
            }
        } else if (path.startsWith("/festival/")) {
            // Handle festival links (e.g., https://eventwishes.onrender.com/festival/abc123)
            String festivalId = path.substring("/festival/".length());
            if (!festivalId.isEmpty()) {
                return openFestivalDetail(context, festivalId);
            }
        } else if (path.startsWith("/template/")) {
            // Handle template links (e.g., https://eventwishes.onrender.com/template/abc123)
            String templateId = path.substring("/template/".length());
            if (!templateId.isEmpty()) {
                return openTemplateDetail(context, templateId);
            }
        }
        
        return false;
    }
    
    /**
     * Open the wish detail screen
     * @param context The context
     * @param shortCode The wish short code
     * @return true if the wish detail screen was opened, false otherwise
     */
    private static boolean openWishDetail(Context context, String shortCode) {
        Log.d(TAG, "Opening wish detail for short code: " + shortCode);
        
        // Create an intent to open the MainActivity with the wish short code
        Intent intent = new Intent(context, com.ds.eventwish.MainActivity.class);
        intent.putExtra("SHORT_CODE", shortCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Start the activity
        context.startActivity(intent);
        
        return true;
    }
    
    /**
     * Open the festival detail screen
     * @param context The context
     * @param festivalId The festival ID
     * @return true if the festival detail screen was opened, false otherwise
     */
    private static boolean openFestivalDetail(Context context, String festivalId) {
        Log.d(TAG, "Opening festival detail for ID: " + festivalId);
        
        // Create an intent to open the MainActivity with the festival ID
        Intent intent = new Intent(context, com.ds.eventwish.MainActivity.class);
        intent.putExtra("FESTIVAL_ID", festivalId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Start the activity
        context.startActivity(intent);
        
        return true;
    }
    
    /**
     * Open the template detail screen
     * @param context The context
     * @param templateId The template ID
     * @return true if the template detail screen was opened, false otherwise
     */
    private static boolean openTemplateDetail(Context context, String templateId) {
        Log.d(TAG, "Opening template detail for ID: " + templateId);
        
        // Create an intent to open the MainActivity with the template ID
        Intent intent = new Intent(context, com.ds.eventwish.MainActivity.class);
        intent.putExtra("TEMPLATE_ID", templateId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Start the activity
        context.startActivity(intent);
        
        return true;
    }
} 