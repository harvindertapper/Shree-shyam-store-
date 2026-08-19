package com.aistudio.shreeshyamstore.pqwzkb.utils

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Device-local credential helpers.
 *
 * New records use a versioned, per-record salted PBKDF2-HMAC-SHA256 verifier:
 * `v2:<scope>:<iterations>:<salt-hex>:<derived-key-hex>`.
 * Legacy SHA-256 and plaintext PIN values are accepted only through the
 * migration-only verification path and are never suitable cloud payloads.
 */
object SecurityUtils {
    const val DEFAULT_PIN: String = "1234"
    const val CURRENT_CREDENTIAL_VERSION: Int = 2
    const val LEGACY_CREDENTIAL_VERSION: Int = 1
    const val PBKDF2_ITERATIONS: Int = 120_000
    const val DERIVED_KEY_BYTES: Int = 32
    const val SALT_BYTES: Int = 16

    private const val CREDENTIAL_PREFIX = "v2"
    private const val MAX_STORED_ITERATIONS = 500_000
    private const val SHA256_HEX_LENGTH = 64
    private const val HEX_RADIX = 16

    enum class CredentialScope(val wireValue: String) {
        LOCAL_ACCOUNT("local-account"),
        APP_LOCK("app-lock")
    }

    data class VerificationResult(
        val matched: Boolean,
        val needsRehash: Boolean
    )

    private data class VersionedCredential(
        val scope: CredentialScope,
        val iterations: Int,
        val salt: ByteArray,
        val derivedKey: ByteArray
    )

    /** Returns a lowercase SHA-256 hex digest for the legacy app-lock format. */
    fun hashPin(pin: String): String {
        val normalized = pin.trim()
        if (normalized.isEmpty()) return ""
        return sha256Hex(normalized)
    }

    /**
     * Legacy local-account hash retained only to verify and migrate existing
     * installations. New code must use [createCredential].
     */
    internal fun legacyPasswordHash(password: String): String = sha256Hex(password)

    /** Returns true when [value] is a canonical SHA-256 hexadecimal digest. */
    fun isSha256Hash(value: String): Boolean =
        value.trim().length == SHA256_HEX_LENGTH &&
            value.trim().all { it in "0123456789abcdefABCDEF" }

    /** Returns true only for the current versioned credential record format. */
    fun isVersionedCredential(value: String, expectedScope: CredentialScope? = null): Boolean =
        parseVersionedCredential(value.trim())?.let { credential ->
            expectedScope == null || credential.scope == expectedScope
        } == true

    /**
     * Creates a new salted credential record. The secret is preserved exactly
     * for local accounts; app-lock PINs retain the historical trim behavior.
     */
    fun createCredential(secret: String, scope: CredentialScope): String {
        val normalized = normalizeSecret(secret, scope)
        require(normalized.isNotEmpty()) { "Credential secret must not be blank" }

        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val derivedKey = pbkdf2HmacSha256(normalized, salt, PBKDF2_ITERATIONS, DERIVED_KEY_BYTES)
        return buildString {
            append(CREDENTIAL_PREFIX)
            append(':')
            append(scope.wireValue)
            append(':')
            append(PBKDF2_ITERATIONS)
            append(':')
            append(salt.toHex())
            append(':')
            append(derivedKey.toHex())
        }
    }

    /**
     * Verifies a local credential and reports whether the caller should replace
     * a legacy verifier immediately after successful authentication.
     *
     * [allowDefaultPinFallback] is intentionally explicit and should only be
     * true for the app-lock migration window. Local account authentication
     * never accepts blank/default or plaintext values.
     */
    fun verifyCredential(
        secret: String,
        storedCredential: String,
        scope: CredentialScope,
        allowDefaultPinFallback: Boolean = false
    ): VerificationResult {
        val stored = storedCredential.trim()
        val normalized = normalizeSecret(secret, scope)
        if (normalized.isEmpty()) return VerificationResult(matched = false, needsRehash = false)

        val versioned = parseVersionedCredential(stored)
        if (versioned != null) {
            if (versioned.scope != scope) return VerificationResult(false, false)
            val actual = pbkdf2HmacSha256(
                normalized,
                versioned.salt,
                versioned.iterations,
                versioned.derivedKey.size
            )
            return VerificationResult(
                matched = MessageDigest.isEqual(actual, versioned.derivedKey),
                needsRehash = false
            )
        }

        // A malformed versioned record must never fall through to a weaker
        // legacy verifier or a value from another authority/scope.
        if (stored.startsWith("$CREDENTIAL_PREFIX:")) {
            return VerificationResult(false, false)
        }

        if (isSha256Hash(stored)) {
            val expected = stored.lowercase().toByteArray(Charsets.US_ASCII)
            val legacy = when (scope) {
                CredentialScope.LOCAL_ACCOUNT -> legacyPasswordHash(secret)
                CredentialScope.APP_LOCK -> hashPin(secret)
            }.toByteArray(Charsets.US_ASCII)
            return VerificationResult(
                matched = MessageDigest.isEqual(legacy, expected),
                needsRehash = true
            )
        }

        // Legacy app-lock installations stored a four-digit PIN in Preferences.
        if (scope == CredentialScope.APP_LOCK && stored.length == 4 && stored.all { it.isDigit() }) {
            return VerificationResult(
                matched = MessageDigest.isEqual(
                    normalized.toByteArray(Charsets.UTF_8),
                    stored.toByteArray(Charsets.UTF_8)
                ),
                needsRehash = true
            )
        }

        // A missing app-lock preference means the app was never configured.
        // This compatibility branch is sunset as soon as a successful unlock
        // persists a v2 record; it is never available to local accounts.
        if (
            scope == CredentialScope.APP_LOCK &&
            allowDefaultPinFallback &&
            stored.isEmpty() &&
            normalized == DEFAULT_PIN
        ) {
            return VerificationResult(matched = true, needsRehash = true)
        }

        return VerificationResult(matched = false, needsRehash = false)
    }

    /**
     * Backwards-compatible app-lock API. New callers should use
     * [verifyCredential] so scope and migration state are explicit.
     */
    fun verifyPin(enteredPin: String, storedHash: String): Boolean =
        verifyCredential(
            secret = enteredPin,
            storedCredential = storedHash,
            scope = CredentialScope.APP_LOCK,
            allowDefaultPinFallback = true
        ).matched

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

    private fun normalizeSecret(secret: String, scope: CredentialScope): String =
        if (scope == CredentialScope.APP_LOCK) secret.trim() else secret

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    private fun parseVersionedCredential(value: String): VersionedCredential? {
        val parts = value.split(':')
        if (parts.size != 5 || parts[0] != CREDENTIAL_PREFIX) return null
        val scope = CredentialScope.entries.firstOrNull { it.wireValue == parts[1] } ?: return null
        val iterations = parts[2].toIntOrNull() ?: return null
        if (iterations < 1 || iterations > MAX_STORED_ITERATIONS) return null
        val salt = parts[3].fromHex() ?: return null
        val derivedKey = parts[4].fromHex() ?: return null
        // Canonical sizes prevent malformed records and authority-mixed values
        // from entering a legacy fallback.
        if (salt.size != SALT_BYTES || derivedKey.size != DERIVED_KEY_BYTES) return null
        return VersionedCredential(scope, iterations, salt, derivedKey)
    }

    private fun pbkdf2HmacSha256(
        secret: String,
        salt: ByteArray,
        iterations: Int,
        outputBytes: Int
    ): ByteArray {
        require(iterations > 0) { "PBKDF2 iterations must be positive" }
        require(outputBytes > 0) { "PBKDF2 output length must be positive" }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val blockCount = (outputBytes + SHA256_HEX_LENGTH / 2 - 1) / (SHA256_HEX_LENGTH / 2)
        val derived = ByteArray(blockCount * (SHA256_HEX_LENGTH / 2))
        for (blockIndex in 1..blockCount) {
            mac.reset()
            mac.update(salt)
            mac.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(blockIndex).array())
            var u = mac.doFinal()
            val t = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (index in t.indices) t[index] = (t[index].toInt() xor u[index].toInt()).toByte()
            }
            t.copyInto(derived, (blockIndex - 1) * t.size)
        }
        return derived.copyOf(outputBytes)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private fun String.fromHex(): ByteArray? {
        if (length == 0 || length % 2 != 0 || !all { it.digitToIntOrNull(HEX_RADIX) != null }) return null
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(HEX_RADIX).toByte()
        }
    }
}

/** Backwards-compatible top-level helpers used by older call sites. */
fun hashPin(pin: String): String = SecurityUtils.hashPin(pin)
fun verifyPin(enteredPin: String, storedHash: String): Boolean =
    SecurityUtils.verifyPin(enteredPin, storedHash)
