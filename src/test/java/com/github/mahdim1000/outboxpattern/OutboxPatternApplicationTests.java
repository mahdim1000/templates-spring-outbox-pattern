package com.github.mahdim1000.outboxpattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.mahdim1000.outboxpattern.outbox.Outbox;
import com.github.mahdim1000.outboxpattern.outbox.OutboxRepository;
import com.github.mahdim1000.outboxpattern.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OutboxPatternApplicationTests {

    @Autowired
    private OutboxService outboxService;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        // Application context loads successfully
    }

    record OrderCreatedEvent(String orderId, String status) {}

    @Test
    void createMessage_persistsOutboxMessage() throws JsonProcessingException {
        String topic = "ORDER_CREATED";
        var event = new OrderCreatedEvent("123", "CREATE");
        outboxService.createMessage(topic, "123", event);
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(1);
        Outbox message = messages.get(0);
        assertThat(message.getTopic()).isEqualTo(topic);
        assertThat(message.getPayload()).isEqualTo(objectMapper.writeValueAsString(event));
        assertThat(message.getStatus()).isEqualTo(Outbox.Status.PENDING);
    }

    @Test
    void processPendingMessage_successfulPublish_setsStatusPublished() throws JsonProcessingException {
        String topic = "ORDER_CREATED";
        var event = new OrderCreatedEvent("123", "CREATE");
        outboxService.createMessage(topic, "123", event);
        outboxService.processPendingMessage();
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(Outbox.Status.PUBLISHED);
    }


    @Test
    void createMessage_handlesMultipleMessages() throws JsonProcessingException {
        for (int i = 0; i < 10; i++) {
            String topic = "TOPIC_" + i;
            String payload = "{\"order\": " + i + ", \"status\": \"created\"}";
            outboxService.createMessage(topic, UlidCreator.getUlid().toString(), payload);
        }
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(10);
    }

    @Test
    void createMessage_assignsSequentialVersionsForSameAggregate() throws JsonProcessingException {
        String aggregateId = "order-123";
        String topic = "ORDER_EVENTS";
        
        // Create multiple events for same aggregate
        outboxService.createMessage(topic, aggregateId, new OrderCreatedEvent(aggregateId, "CREATED"));
        outboxService.createMessage(topic, aggregateId, new OrderCreatedEvent(aggregateId, "CONFIRMED"));
        outboxService.createMessage(topic, aggregateId, new OrderCreatedEvent(aggregateId, "SHIPPED"));
        
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(3);
        
        // Verify sequential versioning
        messages.sort((a, b) -> a.getVersion().compareTo(b.getVersion()));
        assertThat(messages.get(0).getVersion()).isEqualTo(1);
        assertThat(messages.get(1).getVersion()).isEqualTo(2);
        assertThat(messages.get(2).getVersion()).isEqualTo(3);
        
        // All should have same aggregate ID
        assertThat(messages).allMatch(msg -> aggregateId.equals(msg.getAggregateId()));
    }

    @Test
    void processPendingMessage_respectsVersionOrdering() throws JsonProcessingException {
        String aggregateId = "order-123";
        String topic = "ORDER_EVENTS";
        
        // Create multiple events for same aggregate
        outboxService.createMessage(topic, aggregateId, new OrderCreatedEvent(aggregateId, "CREATED"));
        outboxService.createMessage(topic, aggregateId, new OrderCreatedEvent(aggregateId, "CONFIRMED"));
        
        // Process messages - need two calls due to version ordering
        outboxService.processPendingMessage(); // Processes version 1
        outboxService.processPendingMessage(); // Processes version 2
        
        // Verify all messages were processed in order
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(2);
        assertThat(messages).allMatch(msg -> msg.getStatus() == Outbox.Status.PUBLISHED);
        
        // Verify processing order by checking published timestamps
        messages.sort((a, b) -> a.getVersion().compareTo(b.getVersion()));
        assertThat(messages.get(0).getPublishedAt()).isNotNull();
        assertThat(messages.get(1).getPublishedAt()).isNotNull();
        assertThat(messages.get(0).getPublishedAt()).isBeforeOrEqualTo(messages.get(1).getPublishedAt());
    }
}
