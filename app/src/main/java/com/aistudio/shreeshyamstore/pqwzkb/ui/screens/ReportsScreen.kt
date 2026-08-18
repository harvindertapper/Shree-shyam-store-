package com.aistudio.shreeshyamstore.pqwzkb.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.DateTimeUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ReportsViewModel
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.Screen
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ShopViewModel, reportsViewModel: ReportsViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val sales by reportsViewModel.salesHistory.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var selectedIntervalTab by remember { mutableStateOf(0) } // 0: Today, 1: This Month, 2: All Time
    var selectedViewSale by remember { mutableStateOf<Sale?>(null) }

    // Keep time-based report windows fresh while this screen remains visible.
    var clockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            clockTick = System.currentTimeMillis()
        }
    }

    // Intervals calculations
    val todayStart = remember(clockTick) { DateTimeUtils.getStartOfDay() }
    val todayEnd = remember(clockTick) { DateTimeUtils.getEndOfDay() }
    val monthStart = remember(clockTick) { DateTimeUtils.getStartOfMonth() }

    // Intermediary filter collections
    val filteredSales = remember(sales, selectedIntervalTab, todayStart, todayEnd, monthStart) {
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
                title = { Text(strings.reportsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { reportsViewModel.exportSalesCsv(context, filteredSales) },
                        modifier = Modifier.testTag("export_sales_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Sales CSV", tint = SaffronPrimary)
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
                    text = { Text(strings.today, fontSize = 14.sp) },
                    modifier = Modifier.testTag("report_tab_today")
                )
                Tab(
                    selected = selectedIntervalTab == 1,
                    onClick = { selectedIntervalTab = 1 },
                    text = { Text(strings.thisMonth, fontSize = 14.sp) },
                    modifier = Modifier.testTag("report_tab_month")
                )
                Tab(
                    selected = selectedIntervalTab == 2,
                    onClick = { selectedIntervalTab = 2 },
                    text = { Text(strings.allTime, fontSize = 14.sp) },
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
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, SaffronPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val salesVolumeTitle = if (settings.appLanguage == AppLanguage.HINDI) "कुल बिक्री" else "Total Sales"
                                Text(salesVolumeTitle, fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyUtils.formatRupees(totalRevenue),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SaffronDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val billCountMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                                    "$invoicesCount बिल बनाए गए"
                                } else {
                                    "$invoicesCount bills generated"
                                }
                                Text(billCountMsg, fontSize = 12.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
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
                                    Text(strings.cash, fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
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
                                    Text(strings.upiPaytm, fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
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
                                    Text(strings.udhaar, fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyUtils.formatRupees(udhaarRevenue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PaymentDistributionDonutChart(
                            viewModel = viewModel,
                            cashAmount = cashRevenue,
                            upiAmount = upiRevenue,
                            udhaarAmount = udhaarRevenue,
                            totalAmount = totalRevenue
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WeeklySalesBarChart(
                            viewModel = viewModel,
                            salesHistory = sales
                        )
                    }
                }

                // Header lists title
                item {
                    val salesHistoryTitle = if (settings.appLanguage == AppLanguage.HINDI) "बिक्री का इतिहास:" else "Sales History:"
                    Text(
                        text = salesHistoryTitle,
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
                            val noRecordMsg = if (settings.appLanguage == AppLanguage.HINDI) "कोई रिकॉर्ड नहीं मिला!" else "No records found!"
                            Text(noRecordMsg, color = TextMediumGray, fontWeight = FontWeight.Bold)
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
                                        val udhaarCustLabel = if (settings.appLanguage == AppLanguage.HINDI) "उधार ग्राहक" else "Udhaar Client"
                                        Text(
                                            text = "$udhaarCustLabel: $custName",
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
                                    val modeDisplay = when (sale.paymentMode) {
                                        "UPI" -> "UPI"
                                        "UDHAAR" -> strings.udhaar
                                        else -> strings.cash
                                    }
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
                                            text = modeDisplay,
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
            val saleItems = reportsViewModel.getSaleItems(sale.id).collectAsState(initial = emptyList())
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
                            settings.shopName.ifEmpty { strings.defaultShopName },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val billNoLabel = if (settings.appLanguage == AppLanguage.HINDI) "बिल नं:" else "Bill No:"
                            Text("$billNoLabel ${sale.billNumber}", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 12.sp)
                            Text(DateTimeUtils.formatDateOnly(sale.createdAt), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }

                        if (sale.paymentMode == "UDHAAR") {
                            val clientLabel = if (settings.appLanguage == AppLanguage.HINDI) "ग्राहक:" else "Client:"
                            Text("$clientLabel $custName", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ErrorRed)
                        }

                        HorizontalDivider()

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

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.totalAmount, fontWeight = FontWeight.Black, color = TextNearBlack)
                            Text(CurrencyUtils.formatRupees(sale.totalAmount), fontWeight = FontWeight.Black, color = SuccessGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val modeLabel = if (settings.appLanguage == AppLanguage.HINDI) "भुगतान माध्यम:" else "Payment Mode:"
                            Text(modeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                            val modeVal = when (sale.paymentMode) {
                                "UPI" -> "UPI"
                                "UDHAAR" -> strings.udhaar
                                else -> strings.cash
                            }
                            Text(modeVal, fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextNearBlack)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val customerPhone = customers.find { it.id == sale.customerId }?.phone
                                    viewModel.shareInvoiceViaWhatsApp(
                                        context = context,
                                        customSale = sale,
                                        customItems = saleItems.value,
                                        phoneNumber = customerPhone
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.copyInvoiceToClipboard(
                                        context = context,
                                        customSale = sale,
                                        customItems = saleItems.value
                                    )
                                },
                                border = BorderStroke(1.2.dp, BorderStrong),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNearBlack),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, null, tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val copyInvoiceText = if (settings.appLanguage == AppLanguage.HINDI) "कॉपी" else "Copy"
                                    Text(copyInvoiceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = { selectedViewSale = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            val closeText = if (settings.appLanguage == AppLanguage.HINDI) "बंद करें" else "Close"
                            Text(closeText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDistributionDonutChart(
    viewModel: ShopViewModel,
    cashAmount: Long,
    upiAmount: Long,
    udhaarAmount: Long,
    totalAmount: Long
) {
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    if (totalAmount <= 0L) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.2.dp, BorderStrong),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val noDataMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                    "वितरण चार्ट के लिए कोई बिक्री डेटा उपलब्ध नहीं है।"
                } else {
                    "No sales data available for distribution chart."
                }
                Text(
                    text = noDataMsg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val cashPct = (cashAmount.toDouble() / totalAmount.toDouble()).toFloat()
    val upiPct = (upiAmount.toDouble() / totalAmount.toDouble()).toFloat()
    val udhaarPct = (udhaarAmount.toDouble() / totalAmount.toDouble()).toFloat()

    val cashColor = SuccessGreen
    val upiColor = Color(0xFF0E5A94)
    val udhaarColor = ErrorRed

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, BorderStrong),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val chartTitle = if (settings.appLanguage == AppLanguage.HINDI) "भुगतान माध्यम वितरण" else "Payment Mode Split"
            Text(
                text = chartTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // The Donut Canvas
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val strokeWidth = 36f
                        val sizeMin = size.minDimension
                        val adjustedSize = size.copy(width = sizeMin - strokeWidth, height = sizeMin - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        var startAngle = -90f

                        // UPI path
                        if (upiPct > 0f) {
                            val sweepAngle = upiPct * 360f
                            drawArc(
                                color = upiColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = adjustedSize,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }

                        // Cash path
                        if (cashPct > 0f) {
                            val sweepAngle = cashPct * 360f
                            drawArc(
                                color = cashColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = adjustedSize,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }

                        // Udhaar path
                        if (udhaarPct > 0f) {
                            val sweepAngle = udhaarPct * 360f
                            drawArc(
                                color = udhaarColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = adjustedSize,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Total text in center of donut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val totalSalesLabel = if (settings.appLanguage == AppLanguage.HINDI) "कुल बिक्री" else "Total"
                        Text(
                            text = totalSalesLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedGray
                        )
                        Text(
                            text = CurrencyUtils.formatRupees(totalAmount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    val formattedUpiPct = String.format("%.1f", upiPct * 100)
                    val formattedCashPct = String.format("%.1f", cashPct * 100)
                    val formattedUdhaarPct = String.format("%.1f", udhaarPct * 100)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(upiColor, shape = androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "UPI: $formattedUpiPct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNearBlack)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(cashColor, shape = androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${strings.cash}: $formattedCashPct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNearBlack)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(udhaarColor, shape = androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${strings.udhaar}: $formattedUdhaarPct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNearBlack)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklySalesBarChart(
    viewModel: ShopViewModel,
    salesHistory: List<Sale>
) {
    val settings by viewModel.storeSettings.collectAsState()

    if (salesHistory.isEmpty()) return

    // Get sales over the last 7 days
    val last7Days = remember(salesHistory) {
        val daysList = mutableListOf<String>()
        val dayTotals = mutableListOf<Long>()
        
        val format = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayStr = format.format(cal.time)
            daysList.add(dayStr)
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.set(java.util.Calendar.MILLISECOND, 999)
            val endMs = cal.timeInMillis
            
            val totalForDay = salesHistory.filter { it.createdAt in startMs..endMs }.sumOf { it.totalAmount }
            dayTotals.add(totalForDay)
        }
        Pair(daysList, dayTotals)
    }

    val days = last7Days.first
    val totals = last7Days.second
    val maxVal = remember(totals) { totals.maxOrNull()?.coerceAtLeast(1L) ?: 1L }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, BorderStrong),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val weeklyTitle = if (settings.appLanguage == AppLanguage.HINDI) "पिछले 7 दिनों की बिक्री" else "Weekly Sales Trend"
            Text(
                text = weeklyTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextNearBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                totals.forEachIndexed { index, total ->
                    val fraction = (total.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0.04f, 1f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (total > 0) {
                            Text(
                                text = run {
                                    val rupees = total.toDouble() / 100.0
                                    if (rupees >= 1000.0) String.format("%.1fk", rupees / 1000.0)
                                    else MoneyUtils.toInputString(total)
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = SaffronDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction * 0.7f)
                                .width(14.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(SaffronPrimary, SaffronLight)
                                    ),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = days[index],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedGray
                        )
                    }
                }
            }
        }
    }
}
