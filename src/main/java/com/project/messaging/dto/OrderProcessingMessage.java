package com.project.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message DTO for order processing tasks.
 * Sent to RabbitMQ queue for async order fulfillment.
 */
public class OrderProcessingMessage {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;

    public OrderProcessingMessage() {}

    public OrderProcessingMessage(Long orderId, String orderNumber, Long userId,
                                  BigDecimal totalAmount, String shippingAddress,
                                  LocalDateTime createdAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.createdAt = createdAt;
    }

    // Getters and setters
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

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
