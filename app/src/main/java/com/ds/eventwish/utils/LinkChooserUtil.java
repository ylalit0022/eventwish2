package com.ds.eventwish.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.R;

import java.io.File;
import java.util.List;

/**
 * Utility class for handling external links with a chooser dialog
 */
public class LinkChooserUtil {
    private static final String TAG = "LinkChooserUtil";

    /**
     * Open a URL with a chooser dialog
     * @param context The context
     * @param url The URL to open
     * @param chooserTitle The title for the chooser dialog
     * @return true if the URL was opened, false otherwise
     */
    public static boolean openUrlWithChooser(@NonNull Context context, @NonNull String url, @Nullable String chooserTitle) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "Cannot open empty URL");
            return false;
        }

        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Log.d(TAG, "Opening URL with chooser: " + url);
        
        // Create intent to view URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        
        // Check if there's an app that can handle this intent
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        
        if (activities.size() > 0) {
            // Create chooser
            String title = chooserTitle != null ? chooserTitle : context.getString(R.string.open_with);
            Intent chooser = Intent.createChooser(intent, title);
            
            // Add flags to open in new task
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(chooser);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found to handle URL", e);
                Toast.makeText(context, context.getString(R.string.no_app_to_handle_url), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Log.e(TAG, "No activities found to handle URL");
            Toast.makeText(context, context.getString(R.string.no_app_to_handle_url), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Open a file with a chooser dialog
     * @param context The context
     * @param file The file to open
     * @param mimeType The MIME type of the file
     * @param chooserTitle The title for the chooser dialog
     * @return true if the file was opened, false otherwise
     */
    public static boolean openFileWithChooser(@NonNull Context context, @NonNull File file, @NonNull String mimeType, @Nullable String chooserTitle) {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
            return false;
        }

        Log.d(TAG, "Opening file with chooser: " + file.getAbsolutePath());
        
        // Create intent to view file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(file);
        
        // For Android N and above, use FileProvider
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        
        intent.setDataAndType(uri, mimeType);
        
        // Check if there's an app that can handle this intent
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        
        if (activities.size() > 0) {
            // Create chooser
            String title = chooserTitle != null ? chooserTitle : context.getString(R.string.open_with);
            Intent chooser = Intent.createChooser(intent, title);
            
            // Add flags to open in new task
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(chooser);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found to handle file", e);
                Toast.makeText(context, context.getString(R.string.no_app_to_handle_file), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Log.e(TAG, "No activities found to handle file");
            Toast.makeText(context, context.getString(R.string.no_app_to_handle_file), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Share content with a chooser dialog
     * @param context The context
     * @param text The text to share
     * @param subject The subject for the share (optional)
     * @param chooserTitle The title for the chooser dialog
     * @return true if the content was shared, false otherwise
     */
    public static boolean shareWithChooser(@NonNull Context context, @NonNull String text, @Nullable String subject, @Nullable String chooserTitle) {
        if (text == null || text.isEmpty()) {
            Log.e(TAG, "Cannot share empty text");
            return false;
        }

        Log.d(TAG, "Sharing text with chooser");
        
        // Create intent to share text
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        
        if (subject != null && !subject.isEmpty()) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        
        // Create chooser
        String title = chooserTitle != null ? chooserTitle : context.getString(R.string.share_via);
        Intent chooser = Intent.createChooser(intent, title);
        
        try {
            context.startActivity(chooser);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found to handle share", e);
            Toast.makeText(context, context.getString(R.string.no_app_to_handle_share), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Check if a URL is an EventWish URL
     * @param url The URL to check
     * @return true if the URL is an EventWish URL, false otherwise
     */
    public static boolean isEventWishUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return url.contains("eventwishes.onrender.com") || 
               url.startsWith("eventwish://");
    }
} 