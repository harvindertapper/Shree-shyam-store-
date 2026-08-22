package com.aistudio.shreeshyamstore.pqwzkb.utils

/** Stable categories for redacted support and release telemetry. */
enum class OperationsEventCategory {
    APP_START,
    AUTH_FAILURE,
    SECURITY_LOCKOUT,
    MIGRATION_FAILURE,
    CHECKOUT_FAILURE,
    SYNC_HEALTH,
    SYNC_FAILURE,
    BACKUP_FAILURE,
    RESTORE_FAILURE,
    RECOVERY_ROLLBACK,
    RELEASE_GUARD
}

enum class OperationsSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class OperationsIncidentStatus {
    OPEN,
    MITIGATED,
    CLOSED
}

enum class OperationsOwnerRole {
    MERCHANT_OPERATOR,
    RELEASE_OWNER,
    SECURITY_OWNER,
    PLATFORM_OWNER
}

/**
 * Safe telemetry event. It contains operational categories and bounded metadata
 * only; it never carries payload JSON, stack traces, credentials, or customer data.
 */
data class OperationsEvent(
    val eventId: String,
    val category: OperationsEventCategory,
    val severity: OperationsSeverity,
    val occurredAtEpochMs: Long,
    val appVersion: String,
    val buildEnvironment: String,
    val correlationId: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

data class OperationsIncident(
    val incidentId: String,
    val category: OperationsEventCategory,
    val severity: OperationsSeverity,
    val owner: OperationsOwnerRole,
    val runbookReference: String,
    val openedAtEpochMs: Long,
    val status: OperationsIncidentStatus = OperationsIncidentStatus.OPEN,
    val lastEventId: String
)

object OperationsTelemetryPolicy {
    private val safeIdPattern = Regex("[A-Za-z0-9._:-]{1,128}")
    private val allowedAttributeKeys = setOf(
        "health",
        "errorCategory",
        "attemptCount",
        "pendingCount",
        "retryableCount",
        "deadLetterCount",
        "conflictCount",
        "nextRetryAtEpochMs",
        "operation",
        "stage",
        "apiLevel",
        "packageName",
        "sourceCommit",
        "rollbackDecision"
    )
    private val forbiddenFragments = setOf(
        "password",
        "pin",
        "credential",
        "bearer",
        "token",
        "secret",
        "verifier",
        "payload",
        "customer",
        "phone",
        "email",
        "stack",
        "trace"
    )

    fun validate(event: OperationsEvent): OperationsEvent {
        require(event.eventId.matches(safeIdPattern)) { "Operations event ID is invalid" }
        require(event.occurredAtEpochMs > 0L) { "Operations event timestamp is required" }
        require(event.appVersion.isNotBlank()) { "Operations app version is required" }
        require(event.buildEnvironment in setOf("debug", "staging", "production")) {
            "Operations build environment is unsupported"
        }
        event.correlationId?.let {
            require(it.matches(safeIdPattern)) { "Operations correlation ID is invalid" }
        }
        event.attributes.forEach { (key, value) ->
            require(key in allowedAttributeKeys) { "Operations attribute is not allowlisted" }
            val normalizedKey = key.lowercase()
            require(forbiddenFragments.none(normalizedKey::contains)) {
                "Operations attribute key is sensitive"
            }
            require(value.length <= 256) { "Operations attribute value is too long" }
            require(forbiddenFragments.none(value.lowercase()::contains)) {
                "Operations attribute value is sensitive"
            }
        }
        return event
    }

    fun validateIncident(incident: OperationsIncident): OperationsIncident {
        require(incident.incidentId.matches(safeIdPattern)) { "Incident ID is invalid" }
        require(incident.lastEventId.matches(safeIdPattern)) { "Incident event ID is invalid" }
        require(incident.openedAtEpochMs > 0L) { "Incident timestamp is required" }
        require(incident.runbookReference in allowedRunbooks) {
            "Incident runbook reference is unsupported"
        }
        return incident
    }

    val allowedRunbooks: Set<String> = setOf(
        "RUNBOOK_AUTH_LOCKOUT",
        "RUNBOOK_MIGRATION_FAILURE",
        "RUNBOOK_SYNC_HEALTH",
        "RUNBOOK_BACKUP_RESTORE",
        "RUNBOOK_RELEASE_ROLLBACK"
    )

    fun redactedSummary(event: OperationsEvent): Map<String, String> {
        validate(event)
        return buildMap {
            put("eventId", event.eventId)
            put("category", event.category.name)
            put("severity", event.severity.name)
            put("occurredAtEpochMs", event.occurredAtEpochMs.toString())
            put("appVersion", event.appVersion)
            put("buildEnvironment", event.buildEnvironment)
            event.correlationId?.let { put("correlationId", it) }
            event.attributes.forEach { (key, value) -> put(key, value) }
        }
    }
}
