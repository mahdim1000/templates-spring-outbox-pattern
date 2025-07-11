package org.radargps.outboxpattern.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.radargps.outboxpattern.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OutboxService {
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
    public void createMessage(String topic, Object payload) throws JsonProcessingException {
        var strPayload = objectMapper.writeValueAsString(payload);
        var outbox = Outbox.create(topic, strPayload);
        outboxRepository.save(outbox);
    }

    @Transactional
    public void processPendingMessage() {
        var messages = outboxRepository.findPendingMessage(batchSize);
        for (var message : messages) {
            try {
                eventPublisher.publish(message.getTopic(), message.getPayload());
                message.handlePublishSuccess();
            } catch (Exception e) {
                message.handlePublishFailure(e.getMessage());
            }
        }
    }

    @Transactional
    public void processFailedMessage() {
        var messages = outboxRepository.findFailedMessages(batchSize);
        for (var message : messages) {
            try {
                eventPublisher.publish(message.getTopic(), message.getPayload());
                message.handlePublishSuccess();
            } catch (Exception e) {
                message.handlePublishFailure(e.getMessage());
            }
        }
    }

}
