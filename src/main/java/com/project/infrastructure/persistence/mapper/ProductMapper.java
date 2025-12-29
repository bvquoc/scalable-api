package com.project.infrastructure.persistence.mapper;

import com.project.domain.model.Product;
import com.project.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between ProductEntity and Product domain model.
 */
@Component
public class ProductMapper {

    /**
     * Convert ProductEntity to Product domain model.
     */
    public Product toDomain(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        Product product = new Product();
        product.setId(entity.getId());
        product.setName(entity.getName());
        product.setDescription(entity.getDescription());
        product.setSku(entity.getSku());
        product.setPrice(entity.getPrice());
        product.setStockQuantity(entity.getStockQuantity());
        product.setCategory(entity.getCategory());
        product.setIsActive(entity.getIsActive());
        product.setCreatedAt(entity.getCreatedAt());
        product.setUpdatedAt(entity.getUpdatedAt());

        return product;
    }

    /**
     * Convert Product domain model to ProductEntity.
     */
    public ProductEntity toEntity(Product product) {
        if (product == null) {
            return null;
        }

        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setSku(product.getSku());
        entity.setPrice(product.getPrice());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setCategory(product.getCategory());
        entity.setIsActive(product.getIsActive());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());

        return entity;
    }

    /**
     * Update existing entity with domain model data.
     */
    public void updateEntity(ProductEntity entity, Product product) {
        if (entity == null || product == null) {
            return;
        }

        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setCategory(product.getCategory());
        entity.setIsActive(product.getIsActive());
    }
}
