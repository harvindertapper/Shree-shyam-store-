package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.IdentityProvider
import com.example.data.SettingsDataStore
import com.example.data.identitySessionOrNull
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Background bidirectional sync worker with network-aware retry semantics. */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
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
                return retryOrFailure("upstream push failed")
            }

            val previousCursor = parseSyncTimestamp(settings.lastSyncTime)
            val newCursor = service.pullDownstream(session.shopUid, previousCursor)
            if (newCursor <= previousCursor) {
                // A successful pull always returns a fresh high-water mark. Do
                // not advance the stored cursor when the service kept it after
                // a transient failure.
                return retryOrFailure("downstream pull failed")
            }

            val formatted = FORMATTER.format(Date(newCursor.coerceAtLeast(System.currentTimeMillis())))
            settingsStore.updateLastSyncTime(formatted)
            Log.i(TAG, "Background sync completed at $formatted")
            Result.success()
        } catch (error: IOException) {
            Log.w(TAG, "Network error during sync: ${error.message}")
            retryOrFailure("network error")
        } catch (error: Exception) {
            Log.e(TAG, "Sync worker failed: ${error.message}", error)
            retryOrFailure("unexpected error")
        }
    }

    private fun retryOrFailure(reason: String): Result {
        Log.w(TAG, "$reason; attempt=$runAttemptCount")
        return if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
    }

    private fun parseSyncTimestamp(value: String): Long {
        val raw = value.trim()
        if (raw.isEmpty() || raw.equals("Never Synced", ignoreCase = true)) return 0L
        raw.toLongOrNull()?.let { return it }

        TIMESTAMP_PATTERNS.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
            val position = ParsePosition(0)
            val parsed = parser.parse(raw, position)
            if (parsed != null && position.index == raw.length) return parsed.time
        }
        return 0L
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        private val TIMESTAMP_PATTERNS = listOf(
            "dd MMM yyyy, hh:mm:ss a",
            "dd MMM yyyy, hh:mm a"
        )
        private val FORMATTER = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).apply {
            isLenient = false
        }
    }
}
