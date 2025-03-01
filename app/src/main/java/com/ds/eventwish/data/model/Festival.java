package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.local.converters.DateConverter;
import com.ds.eventwish.data.local.converters.TemplateListConverter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "festivals")
@TypeConverters({DateConverter.class, TemplateListConverter.class})
public class Festival {
    
    @PrimaryKey
    @NonNull
    @SerializedName("_id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("date")
    private Date date;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("imageUrl")
    private String imageUrl;
    
    @SerializedName("isActive")
    private boolean isActive;
    
    @SerializedName("templates")
    private List<FestivalTemplate> templates = new ArrayList<>();
    
    private boolean isNotified = false;
    
    private boolean isUnread = true;
    
    public Festival() {
        // Required by Room
    }
    
    @Ignore
    public Festival(@NonNull String id, String name, String description, Date date, 
                    String category, String imageUrl, boolean isActive, 
                    List<FestivalTemplate> templates) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.templates = templates != null ? templates : new ArrayList<>();
        this.isNotified = false;
        this.isUnread = true;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<FestivalTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<FestivalTemplate> templates) {
        this.templates = templates != null ? templates : new ArrayList<>();
    }
    
    public boolean isNotified() {
        return isNotified;
    }
    
    public void setNotified(boolean notified) {
        isNotified = notified;
    }
    
    public boolean isUnread() {
        return isUnread;
    }
    
    public void setUnread(boolean unread) {
        isUnread = unread;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Festival festival = (Festival) o;
        return id.equals(festival.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
