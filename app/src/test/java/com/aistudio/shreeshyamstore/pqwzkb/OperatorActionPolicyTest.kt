package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentitySession
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorAction
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorActionPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorGateException
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorGateFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorActionPolicyTest {
    @Test
    fun localSessionCanUseMerchantActionsOffline() {
        val session = session(IdentityProvider.LOCAL, role = "OWNER")

        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.CATALOG_WRITE, session))
        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.CHECKOUT, session))
        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.LEDGER_RECORD, session))
        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.INVENTORY_ADJUSTMENT, session))
    }

    @Test
    fun localSessionCannotUseCloudBackupOrRestore() {
        val session = session(IdentityProvider.LOCAL)

        assertGateFailure(OperatorAction.CLOUD_BACKUP, session, OperatorGateFailure.FIREBASE_SIGN_IN_REQUIRED)
        assertGateFailure(OperatorAction.CLOUD_RESTORE, session, OperatorGateFailure.FIREBASE_SIGN_IN_REQUIRED)
    }

    @Test
    fun firebaseOwnerCanUseCloudBackupAndRestore() {
        val session = session(IdentityProvider.FIREBASE, role = "OWNER")

        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.CLOUD_BACKUP, session))
        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.CLOUD_RESTORE, session))
    }

    @Test
    fun signedOutSessionIsRejectedBeforeAnyAction() {
        assertGateFailure(OperatorAction.CHECKOUT, null, OperatorGateFailure.SIGN_IN_REQUIRED)
    }

    @Test
    fun cashierCannotAdjustInventoryOrCorrectLedger() {
        val session = session(IdentityProvider.LOCAL, role = "CASHIER")

        assertGateFailure(
            OperatorAction.INVENTORY_ADJUSTMENT,
            session,
            OperatorGateFailure.PERMISSION_DENIED
        )
        assertGateFailure(
            OperatorAction.LEDGER_CORRECTION,
            session,
            OperatorGateFailure.PERMISSION_DENIED
        )
        assertTrue(OperatorActionPolicy.isAllowed(OperatorAction.CHECKOUT, session))
    }

    @Test
    fun unsupportedRoleIsRejectedRatherThanDefaultingToOwner() {
        assertGateFailure(
            OperatorAction.CATALOG_WRITE,
            session(IdentityProvider.LOCAL, role = "UNKNOWN"),
            OperatorGateFailure.UNSUPPORTED_ROLE
        )
    }

    private fun assertGateFailure(
        action: OperatorAction,
        session: IdentitySession?,
        expected: OperatorGateFailure
    ) {
        try {
            OperatorActionPolicy.requireAllowed(action, session)
            throw AssertionError("Expected $expected")
        } catch (error: OperatorGateException) {
            assertEquals(expected, error.failure)
            assertEquals(action, error.action)
        }
    }

    private fun session(provider: IdentityProvider, role: String = "OWNER") = IdentitySession(
        provider = provider,
        uid = "user-1",
        username = "merchant",
        email = "merchant@example.com",
        role = role
    )
}
