package org.radargps.outboxpattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.radargps.outboxpattern.outbox.Outbox;
import org.radargps.outboxpattern.outbox.OutboxRepository;
import org.radargps.outboxpattern.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

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
        outboxService.createMessage(topic, event);
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
        outboxService.createMessage(topic, event);
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
            outboxService.createMessage(topic, payload);
        }
        List<Outbox> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(10);
    }
}
