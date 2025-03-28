package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.ds.eventwish.data.local.entity.CoinsEntity;

@Dao
public interface CoinsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CoinsEntity coinsEntity);
    
    @Update
    void update(CoinsEntity coinsEntity);
    
    @Query("SELECT * FROM coins WHERE id = :id")
    CoinsEntity getCoinsById(String id);
    
    @Query("SELECT * FROM coins WHERE id = :id")
    LiveData<CoinsEntity> getCoinsLiveData(String id);
    
    @Query("UPDATE coins SET coins = coins + :amount WHERE id = :id")
    void addCoins(String id, int amount);
    
    @Query("UPDATE coins SET isUnlocked = :isUnlocked, unlockTimestamp = :timestamp, unlockDuration = :duration WHERE id = :id")
    void updateUnlockStatus(String id, boolean isUnlocked, long timestamp, int duration);
} 