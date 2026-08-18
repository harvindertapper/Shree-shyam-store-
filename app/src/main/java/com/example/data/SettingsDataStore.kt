package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.utils.AppLanguage
import com.example.utils.SecurityUtils
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
    val isUserLoggedIn: Boolean = false,
    val appLockEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val securityPin: String = SecurityUtils.hashPin(SecurityUtils.DEFAULT_PIN),
    val firebaseUrl: String = "",
    val firebasePrefix: String = DEFAULT_FIREBASE_PREFIX,
    val lastSyncTime: String = "Never Synced",
    val autoSyncEnabled: Boolean = false,
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
        private val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val SECURITY_PIN = stringPreferencesKey("security_pin")
        private val FIREBASE_URL = stringPreferencesKey("firebase_url")
        private val FIREBASE_PREFIX = stringPreferencesKey("firebase_prefix")
        private val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
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
                isUserLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false,
                appLockEnabled = preferences[APP_LOCK_ENABLED] ?: true,
                biometricEnabled = preferences[BIOMETRIC_ENABLED] ?: false,
                securityPin = preferences[SECURITY_PIN] ?: SecurityUtils.hashPin(SecurityUtils.DEFAULT_PIN),
                firebaseUrl = preferences[FIREBASE_URL].orEmpty(),
                firebasePrefix = preferences[FIREBASE_PREFIX]
                    ?.trim()
                    ?.ifEmpty { DEFAULT_FIREBASE_PREFIX }
                    ?: DEFAULT_FIREBASE_PREFIX,
                lastSyncTime = preferences[LAST_SYNC_TIME]
                    ?.trim()
                    ?.ifEmpty { "Never Synced" }
                    ?: "Never Synced",
                autoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: false,
                appLanguage = language
            )
        }

    suspend fun updateAppLanguage(language: AppLanguage) = context.dataStore.edit {
        it[APP_LANGUAGE] = language.name
    }

    suspend fun updateSecurityPin(pin: String) = context.dataStore.edit {
        val normalized = pin.trim()
        if (normalized.isEmpty()) {
            it.remove(SECURITY_PIN)
        } else {
            it[SECURITY_PIN] = if (SecurityUtils.isSha256Hash(normalized)) {
                normalized.lowercase()
            } else {
                SecurityUtils.hashPin(normalized)
            }
        }
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

    suspend fun updateAutoSyncEnabled(enabled: Boolean) = context.dataStore.edit {
        it[AUTO_SYNC_ENABLED] = enabled
    }

    suspend fun saveSession(session: IdentitySession) = context.dataStore.edit {
        val normalized = session.normalized()
        require(normalized.isUsable()) { "Cannot persist an identity without a stable uid" }
        it[LOGGED_IN_UID] = normalized.uid
        it[LOGGED_IN_USERNAME] = normalized.username
        it[LOGGED_IN_EMAIL] = normalized.email
        it[LOGGED_IN_ROLE] = normalized.role
        it[IDENTITY_PROVIDER] = normalized.provider.name
        it[IS_USER_LOGGED_IN] = true
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

    suspend fun clearSession() = context.dataStore.edit {
        it[LOGGED_IN_UID] = ""
        it[LOGGED_IN_USERNAME] = ""
        it[LOGGED_IN_EMAIL] = ""
        it[LOGGED_IN_ROLE] = "OWNER"
        it.remove(IDENTITY_PROVIDER)
        it[IS_USER_LOGGED_IN] = false
    }
}
