package com.sevenzenlabs.zenmart.utils

import com.sevenzenlabs.zenmart.data.Category
import com.sevenzenlabs.zenmart.data.Customer
import com.sevenzenlabs.zenmart.data.Product
import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.ReturnItem
import com.sevenzenlabs.zenmart.data.Sale
import com.sevenzenlabs.zenmart.data.SaleItem
import com.sevenzenlabs.zenmart.data.StockAdjustment
import com.sevenzenlabs.zenmart.data.UdhaarTransaction

/**
 * Application-boundary integrity checks for the cloud-restorable tables.
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
        stockAdjustments: List<StockAdjustment>,
        returns: List<Return> = emptyList(),
        returnItems: List<ReturnItem> = emptyList()
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
        if (returns.isNotEmpty()) {
            requireUnique("returns.globalId", returns.map { it.globalId })
            requireUnique("returns.returnNumber", returns.map { it.returnNumber })
            returns.forEach { ret ->
                require(ret.saleId > 0L && ret.saleId in saleIds) {
                    "Return references a missing sale"
                }
            }
        }
        if (returnItems.isNotEmpty()) {
            requireUnique("return_items.globalId", returnItems.map { it.globalId })
            val returnIds = returns.map { it.id }.filter { it > 0L }.toSet()
            val saleItemIds = saleItems.map { it.id }.filter { it > 0L }.toSet()
            val returnsById = returns.associateBy { it.id }
            val saleItemsById = saleItems.associateBy { it.id }
            returnItems.forEach { rItem ->
                require(rItem.returnId > 0L && rItem.returnId in returnIds) {
                    "Return item references a missing return"
                }
                require(rItem.saleItemId > 0L && rItem.saleItemId in saleItemIds) {
                    "Return item references a missing sale item"
                }
                require(rItem.productId > 0L && rItem.productId in productIds) {
                    "Return item references a missing product"
                }
                val parentReturn = returnsById[rItem.returnId]
                val saleItem = saleItemsById[rItem.saleItemId]
                require(
                    parentReturn != null && saleItem != null &&
                    parentReturn.saleId == saleItem.saleId &&
                    rItem.productId == saleItem.productId
                ) {
                    "Return item does not match parent return sale or sale item product"
                }
            }
        }
    }

    private fun requireUnique(field: String, values: List<String>) {
        require(values.none { it.isBlank() } && values.size == values.toSet().size) {
            "$field contains a blank or duplicate value"
        }
    }
}
