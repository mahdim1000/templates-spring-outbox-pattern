package com.github.mahdim1000.outboxpattern.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mahdim1000.outboxpattern.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;


@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    @Value("${outbox.processing.batch-size:10000}")
    private int batchSize;

    public OutboxService(OutboxRepository outboxRepository,
                         ObjectMapper objectMapper,
                         EventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createUnOrderedMessage(String topic, String aggregateId, Object payload) throws JsonProcessingException {
        createMessageWithVersion(topic, aggregateId, payload, 0);
    }

    @Transactional
    public void createOrderedMessage(String topic, String aggregateId, Object payload)
            throws JsonProcessingException {
        var version = outboxRepository.findMaxVersionByAggregateId(aggregateId)
                .map(maxVersion -> maxVersion + 1)
                .orElse(1);
        createMessageWithVersion(topic, aggregateId, payload, version);
    }

    private void createMessageWithVersion(String topic, String aggregateId, Object payload, Integer version) 
            throws JsonProcessingException {
        String serializedPayload = objectMapper.writeValueAsString(payload);
        Outbox outbox = Outbox.create(topic, aggregateId, serializedPayload, version);
        outboxRepository.save(outbox);
        
        log.debug("Created outbox message for aggregate {} with version {}", aggregateId, version);
    }

    @Transactional
    public void processPendingMessage() {
        var messages = outboxRepository.findPendingMessage(batchSize, LocalDateTime.now());
        log.debug("Processing {} pending messages", messages.size());
        
        for (var message : messages) {
            try {
                log.debug("Publishing message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());
                eventPublisher.publish(message.getTopic(), message.getPayload());
                message.handlePublishSuccess();
                log.debug("Successfully published message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());
            } catch (Exception e) {
                log.warn("Failed to publish message for aggregate {} version {}: {}", 
                    message.getAggregateId(), message.getVersion(), e.getMessage());
                message.handlePublishFailure(e.getMessage());
            }
        }
    }

    @Transactional
    public void processFailedMessage() {
        var messages = outboxRepository.findFailedMessages(batchSize, LocalDateTime.now());
        log.debug("Retrying {} failed messages", messages.size());
        
        for (var message : messages) {
            try {
                log.debug("Retrying message for aggregate {} version {} (attempt {})", 
                    message.getAggregateId(), message.getVersion(), message.getRetryCount() + 1);
                eventPublisher.publish(message.getTopic(), message.getPayload());
                message.handlePublishSuccess();
                log.info("Successfully recovered failed message for aggregate {} version {}", 
                    message.getAggregateId(), message.getVersion());
            } catch (Exception e) {
                log.warn("Retry failed for aggregate {} version {}: {}", 
                    message.getAggregateId(), message.getVersion(), e.getMessage());
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

}
