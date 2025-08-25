package com.github.mahdim1000.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mahdim1000.api.OutboxMetrics;
import com.github.mahdim1000.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of OutboxManager.
 * Provides a simple facade over the complex outbox operations.
 */
@Component
public class DefaultOutboxManager implements OutboxManager {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxManager.class);
    
    private final OutboxService outboxService;
    
    public DefaultOutboxManager(OutboxService outboxService, 
                               ObjectMapper objectMapper,
                               OutboxProperties properties) {
        this.outboxService = outboxService;
    }
    
    @Override
    public OutboxEventBuilder publish(String topic, String aggregateId, Object payload) {
        return new DefaultOutboxEventBuilder(topic, aggregateId, payload, false);
    }
    
    @Override
    public OutboxEventBuilder publishOrdered(String topic, String aggregateId, Object payload) {
        return new DefaultOutboxEventBuilder(topic, aggregateId, payload, true);
    }
    
    @Override
    public OutboxMetrics getMetrics() {
        return outboxService.getMetrics();
    }
    
    @Override
    public boolean isHealthy() {
        try {
            var metrics = getMetrics();
            return metrics.isHealthy();
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Internal builder implementation with fluent API.
     */
    private class DefaultOutboxEventBuilder implements OutboxEventBuilder {
        
        private final String topic;
        private final String aggregateId;
        private final Object payload;
        private final boolean ordered;
        private final Map<String, String> headers = new HashMap<>();
        private boolean retryable = true;
        
        public DefaultOutboxEventBuilder(String topic, String aggregateId, Object payload, boolean ordered) {
            this.topic = validateTopic(topic);
            this.aggregateId = validateAggregateId(aggregateId);
            this.payload = validatePayload(payload);
            this.ordered = ordered;
        }
        
        @Override
        public OutboxEventBuilder withHeaders(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }
        
        @Override
        public OutboxEventBuilder withHeader(String key, String value) {
            if (key != null && value != null) {
                this.headers.put(key, value);
            }
            return this;
        }
        
        @Override
        public OutboxEventBuilder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }
        
        @Override
        @Transactional
        public void execute() {
            try {
                if (ordered) {
                    outboxService.createOrderedMessage(topic, aggregateId, payload, headers, retryable);
                } else {
                    outboxService.createUnOrderedMessage(topic, aggregateId, payload, headers, retryable);
                }
                log.debug("Successfully queued {} event for aggregate {} to topic {}", 
                    ordered ? "ordered" : "unordered", aggregateId, topic);
                    
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize payload for aggregate {} to topic {}: {}", 
                    aggregateId, topic, e.getMessage());
                throw new OutboxException("Failed to serialize event payload", e);
            } catch (Exception e) {
                log.error("Failed to create outbox event for aggregate {} to topic {}: {}", 
                    aggregateId, topic, e.getMessage());
                throw new OutboxException("Failed to create outbox event", e);
            }
        }
        
        private String validateTopic(String topic) {
            if (topic == null || topic.trim().isEmpty()) {
                throw new IllegalArgumentException("Topic cannot be null or empty");
            }
            return topic.trim();
        }
        
        private String validateAggregateId(String aggregateId) {
            if (aggregateId == null || aggregateId.trim().isEmpty()) {
                throw new IllegalArgumentException("AggregateId cannot be null or empty");
            }
            return aggregateId.trim();
        }
        
        private Object validatePayload(Object payload) {
            if (payload == null) {
                throw new IllegalArgumentException("Payload cannot be null");
            }
            return payload;
        }
    }
}
