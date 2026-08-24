package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.EnglishStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.HindiStrings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLocaleContractTest {
    @Test
    fun controlCenterStringsArePresentInBothLanguages() {
        val values = listOf(
            HindiStrings.settingsAccountSection to EnglishStrings.settingsAccountSection,
            HindiStrings.settingsAccountProvider to EnglishStrings.settingsAccountProvider,
            HindiStrings.settingsProviderFirebase to EnglishStrings.settingsProviderFirebase,
            HindiStrings.settingsProviderLocal to EnglishStrings.settingsProviderLocal,
            HindiStrings.settingsStoreIdentity to EnglishStrings.settingsStoreIdentity,
            HindiStrings.settingsTenantIdentity to EnglishStrings.settingsTenantIdentity,
            HindiStrings.settingsSignedIn to EnglishStrings.settingsSignedIn,
            HindiStrings.settingsSignedOut to EnglishStrings.settingsSignedOut,
            HindiStrings.settingsCloudSignInRequired to EnglishStrings.settingsCloudSignInRequired,
            HindiStrings.settingsShopProfileHint to EnglishStrings.settingsShopProfileHint,
            HindiStrings.settingsSecurityHint to EnglishStrings.settingsSecurityHint,
            HindiStrings.settingsAppLockOnHint to EnglishStrings.settingsAppLockOnHint,
            HindiStrings.settingsAppLockOffHint to EnglishStrings.settingsAppLockOffHint,
            HindiStrings.settingsSetPinHint to EnglishStrings.settingsSetPinHint,
            HindiStrings.settingsPinConfirmLabel to EnglishStrings.settingsPinConfirmLabel,
            HindiStrings.settingsPinMismatch to EnglishStrings.settingsPinMismatch,
            HindiStrings.settingsBiometricAvailable to EnglishStrings.settingsBiometricAvailable,
            HindiStrings.settingsBiometricUnavailable to EnglishStrings.settingsBiometricUnavailable,
            HindiStrings.settingsLanguageHint to EnglishStrings.settingsLanguageHint,
            HindiStrings.settingsAppearanceSection to EnglishStrings.settingsAppearanceSection,
            HindiStrings.settingsAppearanceHint to EnglishStrings.settingsAppearanceHint,
            HindiStrings.settingsAutomaticSync to EnglishStrings.settingsAutomaticSync,
            HindiStrings.settingsAutomaticSyncHint to EnglishStrings.settingsAutomaticSyncHint,
            HindiStrings.settingsManualSync to EnglishStrings.settingsManualSync,
            HindiStrings.settingsCloudBackupTitle to EnglishStrings.settingsCloudBackupTitle,
            HindiStrings.settingsCloudBackupHint to EnglishStrings.settingsCloudBackupHint,
            HindiStrings.settingsBackupNow to EnglishStrings.settingsBackupNow,
            HindiStrings.settingsRestore to EnglishStrings.settingsRestore,
            HindiStrings.settingsRestoreWarningTitle to EnglishStrings.settingsRestoreWarningTitle,
            HindiStrings.settingsRestoreWarningMessage to EnglishStrings.settingsRestoreWarningMessage,
            HindiStrings.settingsRestoreConfirmAction to EnglishStrings.settingsRestoreConfirmAction,
            HindiStrings.settingsBillingSection to EnglishStrings.settingsBillingSection,
            HindiStrings.settingsManualUpiSettlement to EnglishStrings.settingsManualUpiSettlement,
            HindiStrings.settingsManualUpiSettlementHint to EnglishStrings.settingsManualUpiSettlementHint,
            HindiStrings.settingsInventorySection to EnglishStrings.settingsInventorySection,
            HindiStrings.settingsInventoryHint to EnglishStrings.settingsInventoryHint,
            HindiStrings.settingsCustomersSection to EnglishStrings.settingsCustomersSection,
            HindiStrings.settingsCustomersHint to EnglishStrings.settingsCustomersHint,
            HindiStrings.settingsSyncSection to EnglishStrings.settingsSyncSection,
            HindiStrings.settingsSyncHealth to EnglishStrings.settingsSyncHealth,
            HindiStrings.settingsLastAttempt to EnglishStrings.settingsLastAttempt,
            HindiStrings.settingsLastSuccess to EnglishStrings.settingsLastSuccess,
            HindiStrings.settingsLastAttemptUnavailable to EnglishStrings.settingsLastAttemptUnavailable,
            HindiStrings.settingsHealthHealthy to EnglishStrings.settingsHealthHealthy,
            HindiStrings.settingsHealthNever to EnglishStrings.settingsHealthNever,
            HindiStrings.settingsHealthPending to EnglishStrings.settingsHealthPending,
            HindiStrings.settingsHealthRetrying to EnglishStrings.settingsHealthRetrying,
            HindiStrings.settingsHealthBlocked to EnglishStrings.settingsHealthBlocked,
            HindiStrings.settingsPending to EnglishStrings.settingsPending,
            HindiStrings.settingsRetryable to EnglishStrings.settingsRetryable,
            HindiStrings.settingsDeadLetter to EnglishStrings.settingsDeadLetter,
            HindiStrings.settingsConflicts to EnglishStrings.settingsConflicts,
            HindiStrings.settingsDataPrivacySection to EnglishStrings.settingsDataPrivacySection,
            HindiStrings.settingsDataPrivacyHint to EnglishStrings.settingsDataPrivacyHint,
            HindiStrings.settingsSupportSection to EnglishStrings.settingsSupportSection,
            HindiStrings.settingsVersion to EnglishStrings.settingsVersion,
            HindiStrings.settingsSupportHint to EnglishStrings.settingsSupportHint,
            HindiStrings.settingsSaveHint to EnglishStrings.settingsSaveHint,
            HindiStrings.settingsSavedLocally to EnglishStrings.settingsSavedLocally,
            HindiStrings.settingsSaveValidationShopName to EnglishStrings.settingsSaveValidationShopName,
            HindiStrings.settingsSaveValidationPin to EnglishStrings.settingsSaveValidationPin,
            HindiStrings.settingsSaveValidationPinConfirm to EnglishStrings.settingsSaveValidationPinConfirm,
            HindiStrings.settingsDisableLockTitle to EnglishStrings.settingsDisableLockTitle,
            HindiStrings.settingsDisableLockMessage to EnglishStrings.settingsDisableLockMessage,
            HindiStrings.settingsConfirmDisable to EnglishStrings.settingsConfirmDisable,
            HindiStrings.settingsKeepLock to EnglishStrings.settingsKeepLock,
            HindiStrings.settingsLockDraftNotice to EnglishStrings.settingsLockDraftNotice,
            HindiStrings.settingsOwnerPhonePlaceholder to EnglishStrings.settingsOwnerPhonePlaceholder,
            HindiStrings.settingsPinPlaceholder to EnglishStrings.settingsPinPlaceholder,
            HindiStrings.settingsBack to EnglishStrings.settingsBack,
            HindiStrings.settingsUserAvatar to EnglishStrings.settingsUserAvatar,
            HindiStrings.settingsSignIn to EnglishStrings.settingsSignIn,
            HindiStrings.settingsWelcomeChantHint to EnglishStrings.settingsWelcomeChantHint,
            HindiStrings.settingsQrPreview to EnglishStrings.settingsQrPreview,
            HindiStrings.settingsChangeQr to EnglishStrings.settingsChangeQr,
            HindiStrings.settingsNoQrSelected to EnglishStrings.settingsNoQrSelected,
            HindiStrings.settingsSyncNote to EnglishStrings.settingsSyncNote,
            HindiStrings.settingsLastSyncLabel to EnglishStrings.settingsLastSyncLabel,
            HindiStrings.settingsSyncStatusSuccess to EnglishStrings.settingsSyncStatusSuccess,
            HindiStrings.settingsSyncStatusNoChanges to EnglishStrings.settingsSyncStatusNoChanges,
            HindiStrings.settingsSyncStatusFailed to EnglishStrings.settingsSyncStatusFailed,
            HindiStrings.settingsSyncStatusUnavailable to EnglishStrings.settingsSyncStatusUnavailable,
            HindiStrings.settingsNotAssigned to EnglishStrings.settingsNotAssigned,
            HindiStrings.settingsTitle to EnglishStrings.settingsTitle
        )

        assertTrue(values.isNotEmpty())
        values.forEach { (hindi, english) ->
            assertFalse(hindi.isBlank())
            assertFalse(english.isBlank())
        }
    }
}
