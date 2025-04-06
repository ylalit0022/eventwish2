package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.ResourceEntity;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for ResourceEntity
 */
@Dao
public interface ResourceDao {
    /**
     * Insert a resource
     * @param resource Resource to insert
     * @return ID of the inserted resource
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ResourceEntity resource);
    
    /**
     * Insert multiple resources
     * @param resources Resources to insert
     * @return IDs of the inserted resources
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ResourceEntity> resources);
    
    /**
     * Update a resource
     * @param resource Resource to update
     * @return Number of rows updated
     */
    @Update
    int update(ResourceEntity resource);
    
    /**
     * Delete a resource
     * @param resource Resource to delete
     * @return Number of rows deleted
     */
    @Delete
    int delete(ResourceEntity resource);
    
    /**
     * Get a resource by type and key
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @return Resource entity or null if not found
     */
    @Query("SELECT * FROM resources WHERE resource_type = :resourceType AND resource_key = :resourceKey")
    ResourceEntity getResource(String resourceType, String resourceKey);
    
    /**
     * Get a resource by type and key as LiveData
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @return LiveData with resource entity
     */
    @Query("SELECT * FROM resources WHERE resource_type = :resourceType AND resource_key = :resourceKey")
    LiveData<ResourceEntity> getResourceLiveData(String resourceType, String resourceKey);
    
    /**
     * Get all resources of a specific type
     * @param resourceType Resource type
     * @return List of resources
     */
    @Query("SELECT * FROM resources WHERE resource_type = :resourceType")
    List<ResourceEntity> getResourcesByType(String resourceType);
    
    /**
     * Get all resources of a specific type as LiveData
     * @param resourceType Resource type
     * @return LiveData with list of resources
     */
    @Query("SELECT * FROM resources WHERE resource_type = :resourceType")
    LiveData<List<ResourceEntity>> getResourcesByTypeLiveData(String resourceType);
    
    /**
     * Get all stale resources
     * @return List of stale resources
     */
    @Query("SELECT * FROM resources WHERE is_stale = 1")
    List<ResourceEntity> getStaleResources();
    
    /**
     * Get all expired resources
     * @param currentTime Current time
     * @return List of expired resources
     */
    @Query("SELECT * FROM resources WHERE expiration_time IS NOT NULL AND expiration_time < :currentTime")
    List<ResourceEntity> getExpiredResources(Date currentTime);
    
    /**
     * Mark resources as stale by type
     * @param resourceType Resource type
     * @return Number of rows updated
     */
    @Query("UPDATE resources SET is_stale = 1 WHERE resource_type = :resourceType")
    int markResourcesAsStaleByType(String resourceType);
    
    /**
     * Delete resources by type
     * @param resourceType Resource type
     * @return Number of rows deleted
     */
    @Query("DELETE FROM resources WHERE resource_type = :resourceType")
    int deleteResourcesByType(String resourceType);
    
    /**
     * Delete expired resources
     * @param currentTime Current time
     * @return Number of rows deleted
     */
    @Query("DELETE FROM resources WHERE expiration_time IS NOT NULL AND expiration_time < :currentTime")
    int deleteExpiredResources(Date currentTime);
    
    /**
     * Count resources by type
     * @param resourceType Resource type
     * @return Number of resources
     */
    @Query("SELECT COUNT(*) FROM resources WHERE resource_type = :resourceType")
    int countResourcesByType(String resourceType);
    
    /**
     * Check if a resource exists
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @return true if exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM resources WHERE resource_type = :resourceType AND resource_key = :resourceKey LIMIT 1)")
    boolean resourceExists(String resourceType, String resourceKey);

    @Query("DELETE FROM resources WHERE resource_type = :resourceType AND resource_key = :resourceKey")
    void delete(String resourceType, String resourceKey);

    @Query("DELETE FROM resources WHERE resource_type = :resourceType")
    void deleteByType(String resourceType);

    @Query("DELETE FROM resources")
    void deleteAll();
} 