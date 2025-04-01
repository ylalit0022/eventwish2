package com.ds.eventwish.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Utility class for tracking analytics events
 */
public class AnalyticsUtils {
    private static final String TAG = "AnalyticsUtils";
    
    // Event keys
    public static final String EVENT_TEMPLATE_VIEW = "template_view";
    public static final String EVENT_SHARED_WISH_VIEW = "shared_wish_view";
    public static final String EVENT_VIEWER_ACTIVE = "viewer_active";
    public static final String EVENT_VIEWER_INACTIVE = "viewer_inactive";
    
    // Param keys
    public static final String PARAM_TEMPLATE_ID = "template_id";
    public static final String PARAM_SHORT_CODE = "short_code";
    public static final String PARAM_SENDER_NAME = "sender_name";
    public static final String PARAM_RECIPIENT_NAME = "recipient_name";
    public static final String PARAM_SESSION_ID = "session_id";
    public static final String PARAM_VIEW_DURATION = "view_duration";
    
    private static String sessionId;
    
    /**
     * Initialize the analytics system
     * @param context Application context
     */
    public static void init(Context context) {
        // Generate a unique session ID
        sessionId = generateSessionId();
        Log.d(TAG, "Analytics initialized with session ID: " + sessionId);
    }
    
    /**
     * Track template view event
     * @param templateId ID of the viewed template
     */
    public static void trackTemplateView(String templateId) {
        Bundle params = new Bundle();
        params.putString(PARAM_TEMPLATE_ID, templateId);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        Log.d(TAG, "Tracked template view: " + templateId + ", params: " + params.toString());
    }
    
    /**
     * Track shared wish view event
     * @param shortCode Shortcode of the shared wish
     * @param senderName Name of the sender (if available)
     * @param recipientName Name of the recipient (if available)
     */
    public static void trackSharedWishView(String shortCode, String senderName, String recipientName) {
        Bundle params = new Bundle();
        params.putString(PARAM_SHORT_CODE, shortCode);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        if (senderName != null && !senderName.isEmpty()) {
            params.putString(PARAM_SENDER_NAME, senderName);
        }
        
        if (recipientName != null && !recipientName.isEmpty()) {
            params.putString(PARAM_RECIPIENT_NAME, recipientName);
        }
        
        Log.d(TAG, "Tracked shared wish view: " + shortCode + ", params: " + params.toString());
    }
    
    /**
     * Track active viewer
     * @param pageIdentifier An identifier for the page (template ID or short code)
     */
    public static void trackViewerActive(String pageIdentifier) {
        Bundle params = new Bundle();
        params.putString("page_id", pageIdentifier);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        Log.d(TAG, "Tracked viewer active: " + pageIdentifier + ", params: " + params.toString());
    }
    
    /**
     * Track inactive viewer
     * @param pageIdentifier An identifier for the page (template ID or short code)
     * @param durationSeconds Duration in seconds that the viewer was active
     */
    public static void trackViewerInactive(String pageIdentifier, long durationSeconds) {
        Bundle params = new Bundle();
        params.putString("page_id", pageIdentifier);
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_VIEW_DURATION, durationSeconds);
        
        Log.d(TAG, "Tracked viewer inactive: " + pageIdentifier + ", duration: " + durationSeconds + "s, params: " + params.toString());
    }
    
    /**
     * Set user properties for better analytics segmentation
     * @param userId User ID (if available)
     */
    public static void setUserProperties(String userId) {
        if (userId != null && !userId.isEmpty()) {
            Log.d(TAG, "Set user ID: " + userId);
        }
    }
    
    /**
     * Generate a unique session ID
     * @return A unique session ID
     */
    private static String generateSessionId() {
        return String.valueOf(System.currentTimeMillis()) + "-" + 
               Math.round(Math.random() * 100000);
    }
} 