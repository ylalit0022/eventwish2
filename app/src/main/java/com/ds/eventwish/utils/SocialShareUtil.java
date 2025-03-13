package com.ds.eventwish.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.JsonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for handling social media sharing with preview images
 */
public class SocialShareUtil {
    private static final String TAG = "SocialShareUtil";
    private static final int TIMEOUT_MS = 10000; // 10 seconds timeout
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB max size
    private static final String BACKEND_URL = "https://eventwish.herokuapp.com"; // Update with your backend URL

    /**
     * Create a sharing intent with preview image
     * @param context The context
     * @param title The title of the content
     * @param description The description of the content
     * @param previewUrl The URL of the preview image
     * @param deepLink The deep link URL
     * @return The sharing intent
     */
    public static Intent createSharingIntent(Context context, String title, String description, String previewUrl, String deepLink) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        // Set the sharing text
        String shareText = String.format("%s\n\n%s\n\n%s\n\n%s",
            title,
            description,
            deepLink,
            context.getString(R.string.share_app_prompt)
        );
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        // Add preview image if available
        if (previewUrl != null && !previewUrl.isEmpty()) {
            try {
                // Use backend to fetch the image
                String backendImageUrl = String.format("%s/api/images/fetch?url=%s", 
                    BACKEND_URL, 
                    Uri.encode(previewUrl, "UTF-8"));
                
                // Download the preview image with timeout and size limits
                Bitmap bitmap = downloadImage(backendImageUrl);
                
                if (bitmap != null) {
                    // Save the bitmap to a temporary file
                    File tempFile = saveBitmapToFile(context, bitmap);
                    
                    if (tempFile != null) {
                        // Add the image to the sharing intent
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                        Log.d(TAG, "Successfully added preview image to share intent");
                    } else {
                        Log.w(TAG, "Failed to save preview image to file");
                    }
                    
                    // Recycle the bitmap to free memory
                    bitmap.recycle();
                } else {
                    Log.w(TAG, "Failed to download preview image from URL: " + previewUrl);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding preview image: " + e.getMessage());
                // Fallback to text only sharing
                shareIntent.setType("text/plain");
            }
        }
        
        return shareIntent;
    }
    
    /**
     * Download an image from a URL with timeout and size limits
     * @param imageUrl The URL of the image to download
     * @return The downloaded bitmap, or null if download fails
     */
    private static Bitmap downloadImage(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + responseCode + " for URL: " + imageUrl);
                return null;
            }
            
            // Check content length
            int contentLength = connection.getContentLength();
            if (contentLength > MAX_IMAGE_SIZE) {
                Log.w(TAG, "Image too large: " + contentLength + " bytes");
                return null;
            }
            
            // Read the image data
            inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);
            
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image: " + e.getMessage());
            return null;
        } finally {
            // Clean up resources
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Save a bitmap to a temporary file
     * @param context The context
     * @param bitmap The bitmap to save
     * @return The temporary file, or null if save fails
     */
    private static File saveBitmapToFile(Context context, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            // Create a temporary file
            File tempFile = File.createTempFile("preview", ".png", context.getCacheDir());
            
            // Save the bitmap
            out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file: " + e.getMessage());
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }
    
    /**
     * Create a sharing intent for a shared wish
     * @param context The context
     * @param wish The shared wish
     * @param shareText The text to share
     * @return The sharing intent
     */
    public static Intent createWishSharingIntent(
            @NonNull Context context,
            @NonNull SharedWish wish,
            @NonNull String shareText) {
        
        String title = context.getString(R.string.share_wish_title);
        String description = context.getString(R.string.share_wish_description, 
                wish.getSenderName(), wish.getRecipientName());
        
        return createSharingIntent(
                context,
                title,
                description,
                wish.getPreviewUrl(),
                null);
    }
    
    /**
     * Share content with WhatsApp
     * @param context The context
     * @param text The text to share
     * @param previewUrl The URL of the preview image
     * @return true if the content was shared, false otherwise
     */
    public static boolean shareWithWhatsApp(
            @NonNull Context context,
            @NonNull String text,
            @Nullable String previewUrl) {
        
        try {
            Intent intent = createSharingIntent(
                    context,
                    null,
                    null,
                    previewUrl,
                    null);
            
            intent.setPackage("com.whatsapp");
            
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sharing with WhatsApp", e);
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Share content with Facebook
     * @param context The context
     * @param text The text to share
     * @param previewUrl The URL of the preview image
     * @param title The title of the content
     * @return true if the content was shared, false otherwise
     */
    public static boolean shareWithFacebook(
            @NonNull Context context,
            @NonNull String text,
            @Nullable String previewUrl,
            @Nullable String title) {
        
        try {
            Intent intent = createSharingIntent(
                    context,
                    title,
                    null,
                    previewUrl,
                    null);
            
            intent.setPackage("com.facebook.katana");
            
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sharing with Facebook", e);
            
            // Try Facebook Messenger as fallback
            try {
                Intent messengerIntent = createSharingIntent(
                        context,
                        title,
                        null,
                        previewUrl,
                        null);
                
                messengerIntent.setPackage("com.facebook.orca");
                
                context.startActivity(messengerIntent);
                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Error sharing with Facebook Messenger", ex);
                Toast.makeText(context, "Facebook apps not installed", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
    
    /**
     * Share content with a chooser dialog
     * @param context The context
     * @param text The text to share
     * @param previewUrl The URL of the preview image
     * @param title The title of the content
     * @param chooserTitle The title for the chooser dialog
     * @return The chooser intent
     */
    public static Intent createChooserIntent(
            @NonNull Context context,
            @NonNull String text,
            @Nullable String previewUrl,
            @Nullable String title,
            @Nullable String chooserTitle) {
        
        Intent intent = createSharingIntent(
                context,
                title,
                null,
                previewUrl,
                null);
        
        String chooserTitleText = chooserTitle != null ? 
                chooserTitle : context.getString(R.string.share_via);
        
        return Intent.createChooser(intent, chooserTitleText);
    }

    public static void shareViaWhatsApp(Context context, String shareText) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via WhatsApp", e);
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            shareViaOther(context, shareText);
        }
    }

    public static void shareViaFacebook(Context context, String shareText) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.facebook.katana");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via Facebook", e);
            Toast.makeText(context, "Facebook not installed", Toast.LENGTH_SHORT).show();
            shareViaOther(context, shareText);
        }
    }

    public static void shareViaTwitter(Context context, String shareText) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.twitter.android");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via Twitter", e);
            // Try to open Twitter web intent as fallback
            try {
                String encodedText = java.net.URLEncoder.encode(shareText, "UTF-8");
                String twitterUrl = "https://twitter.com/intent/tweet?text=" + encodedText;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl));
                context.startActivity(browserIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Error opening Twitter web intent", ex);
                Toast.makeText(context, "Twitter not available", Toast.LENGTH_SHORT).show();
                shareViaOther(context, shareText);
            }
        }
    }

    public static void shareViaInstagram(Context context, String shareText) {
        try {
            // Instagram doesn't support direct text sharing, so we'll use a clipboard + app approach
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("EventWish Text", shareText);
            clipboard.setPrimaryClip(clip);

            // Open Instagram app
            Intent instagramIntent = new Intent(Intent.ACTION_VIEW);
            instagramIntent.setPackage("com.instagram.android");

            // Check if Instagram is installed
            if (instagramIntent.resolveActivity(context.getPackageManager()) != null) {
                // Show toast to inform user about clipboard
                Toast.makeText(context, "Text copied to clipboard. Paste in Instagram.", Toast.LENGTH_LONG).show();
                // Open Instagram
                context.startActivity(instagramIntent);
            } else {
                Toast.makeText(context, "Instagram not installed", Toast.LENGTH_SHORT).show();
                shareViaOther(context, shareText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via Instagram", e);
            Toast.makeText(context, "Error sharing to Instagram", Toast.LENGTH_SHORT).show();
            shareViaOther(context, shareText);
        }
    }

    public static void shareViaEmail(Context context, String shareText) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this EventWish!");
            emailIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(emailIntent);
            } else {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show();
                shareViaOther(context, shareText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via Email", e);
            Toast.makeText(context, "Error sharing via email", Toast.LENGTH_SHORT).show();
            shareViaOther(context, shareText);
        }
    }

    public static void shareViaSms(Context context, String shareText) {
        try {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:"));
            smsIntent.putExtra("sms_body", shareText);

            if (smsIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(smsIntent);
            } else {
                Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show();
                shareViaOther(context, shareText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via SMS", e);
            Toast.makeText(context, "Error sharing via SMS", Toast.LENGTH_SHORT).show();
            shareViaOther(context, shareText);
        }
    }

    public static void shareViaOther(Context context, String shareText) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            Intent chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_via));
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error creating chooser intent", e);
            Toast.makeText(context, "Error sharing content", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a sharing intent for a wish using the server-side approach
     * @param context The context
     * @param wish The wish to share
     * @param templateId The template ID
     * @param title The title of the wish
     * @param description The description of the wish
     * @param senderName The sender's name
     * @param recipientName The recipient's name
     * @return The sharing intent
     */
    public static void shareWishViaServer(Context context, SharedWish wish, String templateId, 
                                         String title, String description, 
                                         String senderName, String recipientName,
                                         String platform, ShareCallback callback) {
        // Build the API URL
        String baseUrl = context.getString(R.string.base_url);
        String apiUrl = baseUrl + "wishes/share/" + templateId + 
                "?title=" + Uri.encode(title) + 
                "&description=" + Uri.encode(description) + 
                "&senderName=" + Uri.encode(senderName) + 
                "&recipientName=" + Uri.encode(recipientName);
        
        // Make the API call on a background thread
        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Parse the response
                    String response = NetworkUtils.readStream(connection.getInputStream());
                    final ShareableContent content = JsonUtils.fromJson(response, ShareableContent.class);
                    
                    // Update the wish with the shareable data
                    if (wish != null && content != null) {
                        wish.setTitle(content.getTitle());
                        wish.setDescription(content.getDescription());
                        wish.setDeepLink(content.getDeepLink());
                        wish.setSharedVia(platform);
                    }
                    
                    // Create the sharing intent on the main thread
                    if (content != null) {
                        context.getMainExecutor().execute(() -> {
                            Intent intent = createSharingIntent(
                                    context,
                                    content.getTitle(),
                                    content.getDescription(),
                                    content.getPreviewUrl(),
                                    content.getDeepLink()
                            );
                            
                            if (callback != null) {
                                callback.onShareReady(intent, content.getShareableUrl(), platform);
                            }
                        });
                    } else {
                        handleError(context, "Failed to parse server response", callback);
                    }
                } else {
                    handleError(context, "Server returned error code: " + responseCode, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sharing wish via server", e);
                handleError(context, "Error: " + e.getMessage(), callback);
            }
        }).start();
    }

    /**
     * Handle errors in the sharing process
     */
    private static void handleError(Context context, String errorMessage, ShareCallback callback) {
        Log.e(TAG, errorMessage);
        context.getMainExecutor().execute(() -> {
            Toast.makeText(context, "Failed to create shareable content: " + errorMessage, Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onShareError(errorMessage);
            }
        });
    }

    /**
     * Callback interface for share operations
     */
    public interface ShareCallback {
        void onShareReady(Intent shareIntent, String shareableUrl, String platform);
        void onShareError(String errorMessage);
    }

    /**
     * Model class for shareable content from the server
     */
    public static class ShareableContent {
        private boolean success;
        private String shareableUrl;
        private String shortCode;
        private String previewUrl;
        private String title;
        private String description;
        private String deepLink;
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getShareableUrl() {
            return shareableUrl;
        }
        
        public String getShortCode() {
            return shortCode;
        }
        
        public String getPreviewUrl() {
            return previewUrl;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getDeepLink() {
            return deepLink;
        }
    }
} 