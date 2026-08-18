package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(outbox: SyncOutbox): Long

    @Query("SELECT * FROM sync_outbox WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SyncOutbox?

    @Query("SELECT * FROM sync_outbox WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getByIdempotencyKey(idempotencyKey: String): SyncOutbox?

    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE nextAttemptAt <= :now
          AND (
            state IN ('PENDING', 'RETRYABLE')
            OR (state = 'IN_FLIGHT' AND (leaseUntil IS NULL OR leaseUntil < :now))
          )
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getEligible(now: Long, limit: Int): List<SyncOutbox>

    @Query(
        """
        UPDATE sync_outbox
        SET state = 'IN_FLIGHT', leaseUntil = :leaseUntil, updatedAt = :now
        WHERE id = :id
          AND (
            state IN ('PENDING', 'RETRYABLE')
            OR (state = 'IN_FLIGHT' AND (leaseUntil IS NULL OR leaseUntil < :now))
          )
        """
    )
    suspend fun claim(id: Long, now: Long, leaseUntil: Long): Int

    @Query(
        """
        UPDATE sync_outbox
        SET state = 'ACKED', leaseUntil = NULL, lastError = NULL, updatedAt = :now
        WHERE id = :id AND state = 'IN_FLIGHT'
        """
    )
    suspend fun markAcked(id: Long, now: Long): Int

    @Query(
        """
        UPDATE sync_outbox
        SET state = 'RETRYABLE', attemptCount = :attemptCount,
            nextAttemptAt = :nextAttemptAt, leaseUntil = NULL,
            lastError = :error, updatedAt = :now
        WHERE id = :id AND state = 'IN_FLIGHT'
        """
    )
    suspend fun markRetryable(
        id: Long,
        attemptCount: Int,
        nextAttemptAt: Long,
        error: String,
        now: Long
    ): Int

    @Query(
        """
        UPDATE sync_outbox
        SET state = 'DEAD_LETTER', attemptCount = :attemptCount,
            nextAttemptAt = 0, leaseUntil = NULL,
            lastError = :error, updatedAt = :now
        WHERE id = :id AND state = 'IN_FLIGHT'
        """
    )
    suspend fun markDeadLetter(id: Long, attemptCount: Int, error: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE state = :state")
    suspend fun countByState(state: String): Int

    @Query("DELETE FROM sync_outbox WHERE state = 'ACKED' AND updatedAt < :before")
    suspend fun deleteAcknowledgedBefore(before: Long): Int

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAll()
}
