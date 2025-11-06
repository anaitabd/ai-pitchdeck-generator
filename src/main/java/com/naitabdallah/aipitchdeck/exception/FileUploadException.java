package com.naitabdallah.aipitchdeck.exception;

/**
 * Exception for file upload errors.
 */
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
