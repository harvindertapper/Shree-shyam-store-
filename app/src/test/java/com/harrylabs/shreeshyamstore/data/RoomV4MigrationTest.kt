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
class RoomV4MigrationTest {
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun databaseVersionIsV5() {
        assertEquals(5, AppDatabase.ROOM_SCHEMA_VERSION)
    }

    @Test
    fun migration3To5DropsUsersTableAddsOutboxAndPreservesInventoryData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_3_4.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // 1. Manually create V3 database using raw SQLite containing all tables
        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.version = 3
        
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS categories (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS products (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, categoryId TEXT NOT NULL, mrp REAL NOT NULL, sellingPrice REAL, purchasePrice REAL, unitType TEXT NOT NULL DEFAULT 'PACKET', displayUnit TEXT NOT NULL DEFAULT 'pkt', baseUnit TEXT NOT NULL DEFAULT 'pkt', allowsDecimalQuantity INTEGER NOT NULL DEFAULT 0, quantityScale INTEGER NOT NULL DEFAULT 0, pricePerUnitPaise INTEGER NOT NULL DEFAULT 0, priceUnitBaseQty INTEGER NOT NULL DEFAULT 1, purchasePricePerUnitPaise INTEGER, purchasePriceUnitBaseQty INTEGER, currentStock INTEGER NOT NULL, stockQuantityBase INTEGER NOT NULL DEFAULT 0, trackStock INTEGER NOT NULL, lowStockAlertQty INTEGER NOT NULL, lowStockAlertBase INTEGER NOT NULL DEFAULT 0, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS sales (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, deviceId TEXT, billSequence INTEGER, idempotencyKey TEXT NOT NULL DEFAULT '', billNumber TEXT NOT NULL DEFAULT '', totalAmount REAL NOT NULL DEFAULT 0.0, totalAmountPaise INTEGER NOT NULL DEFAULT 0, paymentMode TEXT NOT NULL DEFAULT 'CASH', saleStatus TEXT NOT NULL DEFAULT 'COMPLETED', customerId TEXT, note TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS sale_items (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, saleId TEXT NOT NULL, productId TEXT NOT NULL, productNameSnapshot TEXT NOT NULL DEFAULT '', quantity INTEGER NOT NULL DEFAULT 1, unitTypeSnapshot TEXT NOT NULL DEFAULT 'PACKET', displayUnitSnapshot TEXT NOT NULL DEFAULT 'pkt', baseUnitSnapshot TEXT NOT NULL DEFAULT 'pkt', enteredQuantityText TEXT NOT NULL DEFAULT '1', quantityBase INTEGER NOT NULL DEFAULT 1, unitPrice REAL NOT NULL DEFAULT 0.0, originalPricePerUnitPaise INTEGER NOT NULL DEFAULT 0, originalPriceUnitBaseQty INTEGER NOT NULL DEFAULT 1, effectivePricePerUnitPaise INTEGER NOT NULL DEFAULT 0, effectivePriceUnitBaseQty INTEGER NOT NULL DEFAULT 1, rateOverridden INTEGER NOT NULL DEFAULT 0, lineTotal REAL NOT NULL DEFAULT 0.0, lineTotalPaise INTEGER NOT NULL DEFAULT 0, purchasePricePerUnitPaiseSnapshot INTEGER, purchasePriceUnitBaseQtySnapshot INTEGER, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS customers (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, phone TEXT, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS udhaar_transactions (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, customerId TEXT NOT NULL, saleId TEXT, type TEXT NOT NULL, amount REAL NOT NULL, amountPaise INTEGER NOT NULL DEFAULT 0, note TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS stock_adjustments (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, productId TEXT NOT NULL, oldStock INTEGER NOT NULL, oldQuantityBase INTEGER NOT NULL DEFAULT 0, newStock INTEGER NOT NULL, newQuantityBase INTEGER NOT NULL DEFAULT 0, difference INTEGER NOT NULL, differenceBase INTEGER NOT NULL DEFAULT 0, displayUnitSnapshot TEXT NOT NULL DEFAULT 'pkt', reason TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        
        // Seed V3 data
        rawDb.execSQL("INSERT INTO users (username, email, passwordHash, createdAt) VALUES ('owner1', 'owner@gmail.com', 'hash', 12345)")
        rawDb.execSQL("INSERT INTO categories (localUuid, name, isActive, syncStatus, createdAt, updatedAt) VALUES ('cat-123', 'Snacks', 1, 'PENDING', 12345, 12345)")
        rawDb.execSQL("INSERT INTO products (localUuid, name, categoryId, mrp, currentStock, trackStock, lowStockAlertQty, isActive, createdAt, updatedAt) VALUES ('prod-abc', 'Chips', 'cat-123', 20.0, 10, 1, 3, 1, 12345, 12345)")
        
        rawDb.close()

        // 2. Open it via Room database builder applying MIGRATION_3_4 and MIGRATION_4_5
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigrationFrom(true, 1, 2)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

        // 3. Verify users table is successfully dropped
        val readableDb = db.openHelper.readableDatabase
        assertFalse(tableExists(readableDb, "users"))

        // 4. Verify categories and products tables exist and their data is fully preserved
        assertTrue(tableExists(readableDb, "categories"))
        assertTrue(tableExists(readableDb, "products"))
        assertTrue(tableExists(readableDb, "sync_outbox_operations"))

        val categories = db.categoryDao().getAllCategories().first()
        assertTrue(categories.any { it.name == "Snacks" && it.localUuid == "cat-123" })

        val products = db.productDao().getAllProducts().first()
        assertTrue(products.any { it.name == "Chips" && it.localUuid == "prod-abc" && it.categoryId == "cat-123" && it.mrp == 20.0 })

        db.close()
        dbFile.delete()
    }

    @Test
    fun migration4To5AddsOutboxAndPreservesInventoryData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_4_5.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.version = 4
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS categories (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS products (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, categoryId TEXT NOT NULL, mrp REAL NOT NULL, sellingPrice REAL, purchasePrice REAL, unitType TEXT NOT NULL DEFAULT 'PIECE', displayUnit TEXT NOT NULL DEFAULT 'PIECE', baseUnit TEXT NOT NULL DEFAULT 'PIECE', allowsDecimalQuantity INTEGER NOT NULL DEFAULT 0, quantityScale INTEGER NOT NULL DEFAULT 0, pricePerUnitPaise INTEGER NOT NULL DEFAULT 0, priceUnitBaseQty INTEGER NOT NULL DEFAULT 1, purchasePricePerUnitPaise INTEGER, purchasePriceUnitBaseQty INTEGER, currentStock INTEGER NOT NULL, stockQuantityBase INTEGER NOT NULL DEFAULT 0, trackStock INTEGER NOT NULL, lowStockAlertQty INTEGER NOT NULL, lowStockAlertBase INTEGER NOT NULL DEFAULT 0, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS sales (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, deviceId TEXT, billSequence INTEGER, idempotencyKey TEXT NOT NULL DEFAULT '', billNumber TEXT NOT NULL DEFAULT '', totalAmount REAL NOT NULL DEFAULT 0.0, totalAmountPaise INTEGER NOT NULL DEFAULT 0, paymentMode TEXT NOT NULL DEFAULT 'CASH', saleStatus TEXT NOT NULL DEFAULT 'COMPLETED', customerId TEXT, note TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS sale_items (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, saleId TEXT NOT NULL, productId TEXT NOT NULL, productNameSnapshot TEXT NOT NULL DEFAULT '', quantity INTEGER NOT NULL DEFAULT 1, unitTypeSnapshot TEXT NOT NULL DEFAULT 'PIECE', displayUnitSnapshot TEXT NOT NULL DEFAULT 'PIECE', baseUnitSnapshot TEXT NOT NULL DEFAULT 'PIECE', enteredQuantityText TEXT NOT NULL DEFAULT '1', quantityBase INTEGER NOT NULL DEFAULT 1, unitPrice REAL NOT NULL DEFAULT 0.0, originalPricePerUnitPaise INTEGER NOT NULL DEFAULT 0, originalPriceUnitBaseQty INTEGER NOT NULL DEFAULT 1, effectivePricePerUnitPaise INTEGER NOT NULL DEFAULT 0, effectivePriceUnitBaseQty INTEGER NOT NULL DEFAULT 1, rateOverridden INTEGER NOT NULL DEFAULT 0, lineTotal REAL NOT NULL DEFAULT 0.0, lineTotalPaise INTEGER NOT NULL DEFAULT 0, purchasePricePerUnitPaiseSnapshot INTEGER, purchasePriceUnitBaseQtySnapshot INTEGER, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS customers (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, phone TEXT, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS udhaar_transactions (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, customerId TEXT NOT NULL, saleId TEXT, type TEXT NOT NULL, amount REAL NOT NULL, amountPaise INTEGER NOT NULL DEFAULT 0, note TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS stock_adjustments (localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL DEFAULT 'PENDING', deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, productId TEXT NOT NULL, oldStock INTEGER NOT NULL, oldQuantityBase INTEGER NOT NULL DEFAULT 0, newStock INTEGER NOT NULL, newQuantityBase INTEGER NOT NULL DEFAULT 0, difference INTEGER NOT NULL, differenceBase INTEGER NOT NULL DEFAULT 0, displayUnitSnapshot TEXT NOT NULL DEFAULT 'PIECE', reason TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(localUuid))")
        rawDb.execSQL("INSERT INTO categories (localUuid, name, isActive, syncStatus, createdAt, updatedAt) VALUES ('cat-456', 'Grocery', 1, 'PENDING', 12345, 12345)")
        rawDb.execSQL("INSERT INTO products (localUuid, name, categoryId, mrp, currentStock, trackStock, lowStockAlertQty, isActive, createdAt, updatedAt) VALUES ('prod-456', 'Atta', 'cat-456', 35.0, 20, 1, 4, 1, 12345, 12345)")
        rawDb.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

        val readableDb = db.openHelper.readableDatabase
        assertTrue(tableExists(readableDb, "sync_outbox_operations"))
        assertTrue(db.categoryDao().getAllCategories().first().any { it.localUuid == "cat-456" })
        assertTrue(db.productDao().getAllProducts().first().any { it.localUuid == "prod-456" })

        db.close()
        dbFile.delete()
    }

    @Test
    fun destructiveMigrationWorksFromV1ToV5() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v1_v5.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // 1. Create a version 1 database structure
        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.version = 1
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        rawDb.execSQL("INSERT INTO categories (name, createdAt, updatedAt) VALUES ('V1 Category', 12345, 12345)")
        rawDb.close()

        // 2. Open it with version 5 (destructive reset on version 1, 2)
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigrationFrom(true, 1, 2)
            .addCallback(AppDatabase.DEFAULT_CATEGORY_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()

        val categories = db.awaitCategoryNamed("Grocery")
        // Verify V1 database got cleared and default categories were re-seeded
        assertFalse(categories.any { it.name == "V1 Category" })
        assertTrue(categories.any { it.name == "Grocery" })

        db.close()
        dbFile.delete()
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            return cursor.count > 0
        }
    }

    private suspend fun AppDatabase.awaitCategoryNamed(categoryName: String): List<Category> {
        repeat(50) {
            val categories = categoryDao().getAllCategories().first()
            if (categories.any { it.name == categoryName }) {
                return categories
            }
            Thread.sleep(20)
        }
        return categoryDao().getAllCategories().first()
    }
}
