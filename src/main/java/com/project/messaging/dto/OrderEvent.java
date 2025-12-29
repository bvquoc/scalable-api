package com.project.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event DTO for order lifecycle events.
 * Published to Kafka topic for event streaming.
 */
public class OrderEvent {

    private String eventType; // CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime timestamp;

    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderEvent(String eventType, Long orderId, String orderNumber,
                      Long userId, BigDecimal totalAmount, String status) {
        this.eventType = eventType;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
