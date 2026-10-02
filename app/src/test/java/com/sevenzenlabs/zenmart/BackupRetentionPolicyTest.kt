package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.recovery.BackupFileRecord
import com.sevenzenlabs.zenmart.recovery.BackupRetentionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BackupRetentionPolicyTest {

    private val oneDayMs = TimeUnit.DAYS.toMillis(1)
    private val oneHourMs = TimeUnit.HOURS.toMillis(1)

    @Test
    fun emptyListReturnsEmptyResult() {
        val result = BackupRetentionPolicy.evaluateRetention(emptyList())
        assertTrue(result.retainedDaily.isEmpty())
        assertTrue(result.retainedWeekly.isEmpty())
        assertTrue(result.filesToPrune.isEmpty())
    }

    @Test
    fun underLimitBackupsAllRetainedAsDaily() {
        val baseTime = 1700000000000L // arbitrary fixed timestamp
        val backups = (0 until 5).map { i ->
            BackupFileRecord(
                filename = "zenmart_saf_backup_day_$i.zenmart",
                createdAtEpochMs = baseTime - (i * oneDayMs)
            )
        }

        val result = BackupRetentionPolicy.evaluateRetention(backups)
        assertEquals(5, result.retainedDaily.size)
        assertEquals(0, result.retainedWeekly.size)
        assertEquals(0, result.filesToPrune.size)
    }

    @Test
    fun multipleBackupsOnSameDayKeepsNewestAndPrunesOlder() {
        val baseTime = 1700000000000L
        val morning = BackupFileRecord("morning.zenmart", baseTime - (10 * oneHourMs))
        val afternoon = BackupFileRecord("afternoon.zenmart", baseTime - (5 * oneHourMs))
        val evening = BackupFileRecord("evening.zenmart", baseTime - (1 * oneHourMs))

        val result = BackupRetentionPolicy.evaluateRetention(listOf(morning, afternoon, evening))
        assertEquals(1, result.retainedDaily.size)
        assertEquals("evening.zenmart", result.retainedDaily.first().filename)
        assertEquals(0, result.retainedWeekly.size)
        assertEquals(2, result.filesToPrune.size)
        assertTrue(result.filesToPrune.contains(morning))
        assertTrue(result.filesToPrune.contains(afternoon))
    }

    @Test
    fun sevenDailyAndFourWeeklyRetentionEnforced() {
        val baseTime = 1700000000000L
        // Generate backups covering 15 consecutive weeks (1 per week)
        val backups = (0 until 15).map { weekIndex ->
            BackupFileRecord(
                filename = "backup_week_$weekIndex.zenmart",
                createdAtEpochMs = baseTime - (weekIndex * 7 * oneDayMs)
            )
        }

        val result = BackupRetentionPolicy.evaluateRetention(backups)
        // 7 distinct days retained in daily
        assertEquals(7, result.retainedDaily.size)
        // 4 distinct weeks retained in weekly from the remaining
        assertEquals(4, result.retainedWeekly.size)
        // 15 - 7 - 4 = 4 pruned
        assertEquals(4, result.filesToPrune.size)
    }

    @Test
    fun allRetainedPropertyContainsDailyAndWeekly() {
        val baseTime = 1700000000000L
        val backups = (0 until 10).map { i ->
            BackupFileRecord(
                filename = "b_$i.zenmart",
                createdAtEpochMs = baseTime - (i * 7 * oneDayMs)
            )
        }

        val result = BackupRetentionPolicy.evaluateRetention(backups)
        assertEquals(result.retainedDaily.size + result.retainedWeekly.size, result.allRetained.size)
        assertTrue(result.allRetained.containsAll(result.retainedDaily))
        assertTrue(result.allRetained.containsAll(result.retainedWeekly))
    }
}
