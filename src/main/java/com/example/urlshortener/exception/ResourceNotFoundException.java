package com.example.urlshortener.exception;

/**
 * Thrown when an alias cannot be resolved. Surfaced to clients as HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
