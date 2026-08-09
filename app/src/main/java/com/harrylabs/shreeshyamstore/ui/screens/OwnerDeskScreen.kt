package com.harrylabs.shreeshyamstore.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.ui.theme.BorderStrong
import com.harrylabs.shreeshyamstore.ui.theme.ErrorRed
import com.harrylabs.shreeshyamstore.ui.theme.SaffronDark
import com.harrylabs.shreeshyamstore.ui.theme.SaffronLight
import com.harrylabs.shreeshyamstore.ui.theme.SuccessGreen
import com.harrylabs.shreeshyamstore.ui.theme.TextMediumGray
import com.harrylabs.shreeshyamstore.ui.theme.TextNearBlack
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDeskScreen(viewModel: ShopViewModel) {
    val ownerDeskState by viewModel.ownerDeskState.collectAsState()
    val stockValue = ownerDeskState.stockValue
    val profit = ownerDeskState.profit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.owner_desk_title),
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("owner_desk_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SaffronLight),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SaffronDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SaffronDark
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.owner_desk_private_badge),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = stringResource(R.string.owner_desk_private_note),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray
                        )
                    }
                }
            }

            OwnerMetricCard(
                title = stringResource(R.string.owner_desk_selling_stock_value),
                value = formatPaise(stockValue.totalSellingValuePaise),
                subtitle = stringResource(R.string.owner_desk_tracked_items, stockValue.trackedProductCount),
                accentColor = SuccessGreen,
                modifier = Modifier.testTag("owner_total_stock_value")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OwnerMetricCard(
                    title = stringResource(R.string.owner_desk_purchase_stock_value),
                    value = formatPaise(stockValue.totalPurchaseValuePaise),
                    subtitle = stringResource(R.string.owner_desk_missing_purchase_price, stockValue.missingPurchasePriceProductCount),
                    accentColor = SaffronDark,
                    modifier = Modifier.weight(1f)
                )
                OwnerMetricCard(
                    title = stringResource(R.string.owner_desk_potential_margin),
                    value = formatPaise(stockValue.potentialMarginPaise),
                    subtitle = stringResource(R.string.owner_desk_untracked_items, stockValue.untrackedProductCount),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(R.string.owner_desk_profit_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OwnerMetricCard(
                    title = stringResource(R.string.owner_desk_today_profit),
                    value = formatPaise(profit.todayProfitPaise),
                    subtitle = stringResource(R.string.owner_desk_sales_value, formatPaise(profit.todaySalesValuePaise)),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f).testTag("owner_today_profit")
                )
                OwnerMetricCard(
                    title = stringResource(R.string.owner_desk_month_profit),
                    value = formatPaise(profit.monthProfitPaise),
                    subtitle = stringResource(R.string.owner_desk_purchase_cost, formatPaise(profit.monthPurchaseCostPaise)),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f).testTag("owner_month_profit")
                )
            }

            if (profit.missingPurchaseCostLineCount > 0) {
                Text(
                    text = stringResource(
                        R.string.owner_desk_missing_profit_costs,
                        profit.missingPurchaseCostLineCount
                    ),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.owner_desk_category_stock_value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack
            )

            if (stockValue.categoryValues.isEmpty()) {
                Text(
                    text = stringResource(R.string.owner_desk_no_category_values),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray
                )
            } else {
                stockValue.categoryValues.forEach { category ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderStrong),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.categoryName,
                                    fontWeight = FontWeight.Black,
                                    color = TextNearBlack
                                )
                                Text(
                                    text = stringResource(
                                        R.string.owner_desk_tracked_items,
                                        category.trackedProductCount
                                    ),
                                    fontSize = 12.sp,
                                    color = TextMediumGray
                                )
                            }
                            Text(
                                text = formatPaise(category.sellingValuePaise),
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OwnerMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.3.dp, BorderStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray
                )
            }
            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
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

private fun formatPaise(paise: Long): String {
    return CurrencyUtils.formatRupees(paise / 100.0)
}
