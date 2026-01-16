package com.project.messaging.consumer;

import com.project.messaging.dto.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AnalyticsEventConsumer Unit Tests")
class AnalyticsEventConsumerTest {

    private AnalyticsEventConsumer analyticsEventConsumer;

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        analyticsEventConsumer = new AnalyticsEventConsumer(redisTemplate);
    }

    @Test
    @DisplayName("Should increment event count in Redis")
    void testConsumeAnalyticsEventIncrementsCount() {
        // Arrange
        AnalyticsEvent event = new AnalyticsEvent("user-123", "PAGE_VIEW", null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(event);

        // Assert
        LocalDate today = LocalDate.now();
        String expectedKey = "analytics:events:PAGE_VIEW:" + today.toString();

        verify(valueOperations, times(1)).increment(expectedKey);
        verify(redisTemplate, times(1)).expire(expectedKey, 90, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("Should set TTL to 90 days after increment")
    void testConsumeAnalyticsEventSetsTTL() {
        // Arrange
        AnalyticsEvent event = new AnalyticsEvent("user-456", "BUTTON_CLICK", null);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(event);

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(1)).expire(keyCaptor.capture(), eq(90L), eq(TimeUnit.DAYS));

        String capturedKey = keyCaptor.getValue();
        assertTrue(capturedKey.contains("BUTTON_CLICK"));
        assertTrue(capturedKey.startsWith("analytics:events:"));
    }

    @Test
    @DisplayName("Should handle different event types")
    void testConsumeAnalyticsEventHandlesMultipleEventTypes() {
        // Arrange
        String[] eventTypes = {"PAGE_VIEW", "BUTTON_CLICK", "FORM_SUBMIT", "API_CALL", "PURCHASE"};
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        for (String eventType : eventTypes) {
            AnalyticsEvent event = new AnalyticsEvent("user-" + eventType, eventType, null);
            analyticsEventConsumer.consumeAnalyticsEvent(event);

            verify(valueOperations).increment(contains(eventType));
        }

        verify(valueOperations, times(5)).increment(anyString());
        verify(redisTemplate, times(5)).expire(anyString(), eq(90L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("Should not throw exception on processing error")
    void testConsumeAnalyticsEventHandlesRedisFailure() {
        // Arrange
        AnalyticsEvent event = new AnalyticsEvent("user-123", "PAGE_VIEW", null);
        when(valueOperations.increment(anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> analyticsEventConsumer.consumeAnalyticsEvent(event));
    }

    @Test
    @DisplayName("Should use today's date for Redis key")
    void testConsumeAnalyticsEventUsesTodayDate() {
        // Arrange
        AnalyticsEvent event = new AnalyticsEvent("user-123", "FORM_SUBMIT", null);
        LocalDate today = LocalDate.now();
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(event);

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).increment(keyCaptor.capture());

        String capturedKey = keyCaptor.getValue();
        assertEquals("analytics:events:FORM_SUBMIT:" + today.toString(), capturedKey);
    }

    @Test
    @DisplayName("Should properly aggregate multiple events for same type")
    void testConsumeAnalyticsEventAggregatesMultipleEvents() {
        // Arrange
        AnalyticsEvent event1 = new AnalyticsEvent("user-1", "PAGE_VIEW", null);
        AnalyticsEvent event2 = new AnalyticsEvent("user-2", "PAGE_VIEW", null);
        AnalyticsEvent event3 = new AnalyticsEvent("user-3", "PAGE_VIEW", null);

        when(valueOperations.increment(anyString()))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(3L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(event1);
        analyticsEventConsumer.consumeAnalyticsEvent(event2);
        analyticsEventConsumer.consumeAnalyticsEvent(event3);

        // Assert
        LocalDate today = LocalDate.now();
        String expectedKey = "analytics:events:PAGE_VIEW:" + today.toString();

        verify(valueOperations, times(3)).increment(expectedKey);
        verify(redisTemplate, times(3)).expire(expectedKey, 90, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("Should handle event with properties")
    void testConsumeAnalyticsEventWithProperties() {
        // Arrange
        AnalyticsEvent event = new AnalyticsEvent(
                "user-123",
                "API_CALL",
                java.util.Map.of("endpoint", "/api/users", "status", "200")
        );
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(event);

        // Assert
        LocalDate today = LocalDate.now();
        String expectedKey = "analytics:events:API_CALL:" + today.toString();

        verify(valueOperations, times(1)).increment(expectedKey);
        verify(redisTemplate, times(1)).expire(expectedKey, 90, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("Should maintain separate counters for different event types")
    void testConsumeAnalyticsEventMaintainsSeparateCounters() {
        // Arrange
        AnalyticsEvent pageViewEvent = new AnalyticsEvent("user-1", "PAGE_VIEW", null);
        AnalyticsEvent purchaseEvent = new AnalyticsEvent("user-2", "PURCHASE", null);

        when(valueOperations.increment(contains("PAGE_VIEW"))).thenReturn(5L);
        when(valueOperations.increment(contains("PURCHASE"))).thenReturn(1L);

        // Act
        analyticsEventConsumer.consumeAnalyticsEvent(pageViewEvent);
        analyticsEventConsumer.consumeAnalyticsEvent(purchaseEvent);

        // Assert
        LocalDate today = LocalDate.now();
        String pageViewKey = "analytics:events:PAGE_VIEW:" + today.toString();
        String purchaseKey = "analytics:events:PURCHASE:" + today.toString();

        verify(valueOperations).increment(pageViewKey);
        verify(valueOperations).increment(purchaseKey);
    }
}
