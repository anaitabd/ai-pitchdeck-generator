package com.naitabdallah.aipitchdeck.exception;

/**
 * Exception for authentication errors.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
