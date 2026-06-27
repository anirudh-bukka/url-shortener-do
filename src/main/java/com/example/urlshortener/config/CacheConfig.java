package com.example.urlshortener.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
