package com.harrylabs.shreeshyamstore.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.data.*
import com.harrylabs.shreeshyamstore.utils.CalculationResult
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import com.harrylabs.shreeshyamstore.utils.QuantityPriceCalculator
import com.harrylabs.shreeshyamstore.utils.UnitRate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

sealed class Screen {
    object Welcome : Screen()
    object Login : Screen()       // Auth screens
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

data class CartLine(
    val product: Product,
    val quantity: Int,
    val quantityBase: Long,
    val enteredQuantityText: String,
    val unitPrice: Double,
    val lineTotalPaise: Long,
    val originalPricePerUnitPaise: Long,
    val originalPriceUnitBaseQty: Long,
    val effectivePricePerUnitPaise: Long,
    val effectivePriceUnitBaseQty: Long,
    val rateOverridden: Boolean = false
) {
    val lineTotal: Double
        get() = lineTotalPaise / 100.0
}
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String, val onRetry: () -> Unit) : AuthState()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}

class ShopViewModel(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore,
    private val firebaseOwnerRepository: FirebaseOwnerRepository,
    private val database: AppDatabase
) : ViewModel() {

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Welcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Auth state ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val sourceDeviceId = "android-local-device"

    val isUserLoggedIn: Boolean
        get() = firebaseOwnerRepository.getCurrentUser() != null

    val currentUser: OwnerIdentity?
        get() = firebaseOwnerRepository.getCurrentUser()

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
                selectedLanguage = "en",
                cachedOwnerUid = "",
                cachedShopId = ""
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

    // --- Central auth startup gating ---
    fun checkSessionAndRoute(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseOwnerRepository.getCurrentUser()
        if (user == null) {
            _authState.value = AuthState.Idle
            navigateTo(Screen.Login)
            onSuccess()
        } else {
            viewModelScope.launch {
                restoreProfileAndVerify(user, context, onSuccess, onError)
            }
        }
    }

    // --- User Authentication & Session Management Functions ---
    fun signInWithGoogle(
        idToken: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val signInResult = firebaseOwnerRepository.signInWithGoogle(idToken)
            signInResult.fold(
                onSuccess = { identity ->
                    restoreProfileAndVerify(identity, context, onSuccess, onError)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Sign-in failed") {
                        signInWithGoogle(idToken, context, onSuccess, onError)
                    }
                    onError(e.localizedMessage ?: "Sign-in failed")
                }
            )
        }
    }

    private suspend fun restoreProfileAndVerify(
        identity: OwnerIdentity,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _authState.value = AuthState.Loading
        val profileResult = firebaseOwnerRepository.fetchUserProfile(identity.uid)
        profileResult.fold(
            onSuccess = { profile ->
                if (profile != null) {
                    val shopId = profile.activeShopId
                    if (!shopId.isNullOrEmpty()) {
                        val shopResult = firebaseOwnerRepository.fetchShopProfile(shopId)
                        shopResult.fold(
                            onSuccess = { shop ->
                                if (shop != null) {
                                    runCatching {
                                        handleTransitionResetIfNeeded(identity.uid, shopId, shop.name, shop.ownerPhone)
                                    }.fold(
                                        onSuccess = { onSuccess() },
                                        onFailure = { e ->
                                            _authState.value = AuthState.Error(context.getString(R.string.error_network_connection)) {
                                                viewModelScope.launch {
                                                    restoreProfileAndVerify(identity, context, onSuccess, onError)
                                                }
                                            }
                                            onError(e.localizedMessage ?: "Cloud restore failed")
                                        }
                                    )
                                } else {
                                    // Shop missing - show recovery error
                                    _authState.value = AuthState.Error(context.getString(R.string.error_shop_not_found)) {
                                        viewModelScope.launch {
                                            restoreProfileAndVerify(identity, context, onSuccess, onError)
                                        }
                                    }
                                    onError("Shop profile not found in cloud")
                                }
                            },
                            onFailure = { e ->
                                _authState.value = AuthState.Error(context.getString(R.string.error_network_connection)) {
                                    viewModelScope.launch {
                                        restoreProfileAndVerify(identity, context, onSuccess, onError)
                                    }
                                }
                                onError(e.localizedMessage ?: "Network error occurred")
                            }
                        )
                    } else {
                        _authState.value = AuthState.Idle
                        navigateTo(Screen.Setup)
                        onSuccess()
                    }
                } else {
                    _authState.value = AuthState.Idle
                    navigateTo(Screen.Setup)
                    onSuccess()
                }
            },
            onFailure = { e ->
                _authState.value = AuthState.Error(context.getString(R.string.error_network_connection)) {
                    viewModelScope.launch {
                        restoreProfileAndVerify(identity, context, onSuccess, onError)
                    }
                }
                onError(e.localizedMessage ?: "Network error occurred")
            }
        )
    }

    private suspend fun handleTransitionResetIfNeeded(
        uid: String,
        shopId: String,
        shopName: String,
        ownerPhone: String
    ) {
        val cachedShopId = storeSettings.value.cachedShopId
        val cachedOwnerUid = storeSettings.value.cachedOwnerUid

        if (cachedShopId.isNotEmpty() && (cachedShopId != shopId || cachedOwnerUid != uid)) {
            // Transition reset triggered: clear old DB & settings cache before restoring the cloud shop.
            repository.clearAllLocalData(database)
        }

        val cloudSnapshot = firebaseOwnerRepository.fetchShopDataSnapshot(shopId).getOrThrow()
        repository.replaceLocalShopDataFromSnapshot(database, cloudSnapshot)

        // Save new session settings
        val user = firebaseOwnerRepository.getCurrentUser()
        settingsDataStore.saveSession(
            username = user?.displayName?.ifEmpty { "Owner" } ?: "Owner",
            email = user?.email ?: "",
            uid = uid,
            shopId = shopId
        )
        settingsDataStore.updateShopName(shopName)
        settingsDataStore.updateOwnerPhone(ownerPhone)
        settingsDataStore.setFirstLaunchCompleted(true)

        _authState.value = AuthState.Idle
        _syncState.value = SyncState.Synced
        navigateTo(Screen.Home)
    }

    private suspend fun syncLocalShopDataIfPossible(
        entityType: String = SyncEntityType.SHOP_SNAPSHOT,
        entityUuid: String? = null,
        enqueueOperation: Boolean = true
    ) {
        val settings = settingsDataStore.settingsFlow.first()
        val shopId = settings.cachedShopId
        val user = firebaseOwnerRepository.getCurrentUser()
        if (shopId.isBlank() || user == null) return
        if (enqueueOperation) {
            repository.enqueueSyncOperation(
                shopId = shopId,
                entityType = entityType,
                entityUuid = entityUuid ?: shopId,
                createdByUid = user.uid,
                sourceDeviceId = sourceDeviceId
            )
        }
        syncLocalShopData(shopId)
    }

    private suspend fun syncLocalShopData(shopId: String): Result<Unit> {
        _syncState.value = SyncState.Syncing
        val result = firebaseOwnerRepository.pushShopDataSnapshot(
            shopId = shopId,
            snapshot = repository.getShopDataSnapshot()
        )
        result.fold(
            onSuccess = {
                repository.clearCompletedSyncOperations(shopId)
                _syncState.value = SyncState.Synced
            },
            onFailure = { e ->
                val message = e.localizedMessage ?: "Cloud sync failed"
                repository.markPendingSyncOperationsFailed(shopId, message)
                _syncState.value = SyncState.Error(message)
            }
        )
        return result
    }

    fun retrySyncNow() {
        viewModelScope.launch {
            syncLocalShopDataIfPossible(enqueueOperation = false)
        }
    }

    fun createShop(
        shopName: String,
        ownerPhone: String,
        welcomeChantEnabled: Boolean,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = firebaseOwnerRepository.getCurrentUser()
        if (user == null) {
            onError("Not authenticated")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val shopId = java.util.UUID.randomUUID().toString()
            val result = firebaseOwnerRepository.createShopAndProfileAtomically(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName.ifBlank { "Owner" },
                shopId = shopId,
                shopName = shopName.trim(),
                ownerPhone = ownerPhone.trim()
            )
            result.fold(
                onSuccess = {
                    // Update settings cache and bind
                    settingsDataStore.updateShopName(shopName.trim())
                    settingsDataStore.updateOwnerPhone(ownerPhone.trim())
                    settingsDataStore.updateWelcomeChantEnabled(welcomeChantEnabled)
                    settingsDataStore.saveSession(
                        username = user.displayName.ifBlank { "Owner" },
                        email = user.email,
                        uid = user.uid,
                        shopId = shopId
                    )
                    settingsDataStore.setFirstLaunchCompleted(true)
                    syncLocalShopData(shopId)

                    _authState.value = AuthState.Idle
                    navigateTo(Screen.Home)
                    onSuccess()
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to create shop") {
                        createShop(shopName, ownerPhone, welcomeChantEnabled, context, onSuccess, onError)
                    }
                    onError(e.localizedMessage ?: "Failed to create shop")
                }
            )
        }
    }

    fun logoutUser(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            firebaseOwnerRepository.signOut()
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // ignore
            }
            settingsDataStore.clearSession()
            _authState.value = AuthState.Idle
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
                    val categoryUuid = repository.insertCategory(Category(name = name.trim()))
                    syncLocalShopDataIfPossible(SyncEntityType.CATEGORY, categoryUuid)
                }
            }
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            if (newName.trim().isNotEmpty()) {
                repository.updateCategory(category.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
                syncLocalShopDataIfPossible(SyncEntityType.CATEGORY, category.localUuid)
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
        isActive: Boolean,
        unitType: String = DataUnitType.PIECE,
        displayUnit: String = DataDisplayUnit.PIECE,
        baseUnit: String = DataDisplayUnit.PIECE,
        allowsDecimalQuantity: Boolean = false,
        quantityScale: Int = 0,
        priceUnitBaseQty: Long = 1L,
        purchasePriceUnitBaseQty: Long? = purchasePrice?.let { priceUnitBaseQty },
        stockQuantityBase: Long = currentStock.toLong(),
        lowStockAlertBase: Long = lowStockAlertQty.toLong()
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val normalizedStockBase = if (trackStock) stockQuantityBase else 0L
            val normalizedLowStockBase = if (trackStock) lowStockAlertBase else 0L
            var changedProductUuid: String? = uuid
            if (uuid == null) {
                // Insert
                val product = Product(
                    name = name.trim(),
                    categoryId = categoryId,
                    mrp = mrp,
                    sellingPrice = sellingPrice,
                    purchasePrice = purchasePrice,
                    unitType = unitType,
                    displayUnit = displayUnit,
                    baseUnit = baseUnit,
                    allowsDecimalQuantity = allowsDecimalQuantity,
                    quantityScale = quantityScale,
                    priceUnitBaseQty = priceUnitBaseQty,
                    purchasePriceUnitBaseQty = purchasePriceUnitBaseQty,
                    currentStock = if (trackStock) currentStock else 0,
                    stockQuantityBase = normalizedStockBase,
                    trackStock = trackStock,
                    lowStockAlertQty = if (trackStock) lowStockAlertQty else 0,
                    lowStockAlertBase = normalizedLowStockBase,
                    isActive = isActive,
                    createdAt = now,
                    updatedAt = now
                )
                val newProductUuid = repository.insertProduct(product)
                changedProductUuid = newProductUuid
                // Log stock adjustment for opening entry
                if (trackStock && normalizedStockBase > 0L) {
                    val adjustment = StockAdjustment(
                        productId = newProductUuid,
                        oldStock = 0,
                        oldQuantityBase = 0L,
                        newStock = currentStock,
                        newQuantityBase = normalizedStockBase,
                        difference = currentStock,
                        differenceBase = normalizedStockBase,
                        displayUnitSnapshot = displayUnit,
                        reason = "Opening stock entry",
                        createdAt = now
                    )
                    repository.insertStockAdjustment(adjustment)
                }
            } else {
                // Update
                val existing = repository.getProductById(uuid)
                if (existing != null) {
                    val finalStock = if (trackStock) currentStock else existing.currentStock
                    val finalStockBase = if (trackStock) normalizedStockBase else existing.stockQuantityBase
                    val product = existing.copy(
                        name = name.trim(),
                        categoryId = categoryId,
                        mrp = mrp,
                        sellingPrice = sellingPrice,
                        purchasePrice = purchasePrice,
                        unitType = unitType,
                        displayUnit = displayUnit,
                        baseUnit = baseUnit,
                        allowsDecimalQuantity = allowsDecimalQuantity,
                        quantityScale = quantityScale,
                        priceUnitBaseQty = priceUnitBaseQty,
                        purchasePriceUnitBaseQty = purchasePriceUnitBaseQty,
                        currentStock = finalStock,
                        stockQuantityBase = finalStockBase,
                        trackStock = trackStock,
                        lowStockAlertQty = if (trackStock) lowStockAlertQty else existing.lowStockAlertQty,
                        lowStockAlertBase = if (trackStock) normalizedLowStockBase else existing.lowStockAlertBase,
                        isActive = isActive,
                        updatedAt = now
                    )
                    repository.updateProduct(product)

                    // Log difference adjustment if stock manual update occurred
                    if (trackStock && finalStockBase != existing.stockQuantityBase) {
                        val diff = currentStock - existing.currentStock
                        val diffBase = finalStockBase - existing.stockQuantityBase
                        val adjustment = StockAdjustment(
                            productId = uuid,
                            oldStock = existing.currentStock,
                            oldQuantityBase = existing.stockQuantityBase,
                            newStock = currentStock,
                            newQuantityBase = finalStockBase,
                            difference = diff,
                            differenceBase = diffBase,
                            displayUnitSnapshot = displayUnit,
                            reason = "Manual correction during edit",
                            createdAt = now
                        )
                        repository.insertStockAdjustment(adjustment)
                    }
                }
            }
            syncLocalShopDataIfPossible(SyncEntityType.PRODUCT, changedProductUuid)
        }
    }

    suspend fun getProduct(uuid: String): Product? = repository.getProductById(uuid)

    fun adjustStock(productUuid: String, actualStockCounted: Int, reason: String) {
        viewModelScope.launch {
            repository.adjustProductStock(productUuid, actualStockCounted, reason)
            syncLocalShopDataIfPossible(SyncEntityType.STOCK_ADJUSTMENT, productUuid)
        }
    }

    fun getAdjustmentsForProduct(productUuid: String): Flow<List<StockAdjustment>> = repository.getAdjustmentsForProduct(productUuid)


    // --- Billing State (Cart) ---
    private val _cartState = MutableStateFlow<Map<Product, CartLine>>(emptyMap())
    val cartState: StateFlow<Map<Product, CartLine>> = _cartState.asStateFlow()

    val cartTotal: StateFlow<Double> = _cartState.map { cart ->
        cart.values.sumOf { line -> line.lineTotal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private fun pieceCartLine(product: Product, quantity: Int): CartLine {
        val unitPrice = product.getEffectivePrice()
        val totalPaise = rupeesToPaise(unitPrice * quantity)
        return CartLine(
            product = product,
            quantity = quantity,
            quantityBase = quantity.toLong(),
            enteredQuantityText = quantity.toString(),
            unitPrice = unitPrice,
            lineTotalPaise = totalPaise,
            originalPricePerUnitPaise = product.pricePerUnitPaise,
            originalPriceUnitBaseQty = product.priceUnitBaseQty,
            effectivePricePerUnitPaise = rupeesToPaise(unitPrice),
            effectivePriceUnitBaseQty = product.priceUnitBaseQty,
            rateOverridden = false
        )
    }

    fun addProductToCart(product: Product, quantity: Int = 1) {
        val current = _cartState.value.toMutableMap()
        val currentQty = current[product]?.quantity ?: 0
        val finalQty = currentQty + quantity
        if (finalQty > 0) {
            current[product] = pieceCartLine(product, finalQty)
        } else {
            current.remove(product)
        }
        _cartState.value = current
    }

    fun setProductQuantityInCart(product: Product, qty: Int) {
        val current = _cartState.value.toMutableMap()
        if (qty > 0) {
            current[product] = pieceCartLine(product, qty)
        } else {
            current.remove(product)
        }
        _cartState.value = current
    }

    fun setLooseProductInCart(product: Product, quantityBase: Long, enteredQuantityText: String): Boolean {
        val rate = UnitRate(
            pricePerUnitPaise = product.pricePerUnitPaise,
            priceUnitBaseQty = product.priceUnitBaseQty.takeIf { it > 0L } ?: 1L
        )
        return when (val result = QuantityPriceCalculator.lineAmount(quantityBase, rate)) {
            is CalculationResult.Failure -> false
            is CalculationResult.Success -> {
                val current = _cartState.value.toMutableMap()
                current[product] = CartLine(
                    product = product,
                    quantity = 1,
                    quantityBase = quantityBase,
                    enteredQuantityText = enteredQuantityText,
                    unitPrice = result.value.lineTotalPaise / 100.0,
                    lineTotalPaise = result.value.lineTotalPaise,
                    originalPricePerUnitPaise = result.value.originalRate.pricePerUnitPaise,
                    originalPriceUnitBaseQty = result.value.originalRate.priceUnitBaseQty,
                    effectivePricePerUnitPaise = result.value.effectiveRate.pricePerUnitPaise,
                    effectivePriceUnitBaseQty = result.value.effectiveRate.priceUnitBaseQty,
                    rateOverridden = result.value.rateOverridden
                )
                _cartState.value = current
                true
            }
        }
    }

    fun setLooseProductAmountInCart(
        product: Product,
        amountPaise: Long,
        quantityBase: Long,
        enteredQuantityText: String
    ): Boolean {
        if (amountPaise <= 0L || quantityBase <= 0L || enteredQuantityText.isBlank()) return false

        val rate = UnitRate(
            pricePerUnitPaise = product.pricePerUnitPaise,
            priceUnitBaseQty = product.priceUnitBaseQty.takeIf { it > 0L } ?: 1L
        )
        if (QuantityPriceCalculator.amountForQuantity(quantityBase, rate) is CalculationResult.Failure) {
            return false
        }

        val current = _cartState.value.toMutableMap()
        current[product] = CartLine(
            product = product,
            quantity = 1,
            quantityBase = quantityBase,
            enteredQuantityText = enteredQuantityText,
            unitPrice = amountPaise / 100.0,
            lineTotalPaise = amountPaise,
            originalPricePerUnitPaise = rate.pricePerUnitPaise,
            originalPriceUnitBaseQty = rate.priceUnitBaseQty,
            effectivePricePerUnitPaise = rate.pricePerUnitPaise,
            effectivePriceUnitBaseQty = rate.priceUnitBaseQty,
            rateOverridden = false
        )
        _cartState.value = current
        return true
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
            syncLocalShopDataIfPossible(SyncEntityType.PRODUCT, newUuid)
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

            val saleItems = cartItems.values.map { line ->
                val prod = line.product
                SaleItem(
                    saleId = sale.localUuid, // linked using UUID string directly
                    productId = prod.localUuid, // linked using UUID string directly
                    productNameSnapshot = prod.name,
                    quantity = line.quantity,
                    unitTypeSnapshot = prod.unitType,
                    displayUnitSnapshot = prod.displayUnit,
                    baseUnitSnapshot = prod.baseUnit,
                    enteredQuantityText = line.enteredQuantityText,
                    quantityBase = line.quantityBase,
                    unitPrice = line.unitPrice,
                    originalPricePerUnitPaise = line.originalPricePerUnitPaise,
                    originalPriceUnitBaseQty = line.originalPriceUnitBaseQty,
                    effectivePricePerUnitPaise = line.effectivePricePerUnitPaise,
                    effectivePriceUnitBaseQty = line.effectivePriceUnitBaseQty,
                    rateOverridden = line.rateOverridden,
                    lineTotal = line.lineTotal,
                    lineTotalPaise = line.lineTotalPaise,
                    purchasePricePerUnitPaiseSnapshot = prod.purchasePricePerUnitPaise,
                    purchasePriceUnitBaseQtySnapshot = prod.purchasePriceUnitBaseQty
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

            syncLocalShopDataIfPossible(SyncEntityType.SALE, savedSaleUuid)

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
            val transactionUuid = repository.insertUdhaarTransaction(tx)
            syncLocalShopDataIfPossible(SyncEntityType.UDHAAR_TRANSACTION, transactionUuid)
        }
    }

    fun quickAddCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isNotEmpty()) {
                val existing = repository.getCustomerByName(trimmedName)
                if (existing == null) {
                    val customerUuid = repository.insertCustomer(
                        Customer(
                            name = trimmedName,
                            phone = phone.trim().ifEmpty { null }
                        )
                    )
                    syncLocalShopDataIfPossible(SyncEntityType.CUSTOMER, customerUuid)
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
    private val settingsDataStore: SettingsDataStore,
    private val firebaseOwnerRepository: FirebaseOwnerRepository,
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository, settingsDataStore, firebaseOwnerRepository, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
