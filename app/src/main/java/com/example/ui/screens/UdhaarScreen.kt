package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.Dialog
import com.example.data.Customer
import com.example.ui.theme.*
import com.example.utils.AppLanguage
import com.example.utils.CurrencyUtils
import com.example.utils.DateTimeUtils
import com.example.utils.LocaleHelper
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel

// ==========================================
// 1. UDHAAR LEDGER MASTER SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.allUdhaarTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val customerBalances = remember(transactions) {
        transactions.groupBy { it.customerId }.mapValues { (_, txList) ->
            txList.sumOf { tx ->
                when (tx.type) {
                    "CREDIT" -> tx.amount
                    "PAYMENT" -> -tx.amount
                    else -> 0.0
                }
            }
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { cust ->
            cust.name.contains(searchQuery, ignoreCase = true) ||
                    (cust.phone != null && cust.phone.contains(searchQuery))
        }
    }

    var filterDebtorsOnly by remember { mutableStateOf(false) }
    val displayCustomers = remember(filteredCustomers, customerBalances, filterDebtorsOnly) {
        if (filterDebtorsOnly) {
            filteredCustomers.filter { (customerBalances[it.id] ?: 0.0) > 0.01 }
        } else {
            filteredCustomers
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.udhaarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportUdhaarCsv(context, customers, customerBalances) },
                        modifier = Modifier.testTag("export_udhaar_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Udhaar CSV", tint = SaffronPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val debtorsOnlyLabel = if (settings.appLanguage == AppLanguage.HINDI) "बकाया वाले" else "Debtors Only"
                        Text(debtorsOnlyLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Checkbox(
                            checked = filterDebtorsOnly,
                            onCheckedChange = { filterDebtorsOnly = it },
                            modifier = Modifier.testTag("debtors_only_checkbox")
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = ErrorRed,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = strings.addCustomer, tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WarmCreamBg)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchCustomer, color = TextMutedGray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SaffronPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SaffronPrimary,
                    unfocusedBorderColor = BorderStrong,
                    focusedTextColor = TextNearBlack,
                    unfocusedTextColor = TextNearBlack
                )
            )

            // Balance Summary Outstanding
            val grandOutstanding = remember(customerBalances) {
                customerBalances.values.filter { it > 0 }.sumOf { it }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, ErrorRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.totalMarketCredit, fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                        Text(
                            text = CurrencyUtils.formatRupees(grandOutstanding),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (displayCustomers.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ImportContacts, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            val noCustMsg = if (settings.appLanguage == AppLanguage.HINDI) "कोई ग्राहक नहीं मिला!" else "No customer found!"
                            Text(noCustMsg, color = Color.Gray)
                        }
                    }
                } else {
                    items(displayCustomers) { cust ->
                        val balance = customerBalances[cust.id] ?: 0.0

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, BorderStrong),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(Screen.CustomerDetail(cust.id))
                                }
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
                                        text = cust.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = TextNearBlack
                                    )
                                    if (!cust.phone.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cust.phone,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMediumGray
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = strings.balanceDue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextMediumGray
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupees(balance),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (balance > 0.01) ErrorRed else SuccessGreen
                                        )
                                    }

                                    if (balance > 0.01) {
                                        IconButton(
                                            onClick = { viewModel.sendUdhaarReminder(context, cust, balance) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = strings.sendWhatsAppReminder,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(20.dp)
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

        // --- ADD CUSTOMER POPUP DIALOG ---
        if (showAddCustomerDialog) {
            Dialog(onDismissRequest = { showAddCustomerDialog = false }) {
                var name by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            strings.addCustomer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(strings.customerName) },
                            modifier = Modifier.fillMaxWidth().testTag("add_customer_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErrorRed,
                                unfocusedBorderColor = BorderStrong,
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack,
                                focusedLabelColor = ErrorRed,
                                unfocusedLabelColor = TextMediumGray
                            )
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(strings.customerPhone) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("add_customer_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErrorRed,
                                unfocusedBorderColor = BorderStrong,
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack,
                                focusedLabelColor = ErrorRed,
                                unfocusedLabelColor = TextMediumGray
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showAddCustomerDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.cancel)
                            }

                            val openLedgerText = if (settings.appLanguage == AppLanguage.HINDI) "खाता खोलें" else "Save Customer"
                            Button(
                                onClick = {
                                    if (name.trim().isEmpty()) {
                                        val reqName = if (settings.appLanguage == AppLanguage.HINDI) "ग्राहक का नाम आवश्यक है!" else "Customer name required!"
                                        Toast.makeText(context, reqName, Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.quickAddCustomer(name, phone)
                                        showAddCustomerDialog = false
                                        val addedMsg = if (settings.appLanguage == AppLanguage.HINDI) "${name.trim()} खाता खुल गया!" else "${name.trim()} added!"
                                        Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("confirm_add_customer_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                            ) {
                                Text(openLedgerText)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. DETAILED LEAF LEDGER & DEPOSIT LOG BOOK
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(viewModel: ShopViewModel, customerId: Long) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    var customer by remember { mutableStateOf<Customer?>(null) }
    var currentBalance by remember { mutableStateOf(0.0) }

    var showReceivePaymentDialog by remember { mutableStateOf(false) }

    val customerTransactions = viewModel.getTransactionsForCustomer(customerId).collectAsState(initial = emptyList())

    LaunchedEffect(customerId, customerTransactions.value) {
        customer = viewModel.customers.value.find { it.id == customerId }
        currentBalance = customerTransactions.value.sumOf { tx ->
            when (tx.type) {
                "CREDIT" -> tx.amount
                "PAYMENT" -> -tx.amount
                else -> 0.0
            }
        }
    }

    LaunchedEffect(customerId) {
        val cust = viewModel.customers.value.find { it.id == customerId }
        customer = cust
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: strings.customerLedger, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Udhaar) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showReceivePaymentDialog = true },
                containerColor = SuccessGreen,
                text = { Text(strings.receivePayment, color = Color.White, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = strings.receivePayment, tint = Color.White) },
                modifier = Modifier.testTag("fab_receive_payment")
            )
        }
    ) { innerPadding ->
        customer?.let { cust ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(WarmCreamBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, ErrorRed),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.balanceDue,
                            fontSize = 13.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupees(currentBalance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )
                        if (!cust.phone.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val phoneLabel = if (settings.appLanguage == AppLanguage.HINDI) "मोबाइल:" else "Phone:"
                            Text(
                                text = "$phoneLabel ${cust.phone}",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }

                        if (currentBalance > 0.01) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.sendUdhaarReminder(context, cust, currentBalance) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("customer_whatsapp_reminder_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.sendWhatsAppReminder, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                val ledgerHistoryTitle = if (settings.appLanguage == AppLanguage.HINDI) "लेन-देन इतिहास:" else "Transaction Ledger History:"
                Text(
                    text = ledgerHistoryTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customerTransactions.value.isEmpty()) {
                        item {
                            val noTxMsg = if (settings.appLanguage == AppLanguage.HINDI) "इस खाते में कोई लेन-देन इतिहास नहीं है।" else "No transactions in this account yet."
                            Text(
                                noTxMsg,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(customerTransactions.value) { record ->
                            val isCredit = record.type == "CREDIT"
                            val isPayment = record.type == "PAYMENT"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.2.dp, BorderStrong)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    when {
                                                        isCredit -> Color(0xFFFFF1F2)
                                                        isPayment -> Color(0xFFF0FDF4)
                                                        else -> Color(0xFFFFF8E1)
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isCredit -> Icons.Default.ArrowOutward
                                                    isPayment -> Icons.Default.CallReceived
                                                    else -> Icons.Default.Info
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    isCredit -> ErrorRed
                                                    isPayment -> SuccessGreen
                                                    else -> WarningOrange
                                                }
                                            )
                                        }

                                        Column {
                                            val txTypeLabel = when {
                                                isCredit -> if (settings.appLanguage == AppLanguage.HINDI) "उधार दिया" else "Credit Given"
                                                isPayment -> if (settings.appLanguage == AppLanguage.HINDI) "रकम प्राप्त हुई" else "Payment Received"
                                                else -> if (settings.appLanguage == AppLanguage.HINDI) "अमान्य प्रविष्टि" else "Invalid ledger entry"
                                            }
                                            Text(
                                                text = txTypeLabel,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextNearBlack,
                                                fontSize = 14.sp
                                            )
                                            if (!record.note.isNullOrEmpty()) {
                                                Text(
                                                    text = record.note,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMutedGray
                                                )
                                            }
                                            Text(
                                                text = DateTimeUtils.formatDateTime(record.createdAt),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMediumGray
                                            )
                                        }
                                    }

                                    Text(
                                        text = when {
                                            isCredit -> "+${CurrencyUtils.formatRupees(record.amount)}"
                                            isPayment -> "-${CurrencyUtils.formatRupees(record.amount)}"
                                            else -> CurrencyUtils.formatRupees(record.amount)
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = when {
                                            isCredit -> ErrorRed
                                            isPayment -> SuccessGreen
                                            else -> WarningOrange
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DEPOSIT RECEIVED INPUT DIALOG PANEL ---
        if (showReceivePaymentDialog) {
            Dialog(onDismissRequest = { showReceivePaymentDialog = false }) {
                var amount by remember { mutableStateOf("") }
                var note by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val depositTitle = if (settings.appLanguage == AppLanguage.HINDI) "जमा राशि दर्ज करें" else "Receive Payment"
                        Text(
                            depositTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )

                        val amountLabel = if (settings.appLanguage == AppLanguage.HINDI) "प्राप्त रकम *" else "Received Amount *"
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(amountLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = BorderStrong,
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack,
                                focusedLabelColor = SuccessGreen,
                                unfocusedLabelColor = TextMediumGray
                            )
                        )

                        val noteLabel = if (settings.appLanguage == AppLanguage.HINDI) "टिप्पणी / माध्यम (वैकल्पिक)" else "Note / Mode (Optional)"
                        val notePlaceholder = if (settings.appLanguage == AppLanguage.HINDI) "उदा. नकद, UPI, Paytm..." else "e.g. Cash, Paytm, PhonePe..."
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(noteLabel) },
                            placeholder = { Text(notePlaceholder, color = TextMutedGray) },
                            modifier = Modifier.fillMaxWidth().testTag("payment_note_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = BorderStrong,
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack,
                                focusedLabelColor = SuccessGreen,
                                unfocusedLabelColor = TextMediumGray
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showReceivePaymentDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.cancel)
                            }

                            val confirmDepositText = if (settings.appLanguage == AppLanguage.HINDI) "जमा सुरक्षित करें" else "Save Payment"
                            Button(
                                onClick = {
                                    val amtValue = amount.trim().toDoubleOrNull()
                                    if (amtValue == null || amtValue <= 0.0) {
                                        val validAmtMsg = if (settings.appLanguage == AppLanguage.HINDI) "वैध रकम आवश्यक है!" else "Valid positive amount required!"
                                        Toast.makeText(context, validAmtMsg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addUdhaarPayment(
                                            customerId = customerId,
                                            amount = amtValue,
                                            note = note
                                        )
                                        showReceivePaymentDialog = false
                                        val successMsg = if (settings.appLanguage == AppLanguage.HINDI) "जमा रकम दर्ज हो गई!" else "Payment logged successfully!"
                                        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("confirm_payment_button")
                            ) {
                                Text(confirmDepositText)
                            }
                        }
                    }
                }
            }
        }
    }
}
