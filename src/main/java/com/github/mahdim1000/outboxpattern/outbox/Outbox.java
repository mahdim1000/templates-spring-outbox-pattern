package com.github.mahdim1000.outboxpattern.outbox;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table
@Entity
public class Outbox {
    @Id
    private String id;
    private String topic;
    private String aggregateId;
    private String payload;
    private Integer version;

    @Enumerated(EnumType.STRING)
    private Status status;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime retryAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;

    public static Outbox create(String topic, String aggregateId, String payload) {
        return create(topic, aggregateId, payload, 1);
    }

    public static Outbox create(String topic, String aggregateId, String payload, Integer version) {
        Outbox outbox = new Outbox();
        outbox.setId(UlidCreator.getUlid().toString());
        outbox.setTopic(topic);
        outbox.setAggregateId(aggregateId);
        outbox.setPayload(payload);
        outbox.setVersion(version);
        outbox.setStatus(Status.PENDING);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setNextRetryAt(LocalDateTime.now());
        return outbox;
    }

    public void handlePublishFailure(String errorMessage) {
        setRetryCount(retryCount + 1);
        setRetryAt(LocalDateTime.now());
        setErrorMessage(errorMessage);
        if (retryCount > 3) {
            setStatus(Status.FAILED);
            setNextRetryAt(nextRetryAt.plusMinutes(retryCount * 2L));
        }
    }

    public void handlePublishSuccess() {
        setStatus(Status.PUBLISHED);
        setPublishedAt(LocalDateTime.now());
    }

    public void incrementVersion() {
        if (version == null) {
            setVersion(1);
        } else {
            setVersion(version + 1);
        }

    }

    // setter
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
    private void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    private void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    // getter
    public String getId() {
        return id;
    }
    public String getTopic() {
        return topic;
    }
    public String getPayload() {
        return payload;
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

    public enum Status {PENDING, PUBLISHED, FAILED}
}
