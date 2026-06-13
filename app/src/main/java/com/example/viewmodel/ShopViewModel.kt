package com.example.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.CurrencyUtils
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
    data class AddEditProduct(val productId: Long? = null) : Screen()
    object OpeningStock : Screen()
    data class StockAdjustment(val productId: Long) : Screen()
    object Udhaar : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
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
                isUserLoggedIn = false
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
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            if (trimmedUsername.length < 3) {
                onError("Username must be at least 3 characters!")
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                onError("Please enter a valid email address!")
                return@launch
            }
            if (password.length < 6) {
                onError("Password must be at least 6 characters!")
                return@launch
            }

            try {
                // Check if user already exists
                val existingUser = repository.getUserByUsernameOrEmail(trimmedUsername, trimmedEmail)
                if (existingUser != null) {
                    if (existingUser.username.equals(trimmedUsername, ignoreCase = true)) {
                        onError("Username is already taken!")
                    } else {
                        onError("Email is already registered!")
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
            } catch (e: Exception) {
                onError("Registration failed: ${e.message}")
            }
        }
    }

    fun loginUser(
        usernameOrEmail: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val key = usernameOrEmail.trim()
            if (key.isEmpty()) {
                onError("Please enter Username or Email!")
                return@launch
            }
            if (password.isEmpty()) {
                onError("Please type your Password!")
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
                    onError("User not found!")
                    return@launch
                }

                val hashedPass = sha256(password)
                if (user.passwordHash == hashedPass) {
                    // Save active session
                    settingsDataStore.saveSession(user.username, user.email)
                    onSuccess()
                } else {
                    onError("Incorrect password! Change and retry.")
                }
            } catch (e: Exception) {
                onError("Login failed: ${e.message}")
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
        id: Long,
        name: String,
        categoryId: Long,
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
            if (id == 0L) {
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
                val newProductId = repository.insertProduct(product)
                // Log stock adjustment for opening entry
                if (trackStock && currentStock > 0) {
                    val adjustment = StockAdjustment(
                        productId = newProductId,
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
                val existing = repository.getProductById(id)
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
                            productId = id,
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

    suspend fun getProduct(id: Long): Product? = repository.getProductById(id)

    fun adjustStock(productId: Long, actualStockCounted: Int, reason: String) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, actualStockCounted, reason)
        }
    }

    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> = repository.getAdjustmentsForProduct(productId)


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
    fun quickAddProduct(name: String, mrp: Double, categoryId: Long, trackStock: Boolean, currentStock: Int) {
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
            val newId = repository.insertProduct(prod)
            val insertedProduct = prod.copy(id = newId)

            if (trackStock && currentStock > 0) {
                repository.insertStockAdjustment(
                    StockAdjustment(
                        productId = newId,
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
        customerId: Long? = null,
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
            val finalCustomerId = if (paymentMode == "UDHAAR") {
                if (customerId != null) {
                    customerId
                } else {
                    // Create customer on the fly
                    val trimmedName = customerName.trim()
                    if (trimmedName.isEmpty()) return@launch // Required

                    val existing = repository.getCustomerByName(trimmedName)
                    if (existing != null) {
                        existing.id
                    } else {
                        repository.insertCustomer(
                            Customer(
                                name = trimmedName,
                                phone = customerPhone.trim().ifEmpty { null },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
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
                customerId = finalCustomerId,
                note = note,
                createdAt = System.currentTimeMillis()
            )

            val saleItems = cartItems.map { (prod, qty) ->
                SaleItem(
                    saleId = 0, // setup in repository
                    productId = prod.id,
                    productNameSnapshot = prod.name,
                    quantity = qty,
                    unitPrice = prod.getEffectivePrice(),
                    lineTotal = prod.getEffectivePrice() * qty
                )
            }

            // 3. Save sale, adjust quantities and logs inside the helper
            val savedSaleId = repository.insertSaleWithItems(sale, saleItems, finalCustomerId)

            // Keep record for invoice receipt visualizer
            val savedSale = repository.getSaleById(savedSaleId)
            if (savedSale != null) {
                _lastSale.value = savedSale
                _lastSaleItems.value = repository.getSaleItemsForSaleList(savedSaleId)
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

    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>> {
        return repository.getTransactionsForCustomer(customerId)
    }

    suspend fun calculateCustomerBalance(customerId: Long): Double {
        val txs = repository.getTransactionsForCustomerList(customerId)
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

    fun addUdhaarPayment(customerId: Long, amount: Double, note: String?) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            val tx = UdhaarTransaction(
                customerId = customerId,
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

    fun getSaleItems(saleId: Long): Flow<List<SaleItem>> = repository.getSaleItemsForSale(saleId)

    // Helper functions for clipboard copy text
    fun generateInvoiceText(): String {
        val sale = _lastSale.value ?: return "No Invoice Found"
        val items = _lastSaleItems.value
        val settings = storeSettings.value

        val sb = StringBuilder()
        sb.append("🚩 ${settings.shopName}\n")
        sb.append("Bill No: ${sale.billNumber}\n")
        val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH)
        sb.append("Date: ${df.format(java.util.Date(sale.createdAt))}\n")
        sb.append("----------------------------\n")
        for (itm in items) {
            sb.append("${itm.productNameSnapshot}\n")
            sb.append("  ${itm.quantity} x ${CurrencyUtils.formatRupees(itm.unitPrice)} = ${CurrencyUtils.formatRupees(itm.lineTotal)}\n")
        }
        sb.append("----------------------------\n")
        sb.append("Total Amount: ${CurrencyUtils.formatRupees(sale.totalAmount)}\n")
        sb.append("Payment Mode: ${sale.paymentMode}\n")
        sb.append("----------------------------\n")
        sb.append("Jai Shree Shyam 🙏\n")
        sb.append("Thank you! Visit Again.")

        return sb.toString()
    }

    fun copyInvoiceToClipboard(context: Context) {
        val txt = generateInvoiceText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Store Bill", txt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Bill Copied to Clipboard!", Toast.LENGTH_SHORT).show()
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
