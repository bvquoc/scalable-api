package com.project.domain.service;

import com.project.api.dto.AnalyticsEventRequest;
import com.project.api.dto.AnalyticsSummaryResponse;
import com.project.messaging.dto.AnalyticsEvent;
import com.project.messaging.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AnalyticsEventService Unit Tests")
class AnalyticsEventServiceTest {

    private AnalyticsEventService analyticsEventService;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        analyticsEventService = new AnalyticsEventService(kafkaProducer, redisTemplate);
    }

    @Test
    @DisplayName("Should log event and publish to Kafka")
    void testLogEvent() {
        // Arrange
        AnalyticsEventRequest request = new AnalyticsEventRequest(
                "user-123",
                "PAGE_VIEW",
                Map.of("page", "/home", "referrer", "google")
        );

        // Act
        analyticsEventService.logEvent(request);

        // Assert
        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(kafkaProducer, times(1)).sendAnalyticsEvent(captor.capture());

        AnalyticsEvent sentEvent = captor.getValue();
        assertEquals("user-123", sentEvent.getUserId());
        assertEquals("PAGE_VIEW", sentEvent.getEventType());
        assertEquals("/home", sentEvent.getProperties().get("page"));
    }

    @Test
    @DisplayName("Should get summary for specific date from Redis")
    void testGetSummaryForSpecificDate() {
        // Arrange
        LocalDate testDate = LocalDate.of(2026, 1, 15);
        when(valueOperations.get("analytics:events:PAGE_VIEW:2026-01-15")).thenReturn(1000L);
        when(valueOperations.get("analytics:events:BUTTON_CLICK:2026-01-15")).thenReturn(500L);
        when(valueOperations.get("analytics:events:FORM_SUBMIT:2026-01-15")).thenReturn(100L);
        when(valueOperations.get("analytics:events:API_CALL:2026-01-15")).thenReturn(50L);
        when(valueOperations.get("analytics:events:PURCHASE:2026-01-15")).thenReturn(10L);

        // Act
        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(testDate);

        // Assert
        assertNotNull(summary);
        assertEquals("2026-01-15", summary.getDate());
        assertEquals(1000L, summary.getEventCounts().get("PAGE_VIEW"));
        assertEquals(500L, summary.getEventCounts().get("BUTTON_CLICK"));
        assertEquals(100L, summary.getEventCounts().get("FORM_SUBMIT"));
        assertEquals(50L, summary.getEventCounts().get("API_CALL"));
        assertEquals(10L, summary.getEventCounts().get("PURCHASE"));
        assertEquals(1660L, summary.getTotalEvents());
    }

    @Test
    @DisplayName("Should get summary for today when date is null")
    void testGetSummaryDefaultsToToday() {
        // Arrange
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        when(valueOperations.get("analytics:events:PAGE_VIEW:" + todayStr)).thenReturn(100L);
        when(valueOperations.get("analytics:events:BUTTON_CLICK:" + todayStr)).thenReturn(50L);
        when(valueOperations.get("analytics:events:FORM_SUBMIT:" + todayStr)).thenReturn(10L);
        when(valueOperations.get("analytics:events:API_CALL:" + todayStr)).thenReturn(5L);
        when(valueOperations.get("analytics:events:PURCHASE:" + todayStr)).thenReturn(1L);

        // Act
        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(null);

        // Assert
        assertNotNull(summary);
        assertEquals(todayStr, summary.getDate());
        assertEquals(166L, summary.getTotalEvents());
    }

    @Test
    @DisplayName("Should return zero counts for events with no data")
    void testGetSummaryWithMissingData() {
        // Arrange
        LocalDate testDate = LocalDate.of(2026, 1, 14);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(testDate);

        // Assert
        assertNotNull(summary);
        assertEquals("2026-01-14", summary.getDate());
        assertEquals(0L, summary.getEventCounts().get("PAGE_VIEW"));
        assertEquals(0L, summary.getEventCounts().get("BUTTON_CLICK"));
        assertEquals(0L, summary.getTotalEvents());
    }

    @Test
    @DisplayName("Should handle event with null properties")
    void testLogEventWithNullProperties() {
        // Arrange
        AnalyticsEventRequest request = new AnalyticsEventRequest("user-456", "BUTTON_CLICK", null);

        // Act
        analyticsEventService.logEvent(request);

        // Assert
        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(kafkaProducer, times(1)).sendAnalyticsEvent(captor.capture());

        AnalyticsEvent sentEvent = captor.getValue();
        assertEquals("user-456", sentEvent.getUserId());
        assertEquals("BUTTON_CLICK", sentEvent.getEventType());
        assertNull(sentEvent.getProperties());
    }

    @Test
    @DisplayName("Should include all event types in summary")
    void testGetSummaryIncludesAllEventTypes() {
        // Arrange
        LocalDate testDate = LocalDate.of(2026, 1, 16);
        Map<String, Long> expectedCounts = new HashMap<>();
        expectedCounts.put("PAGE_VIEW", 1500L);
        expectedCounts.put("BUTTON_CLICK", 300L);
        expectedCounts.put("FORM_SUBMIT", 50L);
        expectedCounts.put("API_CALL", 25L);
        expectedCounts.put("PURCHASE", 5L);

        for (Map.Entry<String, Long> entry : expectedCounts.entrySet()) {
            when(valueOperations.get("analytics:events:" + entry.getKey() + ":2026-01-16"))
                    .thenReturn(entry.getValue());
        }

        // Act
        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(testDate);

        // Assert
        assertEquals(expectedCounts, summary.getEventCounts());
        assertEquals(1880L, summary.getTotalEvents());
    }
}
