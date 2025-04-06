package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity class for storing user authentication data in Room database.
 */
@Entity(
    tableName = "users",
    indices = {
        @Index(value = {"phoneNumber"}, unique = true)
    }
)
public class UserEntity {
    
    @PrimaryKey
    @NonNull
    private String uid;
    
    private String phoneNumber;
    private String displayName;
    private String email;
    private String photoUrl;
    private String idToken;
    private String refreshToken;
    private long tokenExpiryTime;
    private boolean isAuthenticated;
    private long lastLoginTime;
    private long createdAt;
    private long updatedAt;
    
    // Default constructor required by Room
    public UserEntity(@NonNull String uid) {
        this.uid = uid;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Constructor with all fields
    @Ignore
    public UserEntity(@NonNull String uid, String phoneNumber, String displayName, 
                    String email, String photoUrl, String idToken, String refreshToken, 
                    long tokenExpiryTime, boolean isAuthenticated, long lastLoginTime) {
        this.uid = uid;
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.tokenExpiryTime = tokenExpiryTime;
        this.isAuthenticated = isAuthenticated;
        this.lastLoginTime = lastLoginTime;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // No-args constructor
    @Ignore
    public UserEntity() {
        this.uid = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and setters
    @NonNull
    public String getUid() {
        return uid;
    }
    
    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getTokenExpiryTime() {
        return tokenExpiryTime;
    }
    
    public void setTokenExpiryTime(long tokenExpiryTime) {
        this.tokenExpiryTime = tokenExpiryTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    public boolean isTokenExpired() {
        return System.currentTimeMillis() > tokenExpiryTime;
    }
    
    public void updateTokens(String idToken, String refreshToken, long expiryTimeInMillis) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.tokenExpiryTime = expiryTimeInMillis;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void updateUserData(String phoneNumber, String displayName,
                          String email, String profileImage) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = profileImage;
        this.updatedAt = System.currentTimeMillis();
    }
} 