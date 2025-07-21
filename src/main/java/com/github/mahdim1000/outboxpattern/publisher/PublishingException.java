package com.github.mahdim1000.outboxpattern.publisher;

/**
 * Exception thrown when message publishing fails
 */
public class PublishingException extends Exception {
    
    private final boolean retryable;
    
    public PublishingException(String message) {
        super(message);
        this.retryable = true;
    }
    
    public PublishingException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }
    
    public PublishingException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }
    
    public PublishingException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
    
    /**
     * @return true if this failure is retryable, false if permanent
     */
    public boolean isRetryable() {
        return retryable;
    }
} 