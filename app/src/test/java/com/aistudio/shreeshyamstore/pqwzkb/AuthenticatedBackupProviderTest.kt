package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudRestorableSnapshot
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreSnapshotCodec
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentitySession
import com.aistudio.shreeshyamstore.pqwzkb.utils.AuthenticatedBackupTableClient
import com.aistudio.shreeshyamstore.pqwzkb.utils.AuthenticatedRestBackupProvider
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupAuthContext
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupHttpResponse
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupMalformedException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupReplayException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupTransport
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUnauthorizedException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUnavailableException
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupWrongTenantException
import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudSyncPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotEnvelope
import com.aistudio.shreeshyamstore.pqwzkb.utils.BackupIncompatibleException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.SocketTimeoutException

class AuthenticatedBackupProviderTest {
    @Test
    fun unauthorizedResponseIsRejectedBeforeRestore() = runBlocking {
        var roomWrites = 0
        val provider = provider {
            BackupHttpResponse(statusCode = 401, body = "{\"error\":\"unauthorized\"}")
        }

        assertThrows(BackupUnauthorizedException::class.java) {
            runBlocking { provider.download(request = com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest("categories")) }
                .also { roomWrites++ }
        }

        assertEquals(0, roomWrites)
    }

    @Test
    fun wrongTenantHeaderIsRejectedBeforeRestore() = runBlocking {
        val provider = provider {
            BackupHttpResponse(
                statusCode = 200,
                body = "[]",
                headers = mapOf("x-backup-tenant" to "store-belongs-to-someone-else")
            )
        }

        assertThrows(BackupWrongTenantException::class.java) {
            runBlocking { provider.download(com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest("categories")) }
        }
        Unit
    }

    @Test
    fun timeoutAndServerErrorAreUnavailable() = runBlocking {
        val serverErrorProvider = provider {
            BackupHttpResponse(statusCode = 503, body = "service unavailable")
        }
        assertThrows(BackupUnavailableException::class.java) {
            runBlocking { serverErrorProvider.download(com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest("categories")) }
        }

        val timeoutProvider = provider { throw SocketTimeoutException("timed out") }
        assertThrows(BackupUnavailableException::class.java) {
            runBlocking { timeoutProvider.download(com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest("categories")) }
        }
        Unit
    }

    @Test
    fun malformedBodyThrowsMalformedException() = runBlocking {
        val client = AuthenticatedBackupTableClient(
            provider {
                BackupHttpResponse(statusCode = 200, body = "{not-json")
            }
        )

        assertThrows(BackupMalformedException::class.java) {
            runBlocking { client.downloadTable("categories", Category::class.java) }
        }
        Unit
    }

    @Test
    fun replayedSnapshotIsRejected() = runBlocking {
        val provider = provider {
            BackupHttpResponse(
                statusCode = 200,
                body = "[]",
                headers = mapOf("x-backup-revision" to "revision-1")
            )
        }
        val first = provider.download(
            com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest("categories")
        )
        assertEquals("revision-1", first.revision)

        assertThrows(BackupReplayException::class.java) {
            runBlocking {
                provider.download(
                    com.aistudio.shreeshyamstore.pqwzkb.utils.BackupDownloadRequest(
                        tableName = "categories",
                        expectedRevision = first.revision
                    )
                )
            }
        }
        Unit
    }

    @Test
    fun successfulAuthenticatedUploadAndDownload() = runBlocking {
        var capturedMethod = ""
        var capturedUrl = ""
        var capturedHeaders: Map<String, String> = emptyMap()
        var capturedUploadBody: String? = null
        val transport = BackupTransport { method, url, headers, body ->
            capturedMethod = method
            capturedUrl = url
            capturedHeaders = headers
            if (method == "PUT") capturedUploadBody = body
            BackupHttpResponse(
                statusCode = 200,
                body = if (method == "GET") "[{\"id\":7,\"name\":\"Dairy\"}]" else "{}",
                headers = mapOf(
                    "x-backup-revision" to "revision-7",
                    "x-backup-tenant" to "store-1"
                )
            )
        }
        val provider = provider(transport)
        val client = AuthenticatedBackupTableClient(provider)

        val upload = client.uploadTable("categories", listOf(Category(id = 7L, name = "Dairy")), Category::class.java)
        val downloaded = client.downloadTable("categories", Category::class.java)

        assertEquals("revision-7", upload.revision)
        assertEquals(1, downloaded.size)
        assertEquals("Dairy", downloaded.single().name)
        assertEquals("GET", capturedMethod)
        assertTrue(capturedUrl.startsWith("https://backup.example.com/db/shreeshyam_sync/scopes/"))
        assertFalse(capturedUrl.contains("firebase-user"))
        assertEquals("Bearer short-lived-token", capturedHeaders["Authorization"])
        assertEquals("firebase-user", capturedHeaders["X-Backup-Identity"])
        assertEquals("store-1", capturedHeaders["X-Backup-Store"])
        assertNotNull(capturedUploadBody)
        assertFalse(capturedUploadBody!!.contains("passwordHash"))
    }

    @Test
    fun authenticatedSnapshotEnvelopeUsesDedicatedScopedPath() = runBlocking {
        var capturedMethod = ""
        var capturedUrl = ""
        var capturedHeaders: Map<String, String> = emptyMap()
        val envelope = SnapshotEnvelope.create(emptySnapshot(), tenantScope())
        val transport = BackupTransport { method, url, headers, _ ->
            capturedMethod = method
            capturedUrl = url
            capturedHeaders = headers
            BackupHttpResponse(
                statusCode = 200,
                body = if (method == "GET") RestoreSnapshotCodec.encode(envelope) else "{}",
                headers = mapOf("x-backup-revision" to "snapshot-revision", "x-backup-tenant" to "store-1")
            )
        }
        val client = AuthenticatedBackupTableClient(provider(transport))

        client.uploadSnapshot(envelope)
        val downloaded = client.downloadSnapshot()

        assertEquals("GET", capturedMethod)
        assertTrue(capturedUrl.endsWith("/snapshot.json"))
        assertTrue(capturedUrl.contains("/scopes/"))
        assertEquals("Bearer short-lived-token", capturedHeaders["Authorization"])
        assertEquals(envelope, downloaded)
    }

    @Test
    fun untrustedHostIsRejectedAtConstruction() {
        assertThrows(BackupIncompatibleException::class.java) {
            AuthenticatedRestBackupProvider(
                baseUrl = "https://evil.example.com/db",
                basePrefix = "shreeshyam_sync",
                auth = authContext(),
                allowedHosts = setOf("backup.example.com"),
                transport = BackupTransport { _, _, _, _ -> BackupHttpResponse(200, "[]") }
            )
        }
    }

    @Test
    fun nonHttpsUrlIsRejectedAtConstruction() {
        assertThrows(BackupIncompatibleException::class.java) {
            AuthenticatedRestBackupProvider(
                baseUrl = "http://backup.example.com/db",
                basePrefix = "shreeshyam_sync",
                auth = authContext(),
                allowedHosts = setOf("backup.example.com"),
                transport = BackupTransport { _, _, _, _ -> BackupHttpResponse(200, "[]") }
            )
        }
    }

    @Test
    fun credentialTablesAreExcludedFromAllowlist() = runBlocking {
        assertTrue(CloudSyncPolicy.isCloudBusinessTable("categories"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("users"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("passwords"))
        assertFalse(CloudSyncPolicy.isCloudBusinessTable("pin_verifiers"))

        val provider = provider { BackupHttpResponse(statusCode = 200, body = "[]") }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                provider.upload(
                    com.aistudio.shreeshyamstore.pqwzkb.utils.BackupUploadRequest(
                        tableName = "users",
                        jsonBody = "[{\"passwordHash\":\"device-only\"}]"
                    )
                )
            }
        }
        Unit
    }

    @Test
    fun localSessionCannotTriggerCloudBackup() {
        val localSession = IdentitySession.localForUser("cashier", "cashier@example.com")
        assertThrows(BackupUnauthorizedException::class.java) {
            BackupAuthContext.fromFirebaseSession(
                session = localSession,
                tenant = tenantScope(),
                bearerToken = "should-not-be-used"
            )
        }
    }

    private fun provider(response: suspend () -> BackupHttpResponse): AuthenticatedRestBackupProvider =
        provider(BackupTransport { _, _, _, _ -> response() })

    private fun provider(transport: BackupTransport): AuthenticatedRestBackupProvider =
        AuthenticatedRestBackupProvider(
            baseUrl = "https://backup.example.com/db",
            basePrefix = "shreeshyam_sync",
            auth = authContext(),
            allowedHosts = setOf("backup.example.com"),
            transport = transport
        )

    private fun emptySnapshot() = CloudRestorableSnapshot(
        categories = emptyList(),
        products = emptyList(),
        sales = emptyList(),
        saleItems = emptyList(),
        customers = emptyList(),
        udhaarTransactions = emptyList(),
        stockAdjustments = emptyList()
    )

    private fun authContext() = BackupAuthContext(
        identityUid = "firebase-user",
        tenant = tenantScope(),
        bearerToken = "short-lived-token"
    )

    private fun tenantScope() = TenantScope(
        organizationId = "org-1",
        storeId = "store-1",
        membershipId = "membership-1",
        deviceId = "device-1",
        appInstallationId = "installation-1"
    )
}
