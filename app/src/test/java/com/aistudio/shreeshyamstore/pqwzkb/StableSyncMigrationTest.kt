package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StableSyncMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "stable-sync-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v5RowsReceiveDeterministicGlobalIdentityAndOutboxTable() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV5Schema(raw)
        raw.execSQL("INSERT INTO products (id, name, categoryId, mrp, sellingPrice, purchasePrice, currentStock, unit, trackStock, lowStockAlertQty, barcode, isActive, isSynced, createdAt, updatedAt, isDeleted) VALUES (4, 'Legacy Product', 0, 1000, NULL, NULL, 2.0, 'pcs', 1, 1.0, '', 1, 0, 100, 500, 0)")
        raw.execSQL("PRAGMA user_version = 5")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        val product = migrated.productDao().getProductById(4L)!!
        assertEquals(SyncIdentity.legacyGlobalId("products", 4L), product.globalId)
        assertEquals(500L, product.mutationVersion)
        assertEquals(SyncIdentity.LEGACY_DEVICE_ID, product.mutationDeviceId)
        assertEquals(0, migrated.syncOutboxDao().countByState("PENDING"))
        assertTrue(migrated.openHelper.writableDatabase.query("PRAGMA index_list(products)").use { it.count > 0 })

        migrated.close()
    }


}

internal fun createV5Schema(database: SQLiteDatabase) {
    database.execSQL("CREATE TABLE categories (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE products (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, categoryId INTEGER NOT NULL, mrp INTEGER NOT NULL, sellingPrice INTEGER, purchasePrice INTEGER, currentStock REAL NOT NULL, unit TEXT NOT NULL, trackStock INTEGER NOT NULL, lowStockAlertQty REAL NOT NULL, barcode TEXT NOT NULL, isActive INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE sales (id INTEGER NOT NULL PRIMARY KEY, billNumber TEXT NOT NULL, totalAmount INTEGER NOT NULL, paymentMode TEXT NOT NULL, customerId INTEGER, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE sale_items (id INTEGER NOT NULL PRIMARY KEY, saleId INTEGER NOT NULL, productId INTEGER NOT NULL, productNameSnapshot TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, unitPrice INTEGER NOT NULL, lineTotal INTEGER NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE customers (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, phone TEXT, creditLimit INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE udhaar_transactions (id INTEGER NOT NULL PRIMARY KEY, customerId INTEGER NOT NULL, saleId INTEGER, type TEXT NOT NULL, amount INTEGER NOT NULL, balanceEffect INTEGER NOT NULL DEFAULT 0, note TEXT, correctsEventId TEXT, correctionReason TEXT, actorUid TEXT NOT NULL DEFAULT 'legacy-local', actorName TEXT NOT NULL DEFAULT 'Legacy local record', actorRole TEXT NOT NULL DEFAULT 'OWNER', actorDeviceId TEXT NOT NULL DEFAULT 'legacy-device', isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL, eventId TEXT NOT NULL DEFAULT '')")
    database.execSQL("CREATE TABLE stock_adjustments (id INTEGER NOT NULL PRIMARY KEY, productId INTEGER NOT NULL, oldStock REAL NOT NULL, newStock REAL NOT NULL, difference REAL NOT NULL, reason TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE shop_profiles (uid TEXT NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, ownerName TEXT NOT NULL, ownerPhone TEXT NOT NULL, upiId TEXT NOT NULL, email TEXT NOT NULL, address TEXT NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE users (id INTEGER NOT NULL PRIMARY KEY, uid TEXT NOT NULL, username TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
}

internal fun upgradeV5SchemaToV6(database: SQLiteDatabase) {
    val businessTables = listOf(
        "categories", "products", "sales", "sale_items",
        "customers", "udhaar_transactions", "stock_adjustments"
    )
    businessTables.forEach { table ->
        database.execSQL("ALTER TABLE $table ADD COLUMN globalId TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE $table ADD COLUMN mutationVersion INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE $table ADD COLUMN mutationDeviceId TEXT NOT NULL DEFAULT 'legacy-device'")
        database.execSQL("UPDATE $table SET globalId = 'legacy-' || '$table' || '-' || id WHERE globalId = ''")
        database.execSQL("UPDATE $table SET mutationVersion = updatedAt WHERE mutationVersion = 0")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_globalId ON $table (globalId)")
    }
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS sync_outbox (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            tableName TEXT NOT NULL,
            globalId TEXT NOT NULL,
            localId INTEGER NOT NULL,
            mutationVersion INTEGER NOT NULL,
            mutationDeviceId TEXT NOT NULL,
            idempotencyKey TEXT NOT NULL,
            payloadJson TEXT NOT NULL,
            tombstone INTEGER NOT NULL,
            state TEXT NOT NULL,
            attemptCount INTEGER NOT NULL,
            nextAttemptAt INTEGER NOT NULL,
            leaseUntil INTEGER,
            lastError TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
        """.trimIndent()
    )
    database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_state_nextAttemptAt ON sync_outbox (state, nextAttemptAt)")
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_tableName_globalId_mutationVersion ON sync_outbox (tableName, globalId, mutationVersion)")
}

internal fun upgradeV6SchemaToV7(database: SQLiteDatabase) {
    database.execSQL("ALTER TABLE products ADD COLUMN barcodeKey TEXT")
    database.execSQL(
        """
        UPDATE products
        SET barcodeKey = UPPER(TRIM(barcode))
        WHERE isDeleted = 0
          AND TRIM(barcode) <> ''
          AND id IN (
              SELECT MIN(id)
              FROM products
              WHERE isDeleted = 0 AND TRIM(barcode) <> ''
              GROUP BY UPPER(TRIM(barcode))
              HAVING COUNT(*) = 1
          )
        """.trimIndent()
    )
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcodeKey ON products (barcodeKey)")
}
