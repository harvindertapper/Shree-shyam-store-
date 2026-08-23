package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction
import com.aistudio.shreeshyamstore.pqwzkb.utils.BusinessRelationshipPolicy
import org.junit.Test

class BusinessRelationshipPolicyTest {
    @Test(expected = IllegalArgumentException::class)
    fun productOrphanIsRejected() {
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = emptyList(),
            products = listOf(product(categoryId = 99L)),
            sales = emptyList(),
            saleItems = emptyList(),
            customers = emptyList(),
            udhaarTransactions = emptyList(),
            stockAdjustments = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateBillNumbersAreRejected() {
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = emptyList(),
            products = emptyList(),
            sales = listOf(
                sale(id = 1L, billNumber = "DUPLICATE-BILL"),
                sale(id = 2L, billNumber = "DUPLICATE-BILL")
            ),
            saleItems = emptyList(),
            customers = emptyList(),
            udhaarTransactions = emptyList(),
            stockAdjustments = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateBarcodeKeysAreRejected() {
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = listOf(Category(id = 1L, globalId = "category-1", name = "Grocery")),
            products = listOf(
                product(globalId = "product-1", barcodeKey = "8901"),
                product(globalId = "product-2", barcodeKey = "8901")
            ),
            sales = emptyList(),
            saleItems = emptyList(),
            customers = emptyList(),
            udhaarTransactions = emptyList(),
            stockAdjustments = emptyList()
        )
    }

    @Test
    fun validGraphPassesWithParentReferences() {
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = listOf(Category(id = 1L, globalId = "category-1", name = "Grocery")),
            products = listOf(product()),
            sales = listOf(sale(id = 1L)),
            saleItems = listOf(
                SaleItem(
                    id = 1L,
                    globalId = "item-1",
                    saleId = 1L,
                    productId = 1L,
                    productNameSnapshot = "Rice",
                    unitPrice = 100L,
                    lineTotal = 100L,
                    updatedAt = 1L,
                    mutationVersion = 1L
                )
            ),
            customers = listOf(Customer(id = 1L, globalId = "customer-1", name = "Customer")),
            udhaarTransactions = listOf(
                UdhaarTransaction(
                    id = 1L,
                    globalId = "ledger-1",
                    customerId = 1L,
                    saleId = 1L,
                    type = "CREDIT",
                    amount = 100L,
                    balanceEffect = 100L,
                    updatedAt = 1L,
                    mutationVersion = 1L
                )
            ),
            stockAdjustments = listOf(
                StockAdjustment(
                    id = 1L,
                    globalId = "adjustment-1",
                    productId = 1L,
                    reason = "Opening stock",
                    updatedAt = 1L,
                    mutationVersion = 1L
                )
            )
        )
    }

    private fun product(
        globalId: String = "product-1",
        categoryId: Long = 1L,
        barcodeKey: String? = null
    ) = Product(
        id = 1L,
        globalId = globalId,
        name = "Rice",
        categoryId = categoryId,
        mrp = 100L,
        barcodeKey = barcodeKey
    )

    private fun sale(id: Long, billNumber: String = "BILL-$id") = Sale(
        id = id,
        globalId = "sale-$id",
        billNumber = billNumber,
        totalAmount = 100L,
        paymentMode = "CASH",
        updatedAt = 1L,
        mutationVersion = 1L
    )
}
