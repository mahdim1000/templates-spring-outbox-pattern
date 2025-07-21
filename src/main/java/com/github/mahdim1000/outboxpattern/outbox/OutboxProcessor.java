package com.github.mahdim1000.outboxpattern.outbox;

import com.github.mahdim1000.outboxpattern.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
@ConditionalOnProperty(name = "outbox.processing.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    
    private final OutboxService outboxService;
    private final OutboxProperties properties;
    
    // Locks to prevent overlapping processing
    private final ReentrantLock pendingProcessingLock = new ReentrantLock();
    private final ReentrantLock failedProcessingLock = new ReentrantLock();

    public OutboxProcessor(OutboxService outboxService, OutboxProperties properties) {
        this.outboxService = outboxService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.processing.publish-rate:PT10S}")
    @Async("outboxTaskExecutor")
    public void processPendingMessages() {
        if (!pendingProcessingLock.tryLock()) {
            log.debug("Pending message processing already in progress, skipping...");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            log.debug("Starting pending message processing");
            
            outboxService.processPendingMessage();
            
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > 5000) { // 5 seconds warning threshold
                log.warn("Pending message processing took {}ms, consider increasing processing capacity", processingTime);
            }
            
        } catch (Exception e) {
            log.error("Error during pending message processing", e);
        } finally {
            pendingProcessingLock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "${outbox.retry.initial-delay:PT1M}")
    @Async("outboxTaskExecutor")
    public void processFailedMessages() {
        if (!failedProcessingLock.tryLock()) {
            log.debug("Failed message processing already in progress, skipping...");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            log.debug("Starting failed message processing");
            
            outboxService.processFailedMessage();
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Failed message processing completed in {}ms", processingTime);
            
        } catch (Exception e) {
            log.error("Error during failed message processing", e);
        } finally {
            failedProcessingLock.unlock();
        }
    }
}
