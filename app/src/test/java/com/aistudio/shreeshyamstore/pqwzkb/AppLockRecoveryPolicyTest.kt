package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockRecoveryMethod
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockRecoveryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockRecoveryPolicyTest {
    @Test
    fun localSessionRequiresLocalAccountReauthentication() {
        assertEquals(
            AppLockRecoveryMethod.LOCAL_ACCOUNT_REAUTH,
            AppLockRecoveryPolicy.method(IdentityProvider.LOCAL)
        )
    }

    @Test
    fun firebaseSessionRequiresGoogleReauthentication() {
        assertEquals(
            AppLockRecoveryMethod.GOOGLE_REAUTH,
            AppLockRecoveryPolicy.method(IdentityProvider.FIREBASE)
        )
    }

    @Test
    fun missingSessionRequiresAccountSwitch() {
        assertEquals(
            AppLockRecoveryMethod.SWITCH_ACCOUNT,
            AppLockRecoveryPolicy.method(null)
        )
    }
}
