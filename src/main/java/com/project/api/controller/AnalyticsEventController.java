package com.project.api.controller;

import com.project.api.dto.AnalyticsEventRequest;
import com.project.api.dto.AnalyticsSummaryResponse;
import com.project.domain.service.AnalyticsEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for Analytics Events.
 *
 * Endpoints:
 * - POST   /api/events                  - Log analytics event (async)
 * - GET    /api/analytics/summary       - Get event summary
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Analytics", description = "Analytics event logging and aggregation")
@SecurityRequirement(name = "apiKey")
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;

    public AnalyticsEventController(AnalyticsEventService analyticsEventService) {
        this.analyticsEventService = analyticsEventService;
    }

    @Operation(summary = "Log analytics event", description = "Log an analytics event asynchronously via Kafka. Returns 202 Accepted immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Event accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PostMapping("/events")
    public ResponseEntity<Void> logEvent(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.logEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Get analytics summary", description = "Retrieve aggregated event counts for a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics summary retrieved",
                    content = @Content(schema = @Schema(implementation = AnalyticsSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @Parameter(description = "Date for summary (YYYY-MM-DD). Defaults to today if not provided.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(date);
        return ResponseEntity.ok(summary);
    }
}
