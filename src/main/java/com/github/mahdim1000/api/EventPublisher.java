package com.github.mahdim1000.api;

import java.util.Map;

/**
 * Interface for publishing events to external message brokers.
 * 
 * Implementations should:
 * - Handle connection management and retries internally
 * - Be thread-safe for concurrent access
 * - Provide meaningful error messages via PublishingException
 * - Implement proper resource cleanup
 */
public interface EventPublisher {
    
    /**
     * Publishes an event to the specified topic.
     * 
     * @param topic the destination topic/queue
     * @param payload the event payload as JSON string
     * @throws PublishingException if publishing fails
     */
    void publish(String topic, String payload) throws PublishingException;
    
    /**
     * Publishes an event with custom headers.
     * 
     * @param topic the destination topic/queue
     * @param payload the event payload as JSON string  
     * @param headers custom headers for the message
     * @throws PublishingException if publishing fails
     */
    void publish(String topic, String payload, Map<String, String> headers) throws PublishingException;
    
    /**
     * Checks if the publisher is healthy and ready to publish messages.
     * This should be a lightweight check (no network calls if possible).
     * 
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Returns the publisher type for identification and logging.
     * 
     * @return publisher type name (e.g., "kafka", "rabbitmq", "logging")
     */
    String getType();
}
