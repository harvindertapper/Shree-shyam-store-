package com.example.utils

import java.security.MessageDigest

/**
 * Security utilities for PIN hashing and cryptographic verification.
 * Employs SHA-256 with lowercase hexadecimal digest representation.
 */
object SecurityUtils {

    /**
     * Hashes a 4-digit (or any alphanumeric) PIN using SHA-256.
     * Returns a 64-character lowercase hex string.
     */
    fun hashPin(pin: String): String {
        val trimmed = pin.trim()
        if (trimmed.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(trimmed.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Cryptographically verifies whether the entered PIN matches the stored hash.
     * Supports backward-compatible migration for legacy plaintext PINs and unconfigured defaults.
     */
    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        val cleanEntered = enteredPin.trim()
        if (cleanEntered.isEmpty()) return false

        val enteredHashed = hashPin(cleanEntered)

        // 1. Direct SHA-256 hash match (64 hex characters)
        if (storedHash.isNotBlank() && storedHash.equals(enteredHashed, ignoreCase = true)) {
            return true
        }

        // 2. Backward compatibility fallback: legacy unhashed 4-digit PIN in DataStore
        if (storedHash.length == 4 && storedHash == cleanEntered) {
            return true
        }

        // 3. Fallback for unconfigured/default store state (default PIN is "1234")
        if (storedHash.isBlank() && cleanEntered == "1234") {
            return true
        }

        // 4. Fallback if stored is default "1234" SHA-256 hash
        val defaultHash = hashPin("1234")
        if (storedHash.equals(defaultHash, ignoreCase = true) && cleanEntered == "1234") {
            return true
        }

        return false
    }
}

/** Top-level shortcut helper */
fun hashPin(pin: String): String = SecurityUtils.hashPin(pin)

/** Top-level shortcut helper */
fun verifyPin(enteredPin: String, storedHash: String): Boolean = SecurityUtils.verifyPin(enteredPin, storedHash)
