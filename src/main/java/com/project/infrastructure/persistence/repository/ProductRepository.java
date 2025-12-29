package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity operations.
 * Provides queries for product catalog and inventory management.
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * Find product by SKU (unique identifier).
     */
    Optional<ProductEntity> findBySku(String sku);

    /**
     * Find active products (uses partial index).
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<ProductEntity> findActiveProducts();

    /**
     * Find products by category (paginated).
     */
    Page<ProductEntity> findByCategory(String category, Pageable pageable);

    /**
     * Find active products by category (uses composite conditions).
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.category = :category AND p.isActive = true")
    List<ProductEntity> findActiveByCategoryQuery(@Param("category") String category);

    /**
     * Find products with low stock (below threshold).
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.stockQuantity < :threshold AND p.isActive = true")
    List<ProductEntity> findLowStockProducts(@Param("threshold") Integer threshold);

    /**
     * Search products by name or SKU pattern.
     */
    @Query("SELECT p FROM ProductEntity p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ProductEntity> searchProducts(@Param("searchTerm") String searchTerm);

    /**
     * Check if SKU exists.
     */
    boolean existsBySku(String sku);
}
