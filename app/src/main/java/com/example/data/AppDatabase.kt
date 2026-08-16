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
    version = 3,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(seedCategoriesCallback())
                    // Keep recovery for databases from development snapshots whose
                    // original schema is unavailable, while version 2 -> 3 remains safe.
                    .fallbackToDestructiveMigration()
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
