package com.aistudio.shreeshyamstore.pqwzkb.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppOutlinedTextField
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppPrimaryButton
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel

@Composable
fun FirstLaunchSetupScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val currentSettings by viewModel.storeSettings.collectAsState()
    val strings = remember(currentSettings.appLanguage) { LocaleHelper.getStrings(currentSettings.appLanguage) }
    val googleUser = remember { com.aistudio.shreeshyamstore.pqwzkb.utils.AuthManager.currentUser }

    val initialOwnerName = remember(currentSettings, googleUser) {
        if (currentSettings.ownerName.isNotBlank()) {
            currentSettings.ownerName
        } else if (currentSettings.loggedInUsername.isNotBlank()) {
            currentSettings.loggedInUsername
        } else {
            googleUser?.displayName ?: ""
        }
    }

    var shopName by remember { mutableStateOf(currentSettings.shopName.ifEmpty { strings.defaultShopName }) }
    var ownerName by remember(initialOwnerName) { mutableStateOf(initialOwnerName) }
    var ownerPhone by remember { mutableStateOf(currentSettings.ownerPhone) }
    var securityPin by remember { mutableStateOf("1234") }
    var enableBiometric by remember { mutableStateOf(false) }

    var shopNameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = WarmCreamBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBg)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Heading
                Text(
                    text = "🏬",
                    fontSize = 44.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.shopSetupTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.shopSetupDesc,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                // Shop Details Form Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.storeInfo,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronPrimary
                        )

                        // Shop Name
                        AppOutlinedTextField(
                            value = shopName,
                            onValueChange = {
                                shopName = it
                                if (it.trim().isNotEmpty()) shopNameError = false
                            },
                            label = strings.shopNameLabel,
                            leadingIcon = {
                                Icon(Icons.Default.Store, contentDescription = null, tint = SaffronPrimary)
                            },
                            isError = shopNameError,
                            supportingText = {
                                if (shopNameError) {
                                    val err = if (currentSettings.appLanguage == AppLanguage.HINDI) "दुकान का नाम आवश्यक है!" else "Shop name is required!"
                                    Text(err, color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    val hint = if (currentSettings.appLanguage == AppLanguage.HINDI) "यह नाम ग्राहकों के बिल पर दिखेगा।" else "This name will appear on bills."
                                    Text(hint, color = TextMutedGray, fontSize = 12.sp)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_name_input")
                        )

                        // Owner Name
                        AppOutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = strings.ownerNameLabel,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SaffronPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_name_input")
                        )

                        // Phone number
                        AppOutlinedTextField(
                            value = ownerPhone,
                            onValueChange = {
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    ownerPhone = it
                                    phoneError = null
                                }
                            },
                            label = strings.ownerPhoneLabel,
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SaffronPrimary)
                            },
                            placeholder = "9876543210",
                            isError = phoneError != null,
                            supportingText = {
                                if (phoneError != null) {
                                    Text(phoneError ?: "", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_phone_input")
                        )
                    }
                }

                // Security PIN Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.securityPinTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronPrimary
                        )

                        AppOutlinedTextField(
                            value = securityPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    securityPin = it
                                    pinError = null
                                }
                            },
                            label = strings.securityPinLabel,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SaffronPrimary)
                            },
                            placeholder = "1234",
                            isError = pinError != null,
                            supportingText = {
                                if (pinError != null) {
                                    Text(pinError ?: "", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_pin_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.enableBiometricLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextNearBlack
                                )
                            }

                            Switch(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SaffronPrimary,
                                    checkedTrackColor = SaffronLight
                                ),
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save & Complete Button
                AppPrimaryButton(
                    text = strings.startShopButton,
                    onClick = {
                        if (shopName.trim().isEmpty()) {
                            shopNameError = true
                            return@AppPrimaryButton
                        }
                        if (ownerPhone.isNotEmpty() && ownerPhone.length < 10) {
                            phoneError = if (currentSettings.appLanguage == AppLanguage.HINDI) "कृपया सही 10 अंकों का मोबाइल नंबर दर्ज करें" else "Please enter valid 10 digit phone number"
                            return@AppPrimaryButton
                        }
                        if (securityPin.length != 4) {
                            pinError = if (currentSettings.appLanguage == AppLanguage.HINDI) "कृपया 4 अंकों का पिन सेट करें (उदा. 1234)" else "Please enter a 4-digit PIN (e.g. 1234)"
                            return@AppPrimaryButton
                        }

                        viewModel.saveShopProfile(
                            shopName = shopName,
                            ownerName = ownerName,
                            ownerPhone = ownerPhone,
                            onSuccess = {
                                viewModel.setAppLockPin(securityPin, enableBiometric)
                                val successMsg = if (currentSettings.appLanguage == AppLanguage.HINDI) "दुकान सेटअप पूरा हुआ! 🎉" else "Shop setup completed! 🎉"
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                viewModel.completeFirstLaunch()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_shop_setup_button")
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
