package com.project.domain.service;

import com.project.api.dto.AnalyticsEventRequest;
import com.project.api.dto.AnalyticsSummaryResponse;
import com.project.messaging.dto.AnalyticsEvent;
import com.project.messaging.producer.KafkaProducer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for processing analytics events.
 * Publishes events to Kafka for async processing and aggregation.
 */
@Service
public class AnalyticsEventService {

    private final KafkaProducer kafkaProducer;
    private final RedisTemplate<String, Long> redisTemplate;

    private static final List<String> EVENT_TYPES = List.of(
            "PAGE_VIEW", "BUTTON_CLICK", "FORM_SUBMIT", "API_CALL", "PURCHASE"
    );

    public AnalyticsEventService(KafkaProducer kafkaProducer,
                                  RedisTemplate<String, Long> redisTemplate) {
        this.kafkaProducer = kafkaProducer;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Log analytics event asynchronously via Kafka.
     *
     * @param request Analytics event request
     */
    public void logEvent(AnalyticsEventRequest request) {
        AnalyticsEvent event = new AnalyticsEvent(
                request.getUserId(),
                request.getEventType(),
                request.getProperties()
        );

        // Publish to Kafka (async, fire-and-forget)
        kafkaProducer.sendAnalyticsEvent(event);
    }

    /**
     * Get analytics summary for a specific date.
     * Reads aggregated counts from Redis.
     *
     * @param date Date to get summary for (null = today)
     * @return Analytics summary with event counts
     */
    public AnalyticsSummaryResponse getSummary(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        Map<String, Long> eventCounts = new HashMap<>();

        for (String eventType : EVENT_TYPES) {
            Long count = getEventCount(eventType, date);
            eventCounts.put(eventType, count);
        }

        return new AnalyticsSummaryResponse(date.toString(), eventCounts);
    }

    /**
     * Get event count for a specific type and date from Redis.
     *
     * @param eventType Event type
     * @param date      Date
     * @return Event count (0 if not found)
     */
    private Long getEventCount(String eventType, LocalDate date) {
        String key = buildRedisKey(eventType, date);
        Long count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }

    /**
     * Build Redis key for analytics aggregation.
     * Format: analytics:events:{eventType}:{date}
     *
     * @param eventType Event type
     * @param date      Date
     * @return Redis key
     */
    private String buildRedisKey(String eventType, LocalDate date) {
        return "analytics:events:" + eventType + ":" + date.toString();
    }
}
