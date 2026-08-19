package com.aistudio.shreeshyamstore.pqwzkb

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
