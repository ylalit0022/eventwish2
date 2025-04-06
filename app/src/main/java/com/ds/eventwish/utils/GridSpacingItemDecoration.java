package com.ds.eventwish.utils;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Item decoration for RecyclerView grids
 * Adds equal spacing between items in a grid layout
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    
    private final int spanCount;
    private final int spacing;
    private final boolean includeEdge;
    
    /**
     * Constructor for grid spacing decoration
     *
     * @param spanCount Number of columns in the grid
     * @param spacing Spacing between items in pixels
     * @param includeEdge Whether to include edge spacing
     */
    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.includeEdge = includeEdge;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        int column = position % spanCount; // item column
        
        if (includeEdge) {
            // Add spacing to all edges for consistent look
            outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)
            
            if (position < spanCount) { // top edge
                outRect.top = spacing;
            }
            outRect.bottom = spacing; // item bottom
        } else {
            // Add spacing between items but not on the outer edges
            outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
            outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            
            if (position >= spanCount) {
                outRect.top = spacing; // item top
            }
        }
    }
} 