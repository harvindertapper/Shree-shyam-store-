package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.*
import com.example.viewmodel.ShopViewModel

@Composable
fun FirstLaunchSetupScreen(viewModel: ShopViewModel) {
    var shopName by remember { mutableStateOf("Shree Shyam General Store") }
    var ownerPhone by remember { mutableStateOf("") }
    var welcomeChantEnabled by remember { mutableStateOf(true) }

    var shopNameError by remember { mutableStateOf(false) }

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
                    text = "✍️",
                    fontSize = 44.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "पहला सेटअप (Quick Setup) ✍️",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "दुकान की जानकारी डालें ताकि बिल और खाते सही बन सकें।",
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
                            text = "Shop Details (दुकान की जानकारी)",
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
                            label = "Shop Name (दुकान का नाम) *",
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
                                        text = "Shop name is required!", 
                                        color = ErrorRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Text(
                                        text = "यह नाम बिल के ऊपर दिखेगा।",
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
                            onValueChange = { ownerPhone = it },
                            label = "Mobile Number (ओनर का नंबर - optional)",
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Phone, 
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                ) 
                            },
                            placeholder = "e.g. 9876543210",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
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
                                        text = "Welcome Sound (Chant) 🔔",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextNearBlack
                                    )
                                    Text(
                                        text = "ऐप शुरू होने पर 'जय श्री श्याम' भजन बजेगा।",
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

                // Start Button
                AppPrimaryButton(
                    text = "शुरू करें (Start App) 🚀",
                    onClick = {
                        if (shopName.trim().isEmpty()) {
                            shopNameError = true
                        } else {
                            viewModel.updateSettings(
                                shopName = shopName.trim(),
                                ownerPhone = ownerPhone.trim(),
                                welcomeChantEnabled = welcomeChantEnabled,
                                qrImageUri = "" // blank initially
                            )
                            viewModel.completeFirstLaunch()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_app_button")
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
