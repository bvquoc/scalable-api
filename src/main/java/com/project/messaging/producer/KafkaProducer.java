package com.project.messaging.producer;

import com.project.config.KafkaConfig;
import com.project.messaging.dto.AnalyticsEvent;
import com.project.messaging.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for event streaming.
 * Publishes events to Kafka topics for event-driven architecture.
 */
@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish order event to Kafka topic.
     *
     * @param event Order event
     */
    public void publishOrderEvent(OrderEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, event.getOrderNumber(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published order event: eventType={}, orderId={}, partition={}",
                        event.getEventType(), event.getOrderId(),
                        result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish order event: {}", ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Failed to publish order event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish user activity event.
     *
     * @param userId User ID
     * @param action Action performed
     * @param details Event details
     */
    public void publishUserEvent(Long userId, String action, String details) {
        try {
            UserEvent event = new UserEvent(userId, action, details);

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.USER_EVENTS_TOPIC, userId.toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published user event: userId={}, action={}", userId, action);
                } else {
                    log.error("Failed to publish user event: {}", ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Failed to publish user event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish inventory update event.
     *
     * @param productId Product ID
     * @param sku Product SKU
     * @param oldStock Old stock quantity
     * @param newStock New stock quantity
     */
    public void publishInventoryEvent(Long productId, String sku, Integer oldStock, Integer newStock) {
        try {
            InventoryEvent event = new InventoryEvent(productId, sku, oldStock, newStock);

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.INVENTORY_EVENTS_TOPIC, sku, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published inventory event: productId={}, sku={}, stock: {} -> {}",
                        productId, sku, oldStock, newStock);
                } else {
                    log.error("Failed to publish inventory event: {}", ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Failed to publish inventory event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish system event (errors, warnings, metrics).
     *
     * @param level Event level (INFO, WARN, ERROR)
     * @param message Event message
     * @param details Event details
     */
    public void publishSystemEvent(String level, String message, String details) {
        try {
            SystemEvent event = new SystemEvent(level, message, details);

            kafkaTemplate.send(KafkaConfig.SYSTEM_EVENTS_TOPIC, level, event);

            log.debug("Published system event: level={}, message={}", level, message);

        } catch (Exception e) {
            log.error("Failed to publish system event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish analytics event for real-time aggregation.
     * Used for high-throughput event logging (fire-and-forget).
     *
     * @param event Analytics event
     */
    public void sendAnalyticsEvent(AnalyticsEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.ANALYTICS_EVENTS_TOPIC, event.getUserId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published analytics event: eventId={}, eventType={}, userId={}",
                        event.getEventId(), event.getEventType(), event.getUserId());
                } else {
                    log.warn("Failed to publish analytics event: {}", ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Failed to send analytics event: {}", e.getMessage(), e);
        }
    }

    // Helper DTOs
    public static class UserEvent {
        private Long userId;
        private String action;
        private String details;
        private LocalDateTime timestamp;

        public UserEvent() { this.timestamp = LocalDateTime.now(); }

        public UserEvent(Long userId, String action, String details) {
            this.userId = userId;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class InventoryEvent {
        private Long productId;
        private String sku;
        private Integer oldStock;
        private Integer newStock;
        private LocalDateTime timestamp;

        public InventoryEvent() { this.timestamp = LocalDateTime.now(); }

        public InventoryEvent(Long productId, String sku, Integer oldStock, Integer newStock) {
            this.productId = productId;
            this.sku = sku;
            this.oldStock = oldStock;
            this.newStock = newStock;
            this.timestamp = LocalDateTime.now();
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public Integer getOldStock() { return oldStock; }
        public void setOldStock(Integer oldStock) { this.oldStock = oldStock; }
        public Integer getNewStock() { return newStock; }
        public void setNewStock(Integer newStock) { this.newStock = newStock; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class SystemEvent {
        private String level;
        private String message;
        private String details;
        private LocalDateTime timestamp;

        public SystemEvent() { this.timestamp = LocalDateTime.now(); }

        public SystemEvent(String level, String message, String details) {
            this.level = level;
            this.message = message;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
