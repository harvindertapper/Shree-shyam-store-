package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val mrp: Double,
    val sellingPrice: Double? = null,
    val purchasePrice: Double? = null,
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
    fun getEffectivePrice(): Double {
        return if (sellingPrice != null && sellingPrice > 0.0) sellingPrice else mrp
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
    val totalAmount: Double,
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
    val unitPrice: Double,
    val lineTotal: Double,
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
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "udhaar_transactions")
data class UdhaarTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val saleId: Long? = null,
    val type: String, // "CREDIT", "PAYMENT"
    val amount: Double,
    val note: String? = null,
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
