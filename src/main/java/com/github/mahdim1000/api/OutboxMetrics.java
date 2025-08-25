package com.github.mahdim1000.api;

/**
 * Metrics snapshot for monitoring outbox health and performance.
 */
public record OutboxMetrics(
    long pendingCount,
    long failedCount,
    long publishedCount,
    long deadLetterCount
) {
    
    /**
     * @return total number of events in the outbox
     */
    public long totalCount() {
        return pendingCount + failedCount + publishedCount + deadLetterCount;
    }
    
    /**
     * @return true if there are no failed or dead letter events
     */
    public boolean isHealthy() {
        return failedCount == 0 && deadLetterCount == 0;
    }
    
    /**
     * @return percentage of successfully published events
     */
    public double successRate() {
        long total = totalCount();
        return total == 0 ? 100.0 : (publishedCount * 100.0) / total;
    }
}
