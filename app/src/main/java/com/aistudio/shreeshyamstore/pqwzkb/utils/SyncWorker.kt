package com.aistudio.shreeshyamstore.pqwzkb.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.SettingsDataStore
import com.aistudio.shreeshyamstore.pqwzkb.data.identitySessionOrNull
import kotlinx.coroutines.flow.first
import java.io.IOException

/** Background bidirectional sync worker with network-aware retry semantics. */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!com.aistudio.shreeshyamstore.pqwzkb.BuildConfig.CLOUD_SYNC_ENABLED) {
            Log.d(TAG, "Background sync skipped because this build disables cloud sync")
            return Result.success()
        }
        Log.d(TAG, "Background sync started (attempt $runAttemptCount)")
        val settingsStore = SettingsDataStore(applicationContext)

        return try {
            val settings = settingsStore.settingsFlow.first()
            val session = settings.identitySessionOrNull()

            if (session == null) {
                Log.d(TAG, "Skipping sync because there is no valid identity session")
                return Result.success()
            }
            if (session.provider == IdentityProvider.FIREBASE &&
                AuthManager.currentUser?.uid != session.uid
            ) {
                Log.w(TAG, "Skipping sync because Firebase identity does not match the persisted session")
                return Result.success()
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val service = FirebaseSyncService(database, settingsStore)
            if (!service.pushUpstream(session.shopUid)) {
                settingsStore.updateLastSyncStatus(SyncRunStatus.FAILED)
                return retryOrFailure("upstream push failed")
            }

            val previousCursor = SyncCursor.parse(settings.lastSyncTime)
            val pullResult = service.pullDownstream(session.shopUid, previousCursor)
            when (pullResult.status) {
                SyncPullStatus.APPLIED -> {
                    check(pullResult.nextCursor >= previousCursor) {
                        "Downstream pull returned a regressed cursor"
                    }
                    settingsStore.updateLastSyncTime(SyncCursor.format(pullResult.nextCursor))
                    settingsStore.updateLastSyncStatus(SyncRunStatus.SUCCESS)
                    Log.i(TAG, "Background sync completed with ${pullResult.appliedCount} applied records")
                    Result.success()
                }
                SyncPullStatus.NO_CHANGES -> {
                    settingsStore.updateLastSyncStatus(SyncRunStatus.NO_CHANGES)
                    Log.i(TAG, "Background sync completed with no downstream changes")
                    Result.success()
                }
                SyncPullStatus.FAILED -> {
                    settingsStore.updateLastSyncStatus(SyncRunStatus.FAILED)
                    retryOrFailure("downstream pull failed before atomic commit")
                }
            }
        } catch (error: IOException) {
            Log.w(TAG, "Network error during sync: ${error.message}")
            settingsStore.updateLastSyncStatus(SyncRunStatus.FAILED)
            retryOrFailure("network error")
        } catch (error: Exception) {
            Log.e(TAG, "Sync worker failed: ${error.message}", error)
            settingsStore.updateLastSyncStatus(SyncRunStatus.FAILED)
            retryOrFailure("unexpected error")
        }
    }

    private fun retryOrFailure(reason: String): Result {
        Log.w(TAG, "$reason; attempt=$runAttemptCount")
        return if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
