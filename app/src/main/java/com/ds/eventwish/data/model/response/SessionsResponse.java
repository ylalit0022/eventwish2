package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.DeviceSession;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response class for the sessions API
 */
public class SessionsResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("sessions")
    private List<DeviceSession> sessions;

    public SessionsResponse() {
        // Required empty constructor for Gson
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<DeviceSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<DeviceSession> sessions) {
        this.sessions = sessions;
    }
} 