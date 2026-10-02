package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.recovery.BackupEnvelopeMalformedException
import com.sevenzenlabs.zenmart.recovery.EncryptedBackupCodec
import com.sevenzenlabs.zenmart.recovery.EncryptedBackupEnvelope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptedBackupEnvelopeTest {

    private fun sampleEnvelope() = EncryptedBackupEnvelope(
        createdAtEpochMs = 1700000000000L,
        organizationId = "org-1",
        storeId = "store-1",
        membershipId = "member-1",
        sourceDeviceId = "device-1",
        saltHex = "0102030405060708090a0b0c0d0e0f10",
        ivHex = "0102030405060708090a0b0c",
        ciphertextHex = "abcdef1234567890",
        checksumSha256 = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
        tableCounts = mapOf("products" to 10, "categories" to 2)
    )

    @Test
    fun roundTripSerializationPreservesAllFields() {
        val original = sampleEnvelope()
        val json = EncryptedBackupCodec.encode(original)
        val decoded = EncryptedBackupCodec.decode(json)

        assertEquals(original.magic, decoded.magic)
        assertEquals(original.version, decoded.version)
        assertEquals(original.createdAtEpochMs, decoded.createdAtEpochMs)
        assertEquals(original.organizationId, decoded.organizationId)
        assertEquals(original.storeId, decoded.storeId)
        assertEquals(original.membershipId, decoded.membershipId)
        assertEquals(original.sourceDeviceId, decoded.sourceDeviceId)
        assertEquals(original.saltHex, decoded.saltHex)
        assertEquals(original.ivHex, decoded.ivHex)
        assertEquals(original.ciphertextHex, decoded.ciphertextHex)
        assertEquals(original.checksumSha256, decoded.checksumSha256)
        assertEquals(original.tableCounts, decoded.tableCounts)
    }

    @Test
    fun invalidMagicHeaderIsRejected() {
        val json = EncryptedBackupCodec.encode(sampleEnvelope())
        val tamperedJson = json.replace(EncryptedBackupEnvelope.MAGIC_HEADER, "INVALID_MAGIC")

        assertThrows(BackupEnvelopeMalformedException::class.java) {
            EncryptedBackupCodec.decode(tamperedJson)
        }
    }

    @Test
    fun unsupportedVersionIsRejected() {
        val json = EncryptedBackupCodec.encode(sampleEnvelope())
        val tamperedJson = json.replace("\"version\":1", "\"version\":999")

        assertThrows(BackupEnvelopeMalformedException::class.java) {
            EncryptedBackupCodec.decode(tamperedJson)
        }
    }

    @Test
    fun malformedJsonIsRejected() {
        assertThrows(BackupEnvelopeMalformedException::class.java) {
            EncryptedBackupCodec.decode("{not a valid json")
        }
    }
}
