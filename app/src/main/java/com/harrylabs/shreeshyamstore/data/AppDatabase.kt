package com.harrylabs.shreeshyamstore.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

private const val ROOM_DATABASE_VERSION = 3

@Database(
    entities = [
        Category::class,
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        UdhaarTransaction::class,
        StockAdjustment::class,
        User::class
    ],
    version = ROOM_DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun udhaarDao(): UdhaarDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun userDao(): UserDao

    companion object {
        const val ROOM_SCHEMA_VERSION = ROOM_DATABASE_VERSION
        const val ROOM_V1_RESET_START_VERSION = 1

        internal val DEFAULT_CATEGORY_SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDefaultCategories(db)
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                            seedDefaultCategories(db)
                        }
                    }
                } catch (e: Exception) {
                    // Table might not exist yet if something went wrong
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shree_shyam_store_db"
                )
                .addCallback(DEFAULT_CATEGORY_SEED_CALLBACK)
                .fallbackToDestructiveMigrationFrom(true, 1, 2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val seededCategories = listOf(
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
            seededCategories.forEach { categoryName ->
                db.execSQL(
                    "INSERT INTO categories " +
                        "(localUuid, name, isActive, syncStatus, createdAt, updatedAt) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        UUID.randomUUID().toString(),
                        categoryName,
                        1L,
                        SyncStatus.PENDING,
                        now,
                        now
                    )
                )
            }
        }
    }
}
