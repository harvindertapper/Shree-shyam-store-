package com.harrylabs.shreeshyamstore.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.data.*
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

sealed class Screen {
    object Welcome : Screen()
    object Login : Screen()       // Auth screens
    object Register : Screen()    // Auth screens
    object Setup : Screen()
    object Home : Screen()
    object Billing : Screen()
    data class Payment(val invoiceTotal: Double) : Screen()
    object BillSuccess : Screen()
    object Products : Screen()
    data class AddEditProduct(val productUuid: String? = null) : Screen()
    object OpeningStock : Screen()
    data class StockAdjustment(val productUuid: String) : Screen()
    object Udhaar : Screen()
    data class CustomerDetail(val customerUuid: String) : Screen()
    object Reports : Screen()
    object Settings : Screen()
}

fun sha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

class ShopViewModel(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Welcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Settings State ---
    val storeSettings: StateFlow<StoreSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettings(
                shopName = "Shree Shyam General Store",
                ownerPhone = "",
                staticPaytmQrImageUri = "",
                welcomeChantEnabled = true,
                firstLaunchCompleted = false,
                loggedInUsername = "",
                loggedInEmail = "",
                isUserLoggedIn = false,
                selectedLanguage = "en"
            )
        )

    fun updateSettings(shopName: String, ownerPhone: String, welcomeChantEnabled: Boolean, qrImageUri: String) {
        viewModelScope.launch {
            settingsDataStore.updateShopName(shopName)
            settingsDataStore.updateOwnerPhone(ownerPhone)
            settingsDataStore.updateWelcomeChantEnabled(welcomeChantEnabled)
            settingsDataStore.updateStaticPaytmQrImageUri(qrImageUri)
        }
    }

    fun updateSelectedLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsDataStore.updateSelectedLanguage(languageCode)
        }
    }

    fun completeFirstLaunch() {
        viewModelScope.launch {
            settingsDataStore.setFirstLaunchCompleted(true)
            navigateTo(Screen.Home)
        }
    }

    // --- User Authentication & Session Management Functions ---
    fun registerUser(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            if (trimmedUsername.length < 3) {
                onError(R.string.error_username_min_length)
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                onError(R.string.error_valid_email)
                return@launch
            }
            if (password.length < 6) {
                onError(R.string.error_password_min_length)
                return@launch
            }

            try {
                // Check if user already exists
                val existingUser = repository.getUserByUsernameOrEmail(trimmedUsername, trimmedEmail)
                if (existingUser != null) {
                    if (existingUser.username.equals(trimmedUsername, ignoreCase = true)) {
                        onError(R.string.error_username_already_taken)
                    } else {
                        onError(R.string.error_email_already_registered)
                    }
                    return@launch
                }

                // Insert User
                val user = User(
                    username = trimmedUsername,
                    email = trimmedEmail,
                    passwordHash = sha256(password)
                )
                repository.insertUser(user)

                // Save active session
                settingsDataStore.saveSession(trimmedUsername, trimmedEmail)

                onSuccess()
            } catch (_: Exception) {
                onError(R.string.error_registration_failed)
            }
        }
    }

    fun loginUser(
        usernameOrEmail: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val key = usernameOrEmail.trim()
            if (key.isEmpty()) {
                onError(R.string.error_enter_username_or_email)
                return@launch
            }
            if (password.isEmpty()) {
                onError(R.string.error_enter_password)
                return@launch
            }

            try {
                // Check username or email matching
                val user = if (key.contains("@")) {
                    repository.getUserByEmail(key)
                } else {
                    repository.getUserByUsername(key)
                }

                if (user == null) {
                    onError(R.string.error_user_not_found)
                    return@launch
                }

                val hashedPass = sha256(password)
                if (user.passwordHash == hashedPass) {
                    // Save active session
                    settingsDataStore.saveSession(user.username, user.email)
                    onSuccess()
                } else {
                    onError(R.string.error_incorrect_password)
                }
            } catch (_: Exception) {
                onError(R.string.error_login_failed)
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            settingsDataStore.clearSession()
            navigateTo(Screen.Login)
        }
    }

    // --- Category State ---
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        viewModelScope.launch {
            if (name.trim().isNotEmpty()) {
                val existing = repository.getCategoryByName(name.trim())
                if (existing == null) {
                    repository.insertCategory(Category(name = name.trim()))
                }
            }
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            if (newName.trim().isNotEmpty()) {
                repository.updateCategory(category.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
            }
        }
    }

    // --- Product State ---
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveProduct(
        uuid: String?,
        name: String,
        categoryId: String,
        mrp: Double,
        sellingPrice: Double?,
        purchasePrice: Double?,
        currentStock: Int,
        trackStock: Boolean,
        lowStockAlertQty: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (uuid == null) {
                // Insert
                val product = Product(
                    name = name.trim(),
                    categoryId = categoryId,
                    mrp = mrp,
                    sellingPrice = sellingPrice,
                    purchasePrice = purchasePrice,
                    currentStock = currentStock,
                    trackStock = trackStock,
                    lowStockAlertQty = lowStockAlertQty,
                    isActive = isActive,
                    createdAt = now,
                    updatedAt = now
                )
                val newProductUuid = repository.insertProduct(product)
                // Log stock adjustment for opening entry
                if (trackStock && currentStock > 0) {
                    val adjustment = StockAdjustment(
                        productId = newProductUuid,
                        oldStock = 0,
                        newStock = currentStock,
                        difference = currentStock,
                        reason = "Opening stock entry",
                        createdAt = now
                    )
                    repository.insertStockAdjustment(adjustment)
                }
            } else {
                // Update
                val existing = repository.getProductById(uuid)
                if (existing != null) {
                    var finalStock = currentStock
                    if (!trackStock) {
                        finalStock = existing.currentStock // maintain old value
                    }
                    val product = existing.copy(
                        name = name.trim(),
                        categoryId = categoryId,
                        mrp = mrp,
                        sellingPrice = sellingPrice,
                        purchasePrice = purchasePrice,
                        currentStock = finalStock,
                        trackStock = trackStock,
                        lowStockAlertQty = lowStockAlertQty,
                        isActive = isActive,
                        updatedAt = now
                    )
                    repository.updateProduct(product)

                    // Log difference adjustment if stock manual update occurred
                    if (trackStock && currentStock != existing.currentStock) {
                        val diff = currentStock - existing.currentStock
                        val adjustment = StockAdjustment(
                            productId = uuid,
                            oldStock = existing.currentStock,
                            newStock = currentStock,
                            difference = diff,
                            reason = "Manual correction during edit",
                            createdAt = now
                        )
                        repository.insertStockAdjustment(adjustment)
                    }
                }
            }
        }
    }

    suspend fun getProduct(uuid: String): Product? = repository.getProductById(uuid)

    fun adjustStock(productUuid: String, actualStockCounted: Int, reason: String) {
        viewModelScope.launch {
            repository.adjustProductStock(productUuid, actualStockCounted, reason)
        }
    }

    fun getAdjustmentsForProduct(productUuid: String): Flow<List<StockAdjustment>> = repository.getAdjustmentsForProduct(productUuid)


    // --- Billing State (Cart) ---
    private val _cartState = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cartState: StateFlow<Map<Product, Int>> = _cartState.asStateFlow()

    val cartTotal: StateFlow<Double> = _cartState.map { cart ->
        cart.entries.sumOf { (product, quantity) ->
            product.getEffectivePrice() * quantity
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun addProductToCart(product: Product, quantity: Int = 1) {
        val current = _cartState.value.toMutableMap()
        val currentQty = current[product] ?: 0
        val finalQty = currentQty + quantity
        if (finalQty > 0) {
            current[product] = finalQty
        } else {
            current.remove(product)
        }
        _cartState.value = current
    }

    fun setProductQuantityInCart(product: Product, qty: Int) {
        val current = _cartState.value.toMutableMap()
        if (qty > 0) {
            current[product] = qty
        } else {
            current.remove(product)
        }
        _cartState.value = current
    }

    fun removeProductFromCart(product: Product) {
        val current = _cartState.value.toMutableMap()
        current.remove(product)
        _cartState.value = current
    }

    fun clearCart() {
        _cartState.value = emptyMap()
    }

    /**
     * Allows adding a missing item on-the-fly and automatically adding it to the cart
     */
    fun quickAddProduct(name: String, mrp: Double, categoryId: String, trackStock: Boolean, currentStock: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prod = Product(
                name = name.trim(),
                categoryId = categoryId,
                mrp = mrp,
                sellingPrice = mrp,
                currentStock = currentStock,
                trackStock = trackStock,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            val newUuid = repository.insertProduct(prod)
            val insertedProduct = prod.copy(localUuid = newUuid)

            if (trackStock && currentStock > 0) {
                repository.insertStockAdjustment(
                    StockAdjustment(
                        productId = newUuid,
                        oldStock = 0,
                        newStock = currentStock,
                        difference = currentStock,
                        reason = "Opening stock entry",
                        createdAt = now
                    )
                )
            }

            // Add the inserted product directly to our cart
            addProductToCart(insertedProduct, 1)
        }
    }


    // --- Payment & Saving State ---
    private val _lastSale = MutableStateFlow<Sale?>(null)
    val lastSale: StateFlow<Sale?> = _lastSale.asStateFlow()

    private val _lastSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastSaleItems: StateFlow<List<SaleItem>> = _lastSaleItems.asStateFlow()

    fun completeBill(
        paymentMode: String, // "CASH", "UPI", "UDHAAR"
        customerUuid: String? = null,
        customerName: String = "",
        customerPhone: String = "",
        note: String? = null
    ) {
        viewModelScope.launch {
            val total = cartTotal.value
            val cartItems = _cartState.value
            if (cartItems.isEmpty()) return@launch

            // Local bill number generation: BILL-YYYYMMDD-HHmmss
            val formatter = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ENGLISH)
            val billNo = "BILL-${formatter.format(java.util.Date())}"

            // 1. Determine Customer ID if Udhaar
            val finalCustomerUuid = if (paymentMode == "UDHAAR") {
                if (customerUuid != null) {
                    customerUuid
                } else {
                    // Create customer on the fly
                    val trimmedName = customerName.trim()
                    if (trimmedName.isEmpty()) return@launch // Required

                    val existing = repository.getCustomerByName(trimmedName)
                    if (existing != null) {
                        existing.localUuid
                    } else {
                        val newCustomer = Customer(
                            name = trimmedName,
                            phone = customerPhone.trim().ifEmpty { null },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.insertCustomer(newCustomer)
                        newCustomer.localUuid
                    }
                }
            } else {
                null
            }

            // 2. Prep entities
            val sale = Sale(
                billNumber = billNo,
                totalAmount = total,
                paymentMode = paymentMode,
                customerId = finalCustomerUuid,
                note = note,
                createdAt = System.currentTimeMillis()
            )

            val saleItems = cartItems.map { (prod, qty) ->
                SaleItem(
                    saleId = sale.localUuid, // linked using UUID string directly
                    productId = prod.localUuid, // linked using UUID string directly
                    productNameSnapshot = prod.name,
                    quantity = qty,
                    unitPrice = prod.getEffectivePrice(),
                    lineTotal = prod.getEffectivePrice() * qty
                )
            }

            // 3. Save sale, adjust quantities and logs inside the helper
            val savedSaleUuid = repository.insertSaleWithItems(sale, saleItems, finalCustomerUuid)

            // Keep record for invoice receipt visualizer
            val savedSale = repository.getSaleById(savedSaleUuid)
            if (savedSale != null) {
                _lastSale.value = savedSale
                _lastSaleItems.value = repository.getSaleItemsForSaleList(savedSaleUuid)
            }

            // 4. Wipe cart
            clearCart()
            navigateTo(Screen.BillSuccess)
        }
    }


    // --- Udhaar State ---
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allUdhaarTransactions: StateFlow<List<UdhaarTransaction>> = repository.allUdhaarTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getTransactionsForCustomer(customerUuid: String): Flow<List<UdhaarTransaction>> {
        return repository.getTransactionsForCustomer(customerUuid)
    }

    suspend fun calculateCustomerBalance(customerUuid: String): Double {
        val txs = repository.getTransactionsForCustomerList(customerUuid)
        var balance = 0.0
        for (tx in txs) {
            if (tx.type == "CREDIT") {
                balance += tx.amount
            } else if (tx.type == "PAYMENT") {
                balance -= tx.amount
            }
        }
        return balance
    }

    fun addUdhaarPayment(customerUuid: String, amount: Double, note: String?) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            val tx = UdhaarTransaction(
                customerId = customerUuid,
                type = "PAYMENT",
                amount = amount,
                note = note?.trim()?.ifEmpty { "Cash Deposit Received" } ?: "Cash Deposit Received",
                createdAt = System.currentTimeMillis()
            )
            repository.insertUdhaarTransaction(tx)
        }
    }

    fun quickAddCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isNotEmpty()) {
                val existing = repository.getCustomerByName(trimmedName)
                if (existing == null) {
                    repository.insertCustomer(
                        Customer(
                            name = trimmedName,
                            phone = phone.trim().ifEmpty { null }
                        )
                    )
                }
            }
        }
    }

    // --- Daily Sales and History (Reports) ---
    val salesHistory: StateFlow<List<Sale>> = repository.allSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getSaleItems(saleUuid: String): Flow<List<SaleItem>> = repository.getSaleItemsForSale(saleUuid)

    // Helper functions for clipboard copy text
    fun generateInvoiceText(context: Context): String {
        val sale = _lastSale.value ?: return context.getString(R.string.invoice_not_found)
        val items = _lastSaleItems.value
        val settings = storeSettings.value

        val sb = StringBuilder()
        sb.append(settings.shopName).append('\n')
        sb.append(context.getString(R.string.invoice_bill_number_format, sale.billNumber)).append('\n')
        val locale = context.resources.configuration.locales[0]
        val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
        sb.append(context.getString(R.string.invoice_date_format, df.format(java.util.Date(sale.createdAt)))).append('\n')
        sb.append("----------------------------\n")
        for (itm in items) {
            sb.append(
                context.getString(
                    R.string.invoice_line_item_format,
                    itm.productNameSnapshot,
                    itm.quantity,
                    CurrencyUtils.formatRupees(itm.unitPrice),
                    CurrencyUtils.formatRupees(itm.lineTotal)
                )
            ).append('\n')
        }
        sb.append("----------------------------\n")
        sb.append(
            context.getString(
                R.string.invoice_total_amount_format,
                CurrencyUtils.formatRupees(sale.totalAmount)
            )
        ).append('\n')
        val paymentMode = when (sale.paymentMode.uppercase()) {
            "CASH" -> context.getString(R.string.payment_mode_cash)
            "UPI" -> context.getString(R.string.payment_mode_upi)
            "CREDIT" -> context.getString(R.string.payment_mode_credit)
            else -> sale.paymentMode
        }
        sb.append(context.getString(R.string.invoice_payment_mode_format, paymentMode)).append('\n')
        sb.append("----------------------------\n")
        sb.append(context.getString(R.string.invoice_closing_line)).append('\n')
        sb.append(context.getString(R.string.invoice_thank_you))

        return sb.toString()
    }

    fun copyInvoiceToClipboard(context: Context) {
        val txt = generateInvoiceText(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(context.getString(R.string.invoice_clipboard_label), txt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.invoice_copied_toast), Toast.LENGTH_SHORT).show()
    }
}

class ShopViewModelFactory(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
