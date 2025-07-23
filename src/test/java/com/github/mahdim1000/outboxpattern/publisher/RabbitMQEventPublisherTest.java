package com.github.mahdim1000.outboxpattern.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMQEventPublisher(rabbitTemplate);
    }

    @Test
    void shouldPublishMessageWithoutHeaders() throws PublishingException {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";

        // When
        publisher.publish(topic, payload);

        // Then
        verify(rabbitTemplate).send(eq(topic), any(Message.class));
    }

    @Test
    void shouldPublishMessageWithHeaders() throws PublishingException {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        Map<String, String> headers = Map.of("header1", "value1", "header2", "value2");

        // When
        publisher.publish(topic, payload, headers);

        // Then
        verify(rabbitTemplate).send(eq(topic), any(Message.class));
    }

    @Test
    void shouldThrowPublishingExceptionWhenRabbitTemplateFails() {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        doThrow(new RuntimeException("Connection failed")).when(rabbitTemplate).send(anyString(), any(Message.class));

        // When & Then
        assertThrows(PublishingException.class, () -> publisher.publish(topic, payload));
    }

    @Test
    void shouldReturnCorrectPublisherType() {
        // When
        String type = publisher.getPublisherType();

        // Then
        assertEquals("rabbitmq", type);
    }

    @Test
    void shouldReturnTrueWhenHealthy() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(mock(org.springframework.amqp.rabbit.connection.ConnectionFactory.class));
        when(rabbitTemplate.getConnectionFactory().createConnection()).thenReturn(mock(org.springframework.amqp.rabbit.connection.Connection.class));

        // When
        boolean healthy = publisher.isHealthy();

        // Then
        assertTrue(healthy);
    }

    @Test
    void shouldReturnFalseWhenUnhealthy() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean healthy = publisher.isHealthy();

        // Then
        assertFalse(healthy);
    }
} 