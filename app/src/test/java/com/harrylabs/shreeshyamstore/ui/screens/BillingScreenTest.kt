package com.harrylabs.shreeshyamstore.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.harrylabs.shreeshyamstore.data.AppDatabase
import com.harrylabs.shreeshyamstore.data.SettingsDataStore
import com.harrylabs.shreeshyamstore.data.ShopRepository
import com.harrylabs.shreeshyamstore.ui.theme.MyApplicationTheme
import com.harrylabs.shreeshyamstore.viewmodel.FakeFirebaseOwnerRepository
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class BillingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun billingScreenShowsOneTapCashSaleAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { database = it }
        val repository = ShopRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            productDao = db.productDao(),
            saleDao = db.saleDao(),
            customerDao = db.customerDao(),
            udhaarDao = db.udhaarDao(),
            stockAdjustmentDao = db.stockAdjustmentDao()
        )
        val viewModel = ShopViewModel(
            repository = repository,
            settingsDataStore = SettingsDataStore(context),
            firebaseOwnerRepository = FakeFirebaseOwnerRepository(),
            database = db
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                BillingScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onAllNodesWithTag("save_cash_sale_button").assertCountEquals(1)
    }
}
