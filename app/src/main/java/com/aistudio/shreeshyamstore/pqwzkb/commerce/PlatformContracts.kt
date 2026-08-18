package com.aistudio.shreeshyamstore.pqwzkb.commerce

import java.util.Locale

/**
 * Stable tenant scope shared by the Merchant OS, Control Plane API, and future
 * marketplace contracts. Persistence is introduced in a later versioned slice;
 * this type prevents new integration code from inventing ad-hoc scope fields.
 */
data class TenantScope(
    val organizationId: String,
    val storeId: String,
    val membershipId: String,
    val deviceId: String,
    val appInstallationId: String
) {
    init {
        require(organizationId.isNotBlank()) { "Organization ID is required" }
        require(storeId.isNotBlank()) { "Store ID is required" }
        require(membershipId.isNotBlank()) { "Membership ID is required" }
        require(deviceId.isNotBlank()) { "Device ID is required" }
        require(appInstallationId.isNotBlank()) { "App installation ID is required" }
    }
}

/** Actor metadata used for privileged or auditable business mutations. */
data class PlatformActor(
    val actorId: String,
    val displayName: String,
    val role: String,
    val deviceId: String
) {
    init {
        require(actorId.isNotBlank()) { "Actor ID is required" }
        require(displayName.isNotBlank()) { "Actor display name is required" }
        require(role.isNotBlank()) { "Actor role is required" }
        require(deviceId.isNotBlank()) { "Actor device ID is required" }
    }
}

/** Metadata that makes retries and cross-repository commands idempotent. */
data class CommandMetadata(
    val idempotencyKey: String,
    val clientEventId: String,
    val tenant: TenantScope,
    val actor: PlatformActor,
    val clientCreatedAt: Long
) {
    init {
        require(idempotencyKey.isNotBlank()) { "Idempotency key is required" }
        require(clientEventId.isNotBlank()) { "Client event ID is required" }
        require(clientCreatedAt > 0L) { "Client creation time is required" }
    }
}

/** Server-compatible payment lifecycle values for future settlement flows. */
enum class PaymentState(val wireValue: String) {
    NOT_REQUIRED("NOT_REQUIRED"),
    PENDING("PENDING"),
    RECEIVED("RECEIVED"),
    FAILED("FAILED"),
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED"),
    REFUNDED("REFUNDED");

    companion object {
        fun fromWireValue(value: String): PaymentState = entries.firstOrNull {
            it.wireValue == value.trim().uppercase(Locale.ENGLISH)
        } ?: throw IllegalArgumentException("Unsupported payment state: $value")
    }
}

/** Marketplace-facing availability state; this is not a raw stock number. */
enum class MarketplaceAvailability(val wireValue: String) {
    AVAILABLE("AVAILABLE"),
    LIKELY_AVAILABLE("LIKELY_AVAILABLE"),
    CALL_TO_CONFIRM("CALL_TO_CONFIRM"),
    TEMPORARILY_UNAVAILABLE("TEMPORARILY_UNAVAILABLE")
}

/** Stable integration event names shared by the Merchant OS and Control Plane. */
enum class PlatformEventType(val wireValue: String) {
    PRODUCT_UPSERTED("PRODUCT_UPSERTED"),
    STOCK_MOVED("STOCK_MOVED"),
    SALE_COMMITTED("SALE_COMMITTED"),
    PAYMENT_RECORDED("PAYMENT_RECORDED"),
    UDHAAR_EVENT_RECORDED("UDHAAR_EVENT_RECORDED"),
    SYNC_FAILED("SYNC_FAILED"),
    RESTORE_COMPLETED("RESTORE_COMPLETED")
}

/**
 * Compatibility envelope for event metadata. Payload serialization remains
 * owned by each domain and must use an explicit allowlist; credentials and
 * device-only secrets are never valid payload fields.
 */
data class PlatformEventMetadata(
    val eventId: String,
    val eventType: PlatformEventType,
    val command: CommandMetadata,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    init {
        require(eventId.isNotBlank()) { "Event ID is required" }
        require(schemaVersion > 0) { "Schema version must be positive" }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}
