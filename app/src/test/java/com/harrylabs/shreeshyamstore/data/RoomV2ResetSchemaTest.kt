package com.harrylabs.shreeshyamstore.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomV2ResetSchemaTest {
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun roomDatabaseVersionIsV2() {
        assertEquals(2, AppDatabase.ROOM_SCHEMA_VERSION)
    }

    @Test
    fun freshRoomV2DatabaseSeedsDefaultCategoriesAndIncludesV2Columns() = runTest {
        val db = createDatabase()

        val categoryNames = db.categoryDao().getAllCategories().first().map { it.name }

        assertTrue(categoryNames.contains("Biscuits"))
        assertTrue(categoryNames.contains("Grocery"))
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "products",
            expectedColumns = setOf(
                "localUuid",
                "remoteId",
                "shopId",
                "syncStatus",
                "deletedAt",
                "lastSyncedAt",
                "createdByUid",
                "updatedByUid",
                "sourceDeviceId",
                "unitType",
                "displayUnit",
                "baseUnit",
                "allowsDecimalQuantity",
                "quantityScale",
                "pricePerUnitPaise",
                "priceUnitBaseQty",
                "purchasePricePerUnitPaise",
                "purchasePriceUnitBaseQty",
                "stockQuantityBase",
                "lowStockAlertBase"
            )
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "sales",
            expectedColumns = setOf(
                "localUuid",
                "remoteId",
                "shopId",
                "syncStatus",
                "deletedAt",
                "lastSyncedAt",
                "createdByUid",
                "updatedByUid",
                "sourceDeviceId",
                "deviceId",
                "billSequence",
                "idempotencyKey",
                "totalAmountPaise",
                "saleStatus"
            )
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "sale_items",
            expectedColumns = setOf(
                "localUuid",
                "remoteId",
                "shopId",
                "syncStatus",
                "deletedAt",
                "lastSyncedAt",
                "createdByUid",
                "updatedByUid",
                "sourceDeviceId",
                "unitTypeSnapshot",
                "displayUnitSnapshot",
                "baseUnitSnapshot",
                "enteredQuantityText",
                "quantityBase",
                "originalPricePerUnitPaise",
                "originalPriceUnitBaseQty",
                "effectivePricePerUnitPaise",
                "effectivePriceUnitBaseQty",
                "rateOverridden",
                "lineTotalPaise",
                "purchasePricePerUnitPaiseSnapshot",
                "purchasePriceUnitBaseQtySnapshot"
            )
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "stock_adjustments",
            expectedColumns = setOf(
                "oldQuantityBase",
                "newQuantityBase",
                "differenceBase",
                "displayUnitSnapshot"
            )
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "udhaar_transactions",
            expectedColumns = setOf("amountPaise")
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "categories",
            expectedColumns = setOf("localUuid", "syncStatus", "deletedAt", "isActive")
        )
        assertV2Columns(
            db = db.openHelper.readableDatabase,
            tableName = "customers",
            expectedColumns = setOf("localUuid", "syncStatus", "deletedAt", "isActive")
        )
    }

    @Test
    fun legacyEntityInputsBackfillV2PaiseAndBaseQuantityFields() {
        val product = Product(
            name = "Sugar",
            categoryId = 1,
            mrp = 47.0,
            sellingPrice = 45.5,
            purchasePrice = 42.25,
            currentStock = 12,
            lowStockAlertQty = 3
        )
        val sale = Sale(
            billNumber = "BILL-1",
            totalAmount = 91.5,
            paymentMode = "CASH"
        )
        val saleItem = SaleItem(
            saleId = 1,
            productId = 2,
            productNameSnapshot = "Sugar",
            quantity = 2,
            unitPrice = 45.5,
            lineTotal = 91.0
        )
        val udhaarTransaction = UdhaarTransaction(
            customerId = 1,
            type = "CREDIT",
            amount = 91.5
        )
        val adjustment = StockAdjustment(
            productId = 2,
            oldStock = 10,
            newStock = 12,
            difference = 2,
            reason = "Opening stock"
        )

        assertFalse(product.localUuid.isBlank())
        assertEquals(4_550L, product.pricePerUnitPaise)
        assertEquals(1L, product.priceUnitBaseQty)
        assertEquals(4_225L, product.purchasePricePerUnitPaise)
        assertEquals(12L, product.stockQuantityBase)
        assertEquals(3L, product.lowStockAlertBase)
        assertEquals(9_150L, sale.totalAmountPaise)
        assertEquals("COMPLETED", sale.saleStatus)
        assertFalse(sale.idempotencyKey.isBlank())
        assertEquals(2L, saleItem.quantityBase)
        assertEquals(4_550L, saleItem.originalPricePerUnitPaise)
        assertEquals(4_550L, saleItem.effectivePricePerUnitPaise)
        assertEquals(9_100L, saleItem.lineTotalPaise)
        assertEquals(9_150L, udhaarTransaction.amountPaise)
        assertEquals(10L, adjustment.oldQuantityBase)
        assertEquals(12L, adjustment.newQuantityBase)
        assertEquals(2L, adjustment.differenceBase)
    }

    private fun createDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DEFAULT_CATEGORY_SEED_CALLBACK)
            .build()
            .also { database = it }
    }

    private fun assertV2Columns(
        db: SupportSQLiteDatabase,
        tableName: String,
        expectedColumns: Set<String>
    ) {
        val actualColumns = db.columnNames(tableName)
        val missingColumns = expectedColumns - actualColumns

        assertTrue(
            "Missing columns in $tableName: ${missingColumns.sorted()}",
            missingColumns.isEmpty()
        )
    }

    private fun SupportSQLiteDatabase.columnNames(tableName: String): Set<String> {
        val cursor = query("PRAGMA table_info(`$tableName`)")
        cursor.use {
            val nameIndex = it.getColumnIndexOrThrow("name")
            val names = mutableSetOf<String>()
            while (it.moveToNext()) {
                names += it.getString(nameIndex)
            }
            return names
        }
    }
}
