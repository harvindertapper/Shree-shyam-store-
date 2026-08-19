package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutbox
import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutboxState
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncOutboxTest {
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
        database.close()
    }

    @Test
    fun duplicateIdempotencyKeyIsIgnoredAndLeaseIsExclusive() = runBlocking {
        val now = 1_000L
        val key = SyncIdentity.idempotencyKey("products", "global-1", 10L)
        val entry = SyncOutbox(
            tableName = "products",
            globalId = "global-1",
            localId = 1L,
            mutationVersion = 10L,
            mutationDeviceId = "device-a",
            idempotencyKey = key,
            payloadJson = "{}",
            tombstone = false,
            nextAttemptAt = now
        )

        val firstId = database.syncOutboxDao().insert(entry)
        val duplicateId = database.syncOutboxDao().insert(entry)
        assertTrue(firstId > 0L)
        assertEquals(-1L, duplicateId)

        val candidate = database.syncOutboxDao().getEligible(now, 10).single()
        assertEquals(1, database.syncOutboxDao().claim(candidate.id, now, now + 60_000L))
        assertEquals(0, database.syncOutboxDao().claim(candidate.id, now, now + 60_000L))
    }

    @Test
    fun deadLettersCanBeRequeuedForOperatorRecovery() = runBlocking {
        val now = 3_000L
        val entry = SyncOutbox(
            tableName = "products",
            globalId = "global-recovery",
            localId = 3L,
            mutationVersion = 12L,
            mutationDeviceId = "device-a",
            idempotencyKey = SyncIdentity.idempotencyKey("products", "global-recovery", 12L),
            payloadJson = "{\"name\":\"redacted\"}",
            tombstone = false,
            nextAttemptAt = now
        )
        database.syncOutboxDao().insert(entry)
        val candidate = database.syncOutboxDao().getEligible(now, 10).single()
        database.syncOutboxDao().claim(candidate.id, now, now + 60_000L)
        database.syncOutboxDao().markDeadLetter(candidate.id, SyncIdentity.MAX_OUTBOX_ATTEMPTS, "CONFLICT", now)

        assertEquals(1, database.syncOutboxDao().requeueDeadLetters(now + 1L))
        val requeued = database.syncOutboxDao().getById(candidate.id)!!
        assertEquals(SyncOutboxState.PENDING, requeued.state)
        assertEquals(0, requeued.attemptCount)
        assertEquals(0L, requeued.nextAttemptAt)
        assertNull(requeued.leaseUntil)
        assertNull(requeued.lastError)
        assertEquals(now + 1L, requeued.updatedAt)
    }

    @Test
    fun retryBackoffAndDeadLetterStateAreExplicit() = runBlocking {
        val now = 2_000L
        val entry = SyncOutbox(
            tableName = "products",
            globalId = "global-2",
            localId = 2L,
            mutationVersion = 11L,
            mutationDeviceId = "device-a",
            idempotencyKey = SyncIdentity.idempotencyKey("products", "global-2", 11L),
            payloadJson = "{}",
            tombstone = false,
            nextAttemptAt = now
        )
        database.syncOutboxDao().insert(entry)
        val candidate = database.syncOutboxDao().getEligible(now, 10).single()
        database.syncOutboxDao().claim(candidate.id, now, now + 60_000L)
        val retryAt = SyncIdentity.nextRetryAt(now, 1)
        database.syncOutboxDao().markRetryable(candidate.id, 1, retryAt, "offline", now)

        val retryable = database.syncOutboxDao().getById(candidate.id)!!
        assertEquals(SyncOutboxState.RETRYABLE, retryable.state)
        assertTrue(retryable.nextAttemptAt > now)

        database.syncOutboxDao().claim(candidate.id, retryAt, retryAt + 60_000L)
        database.syncOutboxDao().markDeadLetter(candidate.id, SyncIdentity.MAX_OUTBOX_ATTEMPTS, "conflict", retryAt)
        assertEquals(SyncOutboxState.DEAD_LETTER, database.syncOutboxDao().getById(candidate.id)!!.state)
    }
}
