package com.ds.eventwish.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.util.List;

/**
 * Utility class for handling links and implementing chooser dialogs
 */
public class LinkUtils {
    private static final String TAG = "LinkUtils";
    
    /**
     * Open a URL with a chooser dialog
     * @param context The context
     * @param url The URL to open
     * @param chooserTitle The title for the chooser dialog
     */
    public static void openUrlWithChooser(Context context, String url, String chooserTitle) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }
        
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "URL is null or empty");
            Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate URL
        if (!URLUtil.isValidUrl(url)) {
            Log.e(TAG, "Invalid URL: " + url);
            Toast.makeText(context, "Invalid URL format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create the intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            
            // Check if there are apps that can handle this intent
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            
            if (activities.size() > 0) {
                // Create the chooser
                Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
                
                // Add the FLAG_ACTIVITY_NEW_TASK flag if the context is not an Activity
                if (!(context instanceof android.app.Activity)) {
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                
                // Start the chooser activity
                context.startActivity(chooserIntent);
                Log.d(TAG, "Opened URL with chooser: " + url);
            } else {
                // No apps can handle this intent
                Log.e(TAG, "No apps can handle this URL: " + url);
                Toast.makeText(context, "No apps found to open this link", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL: " + e.getMessage());
            Toast.makeText(context, "Error opening link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open a file with a chooser dialog
     * @param context The context
     * @param uri The URI of the file
     * @param mimeType The MIME type of the file
     * @param chooserTitle The title for the chooser dialog
     */
    public static void openFileWithChooser(Context context, Uri uri, String mimeType, String chooserTitle) {
        if (context == null || uri == null) {
            Log.e(TAG, "Context or URI is null");
            return;
        }
        
        try {
            // Create the intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Check if there are apps that can handle this intent
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            
            if (activities.size() > 0) {
                // Create the chooser
                Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
                
                // Add the FLAG_ACTIVITY_NEW_TASK flag if the context is not an Activity
                if (!(context instanceof android.app.Activity)) {
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                
                // Start the chooser activity
                context.startActivity(chooserIntent);
                Log.d(TAG, "Opened file with chooser: " + uri);
            } else {
                // No apps can handle this intent
                Log.e(TAG, "No apps can handle this file: " + uri);
                Toast.makeText(context, "No apps found to open this file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage());
            Toast.makeText(context, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Share content with a chooser dialog
     * @param context The context
     * @param subject The subject of the content
     * @param text The text content to share
     * @param chooserTitle The title for the chooser dialog
     */
    public static void shareWithChooser(Context context, String subject, String text, String chooserTitle) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }
        
        if (text == null || text.isEmpty()) {
            Log.e(TAG, "Text is null or empty");
            Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create the intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            
            if (subject != null && !subject.isEmpty()) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            
            intent.putExtra(Intent.EXTRA_TEXT, text);
            
            // Create the chooser
            Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
            
            // Add the FLAG_ACTIVITY_NEW_TASK flag if the context is not an Activity
            if (!(context instanceof android.app.Activity)) {
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            
            // Start the chooser activity
            context.startActivity(chooserIntent);
            Log.d(TAG, "Shared content with chooser");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing content: " + e.getMessage());
            Toast.makeText(context, "Error sharing content: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check if a URL is from EventWish
     * @param url The URL to check
     * @return true if the URL is from EventWish, false otherwise
     */
    public static boolean isEventWishUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return url.contains("eventwishes.onrender.com") || url.startsWith("eventwish://");
    }
} 