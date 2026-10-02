package com.sevenzenlabs.zenmart.recovery

import android.net.Uri
import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.data.Category
import com.sevenzenlabs.zenmart.data.Customer
import com.sevenzenlabs.zenmart.data.Product
import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.ReturnItem
import com.sevenzenlabs.zenmart.data.Sale
import com.sevenzenlabs.zenmart.data.SaleItem
import com.sevenzenlabs.zenmart.data.SettingsDataStore
import com.sevenzenlabs.zenmart.data.ShopRepository
import com.sevenzenlabs.zenmart.data.StockAdjustment
import com.sevenzenlabs.zenmart.data.UdhaarTransaction
import com.sevenzenlabs.zenmart.recovery.RecoveryCryptoPolicy.hexToByteArray
import com.sevenzenlabs.zenmart.recovery.RecoveryCryptoPolicy.toHex
import com.sevenzenlabs.zenmart.utils.CloudRestorableSnapshot
import com.sevenzenlabs.zenmart.utils.LocalRecoveryPointStore
import com.sevenzenlabs.zenmart.utils.RestoreRecoveryCoordinator
import com.sevenzenlabs.zenmart.utils.RestoreSnapshotCodec
import com.sevenzenlabs.zenmart.utils.RestoreSnapshotPolicy
import com.sevenzenlabs.zenmart.utils.RestoreSnapshotValidator
import com.sevenzenlabs.zenmart.utils.SnapshotEnvelope
import com.sevenzenlabs.zenmart.utils.SnapshotTableCounts
import com.sevenzenlabs.zenmart.utils.SnapshotTenantMismatchException
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SafExportResult(
    val success: Boolean,
    val filename: String,
    val timestampEpochMs: Long,
    val message: String,
    val tableCounts: Map<String, Int> = emptyMap()
)

data class SafRestoreResult(
    val success: Boolean,
    val message: String,
    val tableCounts: Map<String, Int> = emptyMap()
)

class SafBackupCoordinator(
    private val safStorage: SafStorageClient,
    private val settingsDataStore: SettingsDataStore,
    private val repository: ShopRepository,
    private val recoveryPointStore: LocalRecoveryPointStore
) {

    /**
     * Executes an encrypted backup export to the specified SAF tree folder.
     * Validates recovery phrase, gathers current business records, derives AES key,
     * encrypts payload, writes file, verifies via immediate read-back and decryption,
     * prunes older backups according to 7-daily + 4-weekly policy, and updates settings.
     */
    suspend fun exportEncryptedBackup(
        treeUri: Uri,
        phraseWords: List<String>,
        tenant: TenantScope,
        nowEpochMs: Long = System.currentTimeMillis(),
        allowEmpty: Boolean = false
    ): SafExportResult {
        if (!RecoveryCryptoPolicy.validateMnemonic(phraseWords)) {
            throw IllegalArgumentException("Invalid recovery phrase. All 12 words and checksum must match.")
        }

        val snapshot = CloudRestorableSnapshot(
            categories = repository.allCategories.first(),
            products = repository.allProducts.first(),
            sales = repository.allSales.first(),
            saleItems = repository.getAllSaleItems(),
            customers = repository.allCustomers.first(),
            udhaarTransactions = repository.allUdhaarTransactions.first(),
            stockAdjustments = repository.getAllStockAdjustmentsList(),
            returns = repository.allReturnsList(),
            returnItems = repository.getAllReturnItemsList()
        )

        if (!allowEmpty && snapshot.isEmpty()) {
            val failureMsg = "Cannot export empty database"
            settingsDataStore.updateSafExportStatus(
                status = "FAILED",
                lastEpochMs = nowEpochMs,
                filename = "",
                errorMsg = failureMsg
            )
            return SafExportResult(
                success = false,
                filename = "",
                timestampEpochMs = nowEpochMs,
                message = failureMsg
            )
        }

        val normalized = RestoreSnapshotValidator.normalizeLegacyIdentities(snapshot)
        val tableCounts = SnapshotTableCounts.from(normalized).asMap()
        val checksumSha256 = RestoreSnapshotCodec.checksum(normalized)

        val plaintextEnvelope = SnapshotEnvelope(
            schemaVersion = RestoreSnapshotPolicy.CURRENT_SCHEMA_VERSION,
            organizationId = tenant.organizationId,
            storeId = tenant.storeId,
            membershipId = tenant.membershipId,
            createdAtEpochMs = nowEpochMs,
            sourceDeviceId = tenant.deviceId,
            sourceAppInstallationId = tenant.appInstallationId,
            tableCounts = tableCounts,
            checksumSha256 = checksumSha256,
            complete = true,
            snapshot = normalized
        )
        val plainPayloadJson = RestoreSnapshotCodec.encode(plaintextEnvelope)

        val salt = RecoveryCryptoPolicy.generateSalt()
        val key = RecoveryCryptoPolicy.deriveKey(phraseWords, salt)
        val encrypted = RecoveryCryptoPolicy.encrypt(plainPayloadJson, key)

        val backupEnvelope = EncryptedBackupEnvelope(
            createdAtEpochMs = nowEpochMs,
            organizationId = tenant.organizationId,
            storeId = tenant.storeId,
            membershipId = tenant.membershipId,
            sourceDeviceId = tenant.deviceId,
            saltHex = salt.toHex(),
            ivHex = encrypted.iv.toHex(),
            ciphertextHex = encrypted.ciphertextWithTag.toHex(),
            checksumSha256 = checksumSha256,
            tableCounts = tableCounts
        )
        val envelopeJson = EncryptedBackupCodec.encode(backupEnvelope)

        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val filename = "zenmart_backup_${sdf.format(Date(nowEpochMs))}.zmb"

        val fileUri = safStorage.writeBackupFile(treeUri, filename, envelopeJson)

        // Read-back verification
        try {
            val readBackText = safStorage.readBackupFile(fileUri)
            val readBackEnvelope = EncryptedBackupCodec.decode(readBackText)
            val readBackSalt = readBackEnvelope.saltHex.hexToByteArray()
            val readBackIv = readBackEnvelope.ivHex.hexToByteArray()
            val readBackCiphertext = readBackEnvelope.ciphertextHex.hexToByteArray()
            val readBackKey = RecoveryCryptoPolicy.deriveKey(phraseWords, readBackSalt, readBackEnvelope.kdfIterations)
            val decryptedJson = RecoveryCryptoPolicy.decrypt(
                EncryptedPayload(readBackIv, readBackCiphertext),
                readBackKey
            )
            val verifiedSnapshotEnvelope = RestoreSnapshotCodec.decode(decryptedJson)
            val verifiedSnapshot = RestoreSnapshotValidator.validate(
                verifiedSnapshotEnvelope,
                tenant,
                nowEpochMs = nowEpochMs,
                allowEmpty = allowEmpty
            )
            if (verifiedSnapshotEnvelope.checksumSha256 != checksumSha256) {
                safStorage.deleteBackupFile(fileUri)
                throw IOException("Read-back verification checksum mismatch")
            }
        } catch (e: Exception) {
            safStorage.deleteBackupFile(fileUri)
            val errorMsg = "Read-back verification failed: ${e.message}"
            settingsDataStore.updateSafExportStatus(
                status = "FAILED",
                lastEpochMs = nowEpochMs,
                filename = filename,
                errorMsg = errorMsg
            )
            return SafExportResult(
                success = false,
                filename = filename,
                timestampEpochMs = nowEpochMs,
                message = errorMsg
            )
        }

        // Apply retention pruning
        try {
            val existingFiles = safStorage.listBackupFiles(treeUri)
            val records = existingFiles.map { file ->
                BackupFileRecord(file.filename, file.lastModifiedEpochMs, file.uri.toString())
            }
            val retention = BackupRetentionPolicy.evaluateRetention(records)
            for (toPrune in retention.filesToPrune) {
                val entry = existingFiles.firstOrNull { it.filename == toPrune.filename }
                if (entry != null && entry.uri != fileUri) {
                    safStorage.deleteBackupFile(entry.uri)
                }
            }
        } catch (_: Exception) {
            // Retention cleanup failure does not invalidate successful export
        }

        settingsDataStore.updateSafExportStatus(
            status = "SUCCESS",
            lastEpochMs = nowEpochMs,
            filename = filename,
            errorMsg = null
        )

        return SafExportResult(
            success = true,
            filename = filename,
            timestampEpochMs = nowEpochMs,
            message = "Backup successfully verified and saved",
            tableCounts = tableCounts
        )
    }

    /**
     * Restores an encrypted backup from the specified SAF file URI.
     * Decrypts with [phraseWords], validates store matching and table integrity,
     * saves a pre-restore recovery point, and replaces local database with rollback support.
     */
    suspend fun restoreEncryptedBackup(
        fileUri: Uri,
        phraseWords: List<String>,
        currentTenant: TenantScope
    ): SafRestoreResult {
        if (!RecoveryCryptoPolicy.validateMnemonic(phraseWords)) {
            throw IllegalArgumentException("Invalid recovery phrase")
        }

        val rawText = safStorage.readBackupFile(fileUri)
        val envelope = EncryptedBackupCodec.decode(rawText)

        if (currentTenant.storeId.isNotBlank() && envelope.storeId != currentTenant.storeId) {
            throw SnapshotTenantMismatchException("Backup belongs to store ${envelope.storeId}, not active store ${currentTenant.storeId}")
        }

        val salt = envelope.saltHex.hexToByteArray()
        val iv = envelope.ivHex.hexToByteArray()
        val ciphertext = envelope.ciphertextHex.hexToByteArray()
        val key = RecoveryCryptoPolicy.deriveKey(phraseWords, salt, envelope.kdfIterations)
        val decryptedJson = RecoveryCryptoPolicy.decrypt(EncryptedPayload(iv, ciphertext), key)

        val snapshotEnvelope = RestoreSnapshotCodec.decode(decryptedJson)
        val effectiveTenant = if (currentTenant.storeId.isNotBlank()) currentTenant else TenantScope(
            organizationId = snapshotEnvelope.organizationId,
            storeId = snapshotEnvelope.storeId,
            membershipId = snapshotEnvelope.membershipId,
            deviceId = currentTenant.deviceId,
            appInstallationId = currentTenant.appInstallationId
        )

        val validatedSnapshot = RestoreSnapshotValidator.validate(
            snapshotEnvelope,
            effectiveTenant
        )

        // Capture pre-restore snapshot for atomic rollback
        val preRestoreSnapshot = CloudRestorableSnapshot(
            categories = repository.allCategories.first(),
            products = repository.allProducts.first(),
            sales = repository.allSales.first(),
            saleItems = repository.getAllSaleItems(),
            customers = repository.allCustomers.first(),
            udhaarTransactions = repository.allUdhaarTransactions.first(),
            stockAdjustments = repository.getAllStockAdjustmentsList(),
            returns = repository.allReturnsList(),
            returnItems = repository.getAllReturnItemsList()
        )
        val preRestoreEnvelope = SnapshotEnvelope.create(preRestoreSnapshot, effectiveTenant)
        recoveryPointStore.save(preRestoreEnvelope)

        RestoreRecoveryCoordinator { restored: CloudRestorableSnapshot ->
            repository.replaceCloudRestorableTables(
                categoriesList = restored.categories,
                productsList = restored.products,
                salesList = restored.sales,
                saleItemsList = restored.saleItems,
                customersList = restored.customers,
                udhaarTxsList = restored.udhaarTransactions,
                adjustmentsList = restored.stockAdjustments,
                returnsList = restored.returns,
                returnItemsList = restored.returnItems
            )
        }.replaceWithRollback(validatedSnapshot, effectiveTenant, preRestoreEnvelope)

        return SafRestoreResult(
            success = true,
            message = "Backup restored successfully",
            tableCounts = snapshotEnvelope.tableCounts
        )
    }
}
