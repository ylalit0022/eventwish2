package com.ds.eventwish.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ds.eventwish.data.model.EngagementData;
import java.util.List;

@Dao
public interface EngagementDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EngagementData engagementData);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EngagementData> engagementData);
    
    @Query("SELECT * FROM engagement_data WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getEngagementDataForDevice(String deviceId);
    
    @Query("SELECT * FROM engagement_data WHERE deviceId = :deviceId AND type = :type ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getEngagementDataByType(String deviceId, int type);
    
    @Query("SELECT * FROM engagement_data WHERE deviceId = :deviceId AND category = :category ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getEngagementDataByCategory(String deviceId, String category);
    
    @Query("SELECT * FROM engagement_data WHERE deviceId = :deviceId AND templateId = :templateId ORDER BY timestamp DESC")
    LiveData<List<EngagementData>> getEngagementDataByTemplate(String deviceId, String templateId);
    
    @Query("DELETE FROM engagement_data WHERE deviceId = :deviceId")
    void deleteAllForDevice(String deviceId);
    
    @Query("SELECT COUNT(*) FROM engagement_data WHERE deviceId = :deviceId")
    int getEngagementCountForDevice(String deviceId);
} 