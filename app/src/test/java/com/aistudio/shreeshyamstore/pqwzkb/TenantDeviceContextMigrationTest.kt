package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TenantDeviceContextMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databaseName = "tenant-context-migration-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v8Profile_receivesStableLegacyTenantAndInstallationContext() = runBlocking {
        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createV5Schema(raw)
        upgradeV5SchemaToV6(raw)
        upgradeV6SchemaToV7(raw)
        raw.execSQL(
            "INSERT INTO shop_profiles (uid, shopName, ownerName, ownerPhone, upiId, email, address, isSynced, updatedAt, isDeleted) " +
                "VALUES ('legacy-owner', 'Shree Shyam General Store', 'Owner', '9999999999', '', 'owner@example.com', '', 0, 100, 0)"
        )
        raw.execSQL("PRAGMA user_version = 7")
        raw.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11
            )
            .allowMainThreadQueries()
            .build()

        val profile = migrated.shopProfileDao().getByUid("legacy-owner")
        assertNotNull(profile)
        assertEquals("Shree Shyam General Store", profile!!.shopName)
        assertEquals("legacy-org:legacy-owner", profile.organizationId)
        assertEquals("legacy-store:legacy-owner", profile.storeId)
        assertEquals("legacy-membership:legacy-owner", profile.membershipId)
        assertEquals("legacy-device", profile.deviceId)
        assertEquals("legacy-installation:legacy-owner", profile.appInstallationId)

        migrated.close()
    }
}
