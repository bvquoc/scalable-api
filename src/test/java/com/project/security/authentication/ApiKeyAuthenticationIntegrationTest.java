package com.project.security.authentication;

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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API key authentication.
 * Tests complete authentication flow with real Spring Security filters.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserEntity testUser;
    private String plainApiKey;
    private String keyHash;

    @BeforeEach
    void setUp() throws Exception {
        // Clear database
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("auth-test@example.com");
        testUser.setUsername("authtest");
        testUser.setStatus(UserEntity.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        // Create API key
        plainApiKey = "test-api-key-12345";
        keyHash = hashApiKey(plainApiKey);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash(keyHash);
        apiKey.setUser(testUser);
        apiKey.setName("Test API Key");
        apiKey.setRateLimitTier(ApiKeyEntity.RateLimitTier.PREMIUM);
        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);
    }

    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", plainApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully authenticated"))
                .andExpect(jsonPath("$.apiKeyName").value("Test API Key"))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    void shouldRejectRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", "invalid-api-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInactiveApiKey() throws Exception {
        // Deactivate API key
        ApiKeyEntity apiKey = apiKeyRepository.findByKeyHash(keyHash).orElseThrow();
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);

        // Clear cache to force DB lookup
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", plainApiKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpiredApiKey() throws Exception {
        // Expire API key
        ApiKeyEntity apiKey = apiKeyRepository.findByKeyHash(keyHash).orElseThrow();
        apiKey.setExpiresAt(LocalDateTime.now().minusDays(1));
        apiKeyRepository.save(apiKey);

        // Clear cache
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        mockMvc.perform(get("/api/test/protected")
                .header("X-API-Key", plainApiKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPublicEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("Scalable API"));
    }

    private String hashApiKey(String apiKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash));
    }
}
