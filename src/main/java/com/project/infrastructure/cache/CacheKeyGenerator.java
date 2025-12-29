package com.project.infrastructure.cache;

/**
 * Centralized cache key generation for consistent naming.
 * Pattern: {prefix}:{identifier}
 */
public class CacheKeyGenerator {

    private static final String API_KEY_PREFIX = "apikey";
    private static final String USER_PREFIX = "user";
    private static final String PRODUCT_PREFIX = "product";
    private static final String RATE_LIMIT_PREFIX = "ratelimit";

    private CacheKeyGenerator() {
        // Utility class
    }

    /**
     * Generate cache key for API key by hash.
     * Example: "apikey:abc123hash"
     */
    public static String apiKeyByHash(String keyHash) {
        return String.format("%s:%s", API_KEY_PREFIX, keyHash);
    }

    /**
     * Generate cache key for user by ID.
     * Example: "user:123"
     */
    public static String userById(Long userId) {
        return String.format("%s:%d", USER_PREFIX, userId);
    }

    /**
     * Generate cache key for user by email.
     * Example: "user:email:test@example.com"
     */
    public static String userByEmail(String email) {
        return String.format("%s:email:%s", USER_PREFIX, email);
    }

    /**
     * Generate cache key for product by SKU.
     * Example: "product:SKU-001"
     */
    public static String productBySku(String sku) {
        return String.format("%s:%s", PRODUCT_PREFIX, sku);
    }

    /**
     * Generate cache key for rate limiting.
     * Example: "ratelimit:abc123hash:1640000000"
     *
     * @param keyHash API key hash
     * @param windowStart Unix timestamp of rate limit window start (seconds)
     */
    public static String rateLimitWindow(String keyHash, long windowStart) {
        return String.format("%s:%s:%d", RATE_LIMIT_PREFIX, keyHash, windowStart);
    }
}
