package com.sevenzenlabs.zenmart

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.data.AppDatabase
import com.sevenzenlabs.zenmart.data.Category
import com.sevenzenlabs.zenmart.data.Product
import com.sevenzenlabs.zenmart.data.SettingsDataStore
import com.sevenzenlabs.zenmart.data.ShopRepository
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
}
