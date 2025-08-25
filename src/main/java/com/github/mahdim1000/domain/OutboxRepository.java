package com.github.mahdim1000.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OutboxEntity operations.
 * This is an internal repository and should not be exposed to library users.
 */
public interface OutboxRepository extends JpaRepository<OutboxEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM OutboxEntity o
            WHERE o.status = 'PENDING'
            AND o.nextRetryAt <= :now
            AND (o.version = 0 OR NOT EXISTS (
                SELECT 1 FROM OutboxEntity o2
                WHERE o2.aggregateId = o.aggregateId
                AND o2.version < o.version
                AND o2.status != 'PUBLISHED'
                )
            )
            ORDER BY o.createdAt ASC, o.aggregateId ASC, o.version ASC
            LIMIT :batchSize""")
    List<OutboxEntity> findPendingMessages(@Param("batchSize") Integer batchSize, 
                                          @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM OutboxEntity o
            WHERE o.status = 'FAILED'
            AND o.nextRetryAt <= :now
            AND (o.version = 0 OR NOT EXISTS (
                SELECT 1 FROM OutboxEntity o2
                WHERE o2.aggregateId = o.aggregateId
                AND o2.version < o.version
                AND o2.status != 'PUBLISHED'
                )
            )
            ORDER BY o.createdAt ASC, o.aggregateId ASC, o.version ASC
            LIMIT :batchSize""")
    List<OutboxEntity> findFailedMessages(@Param("batchSize") Integer batchSize, 
                                         @Param("now") LocalDateTime now);

    @Query("SELECT MAX(o.version) FROM OutboxEntity o WHERE o.aggregateId = :aggregateId")
    Optional<Integer> findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);

    @Query("""
        SELECT COUNT(o) FROM OutboxEntity o
        WHERE o.aggregateId = :aggregateId
        AND o.version < :version
        AND o.status != 'PUBLISHED'
    """)
    long countUnpublishedVersionsBefore(@Param("aggregateId") String aggregateId, 
                                       @Param("version") Integer version);

    @Query("""
        SELECT o FROM OutboxEntity o
        WHERE o.aggregateId = :aggregateId
        AND o.version = :version
    """)
    Optional<OutboxEntity> findByAggregateIdAndVersion(@Param("aggregateId") String aggregateId, 
                                                      @Param("version") Integer version);

    // Metrics queries
    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") OutboxEntity.Status status);

    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.status = 'PENDING'")
    long countPendingMessages();

    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.status = 'FAILED'")
    long countFailedMessages();

    @Query("""
        SELECT COUNT(o) FROM OutboxEntity o 
        WHERE o.status = 'FAILED' 
        AND o.createdAt < :timestamp
    """)
    long countFailedOlderThan(@Param("timestamp") LocalDateTime timestamp);

    // Utility queries for testing and monitoring
    @Query("SELECT o FROM OutboxEntity o WHERE o.aggregateId = :aggregateId ORDER BY o.version ASC")
    List<OutboxEntity> findByAggregateIdOrderByVersionAsc(@Param("aggregateId") String aggregateId);

    @Query("SELECT o FROM OutboxEntity o WHERE o.aggregateId = :aggregateId")
    List<OutboxEntity> findByAggregateId(@Param("aggregateId") String aggregateId);
}
