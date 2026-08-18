package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ShopProfile::class,
        Category::class,
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        UdhaarTransaction::class,
        StockAdjustment::class,
        User::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopProfileDao(): ShopProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    /** Legacy naming retained for older billing call sites. */
    fun billDao(): SaleDao = saleDao()

    abstract fun customerDao(): CustomerDao
    abstract fun udhaarDao(): UdhaarDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DATABASE_NAME = "shree_shyam_store_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shop_profiles (
                        uid TEXT NOT NULL PRIMARY KEY,
                        shopName TEXT NOT NULL,
                        ownerName TEXT NOT NULL,
                        ownerPhone TEXT NOT NULL,
                        upiId TEXT NOT NULL,
                        email TEXT NOT NULL,
                        address TEXT NOT NULL,
                        isSynced INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products RENAME TO products_v3")
                database.execSQL(
                    """
                    CREATE TABLE products (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        mrp INTEGER NOT NULL,
                        sellingPrice INTEGER,
                        purchasePrice INTEGER,
                        currentStock REAL NOT NULL,
                        unit TEXT NOT NULL,
                        trackStock INTEGER NOT NULL,
                        lowStockAlertQty REAL NOT NULL,
                        barcode TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO products (
                        id, name, categoryId, mrp, sellingPrice, purchasePrice,
                        currentStock, unit, trackStock, lowStockAlertQty, barcode,
                        isActive, isSynced, createdAt, updatedAt, isDeleted
                    )
                    SELECT
                        id, name, categoryId,
                        CAST(ROUND(mrp * 100.0) AS INTEGER),
                        CASE WHEN sellingPrice IS NULL THEN NULL ELSE CAST(ROUND(sellingPrice * 100.0) AS INTEGER) END,
                        CASE WHEN purchasePrice IS NULL THEN NULL ELSE CAST(ROUND(purchasePrice * 100.0) AS INTEGER) END,
                        currentStock, unit, trackStock, lowStockAlertQty, barcode,
                        isActive, isSynced, createdAt, updatedAt, isDeleted
                    FROM products_v3
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE products_v3")

                database.execSQL("ALTER TABLE sales RENAME TO sales_v3")
                database.execSQL(
                    """
                    CREATE TABLE sales (
                        id INTEGER NOT NULL PRIMARY KEY,
                        billNumber TEXT NOT NULL,
                        totalAmount INTEGER NOT NULL,
                        paymentMode TEXT NOT NULL,
                        customerId INTEGER,
                        note TEXT,
                        isSynced INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO sales (
                        id, billNumber, totalAmount, paymentMode, customerId, note,
                        isSynced, createdAt, updatedAt, isDeleted
                    )
                    SELECT
                        id, billNumber, CAST(ROUND(totalAmount * 100.0) AS INTEGER),
                        paymentMode, customerId, note, isSynced, createdAt, updatedAt, isDeleted
                    FROM sales_v3
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE sales_v3")

                database.execSQL("ALTER TABLE sale_items RENAME TO sale_items_v3")
                database.execSQL(
                    """
                    CREATE TABLE sale_items (
                        id INTEGER NOT NULL PRIMARY KEY,
                        saleId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        productNameSnapshot TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        unitPrice INTEGER NOT NULL,
                        lineTotal INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO sale_items (
                        id, saleId, productId, productNameSnapshot, quantity, unit,
                        unitPrice, lineTotal, isSynced, updatedAt, isDeleted
                    )
                    SELECT
                        id, saleId, productId, productNameSnapshot, quantity, unit,
                        CAST(ROUND(unitPrice * 100.0) AS INTEGER),
                        CAST(ROUND(lineTotal * 100.0) AS INTEGER),
                        isSynced, updatedAt, isDeleted
                    FROM sale_items_v3
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE sale_items_v3")

                database.execSQL("ALTER TABLE customers RENAME TO customers_v3")
                database.execSQL(
                    """
                    CREATE TABLE customers (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT,
                        creditLimit INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO customers (
                        id, name, phone, creditLimit, isSynced, createdAt, updatedAt, isDeleted
                    )
                    SELECT
                        id, name, phone, CAST(ROUND(creditLimit * 100.0) AS INTEGER),
                        isSynced, createdAt, updatedAt, isDeleted
                    FROM customers_v3
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE customers_v3")

                database.execSQL("ALTER TABLE udhaar_transactions RENAME TO udhaar_transactions_v3")
                database.execSQL(
                    """
                    CREATE TABLE udhaar_transactions (
                        id INTEGER NOT NULL PRIMARY KEY,
                        customerId INTEGER NOT NULL,
                        saleId INTEGER,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        note TEXT,
                        isSynced INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO udhaar_transactions (
                        id, customerId, saleId, type, amount, note,
                        isSynced, createdAt, updatedAt, isDeleted
                    )
                    SELECT
                        id, customerId, saleId, type, CAST(ROUND(amount * 100.0) AS INTEGER),
                        note, isSynced, createdAt, updatedAt, isDeleted
                    FROM udhaar_transactions_v3
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE udhaar_transactions_v3")
            }
        }

        internal val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN eventId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN balanceEffect INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN correctsEventId TEXT")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN correctionReason TEXT")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN actorUid TEXT NOT NULL DEFAULT 'legacy-local'")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN actorName TEXT NOT NULL DEFAULT 'Legacy local record'")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN actorRole TEXT NOT NULL DEFAULT 'OWNER'")
                database.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN actorDeviceId TEXT NOT NULL DEFAULT 'legacy-device'")
                database.execSQL("UPDATE udhaar_transactions SET eventId = 'legacy-' || id WHERE eventId = ''")
                database.execSQL(
                    "UPDATE udhaar_transactions SET balanceEffect = " +
                        "CASE WHEN type = 'CREDIT' THEN amount " +
                        "WHEN type = 'PAYMENT' THEN -amount ELSE 0 END"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(seedCategoriesCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private fun seedCategoriesCallback() = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                val statement = db.compileStatement(
                    "INSERT INTO categories (name, isSynced, createdAt, updatedAt, isDeleted) " +
                        "VALUES (?, 0, ?, ?, 0)"
                )
                DEFAULT_CATEGORIES.forEach { name ->
                    statement.clearBindings()
                    statement.bindString(1, name)
                    statement.bindLong(2, now)
                    statement.bindLong(3, now)
                    statement.executeInsert()
                }
            }
        }

        private val DEFAULT_CATEGORIES = listOf(
            "Biscuits",
            "Cold Drinks",
            "Namkeen",
            "Dairy",
            "Soap/Shampoo",
            "Stationery",
            "Grocery",
            "Snacks",
            "Household",
            "Miscellaneous"
        )
    }
}
