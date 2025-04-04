package com.ds.eventwish.ui.home;

public class Category {
    private String id;
    private String name;
    private String imageUrl;
    
    public Category(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name; 
        this.imageUrl = imageUrl;
    }
    
    // Getters and setters
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}