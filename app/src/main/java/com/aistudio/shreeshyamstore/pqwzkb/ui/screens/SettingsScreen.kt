package com.aistudio.shreeshyamstore.pqwzkb.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aistudio.shreeshyamstore.pqwzkb.BuildConfig
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppMutationStatusCard
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppOutlinedTextField
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppPrimaryButton
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.SettingsValidation
import com.aistudio.shreeshyamstore.pqwzkb.utils.SettingsValidationError
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncHealth
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncManager
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.Screen
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("") }
    var securityPinConfirm by remember { mutableStateOf("") }
    var welcomeChantEnabled by remember { mutableStateOf(true) }
    var qrUriString by remember { mutableStateOf("") }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var appLockEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var settingsNotice by remember { mutableStateOf<String?>(null) }
    var settingsNoticeIsError by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showDisableLockConfirm by remember { mutableStateOf(false) }

    val mutationStatus by viewModel.mutationStatus.collectAsState()
    val mutationInFlight by viewModel.mutationInFlight.collectAsState()
    val syncHealth by viewModel.syncHealthSnapshot.collectAsState()
    val biometricAvailable = remember(context) { viewModel.isBiometricAvailable() }

    LaunchedEffect(settings) {
        if (hasUnsavedChanges) return@LaunchedEffect
        shopName = settings.shopName
        ownerName = settings.ownerName
        ownerPhone = settings.ownerPhone
        welcomeChantEnabled = settings.welcomeChantEnabled
        qrUriString = settings.staticPaytmQrImageUri
        autoSyncEnabled = settings.autoSyncEnabled
        appLockEnabled = settings.appLockEnabled
        biometricEnabled = settings.biometricEnabled
    }

    LaunchedEffect(settings.lastSyncTime, settings.lastSyncStatus, settings.isUserLoggedIn) {
        viewModel.refreshSyncHealth()
    }

    // Modern Secure Gallery Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            qrUriString = uri.toString()
            hasUnsavedChanges = true
            settingsNotice = strings.settingsSaveHint
            settingsNoticeIsError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.settingsBack)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WarmCreamBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 0: Language Switcher Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("language_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SaffronLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, null, tint = SaffronDark, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = strings.languageSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronDark
                        )
                    }
                    Text(
                        text = strings.settingsLanguageHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMutedGray,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hindi Option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (settings.appLanguage == AppLanguage.HINDI) SaffronLight else SlateContainer,
                            border = BorderStroke(
                                1.5.dp,
                                if (settings.appLanguage == AppLanguage.HINDI) SaffronPrimary else BorderStrong
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setLanguage(AppLanguage.HINDI) }
                                .testTag("settings_lang_hindi")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                RadioButton(
                                    selected = settings.appLanguage == AppLanguage.HINDI,
                                    onClick = { viewModel.setLanguage(AppLanguage.HINDI) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SaffronPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.languageHindi,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (settings.appLanguage == AppLanguage.HINDI) SaffronDark else TextNearBlack
                                )
                            }
                        }

                        // English Option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (settings.appLanguage == AppLanguage.ENGLISH) SaffronLight else SlateContainer,
                            border = BorderStroke(
                                1.5.dp,
                                if (settings.appLanguage == AppLanguage.ENGLISH) SaffronPrimary else BorderStrong
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setLanguage(AppLanguage.ENGLISH) }
                                .testTag("settings_lang_english")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                RadioButton(
                                    selected = settings.appLanguage == AppLanguage.ENGLISH,
                                    onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SaffronPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.languageEnglish,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (settings.appLanguage == AppLanguage.ENGLISH) SaffronDark else TextNearBlack
                                )
                            }
                        }
                    }
                }
            }

            SettingsInfoCard(
                title = strings.settingsAppearanceSection,
                detail = strings.settingsAppearanceHint,
                testTag = "settings_appearance_summary_card"
            )

            SettingsSectionHeading(strings.settingsAccountSection)

            // Session Profile display card
            if (settings.isUserLoggedIn) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SaffronLight),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, SaffronDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("session_profile_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = strings.settingsUserAvatar,
                                    tint = SaffronDark,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.activeOwner,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SaffronDark
                                    )
                                    Text(
                                        text = settings.loggedInUsername,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextNearBlack
                                    )
                                    Text(
                                        text = settings.loggedInEmail,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMediumGray
                                    )
                                    Text(
                                        text = "${strings.settingsAccountProvider}: ${when (settings.identityProvider) {
                                            com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider.FIREBASE -> strings.settingsProviderFirebase
                                            com.aistudio.shreeshyamstore.pqwzkb.data.IdentityProvider.LOCAL -> strings.settingsProviderLocal
                                            null -> strings.settingsNotAssigned
                                        }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMediumGray
                                    )
                                    Text(
                                        text = "${strings.settingsStoreIdentity}: ${maskedIdentity(settings.storeId, strings.settingsNotAssigned)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMediumGray
                                    )
                                    Text(
                                        text = "${strings.settingsTenantIdentity}: ${maskedIdentity(settings.organizationId, strings.settingsNotAssigned)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMediumGray
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.logoutUser() },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .testTag("logout_button")
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = strings.logout, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.logout, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { viewModel.logoutUser() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_account_button")
                        ) {
                            Text(
                                text = strings.switchAccount,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark
                            )
                        }
                    }
                }
            }

            if (!settings.isUserLoggedIn) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("session_sign_in_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = ErrorRed, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsSignedOut,
                                fontWeight = FontWeight.Black,
                                color = ErrorRed
                            )
                            Text(
                                text = strings.settingsCloudSignInRequired,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack,
                                lineHeight = 17.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Welcome) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.testTag("settings_sign_in_button")
                        ) {
                            Text(strings.settingsSignIn)
                        }
                    }
                }
            }

            SettingsSectionHeading(strings.shopProfile)

            // Card 1: Shop details info
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.shopProfile,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark
                    )
                    Text(
                        text = strings.settingsShopProfileHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMutedGray,
                        lineHeight = 17.sp
                    )

                    AppOutlinedTextField(
                        value = shopName,
                        onValueChange = {
                            shopName = it
                            hasUnsavedChanges = true
                        },
                        label = strings.shopNameLabel,
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Business, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_shop_name_field")
                    )

                    AppOutlinedTextField(
                        value = ownerName,
                        onValueChange = {
                            ownerName = it
                            hasUnsavedChanges = true
                        },
                        label = strings.ownerNameLabel,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_name_field")
                    )

                    AppOutlinedTextField(
                        value = ownerPhone,
                        onValueChange = {
                            ownerPhone = it
                            hasUnsavedChanges = true
                        },
                        label = strings.ownerPhoneLabel,
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Phone, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        placeholder = strings.settingsOwnerPhonePlaceholder,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_phone_field")
                    )
                }
            }

            SettingsSectionHeading(strings.securityPinSection)

            // Card: Owner 4-Digit Security PIN
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_pin_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SaffronLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pin, null, tint = SaffronDark, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = strings.securityPinSection,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            Text(
                                text = strings.settingsSecurityHint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedGray,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Text(
                        text = strings.settingsSetPinHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMutedGray,
                        lineHeight = 17.sp
                    )

                    AppOutlinedTextField(
                        value = securityPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                securityPin = it
                                hasUnsavedChanges = true
                            }
                        },
                        label = strings.securityPinLabel,
                        placeholder = strings.settingsPinPlaceholder,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_security_pin_field")
                    )

                    AppOutlinedTextField(
                        value = securityPinConfirm,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                securityPinConfirm = it
                                hasUnsavedChanges = true
                                settingsNotice = null
                                settingsNoticeIsError = false
                            }
                        },
                        label = strings.settingsPinConfirmLabel,
                        placeholder = strings.settingsPinPlaceholder,
                        enabled = securityPin.isNotEmpty(),
                        isError = securityPinConfirm.isNotEmpty() && securityPin != securityPinConfirm,
                        supportingText = if (securityPinConfirm.isNotEmpty() && securityPin != securityPinConfirm) {
                            { Text(strings.settingsPinMismatch) }
                        } else {
                            null
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_security_pin_confirm_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.enableAppLock,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                            Text(
                                text = if (appLockEnabled) strings.settingsAppLockOnHint else strings.settingsAppLockOffHint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMutedGray,
                                lineHeight = 17.sp
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    appLockEnabled = true
                                    hasUnsavedChanges = true
                                } else {
                                    showDisableLockConfirm = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SaffronPrimary,
                                checkedTrackColor = SaffronLight
                            ),
                            modifier = Modifier.testTag("settings_app_lock_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.biometricUnlock,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                            Text(
                                text = if (biometricAvailable) strings.settingsBiometricAvailable else strings.settingsBiometricUnavailable,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMutedGray,
                                lineHeight = 17.sp
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it && biometricAvailable
                                hasUnsavedChanges = true
                            },
                            enabled = biometricAvailable && appLockEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SaffronPrimary,
                                checkedTrackColor = SaffronLight
                            ),
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }
                }
            }

            // Card 2: Babaji welcome sound switch
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.welcomeChant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = strings.settingsWelcomeChantHint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMutedGray,
                        )
                    }
                    Switch(
                        checked = welcomeChantEnabled,
                        onCheckedChange = {
                            welcomeChantEnabled = it
                            hasUnsavedChanges = true
                        },
                        modifier = Modifier.testTag("settings_welcome_sound_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SaffronPrimary,
                            checkedTrackColor = SaffronLight,
                            uncheckedThumbColor = BorderStrong,
                            uncheckedTrackColor = Color.White
                        )
                    )
                }
            }

            SettingsSectionHeading(strings.settingsBillingSection)

            // Card 3: Static QR upload
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.paytmQrCode,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Text(
                        text = strings.scanQrInstruction,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMutedGray,
                        lineHeight = 18.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    if (qrUriString.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .border(2.dp, SaffronPrimary, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = qrUriString,
                                contentDescription = strings.settingsQrPreview,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronLight, contentColor = SaffronDark), border = BorderStroke(1.2.dp, SaffronDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.settingsChangeQr, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Empty QR state
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .border(1.5.dp, BorderStrong, RoundedCornerShape(12.dp))
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(56.dp), tint = BorderStrong)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(strings.settingsNoQrSelected, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                            }
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .testTag("upload_qr_button")
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.uploadQr, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsInfoCard(
                title = strings.settingsManualUpiSettlement,
                detail = strings.settingsManualUpiSettlementHint,
                testTag = "settings_upi_policy_card"
            )

            SettingsSectionHeading(strings.settingsInventorySection)
            SettingsInfoCard(
                title = strings.settingsInventorySection,
                detail = strings.settingsInventoryHint,
                testTag = "settings_inventory_summary_card"
            )

            SettingsSectionHeading(strings.settingsCustomersSection)
            SettingsInfoCard(
                title = strings.settingsCustomersSection,
                detail = strings.settingsCustomersHint,
                testTag = "settings_customers_summary_card"
            )

            // Automatic sync and backup policy
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("automatic_sync_policy_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(SuccessGreenLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sync, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsAutomaticSync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronDark
                        )
                        Text(
                            text = strings.settingsAutomaticSyncHint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedGray
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = {
                            autoSyncEnabled = it
                            hasUnsavedChanges = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen
                        ),
                        modifier = Modifier.testTag("automatic_sync_switch")
                    )
                }
            }

            SettingsSectionHeading(strings.settingsSyncSection)

            // Card 4: Firebase Cloud Sync
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("firebase_sync_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            val backupTitle = strings.settingsCloudBackupTitle
                            val backupSubtitle = strings.settingsCloudBackupHint
                            Text(
                                text = backupTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            Text(
                                text = backupSubtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedGray
                            )
                        }
                    }

                    HorizontalDivider(color = BorderStrong)

                    val syncNote = strings.settingsSyncNote
                    Text(
                        text = syncNote,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark,
                        lineHeight = 18.sp
                    )

                    AppMutationStatusCard(
                        status = mutationStatus,
                        strings = strings,
                        onRetry = if (mutationStatus.canRetry) viewModel::retryLastMutation else null,
                        onDismiss = if (!mutationInFlight) viewModel::clearMutationStatus else null
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val lastSyncLabel = strings.settingsLastSyncLabel
                            Text(
                                text = lastSyncLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMediumGray
                            )
                            Text(
                                text = settings.lastSyncTime,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            val syncStatusText = when (settings.lastSyncStatus) {
                                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus.SUCCESS -> strings.settingsSyncStatusSuccess
                                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus.NO_CHANGES -> strings.settingsSyncStatusNoChanges
                                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus.FAILED -> strings.settingsSyncStatusFailed
                                com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus.UNKNOWN -> strings.settingsSyncStatusUnavailable
                            }
                            Text(
                                text = syncStatusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.lastSyncStatus == com.aistudio.shreeshyamstore.pqwzkb.utils.SyncRunStatus.FAILED) {
                                    Color(0xFFB3261E)
                                } else {
                                    TextMediumGray
                                }
                            )
                        }
                    }

                    Text(
                        text = strings.settingsSyncHealth,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark
                    )
                    val syncHealthLabel = when (syncHealth.health) {
                        SyncHealth.HEALTHY -> strings.settingsHealthHealthy
                        SyncHealth.NEVER_SYNCED -> strings.settingsHealthNever
                        SyncHealth.PENDING -> strings.settingsHealthPending
                        SyncHealth.RETRYING -> strings.settingsHealthRetrying
                        SyncHealth.BLOCKED -> strings.settingsHealthBlocked
                    }
                    val syncHealthColor = when (syncHealth.health) {
                        SyncHealth.HEALTHY -> SuccessGreen
                        SyncHealth.NEVER_SYNCED -> TextMediumGray
                        SyncHealth.PENDING -> SaffronDark
                        SyncHealth.RETRYING, SyncHealth.BLOCKED -> ErrorRed
                    }
                    Surface(
                        color = syncHealthColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, syncHealthColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().testTag("settings_sync_health_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = syncHealthLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = syncHealthColor
                            )
                            Text(
                                text = "${strings.settingsLastSuccess}: ${settings.lastSyncTime}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                            Text(
                                text = "${strings.settingsLastAttempt}: ${strings.settingsLastAttemptUnavailable}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMutedGray,
                                lineHeight = 16.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsHealthMetric(
                                    label = strings.settingsPending,
                                    value = syncHealth.pendingCount + syncHealth.inFlightCount,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsHealthMetric(
                                    label = strings.settingsRetryable,
                                    value = syncHealth.retryableCount,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsHealthMetric(
                                    label = strings.settingsDeadLetter,
                                    value = syncHealth.deadLetterCount,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsHealthMetric(
                                    label = strings.settingsConflicts,
                                    value = syncHealth.conflictCount,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Action Button: Manual Sync Now
                    Button(
                        onClick = { SyncManager.triggerImmediateSync(context) },
                        enabled = !mutationInFlight,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("sync_now_button")
                    ) {
                        Icon(Icons.Default.Sync, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsManualSync,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.syncAllToCloud { _, _ -> }
                            },
                            enabled = !mutationInFlight,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_backup_now_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.settingsBackupNow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showRestoreConfirmDialog = true
                            },
                            enabled = !mutationInFlight,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_restore_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.settingsRestore, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsSectionHeading(strings.settingsDataPrivacySection)
            SettingsInfoCard(
                title = strings.settingsDataPrivacySection,
                detail = strings.settingsDataPrivacyHint,
                testTag = "settings_data_privacy_card"
            )

            SettingsSectionHeading(strings.settingsSupportSection)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BorderStrong),
                modifier = Modifier.fillMaxWidth().testTag("settings_support_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.settingsVersion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark
                    )
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNearBlack
                    )
                    Text(
                        text = strings.settingsSupportHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMutedGray,
                        lineHeight = 17.sp
                    )
                }
            }

            // CONFIRMATION DIALOG FOR DATA RESTORATIVE OVERWRITE
            if (showRestoreConfirmDialog) {
                val dialogTitle = strings.settingsRestoreWarningTitle
                val dialogText = strings.settingsRestoreWarningMessage
                val confirmBtn = strings.settingsRestoreConfirmAction

                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = false },
                    title = {
                        Text(
                            text = dialogTitle,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )
                    },
                    text = {
                        Text(
                            text = dialogText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNearBlack
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRestoreConfirmDialog = false
                                viewModel.restoreAllFromCloud { _, _ -> }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.testTag("settings_confirm_restore")
                        ) {
                            Text(confirmBtn, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showRestoreConfirmDialog = false },
                            modifier = Modifier.testTag("settings_cancel_restore")
                        ) {
                            Text(strings.cancel, fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }
                    }
                )
            }

            if (showDisableLockConfirm) {
                AlertDialog(
                    onDismissRequest = { showDisableLockConfirm = false },
                    title = {
                        Text(
                            text = strings.settingsDisableLockTitle,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )
                    },
                    text = {
                        Text(
                            text = strings.settingsDisableLockMessage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNearBlack,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                appLockEnabled = false
                                biometricEnabled = false
                                hasUnsavedChanges = true
                                settingsNotice = strings.settingsLockDraftNotice
                                settingsNoticeIsError = false
                                showDisableLockConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.testTag("settings_confirm_disable_lock")
                        ) {
                            Text(strings.settingsConfirmDisable, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDisableLockConfirm = false },
                            modifier = Modifier.testTag("settings_keep_lock_button")
                        ) {
                            Text(strings.settingsKeepLock, fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }
                    }
                )
            }

            settingsNotice?.let { notice ->
                Surface(
                    color = if (settingsNoticeIsError) ErrorRedLight else SuccessGreenLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (settingsNoticeIsError) ErrorRed.copy(alpha = 0.35f) else SuccessGreen.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("settings_save_notice")
                ) {
                    Text(
                        text = notice,
                        color = if (settingsNoticeIsError) ErrorRed else SuccessGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.settingsSaveHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMutedGray,
                lineHeight = 17.sp
            )

            // Save configuration button
            AppPrimaryButton(
                text = strings.saveSettings,
                enabled = !mutationInFlight,
                onClick = {
                    settingsNotice = null
                    settingsNoticeIsError = false
                    when (SettingsValidation.errorFor(shopName, securityPin, securityPinConfirm)) {
                        SettingsValidationError.SHOP_NAME_REQUIRED -> {
                            settingsNotice = strings.settingsSaveValidationShopName
                            settingsNoticeIsError = true
                        }
                        SettingsValidationError.INVALID_PIN -> {
                            settingsNotice = strings.settingsSaveValidationPin
                            settingsNoticeIsError = true
                        }
                        SettingsValidationError.PIN_CONFIRMATION_REQUIRED -> {
                            settingsNotice = strings.settingsSaveValidationPinConfirm
                            settingsNoticeIsError = true
                        }
                        null -> {
                            viewModel.saveMerchantSettings(
                                shopName = shopName.trim(),
                                ownerName = ownerName.trim(),
                                ownerPhone = ownerPhone.trim(),
                                welcomeChantEnabled = welcomeChantEnabled,
                                qrImageUri = qrUriString,
                                autoSyncEnabled = autoSyncEnabled,
                                appLockEnabled = appLockEnabled,
                                biometricEnabled = biometricEnabled,
                                newSecurityPin = securityPin.trim().takeIf { it.isNotEmpty() },
                                onSuccess = {
                                    securityPin = ""
                                    securityPinConfirm = ""
                                    hasUnsavedChanges = false
                                    settingsNotice = strings.settingsSavedLocally
                                    settingsNoticeIsError = false
                                },
                                onError = { message ->
                                    settingsNotice = message
                                    settingsNoticeIsError = true
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Footer branding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${strings.settingsVersion}: ${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
internal fun SettingsSectionHeading(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        color = SaffronDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp)
    )
}

@Composable
internal fun SettingsInfoCard(
    title: String,
    detail: String,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMutedGray,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
internal fun SettingsHealthMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = TextNearBlack
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMutedGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

private fun maskedIdentity(value: String, fallback: String): String {
    val normalized = value.trim()
    return if (normalized.isEmpty()) fallback else "••••${normalized.takeLast(8)}"
}
