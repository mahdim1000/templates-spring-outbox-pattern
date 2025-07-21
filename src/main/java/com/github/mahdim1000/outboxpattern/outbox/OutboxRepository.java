package com.github.mahdim1000.outboxpattern.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox o
            WHERE o.status = 'PENDING'
            AND o.nextRetryAt <= :now
            AND (o.version = 0 OR NOT EXISTS (
                SELECT 1 FROM Outbox o2
                WHERE o2.aggregateId = o.aggregateId
                AND o2.version < o.version
                AND o2.status != 'PUBLISHED'
                )
            )
            ORDER BY o.createdAt ASC, o.aggregateId ASC, o.version ASC LIMIT :batchSize""")
    List<Outbox> findPendingMessage(Integer batchSize, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox o
            WHERE o.status = 'FAILED'
            AND o.nextRetryAt <= :now
            AND (o.version = 0 OR NOT EXISTS (
                SELECT 1 FROM Outbox o2
                WHERE o2.aggregateId = o.aggregateId
                AND o2.version < o.version
                AND o2.status != 'PUBLISHED'
                )
            )
            ORDER BY o.createdAt ASC, o.aggregateId ASC, o.version ASC LIMIT :batchSize""")
    List<Outbox> findFailedMessages(Integer batchSize, LocalDateTime now);

    @Query("SELECT MAX(o.version) FROM Outbox o WHERE o.aggregateId = :aggregateId")
    Optional<Integer> findMaxVersionByAggregateId(String aggregateId);

    @Query("""
        SELECT COUNT(o) FROM Outbox o
        WHERE o.aggregateId = :aggregateId
        AND o.version < :version
        AND o.status != 'PUBLISHED'
    """)
    long countUnpublishedVersionsBefore(String aggregateId, Integer version);

    @Query("""
        SELECT o FROM Outbox o
        WHERE o.aggregateId = :aggregateId
        AND o.version = :version
    """)
    Optional<Outbox> findByAggregateIdAndVersion(String aggregateId, Integer version);

    // Additional methods for health monitoring and metrics using enum constants
    @Query("SELECT COUNT(o) FROM Outbox o WHERE o.status = :status")
    long countByStatus(Outbox.Status status);

    @Query("""
        SELECT COUNT(o) FROM Outbox o 
        WHERE o.status = 'FAILED' 
        AND o.createdAt < :timestamp
    """)
    long countFailedOlderThan(LocalDateTime timestamp);

    @Query("SELECT COUNT(o) FROM Outbox o WHERE o.status = 'PENDING'")
    long countPendingMessages();

    @Query("SELECT COUNT(o) FROM Outbox o WHERE o.status = 'FAILED'")
    long countFailedMessages();
}
