package com.project.infrastructure.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CacheKeyGenerator.
 * Tests cache key generation patterns.
 */
class CacheKeyGeneratorTest {

    @Test
    void shouldGenerateApiKeyByHashKey() {
        // When
        String key = CacheKeyGenerator.apiKeyByHash("abc123");

        // Then
        assertThat(key).isEqualTo("apikey:abc123");
    }

    @Test
    void shouldGenerateUserByIdKey() {
        // When
        String key = CacheKeyGenerator.userById(123L);

        // Then
        assertThat(key).isEqualTo("user:123");
    }

    @Test
    void shouldGenerateUserByEmailKey() {
        // When
        String key = CacheKeyGenerator.userByEmail("test@example.com");

        // Then
        assertThat(key).isEqualTo("user:email:test@example.com");
    }

    @Test
    void shouldGenerateProductBySkuKey() {
        // When
        String key = CacheKeyGenerator.productBySku("SKU-001");

        // Then
        assertThat(key).isEqualTo("product:SKU-001");
    }

    @Test
    void shouldGenerateRateLimitWindowKey() {
        // When
        String key = CacheKeyGenerator.rateLimitWindow("abc123", 1640000000L);

        // Then
        assertThat(key).isEqualTo("ratelimit:abc123:1640000000");
    }
}
