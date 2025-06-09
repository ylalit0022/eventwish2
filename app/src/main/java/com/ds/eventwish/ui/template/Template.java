package com.ds.eventwish.ui.template;

import androidx.annotation.NonNull;
import java.util.Objects;

/**
 * Model class for a template
 */
public class Template {
    private final String id;
    private final String name;
    private final String categoryId;
    private final String imageUrl;
    private boolean isLiked;
    private boolean isFavorited;
    private long likeCount;
    
    /**
     * Constructor for a template
     */
    public Template(String id, String name, String categoryId, String imageUrl) {
        this(id, name, categoryId, imageUrl, false, false, 0L);
    }
    
    /**
     * Constructor for a template with interaction state
     *
     * @param id The template ID
     * @param name The template name
     * @param categoryId The category ID this template belongs to
     * @param imageUrl The URL of the template image
     * @param isLiked Whether the template is liked by the user
     * @param isFavorited Whether the template is favorited by the user
     * @param likeCount The number of likes for this template
     */
    public Template(String id, String name, String categoryId, String imageUrl, 
                   boolean isLiked, boolean isFavorited, long likeCount) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.isLiked = isLiked;
        this.isFavorited = isFavorited;
        this.likeCount = likeCount;
    }
    
    /**
     * Get the template ID
     *
     * @return The template ID
     */
    @NonNull
    public String getId() {
        return id;
    }
    
    /**
     * Get the template name
     *
     * @return The template name
     */
    @NonNull
    public String getName() {
        return name;
    }
    
    /**
     * Get the category ID
     *
     * @return The category ID
     */
    @NonNull
    public String getCategoryId() {
        return categoryId;
    }
    
    /**
     * Get the image URL
     *
     * @return The image URL
     */
    @NonNull
    public String getImageUrl() {
        return imageUrl;
    }
    
    /**
     * Check if the template is liked by the user
     *
     * @return true if liked, false otherwise
     */
    public boolean isLiked() {
        return isLiked;
    }
    
    /**
     * Set whether the template is liked by the user
     *
     * @param liked true if liked, false otherwise
     */
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    /**
     * Check if the template is favorited by the user
     *
     * @return true if favorited, false otherwise
     */
    public boolean isFavorited() {
        return isFavorited;
    }
    
    /**
     * Set whether the template is favorited by the user
     *
     * @param favorited true if favorited, false otherwise
     */
    public void setFavorited(boolean favorited) {
        isFavorited = favorited;
    }
    
    /**
     * Get the number of likes for this template
     *
     * @return The like count
     */
    public long getLikeCount() {
        return likeCount;
    }
    
    /**
     * Set the number of likes for this template
     *
     * @param count The new like count
     */
    public void setLikeCount(long count) {
        this.likeCount = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        return Objects.equals(id, template.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 