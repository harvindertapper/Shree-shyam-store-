package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.MarketplaceAvailability
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformEventMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformEventType
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlatformContractsTest {
    private val tenant = TenantScope(
        organizationId = "org-1",
        storeId = "store-1",
        membershipId = "membership-1",
        deviceId = "device-1",
        appInstallationId = "install-1"
    )
    private val actor = PlatformActor(
        actorId = "user-1",
        displayName = "Owner",
        role = "OWNER",
        deviceId = "device-1"
    )

    @Test
    fun `tenant command metadata retains stable integration identity`() {
        val metadata = CommandMetadata(
            idempotencyKey = "idem-1",
            clientEventId = "event-1",
            tenant = tenant,
            actor = actor,
            clientCreatedAt = 1_700_000_000_000L
        )

        assertEquals("org-1", metadata.tenant.organizationId)
        assertEquals("store-1", metadata.tenant.storeId)
        assertEquals("idem-1", metadata.idempotencyKey)
        assertEquals("event-1", metadata.clientEventId)
    }

    @Test
    fun `incomplete tenant and actor metadata is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            TenantScope("", "store-1", "membership-1", "device-1", "install-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlatformActor("user-1", "Owner", "", "device-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CommandMetadata("", "event-1", tenant, actor, 1L)
        }
    }

    @Test
    fun `payment and marketplace wire values are stable`() {
        assertEquals("PENDING", PaymentState.PENDING.wireValue)
        assertEquals(PaymentState.RECEIVED, PaymentState.fromWireValue("received"))
        assertEquals("CALL_TO_CONFIRM", MarketplaceAvailability.CALL_TO_CONFIRM.wireValue)
    }

    @Test
    fun `event metadata freezes schema version and event name`() {
        val metadata = PlatformEventMetadata(
            eventId = "event-1",
            eventType = PlatformEventType.STOCK_MOVED,
            command = CommandMetadata(
                idempotencyKey = "idem-1",
                clientEventId = "event-1",
                tenant = tenant,
                actor = actor,
                clientCreatedAt = 1_700_000_000_000L
            )
        )

        assertEquals(1, metadata.schemaVersion)
        assertEquals("STOCK_MOVED", metadata.eventType.wireValue)
    }
}
