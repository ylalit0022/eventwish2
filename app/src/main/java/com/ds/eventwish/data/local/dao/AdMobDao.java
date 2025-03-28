package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.AdMobEntity;

import java.util.List;

@Dao
public interface AdMobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AdMobEntity adMobEntity);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AdMobEntity> adMobEntities);
    
    @Update
    void update(AdMobEntity adMobEntity);
    
    @Query("SELECT * FROM admobs WHERE status = 1")
    List<AdMobEntity> getActiveAdMobs();
    
    @Query("SELECT * FROM admobs WHERE adType = :adType AND status = 1")
    List<AdMobEntity> getActiveAdMobsByType(String adType);
    
    @Query("SELECT * FROM admobs WHERE adUnitId = :adUnitId")
    AdMobEntity getAdMobByUnitId(String adUnitId);
} 