package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.SettingsValidation
import com.aistudio.shreeshyamstore.pqwzkb.utils.SettingsValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsValidationTest {
    @Test
    fun shopNameIsRequiredBeforeSaving() {
        assertEquals(
            SettingsValidationError.SHOP_NAME_REQUIRED,
            SettingsValidation.errorFor("   ", "", "")
        )
    }

    @Test
    fun unsafePinIsRejectedBeforeConfirmationCheck() {
        assertEquals(
            SettingsValidationError.INVALID_PIN,
            SettingsValidation.errorFor("Store", "1234", "1234")
        )
    }

    @Test
    fun newPinRequiresMatchingConfirmation() {
        assertEquals(
            SettingsValidationError.PIN_CONFIRMATION_REQUIRED,
            SettingsValidation.errorFor("Store", "2468", "8642")
        )
    }

    @Test
    fun blankPinLeavesExistingCredentialUntouched() {
        assertNull(SettingsValidation.errorFor("Store", "", ""))
    }

    @Test
    fun safeConfirmedPinIsAccepted() {
        assertNull(SettingsValidation.errorFor("Store", "2468", "2468"))
    }
}
