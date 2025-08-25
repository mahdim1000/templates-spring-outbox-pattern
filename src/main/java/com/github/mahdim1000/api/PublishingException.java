package com.github.mahdim1000.api;

/**
 * Exception thrown when event publishing fails.
 * This is a checked exception to force proper error handling.
 */
public class PublishingException extends Exception {
    
    public PublishingException(String message) {
        super(message);
    }
    
    public PublishingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PublishingException(Throwable cause) {
        super(cause);
    }
}
