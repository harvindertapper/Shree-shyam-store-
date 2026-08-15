package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.d("SyncWorker", "Background Sync started (Attempt: $runAttemptCount)")

        val database = AppDatabase.getDatabase(context)
        val repository = ShopRepository(
            categoryDao = database.categoryDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao(),
            userDao = database.userDao(),
            database = database
        )
        val settingsDataStore = SettingsDataStore(context)
        val settings = settingsDataStore.settingsFlow.first()

        val url = BuildConfig.FIREBASE_URL
        if (url.isBlank()) {
            Log.w("SyncWorker", "Firebase URL is blank, skipping sync")
            return Result.success()
        }

        val userIdentifier = settings.loggedInUsername.ifBlank {
            settings.loggedInEmail.ifBlank { "default_store" }
        }.lowercase().trim()

        val hashedUser = hashString(userIdentifier)
        val prefix = "shreeshyam_sync/users/$hashedUser"

        return try {
            // 1. Verify connection
            val isConnected = FirebaseSyncService.testFirebaseConnection(url)
            if (!isConnected) {
                Log.w("SyncWorker", "Network test failed, scheduling retry")
                return if (runAttemptCount < 5) Result.retry() else Result.failure()
            }

            // 2. Upload all tables atomically
            val catList = repository.allCategories.first()
            val catSuccess = FirebaseSyncService.uploadTable(url, prefix, "categories", catList, Category::class.java)

            val prodList = repository.allProducts.first()
            val prodSuccess = FirebaseSyncService.uploadTable(url, prefix, "products", prodList, Product::class.java)

            val salesList = repository.allSales.first()
            val salesSuccess = FirebaseSyncService.uploadTable(url, prefix, "sales", salesList, Sale::class.java)

            val saleItemsList = repository.getAllSaleItems()
            val itemsSuccess = FirebaseSyncService.uploadTable(url, prefix, "sale_items", saleItemsList, SaleItem::class.java)

            val customersList = repository.allCustomers.first()
            val custSuccess = FirebaseSyncService.uploadTable(url, prefix, "customers", customersList, Customer::class.java)

            val udhaarList = repository.allUdhaarTransactions.first()
            val udhaarSuccess = FirebaseSyncService.uploadTable(url, prefix, "udhaar_transactions", udhaarList, UdhaarTransaction::class.java)

            val adjList = repository.getAllStockAdjustmentsList()
            val adjSuccess = FirebaseSyncService.uploadTable(url, prefix, "stock_adjustments", adjList, StockAdjustment::class.java)

            val usersList = repository.getAllUsers()
            val usersSuccess = FirebaseSyncService.uploadTable(url, prefix, "users", usersList, User::class.java)

            val allSuccessful = catSuccess && prodSuccess && salesSuccess && itemsSuccess &&
                    custSuccess && udhaarSuccess && adjSuccess && usersSuccess

            if (allSuccessful) {
                val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).format(Date())
                settingsDataStore.updateLastSyncTime(timeStr)
                Log.i("SyncWorker", "Background Sync successfully completed at $timeStr")
                Result.success()
            } else {
                Log.w("SyncWorker", "Partial upload failure, retrying...")
                if (runAttemptCount < 5) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync exception: ${e.message}", e)
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder()
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            input.replace(Regex("[.\\s$#\\[\\]/]"), "_")
        }
    }
}
