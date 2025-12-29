package com.project.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model for API Key.
 * Clean domain object without JPA annotations.
 */
public class ApiKey {

    private Long id;
    private String keyHash;
    private Long userId;
    private String name;
    private String[] scopes;
    private RateLimitTier rateLimitTier;
    private Boolean isActive;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum RateLimitTier {
        BASIC,      // 60 requests/minute
        STANDARD,   // 300 requests/minute
        PREMIUM,    // 1000 requests/minute
        UNLIMITED   // No rate limit
    }

    // Constructors
    public ApiKey() {}

    public ApiKey(Long id, String keyHash, Long userId, String name, String[] scopes,
                  RateLimitTier rateLimitTier, Boolean isActive, LocalDateTime expiresAt,
                  LocalDateTime lastUsedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.keyHash = keyHash;
        this.userId = userId;
        this.name = name;
        this.scopes = scopes;
        this.rateLimitTier = rateLimitTier;
        this.isActive = isActive;
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
