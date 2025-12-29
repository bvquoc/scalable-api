package com.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating product stock quantity.
 */
@Schema(description = "Request body for updating product stock")
public class UpdateStockRequest {

    @Schema(description = "New stock quantity", example = "50", required = true, minimum = "0")
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be >= 0")
    private Integer stockQuantity;

    public UpdateStockRequest() {}

    public UpdateStockRequest(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
