package com.github.mahdim1000.domain;

import com.github.f4b6a3.ulid.UlidCreator;
import com.github.mahdim1000.api.OutboxEvent;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JPA entity representing an outbox event.
 * This is an internal domain entity and should not be exposed to library users.
 */
@Table(name = "outbox")
@Entity
public class OutboxEntity implements OutboxEvent {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String topic;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    @Column(columnDefinition = "TEXT")
    private String headers;
    
    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false)
    private int retryCount = 0;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime retryAt;
    
    @Column(nullable = false)
    private LocalDateTime nextRetryAt;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime deadLetterAt;
    
    @Column(nullable = false)
    private boolean retryable = true;

    // JPA requires default constructor
    protected OutboxEntity() {}

    public static OutboxEntity create(String topic, String aggregateId, String payload, 
                                     String headers, Integer version, boolean retryable) {
        var entity = new OutboxEntity();
        entity.id = UlidCreator.getUlid().toString();
        entity.topic = validateTopic(topic);
        entity.aggregateId = validateAggregateId(aggregateId);
        entity.payload = validatePayload(payload);
        entity.headers = headers;
        entity.version = validateVersion(version);
        entity.status = Status.PENDING;
        entity.createdAt = LocalDateTime.now();
        entity.nextRetryAt = LocalDateTime.now();
        entity.retryable = retryable;
        return entity;
    }

    public void markAsPublished() {
        this.status = Status.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void recordFailure(String errorMessage, int maxRetries, Duration initialDelay) {
        this.retryCount++;
        this.retryAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        
        if (!this.retryable || this.retryCount >= maxRetries) {
            markAsDeadLetter();
        } else {
            this.status = Status.FAILED;
            this.nextRetryAt = calculateBackoff(initialDelay);
        }
    }

    private LocalDateTime calculateBackoff(Duration initialDelay) {
        double multiplier = Math.pow(2, this.retryCount - 1);
        long delaySeconds = (long) (initialDelay.getSeconds() * multiplier);
        long jitter = ThreadLocalRandom.current().nextLong(0, initialDelay.getSeconds() / 2 + 1);
        return LocalDateTime.now().plusSeconds(delaySeconds + jitter);
    }

    private void markAsDeadLetter() {
        this.status = Status.DEAD_LETTER;
        this.deadLetterAt = LocalDateTime.now();
    }

    // OutboxEvent interface implementation
    @Override
    public String getId() { return id; }
    
    @Override
    public String getTopic() { return topic; }
    
    @Override
    public String getAggregateId() { return aggregateId; }
    
    @Override
    public String getPayload() { return payload; }
    
    @Override
    public Map<String, String> getHeaders() {
        // This would need to be parsed from JSON in a real implementation
        // For simplicity, returning empty map here
        return Map.of();
    }
    
    @Override
    public Integer getVersion() { return version; }
    
    @Override
    public EventStatus getStatus() {
        return switch (status) {
            case PENDING -> EventStatus.PENDING;
            case PUBLISHED -> EventStatus.PUBLISHED;
            case FAILED -> EventStatus.FAILED;
            case DEAD_LETTER -> EventStatus.DEAD_LETTER;
        };
    }
    
    @Override
    public String getErrorMessage() { return errorMessage; }
    
    @Override
    public int getRetryCount() { return retryCount; }
    
    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    @Override
    public LocalDateTime getPublishedAt() { return publishedAt; }
    
    @Override
    public boolean isRetryable() { return retryable; }

    // Additional getters for internal use
    public String getHeadersJson() { return headers; }
    public LocalDateTime getRetryAt() { return retryAt; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public LocalDateTime getDeadLetterAt() { return deadLetterAt; }
    public Status getInternalStatus() { return status; }

    // Validation methods
    private static String validateTopic(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        return topic.trim();
    }
    
    private static String validateAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("AggregateId cannot be null or empty");
        }
        return aggregateId.trim();
    }
    
    private static String validatePayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        return payload;
    }
    
    private static Integer validateVersion(Integer version) {
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version cannot be null or negative");
        }
        return version;
    }

    /**
     * Internal status enum for JPA mapping
     */
    public enum Status {
        PENDING, 
        PUBLISHED, 
        FAILED, 
        DEAD_LETTER
    }
}
