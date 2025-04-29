package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.SponsoredAd;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for sponsored ads API.
 * Maps to the server's JSON response structure.
 */
public class SponsoredAdResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("ads")
    private List<SponsoredAd> ads;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    /**
     * Default constructor
     */
    public SponsoredAdResponse() {
        this.success = false;
        this.ads = new ArrayList<>();
    }
    
    /**
     * Full constructor
     * @param success Whether the request was successful
     * @param ads List of sponsored ads
     * @param message Success or informational message
     * @param error Error message if request failed
     */
    public SponsoredAdResponse(boolean success, List<SponsoredAd> ads, String message, String error) {
        this.success = success;
        this.ads = ads != null ? ads : new ArrayList<>();
        this.message = message;
        this.error = error;
    }
    
    /**
     * Check if the request was successful
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Get the list of sponsored ads
     * @return List of SponsoredAd objects, or empty list if none
     */
    public List<SponsoredAd> getAds() {
        return ads != null ? ads : new ArrayList<>();
    }
    
    /**
     * Get the response message
     * @return Message string or null
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get error message if request failed
     * @return Error message or null
     */
    public String getError() {
        return error;
    }
    
    /**
     * Set the success flag
     * @param success Whether the request was successful
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Set the ads list
     * @param ads List of SponsoredAd objects
     */
    public void setAds(List<SponsoredAd> ads) {
        this.ads = ads;
    }
    
    /**
     * Set the response message
     * @param message Message string
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Set the error message
     * @param error Error message string
     */
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Get the count of ads in the response
     * @return Number of ads
     */
    public int getAdCount() {
        return ads != null ? ads.size() : 0;
    }
    
    /**
     * Check if there are any ads in the response
     * @return true if ads exist, false otherwise
     */
    public boolean hasAds() {
        return ads != null && !ads.isEmpty();
    }
    
    /**
     * Convert to string representation
     * @return String representation of the response
     */
    @Override
    public String toString() {
        return "SponsoredAdResponse{" +
                "success=" + success +
                ", adCount=" + getAdCount() +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
} 