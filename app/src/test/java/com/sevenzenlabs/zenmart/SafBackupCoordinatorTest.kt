package com.sevenzenlabs.zenmart

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.data.AppDatabase
import com.sevenzenlabs.zenmart.data.Category
import com.sevenzenlabs.zenmart.data.Customer
import com.sevenzenlabs.zenmart.data.Product
import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.ReturnItem
import com.sevenzenlabs.zenmart.data.Sale
import com.sevenzenlabs.zenmart.data.SaleItem
import com.sevenzenlabs.zenmart.data.SettingsDataStore
import com.sevenzenlabs.zenmart.data.ShopRepository
import com.sevenzenlabs.zenmart.data.StockAdjustment
import com.sevenzenlabs.zenmart.data.UdhaarTransaction
import com.sevenzenlabs.zenmart.recovery.BackupDecryptionException
import com.sevenzenlabs.zenmart.recovery.BackupEnvelopeMalformedException
import com.sevenzenlabs.zenmart.recovery.BackupRetentionPolicy
import com.sevenzenlabs.zenmart.recovery.RecoveryCryptoPolicy
import com.sevenzenlabs.zenmart.recovery.SafBackupCoordinator
import com.sevenzenlabs.zenmart.recovery.SafBackupFileEntry
import com.sevenzenlabs.zenmart.recovery.SafFolderVerification
import com.sevenzenlabs.zenmart.recovery.SafStorageClient
import com.sevenzenlabs.zenmart.utils.LocalRecoveryPointStore
import com.sevenzenlabs.zenmart.utils.SnapshotTenantMismatchException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

class FakeSafStorageClient : SafStorageClient {
    val files = mutableMapOf<Uri, String>()
    var failReadBack = false
    var corruptReadBack = false
    private var uriId = 100

    override fun verifyAndPersistFolder(treeUri: Uri): SafFolderVerification =
        SafFolderVerification(true, "Fake SAF Provider")

    override fun writeBackupFile(treeUri: Uri, filename: String, content: String): Uri {
        val uri = Uri.parse("content://fake.provider/document/${uriId++}/$filename")
        files[uri] = content
        return uri
    }

    override fun readBackupFile(fileUri: Uri): String {
        if (failReadBack) throw IOException("Disk read error")
        val content = files[fileUri] ?: throw IOException("File not found: $fileUri")
        if (corruptReadBack) {
            // Tamper with ciphertext in the JSON envelope
            return content.replace(Regex("\"ciphertextHex\":\"[0-9a-fA-F]{2}"), "\"ciphertextHex\":\"00")
        }
        return content
    }

    override fun listBackupFiles(treeUri: Uri): List<SafBackupFileEntry> =
        files.map { (uri, content) ->
            val name = uri.lastPathSegment ?: "backup.zenmart"
            SafBackupFileEntry(
                filename = name,
                uri = uri,
                lastModifiedEpochMs = System.currentTimeMillis(),
                sizeBytes = content.toByteArray().size.toLong()
            )
        }

    override fun deleteBackupFile(fileUri: Uri): Boolean = files.remove(fileUri) != null

    override fun getProviderDisplayName(treeUri: Uri): String = "Fake SAF Provider"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafBackupCoordinatorTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var recoveryStoreDir: File
    private lateinit var recoveryPointStore: LocalRecoveryPointStore
    private lateinit var fakeStorage: FakeSafStorageClient
    private lateinit var coordinator: SafBackupCoordinator

    private val tenant = TenantScope(
        organizationId = "org-test-1",
        storeId = "store-test-1",
        membershipId = "member-test-1",
        deviceId = "device-test-1",
        appInstallationId = "install-test-1"
    )

    private val validPhrase = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = ShopRepository(
            categoryDao = database.categoryDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao(),
            userDao = database.userDao(),
            database = database,
            shopProfileDao = database.shopProfileDao(),
            returnDao = database.returnDao()
        )

        settingsDataStore = SettingsDataStore(context)

        recoveryStoreDir = File.createTempFile("saf-test-recovery", "").apply {
            delete()
            mkdirs()
        }
        recoveryPointStore = LocalRecoveryPointStore(recoveryStoreDir)
        fakeStorage = FakeSafStorageClient()

        coordinator = SafBackupCoordinator(
            safStorage = fakeStorage,
            settingsDataStore = settingsDataStore,
            repository = repository,
            recoveryPointStore = recoveryPointStore
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        if (::recoveryStoreDir.isInitialized) recoveryStoreDir.deleteRecursively()
    }

    @Test
    fun exportEncryptedBackupRejectsInvalidPhrase() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")
        val invalidPhrase = listOf("not", "twelve", "words")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                coordinator.exportEncryptedBackup(treeUri, invalidPhrase, tenant)
            }
        }
    }

    @Test
    fun exportEncryptedBackupRejectsEmptyDatabaseByDefault() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")
        val result = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant, allowEmpty = false)

        assertFalse(result.success)
        assertTrue(result.message.contains("empty database"))

        val settings = settingsDataStore.settingsFlow.first()
        assertEquals("FAILED", settings.safLastExportStatus)
    }

    @Test
    fun exportEncryptedBackupSucceedsAndPassesImmediateReadBack() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        // Seed initial data
        val cat = Category(id = 1L, globalId = "cat-1", name = "Snacks")
        database.categoryDao().insert(cat)
        val prod = Product(id = 1L, globalId = "prod-1", name = "Biscuits", mrp = 1000L, purchasePrice = 800L, currentStock = 50.0, categoryId = 1L)
        database.productDao().insert(prod)

        val result = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)

        assertTrue(result.success)
        assertTrue(fakeStorage.files.isNotEmpty())

        val settings = settingsDataStore.settingsFlow.first()
        assertEquals("SUCCESS", settings.safLastExportStatus)
        assertEquals(result.filename, settings.safLastExportFilename)
    }

    @Test
    fun exportEncryptedBackupDeletesFileWhenReadBackFails() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        // Seed initial data
        database.categoryDao().insert(Category(id = 1L, globalId = "cat-1", name = "Snacks"))

        fakeStorage.failReadBack = true
        val result = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)

        assertFalse(result.success)
        assertTrue(result.message.contains("Read-back verification failed"))
        assertTrue("Corrupt/failed file must be removed from storage", fakeStorage.files.isEmpty())

        val settings = settingsDataStore.settingsFlow.first()
        assertEquals("FAILED", settings.safLastExportStatus)
    }

    @Test
    fun exportEncryptedBackupDeletesFileWhenReadBackIsCorrupted() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        database.categoryDao().insert(Category(id = 1L, globalId = "cat-1", name = "Snacks"))

        fakeStorage.corruptReadBack = true
        val result = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)

        assertFalse(result.success)
        assertTrue(result.message.contains("Read-back verification failed"))
        assertTrue("Corrupt file must be deleted", fakeStorage.files.isEmpty())
    }

    @Test
    fun restoreEncryptedBackupRejectsWrongStoreId() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        database.categoryDao().insert(Category(id = 1L, globalId = "cat-1", name = "Beverages"))
        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)

        val fileUri = fakeStorage.files.keys.first()
        val otherTenant = tenant.copy(storeId = "different-store-99")

        assertThrows(SnapshotTenantMismatchException::class.java) {
            runBlocking {
                coordinator.restoreEncryptedBackup(fileUri, validPhrase, otherTenant)
            }
        }
    }

    @Test
    fun restoreEncryptedBackupSuccessfullyRestoresBusinessData() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        // Original database state
        val originalCat = Category(id = 10L, globalId = "cat-original", name = "Original Category")
        val originalProd = Product(id = 10L, globalId = "prod-original", name = "Original Product", mrp = 5000L, purchasePrice = 4000L, currentStock = 20.0, categoryId = 10L)
        database.categoryDao().insert(originalCat)
        database.productDao().insert(originalProd)

        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)
        val fileUri = fakeStorage.files.keys.first()

        // Modify local database to simulate new/wiped device
        database.categoryDao().clearAllCategories()
        database.productDao().clearAllProducts()

        assertEquals(0, repository.allCategories.first().size)
        assertEquals(0, repository.allProducts.first().size)

        // Restore backup
        val restoreResult = coordinator.restoreEncryptedBackup(fileUri, validPhrase, tenant)
        assertTrue(restoreResult.success)

        // Verify data was restored
        val restoredCategories = repository.allCategories.first()
        val restoredProducts = repository.allProducts.first()

        assertEquals(1, restoredCategories.size)
        assertEquals("Original Category", restoredCategories.first().name)
        assertEquals(1, restoredProducts.size)
        assertEquals("Original Product", restoredProducts.first().name)
    }

    @Test
    fun restoreEncryptedBackupFailsClosedOnCorruptedPayload() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        val cat = Category(id = 1L, globalId = "cat-1", name = "Snacks")
        database.categoryDao().insert(cat)
        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)

        val fileUri = fakeStorage.files.keys.first()
        val originalContent = fakeStorage.files[fileUri]!!
        // Tamper with ciphertext
        fakeStorage.files[fileUri] = originalContent.replace(
            Regex("\"ciphertextHex\":\"[0-9a-fA-F]{2}"),
            "\"ciphertextHex\":\"00"
        )

        assertThrows(BackupDecryptionException::class.java) {
            runBlocking {
                coordinator.restoreEncryptedBackup(fileUri, validPhrase, tenant)
            }
        }
        // Ensure local database state was NOT cleared or affected
        assertEquals(1, repository.allCategories.first().size)
    }

    @Test
    fun restoreEncryptedBackupFailsClosedOnUnsupportedEnvelopeVersion() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        val cat = Category(id = 1L, globalId = "cat-1", name = "Snacks")
        database.categoryDao().insert(cat)
        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)

        val fileUri = fakeStorage.files.keys.first()
        val originalContent = fakeStorage.files[fileUri]!!
        // Unsupported version 99
        fakeStorage.files[fileUri] = originalContent.replace("\"version\":1", "\"version\":99")

        assertThrows(BackupEnvelopeMalformedException::class.java) {
            runBlocking {
                coordinator.restoreEncryptedBackup(fileUri, validPhrase, tenant)
            }
        }
        assertEquals(1, repository.allCategories.first().size)
    }

    @Test
    fun restoreEncryptedBackupCapturesPreRestorePoint() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        val cat = Category(id = 1L, globalId = "cat-1", name = "Snacks")
        database.categoryDao().insert(cat)
        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)

        val fileUri = fakeStorage.files.keys.first()

        // Modify local state
        database.categoryDao().insert(Category(id = 2L, globalId = "cat-2", name = "Drinks"))
        assertEquals(2, repository.allCategories.first().size)

        val restoreResult = coordinator.restoreEncryptedBackup(fileUri, validPhrase, tenant)
        assertTrue(restoreResult.success)

        // Pre-restore point was saved in recoveryPointStore
        val preRestorePoint = recoveryPointStore.read()
        assertNotNull(preRestorePoint)
        assertEquals(2, preRestorePoint?.tableCounts?.get("categories"))
    }

    @Test
    fun restoreToCleanDeviceRestoresFullBusinessState() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        // Populate complete business data
        val cat = Category(id = 1L, globalId = "cat-1", name = "Snacks")
        database.categoryDao().insert(cat)

        val prod = Product(id = 1L, globalId = "prod-1", name = "Chips", mrp = 2000L, purchasePrice = 1500L, currentStock = 10.0, categoryId = 1L)
        database.productDao().insert(prod)

        val customer = Customer(id = 1L, globalId = "cust-1", name = "Ramesh", phone = "9876543210")
        database.customerDao().insertCustomer(customer)

        val sale = Sale(id = 1L, globalId = "sale-1", billNumber = "BILL-001", customerId = 1L, totalAmount = 2000L, paymentMode = "CASH")
        database.saleDao().insertSale(sale)

        val saleItem = SaleItem(id = 1L, globalId = "item-1", saleId = 1L, productId = 1L, productNameSnapshot = "Chips", quantity = 1.0, unitPrice = 2000L, lineTotal = 2000L)
        database.saleDao().insertSaleItem(saleItem)

        val udhaar = UdhaarTransaction(id = 1L, globalId = "udh-1", customerId = 1L, saleId = 1L, type = "CREDIT", amount = 2000L, balanceEffect = 2000L)
        database.udhaarDao().insertTransaction(udhaar)

        val adjustment = StockAdjustment(id = 1L, globalId = "adj-1", productId = 1L, oldStock = 0.0, newStock = 10.0, difference = 10.0, reason = "Initial count")
        database.stockAdjustmentDao().insertAdjustment(adjustment)

        val ret = Return(id = 1L, globalId = "ret-1", returnNumber = "RET-001", saleId = 1L, originalBillNumber = "BILL-001", customerId = 1L, totalRefundAmount = 2000L, refundMode = "CASH")
        database.returnDao().insertReturn(ret)

        val returnItem = ReturnItem(id = 1L, globalId = "ret-item-1", returnId = 1L, saleItemId = 1L, productId = 1L, productNameSnapshot = "Chips", quantityReturned = 1.0, unitPrice = 2000L, lineRefundTotal = 2000L)
        database.returnDao().insertReturnItems(listOf(returnItem))

        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)
        assertEquals(8, exportResult.tableCounts.size)

        val fileUri = fakeStorage.files.keys.first()

        // Clean second device setup: fresh in-memory Room database and coordinator
        val context: Context = ApplicationProvider.getApplicationContext()
        val cleanDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val cleanRepository = ShopRepository(
            categoryDao = cleanDatabase.categoryDao(),
            productDao = cleanDatabase.productDao(),
            saleDao = cleanDatabase.saleDao(),
            customerDao = cleanDatabase.customerDao(),
            udhaarDao = cleanDatabase.udhaarDao(),
            stockAdjustmentDao = cleanDatabase.stockAdjustmentDao(),
            userDao = cleanDatabase.userDao(),
            database = cleanDatabase,
            shopProfileDao = cleanDatabase.shopProfileDao(),
            returnDao = cleanDatabase.returnDao()
        )
        val cleanRecoveryDir = File.createTempFile("clean-device-recovery", "").apply {
            delete()
            mkdirs()
        }
        val cleanRecoveryStore = LocalRecoveryPointStore(cleanRecoveryDir)
        val cleanSettingsDataStore = SettingsDataStore(context)
        val cleanCoordinator = SafBackupCoordinator(
            safStorage = fakeStorage,
            settingsDataStore = cleanSettingsDataStore,
            repository = cleanRepository,
            recoveryPointStore = cleanRecoveryStore
        )

        // Verify clean device is initially empty
        assertEquals(0, cleanRepository.allCategories.first().size)
        assertEquals(0, cleanRepository.allProducts.first().size)
        assertEquals(0, cleanRepository.allSales.first().size)
        assertEquals(0, cleanRepository.allCustomers.first().size)
        assertEquals(0, cleanRepository.allUdhaarTransactions.first().size)
        assertEquals(0, cleanRepository.getAllStockAdjustmentsList().size)
        assertEquals(0, cleanRepository.allReturnsList().size)

        // Restore onto clean device
        val restoreResult = cleanCoordinator.restoreEncryptedBackup(fileUri, validPhrase, tenant)
        assertTrue(restoreResult.success)

        // Assert all business domains are restored with full fidelity
        assertEquals(1, cleanRepository.allCategories.first().size)
        assertEquals("Snacks", cleanRepository.allCategories.first().first().name)

        assertEquals(1, cleanRepository.allProducts.first().size)
        val restoredProduct = cleanRepository.allProducts.first().first()
        assertEquals("Chips", restoredProduct.name)
        assertEquals(2000L, restoredProduct.mrp)

        assertEquals(1, cleanRepository.allSales.first().size)
        assertEquals("BILL-001", cleanRepository.allSales.first().first().billNumber)

        val restoredSaleItems = cleanRepository.getAllSaleItems()
        assertEquals(1, restoredSaleItems.size)
        assertEquals(1.0, restoredSaleItems.first().quantity, 0.001)

        assertEquals(1, cleanRepository.allCustomers.first().size)
        assertEquals("Ramesh", cleanRepository.allCustomers.first().first().name)

        assertEquals(1, cleanRepository.allUdhaarTransactions.first().size)
        assertEquals(2000L, cleanRepository.allUdhaarTransactions.first().first().amount)

        assertEquals(1, cleanRepository.getAllStockAdjustmentsList().size)
        assertEquals("Initial count", cleanRepository.getAllStockAdjustmentsList().first().reason)

        assertEquals(1, cleanRepository.allReturnsList().size)
        assertEquals("RET-001", cleanRepository.allReturnsList().first().returnNumber)

        val restoredReturnItems = cleanRepository.getAllReturnItemsList()
        assertEquals(1, restoredReturnItems.size)
        assertEquals(2000L, restoredReturnItems.first().lineRefundTotal)

        cleanDatabase.close()
        cleanRecoveryDir.deleteRecursively()
    }

    @Test
    fun restoreToCleanDeviceWithoutPriorTenantAdoptsArchiveTenant() = runBlocking {
        val treeUri = Uri.parse("content://fake.provider/tree/primary%3ABackups")

        val cat = Category(id = 1L, globalId = "cat-1", name = "Groceries")
        database.categoryDao().insert(cat)

        val exportResult = coordinator.exportEncryptedBackup(treeUri, validPhrase, tenant)
        assertTrue(exportResult.success)

        val fileUri = fakeStorage.files.keys.first()

        // Clean second device without any prior tenant setup (expectedTenant = null)
        val context: Context = ApplicationProvider.getApplicationContext()
        val cleanDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val cleanRepository = ShopRepository(
            categoryDao = cleanDatabase.categoryDao(),
            productDao = cleanDatabase.productDao(),
            saleDao = cleanDatabase.saleDao(),
            customerDao = cleanDatabase.customerDao(),
            udhaarDao = cleanDatabase.udhaarDao(),
            stockAdjustmentDao = cleanDatabase.stockAdjustmentDao(),
            userDao = cleanDatabase.userDao(),
            database = cleanDatabase,
            shopProfileDao = cleanDatabase.shopProfileDao(),
            returnDao = cleanDatabase.returnDao()
        )
        val cleanRecoveryDir = File.createTempFile("identity-less-recovery", "").apply {
            delete()
            mkdirs()
        }
        val cleanRecoveryStore = LocalRecoveryPointStore(cleanRecoveryDir)
        val cleanSettingsDataStore = SettingsDataStore(context)
        val cleanCoordinator = SafBackupCoordinator(
            safStorage = fakeStorage,
            settingsDataStore = cleanSettingsDataStore,
            repository = cleanRepository,
            recoveryPointStore = cleanRecoveryStore
        )

        // Restore without passing an expected tenant
        val restoreResult = cleanCoordinator.restoreEncryptedBackup(
            fileUri = fileUri,
            phraseWords = validPhrase,
            expectedTenant = null
        )
        assertTrue(restoreResult.success)
        assertEquals(1, cleanRepository.allCategories.first().size)
        assertEquals("Groceries", cleanRepository.allCategories.first().first().name)

        cleanDatabase.close()
        cleanRecoveryDir.deleteRecursively()
    }
}
