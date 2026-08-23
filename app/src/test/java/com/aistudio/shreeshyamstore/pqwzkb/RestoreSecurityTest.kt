package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import org.robolectric.RuntimeEnvironment
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopProfile
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import com.aistudio.shreeshyamstore.pqwzkb.data.User
import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudSyncPolicy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreSecurityTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
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
            shopProfileDao = database.shopProfileDao()
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun cloudRestorePreservesDeviceOwnedIdentityAndProfile() = runBlocking {
        database.userDao().insertUser(
            User(username = "owner", email = "owner@example.com", passwordHash = "device-only")
        )
        database.shopProfileDao().upsert(
            ShopProfile(
                uid = "local-owner",
                shopName = "Shree Shyam Store",
                ownerName = "Owner",
                ownerPhone = "9999999999"
            )
        )
        val oldCategoryId = database.categoryDao().insert(Category(name = "Old category"))

        repository.replaceCloudRestorableTables(
            categoriesList = listOf(Category(id = 99L, name = "Restored category")),
            productsList = emptyList(),
            salesList = emptyList(),
            saleItemsList = emptyList(),
            customersList = emptyList(),
            udhaarTxsList = emptyList(),
            adjustmentsList = emptyList()
        )

        assertNull(database.categoryDao().getCategoryById(oldCategoryId))
        assertNotNull(database.categoryDao().getCategoryById(99L))
        assertNotNull(database.userDao().getUserByEmail("owner@example.com"))
        assertNotNull(database.shopProfileDao().getByUid("local-owner"))
    }

    @Test
    fun restoreAppliesParentBeforeChildReferences() = runBlocking {
        val categoryId = 41L
        repository.replaceCloudRestorableTables(
            categoriesList = listOf(Category(id = categoryId, globalId = "category-41", name = "Grocery")),
            productsList = listOf(
                Product(
                    id = 42L,
                    globalId = "product-42",
                    name = "Rice",
                    categoryId = categoryId,
                    mrp = 100L
                )
            ),
            salesList = emptyList(),
            saleItemsList = emptyList(),
            customersList = emptyList(),
            udhaarTxsList = emptyList(),
            adjustmentsList = emptyList()
        )

        assertNotNull(database.productDao().getProductById(42L))
    }

    @Test
    fun orphanRestoreIsRejectedBeforeExistingDataIsCleared() = runBlocking {
        val oldCategoryId = database.categoryDao().insert(Category(name = "Existing"))
        try {
            repository.replaceCloudRestorableTables(
                categoriesList = emptyList(),
                productsList = listOf(
                    Product(
                        id = 99L,
                        globalId = "orphan-product",
                        name = "Orphan",
                        categoryId = 404L,
                        mrp = 100L
                    )
                ),
                salesList = emptyList(),
                saleItemsList = emptyList(),
                customersList = emptyList(),
                udhaarTxsList = emptyList(),
                adjustmentsList = emptyList()
            )
            fail("Expected orphan restore to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: validation runs before any restore delete.
        }
        assertNotNull(database.categoryDao().getCategoryById(oldCategoryId))
    }

    @Test
    fun abortConflictRollsBackRestoreTransaction() = runBlocking {
        val category = Category(id = 1L, globalId = "category-1", name = "Grocery")
        val existing = Product(
            id = 2L,
            globalId = "existing-product",
            name = "Existing",
            categoryId = category.id,
            mrp = 100L
        )
        database.categoryDao().insert(category)
        database.productDao().insert(existing)

        try {
            database.withTransaction {
                database.productDao().clearAllProducts()
                database.productDao().insertAllForRestore(
                    listOf(
                        existing.copy(id = 3L),
                        existing.copy(id = 4L, name = "Conflicting global ID")
                    )
                )
            }
            fail("Expected ABORT conflict")
        } catch (_: Exception) {
            // Expected: Room transaction restores the pre-operation rows.
        }

        assertNotNull(database.productDao().getProductById(existing.id))
    }

    @Test
    fun accountTablesAreNotAllowedAsCloudBusinessData() {
        assertTrue(CloudSyncPolicy.isCloudBusinessTable("products"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("users"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("passwords"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable(""))
    }
}
