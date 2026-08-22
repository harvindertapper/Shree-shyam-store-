package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsEvent
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsEventCategory
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsIncident
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsIncidentStatus
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsOwnerRole
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsSeverity
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperationsTelemetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationsReadinessTest {
    private val now = 1_700_000_000_000L

    @Test
    fun redactedEventAcceptsOnlyBoundedOperationalAttributes() {
        val event = OperationsEvent(
            eventId = "evt-1",
            category = OperationsEventCategory.SYNC_HEALTH,
            severity = OperationsSeverity.WARNING,
            occurredAtEpochMs = now,
            appVersion = "1.0.0",
            buildEnvironment = "staging",
            correlationId = "sync:device-1",
            attributes = mapOf(
                "health" to "RETRYING",
                "retryableCount" to "2",
                "nextRetryAtEpochMs" to "1700000015000"
            )
        )

        val summary = OperationsTelemetryPolicy.redactedSummary(event)

        assertEquals("SYNC_HEALTH", summary["category"])
        assertEquals("RETRYING", summary["health"])
        assertEquals("staging", summary["buildEnvironment"])
    }

    @Test
    fun forbiddenAttributesAndRawSensitiveValuesAreRejected() {
        assertRejected {
            OperationsTelemetryPolicy.validate(
                event(attributes = mapOf("passwordHash" to "never"))
            )
        }
        assertRejected {
            OperationsTelemetryPolicy.validate(
                event(attributes = mapOf("operation" to "customer phone 9999999999"))
            )
        }
        assertRejected {
            OperationsTelemetryPolicy.validate(event(correlationId = "customer/123"))
        }
    }

    @Test
    fun invalidBuildEnvironmentAndUnsafeIdentifiersAreRejected() {
        assertRejected { OperationsTelemetryPolicy.validate(event(buildEnvironment = "production-live")) }
        assertRejected { OperationsTelemetryPolicy.validate(event(eventId = "event with spaces")) }
        assertRejected { OperationsTelemetryPolicy.validate(event(attributes = mapOf("raw" to "value"))) }
    }

    @Test
    fun incidentRequiresKnownRunbookAndRetainsOwnerStatus() {
        val incident = OperationsIncident(
            incidentId = "inc-1",
            category = OperationsEventCategory.RESTORE_FAILURE,
            severity = OperationsSeverity.CRITICAL,
            owner = OperationsOwnerRole.RELEASE_OWNER,
            runbookReference = "RUNBOOK_BACKUP_RESTORE",
            openedAtEpochMs = now,
            status = OperationsIncidentStatus.OPEN,
            lastEventId = "evt-restore-1"
        )

        assertEquals(incident, OperationsTelemetryPolicy.validateIncident(incident))
        assertTrue(OperationsTelemetryPolicy.allowedRunbooks.contains("RUNBOOK_BACKUP_RESTORE"))
    }

    @Test
    fun unknownRunbookAndUnsafeIncidentIdsAreRejected() {
        assertRejected {
            OperationsTelemetryPolicy.validateIncident(
                incident().copy(runbookReference = "https://untrusted.example/runbook")
            )
        }
        assertRejected {
            OperationsTelemetryPolicy.validateIncident(
                incident().copy(lastEventId = "raw customer payload")
            )
        }
    }

    @Test
    fun redactedSummaryDoesNotContainNotesOrStackTraceFields() {
        val event = event(
            attributes = mapOf(
                "errorCategory" to "AUTH_FAILURE",
                "operation" to "retry"
            )
        )
        val summary = OperationsTelemetryPolicy.redactedSummary(event)

        assertFalse(summary.keys.any { it.contains("stack", ignoreCase = true) })
        assertFalse(summary.keys.any { it.contains("payload", ignoreCase = true) })
        assertTrue(summary.keys.containsAll(setOf("eventId", "category", "severity")))
    }

    private fun event(
        eventId: String = "evt-1",
        buildEnvironment: String = "staging",
        correlationId: String? = "corr-1",
        attributes: Map<String, String> = emptyMap()
    ) = OperationsEvent(
        eventId = eventId,
        category = OperationsEventCategory.SYNC_FAILURE,
        severity = OperationsSeverity.ERROR,
        occurredAtEpochMs = now,
        appVersion = "1.0.0",
        buildEnvironment = buildEnvironment,
        correlationId = correlationId,
        attributes = attributes
    )

    private fun incident() = OperationsIncident(
        incidentId = "inc-1",
        category = OperationsEventCategory.SYNC_FAILURE,
        severity = OperationsSeverity.ERROR,
        owner = OperationsOwnerRole.PLATFORM_OWNER,
        runbookReference = "RUNBOOK_SYNC_HEALTH",
        openedAtEpochMs = now,
        lastEventId = "evt-1"
    )

    private fun assertRejected(block: () -> Unit) {
        val result = runCatching(block)
        assertTrue(result.isFailure)
    }
}
