package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.FlashyMessageEntity;

import java.util.List;

@Dao
public interface FlashyMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FlashyMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FlashyMessageEntity> messages);

    @Update
    void update(FlashyMessageEntity message);

    @Delete
    void delete(FlashyMessageEntity message);

    @Query("DELETE FROM flashy_messages")
    void deleteAll();

    @Query("SELECT * FROM flashy_messages WHERE id = :id")
    FlashyMessageEntity getById(String id);

    @Query("SELECT * FROM flashy_messages WHERE isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<FlashyMessageEntity>> getUnreadMessages();

    @Query("SELECT * FROM flashy_messages ORDER BY timestamp DESC")
    LiveData<List<FlashyMessageEntity>> getAllMessages();

    @Query("SELECT * FROM flashy_messages WHERE isRead = 0 AND isDisplaying = 0 ORDER BY priority DESC, timestamp ASC LIMIT 1")
    FlashyMessageEntity getNextMessageToDisplay();

    @Query("UPDATE flashy_messages SET isRead = 1 WHERE id = :id")
    void markAsRead(String id);

    @Query("UPDATE flashy_messages SET isDisplaying = :isDisplaying WHERE id = :id")
    void updateDisplayingState(String id, boolean isDisplaying);

    @Query("UPDATE flashy_messages SET isDisplaying = 0")
    void resetAllDisplayingStates();

    @Query("SELECT COUNT(*) FROM flashy_messages WHERE isRead = 0")
    int getUnreadCount();
} 