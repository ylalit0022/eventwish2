package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.ds.eventwish.data.local.entity.CoinsEntity;
import android.util.Log;

@Dao
public interface CoinsDao {
    String TAG = "CoinsDao";

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
    
    @Query("UPDATE coins SET coins = :coins WHERE id = :userId")
    void updateCoins(String userId, int coins);

    @Query("SELECT coins FROM coins WHERE id = :userId")
    int getCurrentCoins(String userId);
    
    @Transaction
    default void updateCoinsWithLog(String userId, int newCoins) {
        try {
            int oldCoins = getCurrentCoins(userId);
            updateCoins(userId, newCoins);
            Log.d(TAG, String.format("Coins updated for user %s: %d -> %d", userId, oldCoins, newCoins));
        } catch (Exception e) {
            Log.e(TAG, "Error updating coins: " + e.getMessage());
            // If update fails, try inserting new entity
            CoinsEntity entity = new CoinsEntity();
            entity.setId(userId);
            entity.setCoins(newCoins);
            insert(entity);
            Log.d(TAG, String.format("Created new coins entry for user %s with %d coins", userId, newCoins));
        }
    }
}