package org.radargps.outboxpattern.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox
            WHERE status = 'PENDING'
            AND nextRetryAt <= CURRENT_TIMESTAMP
            ORDER BY createdAt ASC LIMIT :batchSize""")
    List<Outbox> findPendingMessage(Integer batchSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            FROM Outbox
            WHERE status = 'FAILED'
            AND nextRetryAt <= CURRENT_TIMESTAMP
            ORDER BY createdAt ASC LIMIT :batchSize""")
    List<Outbox> findFailedMessages(Integer batchSize);
}
