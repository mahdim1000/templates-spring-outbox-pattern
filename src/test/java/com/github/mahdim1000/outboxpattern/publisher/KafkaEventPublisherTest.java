package com.github.mahdim1000.outboxpattern.publisher;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SendResult<String, String> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @Test
    void shouldPublishMessageWithoutHeaders() throws PublishingException {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.offset()).thenReturn(123L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(topic, payload)).thenReturn(future);

        // When
        publisher.publish(topic, payload);

        // Then
        verify(kafkaTemplate).send(topic, payload);
    }

    @Test
    void shouldPublishMessageWithHeaders() throws PublishingException {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        Map<String, String> headers = Map.of("header1", "value1");
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.offset()).thenReturn(123L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        publisher.publish(topic, payload, headers);

        // Then
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void shouldThrowPublishingExceptionWhenKafkaTemplateFails() {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(topic, payload)).thenReturn(future);

        // When & Then
        assertThrows(PublishingException.class, () -> publisher.publish(topic, payload));
    }

    @Test
    void shouldReturnCorrectPublisherType() {
        // When
        String type = publisher.getPublisherType();

        // Then
        assertEquals("kafka", type);
    }

    @Test
    void shouldReturnTrueWhenHealthy() {
        // Given
        when(kafkaTemplate.metrics()).thenReturn(Map.of());

        // When
        boolean healthy = publisher.isHealthy();

        // Then
        assertTrue(healthy);
    }

    @Test
    void shouldReturnFalseWhenUnhealthy() {
        // Given
        when(kafkaTemplate.metrics()).thenThrow(new RuntimeException("Kafka unavailable"));

        // When
        boolean healthy = publisher.isHealthy();

        // Then
        assertFalse(healthy);
    }
} 