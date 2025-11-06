package com.naitabdallah.aipitchdeck.exception;

/**
 * Exception for AI generation errors.
 */
public class GenerationException extends RuntimeException {
    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
