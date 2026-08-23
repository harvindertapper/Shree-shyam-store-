package com.aistudio.shreeshyamstore.pqwzkb.utils

/** Minimal local mutation identity used for deterministic cloud conflict decisions. */
data class SyncRecordVersion(
    val globalId: String,
    val mutationVersion: Long,
    val mutationDeviceId: String,
    val isDeleted: Boolean
)

enum class SyncConflictDisposition {
    APPLY,
    REPLAY,
    STALE,
    CONFLICT
}

/**
 * Conflict policy for a single stable global record. Tombstones are not a
 * separate conflict class: they apply or replay using the same version order,
 * so deletion cannot be lost to a stale live record.
 */
object SyncConflictPolicy {
    fun decide(
        incoming: SyncRecordVersion,
        local: SyncRecordVersion?
    ): SyncConflictDisposition {
        require(incoming.globalId.isNotBlank()) { "Incoming global ID is required" }
        require(incoming.mutationVersion > 0L) { "Incoming mutation version is required" }
        require(incoming.mutationDeviceId.isNotBlank()) { "Incoming mutation device is required" }
        if (local == null) return SyncConflictDisposition.APPLY
        require(local.globalId == incoming.globalId) { "Conflicting records must share a global ID" }
        if (local.mutationVersion == incoming.mutationVersion) {
            return if (local.mutationDeviceId == incoming.mutationDeviceId) {
                SyncConflictDisposition.REPLAY
            } else {
                SyncConflictDisposition.CONFLICT
            }
        }
        return if (incoming.mutationVersion > local.mutationVersion) {
            SyncConflictDisposition.APPLY
        } else {
            SyncConflictDisposition.STALE
        }
    }
}
