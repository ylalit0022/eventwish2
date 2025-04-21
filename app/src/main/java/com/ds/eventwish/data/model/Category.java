package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.google.gson.annotations.SerializedName;

/**
 * Data class representing a category
 */
@Entity(
    tableName = "category",
    indices = {
        @Index(value = {"name"}, unique = true),
        @Index(value = {"display_order"})
    }
)
public class Category {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    @SerializedName("_id")
    private String id;

    @ColumnInfo(name = "name")
    @SerializedName("name")
    private String name;

    @ColumnInfo(name = "display_name")
    @SerializedName("displayName")
    private String displayName;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "display_order")
    @SerializedName("displayOrder")
    private int displayOrder;

    @ColumnInfo(name = "icon")
    @SerializedName("icon")
    @TypeConverters(CategoryIconConverter.class)
    private CategoryIcon icon;

    @ColumnInfo(name = "template_count", defaultValue = "0")
    @SerializedName("templateCount")
    private int templateCount;

    @ColumnInfo(name = "is_visible", defaultValue = "1")
    @SerializedName("isVisible")
    private boolean isVisible;
    
    /**
     * Default constructor
     */
    public Category() {
    }
    
    /**
     * Constructor with all fields
     * @param id Category ID
     * @param name Category name
     * @param description Category description
     * @param icon Category icon
     * @param templateCount Number of items in category
     * @param displayOrder Display order
     */
    public Category(String id, String name, String description, CategoryIcon icon, int templateCount, int displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.templateCount = templateCount;
        this.displayOrder = displayOrder;
        this.isVisible = true;
    }
    
    /**
     * Constructor with essential fields
     * @param id Category ID
     * @param name Category name
     * @param icon Category icon
     */
    public Category(String id, String name, CategoryIcon icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = "";
        this.templateCount = 0;
        this.displayOrder = 0;
        this.isVisible = true;
    }
    
    /**
     * Additional constructor with @Ignore annotation
     * @param name Category name
     * @param displayName Category display name
     * @param description Category description
     * @param displayOrder Display order
     */
    @Ignore
    public Category(String name, String displayName, String description, int displayOrder) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.displayOrder = displayOrder;
        this.isVisible = true;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public CategoryIcon getIcon() {
        return icon;
    }
    
    public void setIcon(CategoryIcon icon) {
        this.icon = icon;
    }
    
    public int getTemplateCount() {
        return templateCount;
    }
    
    public void setTemplateCount(int templateCount) {
        this.templateCount = templateCount;
    }
    
    public int getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @NonNull
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", templateCount=" + templateCount +
                ", displayOrder=" + displayOrder +
                ", isVisible=" + isVisible +
                '}';
    }
} 