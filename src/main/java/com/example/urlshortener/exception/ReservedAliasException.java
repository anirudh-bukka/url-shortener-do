package com.example.urlshortener.exception;

/**
 * Thrown when a custom alias collides with a reserved application path
 * (e.g. "api", "actuator"). Surfaced to clients as HTTP 400.
 */
public class ReservedAliasException extends RuntimeException {

    public ReservedAliasException(String alias) {
        super("Alias is reserved and cannot be used: " + alias);
    }
}
