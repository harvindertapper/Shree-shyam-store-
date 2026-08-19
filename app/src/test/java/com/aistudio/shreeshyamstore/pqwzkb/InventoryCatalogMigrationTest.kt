package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InventoryCatalogMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "inventory-catalog-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v6RowsCanonicalizeOnlyUnambiguousBarcodes() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV5Schema(raw)
        upgradeV5SchemaToV6(raw)
        insertV6Product(raw, 1L, "Unique barcode", "  ab-123  ", isDeleted = false)
        insertV6Product(raw, 2L, "Duplicate one", "dup-9", isDeleted = false)
        insertV6Product(raw, 3L, "Duplicate two", " DUP-9 ", isDeleted = false)
        insertV6Product(raw, 4L, "Blank barcode", "   ", isDeleted = false)
        insertV6Product(raw, 5L, "Deleted barcode", "deleted-1", isDeleted = true)
        raw.execSQL("PRAGMA user_version = 6")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        assertEquals("AB-123", migrated.productDao().getProductById(1L)!!.barcodeKey)
        assertNull(migrated.productDao().getProductById(2L)!!.barcodeKey)
        assertNull(migrated.productDao().getProductById(3L)!!.barcodeKey)
        assertNull(migrated.productDao().getProductById(4L)!!.barcodeKey)
        assertNull(migrated.productDao().getProductById(5L)!!.barcodeKey)
        assertTrue(hasUniqueBarcodeKeyIndex(migrated.openHelper.writableDatabase))

        migrated.close()
    }

    private fun insertV6Product(
        database: SQLiteDatabase,
        id: Long,
        name: String,
        barcode: String,
        isDeleted: Boolean
    ) {
        database.execSQL(
            """
            INSERT INTO products (
                id, globalId, name, categoryId, mrp, sellingPrice, purchasePrice,
                currentStock, unit, trackStock, lowStockAlertQty, barcode, isActive,
                isSynced, createdAt, updatedAt, isDeleted, mutationVersion, mutationDeviceId
            ) VALUES (?, ?, ?, 0, 1000, NULL, NULL, 2.0, 'pcs', 1, 1.0, ?, 1, 0, 100, 500, ?, 500, 'migration-test-device')
            """.trimIndent(),
            arrayOf<Any?>(id, "migration-product-$id", name, barcode, if (isDeleted) 1 else 0)
        )
    }

    private fun hasUniqueBarcodeKeyIndex(database: SupportSQLiteDatabase): Boolean =
        database.query("PRAGMA index_list(products)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val uniqueIndex = cursor.getColumnIndex("unique")
            generateSequence {
                if (cursor.moveToNext()) {
                    cursor.getString(nameIndex) to cursor.getInt(uniqueIndex)
                } else {
                    null
                }
            }.any { (name, unique) -> name == "index_products_barcodeKey" && unique == 1 }
        }
}
