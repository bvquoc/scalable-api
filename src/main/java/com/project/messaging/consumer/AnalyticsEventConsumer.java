package com.project.messaging.consumer;

import com.project.config.KafkaConfig;
import com.project.messaging.dto.AnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Kafka consumer for analytics events.
 * Aggregates event counts in Redis for real-time analytics.
 */
@Component
public class AnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

    private final RedisTemplate<String, Long> redisTemplate;

    public AnalyticsEventConsumer(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Consume analytics events and aggregate counts in Redis.
     *
     * @param event Analytics event from Kafka
     */
    @KafkaListener(
            topics = KafkaConfig.ANALYTICS_EVENTS_TOPIC,
            groupId = "analytics-aggregator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAnalyticsEvent(AnalyticsEvent event) {
        try {
            // Build Redis key: analytics:events:{eventType}:{date}
            String key = buildRedisKey(event.getEventType(), LocalDate.now());

            // Increment event count
            Long newCount = redisTemplate.opsForValue().increment(key);

            // Set TTL to 90 days
            redisTemplate.expire(key, 90, TimeUnit.DAYS);

            log.debug("Aggregated analytics event: eventType={}, userId={}, count={}",
                    event.getEventType(), event.getUserId(), newCount);

        } catch (Exception e) {
            log.error("Failed to aggregate analytics event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            // Don't throw - let Kafka continue processing
        }
    }

    /**
     * Build Redis key for analytics aggregation.
     *
     * @param eventType Event type
     * @param date      Date
     * @return Redis key (e.g., "analytics:events:PAGE_VIEW:2026-01-16")
     */
    private String buildRedisKey(String eventType, LocalDate date) {
        return "analytics:events:" + eventType + ":" + date.toString();
    }
}
