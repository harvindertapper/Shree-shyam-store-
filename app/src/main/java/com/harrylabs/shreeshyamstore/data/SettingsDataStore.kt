package com.harrylabs.shreeshyamstore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "store_settings")

data class StoreSettings(
    val shopName: String,
    val ownerPhone: String,
    val staticPaytmQrImageUri: String,
    val welcomeChantEnabled: Boolean,
    val firstLaunchCompleted: Boolean,
    val loggedInUsername: String,
    val loggedInEmail: String,
    val isUserLoggedIn: Boolean,
    val selectedLanguage: String
)

class SettingsDataStore(private val context: Context) {
    companion object {
        private val SHOP_NAME = stringPreferencesKey("shop_name")
        private val OWNER_PHONE = stringPreferencesKey("owner_phone")
        private val STATIC_PAYTM_QR_IMAGE_URI = stringPreferencesKey("static_paytm_qr_image_uri")
        private val WELCOME_CHANT_ENABLED = booleanPreferencesKey("welcome_chant_enabled")
        private val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
        private val LOGGED_IN_USERNAME = stringPreferencesKey("logged_in_username")
        private val LOGGED_IN_EMAIL = stringPreferencesKey("logged_in_email")
        private val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    val settingsFlow: Flow<StoreSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            StoreSettings(
                shopName = preferences[SHOP_NAME] ?: "Shree Shyam General Store",
                ownerPhone = preferences[OWNER_PHONE] ?: "",
                staticPaytmQrImageUri = preferences[STATIC_PAYTM_QR_IMAGE_URI] ?: "",
                welcomeChantEnabled = preferences[WELCOME_CHANT_ENABLED] ?: true,
                firstLaunchCompleted = preferences[FIRST_LAUNCH_COMPLETED] ?: false,
                loggedInUsername = preferences[LOGGED_IN_USERNAME] ?: "",
                loggedInEmail = preferences[LOGGED_IN_EMAIL] ?: "",
                isUserLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false,
                selectedLanguage = preferences[SELECTED_LANGUAGE] ?: "en"
            )
        }

    suspend fun updateShopName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[SHOP_NAME] = name
        }
    }

    suspend fun updateOwnerPhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[OWNER_PHONE] = phone
        }
    }

    suspend fun updateStaticPaytmQrImageUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[STATIC_PAYTM_QR_IMAGE_URI] = uri
        }
    }

    suspend fun updateWelcomeChantEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WELCOME_CHANT_ENABLED] = enabled
        }
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_COMPLETED] = completed
        }
    }

    suspend fun saveSession(username: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_USERNAME] = username
            preferences[LOGGED_IN_EMAIL] = email
            preferences[IS_USER_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_USERNAME] = ""
            preferences[LOGGED_IN_EMAIL] = ""
            preferences[IS_USER_LOGGED_IN] = false
        }
    }

    suspend fun updateSelectedLanguage(languageCode: String) {
        val supportedLanguage = when (languageCode) {
            "hi" -> "hi"
            else -> "en"
        }
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = supportedLanguage
        }
    }
}
