package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutboxSummary
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Redacted operator-facing sync health categories. */
enum class SyncHealth {
    NEVER_SYNCED,
    HEALTHY,
    PENDING,
    RETRYING,
    BLOCKED
}

/**
 * Safe local sync status. It intentionally contains counts and timestamps only;
 * it never exposes payload JSON, credentials, bearer tokens, customer fields, or
 * record identifiers.
 */
data class SyncHealthSnapshot(
    val health: SyncHealth,
    val lastSyncEpochMs: Long,
    val pendingCount: Int,
    val inFlightCount: Int,
    val retryableCount: Int,
    val deadLetterCount: Int,
    val conflictCount: Int,
    val nextRetryAtEpochMs: Long?,
    val redactedMessage: String
) {
    val totalOutstanding: Int
        get() = pendingCount + inFlightCount + retryableCount + deadLetterCount

    companion object {
        private const val STALE_AFTER_MS = 24 * 60 * 60 * 1000L

        fun from(
            nowEpochMs: Long,
            lastSyncEpochMs: Long,
            outbox: SyncOutboxSummary,
            lastSyncStatus: SyncRunStatus = SyncRunStatus.UNKNOWN
        ): SyncHealthSnapshot {
            val health = when {
                outbox.deadLetterCount > 0 -> SyncHealth.BLOCKED
                lastSyncStatus == SyncRunStatus.FAILED -> SyncHealth.RETRYING
                outbox.retryableCount > 0 -> SyncHealth.RETRYING
                outbox.pendingCount > 0 || outbox.inFlightCount > 0 -> SyncHealth.PENDING
                lastSyncEpochMs <= 0L && lastSyncStatus !in setOf(SyncRunStatus.SUCCESS, SyncRunStatus.NO_CHANGES) -> SyncHealth.NEVER_SYNCED
                nowEpochMs - lastSyncEpochMs > STALE_AFTER_MS && lastSyncStatus != SyncRunStatus.NO_CHANGES -> SyncHealth.NEVER_SYNCED
                else -> SyncHealth.HEALTHY
            }
            val message = when {
                lastSyncStatus == SyncRunStatus.FAILED -> "Sync received only part of the batch or could not complete. Nothing was advanced; retrying is required."
                else -> when (health) {
                    SyncHealth.BLOCKED -> "Sync needs operator review. Some changes are in the dead-letter queue."
                    SyncHealth.RETRYING -> "Sync is retrying after a temporary failure."
                    SyncHealth.PENDING -> "Local changes are waiting to sync."
                    SyncHealth.NEVER_SYNCED -> "No recent completed sync is available."
                    SyncHealth.HEALTHY -> "Sync is healthy."
                }
            }
            return SyncHealthSnapshot(
                health = health,
                lastSyncEpochMs = lastSyncEpochMs.coerceAtLeast(0L),
                pendingCount = outbox.pendingCount.coerceAtLeast(0),
                inFlightCount = outbox.inFlightCount.coerceAtLeast(0),
                retryableCount = outbox.retryableCount.coerceAtLeast(0),
                deadLetterCount = outbox.deadLetterCount.coerceAtLeast(0),
                conflictCount = outbox.conflictCount.coerceAtLeast(0),
                nextRetryAtEpochMs = outbox.nextRetryAtEpochMs?.takeIf { it > 0L },
                redactedMessage = message
            )
        }

        fun empty(): SyncHealthSnapshot = from(
            nowEpochMs = 0L,
            lastSyncEpochMs = 0L,
            outbox = SyncOutboxSummary()
        )
    }
}

/** One canonical parser for the legacy human-readable sync cursor. */
object SyncCursor {
    private val patterns = listOf(
        "dd MMM yyyy, hh:mm:ss a",
        "dd MMM yyyy, hh:mm a"
    )

    fun parse(value: String): Long {
        val raw = value.trim()
        if (raw.isEmpty() || raw.equals("Never Synced", ignoreCase = true)) return 0L
        raw.toLongOrNull()?.let { return it.coerceAtLeast(0L) }
        patterns.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
            val position = ParsePosition(0)
            val parsed = parser.parse(raw, position)
            if (parsed != null && position.index == raw.length) return parsed.time.coerceAtLeast(0L)
        }
        return 0L
    }

    fun format(epochMs: Long): String = SimpleDateFormat(patterns.first(), Locale.ENGLISH).apply {
        isLenient = false
    }.format(Date(epochMs.coerceAtLeast(0L)))
}
