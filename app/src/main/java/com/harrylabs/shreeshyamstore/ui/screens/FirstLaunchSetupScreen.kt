package com.harrylabs.shreeshyamstore.ui.screens

import android.widget.Toast
import com.harrylabs.shreeshyamstore.viewmodel.AuthState
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.ui.components.AppOutlinedTextField
import com.harrylabs.shreeshyamstore.ui.components.AppPrimaryButton
import com.harrylabs.shreeshyamstore.ui.theme.*
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel

@Composable
fun FirstLaunchSetupScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    var shopName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var welcomeChantEnabled by remember { mutableStateOf(true) }

    var shopNameError by remember { mutableStateOf(false) }
    var ownerPhoneError by remember { mutableStateOf(false) }
    val defaultShopName = stringResource(R.string.default_shop_name)

    LaunchedEffect(defaultShopName) {
        if (shopName.isBlank()) {
            shopName = defaultShopName
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Heading Logo/Chant Emoji
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SaffronDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.setup_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.setup_intro),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
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
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.setup_shop_details),
                            fontSize = 19.sp,
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
                            label = stringResource(R.string.shop_name_label),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Business, 
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                ) 
                            },
                            isError = shopNameError,
                            supportingText = {
                                if (shopNameError) {
                                    Text(
                                        text = stringResource(R.string.shop_name_required),
                                        color = ErrorRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.shop_name_supporting_text),
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedGray,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_name_input")
                        )

                        // Phone number
                        AppOutlinedTextField(
                            value = ownerPhone,
                            onValueChange = {
                                ownerPhone = it
                                if (isValidOwnerPhone(it)) ownerPhoneError = false
                            },
                            label = stringResource(R.string.owner_phone_label),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Phone, 
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                ) 
                            },
                            placeholder = stringResource(R.string.phone_placeholder),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            isError = ownerPhoneError,
                            supportingText = {
                                if (ownerPhoneError) {
                                    Text(
                                        text = stringResource(R.string.owner_phone_required_or_invalid),
                                        color = ErrorRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.owner_phone_supporting_text),
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedGray,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_phone_input")
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = BorderStrong,
                            thickness = 1.5.dp
                        )

                        // Chant Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.welcome_chant_title),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextNearBlack
                                    )
                                    Text(
                                        text = stringResource(R.string.welcome_chant_description),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMediumGray
                                    )
                                }
                            }
                            Switch(
                                checked = welcomeChantEnabled,
                                onCheckedChange = { welcomeChantEnabled = it },
                                modifier = Modifier.testTag("chant_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SaffronPrimary,
                                    checkedTrackColor = SaffronLight,
                                    uncheckedThumbColor = BorderStrong,
                                    uncheckedTrackColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Start Button or Progress Indicator
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("setup_progress")
                    )
                } else {
                    AppPrimaryButton(
                        text = stringResource(R.string.setup_start_app),
                        onClick = {
                            if (shopName.trim().isEmpty()) {
                                shopNameError = true
                            } else if (!isValidOwnerPhone(ownerPhone)) {
                                ownerPhoneError = true
                            } else {
                                viewModel.createShop(
                                    shopName = shopName.trim(),
                                    ownerPhone = ownerPhone.trim(),
                                    welcomeChantEnabled = welcomeChantEnabled,
                                    context = context,
                                    onSuccess = {
                                        Toast.makeText(context, context.getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_app_button")
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private fun isValidOwnerPhone(ownerPhone: String): Boolean {
    val trimmedPhone = ownerPhone.trim()
    return trimmedPhone.length in 10..15 && trimmedPhone.all { it.isDigit() }
}
