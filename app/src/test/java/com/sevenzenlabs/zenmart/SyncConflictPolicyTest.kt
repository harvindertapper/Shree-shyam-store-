package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.utils.SyncConflictDisposition
import com.sevenzenlabs.zenmart.utils.SyncConflictPolicy
import com.sevenzenlabs.zenmart.utils.SyncRecordVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncConflictPolicyTest {
    @Test
    fun firstRecordIsApplied() {
        assertEquals(
            SyncConflictDisposition.APPLY,
            SyncConflictPolicy.decide(record(version = 10L), local = null)
        )
    }

    @Test
    fun sameMutationIsAReplay() {
        val record = record(version = 10L, device = "device-a")
        assertEquals(
            SyncConflictDisposition.REPLAY,
            SyncConflictPolicy.decide(record, record.copy(isDeleted = true))
        )
    }

    @Test
    fun newerMutationIsApplied() {
        assertEquals(
            SyncConflictDisposition.APPLY,
            SyncConflictPolicy.decide(
                incoming = record(version = 11L),
                local = record(version = 10L)
            )
        )
    }

    @Test
    fun staleMutationIsRejected() {
        assertEquals(
            SyncConflictDisposition.STALE,
            SyncConflictPolicy.decide(
                incoming = record(version = 9L),
                local = record(version = 10L)
            )
        )
    }

    @Test
    fun sameVersionDifferentDeviceIsConflict() {
        assertEquals(
            SyncConflictDisposition.CONFLICT,
            SyncConflictPolicy.decide(
                incoming = record(version = 10L, device = "device-b"),
                local = record(version = 10L, device = "device-a")
            )
        )
    }

    @Test
    fun newerTombstoneWinsOverOlderLiveRecord() {
        assertEquals(
            SyncConflictDisposition.APPLY,
            SyncConflictPolicy.decide(
                incoming = record(version = 11L, isDeleted = true),
                local = record(version = 10L, isDeleted = false)
            )
        )
    }

    private fun record(
        version: Long,
        device: String = "device-a",
        isDeleted: Boolean = false
    ) = SyncRecordVersion(
        globalId = "global-1",
        mutationVersion = version,
        mutationDeviceId = device,
        isDeleted = isDeleted
    )
}
