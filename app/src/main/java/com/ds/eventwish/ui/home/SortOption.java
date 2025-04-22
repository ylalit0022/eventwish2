package com.ds.eventwish.ui.home;

/**
 * Enum representing different sort options for templates.
 */
public enum SortOption {
    TRENDING("Trending"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    MOST_USED("Most Used");

    private final String displayName;

    SortOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 