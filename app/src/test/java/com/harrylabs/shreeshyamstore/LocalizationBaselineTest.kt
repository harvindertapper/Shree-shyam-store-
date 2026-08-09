package com.harrylabs.shreeshyamstore

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.harrylabs.shreeshyamstore.data.SettingsDataStore
import com.harrylabs.shreeshyamstore.data.dataStore
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalizationBaselineTest {

    @Before
    fun resetPreferences() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun defaultResourcesUseEnglishAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Shree Shyam Store", context.getString(R.string.app_name))
    }

    @Test
    fun hindiResourcesProvideHindiAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale("hi"))
        }
        val hindiContext = context.createConfigurationContext(config)

        assertEquals("श्री श्याम स्टोर", hindiContext.getString(R.string.app_name))
    }

    @Test
    fun settingsDataStoreDefaultsToEnglishLanguage() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        assertEquals("en", store.settingsFlow.first().selectedLanguage)
    }

    @Test
    fun settingsDataStorePersistsSelectedLanguage() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.updateSelectedLanguage("hi")

        assertEquals("hi", store.settingsFlow.first().selectedLanguage)
    }
}
