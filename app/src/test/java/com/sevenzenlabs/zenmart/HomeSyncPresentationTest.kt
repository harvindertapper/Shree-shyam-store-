package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.data.IdentityProvider
import com.sevenzenlabs.zenmart.data.StoreSettings
import com.sevenzenlabs.zenmart.data.SyncOutboxSummary
import com.sevenzenlabs.zenmart.utils.AppLanguage
import com.sevenzenlabs.zenmart.utils.HomeSyncTone
import com.sevenzenlabs.zenmart.utils.LocaleHelper
import com.sevenzenlabs.zenmart.utils.SyncCursor
import com.sevenzenlabs.zenmart.utils.SyncHealthSnapshot
import com.sevenzenlabs.zenmart.utils.SyncRunStatus
import com.sevenzenlabs.zenmart.utils.homeSyncPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSyncPresentationTest {
    private val strings = LocaleHelper.getStrings(AppLanguage.ENGLISH)
    private val signedIn = StoreSettings(
        isUserLoggedIn = true,
        identityProvider = IdentityProvider.FIREBASE
    )

    @Test
    fun localBuildNeverClaimsCloudRecoveryOrOffersSync() {
        val state = homeSyncPresentation(signedIn, SyncHealthSnapshot.empty(), false, strings, 1_700_000_000_000L)

        assertEquals(strings.homeSyncUnavailable, state.label)
        assertEquals(HomeSyncTone.NEUTRAL, state.tone)
        assertFalse(state.canTrigger)
        assertNull(state.lastConfirmed)
    }

    @Test
    fun localIdentityAndDisabledAutoSyncDoNotOfferMisleadingTap() {
        val now = 1_700_000_000_000L
        val local = homeSyncPresentation(
            signedIn.copy(identityProvider = IdentityProvider.LOCAL),
            SyncHealthSnapshot.empty(), true, strings, now
        )
        val paused = homeSyncPresentation(
            signedIn.copy(autoSyncEnabled = false),
            SyncHealthSnapshot.empty(), true, strings, now
        )

        assertEquals(strings.homeSyncUnavailable, local.label)
        assertFalse(local.canTrigger)
        assertEquals(strings.homeSyncPaused, paused.label)
        assertFalse(paused.canTrigger)
    }

    @Test
    fun firstNoChangesRunDoesNotBecomeGreenWithoutConfirmedCursor() {
        val health = SyncHealthSnapshot.from(
            nowEpochMs = 1_700_000_000_000L,
            lastSyncEpochMs = 0L,
            outbox = SyncOutboxSummary(),
            lastSyncStatus = SyncRunStatus.NO_CHANGES
        )

        val state = homeSyncPresentation(signedIn, health, true, strings, 1_700_000_000_000L)

        assertEquals(strings.settingsHealthNever, state.label)
        assertEquals(HomeSyncTone.NEUTRAL, state.tone)
        assertNull(state.lastConfirmed)
    }

    @Test
    fun pendingAndFailedRunsCannotShowConfirmedGreenState() {
        val now = 1_700_000_000_000L
        val pending = SyncHealthSnapshot.from(now, now - 1_000L, SyncOutboxSummary(pendingCount = 1))
        val failed = SyncHealthSnapshot.from(
            now, now - 1_000L, SyncOutboxSummary(), SyncRunStatus.FAILED
        )

        assertEquals(HomeSyncTone.PENDING, homeSyncPresentation(signedIn, pending, true, strings, now).tone)
        assertEquals(HomeSyncTone.ERROR, homeSyncPresentation(signedIn, failed, true, strings, now).tone)
    }

    @Test
    fun confirmedCursorIsCalledSyncAndProfitIsNotClaimed() {
        val now = 1_700_000_000_000L
        val cursor = SyncCursor.format(now - 1_000L)
        val health = SyncHealthSnapshot.from(now, now - 1_000L, SyncOutboxSummary())
        val state = homeSyncPresentation(signedIn.copy(lastSyncTime = cursor), health, true, strings, now)

        assertEquals(HomeSyncTone.CONFIRMED, state.tone)
        assertTrue(state.lastConfirmed!!.contains(cursor))
        assertFalse(state.label.contains("backup", ignoreCase = true))
        assertFalse(strings.homeReportsSubtitle.contains("profit", ignoreCase = true))
    }

    @Test
    fun oldCursorNeverKeepsTheGreenIndicatorForever() {
        val now = 1_700_000_000_000L
        val old = now - 2 * 24 * 60 * 60 * 1000L
        val cursor = SyncCursor.format(old)
        val health = SyncHealthSnapshot.from(
            now, old, SyncOutboxSummary(), SyncRunStatus.NO_CHANGES
        )

        val state = homeSyncPresentation(signedIn.copy(lastSyncTime = cursor), health, true, strings, now)

        assertEquals(HomeSyncTone.NEUTRAL, state.tone)
        assertEquals(strings.settingsHealthNever, state.label)
    }
}
