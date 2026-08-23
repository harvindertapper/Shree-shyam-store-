package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.shreeshyamstore.pqwzkb.BuildConfig
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.data.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.AuthenticatedBackupTableClient
import com.aistudio.shreeshyamstore.pqwzkb.utils.AuthenticatedRestBackupProvider
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupAuthContext
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupIncompatibleException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupProviderException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUnauthorizedException
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.PinUnlockResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudRestorableSnapshot
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalRecoveryPointStore
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreRecoveryCoordinator
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreSnapshotException
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreSnapshotValidator
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotEnvelope
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotUnavailableException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncCursor
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncHealthSnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.URI
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

class ShopViewModel(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore,
    private val context: Context? = null
) : ViewModel() {

    suspend fun currentCommandMetadata(): CommandMetadata {
        val session = reconcileIdentitySession()
            ?: error("Authenticated actor is required")
        val tenantContext = settingsDataStore.getOrCreateTenantDeviceContext(session)
        val actor = PlatformActor(
            actorId = session.uid,
            displayName = session.username.ifBlank { session.email }.ifBlank { session.uid },
            role = session.role,
            deviceId = tenantContext.deviceId
        )
        val now = System.currentTimeMillis()
        return CommandMetadata(
            idempotencyKey = UUID.randomUUID().toString(),
            clientEventId = UUID.randomUUID().toString(),
            tenant = tenantContext.toTenantScope(),
            actor = actor,
            clientCreatedAt = now
        )
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
        if (!BuildConfig.CLOUD_SYNC_ENABLED) return
        context?.let { ctx ->
            viewModelScope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                if (settings.isUserLoggedIn && settings.autoSyncEnabled) {
                    try {
                        com.aistudio.shreeshyamstore.pqwzkb.utils.SyncManager.triggerImmediateSync(ctx)
                        com.aistudio.shreeshyamstore.pqwzkb.utils.SyncManager.triggerAutomaticBackup(ctx)
                    } catch (_: Exception) {
                        // Ignore sync scheduling if running in unit test without context.
                    }
                }
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
                securityPin = "",
                firebaseUrl = "",
                firebasePrefix = "shreeshyam_sync",
                lastSyncTime = "Never Synced",
                autoSyncEnabled = true,
                appLanguage = com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage.HINDI
            )
        )

    fun setLanguage(language: com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.updateAppLanguage(language)
        }
    }

    fun updateSettings(shopName: String, ownerPhone: String, welcomeChantEnabled: Boolean, qrImageUri: String, securityPin: String = "") {
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
                val tenantContext = settingsDataStore.getOrCreateTenantDeviceContext(session)
                repository.saveShopProfile(
                    ShopProfile(
                        uid = session.shopUid,
                        organizationId = tenantContext.organizationId,
                        storeId = tenantContext.storeId,
                        membershipId = tenantContext.membershipId,
                        deviceId = tenantContext.deviceId,
                        appInstallationId = tenantContext.appInstallationId,
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

    fun recoverAppLockWithLocalCredentials(
        identifier: String,
        password: String,
        newPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!SecurityUtils.isAcceptableNewPin(newPin)) {
                    onError("Choose a different 4-digit PIN. Avoid 1234, repeated digits, or simple sequences.")
                    return@launch
                }
                val session = settingsDataStore.settingsFlow.first().identitySessionOrNull()
                if (session?.provider != IdentityProvider.LOCAL) {
                    onError("This device is not using a local account. Use the matching recovery method.")
                    return@launch
                }
                val key = identifier.trim()
                val user = if (key.contains("@")) {
                    repository.getUserByEmail(key.lowercase())
                } else {
                    repository.getUserByUsername(key)
                }
                if (user == null || user.uid.trim() != session.uid) {
                    onError("The local account does not match this device session.")
                    return@launch
                }
                val verification = SecurityUtils.verifyCredential(
                    secret = password,
                    storedCredential = user.passwordHash,
                    scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
                )
                if (!verification.matched) {
                    onError("Incorrect local account password.")
                    return@launch
                }
                updateSecurityPinAfterRecovery(newPin)
                onSuccess()
            } catch (_: Exception) {
                onError("Local recovery failed. Verify the account details and try again.")
            }
        }
    }

    fun recoverAppLockWithFirebase(
        newPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!SecurityUtils.isAcceptableNewPin(newPin)) {
                    onError("Choose a different 4-digit PIN. Avoid 1234, repeated digits, or simple sequences.")
                    return@launch
                }
                val session = settingsDataStore.settingsFlow.first().identitySessionOrNull()
                if (session?.provider != IdentityProvider.FIREBASE) {
                    onError("This device is not using a Google account. Use the matching recovery method.")
                    return@launch
                }
                val appContext = context
                if (appContext == null) {
                    onError("Google recovery is unavailable without an active app context.")
                    return@launch
                }
                val currentFirebaseUser = com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.currentUser
                if (currentFirebaseUser == null || currentFirebaseUser.uid != session.uid) {
                    onError("Google session expired. Sign in again before changing the PIN.")
                    return@launch
                }
                val result = com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.signInWithGoogle(appContext)
                val verifiedUser = result.getOrNull()
                if (verifiedUser == null || verifiedUser.uid != session.uid) {
                    onError("The verified Google account does not match this store session.")
                    return@launch
                }
                updateSecurityPinAfterRecovery(newPin)
                onSuccess()
            } catch (_: Exception) {
                onError("Google verification failed. Check Firebase configuration and try again.")
            }
        }
    }

    private suspend fun updateSecurityPinAfterRecovery(newPin: String) {
        settingsDataStore.updateSecurityPin(newPin)
        settingsDataStore.updateAppLockEnabled(true)
        settingsDataStore.updateAppLockState(AppLockPolicy.recordSuccess(System.currentTimeMillis()))
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
                    passwordHash = SecurityUtils.createCredential(
                        password,
                        SecurityUtils.CredentialScope.LOCAL_ACCOUNT
                    )
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
                val nowEpochMs = System.currentTimeMillis()
                val throttleState = settingsDataStore.settingsFlow.first()
                if (LocalLoginPolicy.isLocked(
                        failedAttempts = throttleState.localLoginFailedAttempts,
                        lockedUntilEpochMs = throttleState.localLoginLockedUntilEpochMs,
                        nowEpochMs = nowEpochMs
                    )
                ) {
                    onError("Too many failed login attempts. Try again later.")
                    return@launch
                }

                // Resolve the local account without revealing whether the
                // username/email exists in the error response.
                val user = if (key.contains("@")) {
                    repository.getUserByEmail(key)
                } else {
                    repository.getUserByUsername(key)
                }
                val verification = user?.let {
                    SecurityUtils.verifyCredential(
                        secret = password,
                        storedCredential = it.passwordHash,
                        scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
                    )
                } ?: SecurityUtils.VerificationResult(matched = false, needsRehash = false)

                when (val loginResult = settingsDataStore.evaluateLocalLogin(verification.matched, nowEpochMs)) {
                    LocalLoginResult.Success -> {
                        if (user == null || !verification.matched) {
                            onError("Incorrect username or password. Please retry.")
                            return@launch
                        }

                        if (verification.needsRehash) {
                            // Migration failure must not strand an offline user;
                            // the legacy verifier remains usable for the next
                            // attempt and can be retried without network access.
                            runCatching {
                                repository.updateLocalCredential(
                                    userId = user.id,
                                    credentialVerifier = SecurityUtils.createCredential(
                                        password,
                                        SecurityUtils.CredentialScope.LOCAL_ACCOUNT
                                    )
                                )
                            }
                        }

                        val localSession = IdentitySession.localForUser(
                            username = user.username,
                            email = user.email,
                            existingUid = user.uid
                        )
                        clearFirebaseAuthorityForLocalSession()
                        settingsDataStore.saveSession(localSession)
                        onSuccess()
                    }
                    is LocalLoginResult.Locked -> {
                        onError("Too many failed login attempts. Try again later.")
                    }
                    is LocalLoginResult.Invalid -> {
                        onError("Incorrect username or password. Please retry.")
                    }
                }
            } catch (e: Exception) {
                onError("Login failed. Please retry.")
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
    private val billingCheckout = BillingCheckoutController(
        gateway = ShopRepositoryBillingGateway(repository) { currentCommandMetadata() },
        scope = viewModelScope,
        onAutoSync = ::triggerAutoSync,
        onCheckoutSuccess = { navigateTo(Screen.BillSuccess) }
    )

    val cartState: StateFlow<Map<Product, Double>> = billingCheckout.cartState
    val cartTotal: StateFlow<Long> = billingCheckout.cartTotal

    /** Compatibility facade while billing screens migrate to the focused billing boundary. */
    fun addProductToCart(product: Product, quantity: Double = 1.0) =
        billingCheckout.addProductToCart(product, quantity)

    /** Compatibility facade while billing screens migrate to the focused billing boundary. */
    fun setProductQuantityInCart(product: Product, qty: Double) =
        billingCheckout.setProductQuantityInCart(product, qty)

    /** Compatibility facade while billing screens migrate to the focused billing boundary. */
    fun removeProductFromCart(product: Product) =
        billingCheckout.removeProductFromCart(product)

    /** Compatibility facade while billing screens migrate to the focused billing boundary. */
    fun clearCart() = billingCheckout.clearCart()

    /**
     * Allows adding a missing item on-the-fly and automatically adding it to the cart
     */
    fun quickAddProduct(name: String, mrp: Long, categoryId: Long, trackStock: Boolean, currentStock: Double, unit: String = "pcs", barcode: String = "") {
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateProductName(name)
                val normalizedMrp = InventoryValidation.validateProductMoney(mrp, "MRP")
                val normalizedStock = InventoryValidation.validateQuantity(currentStock, "Current stock")
                val normalizedUnit = InventoryValidation.validateUnit(unit)
                val normalizedBarcode = InventoryValidation.normalizeBarcode(barcode)
                require(repository.isBarcodeAvailable(normalizedBarcode.orEmpty())) {
                    "Barcode already belongs to another active product"
                }
                val now = System.currentTimeMillis()
                val prod = Product(
                    name = normalizedName,
                    categoryId = categoryId,
                    mrp = normalizedMrp,
                    sellingPrice = normalizedMrp,
                    currentStock = normalizedStock,
                    unit = normalizedUnit,
                    trackStock = trackStock,
                    barcode = barcode.trim(),
                    barcodeKey = normalizedBarcode,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
                val newId = repository.insertProductWithOpeningStock(
                    product = prod,
                    openingStock = normalizedStock,
                    createdAt = now,
                    command = currentCommandMetadata()
                )

                // Add the inserted product directly to our cart.
                addProductToCart(prod.copy(id = newId), 1.0)
                triggerAutoSync()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(context, error.message ?: "Product could not be added", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Product could not be added", Toast.LENGTH_LONG).show()
            }
        }
    }



    // --- Payment & Saving State ---
    val lastSale: StateFlow<Sale?> = billingCheckout.lastSale
    val lastSaleItems: StateFlow<List<SaleItem>> = billingCheckout.lastSaleItems
    val checkoutInFlight: StateFlow<Boolean> = billingCheckout.checkoutInFlight
    val checkoutError: StateFlow<String?> = billingCheckout.checkoutError

    fun clearCheckoutError() = billingCheckout.clearCheckoutError()

    fun reconcilePaymentState(
        saleId: Long,
        targetState: PaymentState,
        receivedAmount: Long,
        onResult: (Boolean) -> Unit = {}
    ) = billingCheckout.reconcilePaymentState(saleId, targetState, receivedAmount, onResult)

    fun completeBill(
        paymentMode: String, // "CASH", "UPI", "UDHAAR"
        customerId: Long? = null,
        customerName: String = "",
        customerPhone: String = "",
        note: String? = null,
        receivedAmount: Long? = null
    ) = billingCheckout.completeBill(
        paymentMode = paymentMode,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        note = note,
        receivedAmount = receivedAmount
    )


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
                    command = currentCommandMetadata()
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
                    command = currentCommandMetadata()
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
                    command = currentCommandMetadata()
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
                        customer = Customer(
                            name = trimmedName,
                            phone = phone.trim().ifEmpty { null }
                        ),
                        command = currentCommandMetadata()
                    )
                    triggerAutoSync()
                }
            }
        }
    }

    // Helper functions for clipboard copy text and sharing
    fun generateInvoiceText(customSale: Sale? = null, customItems: List<SaleItem>? = null): String {
        val sale = customSale ?: lastSale.value ?: return "No Invoice Found"
        val items = customItems ?: lastSaleItems.value
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
            ownerName = settings.ownerName.ifEmpty { null },
            paymentState = sale.paymentState,
            receivedAmount = sale.receivedAmount
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
            try {
                val validatedQuantity = InventoryValidation.validateQuantity(quantityToAdd, "Restock quantity")
                require(validatedQuantity > 0.0) { "Restock quantity must be greater than zero" }
                val current = repository.getProductById(product.id)
                    ?: error("Product was not found")
                val newStock = InventoryValidation.validateQuantity(
                    current.currentStock + validatedQuantity,
                    "New stock"
                )
                repository.adjustProductStock(
                    productId = product.id,
                    actualStockCounted = newStock,
                    reason = "Bulk Wholesale Restock",
                    command = currentCommandMetadata()
                )
                triggerAutoSync()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(context, error.message ?: "Restock could not be saved", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Restock could not be saved", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Cloud Synchronization State ---
    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _syncHealthSnapshot = MutableStateFlow(SyncHealthSnapshot.empty())
    val syncHealthSnapshot: StateFlow<SyncHealthSnapshot> = _syncHealthSnapshot.asStateFlow()

    /** Refreshes only redacted counts and the legacy local cursor; no payload leaves the repository. */
    fun refreshSyncHealth() {
        viewModelScope.launch {
            refreshSyncHealthNow()
        }
    }

    private suspend fun refreshSyncHealthNow(nowEpochMs: Long = System.currentTimeMillis()) {
        val settings = settingsDataStore.settingsFlow.first()
        val summary = repository.getSyncOutboxSummary()
        _syncHealthSnapshot.value = SyncHealthSnapshot.from(
            nowEpochMs = nowEpochMs,
            lastSyncEpochMs = SyncCursor.parse(settings.lastSyncTime),
            outbox = summary
        )
    }

    /** Operator recovery: reset dead letters and let the normal sync worker retry them. */
    fun retrySyncDeadLetters() {
        viewModelScope.launch {
            repository.requeueSyncDeadLetters()
            refreshSyncHealthNow()
            triggerAutoSync()
        }
    }

    private suspend fun authenticatedBackupClient(
        settings: StoreSettings,
        session: IdentitySession
    ): AuthenticatedBackupTableClient {
        val firebaseUser = com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.currentUser
        if (session.provider != IdentityProvider.FIREBASE || firebaseUser?.uid != session.uid) {
            throw BackupUnauthorizedException("Cloud backup requires an active authenticated Firebase session")
        }
        val token = firebaseUser.getIdToken(false).await()?.token?.trim().orEmpty()
        if (token.isEmpty()) {
            throw BackupUnauthorizedException("Cloud backup authentication token is unavailable")
        }
        val url = settings.firebaseUrl.ifBlank { com.aistudio.shreeshyamstore.pqwzkb.BuildConfig.FIREBASE_URL }.trim()
        val trustedHost = runCatching {
            URI(com.aistudio.shreeshyamstore.pqwzkb.BuildConfig.FIREBASE_URL).host?.trim()?.lowercase()
        }.getOrNull()
        if (trustedHost.isNullOrBlank()) {
            throw BackupIncompatibleException("Backup provider trusted host is not configured")
        }
        val tenantContext = settingsDataStore.getOrCreateTenantDeviceContext(session)
        val provider = AuthenticatedRestBackupProvider(
            baseUrl = url,
            basePrefix = settings.firebasePrefix,
            auth = BackupAuthContext.fromFirebaseSession(
                session = session,
                tenant = tenantContext.toTenantScope(),
                bearerToken = token
            ),
            allowedHosts = setOf(trustedHost)
        )
        return AuthenticatedBackupTableClient(provider)
    }

    private fun backupFailureMessage(error: Throwable): String = when (error) {
        is BackupProviderException -> when (error) {
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUnauthorizedException -> "Backup authorization failed. Sign in again."
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupWrongTenantException -> "Backup belongs to a different store and was rejected."
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUnavailableException -> "Backup provider is unavailable. Try again when online."
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupMalformedException -> "Backup data is malformed or incomplete. Local data was not changed."
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupIncompatibleException -> "Backup format or provider configuration is incompatible."
            is com.aistudio.shreeshyamstore.pqwzkb.utils.BackupReplayException -> "This backup snapshot was already seen and was rejected."
        }
        is RestoreSnapshotException -> "Backup snapshot validation failed. Local data was not changed."
        is IllegalArgumentException -> error.message ?: "Backup configuration is invalid."
        else -> "Backup operation failed. Local data was not changed."
    }

    private suspend fun currentCloudRestorableSnapshot(): CloudRestorableSnapshot = CloudRestorableSnapshot(
        categories = repository.allCategories.first(),
        products = repository.allProducts.first(),
        sales = repository.allSales.first(),
        saleItems = repository.getAllSaleItems(),
        customers = repository.allCustomers.first(),
        udhaarTransactions = repository.allUdhaarTransactions.first(),
        stockAdjustments = repository.getAllStockAdjustmentsList()
    )

    private fun recoveryPointStore(): LocalRecoveryPointStore? =
        context?.let { LocalRecoveryPointStore(File(it.filesDir, "restore-recovery")) }

    private fun restoreFailureMessage(error: Throwable): String = when (error) {
        is RestoreSnapshotException -> error.message ?: "Restore snapshot validation failed."
        is IllegalArgumentException -> error.message ?: "Restore configuration is invalid."
        else -> "Restore failed. Local data was not changed."
    }

    private suspend fun saveVerifiedRecoveryPoint(
        snapshot: CloudRestorableSnapshot,
        tenant: com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
    ): SnapshotEnvelope {
        val recoveryEnvelope = SnapshotEnvelope.create(snapshot, tenant)
        RestoreSnapshotValidator.validate(recoveryEnvelope, tenant, allowEmpty = true)
        recoveryPointStore()?.save(recoveryEnvelope)
            ?: throw IllegalStateException("Local recovery storage is unavailable")
        return recoveryEnvelope
    }

    private suspend fun replaceCloudSnapshotWithRollback(
        snapshot: CloudRestorableSnapshot,
        tenant: com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope,
        recoveryEnvelope: SnapshotEnvelope
    ) {
        RestoreRecoveryCoordinator { restored: CloudRestorableSnapshot ->
            repository.replaceCloudRestorableTables(
                categoriesList = restored.categories,
                productsList = restored.products,
                salesList = restored.sales,
                saleItemsList = restored.saleItems,
                customersList = restored.customers,
                udhaarTxsList = restored.udhaarTransactions,
                adjustmentsList = restored.stockAdjustments
            )
        }.replaceWithRollback(snapshot, tenant, recoveryEnvelope)
    }

    fun updateFirebaseSettings(url: String, prefix: String, autoSync: Boolean) {
        viewModelScope.launch {
            val current = settingsDataStore.settingsFlow.first()
            val effectiveUrl = url.trim().ifEmpty { current.firebaseUrl }
            val effectivePrefix = prefix.trim().ifEmpty { current.firebasePrefix }
            settingsDataStore.updateFirebaseConfig(effectiveUrl, effectivePrefix)
            settingsDataStore.updateAutoSyncEnabled(autoSync)
            context?.let { ctx ->
                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncManager.configureAutomaticSync(
                    context = ctx,
                    enabled = current.isUserLoggedIn && autoSync
                )
            }
        }
    }

    fun syncAllToCloud(onResult: (Boolean, String) -> Unit) {
        if (!BuildConfig.CLOUD_SYNC_ENABLED) {
            onResult(false, "Cloud sync is disabled in this debug build.")
            return
        }
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val identitySession = reconcileIdentitySession()
            if (identitySession == null) {
                onResult(false, "No valid authenticated store session is available for backup.")
                return@launch
            }
            val tenant = settingsDataStore.getOrCreateTenantDeviceContext(identitySession).toTenantScope()
            val backupClient = try {
                authenticatedBackupClient(settings, identitySession)
            } catch (error: Exception) {
                onResult(false, backupFailureMessage(error))
                return@launch
            }

            _syncInProgress.value = true
            _syncMessage.value = "Starting Backup..."
            try {
                _syncMessage.value = "Creating verified snapshot..."
                val envelope = SnapshotEnvelope.create(currentCloudRestorableSnapshot(), tenant)
                RestoreSnapshotValidator.validate(envelope, tenant)
                _syncMessage.value = "Uploading authenticated snapshot..."
                backupClient.uploadSnapshot(envelope)
                settingsDataStore.updateLastSyncTime(
                    SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).format(Date())
                )
                _syncMessage.value = "Backup Completed!"
                onResult(true, "Cloud Backup successful!")
            } catch (error: Exception) {
                onResult(false, backupFailureMessage(error))
            } finally {
                _syncInProgress.value = false
                _syncMessage.value = null
            }
        }
    }

    fun restoreAllFromCloud(onResult: (Boolean, String) -> Unit) {
        if (!BuildConfig.CLOUD_SYNC_ENABLED) {
            onResult(false, "Cloud restore is disabled in this debug build.")
            return
        }
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val identitySession = reconcileIdentitySession()
            if (identitySession == null) {
                onResult(false, "No valid authenticated store session is available for restore.")
                return@launch
            }
            val tenant = settingsDataStore.getOrCreateTenantDeviceContext(identitySession).toTenantScope()
            val backupClient = try {
                authenticatedBackupClient(settings, identitySession)
            } catch (error: Exception) {
                onResult(false, backupFailureMessage(error))
                return@launch
            }

            _syncInProgress.value = true
            _syncMessage.value = "Downloading complete snapshot..."
            try {
                _syncMessage.value = "Downloading authenticated snapshot..."
                val remoteEnvelope = backupClient.downloadSnapshot()
                _syncMessage.value = "Validating snapshot integrity..."
                val remoteSnapshot = RestoreSnapshotValidator.validate(remoteEnvelope, tenant)

                _syncMessage.value = "Creating local recovery point..."
                val recoveryEnvelope = saveVerifiedRecoveryPoint(currentCloudRestorableSnapshot(), tenant)

                _syncMessage.value = "Restoring validated database..."
                replaceCloudSnapshotWithRollback(remoteSnapshot, tenant, recoveryEnvelope)

                _syncMessage.value = "Database Restored!"
                onResult(true, "All data successfully synchronized from Cloud!")
            } catch (error: Exception) {
                onResult(false, backupFailureMessage(error))
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
