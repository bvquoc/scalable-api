package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for API Key entity operations.
 * Provides queries for authentication and rate limiting.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    /**
     * Find API key by hash for authentication.
     * Uses index on key_hash for fast lookup.
     */
    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    /**
     * Find all API keys for a user.
     */
    List<ApiKeyEntity> findByUserId(Long userId);

    /**
     * Find active API keys for a user.
     * Uses partial index on is_active.
     */
    @Query("SELECT a FROM ApiKeyEntity a WHERE a.user.id = :userId AND a.isActive = true")
    List<ApiKeyEntity> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Find API keys expiring soon (within next 7 days).
     */
    @Query("SELECT a FROM ApiKeyEntity a WHERE a.expiresAt IS NOT NULL AND " +
           "a.expiresAt BETWEEN :now AND :expiryThreshold AND a.isActive = true")
    List<ApiKeyEntity> findExpiringSoon(
        @Param("now") LocalDateTime now,
        @Param("expiryThreshold") LocalDateTime expiryThreshold
    );

    /**
     * Update last used timestamp (called on each API request).
     */
    @Modifying
    @Query("UPDATE ApiKeyEntity a SET a.lastUsedAt = :timestamp WHERE a.keyHash = :keyHash")
    void updateLastUsedAt(@Param("keyHash") String keyHash, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Check if key hash exists.
     */
    boolean existsByKeyHash(String keyHash);
}
