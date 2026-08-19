package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckoutInvariantsTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    private val testActor = LedgerActor("owner-1", "Test Owner", "OWNER", "test-device")
    private val testPlatformActor = PlatformActor("owner-1", "Test Owner", "OWNER", "test-device")
    private val testTenant = TenantScope("org-test", "store-test", "membership-owner-1", "test-device", "install-test")

    private fun command(
        actor: PlatformActor = testPlatformActor,
        tenant: TenantScope = testTenant,
        clientCreatedAt: Long = System.currentTimeMillis()
    ) = CommandMetadata(
        idempotencyKey = UUID.randomUUID().toString(),
        clientEventId = UUID.randomUUID().toString(),
        tenant = tenant,
        actor = actor,
        clientCreatedAt = clientCreatedAt
    )

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
            shopProfileDao = database.shopProfileDao(),
            authorizationContextProvider = { testTenant to testPlatformActor }
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
            database.saleDao().completeBillCheckout(
                sale = sale,
                items = listOf(item),
                selectedCustomerId = customerId,
                ledgerActor = testActor
            )
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertTrue(database.udhaarDao().getTransactionsForCustomerList(customerId).isEmpty())
        assertEquals(5.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun receivedPaymentIsRoundedAndRequiresAnActiveCustomer() = runBlocking {
        val customerId = database.customerDao().insertCustomer(Customer(name = "Payment Customer"))

        repository.recordUdhaarPayment(customerId, 1235L, "cash", command = command())
        val transaction = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        assertEquals("PAYMENT", transaction.type)
        assertEquals(1235L, transaction.amount)
        assertEquals(-1235L, transaction.balanceEffect)
        assertEquals("owner-1", transaction.actorUid)

        expectIllegalArgument {
            repository.recordUdhaarPayment(99999L, 100L, "cash", command = command())
        }
    }

    @Test
    fun ledgerCorrectionIsAppendOnlyAndCannotBeAppliedTwice() = runBlocking {
        val customerId = database.customerDao().insertCustomer(Customer(name = "Correction Customer"))
        repository.recordUdhaarPayment(customerId, 1000L, "cash", command = command())
        val original = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        val correctionId = repository.correctUdhaarTransaction(
            customerId = customerId,
            eventId = original.eventId,
            correctedAmountMinorUnits = 700L,
                reason = "Entered amount was incorrect",
                command = command()
        )
        val rows = database.udhaarDao().getTransactionsForCustomerList(customerId)
        val correction = rows.single { it.id == correctionId }

        assertEquals(2, rows.size)
        assertEquals(-700L, database.udhaarDao().getCustomerBalance(customerId))
        assertEquals(-1000L, original.balanceEffect)
        assertEquals(300L, correction.balanceEffect)
        assertEquals(original.eventId, correction.correctsEventId)

        expectIllegalArgument {
            repository.correctUdhaarTransaction(
                customerId = customerId,
                eventId = original.eventId,
                correctedAmountMinorUnits = 600L,
                reason = "Second correction must fail",
                command = command()
            )
        }
    }

    @Test
    fun cashierCannotCorrectLedgerEvents() = runBlocking {
        val customerId = database.customerDao().insertCustomer(Customer(name = "Authorization Customer"))
        repository.recordUdhaarPayment(customerId, 1000L, "cash", command = command())
        val original = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        expectIllegalArgument {
            repository.reverseUdhaarTransaction(
                customerId = customerId,
                eventId = original.eventId,
                reason = "Cashier correction attempt",
                command = command(actor = testPlatformActor.copy(role = "CASHIER"))
            )
        }
        assertEquals(1, database.udhaarDao().getTransactionsForCustomerList(customerId).size)
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
        assertEquals(PaymentState.RECEIVED.wireValue, savedSale.paymentState)
        assertEquals(2470L, savedSale.receivedAmount ?: -1L)
        assertEquals(1.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun cashChangeIsPersistedAndUpiMismatchRollsBackAtomically() = runBlocking {
        val cashProductId = insertProduct(price = 1000L, stock = 2.0)
        val cashSale = sale(total = 1000L).copy(receivedAmount = 1500L)
        val cashItem = item(productId = cashProductId, unitPrice = 1000L, quantity = 1.0, lineTotal = 1000L)
        val cashSaleId = database.saleDao().completeBillCheckout(cashSale, listOf(cashItem))
        val savedCash = database.saleDao().getSaleById(cashSaleId)!!
        assertEquals(1500L, savedCash.receivedAmount ?: -1L)

        val upiProductId = insertProduct(price = 1000L, stock = 2.0)
        val upiSale = sale(total = 1000L, paymentMode = "UPI").copy(receivedAmount = 999L)
        val upiItem = item(productId = upiProductId, unitPrice = 1000L, quantity = 1.0, lineTotal = 1000L)
        expectIllegalArgument {
            database.saleDao().completeBillCheckout(upiSale, listOf(upiItem))
        }
        assertEquals(1, database.saleDao().countAllSales())
        assertEquals(2.0, database.productDao().getProductById(upiProductId)!!.currentStock, 0.0)
    }

    @Test
    fun paymentStateReconciliationIsAuditedAndCannotRegress() = runBlocking {
        val saleId = database.saleDao().insertSale(
            sale(total = 1000L, paymentMode = "UPI").copy(
                paymentState = PaymentState.PENDING.wireValue,
                receivedAmount = null
            )
        )
        val reconciled = repository.reconcilePaymentState(
            saleId = saleId,
            targetState = PaymentState.RECEIVED,
            receivedAmount = 1000L,
            command = command()
        )
        assertEquals(PaymentState.RECEIVED.wireValue, reconciled.paymentState)
        assertEquals(1000L, reconciled.receivedAmount ?: -1L)

        expectIllegalArgument {
            repository.reconcilePaymentState(
                saleId = saleId,
                targetState = PaymentState.FAILED,
                receivedAmount = 0L,
                command = command()
            )
        }
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
