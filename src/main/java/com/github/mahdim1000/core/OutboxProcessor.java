package com.github.mahdim1000.core;

import com.github.mahdim1000.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Scheduled processor for outbox events.
 * Runs periodically to process pending and failed messages.
 */
@Component
@ConditionalOnProperty(name = "outbox.processing.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    
    private final OutboxService outboxService;
    
    // Locks to prevent overlapping processing
    private final ReentrantLock pendingProcessingLock = new ReentrantLock();
    private final ReentrantLock failedProcessingLock = new ReentrantLock();

    public OutboxProcessor(OutboxService outboxService, OutboxProperties properties) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${outbox.processing.publish-rate:PT10S}")
    @Async("outboxTaskExecutor")
    public void processPendingMessages() {
        if (!pendingProcessingLock.tryLock()) {
            log.debug("Skipping pending message processing - already in progress");
            return;
        }
        
        try {
            log.debug("Starting pending message processing");
            outboxService.processPendingMessages();
            log.debug("Completed pending message processing");
        } catch (Exception e) {
            log.error("Error during pending message processing: {}", e.getMessage(), e);
        } finally {
            pendingProcessingLock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "${outbox.processing.retry-rate:PT30S}")
    @Async("outboxTaskExecutor")
    public void processFailedMessages() {
        if (!failedProcessingLock.tryLock()) {
            log.debug("Skipping failed message processing - already in progress");
            return;
        }
        
        try {
            log.debug("Starting failed message processing");
            outboxService.processFailedMessages();
            log.debug("Completed failed message processing");
        } catch (Exception e) {
            log.error("Error during failed message processing: {}", e.getMessage(), e);
        } finally {
            failedProcessingLock.unlock();
        }
    }
}
