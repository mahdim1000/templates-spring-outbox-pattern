package com.github.mahdim1000.outboxpattern.publisher;

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
 * Kafka implementation of EventPublisher
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.type", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final long SEND_TIMEOUT_SECONDS = 30;
    
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
                // Create ProducerRecord with headers
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload);
                headers.forEach((key, value) -> 
                    record.headers().add(new RecordHeader(key, value.getBytes())));
                future = kafkaTemplate.send(record);
            } else {
                // Send without headers
                future = kafkaTemplate.send(topic, payload);
            }
            
            // Wait for the result with timeout
            SendResult<String, String> result = future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result.getRecordMetadata() != null) {
                log.debug("Successfully published message to Kafka topic: {} at offset: {}", 
                    topic, result.getRecordMetadata().offset());
            } else {
                log.debug("Successfully published message to Kafka topic: {}", topic);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish message to Kafka topic '{}': {}", topic, e.getMessage(), e);
            throw new PublishingException("Failed to publish to Kafka: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check by checking if we can get metrics
            kafkaTemplate.metrics();
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getPublisherType() {
        return "kafka";
    }
} 