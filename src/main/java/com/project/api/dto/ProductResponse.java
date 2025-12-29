package com.project.api.dto;

import com.project.domain.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Product.
 */
@Schema(description = "Product response data")
public class ProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product name", example = "Wireless Mouse")
    private String name;

    @Schema(description = "Product description", example = "Ergonomic wireless mouse with USB receiver")
    private String description;

    @Schema(description = "Stock Keeping Unit", example = "WM-001")
    private String sku;

    @Schema(description = "Product price", example = "29.99")
    private BigDecimal price;

    @Schema(description = "Available stock quantity", example = "100")
    private Integer stockQuantity;

    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Schema(description = "Whether product is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp", example = "2025-12-29T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-12-29T12:00:00")
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setSku(product.getSku());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setCategory(product.getCategory());
        response.setIsActive(product.getIsActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
