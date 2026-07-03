package com.harrylabs.shreeshyamstore.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.harrylabs.shreeshyamstore.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShopViewModelTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var shopRepository: ShopRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var fakeFirebaseOwnerRepo: FakeFirebaseOwnerRepository
    private lateinit var viewModel: ShopViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shopRepository = ShopRepository(
            categoryDao = database.categoryDao(),
            syncOutboxDao = database.syncOutboxDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao()
        )
        settingsDataStore = SettingsDataStore(context)
        fakeFirebaseOwnerRepo = FakeFirebaseOwnerRepository()
        viewModel = ShopViewModel(shopRepository, settingsDataStore, fakeFirebaseOwnerRepo, database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun TestScope.flushTasks() {
        for (i in 1..30) {
            Thread.sleep(10)
            runCurrent()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
    }

    private fun TestScope.waitUntil(timeoutMs: Long = 3_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
            runCurrent()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
    }

    @Test
    fun isUserLoggedInMatchesFirebaseState() = runTest {
        assertFalse(viewModel.isUserLoggedIn)
        
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-123", "owner@gmail.com", "Owner Name")
        assertTrue(viewModel.isUserLoggedIn)
        assertEquals("uid-123", viewModel.currentUser?.uid)
    }

    @Test
    fun signInWithNewUserRoutesToSetupScreen() = runTest {
        val identity = OwnerIdentity("uid-new", "new@gmail.com", "New User")
        fakeFirebaseOwnerRepo.signInResult = Result.success(identity)
        fakeFirebaseOwnerRepo.userProfileResult = Result.success(null) // No profile in cloud yet

        viewModel.signInWithGoogle("valid-token", context, {}, {})
        flushTasks()
        assertEquals(Screen.Setup, viewModel.currentScreen.value)
    }

    @Test
    fun signInWithExistingUserRoutesToHomeScreen() = runTest {
        val identity = OwnerIdentity("uid-existing", "existing@gmail.com", "Existing User")
        fakeFirebaseOwnerRepo.signInResult = Result.success(identity)
        fakeFirebaseOwnerRepo.userProfileResult = Result.success(
            UserProfile("uid-existing", "existing@gmail.com", "Existing User", "shop-123")
        )
        fakeFirebaseOwnerRepo.shopProfileResult = Result.success(
            ShopProfile("shop-123", "Shyam General Store", "9876543210", "uid-existing")
        )

        val job = launch { viewModel.storeSettings.collect {} }
        try {
            viewModel.signInWithGoogle("valid-token", context, {}, {})
            flushTasks()
            assertEquals(Screen.Home, viewModel.currentScreen.value)
            val settings = viewModel.storeSettings.value
            assertEquals("shop-123", settings.cachedShopId)
            assertEquals("uid-existing", settings.cachedOwnerUid)
            assertEquals("Shyam General Store", settings.shopName)
            assertEquals("9876543210", settings.ownerPhone)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun createShopPushesInitialShopDataSnapshot() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-owner", "owner@gmail.com", "Owner Name")
        fakeFirebaseOwnerRepo.createShopResult = Result.success(Unit)
        shopRepository.insertCategory(Category(name = "Grocery"))

        var successCalled = false
        viewModel.createShop(
            shopName = "Owner General Store",
            ownerPhone = "9876543210",
            welcomeChantEnabled = true,
            context = context,
            onSuccess = { successCalled = true },
            onError = {}
        )
        flushTasks()

        assertTrue(successCalled)
        assertEquals(1, fakeFirebaseOwnerRepo.pushShopDataCallCount)
        assertTrue(fakeFirebaseOwnerRepo.lastPushedShopId.isNotBlank())
        assertTrue(fakeFirebaseOwnerRepo.lastPushedSnapshot?.categories?.isNotEmpty() == true)
    }

    @Test
    fun saveProductPushesSnapshotUsingPersistedShopSession() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-owner", "owner@gmail.com", "Owner Name")
        shopRepository.insertCategory(Category(localUuid = "cat-grocery", name = "Grocery"))
        settingsDataStore.saveSession("Owner Name", "owner@gmail.com", "uid-owner", "shop-persisted")
        assertEquals("shop-persisted", settingsDataStore.settingsFlow.first().cachedShopId)

        viewModel.saveProduct(
            uuid = null,
            name = "Sugar",
            categoryId = "cat-grocery",
            mrp = 47.0,
            sellingPrice = 47.0,
            purchasePrice = 42.0,
            currentStock = 10,
            trackStock = true,
            lowStockAlertQty = 2,
            isActive = true
        )
        waitUntil { fakeFirebaseOwnerRepo.pushShopDataCallCount == 1 }

        assertTrue(shopRepository.allProducts.first().any { it.name == "Sugar" })
        assertEquals(1, fakeFirebaseOwnerRepo.pushShopDataCallCount)
        assertEquals("shop-persisted", fakeFirebaseOwnerRepo.lastPushedShopId)
        assertTrue(fakeFirebaseOwnerRepo.lastPushedSnapshot?.products?.any { it.name == "Sugar" } == true)
    }

    @Test
    fun saveProductSyncFailureIsExposed() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-owner", "owner@gmail.com", "Owner Name")
        fakeFirebaseOwnerRepo.pushShopDataResult = Result.failure(Exception("Missing or insufficient permissions"))
        shopRepository.insertCategory(Category(localUuid = "cat-grocery", name = "Grocery"))
        settingsDataStore.saveSession("Owner Name", "owner@gmail.com", "uid-owner", "shop-persisted")

        viewModel.saveProduct(
            uuid = null,
            name = "Rice",
            categoryId = "cat-grocery",
            mrp = 60.0,
            sellingPrice = 60.0,
            purchasePrice = 50.0,
            currentStock = 25,
            trackStock = true,
            lowStockAlertQty = 5,
            isActive = true
        )
        waitUntil { viewModel.syncState.value is SyncState.Error }

        assertTrue(viewModel.syncState.value is SyncState.Error)
    }

    @Test
    fun retrySyncNowPushesPendingLocalDataAfterFailure() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-owner", "owner@gmail.com", "Owner Name")
        fakeFirebaseOwnerRepo.pushShopDataResult = Result.failure(Exception("Network unavailable"))
        shopRepository.insertCategory(Category(localUuid = "cat-grocery", name = "Grocery"))
        settingsDataStore.saveSession("Owner Name", "owner@gmail.com", "uid-owner", "shop-persisted")

        viewModel.saveProduct(
            uuid = null,
            name = "Atta",
            categoryId = "cat-grocery",
            mrp = 35.0,
            sellingPrice = 35.0,
            purchasePrice = 30.0,
            currentStock = 20,
            trackStock = true,
            lowStockAlertQty = 4,
            isActive = true
        )
        waitUntil { viewModel.syncState.value is SyncState.Error }

        fakeFirebaseOwnerRepo.pushShopDataResult = Result.success(Unit)
        viewModel.retrySyncNow()
        waitUntil { viewModel.syncState.value is SyncState.Synced && fakeFirebaseOwnerRepo.pushShopDataCallCount == 2 }

        assertEquals(2, fakeFirebaseOwnerRepo.pushShopDataCallCount)
        assertEquals("shop-persisted", fakeFirebaseOwnerRepo.lastPushedShopId)
        assertTrue(fakeFirebaseOwnerRepo.lastPushedSnapshot?.products?.any { it.name == "Atta" } == true)
        assertTrue(viewModel.syncState.value is SyncState.Synced)
    }

    @Test
    fun syncFailureKeepsPendingOutboxAndRetryClearsIt() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-owner", "owner@gmail.com", "Owner Name")
        fakeFirebaseOwnerRepo.pushShopDataResult = Result.failure(Exception("Network unavailable"))
        shopRepository.insertCategory(Category(localUuid = "cat-grocery", name = "Grocery"))
        settingsDataStore.saveSession("Owner Name", "owner@gmail.com", "uid-owner", "shop-persisted")

        viewModel.saveProduct(
            uuid = null,
            name = "Besan",
            categoryId = "cat-grocery",
            mrp = 80.0,
            sellingPrice = 80.0,
            purchasePrice = 70.0,
            currentStock = 12,
            trackStock = true,
            lowStockAlertQty = 3,
            isActive = true
        )
        waitUntil { viewModel.syncState.value is SyncState.Error }

        assertEquals(1, shopRepository.getPendingSyncOperationCount("shop-persisted"))

        fakeFirebaseOwnerRepo.pushShopDataResult = Result.success(Unit)
        viewModel.retrySyncNow()
        waitUntil { viewModel.syncState.value is SyncState.Synced }

        assertEquals(0, shopRepository.getPendingSyncOperationCount("shop-persisted"))
    }

    @Test
    fun amountBasedLooseProductKeepsEnteredAmountAsLineTotal() = runTest {
        val product = Product(
            name = "Sugar",
            categoryId = "cat-grocery",
            mrp = 47.0,
            sellingPrice = 47.0,
            unitType = DataUnitType.WEIGHT,
            displayUnit = DataDisplayUnit.KILOGRAM,
            baseUnit = DataDisplayUnit.GRAM,
            allowsDecimalQuantity = true,
            quantityScale = 3,
            pricePerUnitPaise = 4_700,
            priceUnitBaseQty = 1_000,
            currentStock = 5,
            stockQuantityBase = 5_000,
            trackStock = true
        )

        val added = viewModel.setLooseProductAmountInCart(
            product = product,
            amountPaise = 5_000,
            quantityBase = 1_064,
            enteredQuantityText = "1.064"
        )

        assertTrue(added)
        val line = viewModel.cartState.value[product]
        assertEquals(1_064L, line?.quantityBase)
        assertEquals(5_000L, line?.lineTotalPaise)
        assertEquals("1.064", line?.enteredQuantityText)
    }

    @Test
    fun accountSwitchClearsLocalDatabaseData() = runTest {
        val job = launch { viewModel.storeSettings.collect {} }
        try {
            // 1. Seed some local data and cache User A session
            shopRepository.insertCategory(Category(name = "CustomTestCategory"))
            settingsDataStore.saveSession("User A", "a@gmail.com", "uid-a", "shop-a")
            flushTasks()
            
            // Verify local categories has 1 category
            assertEquals(1, shopRepository.allCategories.first().size)

            // 2. Mock User B signing in with different shop ID
            val identity = OwnerIdentity("uid-b", "b@gmail.com", "User B")
            fakeFirebaseOwnerRepo.signInResult = Result.success(identity)
            fakeFirebaseOwnerRepo.userProfileResult = Result.success(
                UserProfile("uid-b", "b@gmail.com", "User B", "shop-b")
            )
            fakeFirebaseOwnerRepo.shopProfileResult = Result.success(
                ShopProfile("shop-b", "User B Shop", "9876543211", "uid-b")
            )

            viewModel.signInWithGoogle("valid-token", context, {}, {})
            flushTasks()

            // Verify local category 'CustomTestCategory' was wiped and defaults re-seeded
            val categories = shopRepository.allCategories.first()
            assertFalse(categories.any { it.name == "CustomTestCategory" })
            assertTrue(categories.any { it.name == "Namkeen" }) // from default seeds
        } finally {
            job.cancel()
        }
    }

    @Test
    fun onboardingTransactionAbortHandlesErrors() = runTest {
        fakeFirebaseOwnerRepo.mockCurrentUser = OwnerIdentity("uid-1", "owner@gmail.com", "Owner")
        fakeFirebaseOwnerRepo.createShopResult = Result.failure(Exception("Transaction failed due to network"))

        var errorResult: String? = null
        viewModel.createShop("My New Shop", "9876543210", true, context, {}, { error ->
            errorResult = error
        })
        flushTasks()

        assertEquals("Transaction failed due to network", errorResult)
        assertTrue(viewModel.authState.value is AuthState.Error)
    }
}

class FakeFirebaseOwnerRepository : FirebaseOwnerRepository {
    var mockCurrentUser: OwnerIdentity? = null
    var signInResult: Result<OwnerIdentity> = Result.failure(Exception("Not set"))
    var userProfileResult: Result<UserProfile?> = Result.success(null)
    var shopProfileResult: Result<ShopProfile?> = Result.success(null)
    var createShopResult: Result<Unit> = Result.success(Unit)
    var shopDataSnapshotResult: Result<ShopDataSnapshot> = Result.success(ShopDataSnapshot())
    var pushShopDataResult: Result<Unit> = Result.success(Unit)
    var pushShopDataCallCount: Int = 0
    var lastPushedShopId: String = ""
    var lastPushedSnapshot: ShopDataSnapshot? = null

    override fun getCurrentUser(): OwnerIdentity? = mockCurrentUser

    override suspend fun signInWithGoogle(idToken: String): Result<OwnerIdentity> {
        signInResult.onSuccess { mockCurrentUser = it }
        return signInResult
    }

    override suspend fun fetchUserProfile(uid: String): Result<UserProfile?> = userProfileResult

    override suspend fun fetchShopProfile(shopId: String): Result<ShopProfile?> = shopProfileResult

    override suspend fun fetchShopDataSnapshot(shopId: String): Result<ShopDataSnapshot> = shopDataSnapshotResult

    override suspend fun pushShopDataSnapshot(shopId: String, snapshot: ShopDataSnapshot): Result<Unit> {
        pushShopDataCallCount += 1
        lastPushedShopId = shopId
        lastPushedSnapshot = snapshot
        return pushShopDataResult
    }

    override suspend fun createShopAndProfileAtomically(
        uid: String,
        email: String,
        displayName: String,
        shopId: String,
        shopName: String,
        ownerPhone: String
    ): Result<Unit> {
        return createShopResult
    }

    override suspend fun signOut() {
        mockCurrentUser = null
    }
}
