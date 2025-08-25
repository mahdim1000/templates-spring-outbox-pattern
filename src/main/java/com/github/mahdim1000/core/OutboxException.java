package com.github.mahdim1000.core;

/**
 * Runtime exception for outbox operations.
 * Used for configuration errors and unexpected failures.
 */
public class OutboxException extends RuntimeException {
    
    public OutboxException(String message) {
        super(message);
    }
    
    public OutboxException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public OutboxException(Throwable cause) {
        super(cause);
    }
}
