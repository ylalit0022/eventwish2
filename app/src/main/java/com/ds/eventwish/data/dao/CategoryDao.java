package com.ds.eventwish.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY display_order ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM category WHERE is_visible = 1 ORDER BY display_order ASC")
    LiveData<List<Category>> getVisibleCategories();

    @Query("SELECT * FROM category WHERE id = :id")
    LiveData<Category> getCategoryById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE FROM category")
    void deleteAll();

    @Query("UPDATE category SET template_count = :count WHERE id = :categoryId")
    void updateTemplateCount(String categoryId, int count);
} 