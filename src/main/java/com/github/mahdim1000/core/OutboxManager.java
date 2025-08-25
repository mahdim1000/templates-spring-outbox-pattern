package com.github.mahdim1000.core;

import com.github.mahdim1000.api.OutboxMetrics;

import java.util.Map;

/**
 * Main facade for the Outbox Pattern library.
 * Provides a simple, fluent API for transactional outbox operations.
 * 
 * Usage:
 * <pre>
 * // Simple unordered message
 * outboxManager.publish("user.created", userId, userCreatedEvent);
 * 
 * // Ordered message with headers
 * outboxManager.publishOrdered("order.events", orderId, orderEvent)
 *             .withHeaders(Map.of("version", "1.0"))
 *             .retryable(true);
 * </pre>
 */
public interface OutboxManager {
    
    /**
     * Publishes an unordered event to the outbox.
     * Events will be processed as soon as possible without ordering guarantees.
     * 
     * @param topic the destination topic/queue
     * @param aggregateId the business entity identifier
     * @param payload the event payload (will be JSON serialized)
     * @return fluent builder for additional configuration
     */
    OutboxEventBuilder publish(String topic, String aggregateId, Object payload);
    
    /**
     * Publishes an ordered event to the outbox.
     * Events for the same aggregateId will be processed in version order.
     * 
     * @param topic the destination topic/queue  
     * @param aggregateId the business entity identifier
     * @param payload the event payload (will be JSON serialized)
     * @return fluent builder for additional configuration
     */
    OutboxEventBuilder publishOrdered(String topic, String aggregateId, Object payload);
    
    /**
     * Gets current outbox metrics for monitoring.
     * 
     * @return metrics snapshot
     */
    OutboxMetrics getMetrics();
    
    /**
     * Checks if the outbox system is healthy.
     * 
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Fluent builder for configuring outbox events.
     */
    interface OutboxEventBuilder {
        
        /**
         * Adds custom headers to the event.
         * 
         * @param headers key-value pairs for message headers
         * @return this builder
         */
        OutboxEventBuilder withHeaders(Map<String, String> headers);
        
        /**
         * Adds a single header to the event.
         * 
         * @param key header key
         * @param value header value
         * @return this builder
         */
        OutboxEventBuilder withHeader(String key, String value);
        
        /**
         * Sets whether this event should be retried on failure.
         * Default is true.
         * 
         * @param retryable true to enable retries, false otherwise
         * @return this builder
         */
        OutboxEventBuilder retryable(boolean retryable);
        
        /**
         * Executes the publish operation.
         * This method must be called within an active transaction.
         */
        void execute();
    }
}
