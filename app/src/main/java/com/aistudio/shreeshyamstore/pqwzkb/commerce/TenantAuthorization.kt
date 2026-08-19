package com.aistudio.shreeshyamstore.pqwzkb.commerce

/** Repository capabilities that must be authorized outside the Compose UI. */
enum class TenantCapability(
    val allowedRoles: Set<LedgerRole>
) {
    CATALOG_WRITE(setOf(LedgerRole.OWNER, LedgerRole.MANAGER)),
    CHECKOUT(setOf(LedgerRole.OWNER, LedgerRole.MANAGER, LedgerRole.CASHIER)),
    PAYMENT_RECONCILIATION(setOf(LedgerRole.OWNER, LedgerRole.MANAGER)),
    LEDGER_RECORD(setOf(LedgerRole.OWNER, LedgerRole.MANAGER, LedgerRole.CASHIER)),
    LEDGER_CORRECTION(setOf(LedgerRole.OWNER, LedgerRole.MANAGER)),
    INVENTORY_ADJUSTMENT(setOf(LedgerRole.OWNER, LedgerRole.MANAGER))
}

/**
 * Pure command-boundary policy for the local Merchant OS.
 *
 * The expected scope and actor are supplied by trusted local session/context
 * state; caller-provided metadata is never treated as authoritative by itself.
 */
object TenantAuthorizationPolicy {
    const val MAX_COMMAND_AGE_MS: Long = 5 * 60 * 1000L
    const val MAX_FUTURE_SKEW_MS: Long = 30_000L

    fun requireAuthorized(
        command: CommandMetadata,
        expectedTenant: TenantScope,
        expectedActor: PlatformActor,
        capability: TenantCapability,
        nowEpochMs: Long,
        maxCommandAgeMs: Long = MAX_COMMAND_AGE_MS
    ): CommandMetadata {
        require(maxCommandAgeMs > 0L) { "Command age policy must be positive" }
        require(command.tenant == expectedTenant) { "Tenant scope mismatch" }
        require(command.tenant.deviceId == expectedTenant.deviceId) { "Command device scope mismatch" }
        require(command.actor.actorId == expectedActor.actorId) { "Authenticated actor mismatch" }
        require(command.actor.deviceId == expectedActor.deviceId) { "Actor device mismatch" }
        require(command.actor.deviceId == command.tenant.deviceId) { "Actor and tenant device mismatch" }
        require(command.actor.role.trim().equals(expectedActor.role.trim(), ignoreCase = true)) {
            "Actor role does not match the authenticated session"
        }

        val role = LedgerRole.parse(command.actor.role)
        require(role in capability.allowedRoles) { "Actor is not authorized for this command" }
        require(command.clientCreatedAt <= nowEpochMs + MAX_FUTURE_SKEW_MS) {
            "Command timestamp is too far in the future"
        }
        require(nowEpochMs - command.clientCreatedAt <= maxCommandAgeMs) {
            "Command is stale and must be retried"
        }
        return command
    }
}
