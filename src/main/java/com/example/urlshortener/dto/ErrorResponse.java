package com.example.urlshortener.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent, machine-readable error envelope returned for every 4xx/5xx.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     short status reason phrase
 * @param message   human-readable explanation
 * @param path      request path that produced the error
 * @param fieldErrors per-field validation messages (empty unless it's a 400 validation failure)
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
