package com.aistudio.shreeshyamstore.pqwzkb.utils

/**
 * Coordinates one complete downstream pull. Fetching must finish before the
 * applier is invoked; any fetch, validation, or apply failure retains the
 * previous cursor and returns a redacted failure result.
 */
class SyncPullCoordinator(
    private val fetchBatch: suspend (previousCursor: Long) -> SyncPullBatch,
    private val applyBatch: suspend (batch: SyncPullBatch) -> Unit
) {
    suspend fun run(previousCursor: Long): SyncPullResult {
        return try {
            val batch = fetchBatch(previousCursor)
            batch.validate(previousCursor)
            if (batch.receivedCount == 0) {
                SyncPullResult(
                    status = SyncPullStatus.NO_CHANGES,
                    previousCursor = previousCursor,
                    nextCursor = previousCursor
                )
            } else {
                applyBatch(batch)
                SyncPullResult(
                    status = SyncPullStatus.APPLIED,
                    previousCursor = previousCursor,
                    nextCursor = batch.highWaterMark,
                    receivedCount = batch.receivedCount,
                    appliedCount = batch.appliedCount
                )
            }
        } catch (_: Exception) {
            SyncPullResult(
                status = SyncPullStatus.FAILED,
                previousCursor = previousCursor
            )
        }
    }
}
