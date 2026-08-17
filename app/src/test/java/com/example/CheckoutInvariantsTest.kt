package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Customer
import com.example.data.Product
import com.example.data.Sale
import com.example.data.SaleItem
import com.example.data.ShopRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckoutInvariantsTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShopRepository(
            categoryDao = database.categoryDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao(),
            userDao = database.userDao(),
            database = database,
            shopProfileDao = database.shopProfileDao()
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun checkoutRejectsLineTotalMismatchBeforeWritingAnything() = runBlocking {
        val productId = insertProduct(price = 10.0, stock = 5.0)
        val sale = sale(total = 10.0)
        val item = item(productId = productId, unitPrice = 10.0, quantity = 1.0, lineTotal = 9.0)

        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item))
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertEquals(5.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun checkoutRejectsStockUnderflowAndRollsBackSale() = runBlocking {
        val productId = insertProduct(price = 10.0, stock = 1.0)
        val sale = sale(total = 20.0)
        val item = item(productId = productId, unitPrice = 10.0, quantity = 2.0, lineTotal = 20.0)

        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item))
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertTrue(database.saleDao().getAllSaleItemsList().isEmpty())
        assertEquals(1.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun udhaarCheckoutRejectsProjectedBalanceAboveCreditLimit() = runBlocking {
        val customerId = database.customerDao().insertCustomer(
            Customer(name = "Limit Customer", creditLimit = 100.0)
        )
        val productId = insertProduct(price = 120.0, stock = 5.0)
        val sale = sale(total = 120.0, paymentMode = "UDHAAR", customerId = customerId)
        val item = item(productId = productId, unitPrice = 120.0, quantity = 1.0, lineTotal = 120.0)

        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item), customerId)
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertTrue(database.udhaarDao().getTransactionsForCustomerList(customerId).isEmpty())
        assertEquals(5.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun receivedPaymentIsRoundedAndRequiresAnActiveCustomer() = runBlocking {
        val customerId = database.customerDao().insertCustomer(Customer(name = "Payment Customer"))

        repository.recordUdhaarPayment(customerId, 12.345, "cash")
        val transaction = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        assertEquals("PAYMENT", transaction.type)
        assertEquals(12.35, transaction.amount, 0.0)

        expectIllegalArgument {
            repository.recordUdhaarPayment(99999L, 1.0, "cash")
        }
    }

    @Test
    fun duplicateBillNumberIsRejectedBeforeSecondWrite() = runBlocking {
        val productId = insertProduct(price = 10.0, stock = 3.0)
        val sale = sale(total = 10.0)
        val item = item(productId = productId, unitPrice = 10.0, quantity = 1.0, lineTotal = 10.0)

        database.saleDao().completeBillCheckout(sale, listOf(item))
        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item))
        }

        assertEquals(1, database.saleDao().countAllSales())
        assertEquals(2.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun successfulCheckoutPersistsRoundedAmountsAndDeductsStockAtomically() = runBlocking {
        val productId = insertProduct(price = 12.345, stock = 3.0)
        val sale = sale(total = 24.70)
        val item = item(productId = productId, unitPrice = 12.345, quantity = 2.0, lineTotal = 24.70)

        val saleId = database.saleDao().completeBillCheckout(sale, listOf(item))
        val savedSale = database.saleDao().getSaleById(saleId)!!
        val savedItem = database.saleDao().getSaleItemsForSaleList(saleId).single()

        assertEquals(24.70, savedSale.totalAmount, 0.0)
        assertEquals(12.35, savedItem.unitPrice, 0.0)
        assertEquals(24.70, savedItem.lineTotal, 0.0)
        assertEquals(1.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    private suspend fun insertProduct(price: Double, stock: Double): Long {
        return database.productDao().insert(
            Product(
                name = "Test Product",
                categoryId = 1L,
                mrp = price,
                sellingPrice = price,
                currentStock = stock,
                trackStock = true
            )
        )
    }

    private fun sale(
        total: Double,
        paymentMode: String = "CASH",
        customerId: Long? = null
    ): Sale = Sale(
        billNumber = "TEST-BILL-${System.nanoTime()}",
        totalAmount = total,
        paymentMode = paymentMode,
        customerId = customerId
    )

    private fun item(
        productId: Long,
        unitPrice: Double,
        quantity: Double,
        lineTotal: Double
    ): SaleItem = SaleItem(
        saleId = 0L,
        productId = productId,
        productNameSnapshot = "Test Product",
        quantity = quantity,
        unit = "pcs",
        unitPrice = unitPrice,
        lineTotal = lineTotal
    )

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected checkout validation to reject the operation")
        } catch (_: IllegalArgumentException) {
            // Expected domain rejection.
        }
    }
}
