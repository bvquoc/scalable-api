package com.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new product.
 */
@Schema(description = "Request body for creating a new product")
public class CreateProductRequest {

    @Schema(description = "Product name", example = "Wireless Mouse", required = true, maxLength = 255)
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Schema(description = "Product description", example = "Ergonomic wireless mouse with USB receiver")
    private String description;

    @Schema(description = "Stock Keeping Unit (unique identifier)", example = "WM-001", required = true, maxLength = 100)
    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Schema(description = "Product price", example = "29.99", required = true, minimum = "0")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be >= 0")
    private BigDecimal price;

    @Schema(description = "Available stock quantity", example = "100", required = true, minimum = "0")
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be >= 0")
    private Integer stockQuantity;

    @Schema(description = "Product category", example = "Electronics", maxLength = 50)
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
