package com.project.infrastructure.cache;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import com.project.infrastructure.persistence.mapper.ApiKeyMapper;
import com.project.infrastructure.persistence.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cache service for API keys with cache-aside pattern.
 * Provides high-performance API key lookups for authentication.
 *
 * Cache Strategy:
 * - TTL: 15 minutes (balance between freshness and hit rate)
 * - Pattern: Cache-aside (lazy loading)
 * - Invalidation: On update/delete operations
 * - Target hit rate: >90%
 */
@Service
public class ApiKeyCacheService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyCacheService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final CacheService cacheService;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyCacheService(
            CacheService cacheService,
            ApiKeyRepository apiKeyRepository,
            ApiKeyMapper apiKeyMapper) {
        this.cacheService = cacheService;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyMapper = apiKeyMapper;
    }

    /**
     * Find API key by hash with cache-aside pattern.
     *
     * Flow:
     * 1. Check cache
     * 2. If cache hit, return cached value
     * 3. If cache miss, query database
     * 4. Store result in cache
     * 5. Return value
     *
     * @param keyHash SHA-256 hash of API key
     * @return Optional containing ApiKey domain model
     */
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        String cacheKey = CacheKeyGenerator.apiKeyByHash(keyHash);

        // 1. Check cache first (cache-aside pattern)
        Optional<ApiKey> cached = cacheService.get(cacheKey, ApiKey.class);
        if (cached.isPresent()) {
            log.debug("API key cache HIT for hash: {}", keyHash);
            return cached;
        }

        // 2. Cache miss - query database
        log.debug("API key cache MISS for hash: {}", keyHash);
        Optional<ApiKeyEntity> entity = apiKeyRepository.findByKeyHash(keyHash);

        if (entity.isEmpty()) {
            log.debug("API key not found in database: {}", keyHash);
            return Optional.empty();
        }

        // 3. Convert to domain model
        ApiKey apiKey = apiKeyMapper.toDomain(entity.get());

        // 4. Store in cache only if active and not expired
        if (shouldCache(apiKey)) {
            cacheService.set(cacheKey, apiKey, CACHE_TTL);
            log.debug("Cached API key: {} (TTL: {})", keyHash, CACHE_TTL);
        } else {
            log.debug("Skipped caching inactive/expired API key: {}", keyHash);
        }

        return Optional.of(apiKey);
    }

    /**
     * Invalidate cache for API key.
     * Called when API key is updated or deleted.
     *
     * @param keyHash SHA-256 hash of API key
     */
    public void invalidate(String keyHash) {
        String cacheKey = CacheKeyGenerator.apiKeyByHash(keyHash);
        boolean deleted = cacheService.delete(cacheKey);

        if (deleted) {
            log.info("Invalidated cache for API key: {}", keyHash);
        } else {
            log.debug("No cache entry to invalidate for API key: {}", keyHash);
        }
    }

    /**
     * Update last used timestamp for API key.
     * Updates database and invalidates cache.
     *
     * @param keyHash SHA-256 hash of API key
     */
    @Transactional
    public void updateLastUsedAt(String keyHash) {
        LocalDateTime now = LocalDateTime.now();

        // Update database
        apiKeyRepository.updateLastUsedAt(keyHash, now);

        // Invalidate cache to force refresh on next lookup
        invalidate(keyHash);

        log.debug("Updated last_used_at for API key: {}", keyHash);
    }

    /**
     * Warm cache with frequently used API keys.
     * Can be called on application startup or scheduled.
     *
     * @param keyHashes List of API key hashes to warm
     */
    public void warmCache(Iterable<String> keyHashes) {
        int warmed = 0;
        for (String keyHash : keyHashes) {
            Optional<ApiKey> apiKey = findByKeyHash(keyHash);
            if (apiKey.isPresent()) {
                warmed++;
            }
        }
        log.info("Warmed cache with {} API keys", warmed);
    }

    /**
     * Check if API key should be cached.
     * Only cache active, non-expired keys.
     */
    private boolean shouldCache(ApiKey apiKey) {
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            return false;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }
}
