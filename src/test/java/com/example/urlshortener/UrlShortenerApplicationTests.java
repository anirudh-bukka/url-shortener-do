package com.example.urlshortener;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies the full Spring context (including Flyway migrations
 * against a real Postgres container) starts successfully.
 */
class UrlShortenerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
