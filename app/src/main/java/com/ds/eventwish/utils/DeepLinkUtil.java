package com.ds.eventwish.utils;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.ds.eventwish.R;

public class DeepLinkUtil {
    private static final String TAG = "DeepLinkUtil";
    
    // Web URLs
    private static final String WEB_URL_BASE = "https://eventwishes.onrender.com";
    private static final String WEB_WISH_PATH = "/wish/";
    private static final String WEB_FESTIVAL_PATH = "/festival/";
    private static final String WEB_TEMPLATE_PATH = "/template/";
    
    // App URLs
    private static final String APP_SCHEME = "eventwish";
    private static final String APP_WISH_HOST = "wish";
    private static final String APP_FESTIVAL_HOST = "festival";
    private static final String APP_TEMPLATE_HOST = "template";
    
    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.ds.eventwish";

    /**
     * Extract the short code from a wish URI
     * @param uri The URI to extract from
     * @return The short code, or null if not found
     */
    public static String extractShortCode(Uri uri) {
        if (uri == null) {
            Log.d(TAG, "extractShortCode: Uri is null");
            return null;
        }
        
        Log.d(TAG, "extractShortCode: Processing URI: " + uri.toString());
        Log.d(TAG, "extractShortCode: Scheme=" + uri.getScheme() + ", Host=" + uri.getHost() + ", Path=" + uri.getPath());
        
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            Log.d(TAG, "extractShortCode: Checking for custom scheme");
            // Check if it's a custom scheme URI
            if (APP_SCHEME.equals(uri.getScheme()) && APP_WISH_HOST.equals(uri.getHost())) {
                String lastSegment = uri.getLastPathSegment();
                Log.d(TAG, "extractShortCode: Custom scheme detected, lastSegment=" + lastSegment);
                if (lastSegment != null && !lastSegment.isEmpty()) {
                    return lastSegment;
                }
            }
            Log.e(TAG, "extractShortCode: No valid path or custom scheme found");
            return null;
        }
        
        // Remove leading and trailing slashes
        path = path.replaceAll("^/+|/+$", "");
        Log.d(TAG, "extractShortCode: Cleaned path=" + path);
        
        // Split path and get the last segment
        String[] segments = path.split("/");
        if (segments.length > 0) {
            String shortCode = segments[segments.length - 1];
            Log.d(TAG, "extractShortCode: Extracted shortCode=" + shortCode);
            return shortCode;
        }
        
        Log.e(TAG, "extractShortCode: No segments found in path");
        return null;
    }

    /**
     * Create a shareable URL for a wish
     * @param shortCode The wish short code
     * @return The shareable URL
     */
    public static String createShareUrl(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) return null;
        return generateWebWishUrl(shortCode);
    }

    /**
     * Generate a web URL for a wish
     * @param shortCode The wish short code
     * @return The web URL
     */
    public static String generateWebWishUrl(String shortCode) {
        return WEB_URL_BASE + WEB_WISH_PATH + shortCode;
    }

    /**
     * Generate an app URL for a wish
     * @param shortCode The wish short code
     * @return The app URL
     */
    public static String generateAppWishUrl(String shortCode) {
        return APP_SCHEME + "://" + APP_WISH_HOST + "/" + shortCode;
    }
    
    /**
     * Generate a web URL for a festival
     * @param festivalId The festival ID
     * @return The web URL
     */
    public static String generateWebFestivalUrl(String festivalId) {
        return WEB_URL_BASE + WEB_FESTIVAL_PATH + festivalId;
    }
    
    /**
     * Generate an app URL for a festival
     * @param festivalId The festival ID
     * @return The app URL
     */
    public static String generateAppFestivalUrl(String festivalId) {
        return APP_SCHEME + "://" + APP_FESTIVAL_HOST + "/" + festivalId;
    }
    
    /**
     * Generate a web URL for a template
     * @param templateId The template ID
     * @return The web URL
     */
    public static String generateWebTemplateUrl(String templateId) {
        return WEB_URL_BASE + WEB_TEMPLATE_PATH + templateId;
    }
    
    /**
     * Generate an app URL for a template
     * @param templateId The template ID
     * @return The app URL
     */
    public static String generateAppTemplateUrl(String templateId) {
        return APP_SCHEME + "://" + APP_TEMPLATE_HOST + "/" + templateId;
    }

    /**
     * Share a wish via the system share dialog
     * @param context The context
     * @param shortCode The wish short code
     * @param message The message to share
     */
    public static void shareWish(Context context, String shortCode, String message) {
        String webUrl = generateWebWishUrl(shortCode);
        String shareText = message + "\n\n" + webUrl + "\n\nDownload EventWish to create your own special wishes!";

        // Use LinkChooserUtil to ensure a chooser dialog is shown
        LinkChooserUtil.shareWithChooser(context, shareText, "Share Wish", context.getString(R.string.share_via));
    }
    
    /**
     * Share a festival via the system share dialog
     * @param context The context
     * @param festivalId The festival ID
     * @param message The message to share
     */
    public static void shareFestival(Context context, String festivalId, String message) {
        String webUrl = generateWebFestivalUrl(festivalId);
        String shareText = message + "\n\n" + webUrl + "\n\nDownload EventWish to explore more festivals!";

        // Use LinkChooserUtil to ensure a chooser dialog is shown
        LinkChooserUtil.shareWithChooser(context, shareText, "Share Festival", context.getString(R.string.share_via));
    }
    
    /**
     * Share a template via the system share dialog
     * @param context The context
     * @param templateId The template ID
     * @param message The message to share
     */
    public static void shareTemplate(Context context, String templateId, String message) {
        String webUrl = generateWebTemplateUrl(templateId);
        String shareText = message + "\n\n" + webUrl + "\n\nDownload EventWish to use this template!";

        // Use LinkChooserUtil to ensure a chooser dialog is shown
        LinkChooserUtil.shareWithChooser(context, shareText, "Share Template", context.getString(R.string.share_via));
    }

    /**
     * Share a wish via WhatsApp
     * @param context The context
     * @param shortCode The wish short code
     * @param message The message to share
     */
    public static void shareToWhatsApp(Context context, String shortCode, String message) {
        String webUrl = generateWebWishUrl(shortCode);
        String shareText = message + "\n\n" + webUrl;

        // Try to share directly to WhatsApp
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            // Check if WhatsApp is installed
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            
            // WhatsApp is installed, open it directly
            context.startActivity(whatsappIntent);
        } catch (PackageManager.NameNotFoundException e) {
            // WhatsApp not installed, show chooser instead
            Log.d(TAG, "WhatsApp not installed, showing chooser dialog");
            Toast.makeText(context, "WhatsApp not installed, showing other options", Toast.LENGTH_SHORT).show();
            
            // Use LinkChooserUtil to show a chooser dialog
            LinkChooserUtil.shareWithChooser(context, shareText, null, context.getString(R.string.share_via));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing to WhatsApp", e);
            Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
            
            // Fallback to general share
            LinkChooserUtil.shareWithChooser(context, shareText, null, context.getString(R.string.share_via));
        }
    }

    /**
     * Copy a wish link to the clipboard
     * @param context The context
     * @param shortCode The wish short code
     */
    public static void copyLinkToClipboard(Context context, String shortCode) {
        String webUrl = generateWebWishUrl(shortCode);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Wish Link", webUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Copy a festival link to the clipboard
     * @param context The context
     * @param festivalId The festival ID
     */
    public static void copyFestivalLinkToClipboard(Context context, String festivalId) {
        String webUrl = generateWebFestivalUrl(festivalId);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Festival Link", webUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Copy a template link to the clipboard
     * @param context The context
     * @param templateId The template ID
     */
    public static void copyTemplateLinkToClipboard(Context context, String templateId) {
        String webUrl = generateWebTemplateUrl(templateId);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Template Link", webUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle a deep link
     * @param context The context
     * @param uri The URI to handle
     * @return true if the deep link was handled, false otherwise
     * @deprecated Use {@link DeepLinkHandler#handleDeepLink(Context, Intent)} instead
     */
    @Deprecated
    public static boolean handleDeepLink(Context context, Uri uri) {
        Log.d(TAG, "handleDeepLink: This method is deprecated. Use DeepLinkHandler.handleDeepLink() instead.");
        
        // Create an intent with the URI and let DeepLinkHandler handle it
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        return DeepLinkHandler.handleDeepLink(context, intent);
    }

    /**
     * Check if an app is installed
     * @param context The context
     * @param packageName The package name to check
     * @return true if the app is installed, false otherwise
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Open the Play Store to the EventWish app page
     * @param context The context
     */
    public static void openPlayStore(Context context) {
        try {
            // Try to open Play Store app first
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ds.eventwish"));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(marketIntent);
        } catch (ActivityNotFoundException e) {
            // Fallback to browser if Play Store app is not installed
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Error opening Play Store in browser", ex);
                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Track a deep link open event
     * @param context The context
     * @param shortCode The short code or ID
     * @param source The source of the deep link
     */
    public static void trackDeepLinkOpen(Context context, String shortCode, String source) {
        // TODO: Implement analytics tracking
        Log.d(TAG, "Deep link opened - shortCode: " + shortCode + ", source: " + source);
    }
}
