package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.SettingsDataStore
import com.example.data.ShopRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.ShopViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Core Data Initializations (Room + DataStore)
        val database = AppDatabase.getDatabase(applicationContext)
        val repo = ShopRepository(
            categoryDao = database.categoryDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao(),
            userDao = database.userDao(),
            database = database
        )
        val settingsStore = SettingsDataStore(applicationContext)

        // Initialize WorkManager Sync Engine
        com.example.utils.SyncManager.schedulePeriodicSync(applicationContext)
        com.example.utils.SyncManager.scheduleInstantSync(applicationContext)

        // 2. ViewModel instantiation using custom provider factory
        val viewModel = ViewModelProvider(
            this,
            ShopViewModelFactory(repo, settingsStore, applicationContext)
        )[ShopViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Do not show bottom nav drawer in welcome, login, register, or onboarding setup flows
                        if (currentScreen !is Screen.Welcome && currentScreen !is Screen.Login && currentScreen !is Screen.Register && currentScreen !is Screen.Setup) {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Home,
                                    onClick = { viewModel.navigateTo(Screen.Home) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_home")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Billing || currentScreen is Screen.Payment || currentScreen is Screen.BillSuccess,
                                    onClick = { viewModel.navigateTo(Screen.Billing) },
                                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Billing") },
                                    label = { Text("Billing", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_billing")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Products || currentScreen is Screen.AddEditProduct || currentScreen is Screen.OpeningStock || currentScreen is Screen.StockAdjustment,
                                    onClick = { viewModel.navigateTo(Screen.Products) },
                                    icon = { Icon(Icons.Default.Store, contentDescription = "Products") },
                                    label = { Text("Products", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_products")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Udhaar || currentScreen is Screen.CustomerDetail,
                                    onClick = { viewModel.navigateTo(Screen.Udhaar) },
                                    icon = { Icon(Icons.Default.ImportContacts, contentDescription = "Udhaar") },
                                    label = { Text("Udhaar", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_udhaar")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Reports,
                                    onClick = { viewModel.navigateTo(Screen.Reports) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Reports") },
                                    label = { Text("Reports", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_reports")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Settings,
                                    onClick = { viewModel.navigateTo(Screen.Settings) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0) // don't double consume, child scaffolds handle bars themselves
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Welcome -> WelcomeScreen(viewModel)
                            is Screen.Login -> LoginScreen(viewModel)
                            is Screen.Register -> RegisterScreen(viewModel)
                            is Screen.Setup -> FirstLaunchSetupScreen(viewModel)
                            is Screen.Home -> HomeScreen(viewModel)
                            is Screen.Billing -> BillingScreen(viewModel)
                            is Screen.Payment -> PaymentScreen(viewModel, screen.invoiceTotal)
                            is Screen.BillSuccess -> BillSuccessScreen(viewModel)
                            is Screen.Products -> ProductsScreen(viewModel)
                            is Screen.AddEditProduct -> AddEditProductScreen(viewModel, screen.productId)
                            is Screen.OpeningStock -> OpeningStockScreen(viewModel)
                            is Screen.StockAdjustment -> StockAdjustmentScreen(viewModel, screen.productId)
                            is Screen.Udhaar -> UdhaarScreen(viewModel)
                            is Screen.CustomerDetail -> CustomerDetailScreen(viewModel, screen.customerId)
                            is Screen.Reports -> ReportsScreen(viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
