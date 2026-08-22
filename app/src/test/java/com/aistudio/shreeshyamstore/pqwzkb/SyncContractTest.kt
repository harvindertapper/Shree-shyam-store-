package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncContractCursor
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncContractErrorCategory
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncContractException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncContractV1
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncCursorPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncFailurePolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncMutationEnvelope
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncMutationIdentity
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncMutationOutcome
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncStoredMutation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncContractTest {
    private val now = 1_700_000_000_000L
    private val tenant = TenantScope(
        organizationId = "org-1",
        storeId = "store-1",
        membershipId = "membership-1",
        deviceId = "device-1",
        appInstallationId = "install-1"
    )
    private val actor = PlatformActor(
        actorId = "owner-1",
        displayName = "Owner",
        role = "OWNER",
        deviceId = tenant.deviceId
    )

    @Test
    fun acceptedMutationUsesStableTenantAndIdentityBinding() {
        val envelope = mutation()

        val identity = SyncContractV1.validateEnvelope(envelope, tenant, actor, now)

        assertEquals("products", identity.tableName)
        assertEquals("global-1", identity.globalId)
        assertEquals(4L, identity.mutationVersion)
        assertEquals(
            SyncIdentity.idempotencyKey("products", "global-1", 4L),
            identity.idempotencyKey
        )
    }

    @Test
    fun unauthorizedTenantIsRejectedBeforeMutationProcessing() {
        val otherTenant = tenant.copy(storeId = "store-other")

        assertCategory(SyncContractErrorCategory.UNAUTHORIZED_TENANT) {
            SyncContractV1.validateEnvelope(mutation(), otherTenant, actor, now)
        }
    }

    @Test
    fun staleMutationIsRejectedWithoutAssumingRoomIsServerAuthority() {
        val stored = SyncStoredMutation(
            identity = identity(version = 5L, deviceId = "device-2", key = "products/global-1/5"),
            tombstone = false
        )

        val decision = SyncContractV1.decide(mutation(version = 4L), stored)

        assertEquals(SyncMutationOutcome.STALE_REJECTED, decision.outcome)
        assertEquals(SyncContractErrorCategory.STALE_COMMAND, decision.errorCategory)
    }

    @Test
    fun exactReplayIsIdempotent() {
        val incoming = mutation()
        val stored = SyncStoredMutation(
            identity = identity(
                version = incoming.mutationVersion,
                deviceId = incoming.mutationDeviceId,
                key = incoming.idempotencyKey
            ),
            tombstone = false
        )

        val decision = SyncContractV1.decide(incoming, stored)

        assertEquals(SyncMutationOutcome.REPLAY, decision.outcome)
        assertEquals(null, decision.errorCategory)
    }

    @Test
    fun newerTombstoneIsAcceptedAndCarriesNoPayload() {
        val tombstone = mutation(
            version = 6L,
            tombstone = true,
            tombstoneAtEpochMs = now - 10_000L,
            payload = emptyMap()
        )

        SyncContractV1.validateEnvelope(tombstone, tenant, actor, now)
        val decision = SyncContractV1.decide(tombstone, stored(version = 4L))

        assertEquals(SyncMutationOutcome.TOMBSTONE_ACCEPTED, decision.outcome)
    }

    @Test
    fun invalidTombstoneAndForbiddenPayloadAreRejected() {
        assertCategory(SyncContractErrorCategory.INVALID_TOMBSTONE) {
            SyncContractV1.validateEnvelope(
                mutation(tombstone = true, tombstoneAtEpochMs = now - 10_000L, payload = mapOf("name" to "Milk")),
                tenant,
                actor,
                now
            )
        }
        assertCategory(SyncContractErrorCategory.INVALID_PAYLOAD) {
            SyncContractV1.validateEnvelope(
                mutation(payload = mapOf("passwordHash" to "must-not-sync")),
                tenant,
                actor,
                now
            )
        }
    }

    @Test
    fun sameVersionDifferentStoredIdentityIsAnExplicitConflict() {
        val stored = SyncStoredMutation(
            identity = identity(version = 4L, deviceId = tenant.deviceId, key = "legacy-different-key"),
            tombstone = false
        )

        val decision = SyncContractV1.decide(mutation(version = 4L), stored)

        assertEquals(SyncMutationOutcome.CONFLICT_REJECTED, decision.outcome)
        assertEquals(SyncContractErrorCategory.CONFLICT, decision.errorCategory)
    }

    @Test
    fun cursorAdvancesOnlyWithinTheAuthorizedTenantAndNeverRegresses() {
        val current = cursor(100L)
        val advanced = SyncCursorPolicy.advance(current, cursor(120L), tenant)
        assertEquals(120L, advanced.valueEpochMs)

        val same = SyncCursorPolicy.advance(advanced, cursor(120L), tenant)
        assertEquals(advanced, same)

        assertCategory(SyncContractErrorCategory.CURSOR_REGRESSION) {
            SyncCursorPolicy.advance(advanced, cursor(119L), tenant)
        }
        assertCategory(SyncContractErrorCategory.UNAUTHORIZED_TENANT) {
            SyncCursorPolicy.advance(advanced, cursor(121L, ownerDeviceId = "other-device"), tenant)
        }
    }

    @Test
    fun typedFailurePolicyKeepsPermanentAndRetryableOutcomesDistinct() {
        assertTrue(SyncFailurePolicy.isRetryable(SyncContractErrorCategory.RETRYABLE_FAILURE))
        assertFalse(SyncFailurePolicy.isRetryable(SyncContractErrorCategory.CONFLICT))
        assertTrue(SyncFailurePolicy.isPermanent(SyncContractErrorCategory.CONFLICT))
        assertTrue(SyncFailurePolicy.isPermanent(SyncContractErrorCategory.UNAUTHORIZED_TENANT))
        assertFalse(SyncFailurePolicy.isPermanent(SyncContractErrorCategory.RETRYABLE_FAILURE))
    }

    @Test
    fun contractVersionAndIdempotencyKeyAreRequired() {
        assertCategory(SyncContractErrorCategory.UNSUPPORTED_CONTRACT) {
            SyncContractV1.validateEnvelope(mutation(contractVersion = 2), tenant, actor, now)
        }
        assertCategory(SyncContractErrorCategory.INVALID_MUTATION) {
            SyncContractV1.validateEnvelope(
                mutation(idempotencyKey = "wrong-key"),
                tenant,
                actor,
                now
            )
        }
    }

    private fun mutation(
        contractVersion: Int = SyncContractV1.VERSION,
        version: Long = 4L,
        tombstone: Boolean = false,
        tombstoneAtEpochMs: Long? = null,
        idempotencyKey: String = SyncIdentity.idempotencyKey("products", "global-1", version),
        payload: Map<String, Any?> = mapOf("name" to "Milk")
    ) = SyncMutationEnvelope(
        contractVersion = contractVersion,
        tenant = tenant,
        actor = actor,
        appInstallationId = tenant.appInstallationId,
        tableName = "products",
        globalId = "global-1",
        mutationVersion = version,
        mutationDeviceId = tenant.deviceId,
        idempotencyKey = idempotencyKey,
        clientEventId = "event-$version",
        clientCreatedAt = now - 10_000L,
        updatedAt = tombstoneAtEpochMs ?: now - 5_000L,
        tombstone = tombstone,
        tombstoneAtEpochMs = tombstoneAtEpochMs,
        payload = payload
    )

    private fun stored(version: Long) = SyncStoredMutation(
        identity = identity(version, tenant.deviceId, SyncIdentity.idempotencyKey("products", "global-1", version)),
        tombstone = false
    )

    private fun identity(version: Long, deviceId: String, key: String) = SyncMutationIdentity(
        tableName = "products",
        globalId = "global-1",
        mutationVersion = version,
        mutationDeviceId = deviceId,
        idempotencyKey = key
    )

    private fun cursor(value: Long, ownerDeviceId: String = tenant.deviceId) = SyncContractCursor(
        contractVersion = SyncContractV1.VERSION,
        tenant = tenant,
        ownerDeviceId = ownerDeviceId,
        valueEpochMs = value
    )

    private fun assertCategory(expected: SyncContractErrorCategory, block: () -> Unit) {
        val error = try {
            block()
            throw AssertionError("Expected $expected")
        } catch (error: SyncContractException) {
            error
        }
        assertEquals(expected, error.category)
    }
}
