package com.ds.eventwish.ui.template;

/**
 * Model class for a template
 */
public class Template {
    private final String id;
    private final String name;
    private final String categoryId;
    private final String imageUrl;
    
    /**
     * Constructor for a template
     *
     * @param id The template ID
     * @param name The template name
     * @param categoryId The category ID this template belongs to
     * @param imageUrl The URL of the template image
     */
    public Template(String id, String name, String categoryId, String imageUrl) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
    }
    
    /**
     * Get the template ID
     *
     * @return The template ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the template name
     *
     * @return The template name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the category ID
     *
     * @return The category ID
     */
    public String getCategoryId() {
        return categoryId;
    }
    
    /**
     * Get the image URL
     *
     * @return The image URL
     */
    public String getImageUrl() {
        return imageUrl;
    }
} 