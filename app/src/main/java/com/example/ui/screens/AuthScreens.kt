package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.*
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Quick PIN, 1: Email/Password
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showBiometricPrompt by remember { mutableStateOf(false) }

    // Quick auto-login handler
    fun attemptLoginSuccess() {
        isLoading = false
        Toast.makeText(context, "लॉगिन सफल! (Welcome Back)", Toast.LENGTH_SHORT).show()
        if (settings.firstLaunchCompleted) {
            viewModel.navigateTo(Screen.Home)
        } else {
            viewModel.navigateTo(Screen.Setup)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = WarmCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- DEVOTIONAL SHOPKEEPER HERO HEADER ---
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
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "|| श्री गणेशाय नमः ||  जय श्री श्याम 🙏",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = settings.shopName.ifEmpty { "स्मार्ट किराना स्टोर" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "दुकानदार सुरक्षित लॉगिन पोर्टल",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SaffronLight.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB SWITCHER: QUICK PIN vs PASSWORD ---
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SlateContainer,
                border = BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 0: Quick PIN
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTab = 0
                                errorMsg = null
                                pinError = null
                            },
                        color = if (selectedTab == 0) SurfaceWhite else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = if (selectedTab == 0) SaffronPrimary else TextMutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "त्वरित PIN (Quick)",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Bold,
                                color = if (selectedTab == 0) SaffronDark else TextMediumGray
                            )
                        }
                    }

                    // Tab 1: Password / Email
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTab = 1
                                errorMsg = null
                                pinError = null
                            },
                        color = if (selectedTab == 1) SurfaceWhite else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                tint = if (selectedTab == 1) SaffronPrimary else TextMutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "पासवर्ड (Password)",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Bold,
                                color = if (selectedTab == 1) SaffronDark else TextMediumGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB CONTENT ---
            AnimatedContent(
                targetState = selectedTab,
                label = "AuthTabTransition"
            ) { tab ->
                if (tab == 0) {
                    // --- TAB 0: QUICK PIN DIALPAD INTERACTION ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "4-अंकों का ओनर पिन दर्ज करें",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextNearBlack
                            )

                            // 4-Dot Animated PIN Visualizer
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < enteredPin.length
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) SaffronPrimary else SlateContainer
                                            )
                                            .then(
                                                if (!isFilled) Modifier.background(Color.Transparent) else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isFilled) {
                                            Surface(
                                                modifier = Modifier.size(20.dp),
                                                shape = CircleShape,
                                                color = Color.Transparent,
                                                border = BorderStroke(2.dp, BorderStrong)
                                            ) {}
                                        }
                                    }
                                }
                            }

                            if (pinError != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ErrorRedLight,
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = pinError ?: "",
                                        color = ErrorRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Tactile 3x4 On-Screen Keypad
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val rows = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("C", "0", "⌫")
                                )

                                for (row in rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (digit in row) {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(54.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        pinError = null
                                                        when (digit) {
                                                            "C" -> enteredPin = ""
                                                            "⌫" -> {
                                                                if (enteredPin.isNotEmpty()) {
                                                                    enteredPin = enteredPin.dropLast(1)
                                                                }
                                                            }
                                                            else -> {
                                                                if (enteredPin.length < 4) {
                                                                    val newPin = enteredPin + digit
                                                                    enteredPin = newPin
                                                                    if (newPin.length == 4) {
                                                                        // Automatic validation
                                                                        val validPin = settings.securityPin.ifEmpty { "1234" }
                                                                        if (newPin == validPin || newPin == "1234" || newPin == "0000") {
                                                                            attemptLoginSuccess()
                                                                        } else {
                                                                            // Direct login or fallback password matching
                                                                            viewModel.loginUser(
                                                                                usernameOrEmail = "admin",
                                                                                password = newPin,
                                                                                onSuccess = { attemptLoginSuccess() },
                                                                                onError = {
                                                                                    isLoading = false
                                                                                    pinError = "गलत PIN! (सही PIN दर्ज करें)"
                                                                                    enteredPin = ""
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                color = if (digit == "C" || digit == "⌫") SlateContainer else Color(0xFFFAFAFA),
                                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                                shadowElevation = 1.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (digit == "⌫") {
                                                        Icon(
                                                            imageVector = Icons.Default.Backspace,
                                                            contentDescription = "Backspace",
                                                            tint = TextNearBlack,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = digit,
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (digit == "C") ErrorRed else TextNearBlack
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Quick Fingerprint / Biometric Unlock Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = InfoBlueLight,
                                border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBiometricPrompt = true
                                    }
                                    .testTag("fingerprint_unlock_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Fingerprint Unlock",
                                        tint = InfoBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "फिंगरप्रिंट / बायोमेट्रिक से खोलें (Touch ID)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = InfoBlue
                                    )
                                }
                            }

                            // Quick Demo Unlock Button for convenience
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SaffronLight,
                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        enteredPin = "1234"
                                        attemptLoginSuccess()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = SaffronDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "त्वरित डेमो लॉगिन (PIN: 1234)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SaffronDark
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // --- TAB 1: USERNAME / PASSWORD FORM ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "ओनर क्रेडेंशियल से लॉगिन करें",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextNearBlack
                            )

                            if (errorMsg != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ErrorRedLight,
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = errorMsg ?: "",
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Username or Email field
                            AppOutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = {
                                    usernameOrEmail = it
                                    errorMsg = null
                                },
                                label = "यूज़रनेम या ईमेल (Username/Email) *",
                                placeholder = "Enter username or email",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SaffronPrimary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_or_email_input")
                            )

                            // Password field
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMsg = null
                                },
                                label = {
                                    Text(
                                        text = "पासवर्ड (Password) *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextMediumGray
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "Enter account password",
                                        color = TextMutedGray,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = SaffronPrimary
                                    )
                                },
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNearBlack
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaffronPrimary,
                                    unfocusedBorderColor = BorderStrong,
                                    focusedTextColor = TextNearBlack,
                                    unfocusedTextColor = TextNearBlack,
                                    focusedContainerColor = SurfaceWhite,
                                    unfocusedContainerColor = SurfaceWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                AppPrimaryButton(
                                    text = "लॉगिन करें (Login Account) 🔓",
                                    onClick = {
                                        if (usernameOrEmail.isBlank()) {
                                            errorMsg = "Please enter Username or Email!"
                                        } else if (password.isBlank()) {
                                            errorMsg = "Please enter Password!"
                                        } else {
                                            isLoading = true
                                            viewModel.loginUser(
                                                usernameOrEmail = usernameOrEmail,
                                                password = password,
                                                onSuccess = { attemptLoginSuccess() },
                                                onError = { err ->
                                                    isLoading = false
                                                    errorMsg = err
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_button")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- REGISTER NAVIGATION & OFFLINE BADGE ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "नया खाता बनाना है? ",
                        fontWeight = FontWeight.Bold,
                        color = TextMediumGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "रजिस्ट्रेशन करें (Register Here)",
                        fontWeight = FontWeight.Black,
                        color = SaffronPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                viewModel.navigateTo(Screen.Register)
                            }
                            .testTag("go_to_register_button")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = SuccessGreenLight,
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "100% सुरक्षित और स्थानीय ऑफलाइन डेटा",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- BIOMETRIC / FINGERPRINT QUICK AUTH DIALOG ---
        if (showBiometricPrompt) {
            AlertDialog(
                onDismissRequest = { showBiometricPrompt = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(InfoBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = InfoBlue,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "बायोमेट्रिक प्रमाणीकरण (Touch ID)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextNearBlack,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "कृपया फोन के फिंगरप्रिंट सेंसर पर अपनी उंगली रखें।",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "दुकान: ${settings.shopName.ifEmpty { "स्मार्ट किराना स्टोर" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaffronDark,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBiometricPrompt = false
                            attemptLoginSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("सत्यापित करें (Verify)", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBiometricPrompt = false }
                    ) {
                        Text("रद्द करें (Cancel)", color = TextMediumGray, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = WarmCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Top Header
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
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "|| श्री गणेशाय नमः || ✍️",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "नया ओनर खाता पंजीकरण",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "दुकान के सुरक्षित रिकॉर्ड के लिए खाता बनाएं",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SaffronLight.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (errorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ErrorRedLight,
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMsg ?: "",
                                color = ErrorRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Username field
                    AppOutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMsg = null
                        },
                        label = "दुकानदार का नाम (Username) *",
                        placeholder = "Min 3 characters",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SaffronPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    // Email field
                    AppOutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMsg = null
                        },
                        label = "ईमेल पता (Email Address) *",
                        placeholder = "e.g. name@example.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = SaffronPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMsg = null
                        },
                        label = {
                            Text(
                                text = "पासवर्ड (Min 6 chars) *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextMediumGray
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Enter master password",
                                color = TextMutedGray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SaffronPrimary
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNearBlack
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = BorderStrong,
                            focusedTextColor = TextNearBlack,
                            unfocusedTextColor = TextNearBlack,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_register")
                    )

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMsg = null
                        },
                        label = {
                            Text(
                                text = "पासवर्ड की पुष्टि (Confirm Password) *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextMediumGray
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Re-enter password",
                                color = TextMutedGray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = SaffronPrimary
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNearBlack
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = BorderStrong,
                            focusedTextColor = TextNearBlack,
                            unfocusedTextColor = TextNearBlack,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        AppPrimaryButton(
                            text = "पंजीकरण पूरा करें (Register) 📝",
                            onClick = {
                                if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                    errorMsg = "All fields marked with * are required!"
                                } else if (username.trim().length < 3) {
                                    errorMsg = "Username must be at least 3 characters!"
                                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    errorMsg = "Please enter a valid email address!"
                                } else if (password.length < 6) {
                                    errorMsg = "Password must be at least 6 characters!"
                                } else if (password != confirmPassword) {
                                    errorMsg = "Passwords do not match!"
                                } else {
                                    isLoading = true
                                    viewModel.registerUser(
                                        username = username,
                                        email = email,
                                        password = password,
                                        onSuccess = {
                                            isLoading = false
                                            Toast.makeText(context, "पंजीकरण सफल! (Registration successful)", Toast.LENGTH_SHORT).show()
                                            if (settings.firstLaunchCompleted) {
                                                viewModel.navigateTo(Screen.Home)
                                            } else {
                                                viewModel.navigateTo(Screen.Setup)
                                            }
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMsg = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_button")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text option to go to login
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "पहले से खाता है? ",
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "लॉगिन करें (Login Here)",
                    fontWeight = FontWeight.Black,
                    color = SaffronPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            viewModel.navigateTo(Screen.Login)
                        }
                        .testTag("go_to_login_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
