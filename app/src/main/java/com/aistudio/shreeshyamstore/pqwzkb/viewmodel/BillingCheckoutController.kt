package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository-facing billing boundary.
 *
 * The controller owns ephemeral cart and checkout intent state. It never writes
 * Room directly and never calculates an authoritative persisted sale outside the
 * repository transaction. ShopViewModel keeps compatibility delegates while UI
 * call sites migrate to this focused boundary.
 */
interface BillingCheckoutGateway {
    suspend fun currentCommandMetadata(): CommandMetadata
    suspend fun getCustomerByName(name: String): Customer?
    suspend fun insertSaleWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer,
        command: CommandMetadata
    ): Long
    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long?,
        command: CommandMetadata
    ): Long
    suspend fun getSaleById(id: Long): Sale?
    suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem>
    suspend fun reconcilePaymentState(
        saleId: Long,
        targetState: PaymentState,
        receivedAmount: Long,
        command: CommandMetadata
    ): Sale
}

class ShopRepositoryBillingGateway(
    private val repository: ShopRepository,
    private val commandProvider: suspend () -> CommandMetadata
) : BillingCheckoutGateway {
    override suspend fun currentCommandMetadata(): CommandMetadata = commandProvider()

    override suspend fun getCustomerByName(name: String): Customer? =
        repository.getCustomerByName(name)

    override suspend fun insertSaleWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer,
        command: CommandMetadata
    ): Long = repository.insertSaleWithNewCustomer(sale, items, newCustomer, command)

    override suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long?,
        command: CommandMetadata
    ): Long = repository.insertSaleWithItems(sale, items, selectedCustomerId, command)

    override suspend fun getSaleById(id: Long): Sale? = repository.getSaleById(id)

    override suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem> =
        repository.getSaleItemsForSaleList(saleId)

    override suspend fun reconcilePaymentState(
        saleId: Long,
        targetState: PaymentState,
        receivedAmount: Long,
        command: CommandMetadata
    ): Sale = repository.reconcilePaymentState(saleId, targetState, receivedAmount, command)
}

class BillingCheckoutController(
    private val gateway: BillingCheckoutGateway,
    private val scope: CoroutineScope,
    private val onAutoSync: () -> Unit = {},
    private val onCheckoutSuccess: () -> Unit = {}
) {
    private val billingCart = BillingCartState()
    private val _cartTotal = MutableStateFlow(0L)

    val cartState: StateFlow<Map<Product, Double>> = billingCart.items
    val cartTotal: StateFlow<Long> = _cartTotal.asStateFlow()

    private fun refreshCartTotal() {
        _cartTotal.value = billingCart.currentTotal
    }

    private val _lastSale = MutableStateFlow<Sale?>(null)
    val lastSale: StateFlow<Sale?> = _lastSale.asStateFlow()

    private val _lastSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastSaleItems: StateFlow<List<SaleItem>> = _lastSaleItems.asStateFlow()

    private val _checkoutInFlight = MutableStateFlow(false)
    val checkoutInFlight: StateFlow<Boolean> = _checkoutInFlight.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    fun addProductToCart(product: Product, quantity: Double = 1.0) {
        billingCart.add(product, quantity)
        refreshCartTotal()
    }

    fun setProductQuantityInCart(product: Product, quantity: Double) {
        billingCart.setQuantity(product, quantity)
        refreshCartTotal()
    }

    fun removeProductFromCart(product: Product) {
        billingCart.remove(product)
        refreshCartTotal()
    }

    fun clearCart() {
        billingCart.clear()
        refreshCartTotal()
    }

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    fun reconcilePaymentState(
        saleId: Long,
        targetState: PaymentState,
        receivedAmount: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            try {
                val updatedSale = gateway.reconcilePaymentState(
                    saleId = saleId,
                    targetState = targetState,
                    receivedAmount = receivedAmount,
                    command = gateway.currentCommandMetadata()
                )
                if (_lastSale.value?.id == updatedSale.id) {
                    _lastSale.value = updatedSale
                }
                onAutoSync()
                onResult(true)
            } catch (error: IllegalArgumentException) {
                _checkoutError.value = error.message ?: "Payment state could not be updated"
                onResult(false)
            } catch (_: Exception) {
                _checkoutError.value = "Payment state could not be updated"
                onResult(false)
            }
        }
    }

    fun completeBill(
        paymentMode: String,
        customerId: Long? = null,
        customerName: String = "",
        customerPhone: String = "",
        note: String? = null,
        receivedAmount: Long? = null
    ) {
        if (_checkoutInFlight.value) return
        val cartItems = billingCart.items.value
        if (cartItems.isEmpty()) return

        _checkoutInFlight.value = true
        _checkoutError.value = null
        scope.launch {
            try {
                val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
                val billNo = "BILL-${formatter.format(Date())}-${UUID.randomUUID().toString().take(8).uppercase(Locale.ENGLISH)}"
                val isUdhaar = paymentMode.trim().equals("UDHAAR", ignoreCase = true)
                val command = gateway.currentCommandMetadata()
                var newCustomer: Customer? = null
                val finalCustomerId = if (isUdhaar) {
                    if (customerId != null) {
                        customerId
                    } else {
                        val trimmedName = customerName.trim()
                        require(trimmedName.isNotEmpty()) { "Customer name is required for udhaar" }
                        val existing = gateway.getCustomerByName(trimmedName)
                        if (existing != null) {
                            existing.id
                        } else {
                            newCustomer = Customer(
                                name = trimmedName,
                                phone = customerPhone.trim().ifEmpty { null },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            null
                        }
                    }
                } else {
                    null
                }

                val now = System.currentTimeMillis()
                val saleItems = cartItems.map { (product, quantity) ->
                    val unitPrice = com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
                        .normalizeUnitPrice(product.getEffectivePrice())
                    SaleItem(
                        saleId = 0,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        quantity = quantity,
                        unit = product.unit,
                        unitPrice = unitPrice,
                        lineTotal = com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
                            .calculateLineTotal(unitPrice, quantity),
                        updatedAt = now
                    )
                }
                val sale = Sale(
                    billNumber = billNo,
                    totalAmount = com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
                        .calculateBillTotal(saleItems),
                    paymentMode = paymentMode,
                    receivedAmount = receivedAmount,
                    customerId = finalCustomerId,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )

                val savedSaleId = if (newCustomer != null) {
                    gateway.insertSaleWithNewCustomer(sale, saleItems, newCustomer!!, command)
                } else {
                    gateway.insertSaleWithItems(sale, saleItems, finalCustomerId, command)
                }
                val savedSale = gateway.getSaleById(savedSaleId)
                if (savedSale != null) {
                    _lastSale.value = savedSale
                    _lastSaleItems.value = gateway.getSaleItemsForSaleList(savedSaleId)
                }

                clearCart()
                onAutoSync()
                onCheckoutSuccess()
            } catch (error: IllegalArgumentException) {
                _checkoutError.value = when {
                    error.message?.contains("credit limit", ignoreCase = true) == true ->
                        "Udhaar credit limit exceeded. Bill was not saved."
                    error.message?.contains("stock", ignoreCase = true) == true ->
                        "Insufficient stock. Bill was not saved."
                    error.message?.contains("customer", ignoreCase = true) == true ->
                        "Valid customer details are required. Bill was not saved."
                    else -> "Bill details are invalid. Bill was not saved."
                }
            } catch (_: Exception) {
                _checkoutError.value = "Bill could not be saved. Please try again."
            } finally {
                _checkoutInFlight.value = false
            }
        }
    }
}
