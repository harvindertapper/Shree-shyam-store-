package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.data.StoreSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticSyncPolicyTest {
    @Test
    fun newStoreDefaultsToAutomaticSync() {
        assertTrue(StoreSettings().autoSyncEnabled)
    }

    @Test
    fun explicitOperatorOptOutRemainsDisabled() {
        assertFalse(StoreSettings(autoSyncEnabled = false).autoSyncEnabled)
    }
}
