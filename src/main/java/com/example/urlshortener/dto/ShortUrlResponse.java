package com.example.urlshortener.dto;

import com.example.urlshortener.domain.UrlMapping;

import java.time.Instant;

/**
 * Response body returned after creating or inspecting an alias. Surfaces all
 * available metadata for the mapping.
 *
 * @param id             internal numeric identifier
 * @param shortCode      the alias itself (e.g. "4c92")
 * @param shortUrl       the full clickable short link (baseUrl + "/" + shortCode)
 * @param longUrl        the original / final destination URL
 * @param createdAt      when the alias was created (UTC)
 * @param lastAccessedAt when it was last resolved (UTC); null if never accessed
 * @param hitCount       number of redirects served so far
 */
public record ShortUrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant lastAccessedAt,
        long hitCount
) {
    /** Map a persistence entity to its API representation. */
    public static ShortUrlResponse from(UrlMapping mapping, String baseUrl) {
        return new ShortUrlResponse(
                mapping.getId(),
                mapping.getShortCode(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getLongUrl(),
                mapping.getCreatedAt(),
                mapping.getLastAccessedAt(),
                mapping.getHitCount()
        );
    }
}
