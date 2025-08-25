package com.github.mahdim1000.publisher;

import com.github.mahdim1000.api.EventPublisher;
import com.github.mahdim1000.api.PublishingException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka implementation of EventPublisher.
 * Publishes events to Apache Kafka topics.
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.type", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final long SEND_TIMEOUT_SECONDS = 30;
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void publish(String topic, String payload) throws PublishingException {
        publish(topic, payload, Map.of());
    }
    
    @Override
    public void publish(String topic, String payload, Map<String, String> headers) throws PublishingException {
        try {
            CompletableFuture<SendResult<String, String>> future;
            
            if (headers != null && !headers.isEmpty()) {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload);
                headers.forEach((key, value) -> 
                    record.headers().add(new RecordHeader(key, value.getBytes())));
                future = kafkaTemplate.send(record);
            } else {
                future = kafkaTemplate.send(topic, payload);
            }
            
            SendResult<String, String> result = future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Successfully published message to Kafka topic '{}' at offset: {}", 
                topic, result.getRecordMetadata().offset());
            
        } catch (Exception e) {
            log.error("Failed to publish message to Kafka topic '{}': {}", topic, e.getMessage(), e);
            throw new PublishingException("Failed to publish to Kafka: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            kafkaTemplate.metrics();
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "kafka";
    }
}