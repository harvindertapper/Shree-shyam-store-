package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerActor
import com.aistudio.shreeshyamstore.pqwzkb.data.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.PinUnlockResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class Screen {
    object Welcome : Screen()
    object Login : Screen()       // Auth screens
    object Register : Screen()    // Auth screens
    object Setup : Screen()
    object Home : Screen()
    object Billing : Screen()
    data class Payment(val invoiceTotal: Long) : Screen()
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
    private val settingsDataStore: SettingsDataStore,
    private val context: Context? = null
) : ViewModel() {

    private suspend fun currentLedgerActor(): LedgerActor {
        val session = reconcileIdentitySession()
            ?: error("Authenticated actor is required")
        val settings = settingsDataStore.settingsFlow.first()
        val deviceId = settings.auditDeviceId.trim()
            .ifEmpty { settingsDataStore.getOrCreateAuditDeviceId() }
        return LedgerActor(
            actorUid = session.uid,
            actorName = session.username.ifBlank { session.email }.ifBlank { session.uid },
            actorRole = session.role,
            actorDeviceId = deviceId
        ).normalized()
    }

    /**
     * Reconciles one explicit local session authority with Firebase state.
     * Local sessions remain usable offline; Firebase sessions are accepted only
     * when Firebase exposes the same authenticated account.
     */
    suspend fun reconcileIdentitySession(): IdentitySession? {
        val settings = settingsDataStore.settingsFlow.first()
        val persisted = settings.identitySessionOrNull()
        val firebaseUser = com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.currentUser

        if (persisted?.provider == IdentityProvider.LOCAL) {
            return persisted
        }

        if (firebaseUser != null) {
            val firebaseSession = IdentitySession(
                provider = IdentityProvider.FIREBASE,
                uid = firebaseUser.uid,
                username = firebaseUser.displayName.orEmpty()
                    .ifBlank { firebaseUser.email?.substringBefore("@").orEmpty() },
                email = firebaseUser.email.orEmpty(),
                role = persisted?.role ?: "OWNER"
            ).normalized()
            if (firebaseSession.isUsable() &&
                (persisted?.provider != IdentityProvider.FIREBASE || persisted.uid != firebaseSession.uid)
            ) {
                settingsDataStore.saveSession(firebaseSession)
            }
            return firebaseSession.takeIf { it.isUsable() }
        }

        if (persisted?.provider == IdentityProvider.FIREBASE) {
            settingsDataStore.clearSession()
            return null
        }

        if (settings.isUserLoggedIn && (settings.loggedInEmail.isNotBlank() || settings.loggedInUsername.isNotBlank())) {
            val localSession = IdentitySession.localForUser(
                username = settings.loggedInUsername,
                email = settings.loggedInEmail,
                existingUid = settings.loggedInUid
            )
            if (localSession.isUsable()) {
                settingsDataStore.saveSession(localSession)
                return localSession
            }
        }
        return null
    }

    private suspend fun clearFirebaseAuthorityForLocalSession() {
        context?.let { com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.signOut(it) }
    }

    fun triggerAutoSync() {
        context?.let { ctx ->
            try {
                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncManager.scheduleInstantSync(ctx)
            } catch (e: Exception) {
                // Ignore sync scheduling if running in unit test without context
            }
        }
    }

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
                shopName = "",
                ownerName = "",
                ownerPhone = "",
                staticPaytmQrImageUri = "",
                welcomeChantEnabled = true,
                firstLaunchCompleted = false,
                loggedInUid = "",
                loggedInUsername = "",
                loggedInEmail = "",
                isUserLoggedIn = false,
                appLockEnabled = true,
                biometricEnabled = false,
                securityPin = SecurityUtils.hashPin(SecurityUtils.DEFAULT_PIN),
                firebaseUrl = "",
                firebasePrefix = "shreeshyam_sync",
                lastSyncTime = "Never Synced",
                autoSyncEnabled = false,
                appLanguage = com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage.HINDI
            )
        )

    fun setLanguage(language: com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.updateAppLanguage(language)
        }
    }

    fun updateSettings(shopName: String, ownerPhone: String, welcomeChantEnabled: Boolean, qrImageUri: String, securityPin: String = "1234") {
        viewModelScope.launch {
            settingsDataStore.updateShopName(shopName)
            settingsDataStore.updateOwnerPhone(ownerPhone)
            settingsDataStore.updateWelcomeChantEnabled(welcomeChantEnabled)
            settingsDataStore.updateStaticPaytmQrImageUri(qrImageUri)
            settingsDataStore.updateSecurityPin(securityPin)
            settingsDataStore.updateAppLockState(AppLockPolicy.recordSuccess(System.currentTimeMillis()))
        }
    }

    fun updateSecurityPin(pin: String) {
        viewModelScope.launch {
            settingsDataStore.updateSecurityPin(pin)
            settingsDataStore.updateAppLockState(AppLockPolicy.recordSuccess(System.currentTimeMillis()))
        }
    }

    fun verifyAppLockPin(pin: String, onResult: (PinUnlockResult) -> Unit) {
        viewModelScope.launch {
            onResult(settingsDataStore.evaluateAppLockPin(pin, System.currentTimeMillis()))
        }
    }

    fun recordSuccessfulAppUnlock() {
        viewModelScope.launch {
            settingsDataStore.updateAppLockState(AppLockPolicy.recordSuccess(System.currentTimeMillis()))
        }
    }

    fun isBiometricAvailable(): Boolean {
        val appContext = context ?: return false
        return BiometricManager.from(appContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun sessionRequiresUnlock(nowEpochMs: Long = System.currentTimeMillis()): Boolean =
        AppLockPolicy.sessionExpired(storeSettings.value.lastUnlockAtEpochMs, nowEpochMs)

    fun completeFirstLaunch() {
        viewModelScope.launch {
            settingsDataStore.setFirstLaunchCompleted(true)
            navigateTo(Screen.Home)
        }
    }

    // --- User Authentication & Session Management Functions ---
    fun onGoogleSignInSuccess(
        uid: String,
        email: String,
        displayName: String,
        onSuccess: (isFirstTime: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedUid = uid.trim()
            val normalizedEmail = email.trim().lowercase()
            if (normalizedUid.isEmpty() || normalizedEmail.isEmpty()) {
                onError("Google account did not provide a stable identity.")
                return@launch
            }
            val username = displayName.ifBlank { normalizedEmail.substringBefore("@") }
            val existingUser = repository.getUserByEmail(normalizedEmail)
            if (existingUser != null &&
                existingUser.passwordHash.isNotBlank() &&
                existingUser.uid.trim() != normalizedUid
            ) {
                onError("This email already belongs to a local account. Sign in locally or complete account linking first.")
                return@launch
            }
            if (existingUser == null) {
                val newUser = User(
                    uid = normalizedUid,
                    username = username,
                    email = normalizedEmail,
                    passwordHash = ""
                )
                repository.insertUser(newUser)
            }
            settingsDataStore.saveSession(
                IdentitySession(
                    provider = IdentityProvider.FIREBASE,
                    uid = normalizedUid,
                    username = username,
                    email = normalizedEmail
                )
            )
            val settings = settingsDataStore.settingsFlow.first()
            onSuccess(!settings.firstLaunchCompleted)
        }
    }

    fun saveShopProfile(
        shopName: String,
        ownerName: String,
        ownerPhone: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            settingsDataStore.updateShopName(shopName.trim())
            settingsDataStore.updateOwnerName(ownerName.trim())
            settingsDataStore.updateOwnerPhone(ownerPhone.trim())
            val session = reconcileIdentitySession()
            if (session != null) {
                repository.saveShopProfile(
                    ShopProfile(
                        uid = session.shopUid,
                        shopName = shopName.trim(),
                        ownerName = ownerName.trim(),
                        ownerPhone = ownerPhone.trim(),
                        email = session.email,
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            settingsDataStore.setFirstLaunchCompleted(true)
            triggerAutoSync()
            onSuccess()
        }
    }

    fun setAppLockPin(pin: String, enableBiometric: Boolean = false) {
        viewModelScope.launch {
            settingsDataStore.updateSecurityPin(pin)
            settingsDataStore.updateAppLockEnabled(true)
            settingsDataStore.updateBiometricEnabled(enableBiometric && isBiometricAvailable())
            settingsDataStore.updateAppLockState(AppLockPolicy.recordSuccess(System.currentTimeMillis()))
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateBiometricEnabled(enabled && isBiometricAvailable())
        }
    }

    fun sendForgotPinEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                onError("Please enter a valid registered email address!")
                return@launch
            }
            val result = com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.sendPasswordResetEmail(email.trim(), context)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset email. Verify Firebase configuration.")
            }
        }
    }

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

                val localSession = IdentitySession.localForUser(trimmedUsername, trimmedEmail)
                // Insert a device-local projection with a stable local identity.
                val user = User(
                    uid = localSession.uid,
                    username = trimmedUsername,
                    email = trimmedEmail,
                    passwordHash = sha256(password)
                )
                repository.insertUser(user)

                // Local and Firebase authorities must not remain active together.
                clearFirebaseAuthorityForLocalSession()
                settingsDataStore.saveSession(localSession)

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
                    val localSession = IdentitySession.localForUser(
                        username = user.username,
                        email = user.email,
                        existingUid = user.uid
                    )
                    clearFirebaseAuthorityForLocalSession()
                    settingsDataStore.saveSession(localSession)
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
            context?.let { ctx ->
                com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.signOut(ctx)
            }
            settingsDataStore.clearSession()
            navigateTo(Screen.Login)
        }
    }

    // --- Billing State (Cart) ---
    private val _cartState = MutableStateFlow<Map<Product, Double>>(emptyMap())
    val cartState: StateFlow<Map<Product, Double>> = _cartState.asStateFlow()

    val cartTotal: StateFlow<Long> = _cartState.map { cart ->
        cart.entries.sumOf { (product, quantity) ->
            CommerceValidation.calculateLineTotal(product.getEffectivePrice(), quantity)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun addProductToCart(product: Product, quantity: Double = 1.0) {
        if (!quantity.isFinite()) return
        val current = _cartState.value.toMutableMap()
        val currentQty = current[product] ?: 0.0
        val finalQty = currentQty + quantity
        if (!finalQty.isFinite()) return
        if (finalQty > 0.0) {
            if (product.trackStock && finalQty > product.currentStock) return
            current[product] = finalQty
        } else {
            current.remove(product)
        }
        _cartState.value = current
    }

    fun setProductQuantityInCart(product: Product, qty: Double) {
        if (!qty.isFinite()) return
        val current = _cartState.value.toMutableMap()
        if (qty > 0.0) {
            if (product.trackStock && qty > product.currentStock) return
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
    fun quickAddProduct(name: String, mrp: Long, categoryId: Long, trackStock: Boolean, currentStock: Double, unit: String = "pcs", barcode: String = "") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prod = Product(
                name = name.trim(),
                categoryId = categoryId,
                mrp = mrp,
                sellingPrice = mrp,
                currentStock = currentStock,
                unit = unit.trim().ifEmpty { "pcs" },
                trackStock = trackStock,
                barcode = barcode.trim(),
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            val newId = repository.insertProduct(prod)
            val insertedProduct = prod.copy(id = newId)

            if (trackStock && currentStock > 0.0) {
                repository.insertStockAdjustment(
                    StockAdjustment(
                        productId = newId,
                        oldStock = 0.0,
                        newStock = currentStock,
                        difference = currentStock,
                        reason = "Opening stock entry",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            // Add the inserted product directly to our cart
            addProductToCart(insertedProduct, 1.0)
            triggerAutoSync()
        }
    }


    // --- Payment & Saving State ---
    private val _lastSale = MutableStateFlow<Sale?>(null)
    val lastSale: StateFlow<Sale?> = _lastSale.asStateFlow()

    private val _lastSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastSaleItems: StateFlow<List<SaleItem>> = _lastSaleItems.asStateFlow()

    private val _checkoutInFlight = MutableStateFlow(false)
    val checkoutInFlight: StateFlow<Boolean> = _checkoutInFlight.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    fun completeBill(
        paymentMode: String, // "CASH", "UPI", "UDHAAR"
        customerId: Long? = null,
        customerName: String = "",
        customerPhone: String = "",
        note: String? = null
    ) {
        if (_checkoutInFlight.value) return
        val cartItems = _cartState.value
        if (cartItems.isEmpty()) return

        _checkoutInFlight.value = true
        _checkoutError.value = null
        viewModelScope.launch {
            try {
                val total = cartTotal.value
                val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
                val billNo = "BILL-${formatter.format(Date())}-${UUID.randomUUID().toString().take(8).uppercase(Locale.ENGLISH)}"

                val isUdhaar = paymentMode.trim().equals("UDHAAR", ignoreCase = true)
                val ledgerActor = if (isUdhaar) currentLedgerActor() else null
                var newCustomer: Customer? = null
                val finalCustomerId = if (isUdhaar) {
                    if (customerId != null) {
                        customerId
                    } else {
                        val trimmedName = customerName.trim()
                        require(trimmedName.isNotEmpty()) { "Customer name is required for udhaar" }
                        val existing = repository.getCustomerByName(trimmedName)
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
                val saleItems = cartItems.map { (prod, qty) ->
                    val unitPrice = CommerceValidation.normalizeUnitPrice(prod.getEffectivePrice())
                    SaleItem(
                        saleId = 0,
                        productId = prod.id,
                        productNameSnapshot = prod.name,
                        quantity = qty,
                        unit = prod.unit,
                        unitPrice = unitPrice,
                        lineTotal = CommerceValidation.calculateLineTotal(unitPrice, qty),
                        updatedAt = now
                    )
                }
                val sale = Sale(
                    billNumber = billNo,
                    totalAmount = CommerceValidation.calculateBillTotal(saleItems),
                    paymentMode = paymentMode,
                    customerId = finalCustomerId,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )

                val savedSaleId = if (newCustomer != null) {
                    repository.insertSaleWithNewCustomer(sale, saleItems, newCustomer, ledgerActor)
                } else {
                    repository.insertSaleWithItems(sale, saleItems, finalCustomerId, ledgerActor)
                }
                val savedSale = repository.getSaleById(savedSaleId)
                if (savedSale != null) {
                    _lastSale.value = savedSale
                    _lastSaleItems.value = repository.getSaleItemsForSaleList(savedSaleId)
                }

                clearCart()
                triggerAutoSync()
                navigateTo(Screen.BillSuccess)
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

    val totalUdhaarAmount: StateFlow<Long> = repository.getTotalUdhaarFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>> {
        return repository.getTransactionsForCustomer(customerId)
    }

    fun getCustomerBalanceFlow(customerId: Long): Flow<Long> {
        return repository.getCustomerBalanceFlow(customerId)
    }

    suspend fun calculateCustomerBalance(customerId: Long): Long {
        return repository.getCustomerBalance(customerId)
    }

    fun addUdhaarPayment(customerId: Long, amountMinorUnits: Long, note: String?) {
        viewModelScope.launch {
            try {
                repository.recordUdhaarPayment(
                    customerId = customerId,
                    amountMinorUnits = amountMinorUnits,
                    note = note,
                    actor = currentLedgerActor()
                )
                triggerAutoSync()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(
                    context,
                    error.message ?: "Payment was not saved",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Payment could not be saved", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun reverseUdhaarTransaction(customerId: Long, eventId: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.reverseUdhaarTransaction(
                    customerId = customerId,
                    eventId = eventId,
                    reason = reason,
                    actor = currentLedgerActor()
                )
                triggerAutoSync()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(context, error.message ?: "Ledger reversal was not saved", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Ledger reversal could not be saved", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun correctUdhaarTransaction(
        customerId: Long,
        eventId: String,
        correctedAmountMinorUnits: Long,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                repository.correctUdhaarTransaction(
                    customerId = customerId,
                    eventId = eventId,
                    correctedAmountMinorUnits = correctedAmountMinorUnits,
                    reason = reason,
                    actor = currentLedgerActor()
                )
                triggerAutoSync()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(context, error.message ?: "Ledger correction was not saved", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Ledger correction could not be saved", Toast.LENGTH_LONG).show()
            }
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
                    triggerAutoSync()
                }
            }
        }
    }

    // Helper functions for clipboard copy text and sharing
    fun generateInvoiceText(customSale: Sale? = null, customItems: List<SaleItem>? = null): String {
        val sale = customSale ?: _lastSale.value ?: return "No Invoice Found"
        val items = customItems ?: _lastSaleItems.value
        val settings = storeSettings.value
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper.getStrings(settings.appLanguage)
        val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
        val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH)

        return com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.generateBillReceiptText(
            shopName = shopDisplayName,
            billNumber = sale.billNumber,
            dateFormatted = df.format(java.util.Date(sale.createdAt)),
            items = items,
            totalAmount = sale.totalAmount,
            paymentMode = sale.paymentMode,
            ownerPhone = settings.ownerPhone.ifEmpty { null },
            ownerName = settings.ownerName.ifEmpty { null }
        )
    }

    fun copyInvoiceToClipboard(context: Context, customSale: Sale? = null, customItems: List<SaleItem>? = null) {
        val txt = generateInvoiceText(customSale, customItems)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Store Bill", txt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Bill Copied to Clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareInvoiceViaWhatsApp(context: Context, customSale: Sale? = null, customItems: List<SaleItem>? = null, phoneNumber: String? = null) {
        val txt = generateInvoiceText(customSale, customItems)
        com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.shareText(
            context = context,
            text = txt,
            title = "Share Bill on WhatsApp",
            phoneNumber = phoneNumber
        )
    }

    fun sendUdhaarReminder(context: Context, customer: Customer, balance: Long) {
        val settings = storeSettings.value
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper.getStrings(settings.appLanguage)
        val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
        val msg = com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.generateUdhaarReminderText(
            shopName = shopDisplayName,
            customerName = customer.name,
            balance = balance,
            ownerPhone = settings.ownerPhone.ifEmpty { null },
            ownerName = settings.ownerName.ifEmpty { null }
        )
        com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.shareText(
            context = context,
            text = msg,
            title = "Send Udhaar Reminder",
            phoneNumber = customer.phone
        )
    }

    fun exportStockCsv(
        context: Context,
        products: List<Product>,
        categories: List<Category>
    ) {
        val settings = storeSettings.value
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper.getStrings(settings.appLanguage)
        val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
        val catMap = categories.associate { it.id to it.name }
        com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.exportStockCsv(
            context = context,
            products = products,
            categoryNameMap = catMap,
            shopName = shopDisplayName
        )
    }

    fun exportUdhaarCsv(context: Context, debtorCustomers: List<Customer>, balances: Map<Long, Long>) {
        val settings = storeSettings.value
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper.getStrings(settings.appLanguage)
        val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
        com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.exportUdhaarCsv(
            context = context,
            customers = debtorCustomers,
            balances = balances,
            shopName = shopDisplayName
        )
    }

    fun generateReorderText(lowStockList: List<Product>, categories: List<Category>): String {
        val settings = storeSettings.value
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper.getStrings(settings.appLanguage)
        val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
        val catMap = categories.associate { it.id to it.name }
        return com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.generateReorderListText(
            shopName = shopDisplayName,
            lowStockItems = lowStockList,
            categoryNameMap = catMap
        )
    }

    fun shareReorderListViaWhatsApp(
        context: Context,
        lowStockList: List<Product>,
        categories: List<Category>,
        wholesalerPhone: String? = null
    ) {
        val text = generateReorderText(lowStockList, categories)
        com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils.shareText(
            context = context,
            text = text,
            title = "Share Re-order List (ऑर्डर लिस्ट)",
            phoneNumber = wholesalerPhone
        )
    }

    fun copyReorderListToClipboard(
        context: Context,
        lowStockList: List<Product>,
        categories: List<Category>
    ) {
        val text = generateReorderText(lowStockList, categories)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Re-order List", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Re-order List Copied (ऑर्डर लिस्ट कॉपी हो गई) 📋", Toast.LENGTH_SHORT).show()
    }

    fun bulkRestockProduct(product: Product, quantityToAdd: Double) {
        viewModelScope.launch {
            if (quantityToAdd <= 0.0) return@launch
            val updated = product.copy(
                currentStock = product.currentStock + quantityToAdd,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            repository.updateProduct(updated)
            repository.insertStockAdjustment(
                StockAdjustment(
                    productId = product.id,
                    oldStock = product.currentStock,
                    newStock = updated.currentStock,
                    difference = quantityToAdd,
                    reason = "Bulk Wholesale Restock",
                    createdAt = System.currentTimeMillis()
                )
            )
            triggerAutoSync()
        }
    }

    // --- Cloud Synchronization State ---
    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private fun hashStringSHA256(input: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexString = java.lang.StringBuilder()
            for (b in hash) {
                val hex = java.lang.Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            input.replace(Regex("[.\\s$#\\[\\]/]"), "_")
        }
    }

    fun updateFirebaseSettings(url: String, prefix: String, autoSync: Boolean) {
        viewModelScope.launch {
            val current = settingsDataStore.settingsFlow.first()
            val effectiveUrl = url.trim().ifEmpty { current.firebaseUrl }
            val effectivePrefix = prefix.trim().ifEmpty { current.firebasePrefix }
            settingsDataStore.updateFirebaseConfig(effectiveUrl, effectivePrefix)
            settingsDataStore.updateAutoSyncEnabled(autoSync)
        }
    }

    fun syncAllToCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val url = settings.firebaseUrl.ifBlank { com.aistudio.shreeshyamstore.pqwzkb.BuildConfig.FIREBASE_URL }
            val identitySession = reconcileIdentitySession()
            if (identitySession == null) {
                onResult(false, "No valid authenticated store session is available for backup.")
                return@launch
            }

            val hashedUser = hashStringSHA256(identitySession.shopUid.lowercase().trim())
            val basePrefix = settings.firebasePrefix.trim().ifEmpty { "shreeshyam_sync" }.trim('/')
            val prefix = "$basePrefix/users/$hashedUser"

            _syncInProgress.value = true
            _syncMessage.value = "Starting Backup..."

            try {
                // Test Connection
                val isConnected = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.testFirebaseConnection(url)
                if (!isConnected) {
                    _syncInProgress.value = false
                    _syncMessage.value = null
                    onResult(false, "Could not connect to Firebase database URL. Verify URL!")
                    return@launch
                }

                // Gather snapshot data and require every table to upload successfully.
                val uploadResults = mutableListOf<Boolean>()
                _syncMessage.value = "Backing up Categories..."
                val catList = repository.allCategories.first()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "categories", catList, Category::class.java)

                _syncMessage.value = "Backing up Products..."
                val prodList = repository.allProducts.first()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "products", prodList, Product::class.java)

                _syncMessage.value = "Backing up Bills..."
                val salesList = repository.allSales.first()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "sales", salesList, Sale::class.java)

                _syncMessage.value = "Backing up Sale Items..."
                val saleItemsList = repository.getAllSaleItems()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "sale_items", saleItemsList, SaleItem::class.java)

                _syncMessage.value = "Backing up Customers..."
                val customersList = repository.allCustomers.first()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "customers", customersList, Customer::class.java)

                _syncMessage.value = "Backing up Ledger..."
                val udhaarList = repository.allUdhaarTransactions.first()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "udhaar_transactions", udhaarList, UdhaarTransaction::class.java)

                _syncMessage.value = "Backing up Stock Adjustments..."
                val adjList = repository.getAllStockAdjustmentsList()
                uploadResults += com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.uploadTable(url, prefix, "stock_adjustments", adjList, StockAdjustment::class.java)

                if (uploadResults.any { !it }) {
                    throw IllegalStateException("One or more backup tables failed to upload")
                }

                // Update sync time
                val currentDF = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", java.util.Locale.ENGLISH)
                val timeStr = currentDF.format(java.util.Date())
                settingsDataStore.updateLastSyncTime(timeStr)

                _syncMessage.value = "Backup Completed!"
                onResult(true, "Cloud Backup successful!")
            } catch (e: Exception) {
                onResult(false, "Backup failed: ${e.message}")
            } finally {
                _syncInProgress.value = false
                _syncMessage.value = null
            }
        }
    }

    fun restoreAllFromCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val url = settings.firebaseUrl.ifBlank { com.aistudio.shreeshyamstore.pqwzkb.BuildConfig.FIREBASE_URL }
            val identitySession = reconcileIdentitySession()
            if (identitySession == null) {
                onResult(false, "No valid authenticated store session is available for restore.")
                return@launch
            }

            val hashedUser = hashStringSHA256(identitySession.shopUid.lowercase().trim())
            val basePrefix = settings.firebasePrefix.trim().ifEmpty { "shreeshyam_sync" }.trim('/')
            val prefix = "$basePrefix/users/$hashedUser"

            _syncInProgress.value = true
            _syncMessage.value = "Testing connection..."

            try {
                // Test Connection
                val isConnected = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.testFirebaseConnection(url)
                if (!isConnected) {
                    _syncInProgress.value = false
                    _syncMessage.value = null
                    onResult(false, "Invalid Firebase URL or network unavailable.")
                    return@launch
                }

                _syncMessage.value = "Downloading Categories..."
                val catList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "categories", Category::class.java)

                _syncMessage.value = "Downloading Products..."
                val prodList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "products", Product::class.java)

                _syncMessage.value = "Downloading Bills..."
                val salesList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "sales", Sale::class.java)

                _syncMessage.value = "Downloading Sale Items..."
                val saleItemsList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "sale_items", SaleItem::class.java)

                _syncMessage.value = "Downloading Customers..."
                val customersList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "customers", Customer::class.java)

                _syncMessage.value = "Downloading Ledger..."
                val udhaarList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "udhaar_transactions", UdhaarTransaction::class.java)

                _syncMessage.value = "Downloading Adjustments..."
                val adjList = com.aistudio.shreeshyamstore.pqwzkb.utils.FirebaseSyncService.downloadTable(url, prefix, "stock_adjustments", StockAdjustment::class.java)

                if (catList.isEmpty() && prodList.isEmpty() && salesList.isEmpty() && saleItemsList.isEmpty() &&
                    customersList.isEmpty() && udhaarList.isEmpty() && adjList.isEmpty()) {
                    _syncInProgress.value = false
                    _syncMessage.value = null
                    onResult(false, "No backup data found under prefix '$prefix' at this database URL.")
                    return@launch
                }

                // Clear & Insert
                _syncMessage.value = "Restoring Database..."
                repository.replaceCloudRestorableTables(
                    categoriesList = catList,
                    productsList = prodList,
                    salesList = salesList,
                    saleItemsList = saleItemsList,
                    customersList = customersList,
                    udhaarTxsList = udhaarList,
                    adjustmentsList = adjList
                )

                _syncMessage.value = "Database Restored!"
                onResult(true, "All data successfully synchronized from Cloud!")
            } catch (e: Exception) {
                onResult(false, "Restore failed: ${e.message}")
            } finally {
                _syncInProgress.value = false
                _syncMessage.value = null
            }
        }
    }
}

class ShopViewModelFactory(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository, settingsDataStore, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
