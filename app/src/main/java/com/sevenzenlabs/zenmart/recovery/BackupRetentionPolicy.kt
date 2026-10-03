package com.sevenzenlabs.zenmart.recovery

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class BackupFileRecord(
    val filename: String,
    val createdAtEpochMs: Long,
    val uriString: String? = null
)

data class RetentionResult(
    val retainedDaily: List<BackupFileRecord>,
    val retainedWeekly: List<BackupFileRecord>,
    val filesToPrune: List<BackupFileRecord>
) {
    val allRetained: List<BackupFileRecord>
        get() = retainedDaily + retainedWeekly
}

object BackupRetentionPolicy {
    const val DEFAULT_MAX_DAILY = 7
    const val DEFAULT_MAX_WEEKLY = 4

    private fun dayKey(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    private fun weekKey(epochMs: Long): String {
        val sdf = SimpleDateFormat("YYYY-'W'ww", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    /**
     * Determines which backup files to keep and which to prune according to the
     * 7 daily + 4 weekly retention policy.
     *
     * 1. Files are grouped by calendar day; the latest backup of each distinct day
     *    is picked for up to [maxDaily] (7) distinct days.
     * 2. Remaining backups from earlier weeks (not covered by the daily copies) are
     *    grouped by calendar week; the latest backup of each week is picked for up
     *    to [maxWeekly] (4) distinct weeks.
     * 3. All other backup files are returned in [RetentionResult.filesToPrune].
     */
    fun evaluateRetention(
        allBackups: List<BackupFileRecord>,
        maxDaily: Int = DEFAULT_MAX_DAILY,
        maxWeekly: Int = DEFAULT_MAX_WEEKLY
    ): RetentionResult {
        if (allBackups.isEmpty()) {
            return RetentionResult(emptyList(), emptyList(), emptyList())
        }

        // Sort newest first
        val sorted = allBackups.sortedByDescending { it.createdAtEpochMs }

        // 1. Select up to maxDaily distinct days (latest backup per day)
        val dailyMap = linkedMapOf<String, BackupFileRecord>()
        for (backup in sorted) {
            val dKey = dayKey(backup.createdAtEpochMs)
            if (!dailyMap.containsKey(dKey) && dailyMap.size < maxDaily) {
                dailyMap[dKey] = backup
            }
        }
        val retainedDaily = dailyMap.values.toList()
        val retainedDailySet = retainedDaily.toSet()
        val dailyCoveredWeeks = retainedDaily.map { weekKey(it.createdAtEpochMs) }.toSet()

        // 2. Select up to maxWeekly distinct weeks from older backups not covered by daily
        val remaining = sorted.filter { it !in retainedDailySet }
        val weeklyMap = linkedMapOf<String, BackupFileRecord>()
        for (backup in remaining) {
            val wKey = weekKey(backup.createdAtEpochMs)
            if (!dailyCoveredWeeks.contains(wKey) && !weeklyMap.containsKey(wKey) && weeklyMap.size < maxWeekly) {
                weeklyMap[wKey] = backup
            }
        }
        val retainedWeekly = weeklyMap.values.toList()
        val allRetainedSet = retainedDailySet + weeklyMap.values.toSet()

        // 3. Any files not retained are marked for pruning
        val filesToPrune = sorted.filter { it !in allRetainedSet }

        return RetentionResult(
            retainedDaily = retainedDaily,
            retainedWeekly = retainedWeekly,
            filesToPrune = filesToPrune
        )
    }
}
