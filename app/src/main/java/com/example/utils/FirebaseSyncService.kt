package com.example.utils

import com.example.data.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Robust cloud synchronization service for Shree Shyam Store.
 * Supports Firestore WriteBatch atomic synchronization and offline-safe REST fallback.
 */
class FirebaseSyncService(
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore? = null
) {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Pushes all local unsynced entities (Products, Bills, Customers, Categories, Udhaar)
     * to Cloud Firestore under shops/{shopUid}/... using WriteBatch.
     * Marks entities as isSynced = true upon successful commit.
     */
    suspend fun pushUpstream(shopUid: String): Boolean = withContext(Dispatchers.IO) {
        if (shopUid.isBlank()) return@withContext false

        try {
            val productDao = database.productDao()
            val saleDao = database.saleDao()
            val customerDao = database.customerDao()
            val categoryDao = database.categoryDao()
            val udhaarDao = database.udhaarDao()

            val unsyncedProducts = productDao.getUnsyncedProducts()
            val unsyncedSales = saleDao.getUnsyncedSales()
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            val unsyncedCategories = categoryDao.getUnsyncedCategories()
            val unsyncedUdhaar = udhaarDao.getUnsyncedTransactions()

            val totalCount = unsyncedProducts.size + unsyncedSales.size + unsyncedCustomers.size +
                    unsyncedCategories.size + unsyncedUdhaar.size

            if (totalCount == 0) {
                return@withContext true
            }

            val batch = firestore.batch()
            val shopDoc = firestore.collection("shops").document(shopUid)

            // 1. Upload Products -> shops/{shopUid}/products/{id}
            unsyncedProducts.forEach { product ->
                val docRef = shopDoc.collection("products").document(product.id.toString())
                val dataMap = hashMapOf<String, Any?>(
                    "id" to product.id,
                    "name" to product.name,
                    "categoryId" to product.categoryId,
                    "mrp" to product.mrp,
                    "sellingPrice" to product.sellingPrice,
                    "purchasePrice" to product.purchasePrice,
                    "currentStock" to product.currentStock,
                    "unit" to product.unit,
                    "trackStock" to product.trackStock,
                    "lowStockAlertQty" to product.lowStockAlertQty,
                    "barcode" to product.barcode,
                    "isActive" to product.isActive,
                    "createdAt" to product.createdAt,
                    "updatedAt" to product.updatedAt,
                    "isDeleted" to product.isDeleted
                )
                batch.set(docRef, dataMap, SetOptions.merge())
            }

            // 2. Upload Bills / Sales -> shops/{shopUid}/bills/{id}
            unsyncedSales.forEach { sale ->
                val docRef = shopDoc.collection("bills").document(sale.id.toString())
                val dataMap = hashMapOf<String, Any?>(
                    "id" to sale.id,
                    "billNumber" to sale.billNumber,
                    "totalAmount" to sale.totalAmount,
                    "paymentMode" to sale.paymentMode,
                    "customerId" to sale.customerId,
                    "note" to sale.note,
                    "createdAt" to sale.createdAt,
                    "updatedAt" to sale.updatedAt,
                    "isDeleted" to sale.isDeleted
                )
                batch.set(docRef, dataMap, SetOptions.merge())
            }

            // 3. Upload Customers -> shops/{shopUid}/customers/{id}
            unsyncedCustomers.forEach { customer ->
                val docRef = shopDoc.collection("customers").document(customer.id.toString())
                val dataMap = hashMapOf<String, Any?>(
                    "id" to customer.id,
                    "name" to customer.name,
                    "phone" to customer.phone,
                    "createdAt" to customer.createdAt,
                    "updatedAt" to customer.updatedAt,
                    "isDeleted" to customer.isDeleted
                )
                batch.set(docRef, dataMap, SetOptions.merge())
            }

            // 4. Upload Categories -> shops/{shopUid}/categories/{id}
            unsyncedCategories.forEach { category ->
                val docRef = shopDoc.collection("categories").document(category.id.toString())
                val dataMap = hashMapOf<String, Any?>(
                    "id" to category.id,
                    "name" to category.name,
                    "createdAt" to category.createdAt,
                    "updatedAt" to category.updatedAt,
                    "isDeleted" to category.isDeleted
                )
                batch.set(docRef, dataMap, SetOptions.merge())
            }

            // 5. Upload Udhaar Transactions -> shops/{shopUid}/udhaar_transactions/{id}
            unsyncedUdhaar.forEach { tx ->
                val docRef = shopDoc.collection("udhaar_transactions").document(tx.id.toString())
                val dataMap = hashMapOf<String, Any?>(
                    "id" to tx.id,
                    "customerId" to tx.customerId,
                    "saleId" to tx.saleId,
                    "type" to tx.type,
                    "amount" to tx.amount,
                    "note" to tx.note,
                    "createdAt" to tx.createdAt,
                    "updatedAt" to tx.updatedAt,
                    "isDeleted" to tx.isDeleted
                )
                batch.set(docRef, dataMap, SetOptions.merge())
            }

            // Execute Firestore batch commit
            Tasks.await(batch.commit(), 15, TimeUnit.SECONDS)

            // Mark local records synced upon success
            if (unsyncedProducts.isNotEmpty()) productDao.markProductsSynced(unsyncedProducts.map { it.id })
            if (unsyncedSales.isNotEmpty()) saleDao.markSalesSynced(unsyncedSales.map { it.id })
            if (unsyncedCustomers.isNotEmpty()) customerDao.markCustomersSynced(unsyncedCustomers.map { it.id })
            if (unsyncedCategories.isNotEmpty()) categoryDao.markCategoriesSynced(unsyncedCategories.map { it.id })
            if (unsyncedUdhaar.isNotEmpty()) udhaarDao.markTransactionsSynced(unsyncedUdhaar.map { it.id })

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pulls downstream modifications from Firestore where updatedAt > lastSyncTime,
     * upserts documents into Room database, and returns the new lastSyncTime timestamp.
     */
    suspend fun pullDownstream(shopUid: String, lastSyncTime: Long): Long = withContext(Dispatchers.IO) {
        if (shopUid.isBlank()) return@withContext lastSyncTime
        val newSyncTime = System.currentTimeMillis()

        try {
            val shopDoc = firestore.collection("shops").document(shopUid)

            // Pull Categories
            val catTask = shopDoc.collection("categories")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
            val catSnap = Tasks.await(catTask, 10, TimeUnit.SECONDS)
            val pulledCategories = catSnap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                Category(
                    id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                    name = name,
                    isSynced = true,
                    createdAt = doc.getLong("createdAt") ?: newSyncTime,
                    updatedAt = doc.getLong("updatedAt") ?: newSyncTime,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
            }
            if (pulledCategories.isNotEmpty()) {
                database.categoryDao().insertAll(pulledCategories)
            }

            // Pull Products
            val prodTask = shopDoc.collection("products")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
            val prodSnap = Tasks.await(prodTask, 10, TimeUnit.SECONDS)
            val pulledProducts = prodSnap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                Product(
                    id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                    name = name,
                    categoryId = doc.getLong("categoryId") ?: 1L,
                    mrp = doc.getDouble("mrp") ?: 0.0,
                    sellingPrice = doc.getDouble("sellingPrice"),
                    purchasePrice = doc.getDouble("purchasePrice"),
                    currentStock = doc.getDouble("currentStock") ?: 0.0,
                    unit = doc.getString("unit") ?: "pcs",
                    trackStock = doc.getBoolean("trackStock") ?: true,
                    lowStockAlertQty = doc.getDouble("lowStockAlertQty") ?: 5.0,
                    barcode = doc.getString("barcode") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    isSynced = true,
                    createdAt = doc.getLong("createdAt") ?: newSyncTime,
                    updatedAt = doc.getLong("updatedAt") ?: newSyncTime,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
            }
            if (pulledProducts.isNotEmpty()) {
                database.productDao().insertAll(pulledProducts)
            }

            // Pull Customers
            val custTask = shopDoc.collection("customers")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
            val custSnap = Tasks.await(custTask, 10, TimeUnit.SECONDS)
            val pulledCustomers = custSnap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                Customer(
                    id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                    name = name,
                    phone = doc.getString("phone"),
                    isSynced = true,
                    createdAt = doc.getLong("createdAt") ?: newSyncTime,
                    updatedAt = doc.getLong("updatedAt") ?: newSyncTime,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
            }
            if (pulledCustomers.isNotEmpty()) {
                database.customerDao().insertAll(pulledCustomers)
            }

            // Pull Bills / Sales
            val billTask = shopDoc.collection("bills")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
            val billSnap = Tasks.await(billTask, 10, TimeUnit.SECONDS)
            val pulledSales = billSnap.documents.mapNotNull { doc ->
                val billNum = doc.getString("billNumber") ?: return@mapNotNull null
                Sale(
                    id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                    billNumber = billNum,
                    totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                    paymentMode = doc.getString("paymentMode") ?: "CASH",
                    customerId = doc.getLong("customerId"),
                    note = doc.getString("note"),
                    isSynced = true,
                    createdAt = doc.getLong("createdAt") ?: newSyncTime,
                    updatedAt = doc.getLong("updatedAt") ?: newSyncTime,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
            }
            if (pulledSales.isNotEmpty()) {
                database.saleDao().insertAllSales(pulledSales)
            }

            // Pull Udhaar Transactions
            val udhaarTask = shopDoc.collection("udhaar_transactions")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
            val udhaarSnap = Tasks.await(udhaarTask, 10, TimeUnit.SECONDS)
            val pulledUdhaar = udhaarSnap.documents.mapNotNull { doc ->
                val customerId = doc.getLong("customerId") ?: return@mapNotNull null
                UdhaarTransaction(
                    id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                    customerId = customerId,
                    saleId = doc.getLong("saleId"),
                    type = doc.getString("type") ?: "CREDIT",
                    amount = doc.getDouble("amount") ?: 0.0,
                    note = doc.getString("note"),
                    isSynced = true,
                    createdAt = doc.getLong("createdAt") ?: newSyncTime,
                    updatedAt = doc.getLong("updatedAt") ?: newSyncTime,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
            }
            if (pulledUdhaar.isNotEmpty()) {
                database.udhaarDao().insertAll(pulledUdhaar)
            }

            newSyncTime
        } catch (e: Exception) {
            lastSyncTime
        }
    }

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        private fun getJsonUrl(url: String, prefix: String, table: String): String {
            var cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            cleanUrl = cleanUrl.removeSuffix("/")
            val cleanPrefix = prefix.trim().ifEmpty { "shreeshyam_sync" }
            return "$cleanUrl/$cleanPrefix/$table.json"
        }

        suspend fun testFirebaseConnection(url: String): Boolean = withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext false
            var cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            cleanUrl = cleanUrl.removeSuffix("/")
            val checkUrl = "$cleanUrl/.json?shallow=true"
            val request = Request.Builder().url(checkUrl).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }

        suspend fun <T> uploadTable(url: String, prefix: String, tableName: String, list: List<T>, clazz: Class<T>): Boolean = withContext(Dispatchers.IO) {
            val targetUrl = getJsonUrl(url, prefix, tableName)
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            val json = adapter.serializeNulls().toJson(list)

            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(targetUrl).put(body).build()
            try {
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }

        suspend fun <T> downloadTable(url: String, prefix: String, tableName: String, clazz: Class<T>): List<T> = withContext(Dispatchers.IO) {
            val targetUrl = getJsonUrl(url, prefix, tableName)
            val request = Request.Builder().url(targetUrl).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isBlank() || bodyStr == "null") {
                            emptyList()
                        } else {
                            val type = Types.newParameterizedType(List::class.java, clazz)
                            val adapter = moshi.adapter<List<T>>(type)
                            adapter.fromJson(bodyStr) ?: emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
