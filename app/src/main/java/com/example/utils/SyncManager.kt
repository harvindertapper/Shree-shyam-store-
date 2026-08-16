package com.example.utils

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
    private var isNetworkCallbackRegistered = false

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
                    scheduleInstantSync(context.applicationContext)
                }
            })
            isNetworkCallbackRegistered = true
        } catch (e: Exception) {
            // Defensive handling for restricted environments
        }
    }

    /**
     * Triggers an immediate background sync with exponential retry when network is available.
     * Called whenever a sale is completed, udhaar transaction is made, or product is modified.
     */
    fun scheduleInstantSync(context: Context) {
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
    }

    /**
     * Schedules periodic background sync every 1 hour when connected to network.
     */
    fun schedulePeriodicSync(context: Context) {
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
    }

    /**
     * Returns LiveData/Flow of WorkInfo for the instant sync work
     */
    fun getInstantSyncWorkInfoFlow(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_ONE_TIME_WORK)
}
