package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.CategoryClickEntity;

import java.util.List;

/**
 * Data Access Object for CategoryClick entity
 */
@Dao
public interface CategoryClickDao {
    
    /**
     * Insert a new category click
     * @param categoryClick CategoryClick to insert
     * @return rowId of inserted item
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryClickEntity categoryClick);
    
    /**
     * Update an existing category click
     * @param categoryClick CategoryClick to update
     * @return Number of rows updated
     */
    @Update
    int update(CategoryClickEntity categoryClick);
    
    /**
     * Get category click by user and category
     * @param userId User ID
     * @param categoryName Category name
     * @return CategoryClick entity
     */
    @Query("SELECT * FROM category_clicks WHERE userId = :userId AND categoryName = :categoryName")
    CategoryClickEntity getByUserAndCategory(String userId, String categoryName);
    
    /**
     * Get category click by user and category as LiveData
     * @param userId User ID
     * @param categoryName Category name
     * @return LiveData of CategoryClick entity
     */
    @Query("SELECT * FROM category_clicks WHERE userId = :userId AND categoryName = :categoryName")
    LiveData<CategoryClickEntity> getByUserAndCategoryLive(String userId, String categoryName);
    
    /**
     * Get all category clicks for a user
     * @param userId User ID
     * @return List of CategoryClick entities
     */
    @Query("SELECT * FROM category_clicks WHERE userId = :userId ORDER BY clickCount DESC")
    List<CategoryClickEntity> getAllByUser(String userId);
    
    /**
     * Get all category clicks for a user as LiveData
     * @param userId User ID
     * @return LiveData of list of CategoryClick entities
     */
    @Query("SELECT * FROM category_clicks WHERE userId = :userId ORDER BY clickCount DESC")
    LiveData<List<CategoryClickEntity>> getAllByUserLive(String userId);
    
    /**
     * Get top clicked categories for a user
     * @param userId User ID
     * @param limit Max number of results
     * @return List of CategoryClick entities
     */
    @Query("SELECT * FROM category_clicks WHERE userId = :userId ORDER BY clickCount DESC LIMIT :limit")
    List<CategoryClickEntity> getTopCategoriesByUser(String userId, int limit);
    
    /**
     * Get total clicks for a user
     * @param userId User ID
     * @return Total number of clicks
     */
    @Query("SELECT SUM(clickCount) FROM category_clicks WHERE userId = :userId")
    int getTotalClicksByUser(String userId);
} 