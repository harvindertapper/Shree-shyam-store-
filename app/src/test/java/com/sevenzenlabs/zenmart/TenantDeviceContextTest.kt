package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.data.IdentitySession
import com.sevenzenlabs.zenmart.data.TenantContextPolicy
import com.sevenzenlabs.zenmart.data.TenantDeviceContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TenantDeviceContextTest {
    @Test
    fun legacySessionMapping_isStableAndExplicitlyNamespaced() {
        val session = IdentitySession.localForUser("owner", "owner@example.com")
        val context = TenantDeviceContext.fromLegacySession(
            session = session,
            deviceId = "device-1",
            appInstallationId = "install-1"
        )

        assertEquals("legacy-org:${session.uid}", context.organizationId)
        assertEquals("legacy-store:${session.uid}", context.storeId)
        assertEquals("legacy-membership:${session.uid}", context.membershipId)
        assertEquals("device-1", context.deviceId)
        assertEquals("install-1", context.appInstallationId)
        assertEquals(context.organizationId, context.toTenantScope().organizationId)
    }

    @Test
    fun missingScope_isRejectedBeforeAuthorization() {
        val exception = runCatching {
            TenantScope(
                organizationId = "",
                storeId = "store-1",
                membershipId = "membership-1",
                deviceId = "device-1",
                appInstallationId = "install-1"
            )
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun wrongStore_isRejectedButSameStoreDifferentInstallationIsRecognized() {
        val expected = TenantDeviceContext("org-1", "store-1", "membership-1", "device-1", "install-1")
        val sameStore = expected.copy(deviceId = "device-2", appInstallationId = "install-2")
        val wrongStore = expected.copy(storeId = "store-2")

        assertTrue(TenantContextPolicy.sameStore(expected, sameStore))
        assertTrue(TenantContextPolicy.sameInstallation(expected, expected))
        assertFalse(TenantContextPolicy.sameInstallation(expected, sameStore))
        assertFalse(TenantContextPolicy.sameStore(expected, wrongStore))
        assertTrue(runCatching { TenantContextPolicy.requireSameStore(expected, wrongStore) }.isFailure)
    }

    @Test
    fun legacyContext_requiresUsableIdentityAndTrustedDeviceValues() {
        val unusableSession = IdentitySession(
            provider = com.sevenzenlabs.zenmart.data.IdentityProvider.LOCAL,
            uid = "",
            username = "",
            email = ""
        )
        assertTrue(
            runCatching {
                TenantDeviceContext.fromLegacySession(
                    unusableSession,
                    deviceId = "device-1",
                    appInstallationId = "install-1"
                )
            }.isFailure
        )
        assertTrue(
            runCatching {
                TenantDeviceContext("org-1", "store-1", "membership-1", "", "install-1")
            }.isFailure
        )
    }
}
