package com.sevenzenlabs.zenmart.recovery

import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupDecryptionException(message: String, cause: Throwable? = null) : IOException(message, cause)

data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertextWithTag: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!iv.contentEquals(other.iv)) return false
        if (!ciphertextWithTag.contentEquals(other.ciphertextWithTag)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + ciphertextWithTag.contentHashCode()
        return result
    }
}

object RecoveryCryptoPolicy {
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val DEFAULT_KDF_ITERATIONS = 65536
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_TAG_LENGTH_BITS = 128
    const val GCM_IV_LENGTH_BYTES = 12
    const val SALT_LENGTH_BYTES = 16
    const val MNEMONIC_WORD_COUNT = 12

    /**
     * Generates a cryptographically random 12-word recovery mnemonic phrase
     * following the BIP-39 standard (128 bits entropy + 4 bits checksum).
     */
    fun generateMnemonic(random: SecureRandom = SecureRandom()): List<String> {
        val entropy = ByteArray(16)
        random.nextBytes(entropy)

        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        val checksumByte = hash[0] // First 4 bits used as checksum

        // Total 132 bits: 128 bits entropy + 4 bits checksum
        val bits = BooleanArray(132)
        for (i in 0 until 16) {
            val byteVal = entropy[i].toInt() and 0xFF
            for (j in 0 until 8) {
                bits[i * 8 + j] = ((byteVal shr (7 - j)) and 1) == 1
            }
        }
        for (j in 0 until 4) {
            bits[128 + j] = (((checksumByte.toInt() and 0xFF) shr (7 - j)) and 1) == 1
        }

        // Split into twelve 11-bit chunks
        val words = ArrayList<String>(MNEMONIC_WORD_COUNT)
        for (i in 0 until MNEMONIC_WORD_COUNT) {
            var index = 0
            for (j in 0 until 11) {
                index = (index shl 1) or (if (bits[i * 11 + j]) 1 else 0)
            }
            words.add(MnemonicWordList.getWord(index))
        }
        return words
    }

    /**
     * Validates a 12-word mnemonic phrase against the BIP-39 wordlist and checksum.
     */
    fun validateMnemonic(words: List<String>): Boolean {
        if (words.size != MNEMONIC_WORD_COUNT) return false
        val indices = ArrayList<Int>(MNEMONIC_WORD_COUNT)
        for (word in words) {
            val idx = MnemonicWordList.indexOf(word) ?: return false
            indices.add(idx)
        }

        val bits = BooleanArray(132)
        for (i in 0 until MNEMONIC_WORD_COUNT) {
            val idx = indices[i]
            for (j in 0 until 11) {
                bits[i * 11 + j] = ((idx shr (10 - j)) and 1) == 1
            }
        }

        val entropy = ByteArray(16)
        for (i in 0 until 16) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bits[i * 8 + j]) {
                    byteVal = byteVal or (1 shl (7 - j))
                }
            }
            entropy[i] = byteVal.toByte()
        }

        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        val expectedChecksumNibble = (hash[0].toInt() and 0xFF) shr 4

        var actualChecksumNibble = 0
        for (j in 0 until 4) {
            if (bits[128 + j]) {
                actualChecksumNibble = actualChecksumNibble or (1 shl (3 - j))
            }
        }

        return expectedChecksumNibble == actualChecksumNibble
    }

    /**
     * Normalizes a recovery phrase into a canonical lowercase single-spaced string.
     */
    fun normalizePhrase(words: List<String>): String =
        words.joinToString(" ") { it.lowercase().trim() }

    /**
     * Parses a raw space-separated string into a normalized list of words.
     */
    fun parsePhrase(raw: String): List<String> =
        raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { it.lowercase() }

    /**
     * Derives a 256-bit AES SecretKey from the normalized recovery phrase and salt.
     */
    fun deriveKey(
        words: List<String>,
        salt: ByteArray,
        iterations: Int = DEFAULT_KDF_ITERATIONS
    ): SecretKey {
        require(words.size == MNEMONIC_WORD_COUNT) { "Recovery phrase must contain exactly 12 words" }
        require(salt.size >= SALT_LENGTH_BYTES) { "Salt must be at least $SALT_LENGTH_BYTES bytes" }
        val normalized = normalizePhrase(words)
        val spec = PBEKeySpec(normalized.toCharArray(), salt, iterations, AES_KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the payload string using AES-256-GCM.
     */
    fun encrypt(
        plaintextJson: String,
        secretKey: SecretKey,
        random: SecureRandom = SecureRandom()
    ): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertextWithTag = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(iv = iv, ciphertextWithTag = ciphertextWithTag)
    }

    /**
     * Decrypts the ciphertext using AES-256-GCM.
     * Throws [BackupDecryptionException] if the key is wrong or ciphertext is corrupt/tampered.
     */
    fun decrypt(
        encryptedPayload: EncryptedPayload,
        secretKey: SecretKey
    ): String = try {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedPayload.iv)
        )
        val plainBytes = cipher.doFinal(encryptedPayload.ciphertextWithTag)
        plainBytes.toString(Charsets.UTF_8)
    } catch (e: Exception) {
        throw BackupDecryptionException("Decryption failed: invalid recovery phrase or corrupted backup file", e)
    }

    /**
     * Computes a SHA-256 fingerprint of the normalized recovery phrase.
     * Stored locally to verify phrase possession without ever storing the plaintext phrase.
     */
    fun computePhraseFingerprint(words: List<String>): String {
        val normalized = normalizePhrase(words)
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a fresh random salt.
     */
    fun generateSalt(random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun String.hexToByteArray(): ByteArray {
        val len = length
        require(len % 2 == 0) { "Hex string must have an even length" }
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
