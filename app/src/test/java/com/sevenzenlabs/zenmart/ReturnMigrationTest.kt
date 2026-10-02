package com.sevenzenlabs.zenmart

import android.content.Context
import androidx.room.Room
import com.sevenzenlabs.zenmart.data.AppDatabase
import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.ReturnItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReturnMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "return-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v11DatabaseMigratesToV12AndSupportsReturnEntities() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV5Schema(raw)
        upgradeV5SchemaToV6(raw)
        upgradeV6SchemaToV7(raw)
        raw.execSQL("PRAGMA user_version = 7")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12
            )
            .allowMainThreadQueries()
            .build()

        val returnDao = migrated.returnDao()

        val returnRecord = Return(
            id = 1L,
            globalId = "ret-global-1",
            returnNumber = "RET-001",
            saleId = 10L,
            originalBillNumber = "BILL-001",
            customerId = 2L,
            totalRefundAmount = 1500L,
            refundMode = "CASH",
            refundState = "RECORDED",
            reason = "CUSTOMER_RETURN",
            note = "Returned unused item",
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = false,
            mutationVersion = 1000L,
            mutationDeviceId = "test-device"
        )
        val returnId = returnDao.insertReturn(returnRecord)
        assertEquals(1L, returnId)

        val returnItem = ReturnItem(
            id = 1L,
            globalId = "ret-item-global-1",
            returnId = returnId,
            saleItemId = 20L,
            productId = 5L,
            productNameSnapshot = "Parle-G Biscuit",
            quantityReturned = 2.0,
            unit = "pcs",
            unitPrice = 750L,
            lineRefundTotal = 1500L,
            isSynced = false,
            updatedAt = 1000L,
            isDeleted = false,
            mutationVersion = 1000L,
            mutationDeviceId = "test-device"
        )
        returnDao.insertReturnItems(listOf(returnItem))

        val fetchedReturn = returnDao.getReturnById(1L)
        assertNotNull(fetchedReturn)
        assertEquals("RET-001", fetchedReturn!!.returnNumber)
        assertEquals(1500L, fetchedReturn.totalRefundAmount)
        assertEquals("CASH", fetchedReturn.refundMode)

        val fetchedItems = returnDao.getReturnItemsForReturnList(returnId)
        assertEquals(1, fetchedItems.size)
        assertEquals(2.0, fetchedItems[0].quantityReturned, 0.001)
        assertEquals(1500L, fetchedItems[0].lineRefundTotal)

        val cumulativeReturned = returnDao.getReturnedQuantityForSaleItem(20L)
        assertEquals(2.0, cumulativeReturned, 0.001)

        val expectedReturnIndexes = setOf(
            "index_returns_globalId",
            "index_returns_returnNumber",
            "index_returns_saleId",
            "index_returns_customerId"
        )
        val actualReturnIndexes = indexNames(migrated.openHelper.writableDatabase, "returns")
        expectedReturnIndexes.forEach { index ->
            assertTrue("Missing index $index on returns table", index in actualReturnIndexes)
        }

        val expectedItemIndexes = setOf(
            "index_return_items_globalId",
            "index_return_items_returnId",
            "index_return_items_saleItemId",
            "index_return_items_productId"
        )
        val actualItemIndexes = indexNames(migrated.openHelper.writableDatabase, "return_items")
        expectedItemIndexes.forEach { index ->
            assertTrue("Missing index $index on return_items table", index in actualItemIndexes)
        }

        migrated.close()
    }
}
