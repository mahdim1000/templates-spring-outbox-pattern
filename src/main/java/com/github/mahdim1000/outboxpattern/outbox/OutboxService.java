package com.github.mahdim1000.outboxpattern.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mahdim1000.outboxpattern.config.OutboxProperties;
import com.github.mahdim1000.outboxpattern.publisher.EventPublisher;
import com.github.mahdim1000.outboxpattern.publisher.PublishingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;


@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;
    private final OutboxProperties properties;

    public OutboxService(OutboxRepository outboxRepository,
                         ObjectMapper objectMapper,
                         EventPublisher eventPublisher,
                         OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public void createUnOrderedMessage(String topic, String aggregateId, Object payload, Map<String, String> headers,
                                       boolean retryable) throws JsonProcessingException {
        createMessageWithVersion(topic, aggregateId, payload, 0, headers, retryable);
    }

    @Transactional
    public void createOrderedMessage(String topic, String aggregateId, Object payload, Map<String, String> headers,
                                     boolean retryable) throws JsonProcessingException {
        var version = outboxRepository.findMaxVersionByAggregateId(aggregateId)
                .map(maxVersion -> maxVersion + 1)
                .orElse(1);
        createMessageWithVersion(topic, aggregateId, payload, version, headers, retryable);
    }

    private void createMessageWithVersion(String topic, String aggregateId, Object payload, Integer version, Map<String,
            String> headers, boolean retryable)
            throws JsonProcessingException {
        String serializedPayload = objectMapper.writeValueAsString(payload);
        String serializedHeaders = headers.isEmpty() ? null : objectMapper.writeValueAsString(headers);
        Outbox outbox = Outbox.create(topic, aggregateId, serializedPayload, serializedHeaders, version, retryable);
        
        outboxRepository.save(outbox);
        log.debug("Created outbox message for aggregate {} with version {}", aggregateId, version);
    }

    @Transactional
    public void processPendingMessage() {
        var messages = outboxRepository.findPendingMessage(
            properties.processing().batchSize(), 
            LocalDateTime.now()
        );
        
        log.debug("Processing {} pending messages", messages.size());
        
        for (var message : messages) {
            try {
                log.debug("Publishing message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());
                
                Map<String, String> headers = parseHeaders(message.getHeaders());
                eventPublisher.publish(message.getTopic(), message.getPayload(), headers);
                
                message.handlePublishSuccess();
                log.debug("Successfully published message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());

            } catch (Exception e) {
                log.error("Unexpected error publishing message for aggregate {} version {}: {}", 
                    message.getAggregateId(), message.getVersion(), e.getMessage(), e);
                message.handlePublishFailure(e.getMessage());
            }
        }
    }

    @Transactional
    public void processFailedMessage() {
        var messages = outboxRepository.findFailedMessages(
            properties.processing().batchSize(), 
            LocalDateTime.now()
        );
        
        log.debug("Retrying {} failed messages", messages.size());
        
        for (var message : messages) {
            if (!message.isRetryable() || message.getRetryCount() >= properties.retry().maxRetries()) {
                log.warn("Message for aggregate {} version {} exceeded max retries, moving to dead letter", 
                    message.getAggregateId(), message.getVersion());
                message.moveToDeadLetter();
                continue;
            }

            try {
                log.debug("Retrying message for aggregate {} version {} (attempt {})", 
                    message.getAggregateId(), message.getVersion(), message.getRetryCount() + 1);
                
                Map<String, String> headers = parseHeaders(message.getHeaders());
                eventPublisher.publish(message.getTopic(), message.getPayload(), headers);
                
                message.handlePublishSuccess();
                log.info("Successfully recovered failed message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());
                    
            } catch (Exception e) {
                log.error("Unexpected error retrying message for aggregate {} version {}: {}", 
                    message.getAggregateId(), message.getVersion(), e.getMessage(), e);
                message.handlePublishFailure(e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean canProcessVersion(String aggregateId, Integer version) {
        return outboxRepository.countUnpublishedVersionsBefore(aggregateId, version) == 0;
    }

    @Transactional(readOnly = true)
    public Integer getNextExpectedVersion(String aggregateId) {
        return outboxRepository.findMaxVersionByAggregateId(aggregateId)
            .map(maxVersion -> maxVersion + 1)
            .orElse(1);
    }

    @Transactional(readOnly = true)
    public OutboxMetrics getMetrics() {
        return new OutboxMetrics(
            outboxRepository.countPendingMessages(),
            outboxRepository.countFailedMessages(),
            outboxRepository.countByStatus(Outbox.Status.PUBLISHED),
            outboxRepository.countByStatus(Outbox.Status.DEAD_LETTER)
        );
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(headersJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse headers JSON: {}", headersJson, e);
            return Map.of();
        }
    }

    public record OutboxMetrics(
        long pendingCount,
        long failedCount,
        long publishedCount,
        long deadLetterCount
    ) {}
}
