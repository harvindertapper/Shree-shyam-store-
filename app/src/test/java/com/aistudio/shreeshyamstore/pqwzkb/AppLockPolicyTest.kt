package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockState
import com.aistudio.shreeshyamstore.pqwzkb.utils.PinUnlockResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {
    @Test
    fun fiveFailuresEnterCooldownAndFurtherAttemptsRemainBlocked() {
        var state = AppLockState()
        var result: PinUnlockResult = PinUnlockResult.Success
        val now = 1_000L

        repeat(AppLockPolicy.MAX_FAILED_ATTEMPTS) {
            val evaluated = AppLockPolicy.verifyPin("0000", SecurityUtils.hashPin("2468"), state, now)
            result = evaluated.first
            state = evaluated.second
        }

        assertTrue(result is PinUnlockResult.Locked)
        assertEquals(AppLockPolicy.LOCKOUT_DURATION_MS + now, state.lockedUntilEpochMs)

        val blocked = AppLockPolicy.verifyPin("2468", SecurityUtils.hashPin("2468"), state, now)
        assertTrue(blocked.first is PinUnlockResult.Locked)
        assertEquals(state, blocked.second)
    }

    @Test
    fun cooldownExpiryAllowsVerificationAndSuccessResetsAttempts() {
        val locked = AppLockState(
            failedAttempts = AppLockPolicy.MAX_FAILED_ATTEMPTS,
            lockedUntilEpochMs = 10_000L
        )

        assertTrue(AppLockPolicy.isLocked(locked, 9_999L))
        assertFalse(AppLockPolicy.isLocked(locked, 10_000L))

        val (result, nextState) = AppLockPolicy.verifyPin(
            enteredPin = "2468",
            storedHash = SecurityUtils.hashPin("2468"),
            state = locked,
            nowEpochMs = 10_000L
        )
        assertEquals(PinUnlockResult.Success, result)
        assertEquals(0, nextState.failedAttempts)
        assertEquals(0L, nextState.lockedUntilEpochMs)
        assertEquals(10_000L, nextState.lastUnlockAtEpochMs)
    }

    @Test
    fun inactivityTimeoutUsesLastSuccessfulUnlockOnly() {
        assertTrue(AppLockPolicy.sessionExpired(0L, 1_000L))
        assertFalse(AppLockPolicy.sessionExpired(10_000L, 10_000L + AppLockPolicy.SESSION_TIMEOUT_MS - 1L))
        assertTrue(AppLockPolicy.sessionExpired(10_000L, 10_000L + AppLockPolicy.SESSION_TIMEOUT_MS))
    }

    @Test
    fun newPinPolicyRejectsDefaultsSequencesAndRepeatedDigits() {
        assertFalse(SecurityUtils.isAcceptableNewPin("1234"))
        assertFalse(SecurityUtils.isAcceptableNewPin("4321"))
        assertFalse(SecurityUtils.isAcceptableNewPin("1111"))
        assertFalse(SecurityUtils.isAcceptableNewPin("12a4"))
        assertTrue(SecurityUtils.isAcceptableNewPin("2468"))
    }
}
