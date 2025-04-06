package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.model.EngagementData;

import java.util.List;

/**
 * Data Access Object for engagement_data table
 */
@Dao
public interface EngagementDataDao {
    
    /**
     * Insert an engagement record
     * @param engagementData The engagement data to insert
     * @return The inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EngagementData engagementData);
    
    /**
     * Insert multiple engagement records
     * @param engagementData List of engagement data to insert
     * @return Array of inserted row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(List<EngagementData> engagementData);
    
    /**
     * Update an engagement record
     * @param engagementData The engagement data to update
     */
    @Update
    void update(EngagementData engagementData);
    
    /**
     * Delete an engagement record
     * @param engagementData The engagement data to delete
     */
    @Delete
    void delete(EngagementData engagementData);
    
    /**
     * Get all engagement data
     * @return LiveData list of all engagement data
     */
    @Query("SELECT * FROM engagement_data ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getAllLive();
    
    /**
     * Get all engagement data (non-LiveData)
     * @return List of all engagement data
     */
    @Query("SELECT * FROM engagement_data ORDER BY timestamp DESC")
    List<EngagementData> getAll();
    
    /**
     * Get engagement data by ID
     * @param id The engagement ID
     * @return The engagement data with the specified ID
     */
    @Query("SELECT * FROM engagement_data WHERE id = :id")
    EngagementData getById(String id);
    
    /**
     * Get all engagement data for a specific template
     * @param templateId The template ID
     * @return LiveData list of engagement data for the template
     */
    @Query("SELECT * FROM engagement_data WHERE template_id = :templateId ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getByTemplateLive(String templateId);
    
    /**
     * Get all engagement data for a specific category
     * @param category The category name
     * @return LiveData list of engagement data for the category
     */
    @Query("SELECT * FROM engagement_data WHERE category = :category ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getByCategoryLive(String category);
    
    /**
     * Get all engagement data for a specific type
     * @param type The engagement type
     * @return LiveData list of engagement data for the type
     */
    @Query("SELECT * FROM engagement_data WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getByTypeLive(int type);
    
    /**
     * Get most recently engaged categories (for recommendations)
     * @param limit Maximum number of categories to return
     * @return List of categories sorted by most recent engagement
     */
    @Query("SELECT DISTINCT category FROM engagement_data " +
           "ORDER BY timestamp DESC LIMIT :limit")
    List<String> getMostRecentCategories(int limit);
    
    /**
     * Get category weights based on engagement (for recommendations)
     * @return Map of category to count of engagements
     */
    @Query("SELECT category, COUNT(*) as count FROM engagement_data " +
           "GROUP BY category ORDER BY count DESC")
    List<CategoryCount> getCategoryWeights();
    
    /**
     * Get category weights with time decay (more recent = higher weight)
     * @param daysToConsider Only consider engagements from last X days
     * @return List of categories with weights
     */
    @Query("SELECT category, COUNT(*) as count FROM engagement_data " +
           "WHERE timestamp > :cutoffTime " +
           "GROUP BY category ORDER BY count DESC")
    List<CategoryCount> getRecentCategoryWeights(long cutoffTime);
    
    /**
     * Get most viewed templates
     * @param limit Maximum number of templates to return
     * @return List of template IDs sorted by view count
     */
    @Query("SELECT template_id, COUNT(*) as count FROM engagement_data " +
           "WHERE template_id IS NOT NULL AND type = " + EngagementData.TYPE_TEMPLATE_VIEW + 
           " GROUP BY template_id ORDER BY count DESC LIMIT :limit")
    List<TemplateCount> getMostViewedTemplates(int limit);
    
    /**
     * Get unsynced engagement data that needs to be sent to server
     * @return List of unsynced engagement data
     */
    @Query("SELECT * FROM engagement_data WHERE synced = 0 ORDER BY timestamp ASC")
    List<EngagementData> getUnsynced();
    
    /**
     * Get pending engagements that are marked for sync
     * @return List of pending engagement data
     */
    @Query("SELECT * FROM engagement_data WHERE sync_pending = 1 ORDER BY timestamp ASC")
    List<EngagementData> getPendingEngagements();
    
    /**
     * Count the number of pending engagements
     * @return Number of pending engagements
     */
    @Query("SELECT COUNT(*) FROM engagement_data WHERE sync_pending = 1")
    int getPendingEngagementCount();
    
    /**
     * Mark engagement data as synced
     * @param ids List of engagement IDs to mark as synced
     */
    @Query("UPDATE engagement_data SET synced = 1 WHERE id IN (:ids)")
    void markAsSynced(List<String> ids);
    
    /**
     * Delete engagement data older than a certain time
     * @param cutoffTime Timestamp before which to delete data
     * @return Number of rows deleted
     */
    @Query("DELETE FROM engagement_data WHERE timestamp < :cutoffTime")
    int deleteOlderThan(long cutoffTime);
    
    /**
     * Helper class for category count results
     */
    class CategoryCount {
        public String category;
        public int count;
    }
    
    /**
     * Helper class for template count results
     */
    class TemplateCount {
        public String template_id;
        public int count;
    }
} 