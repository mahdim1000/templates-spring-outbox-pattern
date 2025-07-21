package com.github.mahdim1000.outboxpattern.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbox")
@Validated
public record OutboxProperties(
    Processing processing,
    Retry retry,
    Publisher publisher
) {
    
    public OutboxProperties {
        if (processing == null) processing = new Processing(null, null, null);
        if (retry == null) retry = new Retry(null, null);
        if (publisher == null) publisher = new Publisher(null, null);
    }

    public record Processing(
        Integer batchSize,
        Duration publishRate,
        Duration lockTimeout
    ) {
        public Processing {
            if (batchSize == null) batchSize = 100;
            if (batchSize < 1 || batchSize > 10000) 
                throw new IllegalArgumentException("batchSize must be between 1 and 10000");
            if (publishRate == null) publishRate = Duration.ofSeconds(10);
            if (lockTimeout == null) lockTimeout = Duration.ofSeconds(30);
        }
    }

    public record Retry(
        Integer maxRetries,
        Duration initialDelay
    ) {
        public Retry {
            if (maxRetries == null) maxRetries = 5;
            if (maxRetries < 1 || maxRetries > 10) 
                throw new IllegalArgumentException("maxRetries must be between 1 and 10");
            if (initialDelay == null) initialDelay = Duration.ofMinutes(1);
        }
    }

    public record Publisher(
        String defaultTopic,
        Duration timeout
    ) {
        public Publisher {
            if (defaultTopic == null) defaultTopic = "outbox-events";
            if (defaultTopic.isBlank()) 
                throw new IllegalArgumentException("defaultTopic cannot be blank");
            if (timeout == null) timeout = Duration.ofSeconds(30);
        }
    }
} 