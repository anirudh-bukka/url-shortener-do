package com.example.urlshortener.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache wiring with a graceful fallback.
 *
 * <p>When a Redis host is configured Spring Boot auto-configures a Redis-backed
 * {@link CacheManager}, which is shared across instances and ideal for the
 * read-heavy redirect path. When no Redis host is present (e.g. local dev or a
 * single small Droplet) we fall back to a simple in-process cache so the app
 * still runs without an external dependency.
 */
@Configuration
public class CacheConfig {

    /** Cache name used by the service layer for alias -> long URL lookups. */
    public static final String URL_CACHE = "shortUrls";

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(CacheManager.class)
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(URL_CACHE);
    }
}
