package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.StagingRehearsalEvidence
import com.aistudio.shreeshyamstore.pqwzkb.utils.StagingRehearsalStage
import com.aistudio.shreeshyamstore.pqwzkb.utils.StagingStageEvidence
import com.aistudio.shreeshyamstore.pqwzkb.utils.StagingStageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StagingEvidenceTest {
    private val now = 1_700_000_000_000L

    @Test
    fun completeNonProductionMatrixIsAccepted() {
        val evidence = validEvidence()

        assertTrue(evidence.hasCompletePassingMatrix())
        assertEquals(evidence, evidence.requireCompletePassingMatrix())
    }

    @Test
    fun productionDataIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            validEvidence().copy(usesProductionData = true)
        }
    }

    @Test
    fun missingOrFailedStageCannotClaimReadiness() {
        val missing = validEvidence().copy(
            stages = validEvidence().stages.dropLast(1)
        )
        val failed = validEvidence().copy(
            stages = validEvidence().stages.map {
                if (it.stage == StagingRehearsalStage.SYNC_CONFLICT) {
                    it.copy(status = StagingStageStatus.FAIL)
                } else {
                    it
                }
            }
        )

        assertFalse(missing.hasCompletePassingMatrix())
        assertFalse(failed.hasCompletePassingMatrix())
        assertThrows(IllegalArgumentException::class.java) {
            missing.requireCompletePassingMatrix()
        }
        assertThrows(IllegalArgumentException::class.java) {
            failed.requireCompletePassingMatrix()
        }
    }

    @Test
    fun duplicateStageAndInvalidChecksumAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            validEvidence().copy(
                stages = validEvidence().stages + validEvidence().stages.first()
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validEvidence().copy(artifactSha256 = "not-a-checksum")
        }
    }

    @Test
    fun redactedSummaryOmitsStageNotesAndRawEvidenceReferences() {
        val evidence = validEvidence().copy(
            stages = validEvidence().stages.map {
                it.copy(
                    evidenceReference = "private-log://customer/passwordHash/token",
                    notes = "Bearer token and customer record must never be exported"
                )
            }
        )

        val summary = evidence.redactedSummary()

        assertTrue(summary.contains("completePassingMatrix=true"))
        assertFalse(summary.contains("private-log"))
        assertFalse(summary.contains("passwordHash"))
        assertFalse(summary.contains("Bearer token"))
    }

    @Test
    fun minimumSupportedApiIsEnforced() {
        assertThrows(IllegalArgumentException::class.java) {
            validEvidence().copy(apiLevel = 23)
        }
    }

    private fun validEvidence(): StagingRehearsalEvidence = StagingRehearsalEvidence(
        artifactSha256 = "a".repeat(64),
        packageName = "com.aistudio.shreeshyamstore.pqwzkb",
        appVersion = "1.0.0",
        sourceCommit = "abcdef1234567890",
        deviceModel = "staging-emulator",
        apiLevel = 34,
        testTenantId = "staging-tenant-001",
        usesProductionData = false,
        stages = StagingRehearsalStage.entries.map { stage ->
            StagingStageEvidence(
                stage = stage,
                status = StagingStageStatus.PASS,
                observedAtEpochMs = now,
                evidenceReference = "evidence://$stage",
                notes = "non-production fixture"
            )
        },
        knownLimitations = listOf("Cloud provider rehearsal requires a test tenant"),
        rollbackDecision = "Rollback to last known-good staging artifact if any data or sync invariant fails"
    )
}
