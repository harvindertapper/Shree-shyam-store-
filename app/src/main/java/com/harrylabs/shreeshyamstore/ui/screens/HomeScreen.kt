package com.harrylabs.shreeshyamstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.ui.theme.*
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import com.harrylabs.shreeshyamstore.utils.DateTimeUtils
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel

@Composable
fun HomeScreen(viewModel: ShopViewModel) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp), // spacing for bottom menu
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Devotional Saffron Decorative top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SaffronPrimary, SaffronDark)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = settings.shopName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
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
                        text = stringResource(R.string.home_todays_sales),
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
                            label = { Text(stringResource(R.string.home_bills_today, billsCount), fontWeight = FontWeight.Bold, color = TextNearBlack) },
                            icon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp), tint = TextNearBlack) }
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
                        Text(text = stringResource(R.string.home_cash), fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
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
                        Text(text = stringResource(R.string.home_upi_paid), fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
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
                        Text(text = stringResource(R.string.home_credit), fontSize = 13.sp, color = TextNearBlack, fontWeight = FontWeight.ExtraBold)
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
                            contentDescription = stringResource(R.string.content_description_alert),
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_low_stock_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = ErrorRed
                            )
                            Text(
                                text = stringResource(R.string.home_low_stock_message, lowStockCount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.content_description_see_items),
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
                            contentDescription = stringResource(R.string.content_description_all_set),
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.home_stock_safe_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                            Text(
                                text = stringResource(R.string.home_stock_safe_message),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNearBlack
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_quick_operations),
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
                        title = stringResource(R.string.home_new_bill),
                        subtitle = stringResource(R.string.home_new_bill_subtitle),
                        icon = Icons.Default.AddShoppingCart,
                        backgroundColor = Color(0xFFE3F2FD),
                        iconColor = Color(0xFF0D47A1),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_bill_button"),
                        onClick = { viewModel.navigateTo(Screen.Billing) }
                    )

                    HomeMenuButton(
                        title = stringResource(R.string.home_add_product),
                        subtitle = stringResource(R.string.home_add_product_subtitle),
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
                        title = stringResource(R.string.home_opening_stock),
                        subtitle = stringResource(R.string.home_opening_stock_subtitle),
                        icon = Icons.Default.ViewList,
                        backgroundColor = Color(0xFFFFF3E0),
                        iconColor = Color(0xFFE65100),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_opening_stock_button"),
                        onClick = { viewModel.navigateTo(Screen.OpeningStock) }
                    )

                    HomeMenuButton(
                        title = stringResource(R.string.home_udhaar_ledger),
                        subtitle = stringResource(R.string.home_udhaar_ledger_subtitle),
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
