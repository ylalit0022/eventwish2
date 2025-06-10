package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Wrapper class for Remote Config JSON structure
 */
public class RemoteConfigWrapper {
    @SerializedName("event_push")
    private EventNotificationConfig eventPush;
    
    @SerializedName("events")
    private List<EventNotificationConfig> events;
    
    public EventNotificationConfig getEventPush() {
        return eventPush;
    }
    
    public void setEventPush(EventNotificationConfig eventPush) {
        this.eventPush = eventPush;
    }
    
    public List<EventNotificationConfig> getEvents() {
        return events;
    }
    
    public void setEvents(List<EventNotificationConfig> events) {
        this.events = events;
    }
} 