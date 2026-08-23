package com.aistudio.shreeshyamstore.pqwzkb.utils

/** Redacted outcome of one complete downstream pull attempt. */
enum class SyncPullStatus {
    APPLIED,
    NO_CHANGES,
    FAILED
}

data class SyncPullResult(
    val status: SyncPullStatus,
    val previousCursor: Long,
    val nextCursor: Long = previousCursor,
    val receivedCount: Int = 0,
    val appliedCount: Int = 0
) {
    init {
        require(previousCursor >= 0L) { "Previous sync cursor cannot be negative" }
        require(nextCursor >= previousCursor) { "Downstream cursor cannot regress" }
        require(receivedCount >= 0) { "Received record count cannot be negative" }
        require(appliedCount >= 0) { "Applied record count cannot be negative" }
        require(appliedCount <= receivedCount) { "Applied records cannot exceed received records" }
    }
}

/** Last known worker-level sync outcome, persisted only as a redacted status. */
enum class SyncRunStatus {
    UNKNOWN,
    SUCCESS,
    NO_CHANGES,
    FAILED
}
