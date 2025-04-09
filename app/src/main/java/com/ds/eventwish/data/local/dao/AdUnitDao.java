package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.AdUnitEntity;

import java.util.List;

@Dao
public interface AdUnitDao {
    @Query("SELECT * FROM ad_units")
    LiveData<List<AdUnitEntity>> getAllAdUnits();
    
    @Query("SELECT * FROM ad_units WHERE adType = :adType")
    LiveData<List<AdUnitEntity>> getAdUnitsByType(String adType);
    
    @Query("SELECT * FROM ad_units WHERE id = :id")
    LiveData<AdUnitEntity> getAdUnitById(String id);
    
    @Query("SELECT * FROM ad_units WHERE adType = :adType AND status = 1 AND canShow = 1")
    LiveData<List<AdUnitEntity>> getAvailableAdUnitsByType(String adType);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAdUnits(List<AdUnitEntity> adUnits);
    
    @Update
    void updateAdUnits(List<AdUnitEntity> adUnits);
    
    @Query("DELETE FROM ad_units")
    void deleteAllAdUnits();
    
    @Query("DELETE FROM ad_units WHERE adType = :adType")
    void deleteAdUnitsByType(String adType);
    
    @Query("SELECT * FROM ad_units WHERE adType = :adType ORDER BY targetingPriority DESC LIMIT 1")
    AdUnitEntity getHighestPriorityAdUnit(String adType);
    
    @Query("SELECT COUNT(*) FROM ad_units WHERE adType = :adType")
    int getAdUnitCountByType(String adType);
    
    @Query("UPDATE ad_units SET canShow = :canShow, reason = :reason WHERE id = :id")
    void updateAdUnitStatus(String id, boolean canShow, String reason);
    
    @Query("UPDATE ad_units SET nextAvailable = :nextAvailable WHERE id = :id")
    void updateNextAvailable(String id, String nextAvailable);
    
    @Transaction
    default void refreshAdUnits(List<AdUnitEntity> adUnits) {
        deleteAllAdUnits();
        insertAdUnits(adUnits);
    }
    
    @Transaction
    default void refreshAdUnitsByType(String adType, List<AdUnitEntity> adUnits) {
        deleteAdUnitsByType(adType);
        insertAdUnits(adUnits);
    }
} 