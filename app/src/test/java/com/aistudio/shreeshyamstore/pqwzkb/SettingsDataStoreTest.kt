package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.datastore.preferences.core.clear
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.SettingsDataStore
import com.aistudio.shreeshyamstore.pqwzkb.data.dataStore
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsDataStoreTest {
    @Test
    fun merchantSettingsSaveDoesNotResetExistingPinWhenNoNewPinIsProvided() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.dataStore.edit { it.clear() }
        val settingsDataStore = SettingsDataStore(context)

        settingsDataStore.updateSecurityPin("2468")
        settingsDataStore.updateMerchantSettings(
            shopName = "Store",
            ownerName = "Owner",
            ownerPhone = "9876543210",
            welcomeChantEnabled = true,
            staticPaytmQrImageUri = "",
            autoSyncEnabled = true,
            appLockEnabled = true,
            biometricEnabled = false,
            newSecurityPin = null
        )

        val saved = settingsDataStore.settingsFlow.first()
        val verification = SecurityUtils.verifyCredential(
            secret = "2468",
            storedCredential = saved.securityPin,
            scope = SecurityUtils.CredentialScope.APP_LOCK
        )

        assertTrue(verification.matched)
        assertEquals("Store", saved.shopName)
        assertEquals("Owner", saved.ownerName)
        assertEquals("9876543210", saved.ownerPhone)

        context.dataStore.edit { it.clear() }
    }
}
