package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

public class ServerTimeResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("date")
    private String date;

    public boolean isSuccess() {
        return success;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDate(String date) {
        this.date = date;
    }
} 