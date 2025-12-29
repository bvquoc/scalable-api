package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import com.project.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ApiKeyRepository.
 */
class ApiKeyRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("apikey@example.com");
        testUser.setUsername("apikeyuser");
        testUser.setStatus(UserEntity.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldSaveAndFindApiKeyByHash() {
        // Given
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash("abc123hash");
        apiKey.setUser(testUser);
        apiKey.setName("Test API Key");
        apiKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        apiKey.setIsActive(true);

        // When
        ApiKeyEntity saved = apiKeyRepository.save(apiKey);
        Optional<ApiKeyEntity> found = apiKeyRepository.findByKeyHash("abc123hash");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getKeyHash()).isEqualTo("abc123hash");
        assertThat(found.get().getName()).isEqualTo("Test API Key");
    }

    @Test
    void shouldFindApiKeysByUserId() {
        // Given
        ApiKeyEntity key1 = new ApiKeyEntity();
        key1.setKeyHash("hash1");
        key1.setUser(testUser);
        key1.setName("Key 1");
        key1.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);

        ApiKeyEntity key2 = new ApiKeyEntity();
        key2.setKeyHash("hash2");
        key2.setUser(testUser);
        key2.setName("Key 2");
        key2.setRateLimitTier(ApiKeyEntity.RateLimitTier.PREMIUM);

        apiKeyRepository.save(key1);
        apiKeyRepository.save(key2);

        // When
        List<ApiKeyEntity> keys = apiKeyRepository.findByUserId(testUser.getId());

        // Then
        assertThat(keys).hasSize(2);
        assertThat(keys).allMatch(k -> k.getUser().getId().equals(testUser.getId()));
    }

    @Test
    void shouldFindOnlyActiveApiKeys() {
        // Given
        ApiKeyEntity activeKey = new ApiKeyEntity();
        activeKey.setKeyHash("activehash");
        activeKey.setUser(testUser);
        activeKey.setName("Active Key");
        activeKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        activeKey.setIsActive(true);

        ApiKeyEntity inactiveKey = new ApiKeyEntity();
        inactiveKey.setKeyHash("inactivehash");
        inactiveKey.setUser(testUser);
        inactiveKey.setName("Inactive Key");
        inactiveKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        inactiveKey.setIsActive(false);

        apiKeyRepository.save(activeKey);
        apiKeyRepository.save(inactiveKey);

        // When
        List<ApiKeyEntity> activeKeys = apiKeyRepository.findActiveByUserId(testUser.getId());

        // Then
        assertThat(activeKeys).hasSize(1);
        assertThat(activeKeys.get(0).getIsActive()).isTrue();
        assertThat(activeKeys.get(0).getKeyHash()).isEqualTo("activehash");
    }

    @Test
    void shouldFindExpiringSoonKeys() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime nextMonth = now.plusDays(30);

        ApiKeyEntity expiringSoon = new ApiKeyEntity();
        expiringSoon.setKeyHash("expiringsoon");
        expiringSoon.setUser(testUser);
        expiringSoon.setName("Expiring Soon");
        expiringSoon.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        expiringSoon.setIsActive(true);
        expiringSoon.setExpiresAt(tomorrow);

        ApiKeyEntity expiringLater = new ApiKeyEntity();
        expiringLater.setKeyHash("expiringlater");
        expiringLater.setUser(testUser);
        expiringLater.setName("Expiring Later");
        expiringLater.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        expiringLater.setIsActive(true);
        expiringLater.setExpiresAt(nextMonth);

        apiKeyRepository.save(expiringSoon);
        apiKeyRepository.save(expiringLater);

        // When
        List<ApiKeyEntity> expiring = apiKeyRepository.findExpiringSoon(now, now.plusDays(7));

        // Then
        assertThat(expiring).hasSize(1);
        assertThat(expiring.get(0).getKeyHash()).isEqualTo("expiringsoon");
    }

    @Test
    void shouldUpdateLastUsedAt() {
        // Given
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash("updatehash");
        apiKey.setUser(testUser);
        apiKey.setName("Update Test");
        apiKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);

        LocalDateTime timestamp = LocalDateTime.now();

        // When
        apiKeyRepository.updateLastUsedAt("updatehash", timestamp);
        apiKeyRepository.flush(); // Force database sync

        Optional<ApiKeyEntity> updated = apiKeyRepository.findByKeyHash("updatehash");

        // Then
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldCheckKeyHashExists() {
        // Given
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash("existshash");
        apiKey.setUser(testUser);
        apiKey.setName("Exists Test");
        apiKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        apiKeyRepository.save(apiKey);

        // When
        boolean exists = apiKeyRepository.existsByKeyHash("existshash");
        boolean notExists = apiKeyRepository.existsByKeyHash("notexistshash");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCascadeDeleteWhenUserDeleted() {
        // Given
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash("cascadehash");
        apiKey.setUser(testUser);
        apiKey.setName("Cascade Test");
        apiKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.BASIC);
        apiKeyRepository.save(apiKey);

        // When
        userRepository.delete(testUser);
        userRepository.flush();

        // Then
        Optional<ApiKeyEntity> found = apiKeyRepository.findByKeyHash("cascadehash");
        assertThat(found).isEmpty();
    }
}
