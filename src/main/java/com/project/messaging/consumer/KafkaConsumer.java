package com.project.messaging.consumer;

import com.project.config.KafkaConfig;
import com.project.messaging.dto.OrderEvent;
import com.project.messaging.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for processing event streams.
 * Subscribes to topics and processes events as they arrive.
 */
@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    /**
     * Process order lifecycle events.
     * Use case: Analytics, audit logs, downstream services.
     */
    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "scalable-api-group")
    public void processOrderEvent(OrderEvent event) {
        try {
            log.info("Received order event: eventType={}, orderId={}, orderNumber={}, status={}",
                event.getEventType(), event.getOrderId(), event.getOrderNumber(), event.getStatus());

            // Process based on event type
            switch (event.getEventType()) {
                case "CREATED":
                    handleOrderCreated(event);
                    break;
                case "PAID":
                    handleOrderPaid(event);
                    break;
                case "SHIPPED":
                    handleOrderShipped(event);
                    break;
                case "DELIVERED":
                    handleOrderDelivered(event);
                    break;
                case "CANCELLED":
                    handleOrderCancelled(event);
                    break;
                default:
                    log.warn("Unknown order event type: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process user activity events.
     */
    @KafkaListener(topics = KafkaConfig.USER_EVENTS_TOPIC, groupId = "scalable-api-group")
    public void processUserEvent(KafkaProducer.UserEvent event) {
        try {
            log.debug("Received user event: userId={}, action={}", event.getUserId(), event.getAction());

            // Process user event (analytics, audit, etc.)
            // E.g., increment user activity counter, update last seen, etc.

        } catch (Exception e) {
            log.error("Failed to process user event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process inventory update events.
     */
    @KafkaListener(topics = KafkaConfig.INVENTORY_EVENTS_TOPIC, groupId = "scalable-api-group")
    public void processInventoryEvent(KafkaProducer.InventoryEvent event) {
        try {
            log.info("Received inventory event: productId={}, sku={}, stock: {} -> {}",
                event.getProductId(), event.getSku(), event.getOldStock(), event.getNewStock());

            // Check for low stock alerts
            if (event.getNewStock() < 10) {
                log.warn("Low stock alert: productId={}, sku={}, stock={}",
                    event.getProductId(), event.getSku(), event.getNewStock());
                // Could trigger notification or reorder
            }

        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process system events (monitoring, alerting).
     */
    @KafkaListener(topics = KafkaConfig.SYSTEM_EVENTS_TOPIC, groupId = "scalable-api-group")
    public void processSystemEvent(KafkaProducer.SystemEvent event) {
        try {
            log.info("Received system event: level={}, message={}",
                event.getLevel(), event.getMessage());

            // Process system event (logging, alerting, metrics)
            if ("ERROR".equals(event.getLevel())) {
                // Could trigger alert or notification
                log.error("System error event: {}", event.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process system event: {}", e.getMessage(), e);
        }
    }

    // Event handlers
    private void handleOrderCreated(OrderEvent event) {
        log.debug("Order created: orderId={}, amount={}", event.getOrderId(), event.getTotalAmount());
        // Analytics: Track order creation metrics
    }

    private void handleOrderPaid(OrderEvent event) {
        log.debug("Order paid: orderId={}, amount={}", event.getOrderId(), event.getTotalAmount());
        // Trigger fulfillment workflow
    }

    private void handleOrderShipped(OrderEvent event) {
        log.debug("Order shipped: orderId={}", event.getOrderId());
        // Send shipping notification to customer
    }

    private void handleOrderDelivered(OrderEvent event) {
        log.debug("Order delivered: orderId={}", event.getOrderId());
        // Request customer review
    }

    private void handleOrderCancelled(OrderEvent event) {
        log.debug("Order cancelled: orderId={}", event.getOrderId());
        // Restore inventory, process refund
    }
}
