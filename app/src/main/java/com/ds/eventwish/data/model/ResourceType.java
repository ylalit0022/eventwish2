package com.ds.eventwish.data.model;

/**
 * Enum representing different types of resources in the application.
 * Used for caching and resource management.
 */
public enum ResourceType {
    /**
     * Template resource type
     */
    TEMPLATE("template"),
    
    /**
     * Category resource type
     */
    CATEGORY("category"),
    
    /**
     * Icon resource type
     */
    ICON("icon"),
    
    /**
     * Category icon resource type
     */
    CATEGORY_ICON("category_icon"),
    
    /**
     * User resource type
     */
    USER("user"),
    
    /**
     * Event resource type
     */
    EVENT("event"),
    
    /**
     * Settings resource type
     */
    SETTINGS("settings"),
    
    /**
     * Notification resource type
     */
    NOTIFICATION("notification"),
    
    /**
     * Generic resource type
     */
    GENERIC("generic");
    
    private final String key;
    
    ResourceType(String key) {
        this.key = key;
    }
    
    /**
     * Get the key for this resource type
     * @return Key string
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Get the cache prefix for this resource type
     * @return Cache prefix
     */
    public String getCachePrefix() {
        return key + "_";
    }
    
    /**
     * Get the API path for this resource type
     * @return API path
     */
    public String getApiPath() {
        return key + "s"; // Pluralize for API paths
    }
    
    /**
     * Get the resource type from a key
     * @param key Key to look up
     * @return ResourceType or GENERIC if not found
     */
    public static ResourceType fromKey(String key) {
        for (ResourceType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return GENERIC;
    }
    
    /**
     * Get the resource type from a string
     * @param typeString String representation of the resource type
     * @return ResourceType or GENERIC if not found
     */
    public static ResourceType fromString(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return GENERIC;
        }
        
        try {
            return valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If not a direct enum name, try by key
            return fromKey(typeString);
        }
    }
} 