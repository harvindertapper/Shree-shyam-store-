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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
            .background(WarmCreamBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PREMIUM DEVOTIONAL SHOP HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SaffronGradientStart, SaffronGradientEnd)
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Devotional Badge
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

                // Shop Name
                Text(
                    text = settings.shopName.ifEmpty { "मेरी दुकान (Smart Kirana)" },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                // Subtitle / Time Greeting
                Text(
                    text = timeGreeting,
                    color = SaffronLight.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cloud Auto-Sync Indicator Badge (Interactive)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier
                        .clickable {
                            viewModel.triggerAutoSync()
                            Toast.makeText(context, "⚡ Background Cloud Sync Triggered", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.lastSyncTime.isNotEmpty()) "Cloud Synced: ${settings.lastSyncTime}" else "Cloud Auto-Sync Active ⚡ (Tap to Sync)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- MAIN CONTENT CONTAINER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HERO SALES BANNER ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(SaffronLight.copy(alpha = 0.4f), Color.Transparent),
                                radius = 600f
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "आज का कुल गल्ला (Today's Total Sale)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMediumGray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = CurrencyUtils.formatRupees(totalToday),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SaffronLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = SaffronDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$billsCount Bills Cut Today",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SaffronDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- DAILY REVENUE TARGET TRACKER ---
            val dailyGoal = 5000.0
            val targetProgress = if (dailyGoal > 0) (totalToday / dailyGoal).toFloat().coerceIn(0f, 1f) else 1f

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(WarningOrangeLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "दैनिक बिक्री लक्ष्य (Daily Target)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (targetProgress >= 1f) SuccessGreenLight else SaffronLight
                        ) {
                            Text(
                                text = "${(targetProgress * 100).toInt()}% Done",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (targetProgress >= 1f) SuccessGreen else SaffronDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { targetProgress },
                        color = if (targetProgress >= 1f) SuccessGreen else SaffronPrimary,
                        trackColor = SlateContainer,
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
                            text = "गल्ला: ${CurrencyUtils.formatRupees(totalToday)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray
                        )
                        Text(
                            text = "लक्ष्य: ${CurrencyUtils.formatRupees(dailyGoal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMutedGray
                        )
                    }
                }
            }

            // --- PAYMENT METHOD SUMMARY PILLS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cash Tile
                MetricSummaryTile(
                    title = "नकद (Cash)",
                    amount = cashToday,
                    accentColor = SuccessGreen,
                    containerColor = SuccessGreenLight,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )

                // UPI Tile
                MetricSummaryTile(
                    title = "UPI / Online",
                    amount = upiToday,
                    accentColor = InfoBlue,
                    containerColor = InfoBlueLight,
                    icon = Icons.Default.QrCodeScanner,
                    modifier = Modifier.weight(1f)
                )

                // Udhaar Tile
                MetricSummaryTile(
                    title = "उधार (Udhaar)",
                    amount = udhaarToday,
                    accentColor = ErrorRed,
                    containerColor = ErrorRedLight,
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- INVENTORY ALERT WIDGET ---
            if (lowStockCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.Products) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ErrorRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "कम स्टॉक चेतावनी (Low Stock Alert)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = ErrorRed
                            )
                            Text(
                                text = "$lowStockCount सामान जल्द ख़त्म होने वाले हैं। ऑर्डर करें!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
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
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "दुकान का स्टॉक सुरक्षित है 👍",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "सभी जरूरी सामान पर्याप्त मात्रा में उपलब्ध हैं।",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMediumGray
                            )
                        }
                    }
                }
            }

            // --- QUICK ACTION GRID ---
            Text(
                text = "दुकान के जरूरी काम (Quick Shortcuts)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack,
                modifier = Modifier.padding(top = 4.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernQuickActionCard(
                        title = "नया बिल बनाएं",
                        subtitle = "Instant Counter POS",
                        icon = Icons.Default.AddShoppingCart,
                        badgeColor = SaffronPrimary,
                        containerColor = SaffronLight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_bill_button"),
                        onClick = { viewModel.navigateTo(Screen.Billing) }
                    )

                    ModernQuickActionCard(
                        title = "नया सामान जोड़ें",
                        subtitle = "Add Product Stock",
                        icon = Icons.Default.AddBox,
                        badgeColor = SuccessGreen,
                        containerColor = SuccessGreenLight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_product_button"),
                        onClick = { viewModel.navigateTo(Screen.AddEditProduct(null)) }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernQuickActionCard(
                        title = "उधार खाता बही",
                        subtitle = "Customer Khata Book",
                        icon = Icons.Default.ImportContacts,
                        badgeColor = ErrorRed,
                        containerColor = ErrorRedLight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_udhaar_button"),
                        onClick = { viewModel.navigateTo(Screen.Udhaar) }
                    )

                    ModernQuickActionCard(
                        title = "बिक्री रिपोर्ट देखें",
                        subtitle = "Analytics & Profit",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        badgeColor = PurpleAccent,
                        containerColor = PurpleAccentLight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_reports_button"),
                        onClick = { viewModel.navigateTo(Screen.Reports) }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSummaryTile(
    title: String,
    amount: Double,
    accentColor: Color,
    containerColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextMediumGray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = CurrencyUtils.formatRupees(amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModernQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(containerColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMutedGray
                )
            }
        }
    }
}

