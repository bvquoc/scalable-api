package com.project.infrastructure.cache;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import com.project.infrastructure.persistence.entity.UserEntity;
import com.project.infrastructure.persistence.repository.ApiKeyRepository;
import com.project.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ApiKeyCacheService.
 * Tests cache-aside pattern with real Redis and PostgreSQL.
 */
class ApiKeyCacheServiceIntegrationTest extends BaseRedisTest {

    @Autowired
    private ApiKeyCacheService apiKeyCacheService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheService cacheService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Clear database
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("cache@example.com");
        testUser.setUsername("cacheuser");
        testUser.setStatus(UserEntity.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCacheMissAndLoadFromDatabase() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash001", "Test Key");
        apiKeyRepository.save(entity);

        // When - First call (cache miss)
        Optional<ApiKey> result1 = apiKeyCacheService.findByKeyHash("hash001");

        // Then
        assertThat(result1).isPresent();
        assertThat(result1.get().getKeyHash()).isEqualTo("hash001");

        // Verify it's now cached
        String cacheKey = CacheKeyGenerator.apiKeyByHash("hash001");
        assertThat(cacheService.exists(cacheKey)).isTrue();
    }

    @Test
    void shouldCacheHitOnSecondCall() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash002", "Cached Key");
        apiKeyRepository.save(entity);

        // When - First call loads from DB
        apiKeyCacheService.findByKeyHash("hash002");

        // Second call should hit cache (verify by deleting from DB)
        apiKeyRepository.deleteAll();

        Optional<ApiKey> result = apiKeyCacheService.findByKeyHash("hash002");

        // Then - Still get result from cache even though DB is empty
        assertThat(result).isPresent();
        assertThat(result.get().getKeyHash()).isEqualTo("hash002");
    }

    @Test
    void shouldNotCacheInactiveKeys() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash003", "Inactive Key");
        entity.setIsActive(false);
        apiKeyRepository.save(entity);

        // When
        apiKeyCacheService.findByKeyHash("hash003");

        // Then - Should not be cached
        String cacheKey = CacheKeyGenerator.apiKeyByHash("hash003");
        assertThat(cacheService.exists(cacheKey)).isFalse();
    }

    @Test
    void shouldNotCacheExpiredKeys() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash004", "Expired Key");
        entity.setExpiresAt(LocalDateTime.now().minusDays(1)); // Already expired
        apiKeyRepository.save(entity);

        // When
        apiKeyCacheService.findByKeyHash("hash004");

        // Then - Should not be cached
        String cacheKey = CacheKeyGenerator.apiKeyByHash("hash004");
        assertThat(cacheService.exists(cacheKey)).isFalse();
    }

    @Test
    void shouldInvalidateCache() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash005", "Invalidate Test");
        apiKeyRepository.save(entity);

        // Cache the key
        apiKeyCacheService.findByKeyHash("hash005");
        String cacheKey = CacheKeyGenerator.apiKeyByHash("hash005");
        assertThat(cacheService.exists(cacheKey)).isTrue();

        // When
        apiKeyCacheService.invalidate("hash005");

        // Then
        assertThat(cacheService.exists(cacheKey)).isFalse();
    }

    @Test
    void shouldUpdateLastUsedAtAndInvalidate() {
        // Given
        ApiKeyEntity entity = createApiKeyEntity("hash006", "Last Used Test");
        entity = apiKeyRepository.save(entity);
        Long entityId = entity.getId();

        // Cache the key
        apiKeyCacheService.findByKeyHash("hash006");

        // When
        apiKeyCacheService.updateLastUsedAt("hash006");

        // Then
        // Cache should be invalidated
        String cacheKey = CacheKeyGenerator.apiKeyByHash("hash006");
        assertThat(cacheService.exists(cacheKey)).isFalse();

        // Database should be updated
        Optional<ApiKeyEntity> updated = apiKeyRepository.findById(entityId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldWarmCache() {
        // Given
        createAndSaveApiKey("warm001", "Warm 1");
        createAndSaveApiKey("warm002", "Warm 2");
        createAndSaveApiKey("warm003", "Warm 3");

        // When
        apiKeyCacheService.warmCache(java.util.List.of("warm001", "warm002", "warm003"));

        // Then - All should be cached
        assertThat(cacheService.exists(CacheKeyGenerator.apiKeyByHash("warm001"))).isTrue();
        assertThat(cacheService.exists(CacheKeyGenerator.apiKeyByHash("warm002"))).isTrue();
        assertThat(cacheService.exists(CacheKeyGenerator.apiKeyByHash("warm003"))).isTrue();
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        // When
        Optional<ApiKey> result = apiKeyCacheService.findByKeyHash("nonexistent");

        // Then
        assertThat(result).isEmpty();

        // Should not be cached
        String cacheKey = CacheKeyGenerator.apiKeyByHash("nonexistent");
        assertThat(cacheService.exists(cacheKey)).isFalse();
    }

    private ApiKeyEntity createApiKeyEntity(String keyHash, String name) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setKeyHash(keyHash);
        entity.setUser(testUser);
        entity.setName(name);
        entity.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        entity.setIsActive(true);
        return entity;
    }

    private void createAndSaveApiKey(String keyHash, String name) {
        ApiKeyEntity entity = createApiKeyEntity(keyHash, name);
        apiKeyRepository.save(entity);
    }
}
