package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {
    @Test
    fun hashPin_isDeterministicSha256Hex() {
        val hash = SecurityUtils.hashPin("1234")

        assertEquals(64, hash.length)
        assertEquals(hash, SecurityUtils.hashPin(" 1234 "))
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun verifyPin_acceptsHashedAndLegacyValues() {
        assertTrue(SecurityUtils.verifyPin("1234", SecurityUtils.hashPin("1234")))
        assertTrue(SecurityUtils.verifyPin("1234", "1234"))
        assertTrue(SecurityUtils.verifyPin("1234", ""))
    }

    @Test
    fun verifyPin_rejectsWrongOrMalformedValues() {
        assertFalse(SecurityUtils.verifyPin("0000", SecurityUtils.hashPin("1234")))
        assertFalse(SecurityUtils.verifyPin("1234", "12ab"))
        assertFalse(SecurityUtils.verifyPin("", SecurityUtils.hashPin("1234")))
    }

    @Test
    fun createCredential_isVersionedSaltedAndScopeBound() {
        val first = SecurityUtils.createCredential(
            "2468",
            SecurityUtils.CredentialScope.APP_LOCK
        )
        val second = SecurityUtils.createCredential(
            "2468",
            SecurityUtils.CredentialScope.APP_LOCK
        )

        assertTrue(SecurityUtils.isVersionedCredential(first))
        assertTrue(first.startsWith("v2:app-lock:"))
        assertFalse(first == second)
        assertTrue(
            SecurityUtils.verifyCredential(
                "2468",
                first,
                SecurityUtils.CredentialScope.APP_LOCK
            ).matched
        )
        assertFalse(
            SecurityUtils.verifyCredential(
                "2468",
                first,
                SecurityUtils.CredentialScope.LOCAL_ACCOUNT
            ).matched
        )
    }

    @Test
    fun legacySha256Verification_marksOnlySuccessfulValuesForRehash() {
        val legacy = SecurityUtils.legacyPasswordHash("offline-password")
        val match = SecurityUtils.verifyCredential(
            "offline-password",
            legacy,
            SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )
        val wrong = SecurityUtils.verifyCredential(
            "wrong-password",
            legacy,
            SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )

        assertTrue(match.matched)
        assertTrue(match.needsRehash)
        assertFalse(wrong.matched)
        assertFalse(wrong.needsRehash)
    }
}
