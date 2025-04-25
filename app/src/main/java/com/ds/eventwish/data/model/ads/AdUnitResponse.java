package com.ds.eventwish.data.model.ads;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for ad unit API calls.
 */
public class AdUnitResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("adUnits")
    private List<AdUnit> adUnits;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<AdUnit> getAdUnits() {
        return adUnits;
    }

    @Override
    public String toString() {
        return "AdUnitResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", adUnits=" + adUnits +
                '}';
    }
} 