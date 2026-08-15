package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.CurrencyUtils
import com.example.utils.DateTimeUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel

@Composable
fun HomeScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val sales by viewModel.salesHistory.collectAsState()
    val products by viewModel.products.collectAsState()

    // Determine bounds for Today
    val startOfDay = remember { DateTimeUtils.getStartOfDay() }
    val endOfDay = remember { DateTimeUtils.getEndOfDay() }

    // Aggregate statistics reactively
    val todaySales = remember(sales) {
        sales.filter { it.createdAt in startOfDay..endOfDay }
    }
    val totalToday = remember(todaySales) { todaySales.sumOf { it.totalAmount } }
    val cashToday = remember(todaySales) { todaySales.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } }
    val upiToday = remember(todaySales) { todaySales.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } }
    val udhaarToday = remember(todaySales) { todaySales.filter { it.paymentMode == "UDHAAR" }.sumOf { it.totalAmount } }
    val billsCount = remember(todaySales) { todaySales.size }

    val lowStockProducts = remember(products) {
        products.filter { it.isActive && it.trackStock && it.currentStock <= it.lowStockAlertQty }
    }
    val lowStockCount = remember(lowStockProducts) { lowStockProducts.size }

    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val timeGreeting = remember(hour) {
        when (hour) {
            in 4..11 -> "शुभ प्रभात / Good Morning! ☀️"
            in 12..16 -> "शुभ दोपहर / Good Afternoon! 🌤️"
            in 17..20 -> "शुभ संध्या / Good Evening! 🌅"
            else -> "शुभ रात्रि / Good Night! 🌙"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp), // spacing for bottom menu
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Devotional Saffron Decorative top bar with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SaffronPrimary, SaffronDark)
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$timeGreeting | जय श्री श्याम 🙏",
                    color = SaffronLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = settings.shopName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "कर्म ही पूजा है 🚩 | समृद्धि और सफलता का वरदान",
                    color = SaffronLight.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Cloud Auto-Sync Indicator Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable {
                        viewModel.triggerAutoSync()
                        Toast.makeText(context, "Background sync triggered!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (settings.lastSyncTime.isNotEmpty()) "Cloud Auto-Sync: ${settings.lastSyncTime}" else "Cloud Auto-Sync: Active ⚡",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MAIN SALES CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SaffronLight),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, SaffronPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "आज का कुल गल्ला (Today's Sales) 🪙",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = CurrencyUtils.formatRupees(totalToday),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = TextNearBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$billsCount Bills Today", fontWeight = FontWeight.Bold, color = TextNearBlack) },
                            icon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp), tint = TextNearBlack) }
                        )
                    }
                }
            }

            // --- DAILY TARGET TRACKER ---
            val dailyGoal = 5000.0
            val targetProgress = if (dailyGoal > 0) (totalToday / dailyGoal).toFloat().coerceIn(0f, 1f) else 1f
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "आज का लक्ष्य (Daily Target Tracker) 🎯",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }
                        Text(
                            text = "${(targetProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (targetProgress >= 1f) SuccessGreen else SaffronPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { targetProgress },
                        color = if (targetProgress >= 1f) SuccessGreen else SaffronPrimary,
                        trackColor = SaffronLight,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "कुल गुल्लक: ${CurrencyUtils.formatRupees(totalToday)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMediumGray
                        )
                        Text(
                            text = "लक्ष्य: ${CurrencyUtils.formatRupees(dailyGoal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedGray
                        )
                    }
                }
            }

            // --- BREAKDOWN CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cash
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStrong),
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "नकद (Cash)", fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupees(cashToday),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen // Positive Green
                        )
                    }
                }

                // UPI
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStrong),
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "UPI Paid", fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupees(upiToday),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronPrimary // UPI
                        )
                    }
                }

                // Udhaar
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStrong),
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "उधार (Udhaar)", fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupees(udhaarToday),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed // Warning red
                        )
                    }
                }
            }

            // --- WARNING CARD: LOW STOCK ---
            if (lowStockCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ErrorRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.Products) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "कम स्टॉक चेतावनी (Low Stock) ⚠️",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = ErrorRed
                            )
                            Text(
                                text = "$lowStockCount आइटम स्टॉक ख़त्म होने वाले हैं।",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "See items",
                            tint = ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All set",
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "स्टॉक सुरक्षित है 👍",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                            Text(
                                text = "सभी आइटम का स्टॉक पर्याप्त है।",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }
                    }
                }
            }

            Text(
                text = "त्वरित विकल्प (Quick Operations)",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            // --- QUICK ACTION BUTTONS GRID ---
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeMenuButton(
                        title = "नया बिल (New Bill)",
                        subtitle = "Fast Billing Counter",
                        icon = Icons.Default.AddShoppingCart,
                        backgroundColor = Color(0xFFE3F2FD),
                        iconColor = Color(0xFF0D47A1),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_bill_button"),
                        onClick = { viewModel.navigateTo(Screen.Billing) }
                    )

                    HomeMenuButton(
                        title = "नया माल (Add Product)",
                        subtitle = "Register New Stock",
                        icon = Icons.Default.AddBox,
                        backgroundColor = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF1B5E20),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_product_button"),
                        onClick = { viewModel.navigateTo(Screen.AddEditProduct(null)) }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeMenuButton(
                        title = "शुरुआती स्टॉक (Opening Stock)",
                        subtitle = "Rapid Inventory Entry",
                        icon = Icons.Default.ViewList,
                        backgroundColor = Color(0xFFFFF3E0),
                        iconColor = Color(0xFFE65100),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_opening_stock_button"),
                        onClick = { viewModel.navigateTo(Screen.OpeningStock) }
                    )

                    HomeMenuButton(
                        title = "उधार खाता (Udhaar Ledger)",
                        subtitle = "Track Credits & Payments",
                        icon = Icons.Default.ImportContacts,
                        backgroundColor = Color(0xFFFFEBEE),
                        iconColor = Color(0xFFB71C1C),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_udhaar_button"),
                        onClick = { viewModel.navigateTo(Screen.Udhaar) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeMenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(backgroundColor, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextNearBlack
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMediumGray
            )
        }
    }
}
