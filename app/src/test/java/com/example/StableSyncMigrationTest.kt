package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.utils.SyncIdentity
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
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()

        val product = migrated.productDao().getProductById(4L)!!
        assertEquals(SyncIdentity.legacyGlobalId("products", 4L), product.globalId)
        assertEquals(500L, product.mutationVersion)
        assertEquals(SyncIdentity.LEGACY_DEVICE_ID, product.mutationDeviceId)
        assertEquals(0, migrated.syncOutboxDao().countByState("PENDING"))
        assertTrue(migrated.openHelper.writableDatabase.rawQuery("PRAGMA index_list(products)", null).use { it.count > 0 })

        migrated.close()
    }

    private fun createV5Schema(database: SQLiteDatabase) {
        database.execSQL("CREATE TABLE categories (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE products (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, categoryId INTEGER NOT NULL, mrp INTEGER NOT NULL, sellingPrice INTEGER, purchasePrice INTEGER, currentStock REAL NOT NULL, unit TEXT NOT NULL, trackStock INTEGER NOT NULL, lowStockAlertQty REAL NOT NULL, barcode TEXT NOT NULL, isActive INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sales (id INTEGER NOT NULL PRIMARY KEY, billNumber TEXT NOT NULL, totalAmount INTEGER NOT NULL, paymentMode TEXT NOT NULL, customerId INTEGER, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sale_items (id INTEGER NOT NULL PRIMARY KEY, saleId INTEGER NOT NULL, productId INTEGER NOT NULL, productNameSnapshot TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, unitPrice INTEGER NOT NULL, lineTotal INTEGER NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE customers (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, phone TEXT, creditLimit INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE udhaar_transactions (id INTEGER NOT NULL PRIMARY KEY, customerId INTEGER NOT NULL, saleId INTEGER, type TEXT NOT NULL, amount INTEGER NOT NULL, balanceEffect INTEGER NOT NULL, note TEXT, correctsEventId TEXT, correctionReason TEXT, actorUid TEXT NOT NULL, actorName TEXT NOT NULL, actorRole TEXT NOT NULL, actorDeviceId TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE stock_adjustments (id INTEGER NOT NULL PRIMARY KEY, productId INTEGER NOT NULL, oldStock REAL NOT NULL, newStock REAL NOT NULL, difference REAL NOT NULL, reason TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE shop_profiles (uid TEXT NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, ownerName TEXT NOT NULL, ownerPhone TEXT NOT NULL, upiId TEXT NOT NULL, email TEXT NOT NULL, address TEXT NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE users (id INTEGER NOT NULL PRIMARY KEY, uid TEXT NOT NULL, username TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    }
}
