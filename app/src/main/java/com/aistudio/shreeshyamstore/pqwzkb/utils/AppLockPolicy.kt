package com.aistudio.shreeshyamstore.pqwzkb.utils

/** Persisted state for the local app-lock gate. */
data class AppLockState(
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long = 0L,
    val lastUnlockAtEpochMs: Long = 0L
)

sealed interface PinUnlockResult {
    data object Success : PinUnlockResult
    data class Invalid(val remainingAttempts: Int) : PinUnlockResult
    data class Locked(val untilEpochMs: Long) : PinUnlockResult
}

/** Pure security policy used by the ViewModel and deterministic unit tests. */
object AppLockPolicy {
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L
    const val SESSION_TIMEOUT_MS = 15 * 60 * 1_000L

    fun isLocked(state: AppLockState, nowEpochMs: Long): Boolean =
        state.lockedUntilEpochMs > nowEpochMs

    fun recordFailure(state: AppLockState, nowEpochMs: Long): AppLockState {
        val attempts = (state.failedAttempts + 1).coerceAtMost(MAX_FAILED_ATTEMPTS)
        return state.copy(
            failedAttempts = attempts,
            lockedUntilEpochMs = if (attempts >= MAX_FAILED_ATTEMPTS) {
                nowEpochMs + LOCKOUT_DURATION_MS
            } else {
                0L
            }
        )
    }

    fun recordSuccess(nowEpochMs: Long): AppLockState = AppLockState(
        failedAttempts = 0,
        lockedUntilEpochMs = 0L,
        lastUnlockAtEpochMs = nowEpochMs
    )

    fun verifyPin(
        enteredPin: String,
        storedHash: String,
        state: AppLockState,
        nowEpochMs: Long
    ): Pair<PinUnlockResult, AppLockState> {
        if (isLocked(state, nowEpochMs)) {
            return PinUnlockResult.Locked(state.lockedUntilEpochMs) to state
        }
        if (SecurityUtils.verifyPin(enteredPin, storedHash)) {
            return PinUnlockResult.Success to recordSuccess(nowEpochMs)
        }

        val failedState = recordFailure(state, nowEpochMs)
        return if (isLocked(failedState, nowEpochMs)) {
            PinUnlockResult.Locked(failedState.lockedUntilEpochMs) to failedState
        } else {
            PinUnlockResult.Invalid(MAX_FAILED_ATTEMPTS - failedState.failedAttempts) to failedState
        }
    }

    fun sessionExpired(
        lastUnlockAtEpochMs: Long,
        nowEpochMs: Long,
        timeoutMs: Long = SESSION_TIMEOUT_MS
    ): Boolean = lastUnlockAtEpochMs <= 0L || nowEpochMs - lastUnlockAtEpochMs >= timeoutMs
}
