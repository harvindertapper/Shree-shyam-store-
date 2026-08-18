package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UdhaarAuditMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "udhaar-audit-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v4UdhaarRowsReceiveImmutableAuditDefaults() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV4Schema(raw)
        raw.execSQL("INSERT INTO udhaar_transactions (id, customerId, saleId, type, amount, note, isSynced, createdAt, updatedAt, isDeleted) VALUES (1, 7, NULL, 'PAYMENT', 1000, 'cash', 0, 1000, 1000, 0)")
        raw.execSQL("PRAGMA user_version = 4")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        val transaction = migrated.udhaarDao().getTransactionsForCustomerList(7L).single()

        assertEquals("legacy-1", transaction.eventId)
        assertEquals(-1000L, transaction.balanceEffect)
        assertEquals("legacy-local", transaction.actorUid)
        assertEquals("Legacy local record", transaction.actorName)
        assertEquals("OWNER", transaction.actorRole)
        assertEquals("legacy-device", transaction.actorDeviceId)

        migrated.close()
    }

    private fun createV4Schema(database: SQLiteDatabase) {
        database.execSQL("CREATE TABLE categories (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE products (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, categoryId INTEGER NOT NULL, mrp INTEGER NOT NULL, sellingPrice INTEGER, purchasePrice INTEGER, currentStock REAL NOT NULL, unit TEXT NOT NULL, trackStock INTEGER NOT NULL, lowStockAlertQty REAL NOT NULL, barcode TEXT NOT NULL, isActive INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sales (id INTEGER NOT NULL PRIMARY KEY, billNumber TEXT NOT NULL, totalAmount INTEGER NOT NULL, paymentMode TEXT NOT NULL, customerId INTEGER, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sale_items (id INTEGER NOT NULL PRIMARY KEY, saleId INTEGER NOT NULL, productId INTEGER NOT NULL, productNameSnapshot TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, unitPrice INTEGER NOT NULL, lineTotal INTEGER NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE customers (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, phone TEXT, creditLimit INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE udhaar_transactions (id INTEGER NOT NULL PRIMARY KEY, customerId INTEGER NOT NULL, saleId INTEGER, type TEXT NOT NULL, amount INTEGER NOT NULL, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE stock_adjustments (id INTEGER NOT NULL PRIMARY KEY, productId INTEGER NOT NULL, oldStock REAL NOT NULL, newStock REAL NOT NULL, difference REAL NOT NULL, reason TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE shop_profiles (uid TEXT NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, ownerName TEXT NOT NULL, ownerPhone TEXT NOT NULL, upiId TEXT NOT NULL, email TEXT NOT NULL, address TEXT NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE users (id INTEGER NOT NULL PRIMARY KEY, uid TEXT NOT NULL, username TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    }
}
