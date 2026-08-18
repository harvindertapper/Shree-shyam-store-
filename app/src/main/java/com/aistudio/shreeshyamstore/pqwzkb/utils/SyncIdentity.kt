package com.aistudio.shreeshyamstore.pqwzkb.utils

import java.util.UUID

object SyncIdentity {
    const val LEGACY_DEVICE_ID = "legacy-device"
    const val MAX_OUTBOX_ATTEMPTS = 8

    fun newGlobalId(): String = UUID.randomUUID().toString()

    fun legacyGlobalId(tableName: String, localId: Long): String =
        "legacy-${tableName.trim()}-$localId"

    fun idempotencyKey(tableName: String, globalId: String, mutationVersion: Long): String =
        "${tableName.trim()}/${globalId.trim()}/$mutationVersion"

    fun compareMutation(
        leftVersion: Long,
        leftDeviceId: String,
        rightVersion: Long,
        rightDeviceId: String
    ): Int {
        val versionComparison = leftVersion.compareTo(rightVersion)
        return if (versionComparison != 0) {
            versionComparison
        } else {
            leftDeviceId.compareTo(rightDeviceId)
        }
    }

    fun nextRetryAt(now: Long, attemptCount: Int): Long {
        val exponent = (attemptCount - 1).coerceIn(0, 6)
        val delay = 15_000L * (1L shl exponent)
        return now + delay.coerceAtMost(15 * 60_000L)
    }
}
