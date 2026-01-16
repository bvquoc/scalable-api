package com.project.messaging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics event DTO for tracking user interactions.
 * Published to Kafka topic for real-time aggregation.
 */
public class AnalyticsEvent {

    private String eventId;
    private String userId;
    private String eventType; // PAGE_VIEW, BUTTON_CLICK, FORM_SUBMIT, etc.
    private Map<String, Object> properties;
    private Instant timestamp;

    public AnalyticsEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public AnalyticsEvent(String userId, String eventType, Map<String, Object> properties) {
        this();
        this.userId = userId;
        this.eventType = eventType;
        this.properties = properties;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AnalyticsEvent{" +
                "eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
