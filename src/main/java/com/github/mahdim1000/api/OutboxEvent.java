package com.github.mahdim1000.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents an outbox event with all its metadata.
 * This is a read-only view of an outbox entry.
 */
public interface OutboxEvent {
    
    /**
     * @return unique identifier for this event
     */
    String getId();
    
    /**
     * @return destination topic/queue name
     */
    String getTopic();
    
    /**
     * @return business entity identifier
     */
    String getAggregateId();
    
    /**
     * @return JSON serialized payload
     */
    String getPayload();
    
    /**
     * @return event headers as key-value pairs
     */
    Map<String, String> getHeaders();
    
    /**
     * @return version number for ordering (0 for unordered events)
     */
    Integer getVersion();
    
    /**
     * @return current status of the event
     */
    EventStatus getStatus();
    
    /**
     * @return error message if the event failed
     */
    String getErrorMessage();
    
    /**
     * @return number of retry attempts made
     */
    int getRetryCount();
    
    /**
     * @return when the event was created
     */
    LocalDateTime getCreatedAt();
    
    /**
     * @return when the event was successfully published (null if not published)
     */
    LocalDateTime getPublishedAt();
    
    /**
     * @return whether this event can be retried on failure
     */
    boolean isRetryable();
    
    /**
     * Event processing status
     */
    enum EventStatus {
        PENDING,      // Waiting to be processed
        PUBLISHED,    // Successfully published
        FAILED,       // Failed but retryable
        DEAD_LETTER   // Failed and no more retries
    }
}
