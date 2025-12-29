package com.project.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CacheService.
 * Tests basic Redis operations with real Redis instance.
 */
class CacheServiceIntegrationTest extends BaseRedisTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldSetAndGetValue() {
        // Given
        String key = "test:key";
        String value = "test value";

        // When
        cacheService.set(key, value, Duration.ofMinutes(5));
        Optional<String> retrieved = cacheService.get(key, String.class);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        // When
        Optional<String> retrieved = cacheService.get("nonexistent:key", String.class);

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldDeleteValue() {
        // Given
        String key = "test:delete";
        cacheService.set(key, "value", Duration.ofMinutes(5));

        // When
        boolean deleted = cacheService.delete(key);
        Optional<String> retrieved = cacheService.get(key, String.class);

        // Then
        assertThat(deleted).isTrue();
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldCheckKeyExists() {
        // Given
        String key = "test:exists";
        cacheService.set(key, "value", Duration.ofMinutes(5));

        // When
        boolean exists = cacheService.exists(key);
        boolean notExists = cacheService.exists("test:notexists");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldIncrementValue() {
        // Given
        String key = "test:counter";

        // When
        Long value1 = cacheService.increment(key);
        Long value2 = cacheService.increment(key);
        Long value3 = cacheService.increment(key);

        // Then
        assertThat(value1).isEqualTo(1L);
        assertThat(value2).isEqualTo(2L);
        assertThat(value3).isEqualTo(3L);
    }

    @Test
    void shouldIncrementWithExpiry() {
        // Given
        String key = "test:counter:expiry";
        Duration ttl = Duration.ofSeconds(10);

        // When
        Long value1 = cacheService.incrementWithExpiry(key, ttl);
        Long value2 = cacheService.incrementWithExpiry(key, ttl);

        // Then
        assertThat(value1).isEqualTo(1L);
        assertThat(value2).isEqualTo(2L);
        assertThat(cacheService.exists(key)).isTrue();
    }

    @Test
    void shouldSetExpiry() {
        // Given
        String key = "test:ttl";
        cacheService.set(key, "value"); // No TTL

        // When
        boolean result = cacheService.expire(key, Duration.ofMinutes(5));

        // Then
        assertThat(result).isTrue();
        assertThat(cacheService.exists(key)).isTrue();
    }

    @Test
    void shouldHandleComplexObjects() {
        // Given
        String key = "test:object";
        TestObject original = new TestObject("test", 123, true);

        // When
        cacheService.set(key, original, Duration.ofMinutes(5));
        Optional<TestObject> retrieved = cacheService.get(key, TestObject.class);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("test");
        assertThat(retrieved.get().getValue()).isEqualTo(123);
        assertThat(retrieved.get().isActive()).isTrue();
    }

    // Test helper class
    static class TestObject {
        private String name;
        private int value;
        private boolean active;

        public TestObject() {}

        public TestObject(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
