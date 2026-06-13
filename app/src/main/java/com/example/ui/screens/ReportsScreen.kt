package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Customer
import com.example.data.Sale
import com.example.data.SaleItem
import com.example.utils.CurrencyUtils
import com.example.utils.DateTimeUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ShopViewModel) {
    val sales by viewModel.salesHistory.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var selectedIntervalTab by remember { mutableStateOf(0) } // 0: Today, 1: This Month, 2: All Time
    var selectedViewSale by remember { mutableStateOf<Sale?>(null) }

    // Intervals calculations
    val todayStart = remember { DateTimeUtils.getStartOfDay() }
    val todayEnd = remember { DateTimeUtils.getEndOfDay() }
    val monthStart = remember { DateTimeUtils.getStartOfMonth() }

    // Intermediary filter collections
    val filteredSales = remember(sales, selectedIntervalTab) {
        when (selectedIntervalTab) {
            0 -> sales.filter { it.createdAt in todayStart..todayEnd }
            1 -> sales.filter { it.createdAt >= monthStart }
            else -> sales
        }
    }

    // Aggregates statistics
    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.totalAmount } }
    val cashRevenue = remember(filteredSales) { filteredSales.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } }
    val upiRevenue = remember(filteredSales) { filteredSales.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } }
    val udhaarRevenue = remember(filteredSales) { filteredSales.filter { it.paymentMode == "UDHAAR" }.sumOf { it.totalAmount } }
    val invoicesCount = remember(filteredSales) { filteredSales.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("दुकान रिपोर्ट (Business Reports) 📈", fontWeight = FontWeight.Bold) },
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
        ) {
            // Filter Interval tabs
            TabRow(
                selectedTabIndex = selectedIntervalTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedIntervalTab == 0,
                    onClick = { selectedIntervalTab = 0 },
                    text = { Text("आज (Today)", fontSize = 14.sp) },
                    modifier = Modifier.testTag("report_tab_today")
                )
                Tab(
                    selected = selectedIntervalTab == 1,
                    onClick = { selectedIntervalTab = 1 },
                    text = { Text("महीना (This Month)", fontSize = 14.sp) },
                    modifier = Modifier.testTag("report_tab_month")
                )
                Tab(
                    selected = selectedIntervalTab == 2,
                    onClick = { selectedIntervalTab = 2 },
                    text = { Text("सब (All Time)", fontSize = 14.sp) },
                    modifier = Modifier.testTag("report_tab_all")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Statistics Summary Card Group
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, SaffronPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("कुल बिक्री (Total Sales Volume)", fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyUtils.formatRupees(totalRevenue),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SaffronDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$invoicesCount bills generated", fontSize = 12.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Breakdown metrics cash upi udhaar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cash card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.2.dp, BorderStrong),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cash Paid", fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyUtils.formatRupees(cashRevenue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SuccessGreen
                                    )
                                }
                            }

                            // UPI Paid
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.2.dp, BorderStrong),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("UPI Paid", fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyUtils.formatRupees(upiRevenue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0E5A94)
                                    )
                                }
                            }

                            // Udhaar ledger balance
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.2.dp, BorderStrong),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Udhaar", fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyUtils.formatRupees(udhaarRevenue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Header lists title
                item {
                    Text(
                        text = "बिक्री का इतिहास (Sales History):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextMediumGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (filteredSales.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = BorderStrong)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("कोई रिकॉर्ड नहीं मिला!", color = TextMediumGray, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    items(filteredSales) { sale ->
                        val timeString = DateTimeUtils.formatDateTime(sale.createdAt)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, BorderStrong),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedViewSale = sale }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = sale.billNumber,
                                        fontWeight = FontWeight.Black,
                                        color = TextNearBlack
                                    )
                                    Text(
                                        text = timeString,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedGray
                                    )
                                    // Customer name if Udhaar
                                    if (sale.paymentMode == "UDHAAR") {
                                        val custName = customers.find { it.id == sale.customerId }?.name ?: "Customer"
                                        Text(
                                            text = "👤 Udhaar client: $custName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ErrorRed
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyUtils.formatRupees(sale.totalAmount),
                                        fontWeight = FontWeight.Black,
                                        color = TextNearBlack,
                                        fontSize = 16.sp
                                    )
                                    // Color tag for modes
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(
                                                color = when (sale.paymentMode) {
                                                    "UPI" -> Color(0xFFF0F9FF)
                                                    "UDHAAR" -> Color(0xFFFFF1F2)
                                                    else -> Color(0xFFF0FDF4)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sale.paymentMode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (sale.paymentMode) {
                                                "UPI" -> Color(0xFF0E5A94)
                                                "UDHAAR" -> ErrorRed
                                                else -> SuccessGreen
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DETAILED INVOICE MODAL DIALOG ---
        selectedViewSale?.let { sale ->
            val saleItems = viewModel.getSaleItems(sale.id).collectAsState(initial = emptyList())
            val custName = customers.find { it.id == sale.customerId }?.name ?: "Customer"

            Dialog(onDismissRequest = { selectedViewSale = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "🚩 SHREE SHYAM STORE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bill No: ${sale.billNumber}", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 12.sp)
                            Text(DateTimeUtils.formatDateOnly(sale.createdAt), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }

                        if (sale.paymentMode == "UDHAAR") {
                            Text("client: $custName", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ErrorRed)
                        }

                        Divider()

                        // Itemized summary inside Dialog
                        Box(modifier = Modifier.heightIn(max = 160.dp).fillMaxWidth()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(saleItems.value) { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${line.productNameSnapshot} x${line.quantity}",
                                            color = TextNearBlack,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1.5f)
                                        )
                                        Text(
                                            CurrencyUtils.formatRupees(line.lineTotal),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(0.5f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount:", fontWeight = FontWeight.Black, color = TextNearBlack)
                            Text(CurrencyUtils.formatRupees(sale.totalAmount), fontWeight = FontWeight.Black, color = SuccessGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                            Text(sale.paymentMode, fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextNearBlack)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { selectedViewSale = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close (बंद करें)")
                        }
                    }
                }
            }
        }
    }
}
