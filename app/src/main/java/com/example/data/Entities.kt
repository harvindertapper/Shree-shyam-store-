package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val mrp: Double,
    val sellingPrice: Double? = null,
    val purchasePrice: Double? = null,
    val currentStock: Int = 0,
    val trackStock: Boolean = true,
    val lowStockAlertQty: Int = 5,
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
    val billNumber: String,
    val totalAmount: Double,
    val paymentMode: String, // "CASH", "UPI", "UDHAAR"
    val customerId: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "udhaar_transactions")
data class UdhaarTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val saleId: Long? = null,
    val type: String, // "CREDIT" (when buying on credit), "PAYMENT" (when paying back)
    val amount: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val oldStock: Int,
    val newStock: Int,
    val difference: Int,
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

