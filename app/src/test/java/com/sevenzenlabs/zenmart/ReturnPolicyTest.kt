package com.sevenzenlabs.zenmart

import android.content.Context
import androidx.room.Room
import com.sevenzenlabs.zenmart.commerce.CommandMetadata
import com.sevenzenlabs.zenmart.commerce.CommerceValidation
import com.sevenzenlabs.zenmart.commerce.PaymentMode
import com.sevenzenlabs.zenmart.commerce.PaymentState
import com.sevenzenlabs.zenmart.commerce.PlatformActor
import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.data.AppDatabase
import com.sevenzenlabs.zenmart.data.Category
import com.sevenzenlabs.zenmart.data.Customer
import com.sevenzenlabs.zenmart.data.ItemReturnRequest
import com.sevenzenlabs.zenmart.data.Product
import com.sevenzenlabs.zenmart.data.Sale
import com.sevenzenlabs.zenmart.data.SaleItem
import com.sevenzenlabs.zenmart.data.ShopRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReturnPolicyTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    private val testActor = PlatformActor("owner-1", "Test Owner", "OWNER", "test-device")
    private val cashierActor = PlatformActor("cashier-1", "Test Cashier", "CASHIER", "test-device")
    private val testTenant = TenantScope("org-test", "store-test", "membership-owner-1", "test-device", "install-test")
    private var authenticatedActor = testActor

    private fun command(
        actor: PlatformActor = authenticatedActor,
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
            authorizationContextProvider = { testTenant to authenticatedActor }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fullReturnRestoresStockAndTransitionsPaymentToRefunded() = runBlocking {
        authenticatedActor = testActor
        val categoryId = repository.insertCategory(Category(name = "Groceries"), command())
        val productId = repository.insertProductWithOpeningStock(
            Product(
                name = "Aashirvaad Atta 5kg",
                categoryId = categoryId,
                mrp = 25000L,
                sellingPrice = 24000L,
                currentStock = 10.0,
                unit = "pcs",
                trackStock = true
            ),
            openingStock = 10.0,
            command = command()
        )

        // Checkout 2 units
        val saleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-FULL-101",
                totalAmount = 48000L,
                paymentMode = PaymentMode.CASH.name,
                receivedAmount = 48000L
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Aashirvaad Atta 5kg",
                    quantity = 2.0,
                    unit = "pcs",
                    unitPrice = 24000L,
                    lineTotal = 48000L
                )
            ),
            command = command()
        )

        val prodAfterSale = repository.getProductById(productId)!!
        assertEquals(8.0, prodAfterSale.currentStock, 0.001)

        val saleItems = repository.getSaleItemsForSaleList(saleId)
        val returnResult = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(saleItemId = saleItems[0].id, quantityReturned = 2.0)),
            refundMode = "CASH",
            reason = "Customer changed mind",
            note = "Returned unopened packet",
            command = command()
        )

        assertEquals(48000L, returnResult.totalRefundAmount)
        assertEquals(PaymentState.REFUNDED, returnResult.updatedPaymentState)
        assertEquals(1, returnResult.returnItemCount)

        // Verify Product stock restored
        val prodAfterReturn = repository.getProductById(productId)!!
        assertEquals(10.0, prodAfterReturn.currentStock, 0.001)

        // Verify Sale payment state in DB
        val saleAfterReturn = repository.getSaleById(saleId)!!
        assertEquals(PaymentState.REFUNDED.wireValue, saleAfterReturn.paymentState)
        assertEquals(0L, saleAfterReturn.receivedAmount)

        // Verify Return audit record in DB
        val returns = repository.getReturnsForSaleList(saleId)
        assertEquals(1, returns.size)
        assertEquals(48000L, returns[0].totalRefundAmount)
        assertEquals("BILL-FULL-101", returns[0].originalBillNumber)

        val returnItems = repository.getReturnItemsForReturnList(returns[0].id)
        assertEquals(1, returnItems.size)
        assertEquals(2.0, returnItems[0].quantityReturned, 0.001)
        assertEquals(48000L, returnItems[0].lineRefundTotal)

        // Verify remaining returnable is 0
        val remaining = repository.getRemainingReturnableQuantities(saleId)
        assertEquals(0.0, remaining[saleItems[0].id]!!, 0.001)
    }

    @Test
    fun partialReturnPreventsOverReturnsAndTransitionsIncrementally() = runBlocking {
        authenticatedActor = testActor
        val categoryId = repository.insertCategory(Category(name = "Beverages"), command())
        val productId = repository.insertProductWithOpeningStock(
            Product(
                name = "Amul Milk 1L",
                categoryId = categoryId,
                mrp = 7000L,
                sellingPrice = 6600L,
                currentStock = 20.0,
                unit = "pcs",
                trackStock = true
            ),
            openingStock = 20.0,
            command = command()
        )

        // Checkout 5 units
        val saleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-PARTIAL-202",
                totalAmount = 33000L,
                paymentMode = PaymentMode.UPI.name,
                receivedAmount = 33000L
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Amul Milk 1L",
                    quantity = 5.0,
                    unit = "pcs",
                    unitPrice = 6600L,
                    lineTotal = 33000L
                )
            ),
            command = command()
        )

        val saleItems = repository.getSaleItemsForSaleList(saleId)
        val lineItem = saleItems[0]

        // First partial return: 2 units
        val result1 = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(lineItem.id, 2.0)),
            refundMode = "UPI",
            reason = "Partial return",
            command = command()
        )

        assertEquals(13200L, result1.totalRefundAmount)
        assertEquals(PaymentState.PARTIALLY_REFUNDED, result1.updatedPaymentState)

        val prodAfterReturn1 = repository.getProductById(productId)!!
        assertEquals(17.0, prodAfterReturn1.currentStock, 0.001)

        val remainingAfter1 = repository.getRemainingReturnableQuantities(saleId)
        assertEquals(3.0, remainingAfter1[lineItem.id]!!, 0.001)

        // Attempt over-return: trying to return 4 units when only 3 remain
        try {
            repository.processReturn(
                saleId = saleId,
                itemsToReturn = listOf(ItemReturnRequest(lineItem.id, 4.0)),
                refundMode = "UPI",
                reason = "Over return attempt",
                command = command()
            )
            fail("Expected IllegalArgumentException on over-return")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("only 3.0 remaining"))
        }

        // Attempt duplicate line items in same request
        try {
            repository.processReturn(
                saleId = saleId,
                itemsToReturn = listOf(
                    ItemReturnRequest(lineItem.id, 1.0),
                    ItemReturnRequest(lineItem.id, 1.0)
                ),
                refundMode = "UPI",
                reason = "Duplicate attempt",
                command = command()
            )
            fail("Expected IllegalArgumentException on duplicate line item request")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Duplicate line items"))
        }

        // Return remaining 3 units -> should transition to REFUNDED
        val result2 = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(lineItem.id, 3.0)),
            refundMode = "UPI",
            reason = "Remaining return",
            command = command()
        )

        assertEquals(19800L, result2.totalRefundAmount)
        assertEquals(PaymentState.REFUNDED, result2.updatedPaymentState)

        val prodFinal = repository.getProductById(productId)!!
        assertEquals(20.0, prodFinal.currentStock, 0.001)

        val remainingFinal = repository.getRemainingReturnableQuantities(saleId)
        assertEquals(0.0, remainingFinal[lineItem.id]!!, 0.001)
    }

    @Test
    fun udhaarSaleReturnReversesCustomerDebtAtomically() = runBlocking {
        authenticatedActor = testActor
        val categoryId = repository.insertCategory(Category(name = "Snacks"), command())
        val productId = repository.insertProductWithOpeningStock(
            Product(
                name = "Haldiram Bhujia 400g",
                categoryId = categoryId,
                mrp = 12000L,
                sellingPrice = 11000L,
                currentStock = 15.0,
                unit = "pcs",
                trackStock = true
            ),
            openingStock = 15.0,
            command = command()
        )

        val customer = Customer(name = "Ramesh Kumar", phone = "9876543210", creditLimit = 50000L)
        val customerId = database.customerDao().insertCustomer(customer)

        // Checkout on Udhaar: 3 units = 33000L
        val saleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-UDHAAR-303",
                totalAmount = 33000L,
                paymentMode = PaymentMode.UDHAAR.name,
                customerId = customerId,
                receivedAmount = 0L
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Haldiram Bhujia 400g",
                    quantity = 3.0,
                    unit = "pcs",
                    unitPrice = 11000L,
                    lineTotal = 33000L
                )
            ),
            selectedCustomerId = customerId,
            command = command()
        )

        // Initial Udhaar balance should be 33000L
        val initialBalance = database.udhaarDao().getCustomerBalance(customerId)
        assertEquals(33000L, initialBalance)

        val saleItems = repository.getSaleItemsForSaleList(saleId)

        // Return 1 unit = 11000L
        val returnResult = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(saleItems[0].id, 1.0)),
            refundMode = "UDHAAR_REVERSAL",
            reason = "Customer request",
            command = command()
        )

        assertEquals(11000L, returnResult.totalRefundAmount)
        assertEquals(PaymentState.PARTIALLY_REFUNDED, returnResult.updatedPaymentState)

        // Customer debt must now be 33000 - 11000 = 22000L
        val updatedBalance = database.udhaarDao().getCustomerBalance(customerId)
        assertEquals(22000L, updatedBalance)

        // Verify UdhaarTransaction REVERSAL entry
        val customerTxs = database.udhaarDao().getTransactionsForCustomerList(customerId)
        val reversalTx = customerTxs.find { it.type == "REVERSAL" }
        assertNotNull(reversalTx)
        assertEquals(11000L, reversalTx!!.amount)
        assertEquals(-11000L, reversalTx.balanceEffect)
    }

    /**
     * Verifies that cashier role is authorized to process returns, while commands
     * exceeding the staleness threshold (>5 minutes) are rejected on non-refunded sales.
     */
    @Test
    fun cashierCanProcessReturnWhileStaleCommandFails() = runBlocking {
        authenticatedActor = cashierActor
        val categoryId = repository.insertCategory(Category(name = "Dairy"), command(actor = testActor))
        val productId = repository.insertProductWithOpeningStock(
            Product(
                name = "Ghee 1L",
                categoryId = categoryId,
                mrp = 65000L,
                sellingPrice = 60000L,
                currentStock = 5.0,
                unit = "pcs",
                trackStock = true
            ),
            openingStock = 5.0,
            command = command(actor = testActor)
        )

        val saleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-CASHIER-404",
                totalAmount = 60000L,
                paymentMode = PaymentMode.CASH.name,
                receivedAmount = 60000L
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Ghee 1L",
                    quantity = 1.0,
                    unit = "pcs",
                    unitPrice = 60000L,
                    lineTotal = 60000L
                )
            ),
            command = command(actor = cashierActor)
        )

        val saleItems = repository.getSaleItemsForSaleList(saleId)

        // Cashier processes return successfully
        val res = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(saleItems[0].id, 1.0)),
            refundMode = "CASH",
            reason = "Customer return",
            command = command(actor = cashierActor)
        )
        assertEquals(PaymentState.REFUNDED, res.updatedPaymentState)

        // Stale command (> 5 min old) must be rejected on a fresh non-refunded sale
        val staleSaleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-STALE-505",
                totalAmount = 60000L,
                paymentMode = PaymentMode.CASH.name,
                receivedAmount = 60000L
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Ghee 1L",
                    quantity = 1.0,
                    unit = "pcs",
                    unitPrice = 60000L,
                    lineTotal = 60000L
                )
            ),
            command = command(actor = cashierActor)
        )
        val staleSaleItems = repository.getSaleItemsForSaleList(staleSaleId)
        val staleTime = System.currentTimeMillis() - 10 * 60 * 1000L
        try {
            repository.processReturn(
                saleId = staleSaleId,
                itemsToReturn = listOf(ItemReturnRequest(staleSaleItems[0].id, 1.0)),
                refundMode = "CASH",
                reason = "Stale attempt",
                command = command(actor = cashierActor, clientCreatedAt = staleTime)
            )
            fail("Expected exception for stale command")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("stale", ignoreCase = true))
        }
    }

    /**
     * Verifies that returning fractional quantities (e.g. 1.25 kg of 2.5 kg Basmati Rice)
     * correctly calculates refund amounts in paise, updates payment state, restores stock,
     * and accurately tracks remaining returnable quantities.
     */
    @Test
    fun fractionalQuantityReturnCalculatesCorrectRefundAndRestoresStock() = runBlocking {
        authenticatedActor = testActor
        val categoryId = repository.insertCategory(Category(name = "Grains"), command())
        val productId = repository.insertProductWithOpeningStock(
            Product(
                name = "Basmati Rice Loose",
                categoryId = categoryId,
                mrp = 10000L,
                sellingPrice = 9000L,
                currentStock = 10.0,
                unit = "kg",
                trackStock = true
            ),
            openingStock = 10.0,
            command = command()
        )

        // Checkout 2.5 kg at 90.00 / kg = 225.00 (22500 paise)
        val lineTotal = CommerceValidation.calculateLineTotal(9000L, 2.5)
        val saleId = repository.completeBillCheckout(
            Sale(
                billNumber = "BILL-FRAC-606",
                totalAmount = lineTotal,
                paymentMode = PaymentMode.CASH.name,
                receivedAmount = lineTotal
            ),
            listOf(
                SaleItem(
                    saleId = 0L,
                    productId = productId,
                    productNameSnapshot = "Basmati Rice Loose",
                    quantity = 2.5,
                    unit = "kg",
                    unitPrice = 9000L,
                    lineTotal = lineTotal
                )
            ),
            command = command()
        )

        val prodAfterSale = repository.getProductById(productId)!!
        assertEquals(7.5, prodAfterSale.currentStock, 0.001)

        val saleItems = repository.getSaleItemsForSaleList(saleId)

        // Partial fractional return: 1.25 kg
        val returnResult = repository.processReturn(
            saleId = saleId,
            itemsToReturn = listOf(ItemReturnRequest(saleItemId = saleItems[0].id, quantityReturned = 1.25)),
            refundMode = "CASH",
            reason = "Customer returned 1.25 kg",
            command = command()
        )

        val expectedRefund = CommerceValidation.calculateLineTotal(9000L, 1.25)
        assertEquals(expectedRefund, returnResult.totalRefundAmount)
        assertEquals(PaymentState.PARTIALLY_REFUNDED, returnResult.updatedPaymentState)

        // Stock restored by 1.25 kg: 7.5 + 1.25 = 8.75 kg
        val prodAfterReturn = repository.getProductById(productId)!!
        assertEquals(8.75, prodAfterReturn.currentStock, 0.001)

        val remaining = repository.getRemainingReturnableQuantities(saleId)
        assertEquals(1.25, remaining[saleItems[0].id]!!, 0.001)
    }
}
