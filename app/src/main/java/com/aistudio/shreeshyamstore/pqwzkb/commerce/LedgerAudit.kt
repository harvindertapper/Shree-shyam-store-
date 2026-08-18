package com.aistudio.shreeshyamstore.pqwzkb.commerce

import java.util.Locale

/**
 * Device-local actor snapshot captured at the repository command boundary.
 * It contains identity metadata only; never place credentials or session secrets here.
 */
data class LedgerActor(
    val actorUid: String,
    val actorName: String,
    val actorRole: String = LedgerRole.OWNER.name,
    val actorDeviceId: String = "unknown-device"
) {
    fun normalized(): LedgerActor {
        val uid = actorUid.trim()
        val name = actorName.trim().ifEmpty { uid }
        val deviceId = actorDeviceId.trim().ifEmpty { "unknown-device" }
        val role = LedgerRole.parse(actorRole).name
        require(uid.isNotEmpty()) { "Authenticated actor is required" }
        require(name.isNotEmpty()) { "Actor name is required" }
        return copy(actorUid = uid, actorName = name, actorRole = role, actorDeviceId = deviceId)
    }
}

enum class LedgerRole {
    OWNER,
    MANAGER,
    CASHIER;

    companion object {
        fun parse(raw: String): LedgerRole = values().firstOrNull {
            it.name == raw.trim().uppercase(Locale.ENGLISH)
        } ?: throw IllegalArgumentException("Unsupported ledger actor role")
    }
}

object LedgerAuditPolicy {
    const val MAX_CORRECTION_REASON_LENGTH = 240

    fun requireCanRecord(actor: LedgerActor): LedgerActor {
        val normalized = actor.normalized()
        require(LedgerRole.parse(normalized.actorRole) in setOf(LedgerRole.OWNER, LedgerRole.MANAGER, LedgerRole.CASHIER)) {
            "Actor is not authorized to record ledger entries"
        }
        return normalized
    }

    fun requireCanCorrect(actor: LedgerActor): LedgerActor {
        val normalized = actor.normalized()
        require(LedgerRole.parse(normalized.actorRole) in setOf(LedgerRole.OWNER, LedgerRole.MANAGER)) {
            "Actor is not authorized to correct ledger entries"
        }
        return normalized
    }

    fun requireReason(reason: String): String {
        val normalized = reason.trim()
        require(normalized.isNotEmpty()) { "Correction reason is required" }
        require(normalized.length <= MAX_CORRECTION_REASON_LENGTH) {
            "Correction reason is too long"
        }
        return normalized
    }

    fun balanceEffect(type: UdhaarTransactionType, amountMinorUnits: Long): Long {
        require(amountMinorUnits > 0L) { "Ledger amount must be positive" }
        return when (type) {
            UdhaarTransactionType.CREDIT -> amountMinorUnits
            UdhaarTransactionType.PAYMENT -> -amountMinorUnits
            UdhaarTransactionType.REVERSAL,
            UdhaarTransactionType.CORRECTION -> 0L
        }
    }
}
