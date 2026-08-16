package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.*
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.AuthManager
import com.example.utils.LocaleHelper
import com.example.utils.SecurityUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

@Composable
fun LanguageSwitcherPill(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        modifier = modifier.testTag("language_switcher_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = "Language",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // EN Button
            Surface(
                shape = RoundedCornerShape(50),
                color = if (currentLanguage == AppLanguage.ENGLISH) Color.White else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onLanguageChange(AppLanguage.ENGLISH) }
                    .testTag("lang_en_button")
            ) {
                Text(
                    text = "EN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentLanguage == AppLanguage.ENGLISH) SaffronDark else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Hindi Button
            Surface(
                shape = RoundedCornerShape(50),
                color = if (currentLanguage == AppLanguage.HINDI) Color.White else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onLanguageChange(AppLanguage.HINDI) }
                    .testTag("lang_hi_button")
            ) {
                Text(
                    text = "हिंदी",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentLanguage == AppLanguage.HINDI) SaffronDark else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = WarmCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SaffronGradientStart, SaffronGradientEnd)
                        ),
                        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp)
            ) {
                // Language Switcher on Top-Right
                LanguageSwitcherPill(
                    currentLanguage = settings.appLanguage,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = strings.godBlessing,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = if (settings.shopName.isNotBlank()) settings.shopName else strings.devotionalHeader,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = strings.shopSubtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SaffronLight.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Middle Feature highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.AddShoppingCart,
                    title = strings.feature1Title,
                    desc = strings.feature1Desc
                )
                FeatureRow(
                    icon = Icons.Default.ImportContacts,
                    title = strings.feature2Title,
                    desc = strings.feature2Desc
                )
                FeatureRow(
                    icon = Icons.Default.Inventory2,
                    title = strings.feature3Title,
                    desc = strings.feature3Desc
                )
                FeatureRow(
                    icon = Icons.Default.Lock,
                    title = strings.feature4Title,
                    desc = strings.feature4Desc
                )
            }

            // Bottom Google Sign In & Local Skip Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = ErrorRedLight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Google Sign-In Primary Button
                Button(
                    onClick = {
                        isGoogleLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = AuthManager.signInWithGoogle(context)
                            isGoogleLoading = false
                            if (result.isSuccess) {
                                val user = result.getOrNull()
                                if (user != null) {
                                    viewModel.onGoogleSignInSuccess(
                                        uid = user.uid,
                                        email = user.email ?: "",
                                        displayName = user.displayName ?: "",
                                        onSuccess = { isFirstTime ->
                                            val welcomeMsg = if (settings.appLanguage == AppLanguage.HINDI) "लॉगिन सफल!" else "Login Successful!"
                                            Toast.makeText(context, "$welcomeMsg ${user.displayName ?: ""}", Toast.LENGTH_SHORT).show()
                                            if (isFirstTime) {
                                                viewModel.navigateTo(Screen.Setup)
                                            } else {
                                                viewModel.navigateTo(Screen.Home)
                                            }
                                        }
                                    )
                                }
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Google Sign In Failed"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.continueWithGoogle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // 2. Trust Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SaffronLight.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = strings.offlineBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // 3. Subtle Skip for Now Link
                TextButton(
                    onClick = {
                        if (settings.firstLaunchCompleted) {
                            viewModel.navigateTo(Screen.Login)
                        } else {
                            viewModel.navigateTo(Screen.Setup)
                        }
                    },
                    modifier = Modifier.testTag("skip_login_button")
                ) {
                    Text(
                        text = strings.skipForNow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMediumGray
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SaffronLight,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = SaffronDark, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextNearBlack)
                Text(text = desc, fontSize = 12.sp, color = TextMediumGray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    fun checkPin(pin: String) {
        if (SecurityUtils.verifyPin(pin, settings.securityPin)) {
            pinError = null
            Toast.makeText(context, strings.storeUnlocked, Toast.LENGTH_SHORT).show()
            if (settings.firstLaunchCompleted) {
                viewModel.navigateTo(Screen.Home)
            } else {
                viewModel.navigateTo(Screen.Setup)
            }
        } else {
            pinError = strings.incorrectPin
            enteredPin = ""
        }
    }

    fun launchBiometricPrompt() {
        if (context is FragmentActivity) {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(strings.unlockStore)
                .setSubtitle(strings.verifyBiometric)
                .setNegativeButtonText(strings.usePin)
                .build()

            val biometricPrompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(context, strings.identityVerified, Toast.LENGTH_SHORT).show()
                    if (settings.firstLaunchCompleted) {
                        viewModel.navigateTo(Screen.Home)
                    } else {
                        viewModel.navigateTo(Screen.Setup)
                    }
                }
            })
            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Auto prompt biometric on first load if enabled
    LaunchedEffect(settings.biometricEnabled) {
        if (settings.biometricEnabled) {
            launchBiometricPrompt()
        }
    }

    if (showForgotPinDialog) {
        ForgotPinDialog(
            viewModel = viewModel,
            registeredEmail = settings.loggedInEmail,
            strings = strings,
            onDismiss = { showForgotPinDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = WarmCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBg)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SaffronGradientStart, SaffronGradientEnd)
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.godChant,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = settings.shopName.ifEmpty { strings.defaultShopName },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.secureAppLock,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SaffronLight.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // PIN Dots & Error
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.enterSecurityPin,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Surface(
                            shape = CircleShape,
                            color = if (isFilled) SaffronPrimary else SlateContainer,
                            border = BorderStroke(2.dp, if (isFilled) SaffronPrimary else BorderStrong),
                            modifier = Modifier.size(20.dp)
                        ) {}
                    }
                }

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = pinError ?: "",
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dialpad Grid (1-9, Bio, 0, Backspace)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (digit in row) {
                            when (digit) {
                                "BIO" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (settings.biometricEnabled) SaffronLight else Color.Transparent,
                                        border = if (settings.biometricEnabled) BorderStroke(1.dp, SaffronPrimary) else null,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable(enabled = settings.biometricEnabled) {
                                                launchBiometricPrompt()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (settings.biometricEnabled) {
                                                Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = SaffronDark, modifier = Modifier.size(32.dp))
                                            }
                                        }
                                    }
                                }
                                "DEL" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = SlateContainer,
                                        border = BorderStroke(1.dp, SurfaceCardBorder),
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    pinError = null
                                                }
                                            }
                                            .testTag("pin_key_delete")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextNearBlack, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.5.dp, BorderStrong),
                                        tonalElevation = 2.dp,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + digit
                                                    enteredPin = newPin
                                                    pinError = null
                                                    if (newPin.length == 4) {
                                                        checkPin(newPin)
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$digit")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = digit,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextNearBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Forgot PIN & Switch Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showForgotPinDialog = true },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Text(strings.forgotPin, color = SaffronDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                TextButton(
                    onClick = { viewModel.navigateTo(Screen.Welcome) },
                    modifier = Modifier.testTag("switch_account_button")
                ) {
                    Text(strings.switchAccount, color = TextMediumGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ForgotPinDialog(
    viewModel: ShopViewModel,
    registeredEmail: String,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    var emailInput by remember { mutableStateOf(registeredEmail) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, BorderStrong),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(44.dp))

                Text(
                    text = strings.resetPin,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                Text(
                    text = strings.resetLinkSent,
                    fontSize = 13.sp,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(strings.registeredEmail) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        color = if (isSuccess) SuccessGreen else ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            statusMessage = null
                            viewModel.sendForgotPinEmail(
                                email = emailInput,
                                onSuccess = {
                                    isLoading = false
                                    isSuccess = true
                                    statusMessage = strings.resetLinkSent
                                },
                                onError = { err ->
                                    isLoading = false
                                    isSuccess = false
                                    statusMessage = err
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(strings.sendLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
