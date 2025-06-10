package com.ds.eventwish.ui.template;

import androidx.annotation.NonNull;
import com.ds.eventwish.utils.NumberFormatter;
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
    private long favoriteCount;
    
    /**
     * Constructor for a template
     */
    public Template(String id, String name, String categoryId, String imageUrl) {
        this(id, name, categoryId, imageUrl, false, false, 0L, 0L);
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
        this(id, name, categoryId, imageUrl, isLiked, isFavorited, likeCount, 0L);
    }
    
    /**
     * Constructor for a template with interaction state and favorite count
     *
     * @param id The template ID
     * @param name The template name
     * @param categoryId The category ID this template belongs to
     * @param imageUrl The URL of the template image
     * @param isLiked Whether the template is liked by the user
     * @param isFavorited Whether the template is favorited by the user
     * @param likeCount The number of likes for this template
     * @param favoriteCount The number of favorites for this template
     */
    public Template(String id, String name, String categoryId, String imageUrl, 
                   boolean isLiked, boolean isFavorited, long likeCount, long favoriteCount) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.isLiked = isLiked;
        this.isFavorited = isFavorited;
        this.likeCount = Math.max(0, likeCount);
        this.favoriteCount = Math.max(0, favoriteCount);
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
     * Get the formatted like count (e.g., 1K, 10K, 1M)
     *
     * @return The formatted like count as a string
     */
    public String getFormattedLikeCount() {
        return NumberFormatter.formatCount(likeCount);
    }
    
    /**
     * Set the number of likes for this template
     *
     * @param count The new like count
     */
    public void setLikeCount(long count) {
        this.likeCount = Math.max(0, count);
    }
    
    /**
     * Get the number of favorites for this template
     *
     * @return The favorite count
     */
    public long getFavoriteCount() {
        return favoriteCount;
    }
    
    /**
     * Get the formatted favorite count (e.g., 1K, 10K, 1M)
     *
     * @return The formatted favorite count as a string
     */
    public String getFormattedFavoriteCount() {
        return NumberFormatter.formatCount(favoriteCount);
    }
    
    /**
     * Set the number of favorites for this template
     *
     * @param count The new favorite count
     */
    public void setFavoriteCount(long count) {
        this.favoriteCount = Math.max(0, count);
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