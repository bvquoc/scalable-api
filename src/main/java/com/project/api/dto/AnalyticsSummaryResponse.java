package com.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Response DTO for analytics summary.
 */
@Schema(description = "Analytics event summary for a specific date")
public class AnalyticsSummaryResponse {

    @Schema(description = "Date for this summary", example = "2026-01-16")
    private String date;

    @Schema(description = "Event counts by type", example = "{\"PAGE_VIEW\": 1500, \"BUTTON_CLICK\": 320, \"FORM_SUBMIT\": 45}")
    private Map<String, Long> eventCounts;

    @Schema(description = "Total events across all types", example = "1865")
    private Long totalEvents;

    public AnalyticsSummaryResponse() {}

    public AnalyticsSummaryResponse(String date, Map<String, Long> eventCounts) {
        this.date = date;
        this.eventCounts = eventCounts;
        this.totalEvents = eventCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    // Getters and setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Map<String, Long> getEventCounts() {
        return eventCounts;
    }

    public void setEventCounts(Map<String, Long> eventCounts) {
        this.eventCounts = eventCounts;
        this.totalEvents = eventCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    public Long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(Long totalEvents) {
        this.totalEvents = totalEvents;
    }
}
