package com.aistudio.shreeshyamstore.pqwzkb.utils

import java.security.MessageDigest

/**
 * Small, deterministic security helpers used by the local app-lock feature.
 *
 * PINs are stored as SHA-256 digests. The legacy plaintext branch is retained
 * only so existing installations can still be opened and migrated on the next
 * PIN save; new values are always hashed by [hashPin].
 */
object SecurityUtils {
    const val DEFAULT_PIN: String = "1234"
    private const val SHA256_HEX_LENGTH = 64

    /** Returns a lowercase SHA-256 hex digest, or an empty string for blank input. */
    fun hashPin(pin: String): String {
        val normalized = pin.trim()
        if (normalized.isEmpty()) return ""

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    /** Returns true when [value] is a canonical SHA-256 hexadecimal digest. */
    fun isSha256Hash(value: String): Boolean =
        value.trim().length == SHA256_HEX_LENGTH && value.trim().all { it in "0123456789abcdefABCDEF" }

    /**
     * New PINs must be four digits and must not use the historical default or
     * obvious repeated/sequential values.
     */
    fun isAcceptableNewPin(pin: String): Boolean {
        val normalized = pin.trim()
        if (normalized.length != 4 || normalized.any { !it.isDigit() }) return false
        if (normalized == DEFAULT_PIN || normalized == DEFAULT_PIN.reversed()) return false
        if (normalized.all { it == normalized.first() }) return false
        val ascending = normalized.zipWithNext().all { (left, right) -> right.code == left.code + 1 }
        val descending = normalized.zipWithNext().all { (left, right) -> right.code == left.code - 1 }
        return !ascending && !descending
    }

    /**
     * Verifies a PIN without exposing timing differences for hashed values.
     * A four-digit plaintext value and an empty value are accepted only for
     * backwards compatibility with installations created before PIN hashing.
     */
    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        val entered = enteredPin.trim()
        val stored = storedHash.trim()
        if (entered.isEmpty()) return false

        if (isSha256Hash(stored)) {
            val expected = stored.lowercase().toByteArray(Charsets.US_ASCII)
            val actual = hashPin(entered).toByteArray(Charsets.US_ASCII)
            return MessageDigest.isEqual(actual, expected)
        }

        // Legacy installations stored a four-digit PIN in Preferences.
        if (stored.length == 4 && stored.all { it.isDigit() }) {
            return MessageDigest.isEqual(
                entered.toByteArray(Charsets.UTF_8),
                stored.toByteArray(Charsets.UTF_8)
            )
        }

        // A blank preference means the app was never configured. Keep the
        // historical default usable; saving any new PIN removes this fallback.
        return stored.isEmpty() && entered == DEFAULT_PIN
    }
}

/** Backwards-compatible top-level helpers used by older call sites. */
fun hashPin(pin: String): String = SecurityUtils.hashPin(pin)
fun verifyPin(enteredPin: String, storedHash: String): Boolean =
    SecurityUtils.verifyPin(enteredPin, storedHash)
