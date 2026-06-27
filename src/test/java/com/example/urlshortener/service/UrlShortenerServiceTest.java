package com.example.urlshortener.service;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.ReservedAliasException;
import com.example.urlshortener.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end behaviour of creation, resolution, metadata and validation. */
class UrlShortenerServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UrlShortenerService service;

    @Test
    void createsUniqueGeneratedAlias() {
        UrlMapping mapping = service.createShortUrl("https://example.com/a", null);
        assertThat(mapping.getShortCode()).isNotBlank();
        assertThat(mapping.getLongUrl()).isEqualTo("https://example.com/a");
        assertThat(mapping.getCreatedAt()).isNotNull();
        assertThat(mapping.getHitCount()).isZero();
        assertThat(mapping.getLastAccessedAt()).isNull();
    }

    @Test
    void honoursCustomAlias() {
        UrlMapping mapping = service.createShortUrl("https://example.com/b", "mybrand");
        assertThat(mapping.getShortCode()).isEqualTo("mybrand");
    }

    @Test
    void rejectsReservedAlias() {
        assertThatThrownBy(() -> service.createShortUrl("https://example.com/c", "api"))
                .isInstanceOf(ReservedAliasException.class);
    }

    @Test
    void recordsAccessMetadata() {
        UrlMapping created = service.createShortUrl("https://example.com/d", "metadata");

        assertThat(service.resolve("metadata")).isEqualTo("https://example.com/d");
        service.recordHit("metadata");
        service.recordHit("metadata");

        UrlMapping after = service.getMapping("metadata");
        assertThat(after.getHitCount()).isEqualTo(2);
        assertThat(after.getLastAccessedAt()).isNotNull();
        assertThat(after.getId()).isEqualTo(created.getId());
    }

    @Test
    void unknownAliasResolvesToNotFound() {
        assertThatThrownBy(() -> service.resolve("does-not-exist"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
