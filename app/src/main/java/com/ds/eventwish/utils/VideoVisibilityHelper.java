package com.ds.eventwish.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class to detect video visibility in RecyclerView
 * Calculates visibility percentage and determines if auto-play should trigger
 */
public class VideoVisibilityHelper {
    private static final String TAG = "VideoVisibilityHelper";
    
    // Minimum visibility percentage to trigger auto-play (50%)
    private static final float MIN_VISIBILITY_PERCENTAGE = 0.5f;
    
    // Minimum visible area in pixels to trigger auto-play (for very small videos)
    private static final int MIN_VISIBLE_AREA_PX = 200 * 200; // 200x200 pixels
    
    /**
     * Check if a view is sufficiently visible for video auto-play
     * @param view The view to check visibility for
     * @param recyclerView The parent RecyclerView
     * @return true if the view is visible enough for auto-play
     */
    public static boolean isViewVisibleForAutoPlay(@NonNull View view, @NonNull RecyclerView recyclerView) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        
        try {
            Rect recyclerViewRect = new Rect();
            recyclerView.getGlobalVisibleRect(recyclerViewRect);
            
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect);
            
            // Calculate intersection
            Rect intersection = new Rect();
            boolean hasIntersection = intersection.setIntersect(viewRect, recyclerViewRect);
            
            if (!hasIntersection) {
                return false;
            }
            
            // Calculate visibility percentage
            float visibilityPercentage = calculateVisibilityPercentage(viewRect, intersection);
            
            // Calculate visible area
            int visibleArea = intersection.width() * intersection.height();
            
            // Check both percentage and minimum area thresholds
            boolean isVisibleEnough = visibilityPercentage >= MIN_VISIBILITY_PERCENTAGE || 
                                    visibleArea >= MIN_VISIBLE_AREA_PX;
            
            Log.v(TAG, "View visibility - Percentage: " + String.format("%.2f", visibilityPercentage * 100) + 
                      "%, Area: " + visibleArea + "px, Visible: " + isVisibleEnough);
            
            return isVisibleEnough;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating view visibility", e);
            return false;
        }
    }
    
    /**
     * Get the visibility percentage of a view
     * @param view The view to check
     * @param recyclerView The parent RecyclerView
     * @return Visibility percentage (0.0 to 1.0)
     */
    public static float getVisibilityPercentage(@NonNull View view, @NonNull RecyclerView recyclerView) {
        if (view.getVisibility() != View.VISIBLE) {
            return 0f;
        }
        
        try {
            Rect recyclerViewRect = new Rect();
            recyclerView.getGlobalVisibleRect(recyclerViewRect);
            
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect);
            
            Rect intersection = new Rect();
            boolean hasIntersection = intersection.setIntersect(viewRect, recyclerViewRect);
            
            if (!hasIntersection) {
                return 0f;
            }
            
            return calculateVisibilityPercentage(viewRect, intersection);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating visibility percentage", e);
            return 0f;
        }
    }
    
    /**
     * Find the most visible video template in the RecyclerView
     * @param recyclerView The RecyclerView to search
     * @return The position of the most visible video template, or -1 if none found
     */
    public static int findMostVisibleVideoPosition(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return -1;
        }
        
        int mostVisiblePosition = -1;
        float maxVisibility = 0f;
        
        try {
            int firstVisible = 0;
            int lastVisible = layoutManager.getChildCount() - 1;
            
            for (int i = firstVisible; i <= lastVisible; i++) {
                View child = layoutManager.getChildAt(i);
                if (child == null) continue;
                
                int position = recyclerView.getChildAdapterPosition(child);
                if (position == RecyclerView.NO_POSITION) continue;
                
                // Check if this position contains a video template
                if (isVideoTemplate(recyclerView, position)) {
                    float visibility = getVisibilityPercentage(child, recyclerView);
                    
                    if (visibility > maxVisibility && visibility >= MIN_VISIBILITY_PERCENTAGE) {
                        maxVisibility = visibility;
                        mostVisiblePosition = position;
                    }
                }
            }
            
            Log.v(TAG, "Most visible video position: " + mostVisiblePosition + 
                      " with visibility: " + String.format("%.2f", maxVisibility * 100) + "%");
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding most visible video position", e);
        }
        
        return mostVisiblePosition;
    }
    
    /**
     * Check if the view is completely visible in the RecyclerView
     * @param view The view to check
     * @param recyclerView The parent RecyclerView
     * @return true if the view is completely visible
     */
    public static boolean isViewCompletelyVisible(@NonNull View view, @NonNull RecyclerView recyclerView) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        
        try {
            Rect recyclerViewRect = new Rect();
            recyclerView.getGlobalVisibleRect(recyclerViewRect);
            
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect);
            
            return recyclerViewRect.contains(viewRect);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking complete visibility", e);
            return false;
        }
    }
    
    /**
     * Calculate the visibility percentage based on view and intersection rectangles
     */
    private static float calculateVisibilityPercentage(Rect viewRect, Rect intersection) {
        if (viewRect.isEmpty()) {
            return 0f;
        }
        
        int viewArea = viewRect.width() * viewRect.height();
        int visibleArea = intersection.width() * intersection.height();
        
        return Math.min(1f, (float) visibleArea / viewArea);
    }
    
    /**
     * Check if the template at the given position is a video template
     * This method should be customized based on your adapter implementation
     */
    private static boolean isVideoTemplate(@NonNull RecyclerView recyclerView, int position) {
        try {
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            if (adapter == null) {
                return false;
            }
            
            // This is a placeholder - you'll need to implement this based on your adapter
            // For now, we'll assume it's implemented in the adapter
            if (adapter instanceof VideoTemplateChecker) {
                return ((VideoTemplateChecker) adapter).isVideoTemplate(position);
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if template is video", e);
            return false;
        }
    }
    
    /**
     * Interface for adapters to implement video template checking
     */
    public interface VideoTemplateChecker {
        boolean isVideoTemplate(int position);
    }
    
    /**
     * Get the center point of a view relative to its parent
     */
    public static android.graphics.Point getViewCenter(@NonNull View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        
        return new android.graphics.Point(
            location[0] + view.getWidth() / 2,
            location[1] + view.getHeight() / 2
        );
    }
    
    /**
     * Check if a view is scrolling (for debouncing auto-play during rapid scrolling)
     */
    public static boolean isRecyclerViewScrolling(@NonNull RecyclerView recyclerView) {
        return recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
    }
    
    /**
     * Get the scroll direction of the RecyclerView
     * @param recyclerView The RecyclerView
     * @param dy The vertical scroll offset from onScrolled
     * @return 1 for down, -1 for up, 0 for no scroll
     */
    public static int getScrollDirection(int dy) {
        if (dy > 0) {
            return 1; // Scrolling down
        } else if (dy < 0) {
            return -1; // Scrolling up
        } else {
            return 0; // No scroll
        }
    }
} 