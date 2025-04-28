package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity class for storing category click data in Room database.
 */
@Entity(
    tableName = "category_clicks",
    indices = {
        @Index(value = {"userId", "categoryName"}, unique = true)
    }
)
public class CategoryClickEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @NonNull
    private String userId;
    
    @NonNull
    private String categoryName;
    
    private int clickCount;
    
    private long lastClickedAt;
    
    /**
     * Default constructor required by Room
     */
    public CategoryClickEntity(@NonNull String userId, @NonNull String categoryName) {
        this.userId = userId;
        this.categoryName = categoryName;
        this.clickCount = 1;
        this.lastClickedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @NonNull
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }
    
    @NonNull
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(@NonNull String categoryName) {
        this.categoryName = categoryName;
    }
    
    public int getClickCount() {
        return clickCount;
    }
    
    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }
    
    public long getLastClickedAt() {
        return lastClickedAt;
    }
    
    public void setLastClickedAt(long lastClickedAt) {
        this.lastClickedAt = lastClickedAt;
    }
    
    /**
     * Increment click count and update last clicked time
     */
    public void incrementClickCount() {
        this.clickCount++;
        this.lastClickedAt = System.currentTimeMillis();
    }
} 