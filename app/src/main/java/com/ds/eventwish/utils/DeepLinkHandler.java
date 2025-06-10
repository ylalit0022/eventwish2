package com.ds.eventwish.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.navigation.NavController;
import com.ds.eventwish.R;

public class DeepLinkHandler {
    
    private static final String TAG = "DeepLinkHandler";
    
    public static boolean handleDeepLink(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            Log.d(TAG, "No deep link data in intent");
            
            // Check if intent has extras for festival notification
            if (intent.hasExtra("open_fragment") && 
                "festival_notification".equals(intent.getStringExtra("open_fragment"))) {
                Log.d(TAG, "Found festival_notification extra in intent");
                return processDeepLink(context, Uri.parse("eventwish://open/festival_notification"), null);
            }
            
            return false;
        }
        
        Uri deepLinkUri = intent.getData();
        return processDeepLink(context, deepLinkUri, null);
    }
    
    public static boolean handleDeepLink(Context context, Uri uri, NavController navController) {
        if (uri == null) {
            Log.d(TAG, "No deep link URI provided");
            return false;
        }
        
        return processDeepLink(context, uri, navController);
    }
    
    private static boolean processDeepLink(Context context, Uri uri, NavController navController) {
        Log.d(TAG, "Processing deep link: " + uri.toString());
        
        String host = uri.getHost();
        String path = uri.getPath();
        
        if (host == null || path == null) {
            Log.d(TAG, "Invalid deep link format");
            return false;
        }
        
        // Log the deep link details
        Log.d(TAG, "Deep link details - Scheme: " + uri.getScheme() + ", Host: " + host + ", Path: " + path);
        
        // Handle different paths
        if (navController != null) {
            // Handle festival notification deep link
            if (host.equals("open") && path.equals("/festival_notification")) {
                Log.d(TAG, "Navigating to FestivalNotificationFragment");
                navController.navigate(R.id.navigation_festival_notification);
                return true;
            }
            
            // Handle wish links - route to ResourceFragment
            if (host.equals("wish") || path.startsWith("/wish/")) {
                String shortCode;
                
                // Extract short code from the path
                if (path.startsWith("/wish/")) {
                    shortCode = path.substring("/wish/".length());
                    Log.d(TAG, "Extracted shortCode from path: " + shortCode);
                } else if (path.isEmpty() && host.equals("wish")) {
                    // If the path is empty but host is "wish", use the last path segment or query parameter
                    shortCode = uri.getLastPathSegment();
                    Log.d(TAG, "No path but host is 'wish', using last segment: " + shortCode);
                    
                    // If shortCode is still null, try to get from query parameters
                    if (shortCode == null || shortCode.isEmpty()) {
                        shortCode = uri.getQueryParameter("code");
                        Log.d(TAG, "Trying to get shortCode from query parameter 'code': " + shortCode);
                    }
                } else {
                    // If host is "wish", then the path might not start with "/wish/"
                    shortCode = path.startsWith("/") ? path.substring(1) : path;
                    Log.d(TAG, "Using entire path as shortCode: " + shortCode);
                }
                
                if (shortCode != null && !shortCode.isEmpty()) {
                    Log.d(TAG, "Navigating to ResourceFragment with shortCode: " + shortCode);
                    
                    // Create bundle with short code
                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("shortCode", shortCode);
                    
                    // Navigate to ResourceFragment
                    navController.navigate(R.id.resourceFragment, args);
                    return true;
                } else {
                    Log.e(TAG, "Failed to extract valid shortCode from URI: " + uri);
                }
            }
            // Handle template paths
            else if (path.startsWith("/template/")) {
                Log.d(TAG, "Navigating to template");
                // navController.navigate(R.id.action_to_templateFragment);
                return true;
            } else if (path.startsWith("/event/")) {
                Log.d(TAG, "Navigating to event");
                // navController.navigate(R.id.action_to_eventFragment);
                return true;
            }
        }
        
        // Return true if we handled the deep link, false otherwise
        return path.startsWith("/template/") || path.startsWith("/event/") || path.startsWith("/wish/") || 
               host.equals("wish") || (host.equals("open") && path.equals("/festival_notification"));
    }
}