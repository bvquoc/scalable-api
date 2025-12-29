package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order Item entity operations.
 * Provides queries for order line items and product sales analytics.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    /**
     * Find all items in an order.
     */
    List<OrderItemEntity> findByOrderId(Long orderId);

    /**
     * Find all orders containing a specific product.
     */
    List<OrderItemEntity> findByProductId(Long productId);

    /**
     * Count total quantity sold for a product.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItemEntity oi WHERE oi.product.id = :productId")
    Long countTotalQuantitySoldForProduct(@Param("productId") Long productId);

    /**
     * Find top-selling products (by quantity).
     */
    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalQty FROM OrderItemEntity oi " +
           "GROUP BY oi.product.id ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts();
}
