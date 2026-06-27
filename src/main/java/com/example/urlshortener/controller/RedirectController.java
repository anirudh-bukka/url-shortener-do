package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Public redirect endpoint. Maps {@code GET /{alias}} to a 301 redirect toward
 * the original long URL. Kept separate from the API so the short links can live
 * at the domain root (e.g. https://sho.rt/4c92).
 */
@RestController
@Tag(name = "Redirect", description = "Resolve an alias to its destination")
public class RedirectController {

    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @Operation(summary = "Redirect to the original URL",
            description = "Looks up the alias (served from cache when hot) and issues an HTTP 302. "
                    + "302 (not 301) is used deliberately so browsers re-request each visit, "
                    + "keeping access counts accurate.")
    @GetMapping("/{shortCode:[0-9A-Za-z]{3,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = service.resolve(shortCode);
        // Count the access; the increment is an atomic DB UPDATE, so it is safe
        // under concurrent traffic and never corrupts the counter.
        service.recordHit(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .location(URI.create(longUrl))
                .build();
    }
}
