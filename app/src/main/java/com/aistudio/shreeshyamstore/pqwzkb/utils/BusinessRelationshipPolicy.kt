package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction

/**
 * Application-boundary integrity checks for the seven cloud-restorable tables.
 *
 * The database already contains legacy installations that may have orphaned
 * rows, so this slice adds relationship indexes and rejects invalid complete
 * restore graphs rather than rebuilding every table under a new SQLite FK.
 */
object BusinessRelationshipPolicy {
    fun validateRestoreGraph(
        categories: List<Category>,
        products: List<Product>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        customers: List<Customer>,
        udhaarTransactions: List<UdhaarTransaction>,
        stockAdjustments: List<StockAdjustment>
    ) {
        requireUnique("categories.globalId", categories.map { it.globalId })
        requireUnique("products.globalId", products.map { it.globalId })
        requireUnique(
            "products.barcodeKey",
            products.mapNotNull { it.barcodeKey?.trim()?.takeIf(String::isNotEmpty) }
        )
        requireUnique("sales.globalId", sales.map { it.globalId })
        requireUnique("sales.billNumber", sales.map { it.billNumber })
        requireUnique("sale_items.globalId", saleItems.map { it.globalId })
        requireUnique("customers.globalId", customers.map { it.globalId })
        requireUnique("udhaar_transactions.globalId", udhaarTransactions.map { it.globalId })
        requireUnique("stock_adjustments.globalId", stockAdjustments.map { it.globalId })

        val categoryIds = categories.map { it.id }.filter { it > 0L }.toSet()
        val productIds = products.map { it.id }.filter { it > 0L }.toSet()
        val saleIds = sales.map { it.id }.filter { it > 0L }.toSet()
        val customerIds = customers.map { it.id }.filter { it > 0L }.toSet()

        products.forEach { product ->
            require(product.categoryId > 0L && product.categoryId in categoryIds) {
                "Product references a missing category"
            }
        }
        sales.forEach { sale ->
            require(sale.customerId == null || sale.customerId in customerIds) {
                "Sale references a missing customer"
            }
        }
        saleItems.forEach { item ->
            require(item.saleId > 0L && item.saleId in saleIds) {
                "Sale item references a missing sale"
            }
            require(item.productId > 0L && item.productId in productIds) {
                "Sale item references a missing product"
            }
        }
        udhaarTransactions.forEach { transaction ->
            require(transaction.customerId > 0L && transaction.customerId in customerIds) {
                "Ledger transaction references a missing customer"
            }
            require(transaction.saleId == null || transaction.saleId in saleIds) {
                "Ledger transaction references a missing sale"
            }
        }
        stockAdjustments.forEach { adjustment ->
            require(adjustment.productId > 0L && adjustment.productId in productIds) {
                "Stock adjustment references a missing product"
            }
        }
    }

    private fun requireUnique(field: String, values: List<String>) {
        require(values.none { it.isBlank() } && values.size == values.toSet().size) {
            "$field contains a blank or duplicate value"
        }
    }
}
