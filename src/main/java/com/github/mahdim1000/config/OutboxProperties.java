package com.github.mahdim1000.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for the Outbox Pattern library.
 * 
 * All properties are prefixed with 'outbox' and have sensible defaults.
 * 
 * Example application.yml:
 * <pre>
 * outbox:
 *   processing:
 *     enabled: true
 *     batch-size: 100
 *     publish-rate: PT10S
 *     retry-rate: PT30S
 *   retry:
 *     max-retries: 5
 *     initial-delay: PT1M
 *   publisher:
 *     type: logging
 *     default-topic: outbox-events
 *     timeout: PT30S
 * </pre>
 */
@ConfigurationProperties(prefix = "outbox")
@Validated
public record OutboxProperties(
    Processing processing,
    Retry retry,
    Publisher publisher
) {
    
    public OutboxProperties {
        if (processing == null) processing = new Processing(null, null, null, null);
        if (retry == null) retry = new Retry(null, null);
        if (publisher == null) publisher = new Publisher(null, null, null);
    }

    /**
     * Configuration for message processing behavior.
     */
    public record Processing(
        Boolean enabled,
        Integer batchSize,
        Duration publishRate,
        Duration retryRate
    ) {
        public Processing {
            if (enabled == null) enabled = true;
            if (batchSize == null) batchSize = 100;
            if (batchSize < 1 || batchSize > 10000) {
                throw new IllegalArgumentException("batchSize must be between 1 and 10000");
            }
            if (publishRate == null) publishRate = Duration.ofSeconds(10);
            if (retryRate == null) retryRate = Duration.ofSeconds(30);
        }
    }

    /**
     * Configuration for retry behavior when publishing fails.
     */
    public record Retry(
        Integer maxRetries,
        Duration initialDelay
    ) {
        public Retry {
            if (maxRetries == null) maxRetries = 5;
            if (maxRetries < 1 || maxRetries > 10) {
                throw new IllegalArgumentException("maxRetries must be between 1 and 10");
            }
            if (initialDelay == null) initialDelay = Duration.ofMinutes(1);
        }
    }

    /**
     * Configuration for the event publisher.
     */
    public record Publisher(
        String type,
        String defaultTopic,
        Duration timeout
    ) {
        public Publisher {
            if (type == null) type = "logging";
            if (defaultTopic == null) defaultTopic = "outbox-events";
            if (defaultTopic.isBlank()) {
                throw new IllegalArgumentException("defaultTopic cannot be blank");
            }
            if (timeout == null) timeout = Duration.ofSeconds(30);
        }
    }
}