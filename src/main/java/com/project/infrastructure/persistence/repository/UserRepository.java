package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository for User entity operations.
 * Provides custom queries for user lookup and filtering.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Find user by email (unique identifier).
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Find users by status.
     */
    List<UserEntity> findByStatus(UserEntity.UserStatus status);

    /**
     * Find active users only (optimized query using index).
     */
    @Query("SELECT u FROM UserEntity u WHERE u.status = 'ACTIVE' ORDER BY u.createdAt DESC")
    List<UserEntity> findActiveUsers();

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Search users by email or username pattern.
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<UserEntity> searchUsers(@Param("searchTerm") String searchTerm);
}
