package com.project.infrastructure.cache;

import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import com.project.infrastructure.persistence.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cache warmer to pre-populate cache on application startup.
 * Improves initial response times by caching frequently used data.
 */
@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);

    private final ApiKeyCacheService apiKeyCacheService;
    private final ApiKeyRepository apiKeyRepository;

    public CacheWarmer(
            ApiKeyCacheService apiKeyCacheService,
            ApiKeyRepository apiKeyRepository) {
        this.apiKeyCacheService = apiKeyCacheService;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Warm cache on application startup.
     * Loads active API keys into cache.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("Starting cache warming...");

        try {
            // Find all active API keys
            List<String> activeKeyHashes = apiKeyRepository.findAll().stream()
                    .filter(ApiKeyEntity::getIsActive)
                    .map(ApiKeyEntity::getKeyHash)
                    .collect(Collectors.toList());

            // Warm cache with active keys
            apiKeyCacheService.warmCache(activeKeyHashes);

            log.info("Cache warming completed. Loaded {} active API keys", activeKeyHashes.size());
        } catch (Exception e) {
            log.error("Cache warming failed: {}", e.getMessage(), e);
        }
    }
}
