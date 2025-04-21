package com.ds.eventwish.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.model.SharedWish;

import java.util.List;

@Dao
public interface SharedWishDao {
    @Query("SELECT * FROM shared_wish ORDER BY created_at DESC")
    LiveData<List<SharedWish>> getAllSharedWishes();

    @Query("SELECT * FROM shared_wish WHERE id = :id")
    LiveData<SharedWish> getSharedWishById(String id);

    @Query("SELECT * FROM shared_wish WHERE short_code = :shortCode")
    LiveData<SharedWish> getSharedWishByShortCode(String shortCode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SharedWish sharedWish);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SharedWish> sharedWishes);

    @Update
    void update(SharedWish sharedWish);

    @Delete
    void delete(SharedWish sharedWish);

    @Query("DELETE FROM shared_wish")
    void deleteAll();

    @Query("UPDATE shared_wish SET views = views + 1 WHERE id = :id")
    void incrementViews(String id);
} 