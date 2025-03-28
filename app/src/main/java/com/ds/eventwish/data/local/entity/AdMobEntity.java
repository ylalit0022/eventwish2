package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing AdMob configuration
 */
@Entity(tableName = "admobs")
public class AdMobEntity {
    @PrimaryKey
    @NonNull
    private String adUnitId;
    
    private String adName;
    
    private String adType; // Banner, Interstitial, Rewarded, Native, App Open
    
    private boolean status; // Whether to show/load this ad
    
    public AdMobEntity() {
        this.adUnitId = "";
    }
    
    @Ignore
    public AdMobEntity(@NonNull String adUnitId, String adName, String adType, boolean status) {
        this.adUnitId = adUnitId;
        this.adName = adName;
        this.adType = adType;
        this.status = status;
    }
    
    @NonNull
    public String getAdUnitId() {
        return adUnitId;
    }
    
    public void setAdUnitId(@NonNull String adUnitId) {
        this.adUnitId = adUnitId;
    }
    
    public String getAdName() {
        return adName;
    }
    
    public void setAdName(String adName) {
        this.adName = adName;
    }
    
    public String getAdType() {
        return adType;
    }
    
    public void setAdType(String adType) {
        this.adType = adType;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "AdMobEntity{" +
                "adUnitId='" + adUnitId + '\'' +
                ", adName='" + adName + '\'' +
                ", adType='" + adType + '\'' +
                ", status=" + status +
                '}';
    }
} 