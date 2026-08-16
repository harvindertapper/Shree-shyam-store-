package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.SettingsDataStore
import com.example.data.ShopRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.AuthManager
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.ShopViewModelFactory

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: ShopViewModel
    private var wasInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Throwable) {
            // Graceful offline fallback if Google Services config is absent
        }

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

        // Initialize WorkManager Sync Engine and Network Callback
        com.example.utils.SyncManager.registerNetworkCallback(applicationContext)
        com.example.utils.SyncManager.schedulePeriodicSync(applicationContext)
        com.example.utils.SyncManager.scheduleInstantSync(applicationContext)

        // 2. ViewModel instantiation using custom provider factory
        viewModel = ViewModelProvider(
            this,
            ShopViewModelFactory(repo, settingsStore, applicationContext)
        )[ShopViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val settings by viewModel.storeSettings.collectAsState()
                val strings = remember(settings.appLanguage) { com.example.utils.LocaleHelper.getStrings(settings.appLanguage) }

                // Initial Startup Router: Direct navigation based on Auth and App Lock state
                LaunchedEffect(settings.isUserLoggedIn, settings.firstLaunchCompleted, settings.appLockEnabled) {
                    val firebaseUser = AuthManager.currentUser
                    val isAuthenticated = firebaseUser != null || settings.isUserLoggedIn

                    if (!isAuthenticated) {
                        // User is logged out -> Show Google Sign-In & Welcome screen
                        viewModel.navigateTo(Screen.Welcome)
                    } else if (!settings.firstLaunchCompleted) {
                        // User is logged in but hasn't completed shop onboarding
                        viewModel.navigateTo(Screen.Setup)
                    } else if (settings.appLockEnabled && currentScreen is Screen.Welcome) {
                        // User is logged in & App Lock is enabled -> Direct to 4-Digit PIN screen
                        viewModel.navigateTo(Screen.Login)
                    } else if (!settings.appLockEnabled && currentScreen is Screen.Welcome) {
                        // User is logged in & App Lock is disabled -> Direct to HomeScreen
                        viewModel.navigateTo(Screen.Home)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Do not show bottom nav drawer in welcome, login, or onboarding setup flows
                        if (currentScreen !is Screen.Welcome && currentScreen !is Screen.Login && currentScreen !is Screen.Setup) {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Home,
                                    onClick = { viewModel.navigateTo(Screen.Home) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text(strings.navHome, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_home")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Billing || currentScreen is Screen.Payment || currentScreen is Screen.BillSuccess,
                                    onClick = { viewModel.navigateTo(Screen.Billing) },
                                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Billing") },
                                    label = { Text(strings.navBilling, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_billing")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Products || currentScreen is Screen.AddEditProduct || currentScreen is Screen.OpeningStock || currentScreen is Screen.StockAdjustment,
                                    onClick = { viewModel.navigateTo(Screen.Products) },
                                    icon = { Icon(Icons.Default.Store, contentDescription = "Products") },
                                    label = { Text(strings.navProducts, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_products")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Udhaar || currentScreen is Screen.CustomerDetail,
                                    onClick = { viewModel.navigateTo(Screen.Udhaar) },
                                    icon = { Icon(Icons.Default.ImportContacts, contentDescription = "Udhaar") },
                                    label = { Text(strings.navUdhaar, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_udhaar")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Reports,
                                    onClick = { viewModel.navigateTo(Screen.Reports) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Reports") },
                                    label = { Text(strings.navReports, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_reports")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Settings,
                                    onClick = { viewModel.navigateTo(Screen.Settings) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text(strings.navSettings, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) +
                                        slideInVertically(animationSpec = tween(220), initialOffsetY = { 30 }) togetherWith
                                        fadeOut(animationSpec = tween(150))
                            },
                            label = "ScreenNavigationTransition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Welcome -> WelcomeScreen(viewModel)
                                is Screen.Login -> LoginScreen(viewModel)
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
                                else -> HomeScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        wasInBackground = true
    }

    override fun onResume() {
        super.onResume()
        if (wasInBackground) {
            wasInBackground = false
            if (::viewModel.isInitialized) {
                val settings = viewModel.storeSettings.value
                val currentScreen = viewModel.currentScreen.value
                if (settings.firstLaunchCompleted && settings.appLockEnabled &&
                    currentScreen !is Screen.Welcome && currentScreen !is Screen.Setup && currentScreen !is Screen.Login) {
                    viewModel.navigateTo(Screen.Login)
                }
            }
        }
    }
}
