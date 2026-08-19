package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudSyncPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMigrationTest {
    @Test
    fun legacyLocalPassword_migratesToScopedVersionedCredential() {
        val legacy = SecurityUtils.legacyPasswordHash("correct horse")

        val legacyResult = SecurityUtils.verifyCredential(
            secret = "correct horse",
            storedCredential = legacy,
            scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )
        assertTrue(legacyResult.matched)
        assertTrue(legacyResult.needsRehash)

        val upgraded = SecurityUtils.createCredential(
            secret = "correct horse",
            scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )
        val upgradedResult = SecurityUtils.verifyCredential(
            secret = "correct horse",
            storedCredential = upgraded,
            scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )
        assertTrue(SecurityUtils.isVersionedCredential(upgraded))
        assertTrue(upgraded.startsWith("v2:local-account:"))
        assertTrue(upgradedResult.matched)
        assertFalse(upgradedResult.needsRehash)
        assertNotEquals(upgraded, SecurityUtils.createCredential("correct horse", SecurityUtils.CredentialScope.LOCAL_ACCOUNT))
    }

    @Test
    fun verifier_rejectsWrongSecretBlankCredentialAndCrossAuthorityMixing() {
        val localCredential = SecurityUtils.createCredential(
            secret = "correct horse",
            scope = SecurityUtils.CredentialScope.LOCAL_ACCOUNT
        )
        val appLockCredential = SecurityUtils.createCredential(
            secret = "2468",
            scope = SecurityUtils.CredentialScope.APP_LOCK
        )

        assertFalse(
            SecurityUtils.verifyCredential(
                "wrong horse",
                localCredential,
                SecurityUtils.CredentialScope.LOCAL_ACCOUNT
            ).matched
        )
        assertFalse(
            SecurityUtils.verifyCredential(
                "correct horse",
                "",
                SecurityUtils.CredentialScope.LOCAL_ACCOUNT
            ).matched
        )
        assertFalse(
            SecurityUtils.verifyCredential(
                "correct horse",
                appLockCredential,
                SecurityUtils.CredentialScope.LOCAL_ACCOUNT
            ).matched
        )
        assertFalse(
            SecurityUtils.verifyCredential(
                "2468",
                localCredential,
                SecurityUtils.CredentialScope.APP_LOCK
            ).matched
        )
    }

    @Test
    fun defaultPinFallback_isOnlyMigrationWindowAndDisappearsAfterUpgrade() {
        val legacyDefault = SecurityUtils.verifyCredential(
            secret = SecurityUtils.DEFAULT_PIN,
            storedCredential = "",
            scope = SecurityUtils.CredentialScope.APP_LOCK,
            allowDefaultPinFallback = true
        )
        assertTrue(legacyDefault.matched)
        assertTrue(legacyDefault.needsRehash)

        val upgraded = SecurityUtils.createCredential(
            SecurityUtils.DEFAULT_PIN,
            SecurityUtils.CredentialScope.APP_LOCK
        )
        assertTrue(
            SecurityUtils.verifyCredential(
                SecurityUtils.DEFAULT_PIN,
                upgraded,
                SecurityUtils.CredentialScope.APP_LOCK,
                allowDefaultPinFallback = false
            ).matched
        )
        assertFalse(
            SecurityUtils.verifyCredential(
                SecurityUtils.DEFAULT_PIN,
                "",
                SecurityUtils.CredentialScope.APP_LOCK,
                allowDefaultPinFallback = false
            ).matched
        )
    }

    @Test
    fun usersAndCredentialMaterial_remainOutsideCloudAllowlist() {
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("users"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("passwordHash"))
        assertTrue(CloudSyncPolicy.isCloudBusinessTable("products"))
    }

    @Test
    fun localLoginThrottle_boundsFailuresAndResetsOnSuccess() {
        var attempts = 0
        var lockedUntil = 0L
        val now = 10_000L

        repeat(LocalLoginPolicy.MAX_FAILED_ATTEMPTS - 1) {
            val (result, nextState) = LocalLoginPolicy.evaluate(false, attempts, lockedUntil, now)
            assertTrue(result is LocalLoginResult.Invalid)
            attempts = nextState.first
            lockedUntil = nextState.second
        }

        val (locked, lockedState) = LocalLoginPolicy.evaluate(false, attempts, lockedUntil, now)
        assertTrue(locked is LocalLoginResult.Locked)
        assertTrue(LocalLoginPolicy.isLocked(lockedState.first, lockedState.second, now + 1L))

        val (success, resetState) = LocalLoginPolicy.evaluate(
            credentialMatched = true,
            failedAttempts = lockedState.first,
            lockedUntilEpochMs = lockedState.second,
            nowEpochMs = lockedState.second + 1L
        )
        assertTrue(success is LocalLoginResult.Success)
        assertTrue(resetState.first == 0 && resetState.second == 0L)
    }
}
