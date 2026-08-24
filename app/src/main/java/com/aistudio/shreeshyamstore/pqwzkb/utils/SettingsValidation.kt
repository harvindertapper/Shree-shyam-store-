package com.aistudio.shreeshyamstore.pqwzkb.utils

enum class SettingsValidationError {
    SHOP_NAME_REQUIRED,
    INVALID_PIN,
    PIN_CONFIRMATION_REQUIRED
}

object SettingsValidation {
    fun errorFor(
        shopName: String,
        newPin: String,
        confirmedPin: String
    ): SettingsValidationError? = when {
        shopName.trim().isEmpty() -> SettingsValidationError.SHOP_NAME_REQUIRED
        newPin.isNotEmpty() && !SecurityUtils.isAcceptableNewPin(newPin) -> SettingsValidationError.INVALID_PIN
        newPin.isNotEmpty() && newPin != confirmedPin -> SettingsValidationError.PIN_CONFIRMATION_REQUIRED
        else -> null
    }
}
