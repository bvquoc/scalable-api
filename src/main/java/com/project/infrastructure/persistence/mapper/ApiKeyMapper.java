package com.project.infrastructure.persistence.mapper;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between ApiKeyEntity and ApiKey domain model.
 */
@Component
public class ApiKeyMapper {

    /**
     * Convert ApiKeyEntity to ApiKey domain model.
     */
    public ApiKey toDomain(ApiKeyEntity entity) {
        if (entity == null) {
            return null;
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setId(entity.getId());
        apiKey.setKeyHash(entity.getKeyHash());
        apiKey.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        apiKey.setName(entity.getName());
        apiKey.setScopes(entity.getScopes());
        apiKey.setRateLimitTier(mapTier(entity.getRateLimitTier()));
        apiKey.setIsActive(entity.getIsActive());
        apiKey.setExpiresAt(entity.getExpiresAt());
        apiKey.setLastUsedAt(entity.getLastUsedAt());
        apiKey.setCreatedAt(entity.getCreatedAt());
        apiKey.setUpdatedAt(entity.getUpdatedAt());

        return apiKey;
    }

    /**
     * Update existing entity with domain model data.
     */
    public void updateEntity(ApiKeyEntity entity, ApiKey apiKey) {
        if (entity == null || apiKey == null) {
            return;
        }

        entity.setName(apiKey.getName());
        entity.setScopes(apiKey.getScopes());
        entity.setRateLimitTier(mapTier(apiKey.getRateLimitTier()));
        entity.setIsActive(apiKey.getIsActive());
        entity.setExpiresAt(apiKey.getExpiresAt());
        entity.setLastUsedAt(apiKey.getLastUsedAt());
    }

    private ApiKey.RateLimitTier mapTier(ApiKeyEntity.RateLimitTier entityTier) {
        if (entityTier == null) {
            return null;
        }
        return ApiKey.RateLimitTier.valueOf(entityTier.name());
    }

    private ApiKeyEntity.RateLimitTier mapTier(ApiKey.RateLimitTier domainTier) {
        if (domainTier == null) {
            return null;
        }
        return ApiKeyEntity.RateLimitTier.valueOf(domainTier.name());
    }
}
