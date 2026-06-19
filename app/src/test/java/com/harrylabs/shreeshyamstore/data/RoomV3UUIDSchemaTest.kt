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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomV3UUIDSchemaTest {
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun roomDatabaseVersionIsV3() {
        assertEquals(3, AppDatabase.ROOM_SCHEMA_VERSION)
    }

    @Test
    fun freshRoomV3DatabaseSeedsDefaultCategoriesAndIncludesV3Columns() = runTest {
        val db = createDatabase()

        val categoryNames = db.categoryDao().getAllCategories().first().map { it.name }

        assertTrue(categoryNames.contains("Biscuits"))
        assertTrue(categoryNames.contains("Grocery"))
        assertTrue(categoryNames.contains("Dairy"))

        // Verify localUuid is present and legacy id is NOT present
        assertV3Schema(db.openHelper.readableDatabase)
    }

    @Test
    fun destructiveMigrationWorksFromV1() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v1.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // 1. Manually create V1 database using raw SQLite
        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.version = 1
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        rawDb.execSQL("INSERT INTO categories (name, createdAt, updatedAt) VALUES ('V1 Category', 12345, 12345)")
        rawDb.close()

        // 2. Open it via Room builder with destructive fallback from 1
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .fallbackToDestructiveMigrationFrom(true, 1, 2)
            .addCallback(AppDatabase.DEFAULT_CATEGORY_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()

        val categories = db.categoryDao().getAllCategories().first()
        // Verify V1 Category is removed (since database was reset)
        assertFalse(categories.any { it.name == "V1 Category" })
        // Verify default categories are reseeded
        assertTrue(categories.any { it.name == "Grocery" })

        db.close()
        dbFile.delete()
    }

    @Test
    fun destructiveMigrationWorksFromV2() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v2.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // 1. Manually create V2 database using raw SQLite
        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.version = 2
        rawDb.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localUuid TEXT NOT NULL, remoteId TEXT, shopId TEXT, syncStatus TEXT NOT NULL, deletedAt INTEGER, lastSyncedAt INTEGER, createdByUid TEXT, updatedByUid TEXT, sourceDeviceId TEXT, name TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        rawDb.execSQL("INSERT INTO categories (localUuid, syncStatus, name, isActive, createdAt, updatedAt) VALUES ('uuid-123', 'PENDING', 'V2 Category', 1, 12345, 12345)")
        rawDb.close()

        // 2. Open it via Room builder with destructive fallback from 2
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .fallbackToDestructiveMigrationFrom(true, 1, 2)
            .addCallback(AppDatabase.DEFAULT_CATEGORY_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()

        val categories = db.categoryDao().getAllCategories().first()
        // Verify V2 Category is removed
        assertFalse(categories.any { it.name == "V2 Category" })
        // Verify default categories are reseeded
        assertTrue(categories.any { it.name == "Grocery" })

        db.close()
        dbFile.delete()
    }

    @Test
    fun repositoryInsertSaleWithItemsRelationshipIntegrity() = runTest {
        val db = createDatabase()
        val repository = ShopRepository(
            categoryDao = db.categoryDao(),
            productDao = db.productDao(),
            saleDao = db.saleDao(),
            customerDao = db.customerDao(),
            udhaarDao = db.udhaarDao(),
            stockAdjustmentDao = db.stockAdjustmentDao(),
            userDao = db.userDao()
        )

        // 1. Setup Customer & Category & Product
        val category = Category(name = "Drinks")
        db.categoryDao().insert(category)

        val product = Product(
            name = "Cola",
            categoryId = category.localUuid,
            mrp = 40.0,
            sellingPrice = 38.0,
            currentStock = 50,
            trackStock = true
        )
        db.productDao().insert(product)

        val customer = Customer(
            name = "Rajesh Kumar",
            phone = "9876543210"
        )
        db.customerDao().insertCustomer(customer)

        // 2. Insert Sale
        val sale = Sale(
            billNumber = "BILL-001",
            totalAmount = 76.0,
            paymentMode = "UDHAAR",
            customerId = customer.localUuid
        )

        val item = SaleItem(
            saleId = sale.localUuid,
            productId = product.localUuid,
            productNameSnapshot = "Cola",
            quantity = 2,
            unitPrice = 38.0,
            lineTotal = 76.0
        )

        val savedSaleUuid = repository.insertSaleWithItems(sale, listOf(item), customer.localUuid)

        assertEquals(sale.localUuid, savedSaleUuid)

        // 3. Verify Sale & SaleItem insertion and relationships
        val savedSale = repository.getSaleById(savedSaleUuid)
        assertNotNull(savedSale)
        assertEquals("BILL-001", savedSale?.billNumber)
        assertEquals(customer.localUuid, savedSale?.customerId)

        val items = repository.getSaleItemsForSaleList(savedSaleUuid)
        assertEquals(1, items.size)
        assertEquals(product.localUuid, items[0].productId)
        assertEquals(sale.localUuid, items[0].saleId)

        // 4. Verify Stock Adjustment was inserted and stock reduced
        val updatedProduct = db.productDao().getProductById(product.localUuid)
        assertEquals(48, updatedProduct?.currentStock) // 50 - 2

        val adjustments = db.stockAdjustmentDao().getAdjustmentsForProduct(product.localUuid).first()
        assertEquals(1, adjustments.size)
        assertEquals(product.localUuid, adjustments[0].productId)
        assertEquals(-2, adjustments[0].difference)

        // 5. Verify Udhaar Transaction was logged
        val transactions = db.udhaarDao().getTransactionsForCustomer(customer.localUuid).first()
        assertEquals(1, transactions.size)
        assertEquals(customer.localUuid, transactions[0].customerId)
        assertEquals("CREDIT", transactions[0].type)
        assertEquals(76.0, transactions[0].amount, 0.01)
    }

    private fun createDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DEFAULT_CATEGORY_SEED_CALLBACK)
            .build()
            .also { database = it }
    }

    private fun assertV3Schema(db: SupportSQLiteDatabase) {
        val tables = listOf(
            "categories", "products", "sales", "sale_items",
            "customers", "udhaar_transactions", "stock_adjustments"
        )
        for (table in tables) {
            val columns = db.columnNames(table)
            assertTrue("Table $table should have localUuid", columns.contains("localUuid"))
            assertFalse("Table $table should NOT have id column", columns.contains("id"))
        }
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
