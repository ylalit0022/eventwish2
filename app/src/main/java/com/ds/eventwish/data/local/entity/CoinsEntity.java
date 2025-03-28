package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing user coins and unlock status
 */
@Entity(tableName = "coins")
public class CoinsEntity {
    @PrimaryKey
    @NonNull
    private String id = "user_coins"; // Since we don't have auth, use a fixed ID

    private int coins; // Number of coins user has earned
    
    private long unlockTimestamp; // When the feature was unlocked
    
    private int unlockDuration; // Duration in days
    
    private boolean isUnlocked; // Whether feature is currently unlocked
    
    public CoinsEntity() {
        this.id = "user_coins";
        this.coins = 0;
        this.unlockTimestamp = 0;
        this.unlockDuration = 0;
        this.isUnlocked = false;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public int getCoins() {
        return coins;
    }
    
    public void setCoins(int coins) {
        this.coins = coins;
    }
    
    public long getUnlockTimestamp() {
        return unlockTimestamp;
    }
    
    public void setUnlockTimestamp(long unlockTimestamp) {
        this.unlockTimestamp = unlockTimestamp;
    }
    
    public int getUnlockDuration() {
        return unlockDuration;
    }
    
    public void setUnlockDuration(int unlockDuration) {
        this.unlockDuration = unlockDuration;
    }
    
    public boolean isUnlocked() {
        return isUnlocked;
    }
    
    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
    
    @Override
    public String toString() {
        return "CoinsEntity{" +
                "id='" + id + '\'' +
                ", coins=" + coins +
                ", unlockTimestamp=" + unlockTimestamp +
                ", unlockDuration=" + unlockDuration +
                ", isUnlocked=" + isUnlocked +
                '}';
    }
}