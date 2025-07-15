package org.radargps.outboxpattern.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox
            WHERE status = 'PENDING'
            AND nextRetryAt <= :now
            ORDER BY createdAt ASC LIMIT :batchSize""")
    List<Outbox> findPendingMessage(Integer batchSize, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox
            WHERE status = 'FAILED'
            AND nextRetryAt <= :now
            ORDER BY createdAt ASC LIMIT :batchSize""")
    List<Outbox> findFailedMessages(Integer batchSize, LocalDateTime now);
}
