package com.project.api.dto;

import com.project.domain.model.Order;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Order.
 */
@Schema(description = "Order response data")
public class OrderResponse {

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "User ID who placed the order", example = "1")
    private Long userId;

    @Schema(description = "Unique order number", example = "ORD-20251229-1234")
    private String orderNumber;

    @Schema(description = "Order status", example = "PENDING", allowableValues = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"})
    private String status;

    @Schema(description = "Total order amount", example = "149.99")
    private BigDecimal totalAmount;

    @Schema(description = "Shipping address", example = "123 Main St, City, Country")
    private String shippingAddress;

    @Schema(description = "Order creation timestamp", example = "2025-12-29T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-12-29T12:00:00")
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingAddress(order.getShippingAddress());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
