package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for server time API
 */
public class ServerTimeResponse {
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("formatted")
    private String formatted;

    @SerializedName("timezone")
    private String timezone;

    public ServerTimeResponse() {
    }

    public ServerTimeResponse(long timestamp, String formatted, String timezone) {
        this.timestamp = timestamp;
        this.formatted = formatted;
        this.timezone = timezone;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String toString() {
        return "ServerTimeResponse{" +
                "timestamp=" + timestamp +
                ", formatted='" + formatted + '\'' +
                ", timezone='" + timezone + '\'' +
                '}';
    }
} 