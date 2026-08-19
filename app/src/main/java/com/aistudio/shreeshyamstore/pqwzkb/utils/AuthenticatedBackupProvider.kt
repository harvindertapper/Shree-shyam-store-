package com.aistudio.shreeshyamstore.pqwzkb.utils

import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.IdentitySession
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class BackupProviderException(message: String) : IOException(message)

class BackupUnauthorizedException(message: String = "Backup provider authorization failed") : BackupProviderException(message)
class BackupWrongTenantException(message: String = "Backup provider returned the wrong tenant scope") : BackupProviderException(message)
class BackupUnavailableException(message: String, cause: Throwable? = null) : BackupProviderException(message) {
    init {
        cause?.let(::initCause)
    }
}
class BackupMalformedException(message: String = "Backup provider returned malformed data") : BackupProviderException(message)
class BackupIncompatibleException(message: String = "Backup provider returned an incompatible snapshot") : BackupProviderException(message)
class BackupReplayException(message: String = "Backup provider returned a replayed snapshot") : BackupProviderException(message)

/** Identity and scope resolved from the current authenticated local session. */
data class BackupAuthContext(
    val identityUid: String,
    val tenant: TenantScope,
    val bearerToken: String
) {
    init {
        require(identityUid.trim().isNotEmpty()) { "Backup identity is required" }
        require(bearerToken.trim().isNotEmpty()) { "Backup bearer token is required" }
    }

    companion object {
        fun fromFirebaseSession(
            session: IdentitySession,
            tenant: TenantScope,
            bearerToken: String
        ): BackupAuthContext {
            if (session.provider != IdentityProvider.FIREBASE) {
                throw BackupUnauthorizedException("Cloud backup requires a Firebase-authenticated session")
            }
            if (session.uid.trim().isEmpty()) {
                throw BackupUnauthorizedException("Cloud backup identity is unavailable")
            }
            return BackupAuthContext(session.uid, tenant, bearerToken)
        }
    }
}

data class BackupUploadRequest(
    val tableName: String,
    val jsonBody: String,
    val revision: String? = null
)

data class BackupDownloadRequest(
    val tableName: String,
    val expectedRevision: String? = null
)

data class BackupUploadResult(val revision: String?)
data class BackupDownloadResult(val jsonBody: String, val revision: String?)

data class BackupHttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)

fun interface BackupTransport {
    suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): BackupHttpResponse
}

interface AuthenticatedBackupProvider {
    suspend fun upload(request: BackupUploadRequest): BackupUploadResult
    suspend fun download(request: BackupDownloadRequest): BackupDownloadResult
}

class AuthenticatedBackupTableClient(
    private val provider: AuthenticatedBackupProvider
) {
    suspend fun <T> uploadTable(tableName: String, records: List<T>, clazz: Class<T>): BackupUploadResult =
        provider.upload(
            BackupUploadRequest(
                tableName = tableName,
                jsonBody = BackupPayloadCodec.encode(records, clazz)
            )
        )

    suspend fun <T> downloadTable(
        tableName: String,
        clazz: Class<T>,
        expectedRevision: String? = null
    ): List<T> {
        val result = provider.download(
            BackupDownloadRequest(tableName = tableName, expectedRevision = expectedRevision)
        )
        return BackupPayloadCodec.decode(result.jsonBody, clazz)
    }
}

private object BackupPayloadCodec {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun <T> encode(records: List<T>, clazz: Class<T>): String {
        val type = Types.newParameterizedType(List::class.java, clazz)
        return moshi.adapter<List<T>>(type).serializeNulls().toJson(records)
    }

    fun <T> decode(body: String, clazz: Class<T>): List<T> = try {
        val type = Types.newParameterizedType(List::class.java, clazz)
        moshi.adapter<List<T>>(type).fromJson(body).orEmpty()
    } catch (_: Exception) {
        throw BackupMalformedException()
    }
}

/**
 * Transitional REST provider. The caller supplies a short-lived Firebase ID
 * token; this class never persists it or places it in business payloads.
 */
class AuthenticatedRestBackupProvider(
    baseUrl: String,
    basePrefix: String,
    private val auth: BackupAuthContext,
    allowedHosts: Set<String>,
    private val transport: BackupTransport = OkHttpBackupTransport
) : AuthenticatedBackupProvider {
    private val normalizedBaseUrl = normalizeHttpsBaseUrl(baseUrl)
    private val normalizedHost = URI(normalizedBaseUrl).host.orEmpty().lowercase()
    private val trustedHosts = allowedHosts.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    private val scopedPrefix = BackupPathPolicy.scopedPrefix(basePrefix, auth.tenant)

    init {
        if (trustedHosts.isEmpty() || normalizedHost !in trustedHosts) {
            throw BackupIncompatibleException("Backup provider host is not trusted")
        }
    }

    override suspend fun upload(request: BackupUploadRequest): BackupUploadResult {
        validateTable(request.tableName)
        require(request.jsonBody.isNotBlank()) { "Backup payload cannot be empty" }
        val response = execute(
            method = "PUT",
            tableName = request.tableName,
            body = request.jsonBody,
            revision = request.revision
        )
        val revision = response.headers.headerValue("x-backup-revision")
        return BackupUploadResult(revision)
    }

    override suspend fun download(request: BackupDownloadRequest): BackupDownloadResult {
        validateTable(request.tableName)
        val response = execute(method = "GET", tableName = request.tableName, body = null, revision = null)
        val revision = response.headers.headerValue("x-backup-revision")
        if (request.expectedRevision != null && request.expectedRevision == revision) {
            throw BackupReplayException()
        }
        if (response.body.isBlank() || response.body.trim() == "null") {
            throw BackupMalformedException("Backup table ${request.tableName} is empty")
        }
        return BackupDownloadResult(response.body, revision)
    }

    private suspend fun execute(
        method: String,
        tableName: String,
        body: String?,
        revision: String?
    ): BackupHttpResponse {
        val url = "$normalizedBaseUrl/$scopedPrefix/${tableName.trim()}.json"
        val headers = linkedMapOf(
            "Authorization" to "Bearer ${auth.bearerToken.trim()}",
            "Accept" to "application/json",
            "X-Backup-Identity" to auth.identityUid.trim(),
            "X-Backup-Organization" to auth.tenant.organizationId,
            "X-Backup-Store" to auth.tenant.storeId,
            "X-Backup-Membership" to auth.tenant.membershipId,
            "X-Backup-Device" to auth.tenant.deviceId,
            "X-Backup-Installation" to auth.tenant.appInstallationId
        )
        if (!revision.isNullOrBlank()) headers["If-Match"] = revision
        val response = try {
            transport.execute(method, url, headers, body)
        } catch (error: BackupProviderException) {
            throw error
        } catch (error: Exception) {
            throw BackupUnavailableException("Backup provider is unavailable", error)
        }
        validateResponse(response)
        return response
    }

    private fun validateResponse(response: BackupHttpResponse) {
        val returnedTenant = response.headers.headerValue("x-backup-tenant")
        if (returnedTenant != null && returnedTenant != auth.tenant.storeId && returnedTenant != auth.tenant.organizationId) {
            throw BackupWrongTenantException()
        }
        when (response.statusCode) {
            401, 403 -> throw BackupUnauthorizedException()
            in 200..299 -> Unit
            408, 429, in 500..599 -> throw BackupUnavailableException("Backup provider is temporarily unavailable")
            409 -> throw BackupReplayException()
            415, 422 -> throw BackupIncompatibleException()
            else -> throw BackupUnavailableException("Backup provider returned HTTP ${response.statusCode}")
        }
    }

    private fun validateTable(tableName: String) {
        require(CloudSyncPolicy.isCloudBusinessTable(tableName)) {
            "Backup table is outside the cloud business allowlist"
        }
    }

    companion object {
        private fun normalizeHttpsBaseUrl(raw: String): String {
            val value = raw.trim()
            if (value.isEmpty()) {
                throw BackupIncompatibleException("Backup provider URL is required")
            }
            val uri = runCatching { URI(value) }.getOrElse {
                throw BackupIncompatibleException("Backup provider URL is invalid")
            }
            if (!uri.scheme.equals("https", ignoreCase = true)) {
                throw BackupIncompatibleException("Backup provider requires HTTPS")
            }
            if (uri.host.isNullOrBlank()) {
                throw BackupIncompatibleException("Backup provider URL host is required")
            }
            return value.trimEnd('/')
        }
    }
}

object BackupPathPolicy {
    fun scopedPrefix(basePrefix: String, tenant: TenantScope): String {
        val prefix = basePrefix.trim().trim('/').ifEmpty { "shreeshyam_sync" }
        require(prefix.split('/').all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." &&
                segment.none { it in setOf('#', '$', '[', ']') }
        }) { "Backup prefix is invalid" }
        val scopeHash = sha256Hex(
            listOf(
                tenant.organizationId,
                tenant.storeId,
                tenant.membershipId
            ).joinToString("/")
        )
        return "$prefix/scopes/$scopeHash"
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

private object OkHttpBackupTransport : BackupTransport {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): BackupHttpResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
        headers.forEach { (name, value) -> builder.header(name, value) }
        when (method) {
            "GET" -> builder.get()
            "PUT" -> builder.put((body.orEmpty()).toRequestBody(JSON_MEDIA_TYPE))
            else -> throw BackupIncompatibleException("Unsupported backup HTTP method")
        }
        client.newCall(builder.build()).execute().use { response ->
            BackupHttpResponse(
                statusCode = response.code,
                body = response.body?.string().orEmpty(),
                headers = response.headers.toMultimap().mapValues { it.value.lastOrNull().orEmpty() }
            )
        }
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}

private fun Map<String, String>.headerValue(name: String): String? = entries
    .firstOrNull { it.key.equals(name, ignoreCase = true) }
    ?.value
    ?.trim()
    ?.ifEmpty { null }
