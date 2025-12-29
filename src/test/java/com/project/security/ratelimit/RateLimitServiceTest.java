package com.project.security.ratelimit;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RateLimitService.
 * Tests rate limiting logic with mocked cache.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private CacheService cacheService;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(cacheService);
    }

    @Test
    void shouldAllowRequestUnderLimit() {
        // Given
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.BASIC);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class)))
                .thenReturn(30L); // 30th request out of 60

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(30); // 60 - 30 = 30 remaining
    }

    @Test
    void shouldDenyRequestOverLimit() {
        // Given
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.BASIC);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class)))
                .thenReturn(61L); // 61st request out of 60

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        // Then
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    void shouldAlwaysAllowUnlimitedTier() {
        // Given
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.UNLIMITED);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void shouldHandleBasicTierLimit() {
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.BASIC);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class))).thenReturn(1L);

        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(59); // 60 - 1
    }

    @Test
    void shouldHandleStandardTierLimit() {
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.STANDARD);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class))).thenReturn(1L);

        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(299); // 300 - 1
    }

    @Test
    void shouldHandlePremiumTierLimit() {
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.PREMIUM);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class))).thenReturn(1L);

        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(999); // 1000 - 1
    }

    @Test
    void shouldFailOpenOnRedisError() {
        // Given
        ApiKey apiKey = createApiKey(ApiKey.RateLimitTier.BASIC);
        when(cacheService.incrementWithExpiry(anyString(), any(Duration.class)))
                .thenReturn(null); // Redis failure

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        // Then - Should allow request (fail open)
        assertThat(result.isAllowed()).isTrue();
    }

    private ApiKey createApiKey(ApiKey.RateLimitTier tier) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash("test-hash");
        apiKey.setName("Test Key");
        apiKey.setRateLimitTier(tier);
        apiKey.setIsActive(true);
        return apiKey;
    }
}
