package com.sevenzenlabs.zenmart.recovery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

data class SafFolderVerification(
    val isVerified: Boolean,
    val providerName: String,
    val errorMessage: String? = null
)

data class SafBackupFileEntry(
    val filename: String,
    val uri: Uri,
    val lastModifiedEpochMs: Long,
    val sizeBytes: Long
)

interface SafStorageClient {
    fun verifyAndPersistFolder(treeUri: Uri): SafFolderVerification
    fun writeBackupFile(treeUri: Uri, filename: String, content: String): Uri
    fun readBackupFile(fileUri: Uri): String
    fun listBackupFiles(treeUri: Uri): List<SafBackupFileEntry>
    fun deleteBackupFile(fileUri: Uri): Boolean
    fun getProviderDisplayName(treeUri: Uri): String
}

class AndroidSafStorageClient(private val context: Context) : SafStorageClient {

    override fun verifyAndPersistFolder(treeUri: Uri): SafFolderVerification = try {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val probeName = ".zenmart_probe_${System.currentTimeMillis()}.tmp"
        val probeUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            "application/octet-stream",
            probeName
        ) ?: return SafFolderVerification(false, "", "Provider returned null probe document")

        val probeContent = "zenmart_saf_probe_${System.currentTimeMillis()}"
        context.contentResolver.openOutputStream(probeUri)?.use { out ->
            out.write(probeContent.toByteArray(Charsets.UTF_8))
        } ?: run {
            try {
                DocumentsContract.deleteDocument(context.contentResolver, probeUri)
            } catch (_: Exception) {}
            return SafFolderVerification(false, "", "Cannot open write stream to probe document")
        }

        val readBack = context.contentResolver.openInputStream(probeUri)?.use { inp ->
            inp.readBytes().decodeToString()
        }
        try {
            DocumentsContract.deleteDocument(context.contentResolver, probeUri)
        } catch (_: Exception) {}

        if (readBack != probeContent) {
            SafFolderVerification(false, "", "Probe read-back content mismatch")
        } else {
            val providerName = getProviderDisplayName(treeUri)
            SafFolderVerification(true, providerName, null)
        }
    } catch (e: Exception) {
        SafFolderVerification(false, "", e.message ?: "Permission verification failed")
    }

    override fun getProviderDisplayName(treeUri: Uri): String = when (treeUri.authority) {
        "com.google.android.apps.docs.storage" -> "Google Drive"
        "com.android.externalstorage.documents" -> "Device Storage / SD Card"
        "com.android.providers.downloads.documents" -> "Downloads"
        else -> treeUri.authority ?: "External Storage"
    }

    override fun writeBackupFile(treeUri: Uri, filename: String, content: String): Uri {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            "application/octet-stream",
            filename
        ) ?: throw IOException("Unable to create backup document in selected folder")

        context.contentResolver.openOutputStream(fileUri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Unable to open output stream for backup document")

        return fileUri
    }

    override fun readBackupFile(fileUri: Uri): String =
        context.contentResolver.openInputStream(fileUri)?.use { inp ->
            inp.readBytes().decodeToString()
        } ?: throw IOException("Unable to open input stream for backup document")

    override fun listBackupFiles(treeUri: Uri): List<SafBackupFileEntry> {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        val results = mutableListOf<SafBackupFileEntry>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val docId = if (idCol >= 0) cursor.getString(idCol) else null
                val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                val lastMod = if (modCol >= 0) cursor.getLong(modCol) else 0L
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                if (docId != null && name != null && (name.startsWith("zenmart_backup_") || name.endsWith(".zmb"))) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    results.add(SafBackupFileEntry(name, docUri, lastMod, size))
                }
            }
        }
        return results
    }

    override fun deleteBackupFile(fileUri: Uri): Boolean = try {
        DocumentsContract.deleteDocument(context.contentResolver, fileUri)
    } catch (_: Exception) {
        false
    }
}
