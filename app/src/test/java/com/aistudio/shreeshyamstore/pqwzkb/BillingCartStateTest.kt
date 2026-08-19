package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.BillingCartState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCartStateTest {
    @Test
    fun trackedProductCannotExceedAvailableStock() {
        val cart = BillingCartState(thisScope())
        val product = product(stock = 2.0)

        cart.add(product, quantity = 3.0)

        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun addingAndRemovingQuantityPreservesExpectedCartState() {
        val cart = BillingCartState(thisScope())
        val product = product(stock = 5.0)

        cart.add(product, quantity = 2.0)
        cart.add(product, quantity = -1.0)

        assertEquals(1.0, cart.items.value[product] ?: -1.0, 0.0)

        cart.setQuantity(product, 0.0)

        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun invalidQuantitiesDoNotMutateCart() {
        val cart = BillingCartState(thisScope())
        val product = product(stock = 5.0)

        cart.add(product, quantity = Double.NaN)
        cart.setQuantity(product, Double.POSITIVE_INFINITY)

        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun totalUsesIntegerPaiseAndCommerceRounding() = runTest {
        val cart = BillingCartState(this)
        val product = product(stock = 5.0, sellingPrice = 1235L)

        cart.add(product, quantity = 2.0)

        assertEquals(2470L, cart.total.drop(1).first())
    }

    @Test
    fun untrackedProductDoesNotUseStockAsCartLimit() {
        val cart = BillingCartState(thisScope())
        val product = product(stock = 0.0, trackStock = false)

        cart.add(product, quantity = 10.0)

        assertEquals(10.0, cart.items.value[product] ?: -1.0, 0.0)
    }

    private fun product(
        stock: Double,
        sellingPrice: Long = 1000L,
        trackStock: Boolean = true
    ) = Product(
        name = "Test Product",
        categoryId = 1L,
        mrp = sellingPrice,
        sellingPrice = sellingPrice,
        currentStock = stock,
        trackStock = trackStock
    )

    private fun thisScope() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
}
