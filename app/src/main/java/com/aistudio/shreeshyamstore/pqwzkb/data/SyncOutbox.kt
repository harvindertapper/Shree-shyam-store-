package com.aistudio.shreeshyamstore.pqwzkb.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SyncOutboxState {
    const val PENDING = "PENDING"
    const val IN_FLIGHT = "IN_FLIGHT"
    const val ACKED = "ACKED"
    const val RETRYABLE = "RETRYABLE"
    const val DEAD_LETTER = "DEAD_LETTER"
}

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["state", "nextAttemptAt"]),
        Index(value = ["tableName", "globalId", "mutationVersion"], unique = true)
    ]
)
data class SyncOutbox(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val globalId: String,
    val localId: Long,
    val mutationVersion: Long,
    val mutationDeviceId: String,
    val idempotencyKey: String,
    val payloadJson: String,
    val tombstone: Boolean,
    val state: String = SyncOutboxState.PENDING,
    val attemptCount: Int = 0,
    val nextAttemptAt: Long = 0L,
    val leaseUntil: Long? = null,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Aggregate counts used for redacted operator-facing sync health. */
data class SyncOutboxSummary(
    val pendingCount: Int = 0,
    val inFlightCount: Int = 0,
    val retryableCount: Int = 0,
    val deadLetterCount: Int = 0,
    val conflictCount: Int = 0,
    val nextRetryAtEpochMs: Long? = null
)
