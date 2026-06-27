package com.example.urlshortener.repository;

import com.example.urlshortener.domain.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Data-access layer for {@link UrlMapping}. Spring Data implements this
 * interface at runtime, so no boilerplate JDBC/JPA code is needed.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /** Resolve an alias on the redirect hot path. */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /** Used to deduplicate: return an existing alias for an already-shortened URL. */
    Optional<UrlMapping> findByLongUrl(String longUrl);

    /**
     * Atomically fetch the next value from the database sequence.
     *
     * <p>This is the cornerstone of concurrency-safe alias generation: the
     * database guarantees every caller receives a distinct number even under
     * heavy parallel load, so the Base62 encoding of that number is unique by
     * construction — no locking or retry loop required.
     */
    @Query(value = "SELECT nextval('url_mapping_seq')", nativeQuery = true)
    long nextSequenceValue();

    /**
     * Atomically bump the hit counter and last-accessed timestamp in a single
     * UPDATE. Because the increment happens in the database (not read-modify-write
     * in Java), concurrent redirects count correctly without locks.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.hitCount = u.hitCount + 1, u.lastAccessedAt = :accessedAt "
            + "WHERE u.shortCode = :shortCode")
    void incrementHitCount(@Param("shortCode") String shortCode, @Param("accessedAt") Instant accessedAt);
}
