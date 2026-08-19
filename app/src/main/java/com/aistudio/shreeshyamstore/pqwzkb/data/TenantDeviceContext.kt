package com.aistudio.shreeshyamstore.pqwzkb.data

import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import java.util.UUID

/**
 * Trusted local enrollment context for one Merchant OS installation.
 *
 * These identifiers are scope metadata, not server authorization. Until a
 * Control Plane enrollment exists, legacy values are explicitly namespaced so
 * they cannot be mistaken for server-issued organization or membership IDs.
 */
data class TenantDeviceContext(
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

    fun normalized(): TenantDeviceContext = copy(
        organizationId = organizationId.trim(),
        storeId = storeId.trim(),
        membershipId = membershipId.trim(),
        deviceId = deviceId.trim(),
        appInstallationId = appInstallationId.trim()
    )

    fun toTenantScope(): TenantScope = TenantScope(
        organizationId = organizationId,
        storeId = storeId,
        membershipId = membershipId,
        deviceId = deviceId,
        appInstallationId = appInstallationId
    )

    companion object {
        const val LEGACY_ORGANIZATION_PREFIX = "legacy-org:"
        const val LEGACY_STORE_PREFIX = "legacy-store:"
        const val LEGACY_MEMBERSHIP_PREFIX = "legacy-membership:"
        const val LEGACY_INSTALLATION_PREFIX = "legacy-installation:"

        fun fromLegacySession(
            session: IdentitySession,
            deviceId: String,
            appInstallationId: String
        ): TenantDeviceContext {
            val normalizedSession = session.normalized()
            require(normalizedSession.isUsable()) { "A usable session is required for legacy tenant mapping" }
            val stableKey = normalizedSession.shopUid
            return TenantDeviceContext(
                organizationId = "$LEGACY_ORGANIZATION_PREFIX$stableKey",
                storeId = "$LEGACY_STORE_PREFIX$stableKey",
                membershipId = "$LEGACY_MEMBERSHIP_PREFIX$stableKey",
                deviceId = deviceId,
                appInstallationId = appInstallationId
            ).normalized()
        }

        fun newAppInstallationId(): String = "$LEGACY_INSTALLATION_PREFIX${UUID.randomUUID()}"
    }
}

/** Pure scope policy used by migration, restore, and future authorization tests. */
object TenantContextPolicy {
    fun sameStore(expected: TenantDeviceContext, candidate: TenantDeviceContext): Boolean =
        expected.organizationId == candidate.organizationId &&
            expected.storeId == candidate.storeId

    fun requireSameStore(expected: TenantDeviceContext, candidate: TenantDeviceContext) {
        require(sameStore(expected, candidate)) { "Tenant/store scope mismatch" }
    }

    fun sameInstallation(expected: TenantDeviceContext, candidate: TenantDeviceContext): Boolean =
        sameStore(expected, candidate) &&
            expected.membershipId == candidate.membershipId &&
            expected.deviceId == candidate.deviceId &&
            expected.appInstallationId == candidate.appInstallationId
}
