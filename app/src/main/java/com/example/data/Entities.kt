package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val CONFLICT = "CONFLICT"
    const val FAILED = "FAILED"
}

object DataUnitType {
    const val PIECE = "PIECE"
    const val WEIGHT = "WEIGHT"
    const val VOLUME = "VOLUME"
}

object DataDisplayUnit {
    const val PIECE = "PIECE"
    const val GRAM = "GRAM"
    const val KILOGRAM = "KILOGRAM"
    const val MILLILITER = "MILLILITER"
    const val LITER = "LITER"
}

object SaleStatus {
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
    const val RETURNED = "RETURNED"
    const val REFUNDED = "REFUNDED"
}

internal fun newLocalUuid(): String = UUID.randomUUID().toString()

internal fun rupeesToPaise(amount: Double): Long {
    return BigDecimal.valueOf(amount)
        .multiply(BigDecimal.valueOf(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val name: String,
    val categoryId: Long,
    val mrp: Double,
    val sellingPrice: Double? = null,
    val purchasePrice: Double? = null,
    val unitType: String = DataUnitType.PIECE,
    val displayUnit: String = DataDisplayUnit.PIECE,
    val baseUnit: String = DataDisplayUnit.PIECE,
    val allowsDecimalQuantity: Boolean = false,
    val quantityScale: Int = 0,
    val pricePerUnitPaise: Long = rupeesToPaise(sellingPrice?.takeIf { it > 0.0 } ?: mrp),
    val priceUnitBaseQty: Long = 1,
    val purchasePricePerUnitPaise: Long? = purchasePrice?.let { rupeesToPaise(it) },
    val purchasePriceUnitBaseQty: Long? = purchasePrice?.let { 1L },
    val currentStock: Int = 0,
    val stockQuantityBase: Long = currentStock.toLong(),
    val trackStock: Boolean = true,
    val lowStockAlertQty: Int = 5,
    val lowStockAlertBase: Long = lowStockAlertQty.toLong(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getEffectivePrice(): Double {
        return if (sellingPrice != null && sellingPrice > 0.0) sellingPrice else mrp
    }
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val deviceId: String? = null,
    val billSequence: Long? = null,
    val idempotencyKey: String = newLocalUuid(),
    val billNumber: String,
    val totalAmount: Double,
    val totalAmountPaise: Long = rupeesToPaise(totalAmount),
    val paymentMode: String, // "CASH", "UPI", "UDHAAR"
    val saleStatus: String = SaleStatus.COMPLETED,
    val customerId: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val saleId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitTypeSnapshot: String = DataUnitType.PIECE,
    val displayUnitSnapshot: String = DataDisplayUnit.PIECE,
    val baseUnitSnapshot: String = DataDisplayUnit.PIECE,
    val enteredQuantityText: String = quantity.toString(),
    val quantityBase: Long = quantity.toLong(),
    val unitPrice: Double,
    val originalPricePerUnitPaise: Long = rupeesToPaise(unitPrice),
    val originalPriceUnitBaseQty: Long = 1,
    val effectivePricePerUnitPaise: Long = originalPricePerUnitPaise,
    val effectivePriceUnitBaseQty: Long = originalPriceUnitBaseQty,
    val rateOverridden: Boolean = false,
    val lineTotal: Double,
    val lineTotalPaise: Long = rupeesToPaise(lineTotal),
    val purchasePricePerUnitPaiseSnapshot: Long? = null,
    val purchasePriceUnitBaseQtySnapshot: Long? = null
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val name: String,
    val phone: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "udhaar_transactions")
data class UdhaarTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val customerId: Long,
    val saleId: Long? = null,
    val type: String, // "CREDIT" (when buying on credit), "PAYMENT" (when paying back)
    val amount: Double,
    val amountPaise: Long = rupeesToPaise(amount),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUuid: String = newLocalUuid(),
    val remoteId: String? = null,
    val shopId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val createdByUid: String? = null,
    val updatedByUid: String? = null,
    val sourceDeviceId: String? = null,
    val productId: Long,
    val oldStock: Int,
    val oldQuantityBase: Long = oldStock.toLong(),
    val newStock: Int,
    val newQuantityBase: Long = newStock.toLong(),
    val difference: Int,
    val differenceBase: Long = difference.toLong(),
    val displayUnitSnapshot: String = DataDisplayUnit.PIECE,
    val reason: String, // e.g. "Opening stock entry", "Purchase added", "Manual correction", "Damaged/expired", etc.
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String, // Plaintext or Cipher text representation
    val createdAt: Long = System.currentTimeMillis()
)

