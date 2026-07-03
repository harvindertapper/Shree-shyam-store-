package com.harrylabs.shreeshyamstore.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.data.Category
import com.harrylabs.shreeshyamstore.data.Customer
import com.harrylabs.shreeshyamstore.data.DataUnitType
import com.harrylabs.shreeshyamstore.data.Product
import com.harrylabs.shreeshyamstore.utils.CalculationResult
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import com.harrylabs.shreeshyamstore.utils.QuantityDisplayUnit
import com.harrylabs.shreeshyamstore.utils.QuantityPriceCalculator
import com.harrylabs.shreeshyamstore.utils.ProductUnitType
import com.harrylabs.shreeshyamstore.utils.UnitRate
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel
import com.harrylabs.shreeshyamstore.ui.theme.*

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
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showLooseQuantityDialog by remember { mutableStateOf<Product?>(null) }

    // Warning dialog regarding insufficient stock
    var showStockWarningProduct by remember { mutableStateOf<Product?>(null) }
    val cartEmptyToast = stringResource(R.string.billing_cart_empty_toast)
    val quickAddValidationToast = stringResource(R.string.quick_add_validation_toast)

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
                title = { Text(stringResource(R.string.billing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showQuickAddDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.billing_quick_add))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.billing_quick_add_product), fontWeight = FontWeight.Bold)
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
                placeholder = { Text(stringResource(R.string.billing_search_placeholder), color = TextMutedGray) },
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
                        label = { Text(stringResource(R.string.category_all), fontWeight = FontWeight.Bold) }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.localUuid,
                        onClick = { selectedCategoryId = cat.localUuid },
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
                                    stringResource(R.string.billing_no_products),
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
                                        if (product.unitType != DataUnitType.PIECE) {
                                            showLooseQuantityDialog = product
                                        } else {
                                            // Stock checks
                                            val inCartQty = cart[product]?.quantity ?: 0
                                            if (product.trackStock && product.currentStock <= inCartQty) {
                                                showStockWarningProduct = product
                                            } else {
                                                viewModel.addProductToCart(product, 1)
                                            }
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
                                                    text = stringResource(R.string.mrp_format, CurrencyUtils.formatRupees(product.mrp)),
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
                                                    stringResource(R.string.stock_out),
                                                    color = ErrorRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            } else if (product.currentStock <= product.lowStockAlertQty) {
                                                Text(
                                                    stringResource(R.string.stock_low_format, product.currentStock),
                                                    color = WarningOrange,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            } else {
                                                Text(
                                                    stringResource(R.string.stock_available_format, product.currentStock),
                                                    color = SuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        } else {
                                            Text(stringResource(R.string.stock_untracked), color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Add Indicator badge inside product list if in cart
                                    val lineInCart = cart[product]
                                    if (lineInCart != null) {
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
                                                text = if (product.unitType == DataUnitType.PIECE) "x${lineInCart.quantity}" else billingQuantityText(product, lineInCart.quantityBase, lineInCart.enteredQuantityText),
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            if (product.unitType != DataUnitType.PIECE) {
                                                showLooseQuantityDialog = product
                                            } else {
                                                val inCartQty = cart[product]?.quantity ?: 0
                                                if (product.trackStock && product.currentStock <= inCartQty) {
                                                    showStockWarningProduct = product
                                                } else {
                                                    viewModel.addProductToCart(product, 1)
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Add, stringResource(R.string.content_description_add_to_cart), tint = MaterialTheme.colorScheme.primary)
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
                                text = stringResource(R.string.billing_cart_empty),
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
                                    stringResource(R.string.billing_cart_count, cart.values.sumOf { line -> if (line.product.unitType == DataUnitType.PIECE) line.quantity else 1 }),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SaffronDark
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                items(cart.entries.toList()) { (product, line) ->
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
                                                    text = CurrencyUtils.formatRupees(line.lineTotal),
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
                                                        onClick = { if (product.unitType == DataUnitType.PIECE) viewModel.addProductToCart(product, -1) else viewModel.removeProductFromCart(product) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.RemoveCircleOutline,
                                                            stringResource(R.string.content_description_remove_from_cart),
                                                            tint = TextNearBlack,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = if (product.unitType == DataUnitType.PIECE) line.quantity.toString() else billingQuantityText(product, line.quantityBase, line.enteredQuantityText),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    // Plus button
                                                    IconButton(
                                                        onClick = {
                                                            if (product.unitType != DataUnitType.PIECE) {
                                                                showLooseQuantityDialog = product
                                                            } else if (product.trackStock && product.currentStock <= line.quantity) {
                                                                showStockWarningProduct = product
                                                            } else {
                                                                viewModel.addProductToCart(product, 1)
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.AddCircleOutline,
                                                            stringResource(R.string.content_description_add_to_cart),
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
                        Text(stringResource(R.string.billing_total), fontSize = 12.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
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
                                Toast.makeText(context, cartEmptyToast, Toast.LENGTH_SHORT).show()
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
                            Text(stringResource(R.string.billing_checkout), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                title = { Text(stringResource(R.string.billing_low_stock_dialog_title)) },
                text = {
                    Text(stringResource(R.string.billing_low_stock_dialog_message, product.name, product.currentStock))
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = {
                            viewModel.addProductToCart(product, 1)
                            showStockWarningProduct = null
                        }
                    ) {
                        Text(stringResource(R.string.action_yes_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStockWarningProduct = null }) {
                        Text(stringResource(R.string.action_no))
                    }
                }
            )
        }

        showLooseQuantityDialog?.let { product ->
            Dialog(onDismissRequest = { showLooseQuantityDialog = null }) {
                var quantityInput by remember(product.localUuid) { mutableStateOf("") }
                var amountInput by remember(product.localUuid) { mutableStateOf("") }
                var enterByAmount by remember(product.localUuid) { mutableStateOf(false) }
                val displayUnit = billingQuantityDisplayUnit(product)
                val unitLabel = billingUnitLabel(product)
                val rate = UnitRate(
                    product.pricePerUnitPaise,
                    product.priceUnitBaseQty.takeIf { it > 0L } ?: 1L
                )
                val enteredAmountPaise = if (enterByAmount) {
                    when (val amountResult = QuantityPriceCalculator.parseAmountPaise(amountInput)) {
                        is CalculationResult.Success -> amountResult.value
                        is CalculationResult.Failure -> null
                    }
                } else {
                    null
                }
                val quantityBase = if (enterByAmount) {
                    enteredAmountPaise?.let { amountPaise ->
                        when (val quantityResult = QuantityPriceCalculator.quantityForAmount(
                            amountPaise = amountPaise,
                            rate = rate,
                            unitType = billingProductUnitType(product)
                        )) {
                            is CalculationResult.Success -> quantityResult.value
                            is CalculationResult.Failure -> null
                        }
                    }
                } else {
                    billingParseQuantityBase(quantityInput, displayUnit)
                }
                val amountPaise = if (enterByAmount) {
                    enteredAmountPaise
                } else {
                    quantityBase?.let { qtyBase ->
                    when (val amountResult = QuantityPriceCalculator.amountForQuantity(
                        qtyBase,
                        rate
                    )) {
                        is CalculationResult.Success -> amountResult.value
                        is CalculationResult.Failure -> null
                    }
                    }
                }
                val enteredQuantityText = if (enterByAmount) {
                    quantityBase?.let { billingBaseToDisplayText(it, displayUnit) }.orEmpty()
                } else {
                    quantityInput.trim()
                }
                val exceedsStock = product.trackStock && quantityBase != null && quantityBase > product.stockQuantityBase

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loose_quantity_dialog_title, product.name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !enterByAmount,
                                onClick = { enterByAmount = false },
                                label = { Text(stringResource(R.string.loose_entry_mode_quantity)) }
                            )
                            FilterChip(
                                selected = enterByAmount,
                                onClick = { enterByAmount = true },
                                label = { Text(stringResource(R.string.loose_entry_mode_amount)) }
                            )
                        }
                        if (enterByAmount) {
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text(stringResource(R.string.loose_amount_label)) },
                                placeholder = { Text(stringResource(R.string.loose_amount_placeholder)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("loose_amount_input"),
                                singleLine = true
                            )
                            Text(
                                text = stringResource(
                                    R.string.loose_quantity_preview_format,
                                    quantityBase?.let { billingQuantityText(product, it, null) } ?: "-"
                                ),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it },
                                label = { Text(stringResource(R.string.loose_quantity_label, unitLabel)) },
                                placeholder = { Text(stringResource(R.string.loose_quantity_placeholder)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("loose_quantity_input"),
                                singleLine = true
                            )
                        }
                        Text(
                            text = stringResource(R.string.loose_stock_available_format, billingQuantityText(product, product.stockQuantityBase, null)),
                            fontSize = 12.sp,
                            color = TextMutedGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.loose_amount_preview_format,
                                amountPaise?.let { CurrencyUtils.formatRupees(it / 100.0) } ?: CurrencyUtils.formatRupees(0.0)
                            ),
                            fontSize = 16.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Black
                        )
                        if (exceedsStock) {
                            Text(
                                text = stringResource(R.string.loose_quantity_exceeds_stock_toast),
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showLooseQuantityDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Button(
                                onClick = {
                                    val qtyBase = quantityBase
                                    val finalAmountPaise = amountPaise
                                    if (qtyBase == null || qtyBase <= 0L) {
                                        Toast.makeText(context, context.getString(R.string.loose_invalid_quantity_toast), Toast.LENGTH_SHORT).show()
                                    } else if (product.trackStock && qtyBase > product.stockQuantityBase) {
                                        Toast.makeText(context, context.getString(R.string.loose_quantity_exceeds_stock_toast), Toast.LENGTH_SHORT).show()
                                    } else if (enterByAmount && finalAmountPaise != null && viewModel.setLooseProductAmountInCart(product, finalAmountPaise, qtyBase, enteredQuantityText)) {
                                        showLooseQuantityDialog = null
                                    } else if (!enterByAmount && viewModel.setLooseProductInCart(product, qtyBase, enteredQuantityText)) {
                                        showLooseQuantityDialog = null
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.loose_invalid_quantity_toast), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = quantityBase != null && amountPaise != null && !exceedsStock,
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Text(stringResource(R.string.quick_add_save_add_to_bill))
                            }
                        }
                    }
                }
            }
        }
        // 2. QUICK ADD PRODUCT DIALOG
        if (showQuickAddDialog) {
            Dialog(onDismissRequest = { showQuickAddDialog = false }) {
                var newName by remember { mutableStateOf("") }
                var newMrp by remember { mutableStateOf("") }
                var selectedCatId by remember { mutableStateOf<String>(categories.firstOrNull()?.localUuid ?: "") }
                var trackStock by remember { mutableStateOf(true) }
                var initialStock by remember { mutableStateOf("10") }

                LaunchedEffect(categories) {
                    if (selectedCatId.isBlank() && categories.isNotEmpty()) {
                        val firstValid = categories.firstOrNull { it.localUuid.isNotBlank() }
                        if (firstValid != null) {
                            selectedCatId = firstValid.localUuid
                        }
                    }
                }

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
                            stringResource(R.string.quick_add_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(R.string.quick_add_product_name_label)) },
                            placeholder = { Text(stringResource(R.string.quick_add_product_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_product_name")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newMrp,
                                onValueChange = { newMrp = it },
                                label = { Text(stringResource(R.string.quick_add_mrp_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_mrp")
                            )

                            OutlinedTextField(
                                value = initialStock,
                                onValueChange = { initialStock = it },
                                label = { Text(stringResource(R.string.quick_add_stock_qty_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_stock"),
                                enabled = trackStock
                            )
                        }

                        // Category Dropdown
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        var selectedCatName by remember(selectedCatId) {
                            mutableStateOf(categories.find { it.localUuid == selectedCatId }?.name ?: "")
                        }
                        val fallbackCategoryName = stringResource(R.string.category_miscellaneous)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.category_format, selectedCatName.ifBlank { fallbackCategoryName }))
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
                                            selectedCatId = cat.localUuid
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
                            Text(stringResource(R.string.track_stock_label), fontSize = 14.sp)
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
                                Text(stringResource(R.string.action_cancel))
                            }

                            Button(
                                onClick = {
                                    val mrpValue = newMrp.toDoubleOrNull()
                                    val stockValue = initialStock.toIntOrNull() ?: 0
                                    if (newName.trim().isEmpty() || mrpValue == null || selectedCatId.isBlank()) {
                                        if (selectedCatId.isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.product_category_error), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, quickAddValidationToast, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.quickAddProduct(
                                            name = newName,
                                            mrp = mrpValue,
                                            categoryId = selectedCatId,
                                            trackStock = trackStock,
                                            currentStock = if (trackStock) stockValue else 0
                                        )
                                        showQuickAddDialog = false
                                        Toast.makeText(context, context.getString(R.string.quick_add_added_toast, newName.trim()), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.quick_add_save_add_to_bill))
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
    val customerNameRequiredToast = stringResource(R.string.credit_customer_name_required_toast)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Billing) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                    Text(text = stringResource(R.string.payment_amount_to_pay), fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
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
                            stringResource(R.string.payment_paytm_qr_title),
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
                                contentDescription = stringResource(R.string.content_description_static_paytm_qr),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.payment_qr_instruction),
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
                            Icon(Icons.Default.QrCode2, stringResource(R.string.content_description_no_qr_configured), tint = SaffronPrimary, modifier = Modifier.size(80.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.payment_no_qr_configured),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = stringResource(R.string.payment_no_qr_instruction),
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
                            Text(stringResource(R.string.payment_cash_received), fontWeight = FontWeight.Black, fontSize = 14.sp)
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
                            Text(stringResource(R.string.payment_upi_recorded), fontWeight = FontWeight.Black, fontSize = 14.sp)
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
                        Text(stringResource(R.string.payment_credit_ledger), fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                            stringResource(R.string.credit_customer_dialog_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )

                        // Search box
                        OutlinedTextField(
                            value = searchCustName,
                            onValueChange = { searchCustName = it },
                            placeholder = { Text(stringResource(R.string.credit_customer_search_placeholder), color = TextMutedGray) },
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
                                    Text(stringResource(R.string.credit_no_customer_found), color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { showAddNewCustomerBlock = true }) {
                                        Text(stringResource(R.string.credit_add_new_customer_format, searchCustName), color = SaffronPrimary, fontWeight = FontWeight.Black)
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
                                                        customerUuid = cust.localUuid
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
                            Text(stringResource(R.string.credit_new_customer_title), fontWeight = FontWeight.Black, fontSize = 12.sp, color = TextNearBlack)
                            
                            OutlinedTextField(
                                value = custPhone,
                                onValueChange = { custPhone = it },
                                label = { Text(stringResource(R.string.phone_number_optional_label)) },
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
                                        Toast.makeText(context, customerNameRequiredToast, Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.completeBill(
                                            paymentMode = "UDHAAR",
                                            customerUuid = null, // create dynamic
                                            customerName = searchCustName,
                                            customerPhone = custPhone
                                        )
                                        showUdhaarCustomerDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.credit_open_account_confirm), fontWeight = FontWeight.Black)
                            }
                        }

                        TextButton(
                            onClick = { showUdhaarCustomerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_cancel), color = TextMediumGray, fontWeight = FontWeight.Bold)
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
private fun billingQuantityDisplayUnit(product: Product): QuantityDisplayUnit {
    return when (product.unitType) {
        DataUnitType.WEIGHT -> QuantityDisplayUnit.KILOGRAM
        DataUnitType.VOLUME -> QuantityDisplayUnit.LITER
        else -> QuantityDisplayUnit.PIECE
    }
}

private fun billingProductUnitType(product: Product): ProductUnitType {
    return when (product.unitType) {
        DataUnitType.WEIGHT -> ProductUnitType.WEIGHT
        DataUnitType.VOLUME -> ProductUnitType.VOLUME
        else -> ProductUnitType.PIECE
    }
}

@Composable
private fun billingUnitLabel(product: Product): String {
    return when (product.unitType) {
        DataUnitType.WEIGHT -> stringResource(R.string.unit_kg_short)
        DataUnitType.VOLUME -> stringResource(R.string.unit_liter_short)
        else -> stringResource(R.string.unit_piece_short)
    }
}

@Composable
private fun billingQuantityText(product: Product, quantityBase: Long, enteredQuantityText: String?): String {
    val unitLabel = billingUnitLabel(product)
    val displayUnit = billingQuantityDisplayUnit(product)
    val quantityText = enteredQuantityText?.takeIf { it.isNotBlank() } ?: billingBaseToDisplayText(quantityBase, displayUnit)
    return "$quantityText $unitLabel"
}

private fun billingParseQuantityBase(input: String, displayUnit: QuantityDisplayUnit): Long? {
    return when (val result = QuantityPriceCalculator.parseQuantityBase(input.trim(), displayUnit)) {
        is CalculationResult.Success -> result.value
        is CalculationResult.Failure -> null
    }
}

private fun billingBaseToDisplayText(baseQuantity: Long, displayUnit: QuantityDisplayUnit): String {
    if (baseQuantity <= 0L) return "0"
    val scale = displayUnit.baseUnitsPerDisplayUnit
    if (scale <= 1L) return baseQuantity.toString()
    val whole = baseQuantity / scale
    val fraction = (baseQuantity % scale).toString().padStart(displayUnit.maxFractionDigits, '0').trimEnd('0')
    return if (fraction.isBlank()) whole.toString() else "$whole.$fraction"
}
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
                contentDescription = stringResource(R.string.bill_success_content_description),
                tint = SuccessGreen,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = stringResource(R.string.bill_success_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = SuccessGreen
            )

            lastSale?.let { sale ->
                Text(
                    text = stringResource(R.string.bill_number_format, sale.billNumber),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMediumGray
                )

                Text(
                    text = stringResource(R.string.bill_total_amount_format, CurrencyUtils.formatRupees(sale.totalAmount)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                Text(
                    text = stringResource(R.string.bill_payment_mode_format, localizedPaymentMode(sale.paymentMode)),
                    fontWeight = FontWeight.Black,
                    color = when (sale.paymentMode) {
                        "UPI" -> Color(0xFF0E5A94)
                        "UDHAAR" -> ErrorRed
                        else -> SuccessGreen
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clipboard button
            Button(
                onClick = { viewModel.copyInvoiceToClipboard(context) },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("copy_invoice_text_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bill_copy_invoice), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.bill_history), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.Billing) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("new_bill_confirm")
                ) {
                    Text(stringResource(R.string.bill_new_bill), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun localizedPaymentMode(paymentMode: String): String {
    return when (paymentMode) {
        "UPI" -> stringResource(R.string.payment_mode_upi)
        "UDHAAR" -> stringResource(R.string.payment_mode_credit)
        else -> stringResource(R.string.payment_mode_cash)
    }
}
