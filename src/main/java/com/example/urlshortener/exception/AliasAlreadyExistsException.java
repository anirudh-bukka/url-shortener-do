package com.example.urlshortener.exception;

/**
 * Thrown when a caller-supplied custom alias is already taken. Surfaced to
 * clients as HTTP 409 Conflict by the global exception handler.
 */
public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String alias) {
        super("Alias already in use: " + alias);
    }
}
