package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.utils.AppLanguage
import com.sevenzenlabs.zenmart.utils.BackupIncompatibleException
import com.sevenzenlabs.zenmart.utils.BackupMalformedException
import com.sevenzenlabs.zenmart.utils.BackupUnavailableException
import com.sevenzenlabs.zenmart.utils.LocaleHelper
import com.sevenzenlabs.zenmart.utils.MutationStage
import com.sevenzenlabs.zenmart.utils.OperatorAction
import com.sevenzenlabs.zenmart.utils.OperatorGateException
import com.sevenzenlabs.zenmart.utils.OperatorGateFailure
import com.sevenzenlabs.zenmart.utils.mutationStageFor
import com.sevenzenlabs.zenmart.utils.title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MutationStatusTest {
    @Test
    fun everyVisibleMutationStageHasHindiAndEnglishLabels() {
        val hindi = LocaleHelper.getStrings(AppLanguage.HINDI)
        val english = LocaleHelper.getStrings(AppLanguage.ENGLISH)

        MutationStage.entries
            .filterNot { it == MutationStage.IDLE }
            .forEach { stage ->
                val hindiTitle = stage.title(hindi)
                val englishTitle = stage.title(english)
                assertTrue("Hindi title missing for $stage", hindiTitle.isNotBlank())
                assertTrue("English title missing for $stage", englishTitle.isNotBlank())
                assertNotEquals("Stage should be localized for $stage", hindiTitle, englishTitle)
            }

        assertTrue(hindi.retryAction.isNotBlank())
        assertTrue(english.retryAction.isNotBlank())
        assertTrue(hindi.statusSavedLocallyDetail.isNotBlank())
        assertTrue(english.statusSavedLocallyDetail.isNotBlank())
    }

    @Test
    fun mutationStageClassifierSeparatesRecoveryPaths() {
        assertEquals(
            MutationStage.AUTH_ERROR,
            mutationStageFor(
                OperatorGateException(OperatorGateFailure.SIGN_IN_REQUIRED, OperatorAction.CHECKOUT)
            )
        )
        assertEquals(MutationStage.VALIDATION_ERROR, mutationStageFor(IllegalArgumentException("invalid amount")))
        assertEquals(MutationStage.RETRYABLE_ERROR, mutationStageFor(BackupUnavailableException("network unavailable")))
        assertEquals(MutationStage.CONFLICT, mutationStageFor(BackupIncompatibleException()))
        assertEquals(MutationStage.VALIDATION_ERROR, mutationStageFor(BackupMalformedException()))
        assertEquals(MutationStage.FAILURE, mutationStageFor(IllegalStateException("database failure")))
    }

    @Test
    fun idleStageHasNoVisibleLabel() {
        assertFalse(MutationStage.IDLE.title(LocaleHelper.getStrings(AppLanguage.ENGLISH)).isNotBlank())
    }
}
