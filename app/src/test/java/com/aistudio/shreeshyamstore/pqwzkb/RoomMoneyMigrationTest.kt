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
class RoomMoneyMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "money-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v3MajorUnitMoneyMigratesToIntegerPaise() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV3Schema(raw)
        raw.execSQL("INSERT INTO products (id, name, categoryId, mrp, sellingPrice, purchasePrice, currentStock, unit, trackStock, lowStockAlertQty, barcode, isActive, isSynced, createdAt, updatedAt, isDeleted) VALUES (1, 'Tea', 1, 12.345, 11.995, 9.5, 4.0, 'pcs', 1, 5.0, '', 1, 0, 1000, 1000, 0)")
        raw.execSQL("INSERT INTO sales (id, billNumber, totalAmount, paymentMode, customerId, note, isSynced, createdAt, updatedAt, isDeleted) VALUES (1, 'BILL-1', 24.705, 'CASH', NULL, NULL, 0, 1000, 1000, 0)")
        raw.execSQL("INSERT INTO sale_items (id, saleId, productId, productNameSnapshot, quantity, unit, unitPrice, lineTotal, isSynced, updatedAt, isDeleted) VALUES (1, 1, 1, 'Tea', 2.0, 'pcs', 12.345, 24.705, 0, 1000, 0)")
        raw.execSQL("INSERT INTO customers (id, name, phone, creditLimit, isSynced, createdAt, updatedAt, isDeleted) VALUES (1, 'Customer', NULL, 5000.0, 0, 1000, 1000, 0)")
        raw.execSQL("INSERT INTO udhaar_transactions (id, customerId, saleId, type, amount, note, isSynced, createdAt, updatedAt, isDeleted) VALUES (1, 1, NULL, 'PAYMENT', 12.345, 'cash', 0, 1000, 1000, 0)")
        raw.execSQL("PRAGMA user_version = 3")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        val product = migrated.productDao().getProductById(1L)!!
        val sale = migrated.saleDao().getSaleById(1L)!!
        val item = migrated.saleDao().getSaleItemsForSaleList(1L).single()
        val customer = migrated.customerDao().getCustomerById(1L)!!
        val transaction = migrated.udhaarDao().getTransactionsForCustomerList(1L).single()

        assertEquals(1235L, product.mrp)
        assertEquals(1200L, product.sellingPrice)
        assertEquals(950L, product.purchasePrice)
        assertEquals(2471L, sale.totalAmount)
        assertEquals(1235L, item.unitPrice)
        assertEquals(2471L, item.lineTotal)
        assertEquals(500_000L, customer.creditLimit)
        assertEquals(1235L, transaction.amount)

        migrated.close()
    }

    private fun createV3Schema(database: SQLiteDatabase) {
        database.execSQL("CREATE TABLE categories (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE products (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, categoryId INTEGER NOT NULL, mrp REAL NOT NULL, sellingPrice REAL, purchasePrice REAL, currentStock REAL NOT NULL, unit TEXT NOT NULL, trackStock INTEGER NOT NULL, lowStockAlertQty REAL NOT NULL, barcode TEXT NOT NULL, isActive INTEGER NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sales (id INTEGER NOT NULL PRIMARY KEY, billNumber TEXT NOT NULL, totalAmount REAL NOT NULL, paymentMode TEXT NOT NULL, customerId INTEGER, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sale_items (id INTEGER NOT NULL PRIMARY KEY, saleId INTEGER NOT NULL, productId INTEGER NOT NULL, productNameSnapshot TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, unitPrice REAL NOT NULL, lineTotal REAL NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE customers (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, phone TEXT, creditLimit REAL NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE udhaar_transactions (id INTEGER NOT NULL PRIMARY KEY, customerId INTEGER NOT NULL, saleId INTEGER, type TEXT NOT NULL, amount REAL NOT NULL, note TEXT, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE stock_adjustments (id INTEGER NOT NULL PRIMARY KEY, productId INTEGER NOT NULL, oldStock REAL NOT NULL, newStock REAL NOT NULL, difference REAL NOT NULL, reason TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE shop_profiles (uid TEXT NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, ownerName TEXT NOT NULL, ownerPhone TEXT NOT NULL, upiId TEXT NOT NULL, email TEXT NOT NULL, address TEXT NOT NULL, isSynced INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE users (id INTEGER NOT NULL PRIMARY KEY, uid TEXT NOT NULL, username TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, isSynced INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
    }
}
