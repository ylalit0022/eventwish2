package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Model class representing a device session
 */
public class DeviceSession {
    
    @SerializedName("deviceId")
    private String deviceId;
    
    @SerializedName("deviceModel")
    private String deviceModel;
    
    @SerializedName("deviceName")
    private String deviceName;
    
    @SerializedName("appVersion")
    private String appVersion;
    
    @SerializedName("osVersion")
    private String osVersion;
    
    @SerializedName("loginTimestamp")
    private Date loginTimestamp;
    
    @SerializedName("lastActiveTimestamp")
    private Date lastActiveTimestamp;
    
    @SerializedName("isCurrentDevice")
    private boolean isCurrentDevice;

    public DeviceSession() {
        // Required empty constructor for Gson
    }

    public DeviceSession(String deviceId, String deviceModel, String deviceName, String appVersion, 
                         String osVersion, Date loginTimestamp, Date lastActiveTimestamp, boolean isCurrentDevice) {
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.deviceName = deviceName;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.loginTimestamp = loginTimestamp;
        this.lastActiveTimestamp = lastActiveTimestamp;
        this.isCurrentDevice = isCurrentDevice;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Date getLoginTimestamp() {
        return loginTimestamp;
    }

    public void setLoginTimestamp(Date loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
    }

    public Date getLastActiveTimestamp() {
        return lastActiveTimestamp;
    }

    public void setLastActiveTimestamp(Date lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public boolean isCurrentDevice() {
        return isCurrentDevice;
    }

    public void setCurrentDevice(boolean currentDevice) {
        isCurrentDevice = currentDevice;
    }

    @Override
    public String toString() {
        return "DeviceSession{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", loginTimestamp=" + loginTimestamp +
                ", lastActiveTimestamp=" + lastActiveTimestamp +
                ", isCurrentDevice=" + isCurrentDevice +
                '}';
    }
} 