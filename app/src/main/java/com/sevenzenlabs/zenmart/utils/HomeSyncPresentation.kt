package com.sevenzenlabs.zenmart.utils

import com.sevenzenlabs.zenmart.data.IdentityProvider
import com.sevenzenlabs.zenmart.data.StoreSettings

enum class HomeSyncTone { NEUTRAL, PENDING, ERROR, CONFIRMED }

data class HomeSyncPresentation(
    val label: String,
    val lastConfirmed: String?,
    val tone: HomeSyncTone,
    val canTrigger: Boolean
)

/** Home describes server sync only; an independent backup has its own evidence. */
fun homeSyncPresentation(
    settings: StoreSettings,
    health: SyncHealthSnapshot,
    cloudSyncEnabled: Boolean,
    strings: AppStrings,
    nowEpochMs: Long
): HomeSyncPresentation {
    if (!cloudSyncEnabled || !settings.isUserLoggedIn || settings.identityProvider != IdentityProvider.FIREBASE) {
        return HomeSyncPresentation(strings.homeSyncUnavailable, null, HomeSyncTone.NEUTRAL, false)
    }
    if (!settings.autoSyncEnabled) {
        return HomeSyncPresentation(strings.homeSyncPaused, null, HomeSyncTone.PENDING, false)
    }

    val lastConfirmed = settings.lastSyncTime.takeIf {
        health.lastSyncEpochMs > 0L && SyncCursor.parse(it) > 0L
    }?.let { "${strings.settingsLastSuccess}: $it" }

    val recentAppliedChange = health.lastSyncEpochMs > 0L &&
        nowEpochMs - health.lastSyncEpochMs in 0L..(24 * 60 * 60 * 1000L)
    val (label, tone) = when (health.health) {
        SyncHealth.HEALTHY -> if (recentAppliedChange) {
            strings.settingsHealthHealthy to HomeSyncTone.CONFIRMED
        } else {
            strings.settingsHealthNever to HomeSyncTone.NEUTRAL
        }
        SyncHealth.PENDING -> strings.settingsHealthPending to HomeSyncTone.PENDING
        SyncHealth.RETRYING -> strings.settingsHealthRetrying to HomeSyncTone.ERROR
        SyncHealth.BLOCKED -> strings.settingsHealthBlocked to HomeSyncTone.ERROR
        SyncHealth.NEVER_SYNCED -> strings.settingsHealthNever to HomeSyncTone.NEUTRAL
    }
    return HomeSyncPresentation(label, lastConfirmed, tone, health.health != SyncHealth.BLOCKED)
}
