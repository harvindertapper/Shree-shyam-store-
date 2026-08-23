package com.aistudio.shreeshyamstore.pqwzkb.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLockState
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginPolicy
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalLoginResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.PinUnlockResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private const val DEFAULT_FIREBASE_PREFIX = "shreeshyam_sync"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "store_settings")

data class StoreSettings(
    val shopName: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val staticPaytmQrImageUri: String = "",
    val welcomeChantEnabled: Boolean = true,
    val firstLaunchCompleted: Boolean = false,
    val loggedInUid: String = "",
    val loggedInUsername: String = "",
    val loggedInEmail: String = "",
    val loggedInRole: String = "OWNER",
    val identityProvider: IdentityProvider? = null,
    val auditDeviceId: String = "",
    val organizationId: String = "",
    val storeId: String = "",
    val membershipId: String = "",
    val deviceId: String = "",
    val appInstallationId: String = "",
    val isUserLoggedIn: Boolean = false,
    val appLockEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    /** Device-local verifier; blank means the legacy default-PIN migration window is active. */
    val securityPin: String = "",
    val securityPinFormatVersion: Int = SecurityUtils.LEGACY_CREDENTIAL_VERSION,
    val failedPinAttempts: Int = 0,
    val pinLockedUntilEpochMs: Long = 0L,
    val lastUnlockAtEpochMs: Long = 0L,
    val localLoginFailedAttempts: Int = 0,
    val localLoginLockedUntilEpochMs: Long = 0L,
    val firebaseUrl: String = "",
    val firebasePrefix: String = DEFAULT_FIREBASE_PREFIX,
    val lastSyncTime: String = "Never Synced",
    val lastSyncStatus: SyncRunStatus = SyncRunStatus.UNKNOWN,
    val autoSyncEnabled: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.HINDI
)

class SettingsDataStore(private val context: Context) {
    companion object {
        private val SHOP_NAME = stringPreferencesKey("shop_name")
        private val OWNER_NAME = stringPreferencesKey("owner_name")
        private val OWNER_PHONE = stringPreferencesKey("owner_phone")
        private val STATIC_PAYTM_QR_IMAGE_URI = stringPreferencesKey("static_paytm_qr_image_uri")
        private val WELCOME_CHANT_ENABLED = booleanPreferencesKey("welcome_chant_enabled")
        private val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
        private val LOGGED_IN_UID = stringPreferencesKey("logged_in_uid")
        private val LOGGED_IN_USERNAME = stringPreferencesKey("logged_in_username")
        private val LOGGED_IN_EMAIL = stringPreferencesKey("logged_in_email")
        private val LOGGED_IN_ROLE = stringPreferencesKey("logged_in_role")
        private val IDENTITY_PROVIDER = stringPreferencesKey("identity_provider")
        private val AUDIT_DEVICE_ID = stringPreferencesKey("audit_device_id")
        private val ORGANIZATION_ID = stringPreferencesKey("organization_id")
        private val STORE_ID = stringPreferencesKey("store_id")
        private val MEMBERSHIP_ID = stringPreferencesKey("membership_id")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val APP_INSTALLATION_ID = stringPreferencesKey("app_installation_id")
        private val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val SECURITY_PIN = stringPreferencesKey("security_pin")
        private val SECURITY_PIN_FORMAT_VERSION = intPreferencesKey("security_pin_format_version")
        private val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        private val PIN_LOCKED_UNTIL_EPOCH_MS = longPreferencesKey("pin_locked_until_epoch_ms")
        private val LAST_UNLOCK_AT_EPOCH_MS = longPreferencesKey("last_unlock_at_epoch_ms")
        private val LOCAL_LOGIN_FAILED_ATTEMPTS = intPreferencesKey("local_login_failed_attempts")
        private val LOCAL_LOGIN_LOCKED_UNTIL_EPOCH_MS = longPreferencesKey("local_login_locked_until_epoch_ms")
        private val FIREBASE_URL = stringPreferencesKey("firebase_url")
        private val FIREBASE_PREFIX = stringPreferencesKey("firebase_prefix")
        private val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        private val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val settingsFlow: Flow<StoreSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val language = preferences[APP_LANGUAGE]?.let { value ->
                runCatching { AppLanguage.valueOf(value) }.getOrDefault(AppLanguage.HINDI)
            } ?: AppLanguage.HINDI

            StoreSettings(
                shopName = preferences[SHOP_NAME].orEmpty(),
                ownerName = preferences[OWNER_NAME].orEmpty(),
                ownerPhone = preferences[OWNER_PHONE].orEmpty(),
                staticPaytmQrImageUri = preferences[STATIC_PAYTM_QR_IMAGE_URI].orEmpty(),
                welcomeChantEnabled = preferences[WELCOME_CHANT_ENABLED] ?: true,
                firstLaunchCompleted = preferences[FIRST_LAUNCH_COMPLETED] ?: false,
                loggedInUid = preferences[LOGGED_IN_UID].orEmpty(),
                loggedInUsername = preferences[LOGGED_IN_USERNAME].orEmpty(),
                loggedInEmail = preferences[LOGGED_IN_EMAIL].orEmpty(),
                loggedInRole = preferences[LOGGED_IN_ROLE]?.trim()?.ifEmpty { "OWNER" } ?: "OWNER",
                identityProvider = IdentityProvider.fromStored(preferences[IDENTITY_PROVIDER])
                    ?: if (preferences[IS_USER_LOGGED_IN] == true) {
                        if (preferences[LOGGED_IN_UID].orEmpty().isNotBlank()) {
                            IdentityProvider.FIREBASE
                        } else {
                            IdentityProvider.LOCAL
                        }
                    } else {
                        null
                    },
                auditDeviceId = preferences[AUDIT_DEVICE_ID].orEmpty(),
                organizationId = preferences[ORGANIZATION_ID].orEmpty(),
                storeId = preferences[STORE_ID].orEmpty(),
                membershipId = preferences[MEMBERSHIP_ID].orEmpty(),
                deviceId = preferences[DEVICE_ID].orEmpty(),
                appInstallationId = preferences[APP_INSTALLATION_ID].orEmpty(),
                isUserLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false,
                appLockEnabled = preferences[APP_LOCK_ENABLED] ?: true,
                biometricEnabled = preferences[BIOMETRIC_ENABLED] ?: false,
                securityPin = preferences[SECURITY_PIN].orEmpty(),
                securityPinFormatVersion = (preferences[SECURITY_PIN_FORMAT_VERSION]
                    ?: SecurityUtils.LEGACY_CREDENTIAL_VERSION).coerceAtLeast(SecurityUtils.LEGACY_CREDENTIAL_VERSION),
                failedPinAttempts = (preferences[FAILED_PIN_ATTEMPTS] ?: 0).coerceAtLeast(0),
                pinLockedUntilEpochMs = (preferences[PIN_LOCKED_UNTIL_EPOCH_MS] ?: 0L).coerceAtLeast(0L),
                lastUnlockAtEpochMs = (preferences[LAST_UNLOCK_AT_EPOCH_MS] ?: 0L).coerceAtLeast(0L),
                localLoginFailedAttempts = (preferences[LOCAL_LOGIN_FAILED_ATTEMPTS] ?: 0).coerceAtLeast(0),
                localLoginLockedUntilEpochMs = (preferences[LOCAL_LOGIN_LOCKED_UNTIL_EPOCH_MS] ?: 0L).coerceAtLeast(0L),
                firebaseUrl = preferences[FIREBASE_URL].orEmpty(),
                firebasePrefix = preferences[FIREBASE_PREFIX]
                    ?.trim()
                    ?.ifEmpty { DEFAULT_FIREBASE_PREFIX }
                    ?: DEFAULT_FIREBASE_PREFIX,
                lastSyncTime = preferences[LAST_SYNC_TIME]
                    ?.trim()
                    ?.ifEmpty { "Never Synced" }
                    ?: "Never Synced",
                lastSyncStatus = preferences[LAST_SYNC_STATUS]
                    ?.trim()
                    ?.let { value -> runCatching { SyncRunStatus.valueOf(value) }.getOrDefault(SyncRunStatus.UNKNOWN) }
                    ?: SyncRunStatus.UNKNOWN,
                autoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: true,
                appLanguage = language
            )
        }

    suspend fun updateAppLanguage(language: AppLanguage) = context.dataStore.edit {
        it[APP_LANGUAGE] = language.name
    }

    suspend fun updateSecurityPin(pin: String) = context.dataStore.edit {
        val normalized = pin.trim()
        when {
            normalized.isEmpty() -> {
                // Keep the legacy marker explicitly so a future release can
                // remove the default-PIN fallback without ambiguity.
                it.remove(SECURITY_PIN)
                it[SECURITY_PIN_FORMAT_VERSION] = SecurityUtils.LEGACY_CREDENTIAL_VERSION
            }
            SecurityUtils.isVersionedCredential(
                normalized,
                SecurityUtils.CredentialScope.APP_LOCK
            ) -> {
                it[SECURITY_PIN] = normalized
                it[SECURITY_PIN_FORMAT_VERSION] = SecurityUtils.CURRENT_CREDENTIAL_VERSION
            }
            SecurityUtils.isSha256Hash(normalized) -> {
                // Compatibility only: existing SHA-256 values migrate on the
                // next successful unlock, never on a new PIN entry.
                it[SECURITY_PIN] = normalized.lowercase()
                it[SECURITY_PIN_FORMAT_VERSION] = SecurityUtils.LEGACY_CREDENTIAL_VERSION
            }
            else -> {
                require(SecurityUtils.isAcceptableNewPin(normalized)) {
                    "App-lock PIN does not satisfy the local security policy"
                }
                it[SECURITY_PIN] = SecurityUtils.createCredential(
                    normalized,
                    SecurityUtils.CredentialScope.APP_LOCK
                )
                it[SECURITY_PIN_FORMAT_VERSION] = SecurityUtils.CURRENT_CREDENTIAL_VERSION
            }
        }
        it[FAILED_PIN_ATTEMPTS] = 0
        it[PIN_LOCKED_UNTIL_EPOCH_MS] = 0L
        it[LAST_UNLOCK_AT_EPOCH_MS] = 0L
    }

    suspend fun updateAppLockState(state: AppLockState) = context.dataStore.edit {
        it[FAILED_PIN_ATTEMPTS] = state.failedAttempts.coerceAtLeast(0)
        it[PIN_LOCKED_UNTIL_EPOCH_MS] = state.lockedUntilEpochMs.coerceAtLeast(0L)
        it[LAST_UNLOCK_AT_EPOCH_MS] = state.lastUnlockAtEpochMs.coerceAtLeast(0L)
    }

    suspend fun evaluateAppLockPin(enteredPin: String, nowEpochMs: Long): PinUnlockResult {
        var result: PinUnlockResult? = null
        context.dataStore.edit { preferences ->
            val currentState = AppLockState(
                failedAttempts = (preferences[FAILED_PIN_ATTEMPTS] ?: 0).coerceAtLeast(0),
                lockedUntilEpochMs = (preferences[PIN_LOCKED_UNTIL_EPOCH_MS] ?: 0L).coerceAtLeast(0L),
                lastUnlockAtEpochMs = (preferences[LAST_UNLOCK_AT_EPOCH_MS] ?: 0L).coerceAtLeast(0L)
            )
            val storedCredential = preferences[SECURITY_PIN].orEmpty()
            val formatVersion = (preferences[SECURITY_PIN_FORMAT_VERSION]
                ?: SecurityUtils.LEGACY_CREDENTIAL_VERSION).coerceAtLeast(SecurityUtils.LEGACY_CREDENTIAL_VERSION)
            val verification = SecurityUtils.verifyCredential(
                secret = enteredPin,
                storedCredential = storedCredential,
                scope = SecurityUtils.CredentialScope.APP_LOCK,
                allowDefaultPinFallback = formatVersion < SecurityUtils.CURRENT_CREDENTIAL_VERSION
            )
            val (nextResult, nextState) = AppLockPolicy.verifyPin(
                enteredPin = enteredPin,
                storedHash = storedCredential,
                state = currentState,
                nowEpochMs = nowEpochMs,
                allowDefaultPinFallback = formatVersion < SecurityUtils.CURRENT_CREDENTIAL_VERSION,
                credentialMatched = verification.matched
            )
            result = nextResult
            preferences[FAILED_PIN_ATTEMPTS] = nextState.failedAttempts
            preferences[PIN_LOCKED_UNTIL_EPOCH_MS] = nextState.lockedUntilEpochMs
            preferences[LAST_UNLOCK_AT_EPOCH_MS] = nextState.lastUnlockAtEpochMs

            if (nextResult == PinUnlockResult.Success && verification.needsRehash) {
                preferences[SECURITY_PIN] = SecurityUtils.createCredential(
                    enteredPin,
                    SecurityUtils.CredentialScope.APP_LOCK
                )
                preferences[SECURITY_PIN_FORMAT_VERSION] = SecurityUtils.CURRENT_CREDENTIAL_VERSION
            }
        }
        return result ?: error("App-lock evaluation did not produce a result")
    }

    suspend fun evaluateLocalLogin(
        credentialMatched: Boolean,
        nowEpochMs: Long
    ): LocalLoginResult {
        var result: LocalLoginResult? = null
        context.dataStore.edit { preferences ->
            val (nextResult, nextState) = LocalLoginPolicy.evaluate(
                credentialMatched = credentialMatched,
                failedAttempts = (preferences[LOCAL_LOGIN_FAILED_ATTEMPTS] ?: 0).coerceAtLeast(0),
                lockedUntilEpochMs = (preferences[LOCAL_LOGIN_LOCKED_UNTIL_EPOCH_MS] ?: 0L).coerceAtLeast(0L),
                nowEpochMs = nowEpochMs
            )
            result = nextResult
            preferences[LOCAL_LOGIN_FAILED_ATTEMPTS] = nextState.first
            preferences[LOCAL_LOGIN_LOCKED_UNTIL_EPOCH_MS] = nextState.second
        }
        return result ?: error("Local-login evaluation did not produce a result")
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) = context.dataStore.edit {
        it[APP_LOCK_ENABLED] = enabled
    }

    suspend fun updateBiometricEnabled(enabled: Boolean) = context.dataStore.edit {
        it[BIOMETRIC_ENABLED] = enabled
    }

    suspend fun updateShopName(name: String) = context.dataStore.edit {
        it[SHOP_NAME] = name.trim()
    }

    suspend fun updateOwnerName(name: String) = context.dataStore.edit {
        it[OWNER_NAME] = name.trim()
    }

    suspend fun updateOwnerPhone(phone: String) = context.dataStore.edit {
        it[OWNER_PHONE] = phone.trim()
    }

    suspend fun updateStaticPaytmQrImageUri(uri: String) = context.dataStore.edit {
        it[STATIC_PAYTM_QR_IMAGE_URI] = uri.trim()
    }

    suspend fun updateWelcomeChantEnabled(enabled: Boolean) = context.dataStore.edit {
        it[WELCOME_CHANT_ENABLED] = enabled
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) = context.dataStore.edit {
        it[FIRST_LAUNCH_COMPLETED] = completed
    }

    suspend fun updateFirebaseConfig(url: String, prefix: String) = context.dataStore.edit {
        it[FIREBASE_URL] = url.trim()
        it[FIREBASE_PREFIX] = prefix.trim().ifEmpty { DEFAULT_FIREBASE_PREFIX }
    }

    suspend fun updateLastSyncTime(timeStr: String) = context.dataStore.edit {
        it[LAST_SYNC_TIME] = timeStr.trim().ifEmpty { "Never Synced" }
    }

    suspend fun updateLastSyncStatus(status: SyncRunStatus) = context.dataStore.edit {
        it[LAST_SYNC_STATUS] = status.name
    }

    suspend fun updateAutoSyncEnabled(enabled: Boolean) = context.dataStore.edit {
        it[AUTO_SYNC_ENABLED] = enabled
    }

    suspend fun saveSession(session: IdentitySession) {
        val normalized = session.normalized()
        require(normalized.isUsable()) { "Cannot persist an identity without a stable uid" }
        getOrCreateTenantDeviceContext(normalized)
        context.dataStore.edit {
            it[LOGGED_IN_UID] = normalized.uid
            it[LOGGED_IN_USERNAME] = normalized.username
            it[LOGGED_IN_EMAIL] = normalized.email
            it[LOGGED_IN_ROLE] = normalized.role
            it[IDENTITY_PROVIDER] = normalized.provider.name
            it[IS_USER_LOGGED_IN] = true
        }
    }

    suspend fun saveSession(
        uid: String,
        username: String,
        email: String,
        role: String = "OWNER",
        provider: IdentityProvider = IdentityProvider.LOCAL
    ) = saveSession(
        IdentitySession(
            provider = provider,
            uid = uid,
            username = username,
            email = email,
            role = role
        )
    )

    suspend fun getOrCreateAuditDeviceId(): String {
        var resolved = ""
        context.dataStore.edit {
            resolved = it[AUDIT_DEVICE_ID]?.trim().orEmpty()
            if (resolved.isEmpty()) {
                resolved = UUID.randomUUID().toString()
                it[AUDIT_DEVICE_ID] = resolved
            }
        }
        return resolved
    }

    suspend fun getOrCreateTenantDeviceContext(session: IdentitySession): TenantDeviceContext {
        val normalizedSession = session.normalized()
        require(normalizedSession.isUsable()) { "A usable identity is required for tenant mapping" }
        var resolved: TenantDeviceContext? = null
        context.dataStore.edit { preferences ->
            val storedValues = listOf(
                preferences[ORGANIZATION_ID].orEmpty().trim(),
                preferences[STORE_ID].orEmpty().trim(),
                preferences[MEMBERSHIP_ID].orEmpty().trim(),
                preferences[DEVICE_ID].orEmpty().trim(),
                preferences[APP_INSTALLATION_ID].orEmpty().trim()
            )
            val hasAnyStoredContext = storedValues.any(String::isNotBlank)
            val hasCompleteStoredContext = storedValues.all(String::isNotBlank)
            require(!hasAnyStoredContext || hasCompleteStoredContext) {
                "Tenant context is incomplete and must be repaired before use"
            }

            if (hasCompleteStoredContext) {
                val stored = TenantDeviceContext(
                    organizationId = storedValues[0],
                    storeId = storedValues[1],
                    membershipId = storedValues[2],
                    deviceId = storedValues[3],
                    appInstallationId = storedValues[4]
                )
                val expectedLegacy = TenantDeviceContext.fromLegacySession(
                    session = normalizedSession,
                    deviceId = stored.deviceId,
                    appInstallationId = stored.appInstallationId
                )
                require(
                    !stored.organizationId.startsWith(TenantDeviceContext.LEGACY_ORGANIZATION_PREFIX) ||
                        stored.storeId == expectedLegacy.storeId
                ) { "Tenant/store scope mismatch" }
                resolved = stored
            } else {
                val deviceId = preferences[AUDIT_DEVICE_ID].orEmpty().trim().ifEmpty {
                    UUID.randomUUID().toString()
                }
                val appInstallationId = preferences[APP_INSTALLATION_ID].orEmpty().trim().ifEmpty {
                    TenantDeviceContext.newAppInstallationId()
                }
                val mapped = TenantDeviceContext.fromLegacySession(
                    session = normalizedSession,
                    deviceId = deviceId,
                    appInstallationId = appInstallationId
                )
                preferences[AUDIT_DEVICE_ID] = deviceId
                preferences[ORGANIZATION_ID] = mapped.organizationId
                preferences[STORE_ID] = mapped.storeId
                preferences[MEMBERSHIP_ID] = mapped.membershipId
                preferences[DEVICE_ID] = mapped.deviceId
                preferences[APP_INSTALLATION_ID] = mapped.appInstallationId
                resolved = mapped
            }
        }
        return resolved ?: error("Tenant context persistence did not produce a result")
    }

    suspend fun clearSession() = context.dataStore.edit {
        it[LOGGED_IN_UID] = ""
        it[LOGGED_IN_USERNAME] = ""
        it[LOGGED_IN_EMAIL] = ""
        it[LOGGED_IN_ROLE] = "OWNER"
        it.remove(IDENTITY_PROVIDER)
        it[FAILED_PIN_ATTEMPTS] = 0
        it[PIN_LOCKED_UNTIL_EPOCH_MS] = 0L
        it[LAST_UNLOCK_AT_EPOCH_MS] = 0L
        it[IS_USER_LOGGED_IN] = false
    }
}
