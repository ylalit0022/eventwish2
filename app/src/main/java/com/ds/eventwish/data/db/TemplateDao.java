package com.ds.eventwish.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.ds.eventwish.data.model.Template;

import java.util.List;

/**
 * Data Access Object for Template entity
 */
@Dao
public interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY lastUpdated DESC")
    LiveData<List<Template>> getAllTemplates();

    @Query("SELECT * FROM templates ORDER BY lastUpdated DESC")
    List<Template> getAllTemplatesSync();

    @Query("SELECT * FROM templates WHERE categoryId = :category ORDER BY lastUpdated DESC")
    List<Template> getTemplatesByCategorySync(String category);

    @Query("SELECT * FROM templates WHERE id = :id")
    LiveData<Template> getTemplateById(String id);

    /**
     * Synchronous version of getTemplateById that returns the template directly
     * without using LiveData. Useful for background operations.
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    Template getTemplateByIdSync(String id);

    @Query("SELECT * FROM templates ORDER BY lastUpdated DESC LIMIT :limit OFFSET :offset")
    LiveData<List<Template>> getTemplatesPaged(int limit, int offset);

    @Query("SELECT * FROM templates WHERE categoryId = :category ORDER BY lastUpdated DESC LIMIT :limit OFFSET :offset")
    LiveData<List<Template>> getTemplatesByCategoryPaged(String category, int limit, int offset);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Template template);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Template> templates);

    @Update
    void update(Template template);

    @Delete
    void delete(Template template);

    @Query("DELETE FROM templates")
    void deleteAll();

    @Query("UPDATE templates SET likeCount = likeCount + 1 WHERE id = :id")
    void incrementLikeCount(String id);

    @Query("SELECT * FROM templates WHERE categoryId = :category")
    LiveData<List<Template>> getTemplatesByCategory(String category);

    @Query("SELECT COUNT(*) FROM templates")
    int getTemplateCount();

    @Query("SELECT COUNT(*) FROM templates WHERE categoryId = :category")
    int getTemplateCountByCategory(String category);

    @Transaction
    @Query("SELECT DISTINCT categoryId FROM templates")
    LiveData<List<String>> getAllCategories();

    @Query("UPDATE templates SET isLiked = :isLiked WHERE id = :templateId")
    void updateLikeState(String templateId, boolean isLiked);

    @Query("UPDATE templates SET isFavorited = :isFavorited WHERE id = :templateId")
    void updateFavoriteState(String templateId, boolean isFavorited);

    @Query("UPDATE templates SET likeCount = :likeCount WHERE id = :templateId")
    void updateLikeCount(String templateId, int likeCount);

    @Query("SELECT isLiked FROM templates WHERE id = :templateId")
    LiveData<Boolean> getLikeState(String templateId);

    @Query("SELECT isFavorited FROM templates WHERE id = :templateId")
    LiveData<Boolean> getFavoriteState(String templateId);

    @Query("SELECT likeCount FROM templates WHERE id = :templateId")
    LiveData<Integer> getLikeCount(String templateId);

    @Query("SELECT * FROM templates WHERE isLiked = 1")
    LiveData<List<Template>> getLikedTemplates();

    /**
     * Synchronous version of getLikedTemplates that returns the templates directly
     * without using LiveData. Useful for background operations.
     */
    @Query("SELECT * FROM templates WHERE isLiked = 1")
    List<Template> getLikedTemplatesSync();

    @Query("SELECT * FROM templates WHERE isFavorited = 1")
    LiveData<List<Template>> getFavoritedTemplates();

    /**
     * Synchronous version of getFavoritedTemplates that returns the templates directly
     * without using LiveData. Useful for background operations.
     */
    @Query("SELECT * FROM templates WHERE isFavorited = 1")
    List<Template> getFavoritedTemplatesSync();
} 