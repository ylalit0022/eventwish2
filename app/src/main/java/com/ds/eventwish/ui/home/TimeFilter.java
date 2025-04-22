package com.ds.eventwish.ui.home;

/**
 * Enum representing different time filter options for templates.
 */
public enum TimeFilter {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year");

    private final String displayName;

    TimeFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 