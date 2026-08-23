package com.aistudio.shreeshyamstore.pqwzkb.utils

import androidx.room.withTransaction
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction

/** Parsed records plus redacted metadata from one remote collection query. */
data class SyncPullCollection<T>(
    val records: List<T>,
    val receivedCount: Int,
    val highWaterMark: Long
)

/** Fully parsed downstream data. It contains no Firestore handles or partial state. */
data class SyncPullBatch(
    val categories: List<Category>,
    val products: List<Product>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val customers: List<Customer>,
    val udhaarTransactions: List<UdhaarTransaction>,
    val stockAdjustments: List<StockAdjustment>,
    val highWaterMark: Long,
    val receivedCount: Int
) {
    val appliedCount: Int
        get() = categories.size + products.size + sales.size + saleItems.size +
            customers.size + udhaarTransactions.size + stockAdjustments.size

    fun validate(previousCursor: Long) {
        require(previousCursor >= 0L) { "Previous sync cursor cannot be negative" }
        require(highWaterMark >= previousCursor) { "Pull high-water mark cannot regress" }
        require(receivedCount >= appliedCount) { "Applied records cannot exceed received records" }
        requireUniqueGlobalIds("categories", categories.map { it.globalId })
        requireUniqueGlobalIds("products", products.map { it.globalId })
        requireUniqueGlobalIds("sales", sales.map { it.globalId })
        requireUniqueGlobalIds("sale_items", saleItems.map { it.globalId })
        requireUniqueGlobalIds("customers", customers.map { it.globalId })
        requireUniqueGlobalIds("udhaar_transactions", udhaarTransactions.map { it.globalId })
        requireUniqueGlobalIds("stock_adjustments", stockAdjustments.map { it.globalId })
        categories.forEach { requireRecordMetadata("categories", it.globalId, it.updatedAt, previousCursor) }
        products.forEach { requireRecordMetadata("products", it.globalId, it.updatedAt, previousCursor) }
        sales.forEach { requireRecordMetadata("sales", it.globalId, it.updatedAt, previousCursor) }
        saleItems.forEach { requireRecordMetadata("sale_items", it.globalId, it.updatedAt, previousCursor) }
        customers.forEach { requireRecordMetadata("customers", it.globalId, it.updatedAt, previousCursor) }
        udhaarTransactions.forEach { requireRecordMetadata("udhaar_transactions", it.globalId, it.updatedAt, previousCursor) }
        stockAdjustments.forEach { requireRecordMetadata("stock_adjustments", it.globalId, it.updatedAt, previousCursor) }
    }

    suspend fun applyAtomically(database: AppDatabase) {
        database.withTransaction {
            if (categories.isNotEmpty()) database.categoryDao().upsertAllForSync(categories)
            if (products.isNotEmpty()) database.productDao().upsertAllForSync(products)
            if (customers.isNotEmpty()) database.customerDao().upsertAllForSync(customers)
            if (sales.isNotEmpty()) database.saleDao().upsertAllSalesForSync(sales)
            if (saleItems.isNotEmpty()) database.saleDao().upsertAllSaleItemsForSync(saleItems)
            if (udhaarTransactions.isNotEmpty()) database.udhaarDao().upsertAllForSync(udhaarTransactions)
            if (stockAdjustments.isNotEmpty()) database.stockAdjustmentDao().upsertAllForSync(stockAdjustments)
        }
    }

    private fun requireUniqueGlobalIds(tableName: String, globalIds: List<String>) {
        require(globalIds.none { it.isBlank() }) { "$tableName contains a blank global ID" }
        require(globalIds.size == globalIds.toSet().size) { "$tableName contains duplicate global IDs" }
    }

    private fun requireRecordMetadata(
        tableName: String,
        globalId: String,
        updatedAt: Long,
        previousCursor: Long
    ) {
        require(globalId.isNotBlank()) { "$tableName contains a blank global ID" }
        require(updatedAt > 0L) { "$tableName contains an invalid update timestamp" }
        require(updatedAt >= previousCursor) { "$tableName contains a record older than the requested cursor" }
    }
}
