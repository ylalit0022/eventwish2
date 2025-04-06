package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.LinkedTreeMap;
import android.util.Log;

import java.util.Date;
import java.util.Map;

import com.ds.eventwish.data.local.converter.ObjectTypeConverter;

/**
 * Entity class for storing resources in the Room database
 */
@Entity(
    tableName = "resources",
    indices = {
        @Index(value = {"resource_type", "resource_key"}, unique = true)
    }
)
public class ResourceEntity {
    /**
     * Primary key for the resource
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    /**
     * Type of resource (e.g., "template", "category", "icon")
     */
    @NonNull
    @ColumnInfo(name = "resource_type")
    private String resourceType;
    
    /**
     * Key for the resource (e.g., template ID, category ID)
     */
    @NonNull
    @ColumnInfo(name = "resource_key")
    private String resourceKey;
    
    /**
     * Data for the resource as JSON
     */
    @ColumnInfo(name = "data")
    @TypeConverters(ObjectTypeConverter.class)
    private Object data;
    
    /**
     * Additional metadata for the resource
     */
    @ColumnInfo(name = "metadata")
    private Map<String, String> metadata;
    
    /**
     * Last time the resource was updated
     */
    @NonNull
    @ColumnInfo(name = "last_updated")
    private Date lastUpdated;
    
    /**
     * Expiration time for the resource
     */
    @ColumnInfo(name = "expiration_time")
    private Date expirationTime;
    
    /**
     * Whether the resource is stale and needs to be refreshed
     */
    @ColumnInfo(name = "is_stale", defaultValue = "0")
    private boolean isStale;
    
    /**
     * ETag for the resource (for HTTP caching)
     */
    @ColumnInfo(name = "etag")
    private String etag;
    
    /**
     * Default constructor
     */
    public ResourceEntity() {
        this.lastUpdated = new Date();
        this.isStale = false;
    }
    
    /**
     * Constructor with required fields
     * @param resourceType Type of resource
     * @param resourceKey Key for the resource
     * @param data Data for the resource
     */
    @Ignore
    public ResourceEntity(@NonNull String resourceType, @NonNull String resourceKey, Object data) {
        this();
        this.resourceType = resourceType;
        this.resourceKey = resourceKey;
        this.data = data;
    }
    
    /**
     * Full constructor
     * @param resourceType Type of resource
     * @param resourceKey Key for the resource
     * @param data Data for the resource
     * @param metadata Additional metadata
     * @param expirationTime Expiration time
     * @param etag ETag for HTTP caching
     */
    @Ignore
    public ResourceEntity(
            @NonNull String resourceType,
            @NonNull String resourceKey,
            Object data,
            Map<String, String> metadata,
            Date expirationTime,
            String etag) {
        this(resourceType, resourceKey, data);
        this.metadata = metadata;
        this.expirationTime = expirationTime;
        this.etag = etag;
    }
    
    // Getters and setters
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @NonNull
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(@NonNull String resourceType) {
        this.resourceType = resourceType;
    }
    
    @NonNull
    public String getResourceKey() {
        return resourceKey;
    }
    
    public void setResourceKey(@NonNull String resourceKey) {
        this.resourceKey = resourceKey;
    }
    
    /**
     * Get the data for this resource
     * @return The data as an Object (may be JsonObject or LinkedTreeMap)
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Set the data for this resource
     * @param data The data to set
     */
    public void setData(Object data) {
        this.data = data;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    @NonNull
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(@NonNull Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Date getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }
    
    public boolean isStale() {
        return isStale;
    }
    
    public void setStale(boolean stale) {
        isStale = stale;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
    
    /**
     * Check if the resource is expired
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expirationTime != null && new Date().after(expirationTime);
    }
    
    /**
     * Mark the resource as stale
     */
    public void markAsStale() {
        this.isStale = true;
        this.lastUpdated = new Date();
    }
    
    /**
     * Update the resource with new data
     * @param data New data
     * @param etag New ETag
     * @param expirationTime New expiration time
     */
    public void update(Object data, String etag, Date expirationTime) {
        this.data = data;
        this.etag = etag;
        this.expirationTime = expirationTime;
        this.isStale = false;
        this.lastUpdated = new Date();
    }
    
    /**
     * Create a unique key for the resource
     * @return Unique key combining type and key
     */
    public String getUniqueKey() {
        return resourceType + ":" + resourceKey;
    }
    
    @Override
    public String toString() {
        return "ResourceEntity{" +
                "id=" + id +
                ", resourceType='" + resourceType + '\'' +
                ", resourceKey='" + resourceKey + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", isStale=" + isStale +
                '}';
    }
} 