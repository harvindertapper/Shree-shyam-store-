package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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

        val uid = settings.loggedInEmail.ifBlank {
            settings.loggedInUsername.ifBlank { "store_offline" }
        }.lowercase().trim().replace(Regex("[.#$\\[\\]/\\s]"), "_")

        return try {
            var firestoreSynced = false
            // 1. Attempt Firestore Batch Sync if Firebase is configured
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val shopRef = db.collection("shops").document(uid)

                    // Unsynced Categories
                    val unsyncedCats = repository.getUnsyncedCategories()
                    if (unsyncedCats.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedCats.forEach { cat ->
                            val doc = shopRef.collection("categories").document(cat.id.toString())
                            batch.set(doc, cat, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markCategoriesSynced(unsyncedCats.map { it.id })
                    }

                    // Unsynced Products
                    val unsyncedProds = repository.getUnsyncedProducts()
                    if (unsyncedProds.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedProds.forEach { prod ->
                            val doc = shopRef.collection("products").document(prod.id.toString())
                            batch.set(doc, prod, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markProductsSynced(unsyncedProds.map { it.id })
                    }

                    // Unsynced Sales
                    val unsyncedSales = repository.getUnsyncedSales()
                    if (unsyncedSales.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedSales.forEach { sale ->
                            val doc = shopRef.collection("sales").document(sale.id.toString())
                            batch.set(doc, sale, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markSalesSynced(unsyncedSales.map { it.id })
                    }

                    // Unsynced Sale Items
                    val unsyncedItems = repository.getUnsyncedSaleItems()
                    if (unsyncedItems.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedItems.forEach { item ->
                            val doc = shopRef.collection("sale_items").document(item.id.toString())
                            batch.set(doc, item, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markSaleItemsSynced(unsyncedItems.map { it.id })
                    }

                    // Unsynced Customers
                    val unsyncedCustomers = repository.getUnsyncedCustomers()
                    if (unsyncedCustomers.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedCustomers.forEach { cust ->
                            val doc = shopRef.collection("customers").document(cust.id.toString())
                            batch.set(doc, cust, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markCustomersSynced(unsyncedCustomers.map { it.id })
                    }

                    // Unsynced Udhaar
                    val unsyncedUdhaar = repository.getUnsyncedUdhaarTransactions()
                    if (unsyncedUdhaar.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedUdhaar.forEach { tx ->
                            val doc = shopRef.collection("udhaar_transactions").document(tx.id.toString())
                            batch.set(doc, tx, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markUdhaarTransactionsSynced(unsyncedUdhaar.map { it.id })
                    }

                    // Unsynced Adjustments
                    val unsyncedAdj = repository.getUnsyncedStockAdjustments()
                    if (unsyncedAdj.isNotEmpty()) {
                        val batch = db.batch()
                        unsyncedAdj.forEach { adj ->
                            val doc = shopRef.collection("stock_adjustments").document(adj.id.toString())
                            batch.set(doc, adj, SetOptions.merge())
                        }
                        batch.commit().await()
                        repository.markStockAdjustmentsSynced(unsyncedAdj.map { it.id })
                    }

                    firestoreSynced = true
                }
            } catch (e: Exception) {
                Log.w("SyncWorker", "Firestore direct sync skipped/failed: ${e.message}")
            }

            // 2. Secondary/Fallback Sync to REST service if Firebase REST URL is provided
            val url = BuildConfig.FIREBASE_URL
            if (url.isNotBlank()) {
                val isConnected = FirebaseSyncService.testFirebaseConnection(url)
                if (isConnected) {
                    val hashedUser = hashString(uid)
                    val prefix = "shreeshyam_sync/users/$hashedUser"

                    val catList = repository.allCategories.first()
                    FirebaseSyncService.uploadTable(url, prefix, "categories", catList, Category::class.java)

                    val prodList = repository.allProducts.first()
                    FirebaseSyncService.uploadTable(url, prefix, "products", prodList, Product::class.java)

                    val salesList = repository.allSales.first()
                    FirebaseSyncService.uploadTable(url, prefix, "sales", salesList, Sale::class.java)

                    val saleItemsList = repository.getAllSaleItems()
                    FirebaseSyncService.uploadTable(url, prefix, "sale_items", saleItemsList, SaleItem::class.java)

                    val customersList = repository.allCustomers.first()
                    FirebaseSyncService.uploadTable(url, prefix, "customers", customersList, Customer::class.java)

                    val udhaarList = repository.allUdhaarTransactions.first()
                    FirebaseSyncService.uploadTable(url, prefix, "udhaar_transactions", udhaarList, UdhaarTransaction::class.java)

                    val adjList = repository.getAllStockAdjustmentsList()
                    FirebaseSyncService.uploadTable(url, prefix, "stock_adjustments", adjList, StockAdjustment::class.java)

                    val usersList = repository.getAllUsers()
                    FirebaseSyncService.uploadTable(url, prefix, "users", usersList, User::class.java)
                }
            }

            val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).format(Date())
            settingsDataStore.updateLastSyncTime(timeStr)
            Log.i("SyncWorker", "Background Sync successfully completed at $timeStr")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync exception: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
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
