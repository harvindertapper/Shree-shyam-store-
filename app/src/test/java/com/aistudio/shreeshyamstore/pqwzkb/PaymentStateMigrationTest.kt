package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentStateMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "payment-state-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v7SalesReceiveDeterministicPaymentLifecycleDefaults() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV5Schema(raw)
        upgradeV5SchemaToV6(raw)
        upgradeV6SchemaToV7(raw)
        insertLegacySale(raw, 1L, "CASH", 1000L, isDeleted = false)
        insertLegacySale(raw, 2L, "UPI", 2500L, isDeleted = false)
        insertLegacySale(raw, 3L, "UDHAAR", 3000L, isDeleted = false)
        insertLegacySale(raw, 4L, "CASH", 500L, isDeleted = true)
        raw.execSQL("PRAGMA user_version = 7")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10
            )
            .allowMainThreadQueries()
            .build()

        val cash = migrated.saleDao().getSaleById(1L)!!
        assertEquals(PaymentState.RECEIVED.wireValue, cash.paymentState)
        assertEquals(1000L, cash.receivedAmount)

        val upi = migrated.saleDao().getSaleById(2L)!!
        assertEquals(PaymentState.RECEIVED.wireValue, upi.paymentState)
        assertEquals(2500L, upi.receivedAmount)

        val udhaar = migrated.saleDao().getSaleById(3L)!!
        assertEquals(PaymentState.NOT_REQUIRED.wireValue, udhaar.paymentState)
        assertEquals(0L, udhaar.receivedAmount)

        val deleted = migrated.saleDao().getSaleById(4L)!!
        assertEquals(PaymentState.PENDING.wireValue, deleted.paymentState)
        assertNull(deleted.receivedAmount)

        migrated.close()
    }

    private fun insertLegacySale(
        database: android.database.sqlite.SQLiteDatabase,
        id: Long,
        paymentMode: String,
        totalAmount: Long,
        isDeleted: Boolean
    ) {
        database.execSQL(
            """
            INSERT INTO sales (
                id, globalId, billNumber, totalAmount, paymentMode, customerId, note,
                isSynced, createdAt, updatedAt, isDeleted, mutationVersion, mutationDeviceId
            ) VALUES (?, ?, ?, ?, ?, NULL, NULL, 0, 100, 500, ?, 500, 'migration-test-device')
            """.trimIndent(),
            arrayOf<Any?>(id, "migration-sale-$id", "LEGACY-$id", totalAmount, paymentMode, if (isDeleted) 1 else 0)
        )
    }
}
