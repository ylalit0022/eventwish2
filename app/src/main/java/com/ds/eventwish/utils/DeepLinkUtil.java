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
    private static final String WEB_URL_BASE = "https://eventwishes.onrender.com/wish/";
    private static final String APP_URL_BASE = "eventwish://wish/";
    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.ds.eventwish";

    public static String extractShortCode(Uri uri) {
        if (uri == null) return null;
        
        String path = uri.getPath();
        if (path == null || path.isEmpty()) return null;
        
        // Remove leading and trailing slashes
        path = path.replaceAll("^/+|/+$", "");
        
        // Split path and get the last segment
        String[] segments = path.split("/");
        if (segments.length > 0) {
            return segments[segments.length - 1];
        }
        
        return null;
    }

    public static String createShareUrl(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) return null;
        return WEB_URL_BASE + shortCode;
    }

    public static String generateWebUrl(String shortCode) {
        return WEB_URL_BASE + shortCode;
    }

    public static String generateAppUrl(String shortCode) {
        return APP_URL_BASE + shortCode;
    }

    public static void shareWish(Context context, String shortCode, String message) {
        String webUrl = generateWebUrl(shortCode);
        String shareText = message + "\n\n" + webUrl + "\n\nDownload EventWish to create your own special wishes!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        try {
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_via)));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing wish", e);
            Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareToWhatsApp(Context context, String shortCode, String message) {
        String webUrl = generateWebUrl(shortCode);
        String shareText = message + "\n\n" + webUrl;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setPackage("com.whatsapp");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "WhatsApp not installed", e);
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error sharing to WhatsApp", e);
            Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
        }
    }

    public static void copyLinkToClipboard(Context context, String shortCode) {
        String webUrl = generateWebUrl(shortCode);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Wish Link", webUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }

    public static boolean handleDeepLink(Context context, Uri uri) {
        if (uri == null) return false;

        String shortCode = extractShortCode(uri);
        if (shortCode == null || shortCode.isEmpty()) return false;

        // Handle the deep link based on your app's navigation
        // You might want to navigate to SharedWishFragment with the shortCode
        return true;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

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

    public static void trackDeepLinkOpen(Context context, String shortCode, String source) {
        // TODO: Implement analytics tracking
        Log.d(TAG, "Deep link opened - shortCode: " + shortCode + ", source: " + source);
    }
}
