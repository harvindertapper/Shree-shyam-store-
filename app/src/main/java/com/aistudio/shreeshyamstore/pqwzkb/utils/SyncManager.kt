package com.aistudio.shreeshyamstore.pqwzkb.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val UNIQUE_ONE_TIME_WORK = "shreeshyam_instant_sync"
    private const val UNIQUE_PERIODIC_WORK = "shreeshyam_periodic_sync"
    private const val UNIQUE_AUTOMATIC_BACKUP_ONCE = "shreeshyam_automatic_backup_once"
    private const val UNIQUE_AUTOMATIC_BACKUP_PERIODIC = "shreeshyam_automatic_backup_periodic"
    private var isNetworkCallbackRegistered = false
    @Volatile private var automaticSyncEnabled = false

    /**
     * Registers a live network connectivity callback so that as soon as the device reconnects to the internet,
     * unsynced local store data is automatically synced to the cloud.
     */
    fun registerNetworkCallback(context: Context) {
        if (isNetworkCallbackRegistered) return
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    if (automaticSyncEnabled) {
                        triggerImmediateSync(context.applicationContext)
                    }
                }
            })
            isNetworkCallbackRegistered = true
        } catch (e: Exception) {
            // Defensive handling for restricted environments
        }
    }

    /**
     * Applies the persisted automatic-sync policy to WorkManager. This is the
     * only entrypoint that owns periodic scheduling; manual Sync Now remains
     * available through triggerImmediateSync().
     */
    fun configureAutomaticSync(context: Context, enabled: Boolean) {
        automaticSyncEnabled = enabled
        try {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                registerNetworkCallback(context.applicationContext)
                schedulePeriodicSync(context.applicationContext)
                schedulePeriodicBackup(context.applicationContext)
                triggerImmediateSync(context.applicationContext)
                triggerAutomaticBackup(context.applicationContext)
            } else {
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
                workManager.cancelUniqueWork(UNIQUE_ONE_TIME_WORK)
                workManager.cancelUniqueWork(UNIQUE_AUTOMATIC_BACKUP_ONCE)
                workManager.cancelUniqueWork(UNIQUE_AUTOMATIC_BACKUP_PERIODIC)
            }
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
        }
    }

    /**
     * Triggers an immediate background sync with exponential retry when network is connected.
     */
    fun triggerImmediateSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag("instant_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        } catch (e: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing
        }
    }

    /**
     * Alias for triggerImmediateSync for seamless backward compatibility.
     */
    fun scheduleInstantSync(context: Context) {
        triggerImmediateSync(context)
    }

    /**
     * Schedules periodic background sync every 1 hour when connected to network.
     */
    fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag("periodic_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing
        }
    }

    /** Enqueues a latest-snapshot backup after a successful local mutation. */
    fun triggerAutomaticBackup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<AutomaticBackupWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("automatic_backup")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_AUTOMATIC_BACKUP_ONCE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
        }
    }

    /** Schedules periodic authenticated snapshot backups every six hours. */
    fun schedulePeriodicBackup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag("periodic_automatic_backup")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_AUTOMATIC_BACKUP_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
        }
    }

    /**
     * Returns Flow of WorkInfo for the instant sync work
     */
    fun getInstantSyncWorkInfoFlow(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_ONE_TIME_WORK)
}
