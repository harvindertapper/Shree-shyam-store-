package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.SettingsDataStore
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutbox
import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutboxState
import com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Bidirectional cloud synchronization for the local Room database.
 *
 * Firestore is used by the background worker. Manual backup and restore are
 * handled by the authenticated provider boundary, while this service remains
 * responsible for background outbox synchronization only.
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
            materializeOutboxEntries()
            val shop = firestore.collection("shops").document(uid)
            val now = System.currentTimeMillis()
            val candidates = database.syncOutboxDao().getEligible(now, MAX_OUTBOX_BATCH)
            candidates.forEach { candidate ->
                val leaseNow = System.currentTimeMillis()
                if (database.syncOutboxDao().claim(candidate.id, leaseNow, leaseNow + OUTBOX_LEASE_MS) != 1) {
                    return@forEach
                }
                val claimed = database.syncOutboxDao().getById(candidate.id) ?: return@forEach
                processOutboxEntry(shop, claimed)
            }
            database.syncOutboxDao().countByState(SyncOutboxState.RETRYABLE) == 0
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun materializeOutboxEntries() {
        val entries = buildList {
            database.productDao().getUnsyncedProducts().forEach { add(SyncOutboxDraft("products", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.saleDao().getUnsyncedSales().forEach { add(SyncOutboxDraft("sales", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.saleDao().getUnsyncedSaleItems().forEach { add(SyncOutboxDraft("sale_items", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.customerDao().getUnsyncedCustomers().forEach { add(SyncOutboxDraft("customers", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.categoryDao().getUnsyncedCategories().forEach { add(SyncOutboxDraft("categories", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.udhaarDao().getUnsyncedTransactions().forEach { add(SyncOutboxDraft("udhaar_transactions", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
            database.stockAdjustmentDao().getUnsyncedAdjustments().forEach { add(SyncOutboxDraft("stock_adjustments", it.id, it.globalId, it.mutationVersion, it.mutationDeviceId, it.isDeleted, it.toCloudMap())) }
        }
        val now = System.currentTimeMillis()
        entries.forEach { draft ->
            val key = SyncIdentity.idempotencyKey(draft.tableName, draft.globalId, draft.mutationVersion)
            val payloadJson = cloudMapAdapter.toJson(draft.payload)
            database.syncOutboxDao().insert(
                SyncOutbox(
                    tableName = draft.tableName,
                    globalId = draft.globalId,
                    localId = draft.localId,
                    mutationVersion = draft.mutationVersion,
                    mutationDeviceId = draft.mutationDeviceId,
                    idempotencyKey = key,
                    payloadJson = payloadJson,
                    tombstone = draft.tombstone,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun processOutboxEntry(
        shop: DocumentReference,
        entry: SyncOutbox
    ) {
        try {
            val collection = cloudCollection(entry.tableName)
            val reference = shop.collection(collection).document(entry.globalId)
            val remote = Tasks.await(reference.get(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (remote.exists()) {
                val remoteKey = remote.getString("idempotencyKey")
                val remoteVersion = remote.long("mutationVersion") ?: remote.long("updatedAt") ?: 0L
                val remoteDevice = remote.syncMutationDeviceId()
                if (remoteKey == entry.idempotencyKey) {
                    acknowledge(entry)
                    return
                }
                val comparison = SyncIdentity.compareMutation(
                    entry.mutationVersion,
                    entry.mutationDeviceId,
                    remoteVersion,
                    remoteDevice
                )
                if (comparison < 0 || (comparison == 0 && remoteKey != null)) {
                    database.syncOutboxDao().markDeadLetter(
                        entry.id,
                        entry.attemptCount + 1,
                        "Remote conflict won for ${entry.tableName}/${entry.globalId}",
                        System.currentTimeMillis()
                    )
                    return
                }
            }
            val payload = cloudMapAdapter.fromJson(entry.payloadJson).orEmpty()
            Tasks.await(reference.set(payload, SetOptions.merge()), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            acknowledge(entry)
        } catch (error: Exception) {
            val now = System.currentTimeMillis()
            val nextAttempt = entry.attemptCount + 1
            if (nextAttempt >= SyncIdentity.MAX_OUTBOX_ATTEMPTS) {
                database.syncOutboxDao().markDeadLetter(entry.id, nextAttempt, error.message ?: "sync failure", now)
            } else {
                database.syncOutboxDao().markRetryable(
                    entry.id,
                    nextAttempt,
                    SyncIdentity.nextRetryAt(now, nextAttempt),
                    error.message ?: "sync failure",
                    now
                )
            }
        }
    }

    private suspend fun acknowledge(entry: SyncOutbox) {
        val now = System.currentTimeMillis()
        database.syncOutboxDao().markAcked(entry.id, now)
        when (entry.tableName) {
            "categories" -> database.categoryDao().markCategorySyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "products" -> database.productDao().markProductSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "sales" -> database.saleDao().markSaleSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "sale_items" -> database.saleDao().markSaleItemSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "customers" -> database.customerDao().markCustomerSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "udhaar_transactions" -> database.udhaarDao().markTransactionSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
            "stock_adjustments" -> database.stockAdjustmentDao().markAdjustmentSyncedIfVersion(entry.localId, entry.mutationVersion, entry.mutationDeviceId)
        }
    }

    private fun cloudCollection(tableName: String): String = when (tableName) {
        "sales" -> "bills"
        else -> tableName
    }

    private data class SyncOutboxDraft(
        val tableName: String,
        val localId: Long,
        val globalId: String,
        val mutationVersion: Long,
        val mutationDeviceId: String,
        val tombstone: Boolean,
        val payload: Map<String, Any?>
    )

    /**
     * Downloads and validates every changed collection before applying anything
     * to Room. An inclusive timestamp boundary deliberately replays equal-time
     * records; global IDs and mutation comparison make that replay idempotent.
     */
    suspend fun pullDownstream(shopUid: String, lastSyncTime: Long): SyncPullResult = withContext(Dispatchers.IO) {
        val uid = shopUid.trim()
        if (uid.isEmpty()) {
            return@withContext SyncPullResult(SyncPullStatus.FAILED, lastSyncTime)
        }
        val coordinator = SyncPullCoordinator(
            fetchBatch = { previousCursor -> downloadBatch(uid, previousCursor) },
            applyBatch = { batch -> batch.applyAtomically(database) }
        )
        return@withContext coordinator.run(lastSyncTime)
    }

    private suspend fun downloadBatch(shopUid: String, previousCursor: Long): SyncPullBatch {
        val shop = firestore.collection("shops").document(shopUid)
        val fallbackTime = System.currentTimeMillis()
        val categoryBatch = pullCategories(shop, previousCursor, fallbackTime)
        val productBatch = pullProducts(shop, previousCursor, fallbackTime)
        val customerBatch = pullCustomers(shop, previousCursor, fallbackTime)
        val salesBatch = pullSales(shop, previousCursor, fallbackTime)
        val saleItemBatch = pullSaleItems(shop, previousCursor, fallbackTime)
        val udhaarBatch = pullUdhaar(shop, previousCursor, fallbackTime)
        val adjustmentBatch = pullAdjustments(shop, previousCursor, fallbackTime)
        return SyncPullBatch(
            categories = categoryBatch.records,
            products = productBatch.records,
            customers = customerBatch.records,
            sales = salesBatch.records,
            saleItems = saleItemBatch.records,
            udhaarTransactions = udhaarBatch.records,
            stockAdjustments = adjustmentBatch.records,
            highWaterMark = listOf(
                categoryBatch.highWaterMark,
                productBatch.highWaterMark,
                customerBatch.highWaterMark,
                salesBatch.highWaterMark,
                saleItemBatch.highWaterMark,
                udhaarBatch.highWaterMark,
                adjustmentBatch.highWaterMark
            ).maxOrNull() ?: previousCursor,
            receivedCount = listOf(
                categoryBatch.receivedCount,
                productBatch.receivedCount,
                customerBatch.receivedCount,
                salesBatch.receivedCount,
                saleItemBatch.receivedCount,
                udhaarBatch.receivedCount,
                adjustmentBatch.receivedCount
            ).sum()
        )
    }

    private suspend fun localIdForDocument(
        tableName: String,
        document: DocumentSnapshot
    ): Long {
        val remoteId = document.long("id") ?: document.id.toLongOrNull()
        val globalId = document.syncGlobalId(tableName, remoteId ?: 0L)
        return when (tableName) {
            "categories" -> database.categoryDao().getSyncStamp(globalId)?.id
            "products" -> database.productDao().getSyncStamp(globalId)?.id
            "sales" -> database.saleDao().getSaleSyncStamp(globalId)?.id
            "sale_items" -> database.saleDao().getSaleItemSyncStamp(globalId)?.id
            "customers" -> database.customerDao().getSyncStamp(globalId)?.id
            "udhaar_transactions" -> database.udhaarDao().getSyncStamp(globalId)?.id
            "stock_adjustments" -> database.stockAdjustmentDao().getSyncStamp(globalId)?.id
            else -> null
        } ?: remoteId ?: 0L
    }

    private suspend fun eligibleDocuments(
        documents: List<DocumentSnapshot>,
        tableName: String,
        fallbackTime: Long
    ): List<DocumentSnapshot> = documents.onEach { document ->
        validateRemoteDocument(document, tableName, fallbackTime)
    }.filter { document ->
        val localId = document.long("id") ?: document.id.toLongOrNull() ?: 0L
        val globalId = document.syncGlobalId(tableName, localId)
        val remoteVersion = document.syncMutationVersion(fallbackTime)
        val remoteDevice = document.syncMutationDeviceId()
        val local = when (tableName) {
            "categories" -> database.categoryDao().getSyncStamp(globalId)
            "products" -> database.productDao().getSyncStamp(globalId)
            "sales" -> database.saleDao().getSaleSyncStamp(globalId)
            "sale_items" -> database.saleDao().getSaleItemSyncStamp(globalId)
            "customers" -> database.customerDao().getSyncStamp(globalId)
            "udhaar_transactions" -> database.udhaarDao().getSyncStamp(globalId)
            "stock_adjustments" -> database.stockAdjustmentDao().getSyncStamp(globalId)
            else -> null
        }
        local == null || SyncIdentity.compareMutation(
            remoteVersion,
            remoteDevice,
            local.mutationVersion,
            local.mutationDeviceId
        ) >= 0
    }

    private fun validateRemoteDocument(
        document: DocumentSnapshot,
        tableName: String,
        fallbackTime: Long
    ) {
        val remoteId = document.long("id") ?: document.id.toLongOrNull()
        require(remoteId == null || remoteId > 0L) { "$tableName contains an invalid record ID" }
        val explicitGlobalId = document.getString("globalId")?.trim()
        require(!explicitGlobalId.isNullOrEmpty() || remoteId != null) {
            "$tableName contains no stable record identity"
        }
        val globalId = document.syncGlobalId(tableName, remoteId ?: 0L)
        require(globalId.isNotBlank()) { "$tableName contains a blank global ID" }
        val updatedAt = document.long("updatedAt")
        require(updatedAt != null && updatedAt > 0L) { "$tableName contains an invalid update timestamp" }
        require(document.syncMutationVersion(fallbackTime) > 0L) {
            "$tableName contains invalid mutation metadata"
        }
    }

    private suspend fun <T> pullCollection(
        shop: DocumentReference,
        collectionName: String,
        tableName: String,
        since: Long,
        fallbackTime: Long,
        mapper: suspend (DocumentSnapshot) -> T
    ): SyncPullCollection<T> {
        val snapshot = Tasks.await(
            shop.collection(collectionName).whereGreaterThanOrEqualTo("updatedAt", since).get(),
            FIRESTORE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        val documents = snapshot.documents
        val eligible = eligibleDocuments(documents, tableName, fallbackTime)
        val records = mutableListOf<T>()
        for (document in eligible) {
            records += mapper(document)
        }
        return SyncPullCollection(
            records = records,
            receivedCount = documents.size,
            highWaterMark = documents.maxOfOrNull { document ->
                requireNotNull(document.long("updatedAt")) { "$tableName contains an invalid update timestamp" }
            } ?: since
        )
    }

    private suspend fun pullCategories(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<Category> = pullCollection(shop, "categories", "categories", since, fallbackTime) { doc ->
        val id = localIdForDocument("categories", doc)
        Category(
            id = id,
            globalId = doc.syncGlobalId("categories", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            name = requireNotNull(doc.getString("name")) { "Malformed downstream document" },
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullProducts(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<Product> = pullCollection(shop, "products", "products", since, fallbackTime) { doc ->
        val id = localIdForDocument("products", doc)
        Product(
            id = id,
            globalId = doc.syncGlobalId("products", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            name = requireNotNull(doc.getString("name")) { "Malformed downstream document" },
            categoryId = doc.long("categoryId") ?: 0L,
            mrp = doc.moneyMinor("mrp"),
            sellingPrice = doc.optionalMoneyMinor("sellingPrice"),
            purchasePrice = doc.optionalMoneyMinor("purchasePrice"),
            currentStock = doc.number("currentStock"),
            unit = doc.getString("unit") ?: "pcs",
            trackStock = doc.getBoolean("trackStock") ?: true,
            lowStockAlertQty = doc.number("lowStockAlertQty", 5.0),
            barcode = doc.getString("barcode").orEmpty(),
            barcodeKey = doc.getString("barcodeKey")?.takeIf { it.isNotBlank() }
                ?: InventoryValidation.normalizeBarcode(doc.getString("barcode").orEmpty()),
            isActive = doc.getBoolean("isActive") ?: true,
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullCustomers(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<Customer> = pullCollection(shop, "customers", "customers", since, fallbackTime) { doc ->
        val id = localIdForDocument("customers", doc)
        Customer(
            id = id,
            globalId = doc.syncGlobalId("customers", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            name = requireNotNull(doc.getString("name")) { "Malformed downstream document" },
            phone = doc.getString("phone"),
            creditLimit = doc.moneyMinor("creditLimit", 500_000L),
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullSales(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<Sale> = pullCollection(shop, "bills", "sales", since, fallbackTime) { doc ->
        val id = localIdForDocument("sales", doc)
        val paymentMode = doc.getString("paymentMode") ?: "CASH"
        val paymentState = doc.getString("paymentState")
            ?.let { runCatching { PaymentState.fromWireValue(it).wireValue }.getOrNull() }
            ?: legacyPaymentState(paymentMode)
        Sale(
            id = id,
            globalId = doc.syncGlobalId("sales", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            billNumber = requireNotNull(doc.getString("billNumber")) { "Malformed downstream document" },
            totalAmount = doc.moneyMinor("totalAmount"),
            paymentMode = paymentMode,
            paymentState = paymentState,
            receivedAmount = doc.long("receivedAmount")
                ?: if (paymentState == PaymentState.RECEIVED.wireValue) doc.moneyMinor("totalAmount") else null,
            customerId = doc.long("customerId"),
            note = doc.getString("note"),
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullSaleItems(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<SaleItem> = pullCollection(shop, "sale_items", "sale_items", since, fallbackTime) { doc ->
        val id = localIdForDocument("sale_items", doc)
        SaleItem(
            id = id,
            globalId = doc.syncGlobalId("sale_items", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            saleId = requireNotNull(doc.long("saleId")) { "Malformed downstream document" },
            productId = requireNotNull(doc.long("productId")) { "Malformed downstream document" },
            productNameSnapshot = requireNotNull(doc.getString("productNameSnapshot")) { "Malformed downstream document" },
            quantity = doc.number("quantity", 1.0),
            unit = doc.getString("unit") ?: "pcs",
            unitPrice = doc.moneyMinor("unitPrice"),
            lineTotal = doc.moneyMinor("lineTotal"),
            isSynced = true,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullUdhaar(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<UdhaarTransaction> = pullCollection(
        shop,
        "udhaar_transactions",
        "udhaar_transactions",
        since,
        fallbackTime
    ) { doc ->
        val id = localIdForDocument("udhaar_transactions", doc)
        val type = doc.getString("type") ?: "CREDIT"
        val amount = doc.moneyMinor("amount")
        val eventId = doc.getString("eventId")?.trim()?.ifEmpty { "legacy-$id" } ?: "legacy-$id"
        val balanceEffect = doc.long("balanceEffect") ?: when (type) {
            "CREDIT" -> amount
            "PAYMENT" -> -amount
            else -> 0L
        }
        UdhaarTransaction(
            id = id,
            eventId = eventId,
            customerId = requireNotNull(doc.long("customerId")) { "Malformed downstream document" },
            saleId = doc.long("saleId"),
            type = type,
            amount = amount,
            balanceEffect = balanceEffect,
            note = doc.getString("note"),
            correctsEventId = doc.getString("correctsEventId"),
            correctionReason = doc.getString("correctionReason"),
            actorUid = doc.getString("actorUid")?.trim()?.ifEmpty { "legacy-cloud" } ?: "legacy-cloud",
            actorName = doc.getString("actorName")?.trim()?.ifEmpty { "Legacy cloud record" } ?: "Legacy cloud record",
            actorRole = doc.getString("actorRole")?.trim()?.ifEmpty { "OWNER" } ?: "OWNER",
            actorDeviceId = doc.getString("actorDeviceId")?.trim()?.ifEmpty { "legacy-cloud" } ?: "legacy-cloud",
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private suspend fun pullAdjustments(
        shop: DocumentReference,
        since: Long,
        fallbackTime: Long
    ): SyncPullCollection<StockAdjustment> = pullCollection(
        shop,
        "stock_adjustments",
        "stock_adjustments",
        since,
        fallbackTime
    ) { doc ->
        val id = localIdForDocument("stock_adjustments", doc)
        StockAdjustment(
            id = id,
            globalId = doc.syncGlobalId("stock_adjustments", doc.long("id") ?: doc.id.toLongOrNull() ?: id),
            productId = requireNotNull(doc.long("productId")) { "Malformed downstream document" },
            oldStock = doc.number("oldStock"),
            newStock = doc.number("newStock"),
            difference = doc.number("difference"),
            reason = doc.getString("reason") ?: "Cloud adjustment",
            isSynced = true,
            createdAt = doc.long("createdAt") ?: doc.long("updatedAt")!!,
            updatedAt = doc.long("updatedAt")!!,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            mutationVersion = doc.syncMutationVersion(fallbackTime),
            mutationDeviceId = doc.syncMutationDeviceId()
        )
    }

    private fun Product.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "name" to name, "categoryId" to categoryId, "mrp" to mrp,
        "sellingPrice" to sellingPrice, "purchasePrice" to purchasePrice,
        "moneyScale" to 2L,
        "currentStock" to currentStock, "unit" to unit, "trackStock" to trackStock,
        "lowStockAlertQty" to lowStockAlertQty, "barcode" to barcode, "barcodeKey" to barcodeKey,
        "isActive" to isActive,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("products", globalId, mutationVersion)
    )

    private fun Sale.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "billNumber" to billNumber, "totalAmount" to totalAmount,
        "moneyScale" to 2L,
        "paymentMode" to paymentMode, "paymentState" to paymentState,
        "receivedAmount" to receivedAmount, "customerId" to customerId, "note" to note,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("sales", globalId, mutationVersion)
    )

    private fun SaleItem.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "saleId" to saleId, "productId" to productId,
        "productNameSnapshot" to productNameSnapshot, "quantity" to quantity,
        "unit" to unit, "unitPrice" to unitPrice, "lineTotal" to lineTotal,
        "moneyScale" to 2L,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("sale_items", globalId, mutationVersion)
    )

    private fun Customer.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "name" to name, "phone" to phone, "creditLimit" to creditLimit,
        "moneyScale" to 2L,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("customers", globalId, mutationVersion)
    )

    private fun Category.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "name" to name, "createdAt" to createdAt,
        "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("categories", globalId, mutationVersion)
    )

    private fun UdhaarTransaction.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "eventId" to eventId, "customerId" to customerId, "saleId" to saleId, "type" to type,
        "amount" to amount, "balanceEffect" to balanceEffect, "moneyScale" to 2L,
        "note" to note, "correctsEventId" to correctsEventId, "correctionReason" to correctionReason,
        "actorUid" to actorUid, "actorName" to actorName, "actorRole" to actorRole,
        "actorDeviceId" to actorDeviceId, "createdAt" to createdAt, "updatedAt" to updatedAt,
        "isDeleted" to isDeleted, "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("udhaar_transactions", globalId, mutationVersion)
    )

    private fun StockAdjustment.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id, "globalId" to globalId, "productId" to productId, "oldStock" to oldStock,
        "newStock" to newStock, "difference" to difference, "reason" to reason,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isDeleted" to isDeleted,
        "mutationVersion" to mutationVersion, "mutationDeviceId" to mutationDeviceId,
        "idempotencyKey" to SyncIdentity.idempotencyKey("stock_adjustments", globalId, mutationVersion)
    )

    companion object {
        private const val MAX_BATCH_WRITES = 450
        private const val MAX_OUTBOX_BATCH = 50
        private const val OUTBOX_LEASE_MS = 2 * 60_000L
        private const val FIRESTORE_TIMEOUT_SECONDS = 15L
        private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        private val cloudMapAdapter = moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.long(field: String): Long? =
    getLong(field) ?: getDouble(field)?.toLong()

private fun com.google.firebase.firestore.DocumentSnapshot.number(field: String, default: Double = 0.0): Double =
    getDouble(field) ?: getLong(field)?.toDouble() ?: default

private fun com.google.firebase.firestore.DocumentSnapshot.optionalNumber(field: String): Double? =
    getDouble(field) ?: getLong(field)?.toDouble()

private fun legacyPaymentState(paymentMode: String): String =
    if (paymentMode.trim().equals("UDHAAR", ignoreCase = true)) {
        PaymentState.NOT_REQUIRED.wireValue
    } else {
        PaymentState.RECEIVED.wireValue
    }

private fun com.google.firebase.firestore.DocumentSnapshot.moneyMinor(
    field: String,
    default: Long = 0L
): Long {
    val scale = long("moneyScale")
    return if (scale == 2L) {
        long(field) ?: default
    } else {
        getDouble(field)?.let { MoneyUtils.fromLegacyMajor(it) }
            ?: getLong(field)?.let { MoneyUtils.fromLegacyMajor(it.toDouble()) }
            ?: default
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.optionalMoneyMinor(field: String): Long? {
    val scale = long("moneyScale")
    return if (scale == 2L) {
        long(field)
    } else {
        getDouble(field)?.let { MoneyUtils.fromLegacyMajor(it) }
            ?: getLong(field)?.let { MoneyUtils.fromLegacyMajor(it.toDouble()) }
    }
}
