package com.example.urlshortener.service;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs a single, isolated INSERT attempt.
 *
 * <p>Lives in its own bean (so Spring's transactional proxy applies) and uses
 * {@link Propagation#REQUIRES_NEW}. Each attempt therefore runs in a dedicated
 * transaction: if a UNIQUE-constraint violation rolls it back, only that attempt
 * is discarded and the caller can safely retry with a fresh transaction —
 * avoiding the "transaction marked rollback-only" trap you hit when catching a
 * constraint violation inside the same transaction you keep using.
 */
@Service
public class UrlMappingWriter {

    private final UrlMappingRepository repository;

    public UrlMappingWriter(UrlMappingRepository repository) {
        this.repository = repository;
    }

    /**
     * Insert a new mapping. Throws
     * {@link org.springframework.dao.DataIntegrityViolationException} if
     * {@code shortCode} is already taken (the UNIQUE constraint fires).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UrlMapping insert(long id, String shortCode, String longUrl) {
        return repository.saveAndFlush(new UrlMapping(id, shortCode, longUrl));
    }
}
