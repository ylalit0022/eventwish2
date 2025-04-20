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
    @Query("SELECT * FROM template ORDER BY created_at DESC")
    LiveData<List<Template>> getAllTemplates();

    @Query("SELECT * FROM template WHERE id = :id")
    LiveData<Template> getTemplateById(String id);

    @Query("SELECT * FROM template ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    LiveData<List<Template>> getTemplatesPaged(int limit, int offset);

    @Query("SELECT * FROM template WHERE category_id = :category ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    LiveData<List<Template>> getTemplatesByCategoryPaged(String category, int limit, int offset);

    @Query("SELECT * FROM template WHERE is_featured = 1 ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Template>> getFeaturedTemplates(int limit);

    @Query("SELECT * FROM template WHERE is_visible = 1 ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Template>> getVisibleTemplates(int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Template template);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Template> templates);

    @Update
    void update(Template template);

    @Delete
    void delete(Template template);

    @Query("DELETE FROM template")
    void deleteAll();

    @Query("UPDATE template SET view_count = view_count + 1 WHERE id = :id")
    void incrementViewCount(String id);

    @Query("UPDATE template SET share_count = share_count + 1 WHERE id = :id")
    void incrementShareCount(String id);

    @Query("UPDATE template SET like_count = like_count + 1 WHERE id = :id")
    void incrementLikeCount(String id);

    @Query("SELECT * FROM template WHERE category = :category")
    LiveData<List<Template>> getTemplatesByCategory(String category);

    @Query("SELECT COUNT(*) FROM template")
    int getTemplateCount();

    @Query("SELECT COUNT(*) FROM template WHERE category = :category")
    int getTemplateCountByCategory(String category);

    @Transaction
    @Query("SELECT DISTINCT category FROM template")
    LiveData<List<String>> getAllCategories();
} 