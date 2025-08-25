package com.github.mahdim1000.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mahdim1000.api.OutboxMetrics;
import com.github.mahdim1000.api.EventPublisher;
import com.github.mahdim1000.api.PublishingException;
import com.github.mahdim1000.config.OutboxProperties;
import com.github.mahdim1000.domain.OutboxEntity;
import com.github.mahdim1000.domain.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Internal service for outbox operations.
 * This class handles the core business logic and should not be exposed to library users.
 */
@Service
public class OutboxService {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;
    private final OutboxProperties properties;

    public OutboxService(OutboxRepository repository,
                        ObjectMapper objectMapper,
                        EventPublisher eventPublisher,
                        OutboxProperties properties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public void createUnOrderedMessage(String topic, String aggregateId, Object payload, 
                                      Map<String, String> headers, boolean retryable) 
                                      throws JsonProcessingException {
        createMessageWithVersion(topic, aggregateId, payload, 0, headers, retryable);
    }

    @Transactional
    public void createOrderedMessage(String topic, String aggregateId, Object payload,
                                    Map<String, String> headers, boolean retryable) 
                                    throws JsonProcessingException {
        var version = repository.findMaxVersionByAggregateId(aggregateId)
                .map(maxVersion -> maxVersion + 1)
                .orElse(1);
        createMessageWithVersion(topic, aggregateId, payload, Integer.valueOf(version), headers, retryable);
    }

    private void createMessageWithVersion(String topic, String aggregateId, Object payload, 
                                         Integer version, Map<String, String> headers, 
                                         boolean retryable) throws JsonProcessingException {
        String serializedPayload = objectMapper.writeValueAsString(payload);
        String serializedHeaders = headers.isEmpty() ? null : objectMapper.writeValueAsString(headers);
        
        OutboxEntity entity = OutboxEntity.create(topic, aggregateId, serializedPayload, 
                                                 serializedHeaders, version, retryable);
        
        repository.save(entity);
        log.debug("Created outbox message for aggregate {} with version {}", aggregateId, version);
    }

    @Transactional
    public void processPendingMessages() {
        var messages = repository.findPendingMessages(
            properties.processing().batchSize(), 
            LocalDateTime.now()
        );
        
        log.debug("Processing {} pending messages", messages.size());
        
        for (var message : messages) {
            processMessage(message);
        }
    }

    @Transactional
    public void processFailedMessages() {
        var messages = repository.findFailedMessages(
            properties.processing().batchSize(), 
            LocalDateTime.now()
        );
        
        log.debug("Retrying {} failed messages", messages.size());
        
        for (var message : messages) {
            processMessage(message);
        }
    }

    private void processMessage(OutboxEntity message) {
        try {
            log.debug("Publishing message for aggregate {} version {}", 
                message.getAggregateId(), message.getVersion());
            
            Map<String, String> headers = parseHeaders(message.getHeadersJson());
            eventPublisher.publish(message.getTopic(), message.getPayload(), headers);
            
            message.markAsPublished();
            log.debug("Successfully published message for aggregate {} version {}", 
                message.getAggregateId(), message.getVersion());

        } catch (PublishingException e) {
            log.error("Publishing failed for aggregate {} version {}: {}", 
                message.getAggregateId(), message.getVersion(), e.getMessage());
            message.recordFailure(e.getMessage(), properties.retry().maxRetries(), 
                                properties.retry().initialDelay());
        } catch (Exception e) {
            log.error("Unexpected error publishing message for aggregate {} version {}: {}", 
                message.getAggregateId(), message.getVersion(), e.getMessage(), e);
            message.recordFailure(e.getMessage(), properties.retry().maxRetries(), 
                                properties.retry().initialDelay());
        }
    }

    @Transactional(readOnly = true)
    public OutboxMetrics getMetrics() {
        return new OutboxMetrics(
            repository.countPendingMessages(),
            repository.countFailedMessages(),
            repository.countByStatus(OutboxEntity.Status.PUBLISHED),
            repository.countByStatus(OutboxEntity.Status.DEAD_LETTER)
        );
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return Map.of();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> result = objectMapper.readValue(headersJson, Map.class);
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse headers JSON: {}", headersJson, e);
            return Map.of();
        }
    }


}
