package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerRole
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantCapability
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentitySession

enum class OperatorAction(
    val capability: TenantCapability?,
    val requiresFirebase: Boolean = false
) {
    CATALOG_WRITE(TenantCapability.CATALOG_WRITE),
    CHECKOUT(TenantCapability.CHECKOUT),
    PAYMENT_RECONCILIATION(TenantCapability.PAYMENT_RECONCILIATION),
    LEDGER_RECORD(TenantCapability.LEDGER_RECORD),
    LEDGER_CORRECTION(TenantCapability.LEDGER_CORRECTION),
    INVENTORY_ADJUSTMENT(TenantCapability.INVENTORY_ADJUSTMENT),
    CLOUD_BACKUP(capability = null, requiresFirebase = true),
    CLOUD_RESTORE(capability = null, requiresFirebase = true)
}

enum class OperatorGateFailure {
    SIGN_IN_REQUIRED,
    FIREBASE_SIGN_IN_REQUIRED,
    PERMISSION_DENIED,
    UNSUPPORTED_ROLE
}

class OperatorGateException(
    val failure: OperatorGateFailure,
    val action: OperatorAction
) : IllegalArgumentException(failure.name)

fun localizedOperatorGateMessage(error: Throwable, language: AppLanguage): String? {
    val gateError = error as? OperatorGateException ?: return null
    val strings = LocaleHelper.getStrings(language)
    return when (gateError.failure) {
        OperatorGateFailure.SIGN_IN_REQUIRED -> strings.actionSignInRequired
        OperatorGateFailure.FIREBASE_SIGN_IN_REQUIRED -> strings.cloudAccountRequired
        OperatorGateFailure.PERMISSION_DENIED -> strings.actionPermissionDenied
        OperatorGateFailure.UNSUPPORTED_ROLE -> strings.actionRoleInvalid
    }
}

/**
 * Fast pre-submit policy for merchant journeys. Room/repository authorization
 * remains the final security boundary and must still validate tenant, actor,
 * command age, and capability at mutation time.
 */
object OperatorActionPolicy {
    fun requireAllowed(action: OperatorAction, session: IdentitySession?): IdentitySession {
        val normalized = session?.normalized()?.takeIf { it.isUsable() }
            ?: throw OperatorGateException(OperatorGateFailure.SIGN_IN_REQUIRED, action)
        if (action.requiresFirebase && normalized.provider != IdentityProvider.FIREBASE) {
            throw OperatorGateException(OperatorGateFailure.FIREBASE_SIGN_IN_REQUIRED, action)
        }
        val capability = action.capability ?: return normalized
        val role = runCatching { LedgerRole.parse(normalized.role) }
            .getOrElse {
                throw OperatorGateException(OperatorGateFailure.UNSUPPORTED_ROLE, action)
            }
        if (role !in capability.allowedRoles) {
            throw OperatorGateException(OperatorGateFailure.PERMISSION_DENIED, action)
        }
        return normalized
    }

    fun isAllowed(action: OperatorAction, session: IdentitySession?): Boolean =
        runCatching { requireAllowed(action, session) }.isSuccess
}
