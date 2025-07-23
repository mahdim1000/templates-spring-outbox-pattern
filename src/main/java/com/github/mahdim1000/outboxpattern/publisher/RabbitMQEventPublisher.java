package com.github.mahdim1000.outboxpattern.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ implementation of EventPublisher
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.type", havingValue = "rabbitmq")
public class RabbitMQEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;
    
    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @Override
    public void publish(String topic, String payload) throws PublishingException {
        publish(topic, payload, Map.of());
    }
    
    @Override
    public void publish(String topic, String payload, Map<String, String> headers) throws PublishingException {
        try {
            MessageProperties properties = MessagePropertiesBuilder.newInstance()
                    .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                    .build();
            
            // Add custom headers
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(properties::setHeader);
            }
            
            org.springframework.amqp.core.Message message = 
                new org.springframework.amqp.core.Message(payload.getBytes(), properties);
            
            rabbitTemplate.send(topic, message);
            log.debug("Successfully published message to RabbitMQ topic: {}", topic);
            
        } catch (Exception e) {
            log.error("Failed to publish message to RabbitMQ topic '{}': {}", topic, e.getMessage(), e);
            throw new PublishingException("Failed to publish to RabbitMQ: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check by checking if we can get connection factory
            rabbitTemplate.getConnectionFactory().createConnection().close();
            return true;
        } catch (Exception e) {
            log.warn("RabbitMQ health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getPublisherType() {
        return "rabbitmq";
    }
} 