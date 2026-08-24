package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InventoryCatalogRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    private val actor = PlatformActor("owner-1", "Test Owner", "OWNER", "test-device")
    private val tenant = TenantScope("org-test", "store-test", "membership-owner-1", "test-device", "install-test")

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
            shopProfileDao = database.shopProfileDao(),
            authorizationContextProvider = { tenant to actor }
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun productSaveRejectsMissingCategoryWithoutWritingProduct() = runBlocking {
        expectIllegalArgument {
            repository.insertProductWithOpeningStock(
                product = product(categoryId = 404L),
                openingStock = 2.0,
                command = command()
            )
        }

        assertTrue(database.productDao().getAllProducts().first().isEmpty())
    }

    @Test
    fun productSaveRejectsDuplicateActiveBarcode() = runBlocking {
        val categoryId = createCategory("Grocery")
        repository.insertProductWithOpeningStock(
            product = product(categoryId = categoryId, name = "Rice", barcode = "AB-123"),
            openingStock = 2.0,
            command = command()
        )

        expectIllegalArgument {
            repository.insertProductWithOpeningStock(
                product = product(categoryId = categoryId, name = "Flour", barcode = "ab-123"),
                openingStock = 1.0,
                command = command()
            )
        }

        assertEquals(1, database.productDao().getAllProducts().first().size)
    }

    @Test
    fun editingProductMayKeepItsOwnBarcodeAndPreservesUnit() = runBlocking {
        val categoryId = createCategory("Grocery")
        val productId = repository.insertProductWithOpeningStock(
            product = product(categoryId = categoryId, name = "Rice", barcode = "AB-123", unit = "kg"),
            openingStock = 2.5,
            command = command()
        )
        val existing = database.productDao().getProductById(productId)!!

        repository.updateProductWithStockAdjustment(
            product = existing.copy(name = "Basmati Rice", barcode = "ab-123"),
            oldStock = existing.currentStock,
            newStock = 3.5,
            reason = "Edit correction",
            command = command()
        )

        val updated = database.productDao().getProductById(productId)!!
        assertEquals("Basmati Rice", updated.name)
        assertEquals("kg", updated.unit)
        assertEquals("AB-123", updated.barcodeKey)
        assertEquals(3.5, updated.currentStock, 0.0)
    }

    @Test
    fun wholeCountUnitRejectsFractionalOpeningStockWithoutWritingProduct() = runBlocking {
        val categoryId = createCategory("Grocery")

        expectIllegalArgument {
            repository.insertProductWithOpeningStock(
                product = product(categoryId = categoryId, unit = "pcs"),
                openingStock = 1.5,
                command = command()
            )
        }

        assertTrue(database.productDao().getAllProducts().first().isEmpty())
    }

    @Test
    fun stockAdjustmentUpdatesProductAndAuditHistoryTogether() = runBlocking {
        val categoryId = createCategory("Grocery")
        val productId = repository.insertProductWithOpeningStock(
            product = product(categoryId = categoryId, name = "Rice", unit = "kg"),
            openingStock = 3.0,
            command = command()
        )

        repository.adjustProductStock(
            productId = productId,
            actualStockCounted = 2.5,
            reason = "Stock count correction",
            command = command()
        )

        assertEquals(2.5, database.productDao().getProductById(productId)!!.currentStock, 0.0)
        val adjustments = database.stockAdjustmentDao().getAdjustmentsForProduct(productId).first()
        val correction = adjustments.first()
        assertEquals(3.0, correction.oldStock, 0.0)
        assertEquals(2.5, correction.newStock, 0.0)
        assertEquals(-0.5, correction.difference, 0.0)
    }

    @Test
    fun stockAdjustmentRejectsFractionalPiecesBeforeChangingProduct() = runBlocking {
        val categoryId = createCategory("Grocery")
        val productId = repository.insertProductWithOpeningStock(
            product = product(categoryId = categoryId, unit = "pcs"),
            openingStock = 3.0,
            command = command()
        )

        expectIllegalArgument {
            repository.adjustProductStock(
                productId = productId,
                actualStockCounted = 2.5,
                reason = "Stock count correction",
                command = command()
            )
        }

        assertEquals(3.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
        assertEquals(1, database.stockAdjustmentDao().getAdjustmentsForProduct(productId).first().size)
    }

    private suspend fun createCategory(name: String): Long = repository.insertCategory(
        Category(name = name),
        command()
    )

    private fun product(
        categoryId: Long,
        name: String = "Test Product",
        barcode: String = "",
        unit: String = "pcs"
    ) = Product(
        name = name,
        categoryId = categoryId,
        mrp = 1000L,
        sellingPrice = 900L,
        currentStock = 0.0,
        unit = unit,
        trackStock = true,
        barcode = barcode
    )

    private fun command() = CommandMetadata(
        idempotencyKey = UUID.randomUUID().toString(),
        clientEventId = UUID.randomUUID().toString(),
        tenant = tenant,
        actor = actor,
        clientCreatedAt = System.currentTimeMillis()
    )

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected catalog validation to reject the operation")
        } catch (_: IllegalArgumentException) {
            // Expected domain rejection.
        }
    }
}
