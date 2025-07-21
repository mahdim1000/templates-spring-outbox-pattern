package com.github.mahdim1000.outboxpattern.outbox;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

@Table
@Entity
public class Outbox {
    @Id
    private String id;
    private String topic;
    private String aggregateId;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    @Column(columnDefinition = "TEXT")
    private String headers;
    
    private Integer version;

    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime retryAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;
    private LocalDateTime deadLetterAt;
    private boolean retryable = true;

    public static Outbox create(String topic, String aggregateId, String payload, String headers, Integer version, boolean retryable) {
        Outbox outbox = new Outbox();
        outbox.setId(UlidCreator.getUlid().toString());
        outbox.setTopic(topic);
        outbox.setAggregateId(aggregateId);
        outbox.setPayload(payload);
        outbox.setHeaders(headers);
        outbox.setVersion(version);
        outbox.setStatus(Status.PENDING);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setNextRetryAt(LocalDateTime.now());
        outbox.setRetryable(retryable);
        return outbox;
    }

    public void handlePublishFailure(String errorMessage) {
        setRetryCount(retryCount + 1);
        setRetryAt(LocalDateTime.now());
        setErrorMessage(errorMessage);
        if (!retryable) {
            setStatus(Status.DEAD_LETTER);
            setDeadLetterAt(LocalDateTime.now());
        } else if (retryCount > 3) {
            setStatus(Status.FAILED);
            setNextRetryWithBackoff();
        }
    }

    public void handlePublishSuccess() {
        setStatus(Status.PUBLISHED);
        setPublishedAt(LocalDateTime.now());
        setErrorMessage(null);
    }

    public void moveToDeadLetter() {
        setStatus(Status.DEAD_LETTER);
        setDeadLetterAt(LocalDateTime.now());
    }


    // Setters
    private void setId(String id) {
        this.id = id;
    }
    
    private void setTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        this.topic = topic;
    }
    
    private void setAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.isEmpty()) {
            throw new IllegalArgumentException("AggregateId cannot be null or empty");
        }
        this.aggregateId = aggregateId;
    }
    
    private void setPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        this.payload = payload;
    }
    
    public void setHeaders(String headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        this.headers = headers;
    }
    
    private void setVersion(Integer version) {
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version cannot be null or less than 0");
        }
        this.version = version;
    }
    
    private void setStatus(Status status) {
        this.status = status;
    }
    
    private void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    private void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    private void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    private void setRetryAt(LocalDateTime retryAt) {
        this.retryAt = retryAt;
    }
    private void setNextRetryWithBackoff() {
        long baseDelay = Math.min(retryCount * retryCount * 2L, 1440);
        long jitter = new java.util.Random().nextInt(5);
        long totalDelay = baseDelay + jitter;
        setNextRetryAt(LocalDateTime.now().plusMinutes(totalDelay));
    }

    private void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    private void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
    
    private void setDeadLetterAt(LocalDateTime deadLetterAt) {
        this.deadLetterAt = deadLetterAt;
    }
    
    private void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    // Getters
    public String getId() {
        return id;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public String getHeaders() {
        return headers;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getRetryAt() {
        return retryAt;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }
    
    public LocalDateTime getDeadLetterAt() {
        return deadLetterAt;
    }
    
    public boolean isRetryable() {
        return retryable;
    }

    public enum Status {
        PENDING, 
        PUBLISHED, 
        FAILED, 
        DEAD_LETTER
    }
}
