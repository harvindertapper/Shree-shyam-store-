package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.data.SyncOutboxSummary
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncCursor
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncHealth
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncHealthSnapshot
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncObservabilityTest {
    @Test
    fun healthyStateWhenSyncedRecentlyAndNoOutstanding() {
        val now = 1_700_000_000_000L

        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = now,
            lastSyncEpochMs = now - 60 * 60 * 1000L,
            outbox = SyncOutboxSummary()
        )

        assertEquals(SyncHealth.HEALTHY, snapshot.health)
        assertEquals("Sync is healthy.", snapshot.redactedMessage)
    }

    @Test
    fun neverSyncedWhenCursorIsZero() {
        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 0L,
            outbox = SyncOutboxSummary()
        )

        assertEquals(SyncHealth.NEVER_SYNCED, snapshot.health)
    }

    @Test
    fun pendingWhenOutboxHasPendingEntries() {
        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 1_699_999_900_000L,
            outbox = SyncOutboxSummary(pendingCount = 2, inFlightCount = 1)
        )

        assertEquals(SyncHealth.PENDING, snapshot.health)
        assertEquals(3, snapshot.totalOutstanding)
    }

    @Test
    fun retryingWhenOutboxHasRetryableEntries() {
        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 1_699_999_900_000L,
            outbox = SyncOutboxSummary(
                retryableCount = 1,
                nextRetryAtEpochMs = 1_700_000_010_000L
            )
        )

        assertEquals(SyncHealth.RETRYING, snapshot.health)
        assertEquals(1_700_000_010_000L, snapshot.nextRetryAtEpochMs)
    }

    @Test
    fun blockedWhenDeadLetterEntriesExist() {
        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 1_699_999_900_000L,
            outbox = SyncOutboxSummary(deadLetterCount = 3)
        )

        assertEquals(SyncHealth.BLOCKED, snapshot.health)
        assertEquals(3, snapshot.deadLetterCount)
    }

    @Test
    fun conflictCountIsSeparateFromDeadLetterCount() {
        val snapshot = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 1_699_999_900_000L,
            outbox = SyncOutboxSummary(deadLetterCount = 4, conflictCount = 2)
        )

        assertEquals(4, snapshot.deadLetterCount)
        assertEquals(2, snapshot.conflictCount)
    }

    @Test
    fun cursorParsesLegacyFormats() {
        val withSeconds = "19 Aug 2026, 04:05:06 PM"
        val withoutSeconds = "19 Aug 2026, 04:05 PM"
        val expectedWithSeconds = SimpleDateFormat(
            "dd MMM yyyy, hh:mm:ss a",
            Locale.ENGLISH
        ).parse(withSeconds)!!.time
        val expectedWithoutSeconds = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.ENGLISH
        ).parse(withoutSeconds)!!.time

        assertEquals(expectedWithSeconds, SyncCursor.parse(withSeconds))
        assertEquals(expectedWithoutSeconds, SyncCursor.parse(withoutSeconds))
        assertEquals(1_700_000_000_000L, SyncCursor.parse("1700000000000"))
        assertEquals(0L, SyncCursor.parse("Never Synced"))
        assertEquals(0L, SyncCursor.parse(""))
        assertEquals(0L, SyncCursor.parse("not a cursor"))
    }

    @Test
    fun cursorFormatsToTheCanonicalLegacyRepresentation() {
        val epochMs = 1_700_000_000_000L
        val formatted = SyncCursor.format(epochMs)

        assertEquals(epochMs, SyncCursor.parse(formatted))
    }

    @Test
    fun snapshotContainsNoPayloadOrCredentialFields() {
        val fields = SyncHealthSnapshot::class.java.declaredFields
            .map { it.name.lowercase(Locale.ENGLISH) }
        val forbidden = listOf(
            "payloadjson",
            "lasterror",
            "globalid",
            "credential",
            "passwordhash",
            "bearertoken",
            "pinverifier"
        )

        assertFalse(fields.any { field -> forbidden.any(field::contains) })
        assertTrue(fields.contains("redactedMessage".lowercase(Locale.ENGLISH)))
    }
}
