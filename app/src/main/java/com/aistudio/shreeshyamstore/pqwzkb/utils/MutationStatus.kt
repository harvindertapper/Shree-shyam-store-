package com.aistudio.shreeshyamstore.pqwzkb.utils

/**
 * Redacted, UI-facing state for one merchant mutation. It intentionally carries
 * no record payload, credential, token, or customer data.
 */
enum class MutationStage {
    IDLE,
    VALIDATING,
    SAVING_LOCALLY,
    SAVED_LOCALLY,
    SYNCING,
    SUCCESS,
    VALIDATION_ERROR,
    AUTH_ERROR,
    RETRYABLE_ERROR,
    CONFLICT,
    FAILURE
}

data class MutationStatus(
    val stage: MutationStage = MutationStage.IDLE,
    val message: String? = null,
    val canRetry: Boolean = false
)

fun mutationStageFor(error: Throwable, localizedGateMessage: String? = null): MutationStage {
    val message = error.message.orEmpty()
    return when {
        localizedGateMessage?.isNotBlank() == true || error is OperatorGateException -> MutationStage.AUTH_ERROR
        error is BackupUnauthorizedException || error is BackupWrongTenantException ||
            message.contains("unauthorized", ignoreCase = true) ||
            message.contains("authorization", ignoreCase = true) || message.contains("wrong tenant", ignoreCase = true) ->
            MutationStage.AUTH_ERROR
        error is BackupIncompatibleException || error is BackupReplayException ||
            message.contains("conflict", ignoreCase = true) || message.contains("incompatible", ignoreCase = true) ||
            message.contains("replayed", ignoreCase = true) -> MutationStage.CONFLICT
        error is BackupMalformedException || error is RestoreSnapshotException ||
            message.contains("malformed", ignoreCase = true) || message.contains("snapshot validation", ignoreCase = true) ->
            MutationStage.VALIDATION_ERROR
        error is IllegalArgumentException || message.contains("required", ignoreCase = true) ||
            message.contains("invalid", ignoreCase = true) || message.contains("already exists", ignoreCase = true) ||
            message.contains("barcode", ignoreCase = true) -> MutationStage.VALIDATION_ERROR
        error is BackupProviderException || error is SnapshotUnavailableException ||
            message.contains("network", ignoreCase = true) || message.contains("timeout", ignoreCase = true) ||
            message.contains("unavailable", ignoreCase = true) -> MutationStage.RETRYABLE_ERROR
        else -> MutationStage.FAILURE
    }
}

fun MutationStage.title(strings: AppStrings): String = when (this) {
    MutationStage.IDLE -> ""
    MutationStage.VALIDATING -> strings.statusValidating
    MutationStage.SAVING_LOCALLY -> strings.statusSavingLocally
    MutationStage.SAVED_LOCALLY -> strings.statusSavedLocally
    MutationStage.SYNCING -> strings.statusSyncing
    MutationStage.SUCCESS -> strings.statusSuccess
    MutationStage.VALIDATION_ERROR -> strings.statusValidationError
    MutationStage.AUTH_ERROR -> strings.statusAuthError
    MutationStage.RETRYABLE_ERROR -> strings.statusRetryableError
    MutationStage.CONFLICT -> strings.statusConflict
    MutationStage.FAILURE -> strings.statusFailure
}
