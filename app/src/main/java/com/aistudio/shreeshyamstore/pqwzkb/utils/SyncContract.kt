package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope

/** Versioned wire contract shared by Merchant OS adapters and the future Control Plane. */
data class SyncMutationEnvelope(
    val contractVersion: Int,
    val tenant: TenantScope,
    val actor: PlatformActor,
    val appInstallationId: String,
    val tableName: String,
    val globalId: String,
    val mutationVersion: Long,
    val mutationDeviceId: String,
    val idempotencyKey: String,
    val clientEventId: String,
    val clientCreatedAt: Long,
    val updatedAt: Long,
    val tombstone: Boolean,
    val tombstoneAtEpochMs: Long? = null,
    val payload: Map<String, Any?> = emptyMap()
)

data class SyncMutationIdentity(
    val tableName: String,
    val globalId: String,
    val mutationVersion: Long,
    val mutationDeviceId: String,
    val idempotencyKey: String
)

data class SyncStoredMutation(
    val identity: SyncMutationIdentity,
    val tombstone: Boolean
)

enum class SyncMutationOutcome {
    ACCEPTED,
    TOMBSTONE_ACCEPTED,
    REPLAY,
    STALE_REJECTED,
    CONFLICT_REJECTED
}

data class SyncMutationDecision(
    val outcome: SyncMutationOutcome,
    val errorCategory: SyncContractErrorCategory? = null
)

enum class SyncContractErrorCategory {
    UNAUTHORIZED_TENANT,
    UNSUPPORTED_CONTRACT,
    INVALID_MUTATION,
    INVALID_PAYLOAD,
    INVALID_TOMBSTONE,
    STALE_COMMAND,
    CONFLICT,
    CURSOR_REGRESSION,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}

class SyncContractException(
    val category: SyncContractErrorCategory,
    message: String
) : IllegalArgumentException(message)

/**
 * Contract v1 policy. It validates client claims against trusted session context,
 * but deliberately does not decide server authority or mutate Room state.
 */
object SyncContractV1 {
    const val VERSION = 1
    const val MAX_FUTURE_SKEW_MS = 30_000L

    val allowedTables: Set<String> = setOf(
        "categories",
        "products",
        "sales",
        "sale_items",
        "customers",
        "udhaar_transactions",
        "stock_adjustments"
    )

    private val forbiddenPayloadFragments = setOf(
        "password",
        "pin",
        "credential",
        "bearer",
        "token",
        "secret",
        "verifier"
    )

    fun validateEnvelope(
        envelope: SyncMutationEnvelope,
        expectedTenant: TenantScope,
        expectedActor: PlatformActor,
        nowEpochMs: Long
    ): SyncMutationIdentity {
        if (envelope.contractVersion != VERSION) {
            reject(SyncContractErrorCategory.UNSUPPORTED_CONTRACT, "Unsupported sync contract version")
        }
        if (envelope.tenant != expectedTenant || envelope.appInstallationId != expectedTenant.appInstallationId) {
            reject(SyncContractErrorCategory.UNAUTHORIZED_TENANT, "Sync tenant scope is not authorized")
        }
        if (
            envelope.actor.actorId != expectedActor.actorId ||
            envelope.actor.deviceId != expectedActor.deviceId ||
            !envelope.actor.role.trim().equals(expectedActor.role.trim(), ignoreCase = true)
        ) {
            reject(SyncContractErrorCategory.UNAUTHORIZED_TENANT, "Sync actor is not authorized")
        }
        if (envelope.actor.deviceId != envelope.tenant.deviceId ||
            envelope.mutationDeviceId != envelope.tenant.deviceId
        ) {
            reject(SyncContractErrorCategory.UNAUTHORIZED_TENANT, "Sync device binding is not authorized")
        }
        if (envelope.tableName !in allowedTables || envelope.globalId.isBlank()) {
            reject(SyncContractErrorCategory.INVALID_MUTATION, "Sync mutation identity is invalid")
        }
        if (envelope.mutationVersion <= 0L || envelope.clientEventId.isBlank()) {
            reject(SyncContractErrorCategory.INVALID_MUTATION, "Sync mutation metadata is invalid")
        }
        if (envelope.clientCreatedAt <= 0L || envelope.updatedAt <= 0L) {
            reject(SyncContractErrorCategory.INVALID_MUTATION, "Sync mutation timestamps are invalid")
        }
        if (envelope.clientCreatedAt > nowEpochMs + MAX_FUTURE_SKEW_MS ||
            envelope.updatedAt > nowEpochMs + MAX_FUTURE_SKEW_MS
        ) {
            reject(SyncContractErrorCategory.INVALID_MUTATION, "Sync mutation timestamp is too far in the future")
        }
        val expectedIdempotencyKey = SyncIdentity.idempotencyKey(
            envelope.tableName,
            envelope.globalId,
            envelope.mutationVersion
        )
        if (envelope.idempotencyKey != expectedIdempotencyKey) {
            reject(SyncContractErrorCategory.INVALID_MUTATION, "Sync idempotency key does not match mutation identity")
        }
        validatePayload(envelope.payload)
        if (envelope.tombstone) {
            if (envelope.tombstoneAtEpochMs != envelope.updatedAt || envelope.payload.isNotEmpty()) {
                reject(SyncContractErrorCategory.INVALID_TOMBSTONE, "Tombstones must carry only deletion metadata")
            }
        } else if (envelope.tombstoneAtEpochMs != null) {
            reject(SyncContractErrorCategory.INVALID_TOMBSTONE, "Live mutations cannot carry tombstone metadata")
        }
        return SyncMutationIdentity(
            tableName = envelope.tableName,
            globalId = envelope.globalId,
            mutationVersion = envelope.mutationVersion,
            mutationDeviceId = envelope.mutationDeviceId,
            idempotencyKey = envelope.idempotencyKey
        )
    }

    fun decide(
        incoming: SyncMutationEnvelope,
        stored: SyncStoredMutation?
    ): SyncMutationDecision {
        if (stored == null) {
            return SyncMutationDecision(
                outcome = if (incoming.tombstone) {
                    SyncMutationOutcome.TOMBSTONE_ACCEPTED
                } else {
                    SyncMutationOutcome.ACCEPTED
                }
            )
        }
        if (stored.identity.idempotencyKey == incoming.idempotencyKey) {
            return SyncMutationDecision(SyncMutationOutcome.REPLAY)
        }
        val comparison = SyncIdentity.compareMutation(
            incoming.mutationVersion,
            incoming.mutationDeviceId,
            stored.identity.mutationVersion,
            stored.identity.mutationDeviceId
        )
        return when {
            comparison < 0 -> SyncMutationDecision(
                outcome = SyncMutationOutcome.STALE_REJECTED,
                errorCategory = SyncContractErrorCategory.STALE_COMMAND
            )
            comparison == 0 -> SyncMutationDecision(
                outcome = SyncMutationOutcome.CONFLICT_REJECTED,
                errorCategory = SyncContractErrorCategory.CONFLICT
            )
            else -> SyncMutationDecision(
                outcome = if (incoming.tombstone) {
                    SyncMutationOutcome.TOMBSTONE_ACCEPTED
                } else {
                    SyncMutationOutcome.ACCEPTED
                }
            )
        }
    }

    private fun validatePayload(payload: Map<String, Any?>) {
        payload.keys.forEach { key ->
            val normalized = key.trim().lowercase()
            if (forbiddenPayloadFragments.any(normalized::contains)) {
                reject(SyncContractErrorCategory.INVALID_PAYLOAD, "Sync payload contains a forbidden field")
            }
        }
    }

    private fun reject(category: SyncContractErrorCategory, message: String): Nothing =
        throw SyncContractException(category, message)
}

data class SyncContractCursor(
    val contractVersion: Int,
    val tenant: TenantScope,
    val ownerDeviceId: String,
    val valueEpochMs: Long
)

object SyncCursorPolicy {
    fun validateOwnedBy(cursor: SyncContractCursor, expectedTenant: TenantScope): SyncContractCursor {
        if (cursor.contractVersion != SyncContractV1.VERSION) {
            throw SyncContractException(
                SyncContractErrorCategory.UNSUPPORTED_CONTRACT,
                "Unsupported sync cursor version"
            )
        }
        if (cursor.tenant != expectedTenant || cursor.ownerDeviceId != expectedTenant.deviceId) {
            throw SyncContractException(
                SyncContractErrorCategory.UNAUTHORIZED_TENANT,
                "Sync cursor is not owned by this tenant device"
            )
        }
        if (cursor.valueEpochMs < 0L) {
            throw SyncContractException(
                SyncContractErrorCategory.INVALID_MUTATION,
                "Sync cursor value cannot be negative"
            )
        }
        return cursor
    }

    fun advance(
        current: SyncContractCursor?,
        candidate: SyncContractCursor,
        expectedTenant: TenantScope
    ): SyncContractCursor {
        validateOwnedBy(candidate, expectedTenant)
        if (current == null) return candidate
        validateOwnedBy(current, expectedTenant)
        if (candidate.valueEpochMs < current.valueEpochMs) {
            throw SyncContractException(
                SyncContractErrorCategory.CURSOR_REGRESSION,
                "Sync cursor cannot move backwards"
            )
        }
        return if (candidate.valueEpochMs == current.valueEpochMs) current else candidate
    }
}

object SyncFailurePolicy {
    fun isRetryable(category: SyncContractErrorCategory): Boolean =
        category == SyncContractErrorCategory.RETRYABLE_FAILURE

    fun isPermanent(category: SyncContractErrorCategory): Boolean =
        category == SyncContractErrorCategory.PERMANENT_FAILURE ||
            category in setOf(
                SyncContractErrorCategory.UNAUTHORIZED_TENANT,
                SyncContractErrorCategory.UNSUPPORTED_CONTRACT,
                SyncContractErrorCategory.INVALID_MUTATION,
                SyncContractErrorCategory.INVALID_PAYLOAD,
                SyncContractErrorCategory.INVALID_TOMBSTONE,
                SyncContractErrorCategory.STALE_COMMAND,
                SyncContractErrorCategory.CONFLICT,
                SyncContractErrorCategory.CURSOR_REGRESSION
            )
}
