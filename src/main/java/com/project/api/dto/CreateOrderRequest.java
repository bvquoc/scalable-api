package com.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new order.
 */
@Schema(description = "Request body for creating a new order")
public class CreateOrderRequest {

    @Schema(description = "User ID who is placing the order", example = "1", required = true)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Total order amount", example = "149.99", required = true, minimum = "0")
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be >= 0")
    private BigDecimal totalAmount;

    @Schema(description = "Shipping address for delivery", example = "123 Main St, City, Country", required = true)
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    // Getters and setters
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
}
