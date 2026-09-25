package com.sevenzenlabs.zenmart.utils

import com.sevenzenlabs.zenmart.data.IdentityProvider

/**
 * Recovery methods for the device-local app-lock PIN.
 *
 * An app-lock PIN is never reset by a Firebase password-reset email. The
 * account provider must re-authenticate the operator before a new local PIN
 * can be stored.
 */
enum class AppLockRecoveryMethod {
    LOCAL_ACCOUNT_REAUTH,
    GOOGLE_REAUTH,
    SWITCH_ACCOUNT
}

object AppLockRecoveryPolicy {
    fun method(provider: IdentityProvider?): AppLockRecoveryMethod = when (provider) {
        IdentityProvider.LOCAL -> AppLockRecoveryMethod.LOCAL_ACCOUNT_REAUTH
        IdentityProvider.FIREBASE -> AppLockRecoveryMethod.GOOGLE_REAUTH
        null -> AppLockRecoveryMethod.SWITCH_ACCOUNT
    }
}
