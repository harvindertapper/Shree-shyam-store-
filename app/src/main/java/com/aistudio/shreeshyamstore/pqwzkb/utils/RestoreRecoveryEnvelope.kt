package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
import com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.IOException
import java.security.MessageDigest

sealed class RestoreSnapshotException(message: String) : IOException(message)

class SnapshotSchemaUnsupportedException(message: String) : RestoreSnapshotException(message)
class SnapshotTenantMismatchException(message: String) : RestoreSnapshotException(message)
class SnapshotIncompleteException(message: String) : RestoreSnapshotException(message)
class SnapshotIntegrityException(message: String) : RestoreSnapshotException(message)
class SnapshotReferentialIntegrityException(message: String) : RestoreSnapshotException(message)
class SnapshotEmptyException(message: String) : RestoreSnapshotException(message)
class SnapshotUnavailableException(message: String, cause: Throwable? = null) : RestoreSnapshotException(message) {
    init {
        cause?.let(::initCause)
    }
}
class SnapshotMalformedException(message: String, cause: Throwable? = null) : RestoreSnapshotException(message) {
    init {
        cause?.let(::initCause)
    }
}

data class CloudRestorableSnapshot(
    val categories: List<Category>,
    val products: List<Product>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val customers: List<Customer>,
    val udhaarTransactions: List<UdhaarTransaction>,
    val stockAdjustments: List<StockAdjustment>
) {
    fun isEmpty(): Boolean = categories.isEmpty() && products.isEmpty() && sales.isEmpty() &&
        saleItems.isEmpty() && customers.isEmpty() && udhaarTransactions.isEmpty() &&
        stockAdjustments.isEmpty()
}

data class SnapshotTableCounts(
    val categories: Int,
    val products: Int,
    val sales: Int,
    val saleItems: Int,
    val customers: Int,
    val udhaarTransactions: Int,
    val stockAdjustments: Int
) {
    fun asMap(): Map<String, Int> = linkedMapOf(
        "categories" to categories,
        "products" to products,
        "sales" to sales,
        "sale_items" to saleItems,
        "customers" to customers,
        "udhaar_transactions" to udhaarTransactions,
        "stock_adjustments" to stockAdjustments
    )

    companion object {
        fun from(snapshot: CloudRestorableSnapshot): SnapshotTableCounts = SnapshotTableCounts(
            categories = snapshot.categories.size,
            products = snapshot.products.size,
            sales = snapshot.sales.size,
            saleItems = snapshot.saleItems.size,
            customers = snapshot.customers.size,
            udhaarTransactions = snapshot.udhaarTransactions.size,
            stockAdjustments = snapshot.stockAdjustments.size
        )
    }
}

data class SnapshotEnvelope(
    val schemaVersion: Int,
    val organizationId: String,
    val storeId: String,
    val membershipId: String,
    val createdAtEpochMs: Long,
    val sourceDeviceId: String,
    val sourceAppInstallationId: String,
    val tableCounts: Map<String, Int>,
    val checksumSha256: String,
    val complete: Boolean,
    val snapshot: CloudRestorableSnapshot
) {
    companion object {
        fun create(
            snapshot: CloudRestorableSnapshot,
            tenant: TenantScope,
            createdAtEpochMs: Long = System.currentTimeMillis(),
            complete: Boolean = true
        ): SnapshotEnvelope {
            val normalized = RestoreSnapshotValidator.normalizeLegacyIdentities(snapshot)
            return SnapshotEnvelope(
                schemaVersion = RestoreSnapshotPolicy.CURRENT_SCHEMA_VERSION,
                organizationId = tenant.organizationId,
                storeId = tenant.storeId,
                membershipId = tenant.membershipId,
                createdAtEpochMs = createdAtEpochMs,
                sourceDeviceId = tenant.deviceId,
                sourceAppInstallationId = tenant.appInstallationId,
                tableCounts = SnapshotTableCounts.from(normalized).asMap(),
                checksumSha256 = RestoreSnapshotCodec.checksum(normalized),
                complete = complete,
                snapshot = normalized
            )
        }
    }
}

object RestoreSnapshotPolicy {
    const val CURRENT_SCHEMA_VERSION: Int = 1
    const val FUTURE_CLOCK_SKEW_MS: Long = 5 * 60 * 1000L
    val REQUIRED_TABLES: Set<String> = linkedSetOf(
        "categories",
        "products",
        "sales",
        "sale_items",
        "customers",
        "udhaar_transactions",
        "stock_adjustments"
    )
}

object RestoreSnapshotValidator {
    fun validate(
        envelope: SnapshotEnvelope,
        expectedTenant: TenantScope,
        nowEpochMs: Long = System.currentTimeMillis(),
        allowEmpty: Boolean = false
    ): CloudRestorableSnapshot {
        if (envelope.schemaVersion != RestoreSnapshotPolicy.CURRENT_SCHEMA_VERSION) {
            throw SnapshotSchemaUnsupportedException(
                "Unsupported restore snapshot schema ${envelope.schemaVersion}"
            )
        }
        if (envelope.organizationId != expectedTenant.organizationId ||
            envelope.storeId != expectedTenant.storeId ||
            envelope.membershipId != expectedTenant.membershipId
        ) {
            throw SnapshotTenantMismatchException("Restore snapshot belongs to another store")
        }
        if (!envelope.complete) {
            throw SnapshotIncompleteException("Restore snapshot is not marked complete")
        }
        if (envelope.createdAtEpochMs <= 0L ||
            envelope.createdAtEpochMs > nowEpochMs + RestoreSnapshotPolicy.FUTURE_CLOCK_SKEW_MS
        ) {
            throw SnapshotIntegrityException("Restore snapshot creation time is invalid")
        }
        if (envelope.sourceDeviceId.isBlank() || envelope.sourceAppInstallationId.isBlank()) {
            throw SnapshotIntegrityException("Restore snapshot source metadata is incomplete")
        }
        if (envelope.tableCounts.keys != RestoreSnapshotPolicy.REQUIRED_TABLES) {
            throw SnapshotIncompleteException("Restore snapshot table set is incomplete")
        }

        val snapshot = normalizeLegacyIdentities(envelope.snapshot)
        val actualCounts = SnapshotTableCounts.from(snapshot).asMap()
        if (actualCounts != envelope.tableCounts) {
            throw SnapshotIntegrityException("Restore snapshot table counts do not match records")
        }
        if (!allowEmpty && snapshot.isEmpty()) {
            throw SnapshotEmptyException("Restore snapshot contains no business records")
        }
        if (!envelope.checksumSha256.equals(RestoreSnapshotCodec.checksum(snapshot), ignoreCase = true)) {
            throw SnapshotIntegrityException("Restore snapshot checksum mismatch")
        }

        validateUniqueIdentity("categories", snapshot.categories.map { it.globalId })
        validateUniqueIdentity("products", snapshot.products.map { it.globalId })
        validateUniqueIdentity("sales", snapshot.sales.map { it.globalId })
        validateUniqueIdentity("sale_items", snapshot.saleItems.map { it.globalId })
        validateUniqueIdentity("customers", snapshot.customers.map { it.globalId })
        validateUniqueIdentity("udhaar_transactions", snapshot.udhaarTransactions.map { it.globalId })
        validateUniqueIdentity("stock_adjustments", snapshot.stockAdjustments.map { it.globalId })

        validateRows(snapshot)
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = snapshot.categories,
            products = snapshot.products,
            sales = snapshot.sales,
            saleItems = snapshot.saleItems,
            customers = snapshot.customers,
            udhaarTransactions = snapshot.udhaarTransactions,
            stockAdjustments = snapshot.stockAdjustments
        )
        return snapshot
    }

    internal fun normalizeLegacyIdentities(snapshot: CloudRestorableSnapshot): CloudRestorableSnapshot = snapshot.copy(
        categories = snapshot.categories.map { it.copy(globalId = stableId("categories", it.id, it.globalId)) },
        products = snapshot.products.map { it.copy(globalId = stableId("products", it.id, it.globalId)) },
        sales = snapshot.sales.map { it.copy(globalId = stableId("sales", it.id, it.globalId)) },
        saleItems = snapshot.saleItems.map { it.copy(globalId = stableId("sale_items", it.id, it.globalId)) },
        customers = snapshot.customers.map { it.copy(globalId = stableId("customers", it.id, it.globalId)) },
        udhaarTransactions = snapshot.udhaarTransactions.map {
            it.copy(globalId = stableId("udhaar_transactions", it.id, it.globalId))
        },
        stockAdjustments = snapshot.stockAdjustments.map {
            it.copy(globalId = stableId("stock_adjustments", it.id, it.globalId))
        }
    )

    private fun stableId(tableName: String, id: Long, globalId: String): String =
        globalId.trim().ifEmpty { SyncIdentity.legacyGlobalId(tableName, id) }

    private fun validateUniqueIdentity(tableName: String, ids: List<String>) {
        if (ids.any { it.isBlank() } || ids.size != ids.toSet().size) {
            throw SnapshotIntegrityException("Duplicate or blank global identity in $tableName")
        }
    }

    private fun validateRows(snapshot: CloudRestorableSnapshot) {
        val categoryIds = snapshot.categories.map { it.id }.toSet()
        val productIds = snapshot.products.map { it.id }.toSet()
        val saleIds = snapshot.sales.map { it.id }.toSet()
        val customerIds = snapshot.customers.map { it.id }.toSet()

        snapshot.categories.forEach { category ->
            if (category.id <= 0L || category.name.isBlank()) {
                throw SnapshotReferentialIntegrityException("Invalid category record ${category.globalId}")
            }
        }
        snapshot.customers.forEach { customer ->
            if (customer.id <= 0L || customer.name.isBlank() || customer.creditLimit < 0L) {
                throw SnapshotReferentialIntegrityException("Invalid customer record ${customer.globalId}")
            }
        }
        snapshot.products.forEach { product ->
            if (product.id <= 0L || product.name.isBlank() || product.categoryId <= 0L ||
                product.categoryId !in categoryIds || product.mrp < 0L ||
                product.sellingPrice != null && product.sellingPrice < 0L ||
                product.purchasePrice != null && product.purchasePrice < 0L ||
                !product.currentStock.isFinite() || product.currentStock < 0.0
            ) {
                throw SnapshotReferentialIntegrityException("Invalid product record ${product.globalId}")
            }
        }
        snapshot.sales.forEach { sale ->
            if (sale.id <= 0L || sale.billNumber.isBlank() || sale.totalAmount < 0L ||
                sale.paymentMode !in setOf("CASH", "UPI", "UDHAAR") ||
                sale.receivedAmount != null && (sale.receivedAmount < 0L || sale.receivedAmount > sale.totalAmount) ||
                sale.customerId != null && sale.customerId !in customerIds ||
                runCatching { PaymentState.fromWireValue(sale.paymentState) }.isFailure
            ) {
                throw SnapshotReferentialIntegrityException("Invalid sale record ${sale.globalId}")
            }
        }
        snapshot.saleItems.forEach { item ->
            if (item.id <= 0L || item.saleId !in saleIds || item.productId !in productIds ||
                item.productNameSnapshot.isBlank() || !item.quantity.isFinite() || item.quantity <= 0.0 ||
                item.unitPrice < 0L || item.lineTotal < 0L
            ) {
                throw SnapshotReferentialIntegrityException("Invalid sale item record ${item.globalId}")
            }
        }
        snapshot.udhaarTransactions.forEach { transaction ->
            if (transaction.id <= 0L || transaction.customerId !in customerIds ||
                transaction.saleId != null && transaction.saleId !in saleIds ||
                transaction.type !in setOf("CREDIT", "PAYMENT", "REVERSAL", "CORRECTION") ||
                transaction.amount <= 0L
            ) {
                throw SnapshotReferentialIntegrityException("Invalid ledger record ${transaction.globalId}")
            }
        }
        snapshot.stockAdjustments.forEach { adjustment ->
            if (adjustment.id <= 0L || adjustment.productId !in productIds ||
                !adjustment.oldStock.isFinite() || !adjustment.newStock.isFinite() ||
                !adjustment.difference.isFinite() || adjustment.reason.isBlank()
            ) {
                throw SnapshotReferentialIntegrityException("Invalid stock adjustment ${adjustment.globalId}")
            }
        }
    }
}

object RestoreSnapshotCodec {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val envelopeAdapter = moshi.adapter(SnapshotEnvelope::class.java)
    private val categoryAdapter = moshi.adapter<List<Category>>(
        Types.newParameterizedType(List::class.java, Category::class.java)
    )
    private val productAdapter = moshi.adapter<List<Product>>(
        Types.newParameterizedType(List::class.java, Product::class.java)
    )
    private val saleAdapter = moshi.adapter<List<Sale>>(
        Types.newParameterizedType(List::class.java, Sale::class.java)
    )
    private val saleItemAdapter = moshi.adapter<List<SaleItem>>(
        Types.newParameterizedType(List::class.java, SaleItem::class.java)
    )
    private val customerAdapter = moshi.adapter<List<Customer>>(
        Types.newParameterizedType(List::class.java, Customer::class.java)
    )
    private val udhaarAdapter = moshi.adapter<List<UdhaarTransaction>>(
        Types.newParameterizedType(List::class.java, UdhaarTransaction::class.java)
    )
    private val adjustmentAdapter = moshi.adapter<List<StockAdjustment>>(
        Types.newParameterizedType(List::class.java, StockAdjustment::class.java)
    )

    fun encode(envelope: SnapshotEnvelope): String = envelopeAdapter.serializeNulls().toJson(envelope)

    fun decode(json: String): SnapshotEnvelope = try {
        envelopeAdapter.fromJson(json) ?: throw SnapshotMalformedException("Snapshot JSON is null")
    } catch (error: SnapshotMalformedException) {
        throw error
    } catch (error: Exception) {
        throw SnapshotMalformedException("Snapshot JSON is malformed", error)
    }

    fun checksum(snapshot: CloudRestorableSnapshot): String {
        val canonical = buildString {
            append("categories=").append(categoryAdapter.toJson(snapshot.categories)).append('\n')
            append("products=").append(productAdapter.toJson(snapshot.products)).append('\n')
            append("sales=").append(saleAdapter.toJson(snapshot.sales)).append('\n')
            append("sale_items=").append(saleItemAdapter.toJson(snapshot.saleItems)).append('\n')
            append("customers=").append(customerAdapter.toJson(snapshot.customers)).append('\n')
            append("udhaar_transactions=").append(udhaarAdapter.toJson(snapshot.udhaarTransactions)).append('\n')
            append("stock_adjustments=").append(adjustmentAdapter.toJson(snapshot.stockAdjustments))
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

class RestoreRecoveryCoordinator(
    private val replaceSnapshot: suspend (CloudRestorableSnapshot) -> Unit
) {
    suspend fun replaceWithRollback(
        snapshot: CloudRestorableSnapshot,
        tenant: TenantScope,
        recoveryEnvelope: SnapshotEnvelope
    ) {
        try {
            replaceSnapshot(snapshot)
        } catch (restoreError: Exception) {
            runCatching {
                val verifiedRecovery = RestoreSnapshotValidator.validate(
                    recoveryEnvelope,
                    tenant,
                    allowEmpty = true
                )
                replaceSnapshot(verifiedRecovery)
            }.onFailure { rollbackError -> restoreError.addSuppressed(rollbackError) }
            throw restoreError
        }
    }
}

class LocalRecoveryPointStore(private val directory: File) {
    private val recoveryFile: File
        get() = File(directory, "restore-recovery-point.json")
    private val recoveryBackupFile: File
        get() = File(directory, "restore-recovery-point.json.bak")

    fun save(envelope: SnapshotEnvelope) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create local restore recovery directory")
        }
        val temporary = File(directory, "restore-recovery-point.json.tmp")
        temporary.writeText(RestoreSnapshotCodec.encode(envelope), Charsets.UTF_8)
        if (recoveryBackupFile.exists()) recoveryBackupFile.delete()
        if (recoveryFile.exists() && !recoveryFile.renameTo(recoveryBackupFile)) {
            throw IOException("Unable to stage previous local restore recovery point")
        }
        if (!temporary.renameTo(recoveryFile)) {
            recoveryBackupFile.renameTo(recoveryFile)
            throw IOException("Unable to commit local restore recovery point")
        }
        recoveryBackupFile.delete()
    }

    fun read(): SnapshotEnvelope? {
        val source = when {
            recoveryFile.isFile -> recoveryFile
            recoveryBackupFile.isFile -> recoveryBackupFile
            else -> return null
        }
        return RestoreSnapshotCodec.decode(source.readText(Charsets.UTF_8))
    }

    fun clear() {
        recoveryFile.delete()
        recoveryBackupFile.delete()
        File(directory, "restore-recovery-point.json.tmp").delete()
    }
}
