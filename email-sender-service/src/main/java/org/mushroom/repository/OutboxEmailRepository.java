package org.mushroom.repository;

import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO outbox_emails (
                payload,
                status,
                type,
                dedup_key,
                attempts,
                next_attempt_at,
                created_at,
                updated_at
            )
            VALUES (
                CAST(:payload AS jsonb),
                :status,
                :type,
                :dedupKey,
                :attempts,
                :nextAttemptAt,
                :createdAt,
                :updatedAt
            )
            ON CONFLICT (dedup_key, type) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(
            @Param("payload") String payload,
            @Param("status") String status,
            @Param("type") String type,
            @Param("dedupKey") String dedupKey,
            @Param("attempts") int attempts,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query(value = """
            SELECT id
            FROM outbox_emails
            WHERE status IN ('NEW', 'FAILED')
              AND next_attempt_at <= :now
            ORDER BY next_attempt_at, id
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Long> lockNextBatchIds(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
            UPDATE OutboxEmail e
               SET e.status = :status,
                   e.updatedAt = :updatedAt
             WHERE e.id IN :ids
            """)
    int updateStatusByIds(
            @Param("ids") List<Long> ids,
            @Param("status") OutboxEmailStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}