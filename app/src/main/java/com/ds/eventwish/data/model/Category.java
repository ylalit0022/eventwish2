package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;

/**
 * Data class representing a category
 */
public class Category {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private int count;
    private int order;
    
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
     * @param iconUrl URL to category icon
     * @param count Number of items in category
     * @param order Display order
     */
    public Category(String id, String name, String description, String iconUrl, int count, int order) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.count = count;
        this.order = order;
    }
    
    /**
     * Constructor with essential fields
     * @param id Category ID
     * @param name Category name
     * @param iconUrl URL to category icon
     */
    public Category(String id, String name, String iconUrl) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.description = "";
        this.count = 0;
        this.order = 0;
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
    
    public String getIconUrl() {
        return iconUrl;
    }
    
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    @NonNull
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", count=" + count +
                ", order=" + order +
                '}';
    }
} 