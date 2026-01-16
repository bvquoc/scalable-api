package com.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for logging analytics events.
 */
@Schema(description = "Request body for logging analytics events")
public class AnalyticsEventRequest {

    @Schema(description = "User ID who triggered the event", example = "user-123", required = true)
    @NotBlank(message = "User ID is required")
    private String userId;

    @Schema(description = "Type of event", example = "PAGE_VIEW", required = true,
            allowableValues = {"PAGE_VIEW", "BUTTON_CLICK", "FORM_SUBMIT", "API_CALL", "PURCHASE"})
    @NotBlank(message = "Event type is required")
    private String eventType;

    @Schema(description = "Additional event properties", example = "{\"page\": \"/home\", \"referrer\": \"google\"}")
    private Map<String, Object> properties;

    public AnalyticsEventRequest() {}

    public AnalyticsEventRequest(String userId, String eventType, Map<String, Object> properties) {
        this.userId = userId;
        this.eventType = eventType;
        this.properties = properties;
    }

    // Getters and setters
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
}
