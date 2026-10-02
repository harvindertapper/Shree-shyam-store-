package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.recovery.BackupDecryptionException
import com.sevenzenlabs.zenmart.recovery.MnemonicWordList
import com.sevenzenlabs.zenmart.recovery.RecoveryCryptoPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCryptoPolicyTest {

    @Test
    fun mnemonicGenerationProducesValid12Words() {
        val mnemonic = RecoveryCryptoPolicy.generateMnemonic()
        assertEquals(12, mnemonic.size)

        mnemonic.forEach { word ->
            assertTrue(
                "Word '$word' must exist in standard BIP-39 wordlist",
                MnemonicWordList.indexOf(word) != null
            )
        }

        assertTrue(
            "Generated mnemonic must pass checksum verification",
            RecoveryCryptoPolicy.validateMnemonic(mnemonic)
        )
    }

    @Test
    fun mnemonicValidationRejectsWrongWordCounts() {
        val validMnemonic = RecoveryCryptoPolicy.generateMnemonic()
        assertFalse(RecoveryCryptoPolicy.validateMnemonic(validMnemonic.take(11)))
        assertFalse(RecoveryCryptoPolicy.validateMnemonic(validMnemonic + "abandon"))
        assertFalse(RecoveryCryptoPolicy.validateMnemonic(emptyList()))
    }

    @Test
    fun mnemonicValidationRejectsUnknownWords() {
        val validMnemonic = RecoveryCryptoPolicy.generateMnemonic().toMutableList()
        validMnemonic[0] = "invalidwordxyz"
        assertFalse(RecoveryCryptoPolicy.validateMnemonic(validMnemonic))
    }

    @Test
    fun mnemonicValidationRejectsCorruptedChecksum() {
        val validMnemonic = RecoveryCryptoPolicy.generateMnemonic().toMutableList()
        val originalLastWord = validMnemonic[11]
        val otherWord = if (originalLastWord == "abandon") "ability" else "abandon"
        validMnemonic[11] = otherWord

        val checksumMatches = RecoveryCryptoPolicy.validateMnemonic(validMnemonic)
        if (checksumMatches) {
            validMnemonic[11] = "zoo"
            assertFalse(RecoveryCryptoPolicy.validateMnemonic(validMnemonic))
        } else {
            assertFalse(checksumMatches)
        }
    }

    @Test
    fun keyDerivationIsDeterministic() {
        val mnemonic = RecoveryCryptoPolicy.generateMnemonic()
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val key1 = RecoveryCryptoPolicy.deriveKey(mnemonic, salt)
        val key2 = RecoveryCryptoPolicy.deriveKey(mnemonic, salt)

        assertEquals("AES", key1.algorithm)
        assertEquals(32, key1.encoded.size)
        assertTrue(key1.encoded.contentEquals(key2.encoded))

        val diffSalt = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6)
        val keyDiffSalt = RecoveryCryptoPolicy.deriveKey(mnemonic, diffSalt)
        assertFalse(key1.encoded.contentEquals(keyDiffSalt.encoded))
    }

    @Test
    fun encryptionDecryptionRoundTrip() {
        val mnemonic = RecoveryCryptoPolicy.generateMnemonic()
        val salt = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 13, 14, 15, 16)
        val key = RecoveryCryptoPolicy.deriveKey(mnemonic, salt)

        val plaintext = "{\"shopName\":\"Shree Shyam Store\",\"version\":1,\"recordCount\":42}"
        val encrypted = RecoveryCryptoPolicy.encrypt(plaintext, key)

        assertEquals(12, encrypted.iv.size)
        assertTrue(encrypted.ciphertextWithTag.isNotEmpty())
        assertNotEquals(plaintext, String(encrypted.ciphertextWithTag, Charsets.UTF_8))

        val decrypted = RecoveryCryptoPolicy.decrypt(encrypted, key)
        assertEquals(plaintext, decrypted)
    }

    @Test(expected = BackupDecryptionException::class)
    fun decryptionFailsOnTamperedCiphertext() {
        val mnemonic = RecoveryCryptoPolicy.generateMnemonic()
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val key = RecoveryCryptoPolicy.deriveKey(mnemonic, salt)

        val plaintext = "Sensitive inventory data"
        val encrypted = RecoveryCryptoPolicy.encrypt(plaintext, key)

        val tampered = encrypted.ciphertextWithTag.copyOf()
        tampered[0] = (tampered[0].toInt() xor 0xFF).toByte()

        RecoveryCryptoPolicy.decrypt(encrypted.copy(ciphertextWithTag = tampered), key)
    }

    @Test(expected = BackupDecryptionException::class)
    fun decryptionFailsWithWrongKey() {
        val mnemonic1 = RecoveryCryptoPolicy.generateMnemonic()
        val mnemonic2 = RecoveryCryptoPolicy.generateMnemonic()
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val key1 = RecoveryCryptoPolicy.deriveKey(mnemonic1, salt)
        val key2 = RecoveryCryptoPolicy.deriveKey(mnemonic2, salt)

        val plaintext = "Store secrets"
        val encrypted = RecoveryCryptoPolicy.encrypt(plaintext, key1)

        RecoveryCryptoPolicy.decrypt(encrypted, key2)
    }

    @Test
    fun computePhraseFingerprintIsStableAndNormalized() {
        val words = listOf("abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident")
        val fp1 = RecoveryCryptoPolicy.computePhraseFingerprint(words)
        val fp2 = RecoveryCryptoPolicy.computePhraseFingerprint(listOf("ABANDON", " Ability ", "ABLE", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident"))

        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length)
    }
}
