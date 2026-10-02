package com.sevenzenlabs.zenmart.recovery

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

class BackupEnvelopeMalformedException(message: String, cause: Throwable? = null) : IOException(message, cause)

data class EncryptedBackupEnvelope(
    val magic: String = MAGIC_HEADER,
    val version: Int = CURRENT_VERSION,
    val createdAtEpochMs: Long,
    val organizationId: String,
    val storeId: String,
    val membershipId: String,
    val sourceDeviceId: String,
    val kdfAlgorithm: String = RecoveryCryptoPolicy.KDF_ALGORITHM,
    val kdfIterations: Int = RecoveryCryptoPolicy.DEFAULT_KDF_ITERATIONS,
    val saltHex: String,
    val ivHex: String,
    val ciphertextHex: String,
    val checksumSha256: String,
    val tableCounts: Map<String, Int>
) {
    companion object {
        const val MAGIC_HEADER = "ZENMART_SAF_V1"
        const val CURRENT_VERSION = 1
    }
}

object EncryptedBackupCodec {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(EncryptedBackupEnvelope::class.java)

    fun encode(envelope: EncryptedBackupEnvelope): String =
        adapter.serializeNulls().toJson(envelope)

    fun decode(json: String): EncryptedBackupEnvelope = try {
        val envelope = adapter.fromJson(json)
            ?: throw BackupEnvelopeMalformedException("Backup envelope JSON parsed to null")
        if (envelope.magic != EncryptedBackupEnvelope.MAGIC_HEADER) {
            throw BackupEnvelopeMalformedException("Unknown backup format magic: ${envelope.magic}")
        }
        if (envelope.version != EncryptedBackupEnvelope.CURRENT_VERSION) {
            throw BackupEnvelopeMalformedException("Unsupported backup version: ${envelope.version}")
        }
        envelope
    } catch (e: BackupEnvelopeMalformedException) {
        throw e
    } catch (e: Exception) {
        throw BackupEnvelopeMalformedException("Malformed backup envelope", e)
    }
}
