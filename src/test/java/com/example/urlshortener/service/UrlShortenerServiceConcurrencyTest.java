package com.example.urlshortener.service;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the two concurrency-safety guarantees against a real database:
 *  1. Parallel auto-generated requests never collide (all aliases unique).
 *  2. A race for the same custom alias yields exactly one winner; the rest 409.
 */
class UrlShortenerServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private UrlShortenerService service;

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void generatedAliasesAreUniqueUnderConcurrency() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<UrlMapping>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            tasks.add(() -> service.createShortUrl("https://example.com/page/" + n, null));
        }

        List<Future<UrlMapping>> results = pool.invokeAll(tasks);
        pool.shutdown();

        Set<String> codes = ConcurrentHashMap.newKeySet();
        for (Future<UrlMapping> f : results) {
            codes.add(f.get().getShortCode());
        }
        assertThat(codes).hasSize(threads);
    }

    @Test
    void customAliasRaceHasExactlyOneWinner() throws Exception {
        int threads = 20;
        String alias = "promo2026";
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    service.createShortUrl("https://example.com/landing", alias);
                    success.incrementAndGet();
                } catch (AliasAlreadyExistsException expected) {
                    conflict.incrementAndGet();
                }
                return null;
            });
        }
        pool.invokeAll(tasks);
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        assertThat(repository.findByShortCode(alias)).isPresent();
    }
}
