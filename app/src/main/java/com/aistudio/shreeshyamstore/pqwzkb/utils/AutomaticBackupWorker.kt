package com.aistudio.shreeshyamstore.pqwzkb.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.shreeshyamstore.pqwzkb.BuildConfig
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.SettingsDataStore
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import com.aistudio.shreeshyamstore.pqwzkb.data.identitySessionOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.net.URI

/**
 * Periodic and mutation-triggered authenticated snapshot backup.
 *
 * This worker is deliberately backup-only: it never downloads or restores data,
 * never persists bearer tokens, and never includes users/password verifiers in
 * the CloudRestorableSnapshot allowlist.
 */
class AutomaticBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settingsStore = SettingsDataStore(applicationContext)
            val settings = settingsStore.settingsFlow.first()
            if (!settings.autoSyncEnabled) {
                return Result.success()
            }

            val session = settings.identitySessionOrNull()
            if (session == null || session.provider != IdentityProvider.FIREBASE) {
                Log.d(TAG, "Skipping automatic backup because Firebase identity is unavailable")
                return Result.success()
            }

            val firebaseUser = AuthManager.currentUser
            if (firebaseUser?.uid != session.uid) {
                Log.w(TAG, "Skipping automatic backup because Firebase identity does not match the local session")
                return Result.success()
            }

            val token = firebaseUser.getIdToken(false).await()?.token?.trim().orEmpty()
            if (token.isEmpty()) {
                return retryOrFailure("Firebase token unavailable")
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val repository = ShopRepository(
                categoryDao = database.categoryDao(),
                productDao = database.productDao(),
                saleDao = database.saleDao(),
                customerDao = database.customerDao(),
                udhaarDao = database.udhaarDao(),
                stockAdjustmentDao = database.stockAdjustmentDao(),
                userDao = database.userDao(),
                database = database,
                settingsDataStore = settingsStore
            )
            val tenant = settingsStore.getOrCreateTenantDeviceContext(session).toTenantScope()
            val snapshot = CloudRestorableSnapshot(
                categories = repository.allCategories.first(),
                products = repository.allProducts.first(),
                sales = repository.allSales.first(),
                saleItems = repository.getAllSaleItems(),
                customers = repository.allCustomers.first(),
                udhaarTransactions = repository.allUdhaarTransactions.first(),
                stockAdjustments = repository.getAllStockAdjustmentsList()
            )
            val envelope = SnapshotEnvelope.create(snapshot, tenant)
            RestoreSnapshotValidator.validate(envelope, tenant)

            val baseUrl = settings.firebaseUrl.ifBlank { BuildConfig.FIREBASE_URL }.trim()
            val trustedHost = URI(BuildConfig.FIREBASE_URL).host?.trim()?.lowercase()
                ?: return retryOrFailure("Backup provider host is not configured")
            val provider = AuthenticatedRestBackupProvider(
                baseUrl = baseUrl,
                basePrefix = settings.firebasePrefix,
                auth = BackupAuthContext.fromFirebaseSession(
                    session = session,
                    tenant = tenant,
                    bearerToken = token
                ),
                allowedHosts = setOf(trustedHost)
            )
            AuthenticatedBackupTableClient(provider).uploadSnapshot(envelope)
            Log.i(TAG, "Automatic tenant snapshot backup completed")
            Result.success()
        } catch (error: BackupProviderException) {
            Log.w(TAG, "Automatic backup provider failure: ${error.javaClass.simpleName}")
            retryOrFailure("backup provider failure")
        } catch (error: RestoreSnapshotException) {
            Log.e(TAG, "Automatic backup snapshot validation failed: ${error.javaClass.simpleName}")
            Result.failure()
        } catch (error: Exception) {
            Log.w(TAG, "Automatic backup failed: ${error.javaClass.simpleName}")
            retryOrFailure("automatic backup failure")
        }
    }

    private fun retryOrFailure(reason: String): Result {
        Log.w(TAG, "$reason; attempt=$runAttemptCount")
        return if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        private const val TAG = "AutomaticBackupWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
