package com.example.utils

import com.example.data.AppDatabase
import com.example.data.Category
import com.example.data.Customer
import com.example.data.Product
import com.example.data.Sale
import com.example.data.SaleItem
import com.example.data.SettingsDataStore
import com.example.data.StockAdjustment
import com.example.data.UdhaarTransaction
import com.example.data.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Bidirectional cloud synchronization for the local Room database.
 *
 * Firestore is used by the background worker. The REST helpers are retained for
 * the manual JSON backup/restore screen and intentionally return safe values on
 * network or decoding failures so offline use never crashes the app.
 */
class FirebaseSyncService(
    private val database: AppDatabase,
    @Suppress("UNUSED_PARAMETER") private val settingsDataStore: SettingsDataStore? = null
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun pushUpstream(shopUid: String): Boolean = withContext(Dispatchers.IO) {
        val uid = shopUid.trim()
        if (uid.isEmpty()) return@withContext false

        try {
            val products = database.productDao().getUnsyncedProducts()
            val sales = database.saleDao().getUnsyncedSales()
            val saleItems = database.saleDao().getUnsyncedSaleItems()
            val customers = database.customerDao().getUnsyncedCustomers()
            val categories = database.categoryDao().getUnsyncedCategories()
            val udhaar = database.udhaarDao().getUnsyncedTransactions()
            val adjustments = database.stockAdjustmentDao().getUnsyncedAdjustments()
            val users = database.userDao().getUnsyncedUsers()

            val shop = firestore.collection("shops").document(uid)
            val writes = buildList {
                products.forEach { add(shop.collection("products").document(it.id.toString()) to it.toCloudMap()) }
                sales.forEach { add(shop.collection("bills").document(it.id.toString()) to it.toCloudMap()) }
                saleItems.forEach { add(shop.collection("sale_items").document(it.id.toString()) to it.toCloudMap()) }
                customers.forEach { add(shop.collection("customers").document(it.id.toString()) to it.toCloudMap()) }
                categories.forEach { add(shop.collection("categories").document(it.id.toString()) to it.toCloudMap()) }
                udhaar.forEach { add(shop.collection("udhaar_transactions").document(it.id.toString()) to it.toCloudMap()) }
                adjustments.forEach { add(shop.collection("stock_adjustments").document(it.id.toString()) to it.toCloudMap()) }
                users.forEach { add(shop.collection("users").document(it.id.toString()) to it.toCloudMap()) }
            }

            // Firestore limits a write batch to 500 operations. Keep headroom
            // for server-side metadata and make large offline queues safe.
            writes.chunked(MAX_BATCH_WRITES).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { (reference, data) ->
                    batch.set(reference, data, SetOptions.merge())
                }
                Tasks.await(batch.commit(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }

            if (products.isNotEmpty()) database.productDao().markProductsSynced(products.map { it.id })
            if (sales.isNotEmpty()) database.saleDao().markSalesSynced(sales.map { it.id })
            if (saleItems.isNotEmpty()) database.saleDao().markSaleItemsSynced(saleItems.map { it.id })
            if (customers.isNotEmpty()) database.customerDao().markCustomersSynced(customers.map { it.id })
            if (categories.isNotEmpty()) database.categoryDao().markCategoriesSynced(categories.map { it.id })
            if (udhaar.isNotEmpty()) database.udhaarDao().markTransactionsSynced(udhaar.map { it.id })
            if (adjustments.isNotEmpty()) database.stockAdjustmentDao().markAdjustmentsSynced(adjustments.map { it.id })
            if (users.isNotEmpty()) database.userDao().markUsersSynced(users.map { it.id })
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Pulls all changed cloud collections and returns the new high-water mark. */
    suspend fun pullDownstream(shopUid: String, lastSyncTime: Long): Long = withContext(Dispatchers.IO) {
        val uid = shopUid.trim()
        if (uid.isEmpty()) return@withContext lastSyncTime
        val now = System.currentTimeMillis()

        try {
            val shop = firestore.collection("shops").document(uid)
            pullCategories(shop, lastSyncTime, now)
            pullProducts(shop, lastSyncTime, now)
            pullCustomers(shop, lastSyncTime, now)
            pullSales(shop, lastSyncTime, now)
            pullSaleItems(shop, lastSyncTime, now)
            pullUdhaar(shop, lastSyncTime, now)
            pullAdjustments(shop, lastSyncTime, now)
            pullUsers(shop, lastSyncTime, now)
            now
        } catch (_: Exception) {
            // Keep the previous cursor so a transient failure is retried rather
            // than silently skipping records.
            lastSyncTime
        }
    }

    private suspend fun pullCategories(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("categories").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.categoryDao().insertAll(docs.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            Category(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                name = name,
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullProducts(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("products").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.productDao().insertAll(docs.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            Product(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                name = name,
                categoryId = doc.long("categoryId") ?: 0L,
                mrp = doc.number("mrp"),
                sellingPrice = doc.optionalNumber("sellingPrice"),
                purchasePrice = doc.optionalNumber("purchasePrice"),
                currentStock = doc.number("currentStock"),
                unit = doc.getString("unit") ?: "pcs",
                trackStock = doc.getBoolean("trackStock") ?: true,
                lowStockAlertQty = doc.number("lowStockAlertQty", 5.0),
                barcode = doc.getString("barcode").orEmpty(),
                isActive = doc.getBoolean("isActive") ?: true,
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullCustomers(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("customers").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.customerDao().insertAll(docs.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            Customer(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                name = name,
                phone = doc.getString("phone"),
                creditLimit = doc.number("creditLimit", 5000.0),
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullSales(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("bills").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.saleDao().insertAllSales(docs.mapNotNull { doc ->
            val number = doc.getString("billNumber") ?: return@mapNotNull null
            Sale(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                billNumber = number,
                totalAmount = doc.number("totalAmount"),
                paymentMode = doc.getString("paymentMode") ?: "CASH",
                customerId = doc.long("customerId"),
                note = doc.getString("note"),
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullSaleItems(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("sale_items").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.saleDao().insertAllSaleItems(docs.mapNotNull { doc ->
            val productName = doc.getString("productNameSnapshot") ?: return@mapNotNull null
            SaleItem(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                saleId = doc.long("saleId") ?: return@mapNotNull null,
                productId = doc.long("productId") ?: return@mapNotNull null,
                productNameSnapshot = productName,
                quantity = doc.number("quantity", 1.0),
                unit = doc.getString("unit") ?: "pcs",
                unitPrice = doc.number("unitPrice"),
                lineTotal = doc.number("lineTotal"),
                isSynced = true,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullUdhaar(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("udhaar_transactions").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.udhaarDao().insertAll(docs.mapNotNull { doc ->
            UdhaarTransaction(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                customerId = doc.long("customerId") ?: return@mapNotNull null,
                saleId = doc.long("saleId"),
                type = doc.getString("type") ?: "CREDIT",
                amount = doc.number("amount"),
                note = doc.getString("note"),
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullAdjustments(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("stock_adjustments").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.stockAdjustmentDao().insertAll(docs.mapNotNull { doc ->
            StockAdjustment(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                productId = doc.long("productId") ?: return@mapNotNull null,
                oldStock = doc.number("oldStock"),
                newStock = doc.number("newStock"),
                difference = doc.number("difference"),
                reason = doc.getString("reason") ?: "Cloud adjustment",
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private suspend fun pullUsers(shop: DocumentReference, since: Long, fallbackTime: Long) {
        val docs = Tasks.await(
            shop.collection("users").whereGreaterThan("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        ).documents
        database.userDao().insertAll(docs.mapNotNull { doc ->
            User(
                id = doc.long("id") ?: doc.id.toLongOrNull() ?: 0L,
                uid = doc.getString("uid").orEmpty(),
                username = doc.getString("username") ?: return@mapNotNull null,
                email = doc.getString("email") ?: return@mapNotNull null,
                passwordHash = doc.getString("passwordHash").orEmpty(),
                isSynced = true,
                createdAt = doc.long("createdAt") ?: fallbackTime,
                updatedAt = doc.long("updatedAt") ?: fallbackTime,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        })
    }

    private fun Product.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "name" to name, "categoryId" to categoryId, "mrp" to mrp,
        "sellingPrice" to sellingPrice, "purchasePrice" to purchasePrice,
        "currentStock" to currentStock, "unit" to unit, "trackStock" to trackStock,
        "lowStockAlertQty" to lowStockAlertQty, "barcode" to barcode, "isActive" to isActive,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun Sale.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "billNumber" to billNumber, "totalAmount" to totalAmount,
        "paymentMode" to paymentMode, "customerId" to customerId, "note" to note,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun SaleItem.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "saleId" to saleId, "productId" to productId,
        "productNameSnapshot" to productNameSnapshot, "quantity" to quantity,
        "unit" to unit, "unitPrice" to unitPrice, "lineTotal" to lineTotal,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun Customer.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "name" to name, "phone" to phone, "creditLimit" to creditLimit,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun Category.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "name" to name, "createdAt" to createdAt,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun UdhaarTransaction.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "customerId" to customerId, "saleId" to saleId, "type" to type,
        "amount" to amount, "note" to note, "createdAt" to createdAt,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun StockAdjustment.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "productId" to productId, "oldStock" to oldStock,
        "newStock" to newStock, "difference" to difference, "reason" to reason,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    private fun User.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "uid" to uid, "username" to username, "email" to email,
        "passwordHash" to passwordHash, "createdAt" to createdAt,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted
    )

    companion object {
        private const val MAX_BATCH_WRITES = 450
        private const val FIRESTORE_TIMEOUT_SECONDS = 15L
        private const val DEFAULT_PREFIX = "shreeshyam_sync"
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        private fun normalizedBaseUrl(url: String): String? {
            val value = url.trim()
            if (value.isEmpty()) return null
            val withScheme = if (value.startsWith("http://") || value.startsWith("https://")) {
                value
            } else {
                "https://$value"
            }
            return withScheme.trimEnd('/').removeSuffix(".json")
        }

        private fun getJsonUrl(url: String, prefix: String, table: String): String? {
            val base = normalizedBaseUrl(url) ?: return null
            val cleanPrefix = prefix.trim().trim('/').ifEmpty { DEFAULT_PREFIX }
            val cleanTable = table.trim().trim('/')
            if (cleanTable.isEmpty()) return null
            return "$base/$cleanPrefix/$cleanTable.json"
        }

        suspend fun testFirebaseConnection(url: String): Boolean = withContext(Dispatchers.IO) {
            val base = normalizedBaseUrl(url) ?: return@withContext false
            val request = runCatching { Request.Builder().url("$base/.json?shallow=true").get().build() }
                .getOrNull() ?: return@withContext false
            runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrDefault(false)
        }

        suspend fun <T> uploadTable(
            url: String,
            prefix: String,
            tableName: String,
            list: List<T>,
            clazz: Class<T>
        ): Boolean = withContext(Dispatchers.IO) {
            val targetUrl = getJsonUrl(url, prefix, tableName) ?: return@withContext false
            runCatching {
                val type = Types.newParameterizedType(List::class.java, clazz)
                val json = moshi.adapter<List<T>>(type).serializeNulls().toJson(list)
                val request = Request.Builder()
                    .url(targetUrl)
                    .put(json.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }

        suspend fun <T> downloadTable(
            url: String,
            prefix: String,
            tableName: String,
            clazz: Class<T>
        ): List<T> = withContext(Dispatchers.IO) {
            val targetUrl = getJsonUrl(url, prefix, tableName) ?: return@withContext emptyList()
            runCatching {
                val request = Request.Builder().url(targetUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank() || body == "null") return@use emptyList()
                    val type = Types.newParameterizedType(List::class.java, clazz)
                    moshi.adapter<List<T>>(type).fromJson(body).orEmpty()
                }
            }.getOrDefault(emptyList())
        }

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.long(field: String): Long? =
    getLong(field) ?: getDouble(field)?.toLong()

private fun com.google.firebase.firestore.DocumentSnapshot.number(field: String, default: Double = 0.0): Double =
    getDouble(field) ?: getLong(field)?.toDouble() ?: default

private fun com.google.firebase.firestore.DocumentSnapshot.optionalNumber(field: String): Double? =
    getDouble(field) ?: getLong(field)?.toDouble()
