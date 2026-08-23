package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncPullBatch
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncPullCoordinator
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncPullStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncPullCoordinatorTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun fetchFailureDoesNotInvokeApplyOrAdvanceCursor() = runBlocking {
        var applied = false
        val coordinator = SyncPullCoordinator(
            fetchBatch = { error("simulated mid-pull failure") },
            applyBatch = {
                applied = true
            }
        )

        val result = coordinator.run(previousCursor = 100L)

        assertEquals(SyncPullStatus.FAILED, result.status)
        assertEquals(100L, result.nextCursor)
        assertFalse(applied)
    }

    @Test
    fun malformedBatchIsRejectedBeforeApply() = runBlocking {
        var applied = false
        val malformed = batch(
            previousCursor = 100L,
            category = category(updatedAt = 0L),
            highWaterMark = 100L
        )
        val coordinator = SyncPullCoordinator(
            fetchBatch = { malformed },
            applyBatch = {
                applied = true
            }
        )

        val result = coordinator.run(previousCursor = 100L)

        assertEquals(SyncPullStatus.FAILED, result.status)
        assertEquals(100L, result.nextCursor)
        assertFalse(applied)
    }

    @Test
    fun duplicateGlobalIdsAreRejectedBeforeApply() = runBlocking {
        var applied = false
        val duplicate = batch(
            previousCursor = 100L,
            categories = listOf(
                category(id = 1L, globalId = "same", updatedAt = 101L),
                category(id = 2L, globalId = "same", updatedAt = 101L)
            ),
            highWaterMark = 101L
        )
        val coordinator = SyncPullCoordinator(
            fetchBatch = { duplicate },
            applyBatch = {
                applied = true
            }
        )

        val result = coordinator.run(previousCursor = 100L)

        assertEquals(SyncPullStatus.FAILED, result.status)
        assertEquals(100L, result.nextCursor)
        assertFalse(applied)
    }

    @Test
    fun equalTimestampDeliveryIsAcceptedWithoutCursorRegression() = runBlocking {
        var applied = false
        val equalTimestamp = batch(
            previousCursor = 100L,
            category = category(updatedAt = 100L),
            highWaterMark = 100L
        )
        val coordinator = SyncPullCoordinator(
            fetchBatch = { equalTimestamp },
            applyBatch = {
                applied = true
            }
        )

        val result = coordinator.run(previousCursor = 100L)

        assertEquals(SyncPullStatus.APPLIED, result.status)
        assertEquals(100L, result.nextCursor)
        assertTrue(applied)
    }

    @Test
    fun noChangeBatchCompletesWithoutMovingCursor() = runBlocking {
        val coordinator = SyncPullCoordinator(
            fetchBatch = {
                SyncPullBatch(
                    categories = emptyList(),
                    products = emptyList(),
                    sales = emptyList(),
                    saleItems = emptyList(),
                    customers = emptyList(),
                    udhaarTransactions = emptyList(),
                    stockAdjustments = emptyList(),
                    highWaterMark = 100L,
                    receivedCount = 0
                )
            },
            applyBatch = { error("no-change batch must not be applied") }
        )

        val result = coordinator.run(previousCursor = 100L)

        assertEquals(SyncPullStatus.NO_CHANGES, result.status)
        assertEquals(100L, result.nextCursor)
    }

    @Test
    fun failedApplyCanBeRetriedWithSameBatchWithoutCursorAdvance() = runBlocking {
        var applyAttempts = 0
        val candidate = batch(previousCursor = 100L, highWaterMark = 120L)
        val coordinator = SyncPullCoordinator(
            fetchBatch = { candidate },
            applyBatch = {
                applyAttempts += 1
                if (applyAttempts == 1) error("simulated Room interruption")
            }
        )

        val first = coordinator.run(previousCursor = 100L)
        val second = coordinator.run(previousCursor = first.nextCursor)

        assertEquals(SyncPullStatus.FAILED, first.status)
        assertEquals(100L, first.nextCursor)
        assertEquals(SyncPullStatus.APPLIED, second.status)
        assertEquals(120L, second.nextCursor)
        assertEquals(2, applyAttempts)
    }

    @Test
    fun duplicateBatchDeliveryIsIdempotentInRoom() = runBlocking {
        val first = batch(
            previousCursor = 100L,
            category = category(id = 1L, globalId = "category-1", name = "Old", updatedAt = 101L),
            highWaterMark = 101L
        )
        val replay = batch(
            previousCursor = 100L,
            category = category(id = 1L, globalId = "category-1", name = "New", updatedAt = 101L),
            highWaterMark = 101L
        )

        first.validate(100L)
        first.applyAtomically(database)
        replay.validate(100L)
        replay.applyAtomically(database)

        val stored = database.categoryDao().getCategoryById(1L)
        assertEquals("New", stored?.name)
        assertEquals(1, database.categoryDao().getAllCategories().first().size)
    }

    private fun batch(
        previousCursor: Long,
        category: Category = category(updatedAt = previousCursor + 1L),
        categories: List<Category> = listOf(category),
        highWaterMark: Long = category.updatedAt
    ) = SyncPullBatch(
        categories = categories,
        products = emptyList(),
        sales = emptyList(),
        saleItems = emptyList(),
        customers = emptyList(),
        udhaarTransactions = emptyList(),
        stockAdjustments = emptyList(),
        highWaterMark = highWaterMark,
        receivedCount = categories.size
    )

    private fun category(
        id: Long = 0L,
        globalId: String = "category-1",
        name: String = "Grocery",
        updatedAt: Long = 101L
    ) = Category(
        id = id,
        globalId = globalId,
        name = name,
        isSynced = true,
        createdAt = updatedAt,
        updatedAt = updatedAt,
        mutationVersion = updatedAt,
        mutationDeviceId = "device-1"
    )
}
