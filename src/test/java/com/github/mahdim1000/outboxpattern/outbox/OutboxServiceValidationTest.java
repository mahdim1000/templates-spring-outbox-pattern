package com.github.mahdim1000.outboxpattern.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("OutboxService Validation Tests")
class OutboxServiceValidationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    private static final String TEST_TOPIC = "TEST_TOPIC";
    private static final String AGGREGATE_ID = "test-aggregate-123";

    record TestEvent(String id, String action) {}

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Should throw exception when payload is null")
    void shouldThrowExceptionWhenPayloadIsNull() {
        assertThatThrownBy(() -> outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when expected version is null")
    void shouldThrowExceptionWhenExpectedVersionIsNull() {
        TestEvent event = new TestEvent("1", "CREATE");
        
        assertThatThrownBy(() -> outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected version must be a positive integer");
    }

    @Test
    @DisplayName("Should throw exception when expected version is zero")
    void shouldThrowExceptionWhenExpectedVersionIsZero() {
        TestEvent event = new TestEvent("1", "CREATE");
        
        assertThatThrownBy(() -> outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected version must be a positive integer");
    }

    @Test
    @DisplayName("Should throw exception when expected version is negative")
    void shouldThrowExceptionWhenExpectedVersionIsNegative() {
        TestEvent event = new TestEvent("1", "CREATE");
        
        assertThatThrownBy(() -> outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected version must be a positive integer");
    }

    @Test
    @DisplayName("Should allow version 1 for new aggregate")
    void shouldAllowVersion1ForNewAggregate() throws JsonProcessingException {
        TestEvent event = new TestEvent("1", "CREATE");
        
        assertThatCode(() -> outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event, 1))
                .doesNotThrowAnyException();

        var messages = outboxRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw exception when version gap exists")
    void shouldThrowExceptionWhenVersionGapExists() throws JsonProcessingException {
        // Create first message
        TestEvent event1 = new TestEvent("1", "CREATE");
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event1);

        // Try to create version 3 (skipping version 2)
        TestEvent event3 = new TestEvent("3", "SKIP_VERSION");
        assertThatThrownBy(() -> outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event3, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version mismatch for aggregate")
                .hasMessageContaining("Expected 2 but got 3");
    }

    @Test
    @DisplayName("Should allow sequential versions")
    void shouldAllowSequentialVersions() throws JsonProcessingException {
        TestEvent event1 = new TestEvent("1", "CREATE");
        TestEvent event2 = new TestEvent("2", "UPDATE");
        TestEvent event3 = new TestEvent("3", "DELETE");

        // Create version 1
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event1);
        
        // Create version 2 explicitly
        outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event2, 2);
        
        // Create version 3 explicitly
        outboxService.createOrderedMessage(TEST_TOPIC, AGGREGATE_ID, event3, 3);

        var messages = outboxRepository.findAll();
        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(Outbox::getVersion).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Should handle multiple aggregates independently")
    void shouldHandleMultipleAggregatesIndependently() throws JsonProcessingException {
        String aggregateId1 = "aggregate-1";
        String aggregateId2 = "aggregate-2";

        TestEvent event1 = new TestEvent("1", "CREATE");
        TestEvent event2 = new TestEvent("2", "CREATE");

        // Create version 1 for both aggregates using ordered method
        outboxService.createOrderedMessage(TEST_TOPIC, aggregateId1, event1, 1);
        outboxService.createOrderedMessage(TEST_TOPIC, aggregateId2, event2, 1);

        var messages = outboxRepository.findAll();
        assertThat(messages).hasSize(2);
        
        var aggregate1Messages = messages.stream()
                .filter(msg -> aggregateId1.equals(msg.getAggregateId()))
                .toList();
        var aggregate2Messages = messages.stream()
                .filter(msg -> aggregateId2.equals(msg.getAggregateId()))
                .toList();

        assertThat(aggregate1Messages).hasSize(1);
        assertThat(aggregate2Messages).hasSize(1);
        assertThat(aggregate1Messages.get(0).getVersion()).isEqualTo(1);
        assertThat(aggregate2Messages.get(0).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should determine correct next version automatically")
    void shouldDetermineCorrectNextVersionAutomatically() throws JsonProcessingException {
        TestEvent event1 = new TestEvent("1", "CREATE");
        TestEvent event2 = new TestEvent("2", "UPDATE");
        TestEvent event3 = new TestEvent("3", "DELETE");

        // Create messages with automatic versioning
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event1);
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event2);
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event3);

        var messages = outboxRepository.findAll();
        assertThat(messages).hasSize(3);
        
        // Sort by creation order (which should match version order)
        messages.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        assertThat(messages).extracting(Outbox::getVersion).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Should validate utility methods work correctly")
    void shouldValidateUtilityMethodsWorkCorrectly() throws JsonProcessingException {
        // Initially, next version should be 1
        assertThat(outboxService.getNextExpectedVersion(AGGREGATE_ID)).isEqualTo(1);

        // After creating one message, next version should be 2
        TestEvent event1 = new TestEvent("1", "CREATE");
        outboxService.createMessage(TEST_TOPIC, AGGREGATE_ID, event1);
        assertThat(outboxService.getNextExpectedVersion(AGGREGATE_ID)).isEqualTo(2);

        // Version 1 should be processable (no earlier versions exist)
        assertThat(outboxService.canProcessVersion(AGGREGATE_ID, 1)).isTrue();
        
        // Version 2 should not be processable yet (version 1 not published)
        assertThat(outboxService.canProcessVersion(AGGREGATE_ID, 2)).isFalse();
    }
} 