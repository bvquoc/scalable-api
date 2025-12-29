package com.project.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Cache metrics for monitoring cache performance.
 * Tracks hit rate, miss rate, and latency.
 */
@Component
public class CacheMetrics {

    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheLatency;

    public CacheMetrics(MeterRegistry meterRegistry) {
        this.cacheHits = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .tag("cache", "redis")
                .register(meterRegistry);

        this.cacheMisses = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .tag("cache", "redis")
                .register(meterRegistry);

        this.cacheLatency = Timer.builder("cache.latency")
                .description("Cache operation latency")
                .tag("cache", "redis")
                .register(meterRegistry);
    }

    /**
     * Record cache hit.
     */
    public void recordHit() {
        cacheHits.increment();
    }

    /**
     * Record cache miss.
     */
    public void recordMiss() {
        cacheMisses.increment();
    }

    /**
     * Record cache operation latency.
     *
     * @param duration Duration in nanoseconds
     */
    public void recordLatency(long duration) {
        cacheLatency.record(duration, TimeUnit.NANOSECONDS);
    }

    /**
     * Get cache hit rate (0.0 to 1.0).
     *
     * @return Hit rate percentage
     */
    public double getHitRate() {
        double hits = cacheHits.count();
        double misses = cacheMisses.count();
        double total = hits + misses;

        if (total == 0) {
            return 0.0;
        }

        return hits / total;
    }
}
