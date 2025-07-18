package com.github.mahdim1000.outboxpattern.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxProcessor {

    private final OutboxService outboxService;

    public OutboxProcessor(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${outbox.publish.rate:10000}")
    public void processPendingMessages() {
        outboxService.processPendingMessage();
    }

    @Scheduled(fixedDelayString = "${outbox.retry.failed.rate:600000}")
    public void processFailedMessages() {
        outboxService.processFailedMessage();
    }
}
