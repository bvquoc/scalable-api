package com.project.security.ratelimit;

import com.project.infrastructure.persistence.entity.ApiKeyEntity;
import com.project.infrastructure.persistence.entity.UserEntity;
import com.project.infrastructure.persistence.repository.ApiKeyRepository;
import com.project.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting.
 * Tests rate limit enforcement with different tiers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clear database
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("ratelimit-test@example.com");
        testUser.setUsername("ratelimittest");
        testUser.setStatus(UserEntity.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldEnforceBasicTierRateLimit() throws Exception {
        // BASIC tier: 60 requests/minute
        String apiKey = createApiKey("basic-key", ApiKeyEntity.RateLimitTier.BASIC);

        // First request should succeed
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "60"))
                .andExpect(header().string("X-RateLimit-Remaining", "59"));

        // Make 60 more requests (total 61)
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/test/protected")
                    .header("X-API-Key", apiKey));
        }

        // 62nd request should be rate limited
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limit_exceeded"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    void shouldEnforceStandardTierRateLimit() throws Exception {
        // STANDARD tier: 300 requests/minute
        String apiKey = createApiKey("standard-key", ApiKeyEntity.RateLimitTier.STANDARD);

        // First request should have correct limit
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "300"))
                .andExpect(header().string("X-RateLimit-Remaining", "299"));
    }

    @Test
    void shouldEnforcePremiumTierRateLimit() throws Exception {
        // PREMIUM tier: 1000 requests/minute
        String apiKey = createApiKey("premium-key", ApiKeyEntity.RateLimitTier.PREMIUM);

        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "1000"))
                .andExpect(header().string("X-RateLimit-Remaining", "999"));
    }

    @Test
    void shouldNotRateLimitUnlimitedTier() throws Exception {
        // UNLIMITED tier: No rate limit
        String apiKey = createApiKey("unlimited-key", ApiKeyEntity.RateLimitTier.UNLIMITED);

        // Make multiple requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/test/protected")
                    .header("X-API-Key", apiKey))
                    .andExpect(status().isOk());
        }

        // All should succeed
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIsolateRateLimitsBetweenKeys() throws Exception {
        String apiKey1 = createApiKey("isolated-key-1", ApiKeyEntity.RateLimitTier.BASIC);
        String apiKey2 = createApiKey("isolated-key-2", ApiKeyEntity.RateLimitTier.BASIC);

        // Use key1 once
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey1))
                .andExpect(header().string("X-RateLimit-Remaining", "59"));

        // Key2 should have fresh limit
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", apiKey2))
                .andExpect(header().string("X-RateLimit-Remaining", "59"));
    }

    private String createApiKey(String plainKey, ApiKeyEntity.RateLimitTier tier) throws Exception {
        String keyHash = hashApiKey(plainKey);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash(keyHash);
        apiKey.setUser(testUser);
        apiKey.setName("Test Key - " + tier);
        apiKey.setRateLimitTier(tier);
        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);

        return plainKey;
    }

    private String hashApiKey(String apiKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash));
    }
}
