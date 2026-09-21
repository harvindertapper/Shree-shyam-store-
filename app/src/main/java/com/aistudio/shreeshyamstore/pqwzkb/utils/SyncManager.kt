package com.aistudio.shreeshyamstore.pqwzkb.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import com.aistudio.shreeshyamstore.pqwzkb.BuildConfig
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val UNIQUE_ONE_TIME_WORK = "shreeshyam_instant_sync"
    private const val UNIQUE_PERIODIC_WORK = "shreeshyam_periodic_sync"
    private const val UNIQUE_AUTOMATIC_BACKUP_ONCE = "shreeshyam_automatic_backup_once"
    private const val UNIQUE_AUTOMATIC_BACKUP_PERIODIC = "shreeshyam_automatic_backup_periodic"

    private val callbackLock = Any()
    private var isNetworkCallbackRegistered = false
    private var registeredConnectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var automaticSyncEnabled = false

    /**
     * Registers one live network callback so that a reconnect can enqueue work.
     * The callback is explicitly paired with unregisterNetworkCallback; it must
     * never outlive the automatic-sync policy that created it.
     */
    fun registerNetworkCallback(context: Context) {
        if (!BuildConfig.CLOUD_SYNC_ENABLED) return
        synchronized(callbackLock) {
            if (isNetworkCallbackRegistered) return
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? ConnectivityManager ?: return
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        if (automaticSyncEnabled) {
                            triggerImmediateSync(context.applicationContext)
                        }
                    }
                }
                connectivityManager.registerNetworkCallback(request, callback)
                registeredConnectivityManager = connectivityManager
                networkCallback = callback
                isNetworkCallbackRegistered = true
            } catch (_: Exception) {
                // Defensive handling for restricted environments.
            }
        }
    }

    /** Unregisters the reconnect callback when automatic sync is disabled. */
    fun unregisterNetworkCallback(context: Context) {
        synchronized(callbackLock) {
            val callback = networkCallback ?: return
            try {
                val connectivityManager = registeredConnectivityManager
                    ?: context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Defensive handling for already-unregistered/restricted callbacks.
            } finally {
                networkCallback = null
                registeredConnectivityManager = null
                isNetworkCallbackRegistered = false
            }
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
                unregisterNetworkCallback(context.applicationContext)
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
                workManager.cancelUniqueWork(UNIQUE_ONE_TIME_WORK)
                workManager.cancelUniqueWork(UNIQUE_AUTOMATIC_BACKUP_ONCE)
                workManager.cancelUniqueWork(UNIQUE_AUTOMATIC_BACKUP_PERIODIC)
            }
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
        }
    }

    /** Triggers an immediate background sync with network-aware retry. */
    fun triggerImmediateSync(context: Context) {
        if (!BuildConfig.CLOUD_SYNC_ENABLED) return
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag("instant_sync")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
        }
    }

    /** Alias retained for existing callers. */
    fun scheduleInstantSync(context: Context) = triggerImmediateSync(context)

    /** Schedules periodic background sync every hour while connected. */
    fun schedulePeriodicSync(context: Context) {
        if (!BuildConfig.CLOUD_SYNC_ENABLED) return
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("periodic_sync")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (_: Throwable) {
            // Defensive handling if WorkManager is not initialized or in testing.
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

    /** Returns the current WorkManager state for the instant sync work. */
    fun getInstantSyncWorkInfoFlow(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_ONE_TIME_WORK)
}
