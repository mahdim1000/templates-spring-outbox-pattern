package com.github.mahdim1000.publisher;

/**
 * Interface for publishing events to external message brokers.
 * Implementations should handle connection management, retries, and serialization.
 */
public interface EventPublisher {
    
    /**
     * Publishes an event to the specified topic
     * @param topic the topic/queue name
     * @param payload the event payload as string
     * @throws PublishingException if publishing fails
     */
    void publish(String topic, String payload) throws PublishingException;
    
    /**
     * Publishes an event with custom headers
     * @param topic the topic/queue name
     * @param payload the event payload as string
     * @param headers custom headers for the message
     * @throws PublishingException if publishing fails
     */
    void publish(String topic, String payload, java.util.Map<String, String> headers) throws PublishingException;
    
    /**
     * Checks if the publisher is healthy and ready to publish messages
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Returns the publisher type for identification
     * @return publisher type name
     */
    String getPublisherType();
}
