package com.project.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * API Key entity for authentication and rate limiting.
 * Stores hashed API keys with associated metadata and rate limit tiers.
 */
@Entity
@Table(name = "api_keys")
public class ApiKeyEntity extends BaseEntity {

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_keys_user"))
    private UserEntity user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "scopes", columnDefinition = "text[]")
    private String[] scopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_limit_tier", nullable = false, length = 20)
    private RateLimitTier rateLimitTier = RateLimitTier.BASIC;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public enum RateLimitTier {
        BASIC,      // 60 requests/minute
        STANDARD,   // 300 requests/minute
        PREMIUM,    // 1000 requests/minute
        UNLIMITED   // No rate limit
    }

    // Getters and setters
    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public RateLimitTier getRateLimitTier() {
        return rateLimitTier;
    }

    public void setRateLimitTier(RateLimitTier rateLimitTier) {
        this.rateLimitTier = rateLimitTier;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
