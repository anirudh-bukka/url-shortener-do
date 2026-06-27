package com.example.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistent record mapping a short alias to its original long URL.
 *
 * <p>The table backing this entity (see the Flyway migration) enforces the
 * invariants that keep concurrent creation safe:
 * <ul>
 *   <li>{@code short_code} has a UNIQUE constraint, so two requests can never
 *       persist the same alias — the second insert fails fast.</li>
 *   <li>The numeric {@code id} is sourced from a database sequence, giving every
 *       auto-generated alias a globally unique, collision-free value.</li>
 * </ul>
 */
@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    /** Numeric primary key; assigned from the {@code url_mapping_seq} sequence. */
    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    /** The Base62 (or custom) alias appended to the base URL. */
    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    /** The original destination users are redirected to. */
    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Number of times the alias has been resolved (best-effort analytics). */
    @Column(name = "hit_count", nullable = false)
    private long hitCount;

    /** Timestamp of the most recent redirect; {@code null} until first accessed. */
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    protected UrlMapping() {
        // Required by JPA.
    }

    public UrlMapping(Long id, String shortCode, String longUrl) {
        this.id = id;
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = Instant.now();
        this.hitCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getHitCount() {
        return hitCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void incrementHitCount() {
        this.hitCount++;
        this.lastAccessedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UrlMapping that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
