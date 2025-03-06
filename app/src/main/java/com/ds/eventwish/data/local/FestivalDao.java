package com.ds.eventwish.data.local;

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
    void insertFestival(Festival festival);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllFestivals(List<Festival> festivals);
    
    @Update
    void updateFestival(Festival festival);
    
    @Delete
    void deleteFestival(Festival festival);
    
    @Query("DELETE FROM festivals")
    void deleteAllFestivals();
    
    @Query("DELETE FROM festivals WHERE id = :festivalId")
    void deleteFestivalById(String festivalId);
    
    @Query("DELETE FROM festivals WHERE category = :category")
    void deleteFestivalsByCategory(String category);
    
    @Query("SELECT * FROM festivals ORDER BY date ASC")
    LiveData<List<Festival>> getAllFestivals();
    
    @Query("SELECT * FROM festivals WHERE id = :festivalId")
    LiveData<Festival> getFestivalById(String festivalId);
    
    @Query("SELECT * FROM festivals WHERE date BETWEEN :startDate AND :endDate AND isActive = 1 ORDER BY date ASC")
    LiveData<List<Festival>> getUpcomingFestivals(Date startDate, Date endDate);
    
    @Query("SELECT * FROM festivals WHERE date BETWEEN :startDate AND :endDate AND isActive = 1 ORDER BY date ASC")
    List<Festival> getUpcomingFestivalsSync(Date startDate, Date endDate);
    
    @Query("SELECT * FROM festivals WHERE category = :category AND isActive = 1 ORDER BY date ASC")
    LiveData<List<Festival>> getFestivalsByCategory(String category);
    
    @Query("SELECT * FROM festivals WHERE category = :category AND isActive = 1 ORDER BY date ASC")
    List<Festival> getFestivalsByCategorySync(String category);
    
    @Query("SELECT COUNT(*) FROM festivals WHERE isUnread = 1")
    LiveData<Integer> getUnreadCount();
    
    @Query("SELECT COUNT(*) FROM festivals WHERE isUnread = 1")
    int getUnreadCountSync();
    
    @Query("UPDATE festivals SET isUnread = 0")
    void markAllAsRead();
    
    @Query("UPDATE festivals SET isUnread = 0 WHERE id = :festivalId")
    void markAsRead(String festivalId);
    
    @Query("UPDATE festivals SET isNotified = 1 WHERE id = :festivalId")
    void markAsNotified(String festivalId);
    
    @Query("SELECT * FROM festivals WHERE isNotified = 0 AND date BETWEEN :startDate AND :endDate AND isActive = 1")
    List<Festival> getUnnotifiedUpcomingFestivals(Date startDate, Date endDate);
}
