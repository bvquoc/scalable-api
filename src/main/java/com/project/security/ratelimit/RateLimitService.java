package com.project.security.ratelimit;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.cache.CacheKeyGenerator;
import com.project.infrastructure.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Rate limiting service using Redis token bucket algorithm.
 *
 * Rate Limit Tiers:
 * - BASIC: 60 requests/minute
 * - STANDARD: 300 requests/minute
 * - PREMIUM: 1000 requests/minute
 * - UNLIMITED: No rate limit
 *
 * Algorithm: Fixed window counter with Redis atomic increment.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final CacheService cacheService;

    public RateLimitService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Check if request is allowed under rate limit.
     *
     * @param apiKey API key with rate limit tier
     * @return RateLimitResult with allowed status and remaining quota
     */
    public RateLimitResult checkRateLimit(ApiKey apiKey) {
        // UNLIMITED tier always allowed
        if (apiKey.getRateLimitTier() == ApiKey.RateLimitTier.UNLIMITED) {
            return new RateLimitResult(true, Integer.MAX_VALUE, WINDOW_DURATION.getSeconds());
        }

        // Get rate limit for tier
        int limit = getRateLimitForTier(apiKey.getRateLimitTier());

        // Get current window start (aligned to minute boundary)
        long windowStart = getCurrentWindowStart();

        // Generate cache key for this window
        String cacheKey = CacheKeyGenerator.rateLimitWindow(apiKey.getKeyHash(), windowStart);

        // Atomic increment with expiry
        Long currentCount = cacheService.incrementWithExpiry(cacheKey, WINDOW_DURATION);

        if (currentCount == null) {
            log.error("Failed to increment rate limit counter for key: {}", apiKey.getKeyHash());
            // Allow request on Redis failure (fail open)
            return new RateLimitResult(true, limit, WINDOW_DURATION.getSeconds());
        }

        // Check if limit exceeded
        boolean allowed = currentCount <= limit;
        int remaining = Math.max(0, limit - currentCount.intValue());
        long resetSeconds = WINDOW_DURATION.getSeconds() - (Instant.now().getEpochSecond() % 60);

        if (!allowed) {
            log.warn("Rate limit exceeded for API key: {} (tier: {}, count: {}/{})",
                apiKey.getName(), apiKey.getRateLimitTier(), currentCount, limit);
        }

        return new RateLimitResult(allowed, remaining, resetSeconds);
    }

    /**
     * Get rate limit for tier.
     */
    private int getRateLimitForTier(ApiKey.RateLimitTier tier) {
        return switch (tier) {
            case BASIC -> 60;
            case STANDARD -> 300;
            case PREMIUM -> 1000;
            case UNLIMITED -> Integer.MAX_VALUE;
        };
    }

    /**
     * Get current window start timestamp (aligned to minute boundary).
     * Returns Unix timestamp in seconds.
     */
    private long getCurrentWindowStart() {
        long nowSeconds = Instant.now().getEpochSecond();
        return nowSeconds - (nowSeconds % 60);
    }

    /**
     * Rate limit check result.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remaining;
        private final long resetSeconds;

        public RateLimitResult(boolean allowed, int remaining, long resetSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetSeconds = resetSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getRemaining() {
            return remaining;
        }

        public long getResetSeconds() {
            return resetSeconds;
        }
    }
}
