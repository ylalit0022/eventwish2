package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * BlockedUser model representing user blocking information
 * Matches the backend BlockInfo schema from User model
 */
public class BlockedUser {
    
    @SerializedName("uid")
    private String uid;
    
    @SerializedName("displayName")
    private String displayName;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("isBlocked")
    private boolean isBlocked;
    
    @SerializedName("blockInfo")
    private BlockInfo blockInfo;
    
    // Constructors
    public BlockedUser() {}
    
    public BlockedUser(String uid, String displayName, String email, boolean isBlocked, BlockInfo blockInfo) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.isBlocked = isBlocked;
        this.blockInfo = blockInfo;
    }
    
    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    
    public BlockInfo getBlockInfo() { return blockInfo; }
    public void setBlockInfo(BlockInfo blockInfo) { this.blockInfo = blockInfo; }
    
    /**
     * Nested class for block information details
     */
    public static class BlockInfo {
        @SerializedName("blockedBy")
        private String blockedBy;
        
        @SerializedName("reason")
        private String reason;
        
        @SerializedName("blockedAt")
        private Date blockedAt;
        
        @SerializedName("blockExpiresAt")
        private Date blockExpiresAt;
        
        @SerializedName("notes")
        private String notes;
        
        @SerializedName("contactEmail")
        private String contactEmail;
        
        // Constructors
        public BlockInfo() {}
        
        public BlockInfo(String blockedBy, String reason, Date blockedAt, Date blockExpiresAt, String notes, String contactEmail) {
            this.blockedBy = blockedBy;
            this.reason = reason;
            this.blockedAt = blockedAt;
            this.blockExpiresAt = blockExpiresAt;
            this.notes = notes;
            this.contactEmail = contactEmail;
        }
        
        // Getters and Setters
        public String getBlockedBy() { return blockedBy; }
        public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public Date getBlockedAt() { return blockedAt; }
        public void setBlockedAt(Date blockedAt) { this.blockedAt = blockedAt; }
        
        public Date getBlockExpiresAt() { return blockExpiresAt; }
        public void setBlockExpiresAt(Date blockExpiresAt) { this.blockExpiresAt = blockExpiresAt; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        
        /**
         * Check if the block is temporary (has expiration date)
         */
        public boolean isTemporary() {
            return blockExpiresAt != null;
        }
        
        /**
         * Check if the block has expired
         */
        public boolean hasExpired() {
            return blockExpiresAt != null && new Date().after(blockExpiresAt);
        }
        
        /**
         * Get formatted reason for display
         */
        public String getDisplayReason() {
            return reason != null && !reason.trim().isEmpty() ? reason : "Your account has been blocked";
        }
        
        /**
         * Get contact email for appeals
         */
        public String getContactEmailForAppeals() {
            return contactEmail != null && !contactEmail.trim().isEmpty() ? contactEmail : "support@eventwish.com";
        }
    }
    
    /**
     * Check if user is currently blocked (considering expiration)
     */
    public boolean isCurrentlyBlocked() {
        if (!isBlocked || blockInfo == null) {
            return false;
        }
        
        // Check if block has expired
        return !blockInfo.hasExpired();
    }
    
    /**
     * Get display-friendly blocking reason
     */
    public String getDisplayReason() {
        if (blockInfo != null) {
            return blockInfo.getDisplayReason();
        }
        return "Your account has been blocked";
    }
    
    /**
     * Get contact email for appeals
     */
    public String getContactEmail() {
        if (blockInfo != null) {
            return blockInfo.getContactEmailForAppeals();
        }
        return "support@eventwish.com";
    }
    
    @Override
    public String toString() {
        return "BlockedUser{" +
                "uid='" + uid + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", isBlocked=" + isBlocked +
                ", blockInfo=" + (blockInfo != null ? "present" : "null") +
                '}';
    }
} 