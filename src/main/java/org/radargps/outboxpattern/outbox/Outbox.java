package org.radargps.outboxpattern.outbox;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table
@Entity
public class Outbox {
    @Id
    private String id;
    private String topic;
    private String payload;

    @Enumerated(EnumType.STRING)
    private Status status;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime retryAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;

    public static Outbox create(String topic, String payload) {
        Outbox outbox = new Outbox();
        outbox.setId(UlidCreator.getUlid().toString());
        outbox.setTopic(topic);
        outbox.setPayload(payload);
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
    }

    // setter
    private void setId(String id) {
        this.id = id;
    }
    private void setTopic(String topic) {
        this.topic = topic;
    }
    private void setPayload(String payload) {
        this.payload = payload;
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

    enum Status {PENDING, PUBLISHED, FAILED}
}
