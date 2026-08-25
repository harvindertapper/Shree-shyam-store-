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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDate
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDateRange
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportInterval
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportPolicy
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeError
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeResult
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.DateTimeUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.SalesExportResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ReportsViewModel
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.Screen
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ShopViewModel, reportsViewModel: ReportsViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val sales by reportsViewModel.salesHistory.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var selectedViewSale by remember { mutableStateOf<Sale?>(null) }
    var selectedIntervalName by rememberSaveable { mutableStateOf(ReportInterval.TODAY.name) }
    var customStartPickerMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEndPickerMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var datePickerTarget by remember { mutableStateOf<ReportDatePickerTarget?>(null) }
    var exportResult by remember { mutableStateOf<SalesExportResult?>(null) }
    val selectedInterval = remember(selectedIntervalName) {
        runCatching { ReportInterval.valueOf(selectedIntervalName) }
            .getOrDefault(ReportInterval.TODAY)
    }

    // Keep time-based report windows fresh while this screen remains visible.
    var clockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            clockTick = System.currentTimeMillis()
        }
    }

    val reportTimeZone = remember { TimeZone.getDefault() }
    val customStartDate = customStartPickerMillis?.let(ReportDate::fromDatePickerMillis)
    val customEndDate = customEndPickerMillis?.let(ReportDate::fromDatePickerMillis)
    val rangeResult = remember(
        selectedInterval,
        clockTick,
        customStartPickerMillis,
        customEndPickerMillis,
        reportTimeZone
    ) {
        ReportPolicy.resolveRange(
            interval = selectedInterval,
            nowMillis = clockTick,
            timeZone = reportTimeZone,
            customStart = customStartDate,
            customEnd = customEndDate
        )
    }
    val validRange = (rangeResult as? ReportRangeResult.Valid)?.range
    val eligibleSales = remember(sales) {
        ReportPolicy.filterSales(sales, ReportDateRange(startInclusiveMillis = null, endExclusiveMillis = null))
    }
    val filteredSales = remember(eligibleSales, validRange) {
        validRange?.let { ReportPolicy.filterSales(eligibleSales, it) } ?: emptyList()
    }
    val reportSummary = remember(filteredSales) { ReportPolicy.summarize(filteredSales) }
    val isSalesHistoryLoading by reportsViewModel.isSalesHistoryLoading.collectAsState()
    val salesHistoryHasError by reportsViewModel.salesHistoryHasError.collectAsState()
    val exportMessage = exportResult?.let { result ->
        when (result) {
            SalesExportResult.SHARED -> strings.reportsExportReady
            SalesExportResult.NO_SALES -> strings.reportsExportEmpty
            SalesExportResult.FAILED -> strings.reportsExportFailed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.reportsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.commonBack)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            reportsViewModel.exportSalesCsv(context, filteredSales) { result ->
                                exportResult = result
                            }
                        },
                        enabled = !isSalesHistoryLoading && !salesHistoryHasError && validRange != null,
                        modifier = Modifier.testTag("export_sales_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = strings.commonExportSales, tint = SaffronPrimary)
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
            ReportIntervalTabs(
                strings = strings,
                selectedInterval = selectedInterval,
                onSelected = { interval ->
                    selectedIntervalName = interval.name
                    exportResult = null
                    if (interval == ReportInterval.CUSTOM) {
                        datePickerTarget = ReportDatePickerTarget.START
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedInterval == ReportInterval.CUSTOM) {
                CustomReportRangeSelector(
                    strings = strings,
                    startDate = customStartDate,
                    endDate = customEndDate,
                    rangeResult = rangeResult,
                    onSelectStart = { datePickerTarget = ReportDatePickerTarget.START },
                    onSelectEnd = { datePickerTarget = ReportDatePickerTarget.END },
                    onClear = {
                        customStartPickerMillis = null
                        customEndPickerMillis = null
                        exportResult = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("report_custom_range_selector")
                )
            }

            exportMessage?.let { message ->
                Text(
                    text = message,
                    color = if (message == strings.reportsExportFailed) ErrorRed else SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("report_export_message")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    isSalesHistoryLoading -> item {
                        ReportLoadingState(strings = strings, modifier = Modifier.testTag("report_loading_state"))
                    }
                    salesHistoryHasError -> item {
                        ReportErrorState(
                            strings = strings,
                            onRetry = reportsViewModel::refreshSalesHistory,
                            modifier = Modifier.testTag("report_error_state")
                        )
                    }
                    rangeResult is ReportRangeResult.Invalid -> item {
                        ReportEmptyState(
                            title = strings.reportsInvalidCustomRange,
                            detail = strings.reportsInvalidCustomRange,
                            modifier = Modifier.testTag("report_invalid_range_state")
                        )
                    }
                    else -> {
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
                                val salesVolumeTitle = strings.reportsTotalSalesTitle
                                Text(salesVolumeTitle, fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyUtils.formatRupees(reportSummary.totalRevenuePaise),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SaffronDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val billCountMsg = strings.reportsBillsGenerated(reportSummary.billsCount)
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
                                        CurrencyUtils.formatRupees(reportSummary.cashRevenuePaise),
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
                                        CurrencyUtils.formatRupees(reportSummary.upiRevenuePaise),
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
                                        CurrencyUtils.formatRupees(reportSummary.udhaarRevenuePaise),
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
                            cashAmount = reportSummary.cashRevenuePaise,
                            upiAmount = reportSummary.upiRevenuePaise,
                            udhaarAmount = reportSummary.udhaarRevenuePaise,
                            totalAmount = reportSummary.totalRevenuePaise
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WeeklySalesBarChart(
                            viewModel = viewModel,
                            salesHistory = eligibleSales
                        )
                    }
                }

                // Header lists title
                item {
                    val salesHistoryTitle = strings.reportsHistoryTitle
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
                        ReportEmptyState(
                            title = strings.reportsNoSalesInRange,
                            detail = strings.reportsNoRecords,
                            modifier = Modifier.testTag("report_empty_state")
                        )
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
                                        val custName = customers.find { it.id == sale.customerId }?.name ?: strings.commonCustomer
                                        val udhaarCustLabel = strings.reportsUdhaarCustomer
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
            }
        }

        datePickerTarget?.let { target ->
            val initialDate = when (target) {
                ReportDatePickerTarget.START -> customStartDate
                ReportDatePickerTarget.END -> customEndDate
            }
            ReportDatePickerDialog(
                strings = strings,
                target = target,
                initialSelectedDateMillis = initialDate?.toDatePickerMillis(),
                onDismiss = { datePickerTarget = null },
                onDateSelected = { selectedDateMillis ->
                    when (target) {
                        ReportDatePickerTarget.START -> customStartPickerMillis = selectedDateMillis
                        ReportDatePickerTarget.END -> customEndPickerMillis = selectedDateMillis
                    }
                    exportResult = null
                    datePickerTarget = null
                }
            )
        }

        // --- DETAILED INVOICE MODAL DIALOG ---
        selectedViewSale?.let { sale ->
            val saleItems = reportsViewModel.getSaleItems(sale.id).collectAsState(initial = emptyList())
            val custName = customers.find { it.id == sale.customerId }?.name ?: strings.commonCustomer

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
                            val billNoLabel = strings.reportsBillNumber
                            Text("$billNoLabel ${sale.billNumber}", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 12.sp)
                            Text(DateTimeUtils.formatDateOnly(sale.createdAt), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                        }

                        if (sale.paymentMode == "UDHAAR") {
                            val clientLabel = strings.reportsCustomer
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
                            val modeLabel = strings.reportsPaymentMode
                            Text(modeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                            val modeVal = when (sale.paymentMode) {
                                "UPI" -> "UPI"
                                "UDHAAR" -> strings.udhaar
                                else -> strings.cash
                            }
                            Text(modeVal, fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextNearBlack)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.reportsPaymentState, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMediumGray)
                            Text(
                                reportPaymentStateLabel(sale.paymentState, strings),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextNearBlack
                            )
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
                                    Text(strings.commonWhatsApp, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                    val copyInvoiceText = strings.reportsCopy
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
                            val closeText = strings.reportsClose
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
                val noDataMsg = strings.reportsNoChartData
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
            val chartTitle = strings.reportsPaymentModeSplit
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
                        val totalSalesLabel = strings.reportsTotal
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
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

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
            val weeklyTitle = strings.reportsWeeklyTrend
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

@Composable
internal fun ReportIntervalTabs(
    strings: AppStrings,
    selectedInterval: ReportInterval,
    onSelected: (ReportInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    // ScrollableTabRow keeps long Hindi labels readable.
    ScrollableTabRow(
        selectedTabIndex = selectedInterval.ordinal,
        modifier = modifier
    ) {
        ReportInterval.entries.forEach { interval ->
            val label = when (interval) {
                ReportInterval.TODAY -> strings.today
                ReportInterval.THIS_WEEK -> strings.thisWeek
                ReportInterval.THIS_MONTH -> strings.thisMonth
                ReportInterval.ALL_TIME -> strings.allTime
                ReportInterval.CUSTOM -> strings.customRange
            }
            Tab(
                selected = selectedInterval == interval,
                onClick = { onSelected(interval) },
                text = { Text(label, fontSize = 14.sp) },
                modifier = Modifier.testTag("report_tab_${interval.name.lowercase(Locale.ENGLISH)}")
            )
        }
    }
}

private enum class ReportDatePickerTarget {
    START,
    END
}

@Composable
internal fun ReportLoadingState(
    strings: com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = SaffronPrimary)
        Text(
            text = strings.reportsLoading,
            color = TextMediumGray,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ReportErrorState(
    strings: com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(44.dp))
        Text(
            text = strings.reportsLoadError,
            color = TextNearBlack,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            modifier = Modifier.testTag("report_retry_button")
        ) {
            Text(strings.reportsRetry, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ReportEmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = BorderStrong)
        Text(
            text = title,
            color = TextNearBlack,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = detail,
            color = TextMediumGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun CustomReportRangeSelector(
    strings: com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings,
    startDate: ReportDate?,
    endDate: ReportDate?,
    rangeResult: ReportRangeResult,
    onSelectStart: () -> Unit,
    onSelectEnd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSelectStart,
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_select_start_date")
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(strings.reportsStartDate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = startDate?.displayValue() ?: strings.reportsSelectStartDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            OutlinedButton(
                onClick = onSelectEnd,
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_select_end_date")
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(strings.reportsEndDate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = endDate?.displayValue() ?: strings.reportsSelectEndDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onClear,
                enabled = startDate != null || endDate != null,
                modifier = Modifier.testTag("report_clear_custom_range")
            ) {
                Text(strings.reportsClearCustomRange, fontWeight = FontWeight.Bold)
            }
        }

        if (startDate != null && endDate != null && rangeResult is ReportRangeResult.Valid) {
            Text(
                text = strings.reportsRangeSummary(startDate.displayValue(), endDate.displayValue()),
                color = TextMediumGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("report_custom_range_summary")
            )
        }

        val rangeError = (rangeResult as? ReportRangeResult.Invalid)?.error
        if (rangeError != null &&
            rangeError != ReportRangeError.START_DATE_REQUIRED &&
            rangeError != ReportRangeError.END_DATE_REQUIRED
        ) {
            Text(
                text = strings.reportsInvalidCustomRange,
                color = ErrorRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("report_custom_range_error")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDatePickerDialog(
    strings: com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings,
    target: ReportDatePickerTarget,
    initialSelectedDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { datePickerState.selectedDateMillis?.let(onDateSelected) },
                modifier = Modifier.testTag("report_confirm_date")
            ) {
                Text(strings.reportsApplyCustomRange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("report_cancel_date")) {
                Text(strings.commonClose, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column {
            Text(
                text = if (target == ReportDatePickerTarget.START) {
                    strings.reportsSelectStartDate
                } else {
                    strings.reportsSelectEndDate
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier.testTag("report_date_picker")
            )
        }
    }
}

private fun reportPaymentStateLabel(
    paymentState: String,
    strings: com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
): String = when (runCatching { PaymentState.fromWireValue(paymentState) }.getOrNull()) {
    PaymentState.NOT_REQUIRED -> strings.reportsPaymentNotRequired
    PaymentState.PENDING -> strings.reportsPaymentPending
    PaymentState.RECEIVED -> strings.reportsPaymentReceived
    PaymentState.FAILED -> strings.reportsPaymentFailed
    PaymentState.PARTIALLY_REFUNDED -> strings.reportsPaymentPartiallyRefunded
    PaymentState.REFUNDED -> strings.reportsPaymentRefunded
    null -> strings.reportsPaymentUnknown
}
