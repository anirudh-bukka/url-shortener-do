package com.example.urlshortener.service;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.config.CacheConfig;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.ReservedAliasException;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core business logic for creating aliases and resolving redirects.
 *
 * <h2>How concurrent creation stays safe</h2>
 * <ul>
 *   <li><b>Auto-generated aliases</b> draw a unique number from a database
 *       sequence and Base62-encode it. The sequence never returns the same value
 *       twice, so two parallel requests can never generate the same alias — no
 *       shared mutable state, no lock to contend on. In the rare case a generated
 *       code happens to equal an existing <i>custom</i> alias, the insert is
 *       retried with the next sequence value (each attempt in its own
 *       transaction via {@link UrlMappingWriter}).</li>
 *   <li><b>Custom aliases</b> rely on the UNIQUE constraint on
 *       {@code short_code}. If two requests race for the same custom alias, the
 *       database lets exactly one INSERT succeed; the loser receives a
 *       {@link DataIntegrityViolationException} which we translate into a clean
 *       409 Conflict.</li>
 * </ul>
 */
@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    /** Safety bound on retries when a generated code collides with a custom alias. */
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlMappingRepository repository;
    private final UrlMappingWriter writer;
    private final Set<String> reservedPaths;

    public UrlShortenerService(UrlMappingRepository repository,
                               UrlMappingWriter writer,
                               AppProperties properties) {
        this.repository = repository;
        this.writer = writer;
        this.reservedPaths = parseReserved(properties.reservedPaths());
    }

    /**
     * Create (or reuse) a short alias for {@code longUrl}.
     *
     * @param longUrl     the destination to shorten (already format-validated by the DTO)
     * @param customAlias optional caller-chosen alias; {@code null}/blank means auto-generate
     * @return the persisted mapping
     */
    public UrlMapping createShortUrl(String longUrl, String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            return createWithCustomAlias(longUrl, customAlias.trim());
        }
        return createWithGeneratedAlias(longUrl);
    }

    private UrlMapping createWithCustomAlias(String longUrl, String alias) {
        if (reservedPaths.contains(alias.toLowerCase())) {
            throw new ReservedAliasException(alias);
        }
        long id = repository.nextSequenceValue();
        try {
            return writer.insert(id, alias, longUrl);
        } catch (DataIntegrityViolationException ex) {
            // The UNIQUE constraint rejected a concurrent/existing duplicate.
            // Expected outcome of a race — surfaced as a clean 409 Conflict.
            throw new AliasAlreadyExistsException(alias);
        }
    }

    private UrlMapping createWithGeneratedAlias(String longUrl) {
        // Best-effort deduplication: hand back an existing alias for a URL we've
        // already shortened. (Under a true tie two short links may exist for the
        // same long URL; that is harmless — both resolve correctly.)
        Optional<UrlMapping> existing = repository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            long id = repository.nextSequenceValue();
            String shortCode = Base62.encode(id);
            try {
                return writer.insert(id, shortCode, longUrl);
            } catch (DataIntegrityViolationException ex) {
                // Generated code collided with an existing custom alias. Each
                // attempt ran in its own transaction, so we can simply retry.
                log.warn("Generated alias '{}' collided (attempt {}/{}), retrying",
                        shortCode, attempt, MAX_GENERATION_ATTEMPTS);
            }
        }
        throw new IllegalStateException(
                "Unable to generate a unique alias after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Resolve an alias to its long URL for redirection. Cached because the
     * redirect path is read-dominated and latency-sensitive; the mapping is
     * immutable once created, so the cache never serves stale destinations.
     */
    @Cacheable(cacheNames = CacheConfig.URL_CACHE, key = "#shortCode")
    @Transactional(readOnly = true)
    public String resolve(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(UrlMapping::getLongUrl)
                .orElseThrow(() -> new ResourceNotFoundException("No URL found for alias: " + shortCode));
    }

    /** Fetch full metadata for an alias (used by the inspection endpoint). */
    @Transactional(readOnly = true)
    public UrlMapping getMapping(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("No URL found for alias: " + shortCode));
    }

    /**
     * Record an access: atomically bump the hit counter and last-accessed time.
     * The single {@code UPDATE ... SET hit_count = hit_count + 1} is performed by
     * the database, so concurrent redirects increment safely without read-modify-write
     * races or application-level locking.
     */
    @Transactional
    public void recordHit(String shortCode) {
        repository.incrementHitCount(shortCode, Instant.now());
    }

    private static Set<String> parseReserved(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }
}
