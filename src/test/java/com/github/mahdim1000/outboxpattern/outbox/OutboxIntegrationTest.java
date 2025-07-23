package com.github.mahdim1000.outboxpattern.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mahdim1000.outboxpattern.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @Test
    void shouldCreateAndProcessUnorderedMessage() throws JsonProcessingException {
        // Given
        String topic = "test-topic";
        String aggregateId = "test-aggregate-1";
        String payload = "test-payload";
        Map<String, String> headers = Map.of("eventType", "TestEvent");

        // When - Create message
        outboxService.createUnOrderedMessage(topic, aggregateId, payload, headers, true);

        // Then - Verify message was created
        var messages = outboxRepository.findAll();
        assertFalse(messages.isEmpty());
        
        var message = messages.get(0);
        assertEquals(topic, message.getTopic());
        assertEquals(aggregateId, message.getAggregateId());
        assertEquals(Outbox.Status.PENDING, message.getStatus());
        assertEquals(0, message.getVersion()); // Unordered messages have version 0
        
        // When - Process pending messages
        outboxService.processPendingMessage();
        
        // Then - Verify message was published
        var processedMessage = outboxRepository.findById(message.getId()).orElseThrow();
        assertEquals(Outbox.Status.PUBLISHED, processedMessage.getStatus());
        assertNotNull(processedMessage.getPublishedAt());
    }

    @Test
    void shouldCreateAndProcessOrderedMessage() throws JsonProcessingException {
        // Given
        String topic = "test-topic";
        String aggregateId = "test-aggregate-2";
        String payload1 = "test-payload-1";
        String payload2 = "test-payload-2";
        Map<String, String> headers = Map.of("eventType", "TestEvent");

        // When - Create ordered messages
        outboxService.createOrderedMessage(topic, aggregateId, payload1, headers, true);
        outboxService.createOrderedMessage(topic, aggregateId, payload2, headers, true);

        // Then - Verify messages were created with correct versions
        var messages = outboxRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        assertEquals(2, messages.size());
        assertEquals(1, messages.get(0).getVersion());
        assertEquals(2, messages.get(1).getVersion());
        
        // When - Process pending messages (first message)
        outboxService.processPendingMessage();
        
        // Process again to handle the second message after first is published
        outboxService.processPendingMessage();
        
        // Then - Verify messages were published
        var processedMessages = outboxRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        processedMessages.forEach(message -> {
            assertEquals(Outbox.Status.PUBLISHED, message.getStatus());
            assertNotNull(message.getPublishedAt());
        });
    }

    @Test
    void shouldGetCorrectMetrics() throws JsonProcessingException {
        // Given
        String topic = "test-topic";
        String aggregateId = "test-aggregate-3";

        // When - Create some messages
        outboxService.createUnOrderedMessage(topic, aggregateId + "-1", "payload1", Map.of(), true);
        outboxService.createUnOrderedMessage(topic, aggregateId + "-2", "payload2", Map.of(), true);

        // Then - Check metrics
        var metrics = outboxService.getMetrics();
        assertTrue(metrics.pendingCount() >= 2);
        assertEquals(0, metrics.failedCount());
        
        // When - Process messages
        outboxService.processPendingMessage();
        
        // Then - Check updated metrics
        var updatedMetrics = outboxService.getMetrics();
        assertTrue(updatedMetrics.publishedCount() >= 2);
    }

    @Test
    void shouldHandlePublishingFailure() throws JsonProcessingException {
        // Given
        String topic = "test-topic";
        String aggregateId = "test-aggregate-4";
        String payload = "FAIL_TEST"; // This will trigger failure in LoggingEventPublisher

        // When - Create message that will fail
        outboxService.createUnOrderedMessage(topic, aggregateId, payload, Map.of(), true);
        
        // Then - Process and verify failure handling
        outboxService.processPendingMessage();
        
        var messages = outboxRepository.findByAggregateId(aggregateId);
        assertFalse(messages.isEmpty());
        var message = messages.get(0);
        // Message should be marked as failed after processing
        assertTrue(message.getRetryCount() > 0);
        assertNotNull(message.getErrorMessage());
    }

    @Test
    void shouldVerifyPublisherType() {
        // Then - Verify the correct publisher is being used
        String publisherType = eventPublisher.getPublisherType();
        assertNotNull(publisherType);
        assertTrue(publisherType.matches("logging|kafka|rabbitmq"));
    }
} 