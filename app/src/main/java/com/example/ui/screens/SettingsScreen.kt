package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.AppPrimaryButton
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()

    var shopName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("1234") }
    var welcomeChantEnabled by remember { mutableStateOf(true) }
    var qrUriString by remember { mutableStateOf("") }
    var autoSyncEnabled by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val syncInProgress by viewModel.syncInProgress.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    LaunchedEffect(settings) {
        shopName = settings.shopName
        ownerPhone = settings.ownerPhone
        securityPin = settings.securityPin
        welcomeChantEnabled = settings.welcomeChantEnabled
        qrUriString = settings.staticPaytmQrImageUri
        autoSyncEnabled = settings.autoSyncEnabled
    }

    // Modern Secure Gallery Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            qrUriString = uri.toString()
            Toast.makeText(context, "QR code image selected! सुरक्षित करें पर टैप करें।", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ऐप सेटिंग्स (Settings) ⚙️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                                    contentDescription = "User avatar",
                                    tint = SaffronDark,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "एक्टिव ओनर (Active Owner):",
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
                                }
                            }
                            Button(
                                onClick = { viewModel.logoutUser() },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("logout_button").heightIn(min = 40.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Log out", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logout", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

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
                        text = "दुकान की जानकारी (Shop Profile)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark
                    )

                    AppOutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = "Shop Name (दुकान का नाम) *",
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
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = "Owner Phone Number (ओनर का नंबर)",
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Phone, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        placeholder = "e.g. 9876543210",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_phone_field")
                    )
                }
            }

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
                                text = "4-अंकों का ओनर पिन (Security PIN) 🔒",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            Text(
                                text = "ऐप में तुरंत लॉगिन करने के लिए अपना PIN सेट करें",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedGray
                            )
                        }
                    }

                    AppOutlinedTextField(
                        value = securityPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                securityPin = it
                            }
                        },
                        label = "Owner PIN (डिफ़ॉल्ट: 1234) *",
                        placeholder = "4-digit PIN",
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
                            text = "Welcome Chanting 🔔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = "ऐप शुरू होने पर 'जय श्री श्याम' भजन बजाएं।",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMutedGray,
                        )
                    }
                    Switch(
                        checked = welcomeChantEnabled,
                        onCheckedChange = { welcomeChantEnabled = it },
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
                        text = "Paytm Business QR Code 📲",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = SaffronDark,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Text(
                        text = "यहाँ दुकान का static Paytm QR कोड फोटो अपलोड करें ताकि हिसाब करते समय कस्टमर इसे देख सकें।",
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
                                contentDescription = "Uploaded QR Preview",
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
                            Text("Change QR Photo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                                Text("No QR Selected", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
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
                            Text("Upload QR Code Photo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 4: Firebase REST Sync Config & Operations
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
                            Text(
                                text = "क्लाउड बैकअप (Firebase Cloud Sync) ☁️",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            Text(
                                text = "सुरक्षित रियल-टाइम डेटा सिंक और रिकवरी",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedGray
                            )
                        }
                    }

                    Divider(color = BorderStrong)

                    Text(
                        text = "क्लाउड बैकअप आपके स्टोर अकाउंट (Username/Email) के साथ स्वचालित (Automatic) रूप से आपके सुरक्षित स्टोर डेटाबेस पर सिंक होता है। 👍",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "अंतिम सिंक समय (Last Synced):",
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
                        }
                    }

                    if (syncInProgress) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = SaffronPrimary)
                            Text(
                                text = syncMessage ?: "Processing...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextNearBlack
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Save config first, then sync
                                    viewModel.updateFirebaseSettings("", "", autoSyncEnabled)
                                    viewModel.syncAllToCloud { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("बैकअप (Backup)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateFirebaseSettings("", "", autoSyncEnabled)
                                    showRestoreConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("रीस्टोर (Restore)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // CONFIRMATION DIALOG FOR DATA RESTORATIVE OVERWRITE
            if (showRestoreConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = false },
                    title = {
                        Text(
                            text = "डेटा मिटाने की चेतावनी! ⚠️",
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )
                    },
                    text = {
                        Text(
                            text = "क्या आप निश्चित रूप से क्लाउड बैकअप से डेटा रीस्टोर करना चाहते हैं? इससे आपके फोन का वर्तमान लोकल डेटा पूरी तरह मिट जाएगा और क्लाउड बैकअप डेटा डाला जाएगा!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNearBlack
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRestoreConfirmDialog = false
                                viewModel.restoreAllFromCloud { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("हाँ, रीस्टोर करें", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showRestoreConfirmDialog = false }
                        ) {
                            Text("रद्द करें", fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save configuration button
            AppPrimaryButton(
                text = "सुरक्षित करें (Save Settings) 💾",
                onClick = {
                    if (shopName.trim().isEmpty()) {
                        Toast.makeText(context, "Shop name is required!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateSettings(
                            shopName = shopName.trim(),
                            ownerPhone = ownerPhone.trim(),
                            welcomeChantEnabled = welcomeChantEnabled,
                            qrImageUri = qrUriString,
                            securityPin = if (securityPin.length == 4) securityPin else "1234"
                        )
                        viewModel.updateFirebaseSettings(
                            url = "",
                            prefix = "",
                            autoSync = autoSyncEnabled
                        )
                        Toast.makeText(context, "Settings saved successfully! 👍", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(Screen.Home)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
