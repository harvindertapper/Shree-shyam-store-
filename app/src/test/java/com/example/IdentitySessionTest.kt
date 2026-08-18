package com.example

import com.example.data.IdentityProvider
import com.example.data.IdentitySession
import com.example.data.StoreSettings
import com.example.data.identitySessionOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentitySessionTest {
    @Test
    fun localIdentityIsDeterministicAndDoesNotUseEmailAsRawNamespace() {
        val first = IdentitySession.localForUser("  Owner ", "Owner@Example.com")
        val second = IdentitySession.localForUser("Owner", "owner@example.com")

        assertEquals(IdentityProvider.LOCAL, first.provider)
        assertEquals(first.uid, second.uid)
        assertTrue(first.uid.startsWith("local:"))
        assertTrue(first.uid != "owner@example.com")
        assertEquals("Owner", first.username)
        assertEquals("owner@example.com", first.email)
    }

    @Test
    fun normalizationMakesAuthorityAndRoleCanonical() {
        val session = IdentitySession(
            provider = IdentityProvider.FIREBASE,
            uid = " firebase-user ",
            username = "  Owner ",
            email = "Owner@Example.com ",
            role = " cashier "
        ).normalized()

        assertEquals("firebase-user", session.uid)
        assertEquals("Owner", session.username)
        assertEquals("owner@example.com", session.email)
        assertEquals("CASHIER", session.role)
        assertTrue(session.isUsable())
    }

    @Test
    fun persistedSessionRequiresExplicitProviderAndStableUid() {
        val missingProvider = StoreSettings(
            loggedInUid = "firebase-user",
            loggedInUsername = "Owner",
            loggedInEmail = "owner@example.com",
            isUserLoggedIn = true
        )
        val localSession = StoreSettings(
            loggedInUid = "local:123",
            loggedInUsername = "Owner",
            loggedInEmail = "owner@example.com",
            identityProvider = IdentityProvider.LOCAL,
            isUserLoggedIn = true
        )

        assertNull(missingProvider.identitySessionOrNull())
        assertEquals(IdentityProvider.LOCAL, localSession.identitySessionOrNull()?.provider)
    }

    @Test
    fun logoutLikeStateCannotResolveAnIdentity() {
        assertNull(StoreSettings(isUserLoggedIn = false).identitySessionOrNull())
    }
}
