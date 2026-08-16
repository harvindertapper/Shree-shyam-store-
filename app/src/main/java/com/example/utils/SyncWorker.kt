package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CoroutineWorker responsible for background bidirectional synchronization
 * with Google Cloud Firestore using FirebaseSyncService.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.d("SyncWorker", "Background Sync started (Attempt: $runAttemptCount)")

        val database = AppDatabase.getDatabase(context)
        val settingsDataStore = SettingsDataStore(context)

        return try {
            val settings = settingsDataStore.settingsFlow.first()

            // 1. Check user login status; skip sync if offline/unauthenticated
            val shopUid = settings.loggedInUid.ifBlank {
                settings.loggedInEmail.ifBlank {
                    settings.loggedInUsername
                }
            }.trim()

            if (!settings.isUserLoggedIn || shopUid.isBlank()) {
                Log.d("SyncWorker", "User not logged in or shopUid empty. Skipping sync.")
                return Result.success()
            }

            // 2. Instantiate FirebaseSyncService
            val syncService = FirebaseSyncService(database, settingsDataStore)

            // 3. Push upstream local changes
            val pushSuccess = syncService.pushUpstream(shopUid)
            Log.d("SyncWorker", "Upstream push result: $pushSuccess")

            // 4. Parse lastSyncTimestamp and pull downstream changes
            val lastSyncTimestamp = parseSyncTimestamp(settings.lastSyncTime)
            val newSyncTimestamp = syncService.pullDownstream(shopUid, lastSyncTimestamp)
            Log.d("SyncWorker", "Downstream pull completed. New timestamp: $newSyncTimestamp")

            // 5. Format and persist new last sync time string
            val formattedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(newSyncTimestamp))
            settingsDataStore.updateLastSyncTime(formattedTime)

            Log.i("SyncWorker", "Background Sync successfully finished at $formattedTime")
            Result.success()
        } catch (e: IOException) {
            Log.w("SyncWorker", "Network IO exception during sync, scheduling retry: ${e.message}")
            Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync worker exception: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun parseSyncTimestamp(lastSyncTimeStr: String): Long {
        if (lastSyncTimeStr.isBlank() || lastSyncTimeStr.equals("Never Synced", ignoreCase = true)) {
            return 0L
        }
        return try {
            lastSyncTimeStr.toLong()
        } catch (e: NumberFormatException) {
            try {
                val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                format.parse(lastSyncTimeStr)?.time ?: 0L
            } catch (e: Exception) {
                try {
                    val formatSec = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH)
                    formatSec.parse(lastSyncTimeStr)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }
    }
}
