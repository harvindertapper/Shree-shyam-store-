package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.BillingCheckoutController
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.BillingCheckoutGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BillingCheckoutControllerTest {
    @Test
    fun duplicateSubmitCannotInvokeCheckoutGatewayTwice() = runTest {
        val gateway = FakeBillingGateway()
        val completionGate = CompletableDeferred<Unit>()
        gateway.insertGate = completionGate
        var autoSyncObserved = false
        var successObserved = false
        val controller = BillingCheckoutController(
            gateway = gateway,
            scope = this,
            onAutoSync = { autoSyncObserved = true },
            onCheckoutSuccess = { successObserved = true }
        )
        controller.addProductToCart(product())

        controller.completeBill(paymentMode = "CASH", receivedAmount = 1000L)
        runCurrent()
        controller.completeBill(paymentMode = "CASH", receivedAmount = 1000L)

        assertTrue(controller.checkoutInFlight.value)
        assertEquals(1, gateway.insertCalls)

        completionGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, gateway.insertCalls)
        assertTrue(controller.cartState.value.isEmpty())
        assertEquals(41L, controller.lastSale.value?.id)
        assertTrue(autoSyncObserved)
        assertTrue(successObserved)
    }

    @Test
    fun failedCheckoutPreservesCartAndPublishesSafeError() = runTest {
        val gateway = FakeBillingGateway()
        gateway.insertFailure = IllegalArgumentException("stock underflow")
        val controller = BillingCheckoutController(gateway, this)
        val cartProduct = product()
        controller.addProductToCart(cartProduct)

        controller.completeBill(paymentMode = "CASH", receivedAmount = 1000L)
        advanceUntilIdle()

        assertFalse(controller.checkoutInFlight.value)
        assertEquals(1.0, controller.cartState.value[cartProduct] ?: -1.0, 0.0)
        assertEquals("Insufficient stock. Bill was not saved.", controller.checkoutError.value)
        assertFalse(gateway.successObserved)
    }

    @Test
    fun udhaarWithoutCustomerFailsBeforeRepositoryWrite() = runTest {
        val gateway = FakeBillingGateway()
        val controller = BillingCheckoutController(gateway, this)
        controller.addProductToCart(product())

        controller.completeBill(paymentMode = "UDHAAR", receivedAmount = null)
        advanceUntilIdle()

        assertEquals(0, gateway.insertCalls)
        assertEquals("Valid customer details are required. Bill was not saved.", controller.checkoutError.value)
        assertEquals(1.0, controller.cartState.value.values.single(), 0.0)
    }

    private fun product() = Product(
        name = "Rice",
        categoryId = 1L,
        mrp = 1000L,
        sellingPrice = 1000L,
        currentStock = 5.0,
        trackStock = true
    )

    private class FakeBillingGateway : BillingCheckoutGateway {
        var insertCalls = 0
        var insertGate: CompletableDeferred<Unit>? = null
        var insertFailure: IllegalArgumentException? = null
        private val savedSale = Sale(
            id = 41L,
            billNumber = "BILL-TEST",
            totalAmount = 1000L,
            paymentMode = "CASH",
            paymentState = PaymentState.RECEIVED.wireValue,
            receivedAmount = 1000L
        )

        override suspend fun currentCommandMetadata(): CommandMetadata = CommandMetadata(
            idempotencyKey = UUID.randomUUID().toString(),
            clientEventId = UUID.randomUUID().toString(),
            tenant = TenantScope("org", "store", "membership", "device", "installation"),
            actor = PlatformActor("actor", "Test Owner", "OWNER", "device"),
            clientCreatedAt = 1L
        )

        override suspend fun getCustomerByName(name: String): Customer? = null

        override suspend fun insertSaleWithNewCustomer(
            sale: Sale,
            items: List<SaleItem>,
            newCustomer: Customer,
            command: CommandMetadata
        ): Long = insertSaleWithItems(sale, items, null, command)

        override suspend fun insertSaleWithItems(
            sale: Sale,
            items: List<SaleItem>,
            selectedCustomerId: Long?,
            command: CommandMetadata
        ): Long {
            insertCalls += 1
            insertFailure?.let { throw it }
            insertGate?.await()
            return savedSale.id
        }

        override suspend fun getSaleById(id: Long): Sale? = savedSale

        override suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem> = emptyList()

        override suspend fun reconcilePaymentState(
            saleId: Long,
            targetState: PaymentState,
            receivedAmount: Long,
            command: CommandMetadata
        ): Sale = savedSale.copy(paymentState = targetState.wireValue, receivedAmount = receivedAmount)
    }
}
