package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity operations.
 * Provides queries for order management and user order history.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Find order by order number (unique identifier).
     */
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    /**
     * Find orders by user (paginated).
     */
    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * Find orders by status.
     */
    List<OrderEntity> findByStatus(OrderEntity.OrderStatus status);

    /**
     * Find user orders by status (uses composite index).
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.user.id = :userId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<OrderEntity> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") OrderEntity.OrderStatus status
    );

    /**
     * Find recent orders (last N days).
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<OrderEntity> findRecentOrders(@Param("since") LocalDateTime since);

    /**
     * Find pending orders older than threshold (for automated processing).
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = 'PENDING' AND o.createdAt < :threshold")
    List<OrderEntity> findStalePendingOrders(@Param("threshold") LocalDateTime threshold);

    /**
     * Count orders by user.
     */
    long countByUserId(Long userId);

    /**
     * Check if order number exists.
     */
    boolean existsByOrderNumber(String orderNumber);
}
