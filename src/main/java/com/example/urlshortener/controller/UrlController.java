package com.example.urlshortener.controller;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.dto.CreateShortUrlRequest;
import com.example.urlshortener.dto.ShortUrlResponse;
import com.example.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * JSON REST API for managing short URLs. The redirect itself lives in
 * {@link RedirectController} so it can own the root path ("/{alias}").
 */
@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "Create and inspect short URLs")
public class UrlController {

    private final UrlShortenerService service;
    private final String baseUrl;

    public UrlController(UrlShortenerService service, AppProperties properties) {
        this.service = service;
        this.baseUrl = properties.baseUrl();
    }

    @Operation(summary = "Create a short URL",
            description = "Generates a unique alias, or uses the supplied customAlias. "
                    + "Returns 409 if a custom alias is already taken.")
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        UrlMapping mapping = service.createShortUrl(request.url(), request.customAlias());
        ShortUrlResponse body = ShortUrlResponse.from(mapping, baseUrl);
        URI location = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/{code}")
                .buildAndExpand(mapping.getShortCode())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(body);
    }

    @Operation(summary = "Inspect a short URL", description = "Returns metadata and hit count for an alias.")
    @GetMapping("/{shortCode}")
    public ShortUrlResponse get(@PathVariable String shortCode) {
        return ShortUrlResponse.from(service.getMapping(shortCode), baseUrl);
    }
}
