package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Local billing cart boundary.
 *
 * This class owns only ephemeral cart state. It deliberately does not persist
 * financial records or calculate an authoritative sale: checkout remains inside
 * the repository/Room transaction boundary. ShopViewModel temporarily exposes
 * compatibility delegates while billing screens migrate to this boundary.
 */
class BillingCartState {
    private val _items = MutableStateFlow<Map<Product, Double>>(emptyMap())

    val items: StateFlow<Map<Product, Double>> = _items.asStateFlow()

    val total: Flow<Long> = _items.map { cart ->
        cart.entries.sumOf { (product, quantity) ->
            CommerceValidation.calculateLineTotal(product.getEffectivePrice(), quantity)
        }
    }

    fun add(product: Product, quantity: Double = 1.0) {
        if (!quantity.isFinite()) return
        val current = _items.value.toMutableMap()
        val currentQuantity = current[product] ?: 0.0
        val finalQuantity = currentQuantity + quantity
        if (!finalQuantity.isFinite()) return

        if (finalQuantity > 0.0) {
            if (product.trackStock && finalQuantity > product.currentStock) return
            current[product] = finalQuantity
        } else {
            current.remove(product)
        }
        _items.value = current
    }

    fun setQuantity(product: Product, quantity: Double) {
        if (!quantity.isFinite()) return
        val current = _items.value.toMutableMap()
        if (quantity > 0.0) {
            if (product.trackStock && quantity > product.currentStock) return
            current[product] = quantity
        } else {
            current.remove(product)
        }
        _items.value = current
    }

    fun remove(product: Product) {
        val current = _items.value.toMutableMap()
        current.remove(product)
        _items.value = current
    }

    fun clear() {
        _items.value = emptyMap()
    }
}
