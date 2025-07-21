package com.github.mahdim1000.outboxpattern.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Simple logging implementation of EventPublisher for development/testing
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.type", havingValue = "logging", matchIfMissing = true)
public class LoggingEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);
    
    @Override
    public void publish(String topic, String payload) throws PublishingException {
        publish(topic, payload, Map.of());
    }
    
    @Override
    public void publish(String topic, String payload, Map<String, String> headers) throws PublishingException {
        log.info("Publishing to topic '{}': {}", topic, payload);
        if (!headers.isEmpty()) {
            log.debug("Headers: {}", headers);
        }
        
        // Simulate occasional failures for testing
        if (payload.contains("FAIL_TEST")) {
            throw new PublishingException("Simulated publishing failure for testing");
        }
    }
    
    @Override
    public boolean isHealthy() {
        return true;
    }
    
    @Override
    public String getPublisherType() {
        return "logging";
    }
} 