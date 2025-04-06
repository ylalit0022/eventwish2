package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.model.Festival;

import java.util.Date;
import java.util.List;

@Dao
public interface FestivalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Festival festival);

    @Update
    void update(Festival festival);

    @Delete
    void delete(Festival festival);

    @Query("SELECT * FROM festivals WHERE id = :id")
    Festival getFestival(long id);

    @Query("SELECT * FROM festivals WHERE id = :id")
    LiveData<Festival> getFestivalLiveData(long id);

    @Query("SELECT * FROM festivals")
    List<Festival> getAllFestivals();

    @Query("SELECT * FROM festivals")
    LiveData<List<Festival>> getAllFestivalsLiveData();

    @Query("DELETE FROM festivals")
    void deleteAllFestivals();

    @Query("SELECT * FROM festivals WHERE id = :festivalId")
    LiveData<Festival> getFestivalById(String festivalId);

    @Query("SELECT * FROM festivals WHERE category = :category")
    LiveData<List<Festival>> getFestivalsByCategory(String category);

    @Query("SELECT * FROM festivals WHERE date >= :now AND date <= :endDate ORDER BY date ASC")
    LiveData<List<Festival>> getUpcomingFestivals(Date now, Date endDate);

    @Query("SELECT * FROM festivals WHERE date >= :now AND date <= :endDate AND isNotified = 0 ORDER BY date ASC")
    List<Festival> getUnnotifiedUpcomingFestivals(Date now, Date endDate);

    @Query("SELECT * FROM festivals WHERE date >= :now AND date <= :endDate ORDER BY date ASC")
    List<Festival> getUpcomingFestivalsSync(Date now, Date endDate);

    @Query("SELECT * FROM festivals WHERE date >= :now ORDER BY date ASC")
    List<Festival> getUpcomingFestivalsSync(Date now);

    @Query("SELECT * FROM festivals WHERE category = :category")
    List<Festival> getFestivalsByCategorySync(String category);

    @Query("UPDATE festivals SET isUnread = 0 WHERE id = :festivalId")
    void markAsRead(String festivalId);

    @Query("UPDATE festivals SET isNotified = 1 WHERE id = :festivalId")
    void markAsNotified(String festivalId);

    @Query("UPDATE festivals SET isUnread = 0")
    void markAllAsRead();

    @Query("SELECT COUNT(*) FROM festivals WHERE isUnread = 1")
    int getUnreadCountSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Festival> festivals);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllFestivals(List<Festival> festivals);

    @Query("DELETE FROM festivals WHERE category = :category")
    void deleteFestivalsByCategory(String category);
} 