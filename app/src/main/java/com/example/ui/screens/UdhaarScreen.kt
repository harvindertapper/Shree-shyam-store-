package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Customer
import com.example.utils.CurrencyUtils
import com.example.utils.DateTimeUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.*

// ==========================================
// 1. UDHAAR LEDGER MASTER SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.allUdhaarTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Map outstanding customer balances reactively in memory!
    val customerBalances = remember(transactions) {
        transactions.groupBy { it.customerId }.mapValues { (_, txList) ->
            txList.sumOf { tx ->
                if (tx.type == "CREDIT") tx.amount else -tx.amount
            }
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { cust ->
            cust.name.contains(searchQuery, ignoreCase = true) ||
                    (cust.phone != null && cust.phone.contains(searchQuery))
        }
    }

    // Filter debtors with balance > 0
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
                title = { Text("उधार खाता (Debtor Ledger) 👥", fontWeight = FontWeight.Bold) },
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
                        Text("उधारी वाले", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Color.White)
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
                placeholder = { Text("ग्राहक का नाम/नंबर खोजें (Search customer)...", color = TextMutedGray) },
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
                        Text("कुल उधारी (Total Market Outstanding)", fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
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
                            Text("कोई ग्राहक नहीं मिला!", color = Color.Gray)
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
                                            text = "📱 ${cust.phone}",
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
                                            text = "Due Balance",
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
                                                contentDescription = "Send WhatsApp Reminder",
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

        // --- ADD CUSTOMER FROM POPUP DIALOG ---
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
                            "नया ग्राहक खाता खोलें 👤",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer Name * (नाम)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_customer_name_input"), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderStrong, focusedTextColor = TextNearBlack, unfocusedTextColor = TextNearBlack, focusedLabelColor = ErrorRed, unfocusedLabelColor = TextMediumGray)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (नंबर - optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("add_customer_phone_input"), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderStrong, focusedTextColor = TextNearBlack, unfocusedTextColor = TextNearBlack, focusedLabelColor = ErrorRed, unfocusedLabelColor = TextMediumGray)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showAddCustomerDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (name.trim().isEmpty()) {
                                        Toast.makeText(context, "ग्राहक का नाम आवश्यक है!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.quickAddCustomer(name, phone)
                                        showAddCustomerDialog = false
                                        Toast.makeText(context, "${name.trim()} added!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("confirm_add_customer_button"), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                            ) {
                                Text("खाता खोलें")
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
    var customer by remember { mutableStateOf<Customer?>(null) }
    var currentBalance by remember { mutableStateOf(0.0) }

    var showReceivePaymentDialog by remember { mutableStateOf(false) }

    val customerTransactions = viewModel.getTransactionsForCustomer(customerId).collectAsState(initial = emptyList())

    LaunchedEffect(customerId, customerTransactions.value) {
        customer = viewModel.customers.value.find { it.id == customerId }
        // Let's compute outstanding balance locally from transactions list directly!
        currentBalance = customerTransactions.value.sumOf { tx ->
            if (tx.type == "CREDIT") tx.amount else -tx.amount
        }
    }

    // Workaround helper to get customer sync safely inside Compose launch (the viewmodel has repository query)
    LaunchedEffect(customerId) {
        val cust = viewModel.customers.value.find { it.id == customerId }
        customer = cust
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "खाता विवरण (Ledger)", fontWeight = FontWeight.Bold) },
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
                text = { Text("रकम जमा करें 💸", color = Color.White, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Receive payment", tint = Color.White) },
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
                // Large outstanding highlighted red balance card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, ErrorRed),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "कुल बचा हुआ उधार (Outstanding Due)",
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
                            Text(
                                text = "📱 Mobile: ${cust.phone}",
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
                                    Text("WhatsApp तकादा / Reminder भेजें 💬", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "लेन-देन इतिहास (Transaction Ledger History):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                // Transactions history lists
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customerTransactions.value.isEmpty()) {
                        item {
                            Text(
                                "इस खाते में कोई लेन-देन इतिहास नहीं है।",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(customerTransactions.value) { record ->
                            val isCredit = record.type == "CREDIT"

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
                                                    if (isCredit) Color(0xFFFFF1F2) else Color(0xFFF0FDF4),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCredit) Icons.Default.ArrowOutward else Icons.Default.CallReceived,
                                                contentDescription = null,
                                                tint = if (isCredit) ErrorRed else SuccessGreen
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = if (isCredit) "उधार दिया (Goods Purchased)" else "रकम मिली (Cash Received)",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextNearBlack,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = record.note ?: "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMutedGray
                                            )
                                            Text(
                                                text = DateTimeUtils.formatDateTime(record.createdAt),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMediumGray
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${if (isCredit) "+" else "-"}${CurrencyUtils.formatRupees(record.amount)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = if (isCredit) ErrorRed else SuccessGreen
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
                        Text(
                            "जमा राशि दर्ज करें (Deposit Money) 💸",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Payment Received Amount * (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessGreen, unfocusedBorderColor = BorderStrong, focusedTextColor = TextNearBlack, unfocusedTextColor = TextNearBlack, focusedLabelColor = SuccessGreen, unfocusedLabelColor = TextMediumGray)
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note / Remark - (Optional)") },
                            placeholder = { Text("e.g. Cash, Paytm, PhonePe...", color = TextMutedGray) },
                            modifier = Modifier.fillMaxWidth().testTag("payment_note_input"), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessGreen, unfocusedBorderColor = BorderStrong, focusedTextColor = TextNearBlack, unfocusedTextColor = TextNearBlack, focusedLabelColor = SuccessGreen, unfocusedLabelColor = TextMediumGray)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showReceivePaymentDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val amtValue = amount.trim().toDoubleOrNull()
                                    if (amtValue == null || amtValue <= 0.0) {
                                        Toast.makeText(context, "Valid positive amount required!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addUdhaarPayment(
                                            customerId = customerId,
                                            amount = amtValue,
                                            note = note
                                        )
                                        showReceivePaymentDialog = false
                                        Toast.makeText(context, "Payment logged successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("confirm_payment_button")
                            ) {
                                Text("जमा दर्ज Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}
