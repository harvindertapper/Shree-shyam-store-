package com.sevenzenlabs.zenmart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildConfigurationTest {
    @Test
    fun debugArtifactCannotEnableCloudSync() {
        assertEquals("debug", BuildConfig.BUILD_ENVIRONMENT)
        assertFalse(BuildConfig.CLOUD_SYNC_ENABLED)
    }
}
