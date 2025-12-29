package com.project.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generic cache service with cache-aside pattern.
 * Provides reusable caching operations for all domain models.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get value from cache by key.
     *
     * @param key Cache key
     * @param type Expected value type
     * @return Optional containing cached value, or empty if not found
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                log.debug("Cache HIT: {}", key);
                return Optional.of(type.cast(value));
            }
            log.debug("Cache MISS: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Cache GET error for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Set value in cache with TTL.
     *
     * @param key Cache key
     * @param value Value to cache
     * @param ttl Time to live
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Cache SET: {} (TTL: {})", key, ttl);
        } catch (Exception e) {
            log.error("Cache SET error for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Set value in cache without expiration.
     *
     * @param key Cache key
     * @param value Value to cache
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cache SET: {} (no TTL)", key);
        } catch (Exception e) {
            log.error("Cache SET error for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Delete value from cache.
     *
     * @param key Cache key
     * @return true if deleted, false otherwise
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Cache DELETE: {}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Cache DELETE error for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Check if key exists in cache.
     *
     * @param key Cache key
     * @return true if exists, false otherwise
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Cache EXISTS error for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Set TTL for existing key.
     *
     * @param key Cache key
     * @param ttl Time to live
     * @return true if TTL set, false otherwise
     */
    public boolean expire(String key, Duration ttl) {
        try {
            Boolean result = redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Cache EXPIRE error for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Increment numeric value in cache (atomic operation).
     * Creates key with value 1 if it doesn't exist.
     *
     * @param key Cache key
     * @return New value after increment
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Cache INCREMENT error for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Increment numeric value with expiration (for rate limiting).
     *
     * @param key Cache key
     * @param ttl Time to live
     * @return New value after increment
     */
    public Long incrementWithExpiry(String key, Duration ttl) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1) {
                // First increment, set TTL
                redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            }
            return value;
        } catch (Exception e) {
            log.error("Cache INCREMENT error for key {}: {}", key, e.getMessage());
            return null;
        }
    }
}
