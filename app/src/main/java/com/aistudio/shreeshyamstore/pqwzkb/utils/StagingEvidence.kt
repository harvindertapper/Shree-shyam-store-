package com.aistudio.shreeshyamstore.pqwzkb.utils

/** Required rehearsal checkpoints before a Merchant OS artifact can be called staging-ready. */
enum class StagingRehearsalStage {
    MIGRATION,
    OFFLINE_COMMERCE,
    SYNC_RETRY,
    SYNC_CONFLICT,
    AUTHENTICATED_BACKUP,
    RESTORE_VALIDATION,
    RECOVERY_POINT,
    ROLLBACK_DECISION
}

enum class StagingStageStatus {
    PASS,
    FAIL,
    SKIPPED
}

data class StagingStageEvidence(
    val stage: StagingRehearsalStage,
    val status: StagingStageStatus,
    val observedAtEpochMs: Long,
    val evidenceReference: String,
    val notes: String = ""
) {
    init {
        require(observedAtEpochMs > 0L) { "Staging evidence timestamp must be positive" }
        require(evidenceReference.isNotBlank()) { "Staging evidence reference is required" }
    }
}

/**
 * Submission-safe release evidence. The model rejects production data by
 * construction and carries no customer payload, credentials, or raw logs.
 */
data class StagingRehearsalEvidence(
    val artifactSha256: String,
    val packageName: String,
    val appVersion: String,
    val sourceCommit: String,
    val deviceModel: String,
    val apiLevel: Int,
    val testTenantId: String,
    val usesProductionData: Boolean,
    val stages: List<StagingStageEvidence>,
    val knownLimitations: List<String> = emptyList(),
    val rollbackDecision: String
) {
    init {
        require(artifactSha256.matches(SHA256_PATTERN)) {
            "Artifact checksum must be a 64-character SHA-256 hex value"
        }
        require(packageName.isNotBlank()) { "Package name is required" }
        require(appVersion.isNotBlank()) { "App version is required" }
        require(sourceCommit.isNotBlank()) { "Source commit is required" }
        require(deviceModel.isNotBlank()) { "Device model is required" }
        require(apiLevel >= 24) { "API level must satisfy the app minimum" }
        require(testTenantId.isNotBlank()) { "A non-production test tenant is required" }
        require(!usesProductionData) { "Staging rehearsal cannot use production data" }
        require(rollbackDecision.isNotBlank()) { "Rollback decision is required" }
        require(stages.map { it.stage }.toSet().size == stages.size) {
            "Each staging rehearsal stage must appear at most once"
        }
    }

    fun hasCompletePassingMatrix(): Boolean =
        stages.size == StagingRehearsalStage.entries.size &&
            stages.all { it.status == StagingStageStatus.PASS } &&
            stages.map { it.stage }.toSet() == StagingRehearsalStage.entries.toSet()

    fun requireCompletePassingMatrix(): StagingRehearsalEvidence {
        require(hasCompletePassingMatrix()) {
            "All required staging rehearsal stages must pass before release readiness is claimed"
        }
        return this
    }

    /** Deliberately excludes notes and evidence references that may contain sensitive logs. */
    fun redactedSummary(): String = buildString {
        append("artifactSha256=").append(artifactSha256)
        append(", packageName=").append(packageName)
        append(", appVersion=").append(appVersion)
        append(", sourceCommit=").append(sourceCommit)
        append(", deviceModel=").append(deviceModel)
        append(", apiLevel=").append(apiLevel)
        append(", testTenantId=").append(testTenantId)
        append(", usesProductionData=").append(usesProductionData)
        append(", completePassingMatrix=").append(hasCompletePassingMatrix())
    }

    companion object {
        private val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}
