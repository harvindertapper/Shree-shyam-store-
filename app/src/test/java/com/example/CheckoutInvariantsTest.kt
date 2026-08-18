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
        val productId = insertProduct(price = 1000L, stock = 5.0)
        val sale = sale(total = 1000L)
        val item = item(productId = productId, unitPrice = 1000L, quantity = 1.0, lineTotal = 900L)

        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item))
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertEquals(5.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun checkoutRejectsStockUnderflowAndRollsBackSale() = runBlocking {
        val productId = insertProduct(price = 1000L, stock = 1.0)
        val sale = sale(total = 2000L)
        val item = item(productId = productId, unitPrice = 1000L, quantity = 2.0, lineTotal = 2000L)

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
            Customer(name = "Limit Customer", creditLimit = 10_000L)
        )
        val productId = insertProduct(price = 12_000L, stock = 5.0)
        val sale = sale(total = 12_000L, paymentMode = "UDHAAR", customerId = customerId)
        val item = item(productId = productId, unitPrice = 12_000L, quantity = 1.0, lineTotal = 12_000L)

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

        repository.recordUdhaarPayment(customerId, 1235L, "cash")
        val transaction = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        assertEquals("PAYMENT", transaction.type)
        assertEquals(1235L, transaction.amount)

        expectIllegalArgument {
            repository.recordUdhaarPayment(99999L, 100L, "cash")
        }
    }

    @Test
    fun duplicateBillNumberIsRejectedBeforeSecondWrite() = runBlocking {
        val productId = insertProduct(price = 1000L, stock = 3.0)
        val sale = sale(total = 1000L)
        val item = item(productId = productId, unitPrice = 1000L, quantity = 1.0, lineTotal = 1000L)

        database.saleDao().completeBillCheckout(sale, listOf(item))
        expectIllegalArgument {
            database.saleDao().completeBillCheckout(sale, listOf(item))
        }

        assertEquals(1, database.saleDao().countAllSales())
        assertEquals(2.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun successfulCheckoutPersistsRoundedAmountsAndDeductsStockAtomically() = runBlocking {
        val productId = insertProduct(price = 1235L, stock = 3.0)
        val sale = sale(total = 2470L)
        val item = item(productId = productId, unitPrice = 1235L, quantity = 2.0, lineTotal = 2470L)

        val saleId = database.saleDao().completeBillCheckout(sale, listOf(item))
        val savedSale = database.saleDao().getSaleById(saleId)!!
        val savedItem = database.saleDao().getSaleItemsForSaleList(saleId).single()

        assertEquals(2470L, savedSale.totalAmount)
        assertEquals(1235L, savedItem.unitPrice)
        assertEquals(2470L, savedItem.lineTotal)
        assertEquals(1.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    private suspend fun insertProduct(price: Long, stock: Double): Long {
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
        total: Long,
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
        unitPrice: Long,
        quantity: Double,
        lineTotal: Long
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
