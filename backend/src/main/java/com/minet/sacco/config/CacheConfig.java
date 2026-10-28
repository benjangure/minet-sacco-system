package com.minet.sacco.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration.
 *
 * Financial and membership data (loans, members, balances, reports) is NOT cached: screens must always
 * show what is in the database. The few caches that remain (user lookups, settings, notification
 * counts) expire quickly so that nothing can stay stale until a restart.
 *
 * Caches are created on demand, so a cache name can never be "missing".
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Seconds a cached entry may be reused. Override with app.cache.ttl-seconds. */
    @org.springframework.beans.factory.annotation.Value("${app.cache.ttl-seconds:20}")
    private long ttlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(2000));
        return manager;
    }
}
