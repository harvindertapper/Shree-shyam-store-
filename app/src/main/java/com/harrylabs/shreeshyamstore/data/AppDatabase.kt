package com.harrylabs.shreeshyamstore.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val ROOM_DATABASE_VERSION = 5

@Database(
    entities = [
        Category::class,
        SyncOutboxOperation::class,
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        UdhaarTransaction::class,
        StockAdjustment::class
    ],
    version = ROOM_DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun udhaarDao(): UdhaarDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao

    companion object {
        const val ROOM_SCHEMA_VERSION = ROOM_DATABASE_VERSION
        const val ROOM_V1_RESET_START_VERSION = 1

        internal val DEFAULT_CATEGORY_SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    seedDefaultCategories(db)
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
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
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS users")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_outbox_operations` (
                        `localUuid` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityUuid` TEXT NOT NULL,
                        `clientOperationId` TEXT NOT NULL,
                        `sourceDeviceId` TEXT NOT NULL,
                        `createdByUid` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`localUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_sync_outbox_operations_shopId_syncStatus_createdAt`
                    ON `sync_outbox_operations` (`shopId`, `syncStatus`, `createdAt`)
                    """.trimIndent()
                )
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
