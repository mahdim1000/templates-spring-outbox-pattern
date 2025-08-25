package com.github.mahdim1000;

import com.github.mahdim1000.core.OutboxManager;
import com.github.mahdim1000.api.OutboxMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Outbox Pattern Library.
 * Tests the core functionality with minimal setup.
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "outbox.publisher.type=logging",
    "outbox.processing.enabled=false", // Disable automatic processing for tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OutboxLibraryTest {

    @Autowired
    private OutboxManager outboxManager;

    @Test
    @Transactional
    void shouldPublishUnorderedMessage() {
        // Given
        String topic = "test.topic";
        String aggregateId = "test-123";
        TestEvent payload = new TestEvent("test message", LocalDateTime.now());

        // When
        outboxManager.publish(topic, aggregateId, payload)
                     .withHeader("test-header", "test-value")
                     .execute();

        // Then
        OutboxMetrics metrics = outboxManager.getMetrics();
        assertThat(metrics.pendingCount()).isEqualTo(1);
        assertThat(metrics.failedCount()).isEqualTo(0);
        assertThat(metrics.publishedCount()).isEqualTo(0);
        assertThat(metrics.deadLetterCount()).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldPublishOrderedMessage() {
        // Given
        String topic = "order.events";
        String aggregateId = "order-456";
        TestEvent payload = new TestEvent("order created", LocalDateTime.now());

        // When
        outboxManager.publishOrdered(topic, aggregateId, payload)
                     .withHeaders(Map.of("eventType", "OrderCreated", "version", "1.0"))
                     .retryable(true)
                     .execute();

        // Then
        OutboxMetrics metrics = outboxManager.getMetrics();
        assertThat(metrics.pendingCount()).isEqualTo(1);
        assertThat(metrics.totalCount()).isEqualTo(1);
    }

    @Test
    void shouldReportHealthy() {
        // When
        boolean healthy = outboxManager.isHealthy();

        // Then
        assertThat(healthy).isTrue();
    }

    @Test
    @Transactional
    void shouldHandleMultipleEvents() {
        // Given
        String aggregateId = "multi-test";

        // When - publish multiple events
        for (int i = 0; i < 5; i++) {
            outboxManager.publish("test.topic", aggregateId, 
                    new TestEvent("message " + i, LocalDateTime.now()))
                         .execute();
        }

        // Then
        OutboxMetrics metrics = outboxManager.getMetrics();
        assertThat(metrics.pendingCount()).isEqualTo(5);
    }

    // Test event record
    public record TestEvent(String message, LocalDateTime timestamp) {}
}
