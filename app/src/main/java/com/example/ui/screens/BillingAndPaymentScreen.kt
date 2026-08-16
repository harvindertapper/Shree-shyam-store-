package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Category
import com.example.data.Customer
import com.example.data.Product
import com.example.utils.CurrencyUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.*

// ==========================================
// 1. BILLING SCREEN (ENTRY & CART PANEL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cart by viewModel.cartState.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    // Warning dialog regarding insufficient stock
    var showStockWarningProduct by remember { mutableStateOf<Product?>(null) }

    // Filter active products
    val activeProducts = remember(products) { products.filter { it.isActive } }
    
    val filteredProducts = remember(activeProducts, searchQuery, selectedCategoryId) {
        activeProducts.filter { prod ->
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("नया बिल (New Bill) 🛒", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showQuickAddDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Quick Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quick Add Product", fontWeight = FontWeight.Bold)
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
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("सामान का नाम खोजें (Search product name)...", color = TextMutedGray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SaffronPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = TextNearBlack)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("billing_search_input"),
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

            // Category Horizontal Scroll List
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("सभी (All)", fontWeight = FontWeight.Bold) }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Products Picker List & Basket Row side scroll
            Row(modifier = Modifier.weight(1f)) {
                // Product selection column
                LazyColumn(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "कोई प्रोडक्ट नहीं मिला! Quick Add से तुरंत जोड़ें।",
                                    color = TextMediumGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(filteredProducts) { product ->
                            val stockWarning = product.trackStock && product.currentStock <= 0
                            val lowStock = product.trackStock && product.currentStock <= product.lowStockAlertQty

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.2.dp, BorderStrong),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Stock checks
                                        val inCartQty = cart[product] ?: 0
                                        if (product.trackStock && product.currentStock <= inCartQty) {
                                            showStockWarningProduct = product
                                        } else {
                                            viewModel.addProductToCart(product, 1)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = TextNearBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = CurrencyUtils.formatRupees(product.getEffectivePrice()),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = SaffronPrimary
                                            )
                                            if (product.sellingPrice != null && product.sellingPrice != product.mrp) {
                                                Text(
                                                    text = "MRP ${CurrencyUtils.formatRupees(product.mrp)}",
                                                    fontSize = 11.sp,
                                                    color = TextMutedGray,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Stock Indicator badge
                                        if (product.trackStock) {
                                            if (product.currentStock <= 0) {
                                                Text(
                                                    "स्टॉक ख़त्म (Out of Stock)",
                                                    color = ErrorRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            } else if (product.currentStock <= product.lowStockAlertQty) {
                                                Text(
                                                    "कम स्टॉक: ${product.currentStock} बचा है",
                                                    color = WarningOrange,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            } else {
                                                Text(
                                                    "स्टॉक: ${product.currentStock} उपलब्ध",
                                                    color = SuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        } else {
                                            Text("स्टॉक अनट्रैक्ड", color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Add Indicator badge inside product list if in cart
                                    val qtyInCart = cart[product] ?: 0
                                    if (qtyInCart > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    SaffronPrimary,
                                                    shape = CircleShape
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "x$qtyInCart",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            val inCartQty = cart[product] ?: 0
                                            if (product.trackStock && product.currentStock <= inCartQty) {
                                                showStockWarningProduct = product
                                            } else {
                                                viewModel.addProductToCart(product, 1)
                                            }
                                        }) {
                                            Icon(Icons.Default.Add, "Add to Basket", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                        .background(BorderStrong)
                )

                // Cart/Basket Summary Side list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                ) {
                    if (cart.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ShoppingBasket, null, tint = BorderStrong, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "खाली थैला\n(Basket is empty)",
                                fontSize = 12.sp,
                                color = TextMutedGray,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SaffronLight)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "थैला (${cart.values.sum()} चीज़ें)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SaffronDark
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                items(cart.entries.toList()) { (product, quantity) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, BorderStrong),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = product.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = CurrencyUtils.formatRupees(product.getEffectivePrice() * quantity),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SaffronDark
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Minus button
                                                    IconButton(
                                                        onClick = { viewModel.addProductToCart(product, -1) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.RemoveCircleOutline,
                                                            "Remove",
                                                            tint = TextNearBlack,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "$quantity",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    // Plus button
                                                    IconButton(
                                                        onClick = {
                                                            if (product.trackStock && product.currentStock <= quantity) {
                                                                showStockWarningProduct = product
                                                            } else {
                                                                viewModel.addProductToCart(product, 1)
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.AddCircleOutline,
                                                            "Add",
                                                            tint = SaffronPrimary,
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
                }
            }

            // Checkout Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.5.dp, BorderStrong)
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("कुल बिल (Total)", fontSize = 12.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                        Text(
                            text = CurrencyUtils.formatRupees(cartTotal),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                    }

                    Button(
                        onClick = {
                            if (cart.isEmpty()) {
                                Toast.makeText(context, "थैला खाली है! सामान जोड़ें।", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.navigateTo(Screen.Payment(cartTotal))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 160.dp)
                            .testTag("checkout_payment_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("हिसाब करें 👍", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }

        // --- SUB DIALOGS ---

        // 1. Incomplete Stock alert confirmation
        showStockWarningProduct?.let { product ->
            AlertDialog(
                onDismissRequest = { showStockWarningProduct = null },
                title = { Text("स्टॉक कम है ⚠️") },
                text = {
                    Text("प्रोडक्ट '${product.name}' का स्टॉक केवल ${product.currentStock} बचा है। क्या आप इस बिक्री को फिर भी थैले में जोड़ना चाहते हैं?")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = {
                            viewModel.addProductToCart(product, 1)
                            showStockWarningProduct = null
                        }
                    ) {
                        Text("हाँ, जोड़ें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStockWarningProduct = null }) {
                        Text("नहीं")
                    }
                }
            )
        }

        // 2. QUICK ADD PRODUCT DIALOG
        if (showQuickAddDialog) {
            Dialog(onDismissRequest = { showQuickAddDialog = false }) {
                var newName by remember { mutableStateOf("") }
                var newMrp by remember { mutableStateOf("") }
                var selectedCatId by remember { mutableStateOf<Long>(categories.firstOrNull()?.id ?: 1L) }
                var trackStock by remember { mutableStateOf(true) }
                var initialStock by remember { mutableStateOf("10") }

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
                            "Fast Quick Add 📦",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Product Name (e.g. Marie Biscuits)") },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_product_name")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newMrp,
                                onValueChange = { newMrp = it },
                                label = { Text("MRP (Price)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_mrp")
                            )

                            OutlinedTextField(
                                value = initialStock,
                                onValueChange = { initialStock = it },
                                label = { Text("Stock Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_stock"),
                                enabled = trackStock
                            )
                        }

                        // Category Dropdown
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        var selectedCatName by remember(selectedCatId) {
                            mutableStateOf(categories.find { it.id == selectedCatId }?.name ?: "Miscellaneous")
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Category: $selectedCatName")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCatId = cat.id
                                            selectedCatName = cat.name
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Track Stock Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Track Stock (स्टॉक गिनती करें)", fontSize = 14.sp)
                            Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showQuickAddDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val mrpValue = newMrp.toDoubleOrNull()
                                    val stockValue = initialStock.toIntOrNull() ?: 0
                                    if (newName.trim().isEmpty() || mrpValue == null) {
                                        Toast.makeText(context, "Name and valid Price required!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.quickAddProduct(
                                            name = newName,
                                            mrp = mrpValue,
                                            categoryId = selectedCatId,
                                            trackStock = trackStock,
                                            currentStock = if (trackStock) stockValue else 0
                                        )
                                        showQuickAddDialog = false
                                        Toast.makeText(context, "${newName.trim()} Added!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save & Add to Bill")
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. PAYMENT & BILL SETTLEMENT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(viewModel: ShopViewModel, invoiceTotal: Double) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var showUdhaarCustomerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("भुगतान चुने (Payment) 💵", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Billing) }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, BorderStrong),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "कुल चुकाने योग्य राशि (Amount to pay)", fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatRupees(invoiceTotal),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreen
                    )
                }
            }

            // QR code block container
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, BorderStrong),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (settings.staticPaytmQrImageUri.isNotEmpty()) {
                        Text(
                            "PAYTM BUSINESS QR CODE 📲",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = SaffronPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(2.dp, SaffronPrimary, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = settings.staticPaytmQrImageUri,
                                contentDescription = "Static Paytm QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Customer से कहें: 'QR scan karke exact amount manually enter karein.'",
                            fontSize = 12.sp,
                            color = TextNearBlack,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Empty QR fallback visual
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BorderStrong, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCode2, "No QR Configured", tint = SaffronPrimary, modifier = Modifier.size(80.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "UPI QR is not configured!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = "Settings में जाकर दुकान का Paytm Business QR लगायें।",
                            fontSize = 12.sp,
                            color = TextMediumGray,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Action payment buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cash
                    Button(
                        onClick = {
                            viewModel.completeBill(paymentMode = "CASH")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("cash_pay_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payments, null, tint = Color.White)
                            Text("नकद मिला (Cash)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }

                    // UPI
                    Button(
                        onClick = {
                            viewModel.completeBill(paymentMode = "UPI")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5A94), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("upi_pay_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = Color.White)
                            Text("UPI Settle (Paid)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }

                // Udhaar
                Button(
                    onClick = { showUdhaarCustomerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("udhaar_pay_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Book, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("उधार खाता (Mark Udhaar Ledger)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }

        // --- CUSTOMER SELECTOR FOR UDHAAR DIALOG ---
        if (showUdhaarCustomerDialog) {
            Dialog(onDismissRequest = { showUdhaarCustomerDialog = false }) {
                var searchCustName by remember { mutableStateOf("") }
                var custPhone by remember { mutableStateOf("") }
                var showAddNewCustomerBlock by remember { mutableStateOf(false) }

                val filteredCustomers = remember(customers, searchCustName) {
                    customers.filter { it.name.contains(searchCustName, ignoreCase = true) }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "उधार ग्राहक चुनें 👥",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )

                        // Search box
                        OutlinedTextField(
                            value = searchCustName,
                            onValueChange = { searchCustName = it },
                            placeholder = { Text("Search or Type customer name...", color = TextMutedGray) },
                            modifier = Modifier.fillMaxWidth().testTag("udhaar_customer_search"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = BorderStrong,
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack
                            )
                        )

                        Box(modifier = Modifier.heightIn(max = 180.dp).fillMaxWidth()) {
                            if (filteredCustomers.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("कोई ग्राहक नहीं मिला।", color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { showAddNewCustomerBlock = true }) {
                                        Text("+ Add New: '$searchCustName'", color = SaffronPrimary, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filteredCustomers) { cust ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.completeBill(
                                                        paymentMode = "UDHAAR",
                                                        customerId = cust.id
                                                    )
                                                    showUdhaarCustomerDialog = false
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.2.dp, BorderStrong),
                                            color = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(cust.name, fontWeight = FontWeight.ExtraBold, color = TextNearBlack)
                                                    if (!cust.phone.isNullOrEmpty()) {
                                                        Text(cust.phone, fontSize = 11.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Icon(Icons.Default.ChevronRight, null, tint = SaffronPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showAddNewCustomerBlock || filteredCustomers.isEmpty()) {
                            HorizontalDivider(color = BorderStrong, thickness = 1.2.dp)
                            Text("New Customer (नया ग्राहक जोड़ें):", fontWeight = FontWeight.Black, fontSize = 12.sp, color = TextNearBlack)
                            
                            OutlinedTextField(
                                value = custPhone,
                                onValueChange = { custPhone = it },
                                label = { Text("Phone Number (Optional)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("new_customer_phone_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaffronPrimary,
                                    unfocusedBorderColor = BorderStrong,
                                    focusedTextColor = TextNearBlack,
                                    unfocusedTextColor = TextNearBlack,
                                    focusedLabelColor = SaffronPrimary,
                                    unfocusedLabelColor = TextMediumGray
                                )
                            )

                            Button(
                                onClick = {
                                    if (searchCustName.trim().isEmpty()) {
                                        Toast.makeText(context, "ग्राहक का नाम ज़रूरी है!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.completeBill(
                                            paymentMode = "UDHAAR",
                                            customerId = null, // create dynamic
                                            customerName = searchCustName,
                                            customerPhone = custPhone
                                        )
                                        showUdhaarCustomerDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("खाता खोलें और उधार लिखें Confirm", fontWeight = FontWeight.Black)
                            }
                        }

                        TextButton(
                            onClick = { showUdhaarCustomerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = TextMediumGray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 3. INVOICE SUCCESS & RECEIPT SHARING SCREEN
// ==========================================
@Composable
fun BillSuccessScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val lastSale by viewModel.lastSale.collectAsState()
    val lastItems by viewModel.lastSaleItems.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCreamBg), // Uniform warm cream background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.5.dp, BorderStrong, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = SuccessGreen,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "बिल सुरक्षित हो गया! 🎉",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = SuccessGreen
            )

            lastSale?.let { sale ->
                Text(
                    text = "Bill No: ${sale.billNumber}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMediumGray
                )

                Text(
                    text = "कुल राशि: ${CurrencyUtils.formatRupees(sale.totalAmount)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                Text(
                    text = "Payment Mode: ${sale.paymentMode}",
                    fontWeight = FontWeight.Black,
                    color = when (sale.paymentMode) {
                        "UPI" -> Color(0xFF0E5A94)
                        "UDHAAR" -> ErrorRed
                        else -> SuccessGreen
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // WhatsApp Share button
            Button(
                onClick = { viewModel.shareInvoiceViaWhatsApp(context) },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("share_whatsapp_bill_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp पर बिल भेजें 📱", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }

            // Clipboard button
            OutlinedButton(
                onClick = { viewModel.copyInvoiceToClipboard(context) },
                border = BorderStroke(1.5.dp, BorderStrong),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNearBlack),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("copy_invoice_text_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, null, tint = SaffronPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("बिल कॉपी करें (Copy Invoice Ticket)", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.Reports) },
                    border = BorderStroke(1.5.dp, BorderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNearBlack),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("इतिहास (History)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.Billing) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("new_bill_confirm")
                ) {
                    Text("नया बिल (New Bill)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
