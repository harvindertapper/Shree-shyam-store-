package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shop_profiles")
data class ShopProfile(
    @PrimaryKey val uid: String,
    val shopName: String,
    val ownerName: String,
    val ownerPhone: String,
    val upiId: String = "",
    val email: String = "",
    val address: String = "",
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    /** MRP in integer paise. */
    val mrp: Long,
    /** Selling price in integer paise, or null when MRP is used. */
    val sellingPrice: Long? = null,
    /** Purchase price in integer paise, or null when not recorded. */
    val purchasePrice: Long? = null,
    val currentStock: Double = 0.0,
    val unit: String = "pcs", // e.g. "pcs", "kg", "g", "ltr", "pkt"
    val trackStock: Boolean = true,
    val lowStockAlertQty: Double = 5.0,
    val barcode: String = "",
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun getEffectivePrice(): Long {
        return if (sellingPrice != null && sellingPrice > 0L) sellingPrice else mrp
    }

    fun getFormattedStock(): String {
        return if (currentStock % 1.0 == 0.0) {
            "${currentStock.toLong()} $unit"
        } else {
            "%.2f %s".format(currentStock, unit)
        }
    }
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    /** Bill total in integer paise. */
    val totalAmount: Long,
    val paymentMode: String, // "CASH", "UPI", "UDHAAR"
    val customerId: Long? = null,
    val note: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    /** Unit selling price in integer paise. */
    val unitPrice: Long,
    /** Line total in integer paise. */
    val lineTotal: Long,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun getFormattedQuantity(): String {
        return if (quantity % 1.0 == 0.0) {
            "${quantity.toLong()} $unit"
        } else {
            "%.2f %s".format(quantity, unit)
        }
    }
}

// Aliases to ensure full compatibility with POS billing & ledger terminology
typealias Bill = Sale
typealias BillItem = SaleItem
typealias Transaction = UdhaarTransaction

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    /** Credit limit in integer paise. */
    val creditLimit: Long = 500_000L,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "udhaar_transactions")
data class UdhaarTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "''") val eventId: String = UUID.randomUUID().toString(),
    val customerId: Long,
    val saleId: Long? = null,
    val type: String, // "CREDIT", "PAYMENT", "REVERSAL", "CORRECTION"
    /** Ledger amount magnitude in integer paise; it is always positive. */
    val amount: Long,
    /** Signed effect of this event on the customer balance in integer paise. */
    @ColumnInfo(defaultValue = "0") val balanceEffect: Long = when (type) {
        "CREDIT" -> amount
        "PAYMENT" -> -amount
        else -> 0L
    },
    val note: String? = null,
    val correctsEventId: String? = null,
    val correctionReason: String? = null,
    @ColumnInfo(defaultValue = "'legacy-local'") val actorUid: String = "legacy-local",
    @ColumnInfo(defaultValue = "'Legacy local record'") val actorName: String = "Legacy local record",
    @ColumnInfo(defaultValue = "'OWNER'") val actorRole: String = "OWNER",
    @ColumnInfo(defaultValue = "'legacy-device'") val actorDeviceId: String = "legacy-device",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val oldStock: Double = 0.0,
    val newStock: Double = 0.0,
    val difference: Double = 0.0,
    val reason: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = "",
    val username: String,
    val email: String,
    val passwordHash: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
