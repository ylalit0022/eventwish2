package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.local.converter.MapTypeConverter;
import com.ds.eventwish.data.model.ads.AdUnit;

import java.util.Map;

@Entity(tableName = "ad_units")
public class AdUnitEntity {
    @PrimaryKey
    @NonNull
    private String id;
    
    private String adName;
    private String adUnitCode;
    private String adType;
    private boolean status;
    
    @TypeConverters(MapTypeConverter.class)
    private Map<String, Object> targetingCriteria;
    
    private int targetingPriority;
    
    @TypeConverters(MapTypeConverter.class)
    private Map<String, String> parameters;
    
    private int maxImpressionsPerDay;
    private int minIntervalBetweenAds;
    private int cooldownPeriod;
    private boolean canShow;
    private String reason;
    private String nextAvailable;
    private long lastUpdated;

    // Getters and setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdUnitCode() {
        return adUnitCode;
    }

    public void setAdUnitCode(String adUnitCode) {
        this.adUnitCode = adUnitCode;
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

    public Map<String, Object> getTargetingCriteria() {
        return targetingCriteria;
    }

    public void setTargetingCriteria(Map<String, Object> targetingCriteria) {
        this.targetingCriteria = targetingCriteria;
    }

    public int getTargetingPriority() {
        return targetingPriority;
    }

    public void setTargetingPriority(int targetingPriority) {
        this.targetingPriority = targetingPriority;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getMaxImpressionsPerDay() {
        return maxImpressionsPerDay;
    }

    public void setMaxImpressionsPerDay(int maxImpressionsPerDay) {
        this.maxImpressionsPerDay = maxImpressionsPerDay;
    }

    public int getMinIntervalBetweenAds() {
        return minIntervalBetweenAds;
    }

    public void setMinIntervalBetweenAds(int minIntervalBetweenAds) {
        this.minIntervalBetweenAds = minIntervalBetweenAds;
    }

    public int getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(int cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
    }

    public boolean isCanShow() {
        return canShow;
    }

    public void setCanShow(boolean canShow) {
        this.canShow = canShow;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNextAvailable() {
        return nextAvailable;
    }

    public void setNextAvailable(String nextAvailable) {
        this.nextAvailable = nextAvailable;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Convert to AdUnit model
    public AdUnit toAdUnit() {
        AdUnit adUnit = new AdUnit();
        adUnit.setId(id);
        adUnit.setAdName(adName);
        adUnit.setAdUnitCode(adUnitCode);
        adUnit.setAdType(adType);
        adUnit.setStatus(status);
        adUnit.setTargetingCriteria(targetingCriteria);
        adUnit.setTargetingPriority(targetingPriority);
        adUnit.setParameters(parameters);
        
        AdUnit.DisplaySettings displaySettings = new AdUnit.DisplaySettings();
        displaySettings.setMaxImpressionsPerDay(maxImpressionsPerDay);
        displaySettings.setMinIntervalBetweenAds(minIntervalBetweenAds);
        displaySettings.setCooldownPeriod(cooldownPeriod);
        adUnit.setDisplaySettings(displaySettings);
        
        adUnit.setCanShow(canShow);
        adUnit.setReason(reason);
        adUnit.setNextAvailable(nextAvailable);
        
        return adUnit;
    }

    // Create from AdUnit model
    public static AdUnitEntity fromAdUnit(AdUnit adUnit) {
        AdUnitEntity entity = new AdUnitEntity();
        entity.setId(adUnit.getId());
        entity.setAdName(adUnit.getAdName());
        entity.setAdUnitCode(adUnit.getAdUnitCode());
        entity.setAdType(adUnit.getAdType());
        entity.setStatus(adUnit.isStatus());
        entity.setTargetingCriteria(adUnit.getTargetingCriteria());
        entity.setTargetingPriority(adUnit.getTargetingPriority());
        entity.setParameters(adUnit.getParameters());
        
        if (adUnit.getDisplaySettings() != null) {
            entity.setMaxImpressionsPerDay(adUnit.getDisplaySettings().getMaxImpressionsPerDay());
            entity.setMinIntervalBetweenAds(adUnit.getDisplaySettings().getMinIntervalBetweenAds());
            entity.setCooldownPeriod(adUnit.getDisplaySettings().getCooldownPeriod());
        }
        
        entity.setCanShow(adUnit.isCanShow());
        entity.setReason(adUnit.getReason());
        entity.setNextAvailable(adUnit.getNextAvailable());
        entity.setLastUpdated(System.currentTimeMillis());
        
        return entity;
    }
} 