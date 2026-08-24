package com.aistudio.shreeshyamstore.pqwzkb.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuItem
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuSurface
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppMutationStatusCard
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.BarcodeScannerDialog
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStage
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStatus
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.InventoryViewModel
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.Screen
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel

// ==========================================
// 1. BILLING / POINT OF SALE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: ShopViewModel, inventoryViewModel: InventoryViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val products by inventoryViewModel.products.collectAsState()
    val categories by inventoryViewModel.categories.collectAsState()
    val cart by viewModel.cartState.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val mutationStatus by viewModel.mutationStatus.collectAsState()
    val mutationInFlight by viewModel.mutationInFlight.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var quickAddInputError by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Warning dialog regarding insufficient stock
    var showStockWarningProduct by remember { mutableStateOf<Product?>(null) }

    // Filter active products
    val activeProducts = remember(products) { products.filter { it.isActive } }
    
    val filteredProducts = remember(activeProducts, searchQuery, selectedCategoryId) {
        activeProducts.filter { prod ->
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                    (prod.barcode.isNotEmpty() && prod.barcode.contains(searchQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val displayName = settings.shopName.ifEmpty { strings.defaultShopName }
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.newBill,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMediumGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (!mutationInFlight) showQuickAddDialog = true },
                        enabled = !mutationInFlight,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Quick Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.quickAddProduct, fontWeight = FontWeight.Bold)
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
            AppMutationStatusCard(
                status = quickAddInputError?.let {
                    MutationStatus(MutationStage.VALIDATION_ERROR, it)
                } ?: mutationStatus,
                strings = strings,
                onRetry = if (mutationStatus.canRetry) viewModel::retryLastMutation else null,
                onDismiss = if (!mutationInFlight) {
                    { quickAddInputError = null; viewModel.clearMutationStatus() }
                } else null
            )
            // Search and filter controls
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchProduct, color = TextMutedGray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SaffronPrimary) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = TextNearBlack)
                            }
                        }
                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier.testTag("billing_scan_barcode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = SaffronPrimary
                            )
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
                        label = { Text(strings.allCategories, fontWeight = FontWeight.Bold) }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = BorderStrong
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val noProductFound = if (settings.appLanguage == AppLanguage.HINDI) "कोई सामान नहीं मिला!" else "No product found!"
                                Text(
                                    noProductFound,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedGray
                                )
                            }
                        }
                    }

                    items(filteredProducts, key = { it.id }) { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, BorderStrong),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val inCartQty = cart[product] ?: 0.0
                                    if (product.trackStock && product.currentStock <= inCartQty) {
                                        showStockWarningProduct = product
                                    } else {
                                        viewModel.addProductToCart(product, 1.0)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextNearBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = CurrencyUtils.formatRupees(product.getEffectivePrice()),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SaffronDark
                                        )
                                        if (product.sellingPrice != null && product.sellingPrice < product.mrp) {
                                            Text(
                                                text = CurrencyUtils.formatRupees(product.mrp),
                                                fontSize = 11.sp,
                                                color = TextMutedGray,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (product.trackStock) {
                                        if (product.currentStock <= 0) {
                                            Text(
                                                strings.outOfStock,
                                                color = ErrorRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else if (product.currentStock <= product.lowStockAlertQty) {
                                            val lowStockMsg = if (settings.appLanguage == AppLanguage.HINDI) "कम स्टॉक: ${product.currentStock} बचा है" else "Low stock: ${product.currentStock} left"
                                            Text(
                                                lowStockMsg,
                                                color = WarningOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else {
                                            val stockMsg = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक: ${product.currentStock} उपलब्ध" else "Stock: ${product.currentStock} available"
                                            Text(
                                                stockMsg,
                                                color = SuccessGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    } else {
                                        val untrackedMsg = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक अनट्रैक्ड" else "Stock untracked"
                                        Text(untrackedMsg, color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                // Add Indicator badge inside product list if in cart
                                val qtyInCart = cart[product] ?: 0.0
                                if (qtyInCart > 0.0) {
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
                                            text = if (qtyInCart % 1.0 == 0.0) "x${qtyInCart.toLong()}" else "x$qtyInCart",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        val inCartQty = cart[product] ?: 0.0
                                        if (product.trackStock && product.currentStock <= inCartQty) {
                                            showStockWarningProduct = product
                                        } else {
                                            viewModel.addProductToCart(product, 1.0)
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, "Add to Basket", tint = MaterialTheme.colorScheme.primary)
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
                                text = strings.emptyCart,
                                fontSize = 12.sp,
                                color = TextMutedGray,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val totalQty = cart.values.sum()
                            val qtyLabel = if (totalQty % 1.0 == 0.0) "${totalQty.toLong()}" else "$totalQty"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SaffronLight)
                                    .padding(8.dp)
                            ) {
                                val basketHeader = if (settings.appLanguage == AppLanguage.HINDI) "थैला: $qtyLabel सामान" else "Cart: $qtyLabel items"
                                Text(
                                    basketHeader,
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
                                                    text = CurrencyUtils.formatRupees(
                                                        CommerceValidation.calculateLineTotal(product.getEffectivePrice(), quantity)
                                                    ),
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
                                                        onClick = { viewModel.addProductToCart(product, -1.0) },
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
                                                        text = if (quantity % 1.0 == 0.0) "${quantity.toLong()}" else "$quantity",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    // Plus button
                                                    IconButton(
                                                        onClick = {
                                                            if (product.trackStock && product.currentStock <= quantity) {
                                                                showStockWarningProduct = product
                                                            } else {
                                                                viewModel.addProductToCart(product, 1.0)
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

            // Bottom Settlement Bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.totalAmount, fontSize = 12.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
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
                                Toast.makeText(context, strings.emptyCart, Toast.LENGTH_SHORT).show()
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
                            Text(strings.checkoutBill, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            val dialogTitle = if (settings.appLanguage == AppLanguage.HINDI) "पर्याप्त स्टॉक नहीं है" else "Insufficient stock"
            val dialogBody = if (settings.appLanguage == AppLanguage.HINDI) {
                "प्रोडक्ट '${product.name}' का उपलब्ध स्टॉक ${product.currentStock} है। इससे अधिक मात्रा का बिल नहीं बनाया जा सकता।"
            } else {
                "Product '${product.name}' has only ${product.currentStock} available. A bill cannot include more than the available stock."
            }
            val confirmAdd = if (settings.appLanguage == AppLanguage.HINDI) "ठीक है" else "OK"

            AlertDialog(
                onDismissRequest = { showStockWarningProduct = null },
                title = { Text(dialogTitle) },
                text = { Text(dialogBody) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = { showStockWarningProduct = null }
                    ) {
                        Text(confirmAdd)
                    }
                }
            )
        }

        // 2. BARCODE SCANNER DIALOG
        if (showBarcodeScanner) {
            BarcodeScannerDialog(
                onDismiss = { showBarcodeScanner = false },
                onBarcodeScanned = { scannedCode ->
                    showBarcodeScanner = false
                    val matchedProduct = activeProducts.find { 
                        it.barcode.isNotBlank() && it.barcode.equals(scannedCode.trim(), ignoreCase = true)
                    }
                    if (matchedProduct != null) {
                        if (matchedProduct.trackStock && matchedProduct.currentStock <= 0.0) {
                            showStockWarningProduct = matchedProduct
                        } else {
                            viewModel.addProductToCart(matchedProduct)
                            val addedMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                                "✓ ${matchedProduct.name} बिल में जोड़ा गया"
                            } else {
                                "✓ ${matchedProduct.name} added to cart"
                            }
                            Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        searchQuery = scannedCode.trim()
                        val notFoundMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                            "बारकोड: $scannedCode (उत्पाद नहीं मिला - नया उत्पाद जोड़ें)"
                        } else {
                            "Barcode: $scannedCode (Product not found in inventory)"
                        }
                        Toast.makeText(context, notFoundMsg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // 3. QUICK ADD PRODUCT DIALOG
        if (showQuickAddDialog) {
            Dialog(onDismissRequest = { if (!mutationInFlight) showQuickAddDialog = false }) {
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
                        AppMutationStatusCard(
                            status = quickAddInputError?.let { MutationStatus(MutationStage.VALIDATION_ERROR, it) } ?: mutationStatus,
                            strings = strings,
                            onRetry = if (mutationStatus.canRetry) viewModel::retryLastMutation else null,
                            onDismiss = if (!mutationInFlight) {
                                { quickAddInputError = null; viewModel.clearMutationStatus() }
                            } else null
                        )
                        Text(
                            strings.quickAddProduct,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it; quickAddInputError = null },
                            label = { Text(strings.productName) },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_product_name")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newMrp,
                                onValueChange = { newMrp = it; quickAddInputError = null },
                                label = { Text(strings.mrpPrice) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_mrp")
                            )

                            OutlinedTextField(
                                value = initialStock,
                                onValueChange = { initialStock = it; quickAddInputError = null },
                                label = { Text(strings.currentStock) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quick_add_product_stock"),
                                enabled = trackStock
                            )
                        }

                        // Category Dropdown
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        var selectedCatName by remember(selectedCatId) {
                            mutableStateOf(categories.find { it.id == selectedCatId }?.name ?: "General")
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${strings.category}: $selectedCatName")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            AppDropdownMenuSurface(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                if (categories.isEmpty()) {
                                    AppDropdownMenuItem(
                                        text = strings.noCategories,
                                        onClick = {},
                                        enabled = false
                                    )
                                } else {
                                    categories.forEach { cat ->
                                        AppDropdownMenuItem(
                                            text = cat.name,
                                            onClick = {
                                                selectedCatId = cat.id
                                                selectedCatName = cat.name
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Track Stock Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.trackStock, fontSize = 14.sp)
                            Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { if (!mutationInFlight) showQuickAddDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.cancel)
                            }

                            val saveAndAddText = if (settings.appLanguage == AppLanguage.HINDI) "सेव करें व बिल में जोड़ें" else "Save & Add to Bill"
                            Button(
                                enabled = !mutationInFlight,
                                onClick = {
                                    val mrpValue = MoneyUtils.parseMajorUnits(newMrp)
                                    val stockValue = initialStock.toDoubleOrNull() ?: 0.0
                                    if (newName.trim().isEmpty() || mrpValue == null || mrpValue <= 0L) {
                                        quickAddInputError = strings.statusValidationError
                                    } else {
                                        quickAddInputError = null
                                        viewModel.quickAddProduct(
                                            name = newName,
                                            mrp = mrpValue,
                                            categoryId = selectedCatId,
                                            trackStock = trackStock,
                                            currentStock = if (trackStock) stockValue else 0.0,
                                            onSuccess = { showQuickAddDialog = false }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(saveAndAddText)
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
fun PaymentScreen(viewModel: ShopViewModel, invoiceTotal: Long) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }
    val customers by viewModel.customers.collectAsState()
    val allUdhaarTransactions by viewModel.allUdhaarTransactions.collectAsState()
    val checkoutInFlight by viewModel.checkoutInFlight.collectAsState()
    val checkoutStatus by viewModel.checkoutMutationStatus.collectAsState()


    val customerBalanceMap = remember(allUdhaarTransactions) {
        allUdhaarTransactions.groupBy { it.customerId }.mapValues { (_, list) ->
                list.sumOf { it.balanceEffect }
        }
    }

    var showUdhaarCustomerDialog by remember { mutableStateOf(false) }
    var showCreditLimitWarningDialog by remember { mutableStateOf(false) }
    var pendingCreditLimitCustomer by remember { mutableStateOf<Customer?>(null) }
    var pendingProjectedBalance by remember { mutableLongStateOf(0L) }
    var receivedAmountText by remember(invoiceTotal) {
        mutableStateOf(MoneyUtils.toInputString(invoiceTotal))
    }
    var checkoutInputError by remember { mutableStateOf<String?>(null) }
    val visibleCheckoutStatus = checkoutInputError?.let {
        MutationStatus(MutationStage.VALIDATION_ERROR, it)
    } ?: checkoutStatus
    val submitImmediatePayment: (String) -> Unit = { mode ->
        val receivedAmount = MoneyUtils.parseMajorUnits(receivedAmountText)
        if (receivedAmount == null) {
            checkoutInputError = strings.statusValidationError
        } else {
            checkoutInputError = null
            viewModel.completeBill(paymentMode = mode, receivedAmount = receivedAmount)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val displayName = settings.shopName.ifEmpty { strings.defaultShopName }
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.paymentTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMediumGray
                        )
                    }
                },
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
            AppMutationStatusCard(
                status = visibleCheckoutStatus,
                strings = strings,
                onRetry = if (visibleCheckoutStatus.canRetry) viewModel::retryCheckoutMutation else null,
                onDismiss = if (!checkoutInFlight) {
                    { checkoutInputError = null; viewModel.clearCheckoutMutationStatus() }
                } else null
            )
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
                    val payableTitle = if (settings.appLanguage == AppLanguage.HINDI) "कुल चुकाने योग्य राशि" else "Total Payable Amount"
                    Text(text = payableTitle, fontSize = 14.sp, color = TextMutedGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatRupees(invoiceTotal),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreen
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, BorderStrong),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val receivedLabel = if (settings.appLanguage == AppLanguage.HINDI) {
                        "प्राप्त राशि (Cash / UPI)"
                    } else {
                        "Received amount (Cash / UPI)"
                    }
                    Text(receivedLabel, fontWeight = FontWeight.Bold, color = TextNearBlack)
                    OutlinedTextField(
                        value = receivedAmountText,
                        onValueChange = {
                            receivedAmountText = it
                            checkoutInputError = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth().testTag("received_amount_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = BorderStrong,
                            focusedTextColor = TextNearBlack,
                            unfocusedTextColor = TextNearBlack
                        )
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
                            strings.paytmQrCode,
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
                        val qrPrompt = if (settings.appLanguage == AppLanguage.HINDI) {
                            "ग्राहक से कहें: 'QR स्कैन करके राशि दर्ज करें।'"
                        } else {
                            "Ask customer: 'Scan QR and enter exact amount.'"
                        }
                        Text(
                            text = qrPrompt,
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
                        val noQrTitle = if (settings.appLanguage == AppLanguage.HINDI) "UPI QR सेट नहीं है" else "UPI QR not set"
                        val noQrSub = if (settings.appLanguage == AppLanguage.HINDI) "सेटिंग्स में जाकर दुकान का Paytm Business QR लगाएं।" else "Go to Settings to configure shop Paytm QR."
                        Text(
                            text = noQrTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextNearBlack
                        )
                        Text(
                            text = noQrSub,
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
                        onClick = { submitImmediatePayment("CASH") },
                        enabled = !checkoutInFlight,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("cash_pay_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payments, null, tint = Color.White)
                            Text(strings.cash, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }

                    // UPI
                    Button(
                        onClick = { submitImmediatePayment("UPI") },
                        enabled = !checkoutInFlight,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5A94), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("upi_pay_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = Color.White)
                            val upiText = if (settings.appLanguage == AppLanguage.HINDI) "UPI भुगतान" else "UPI Paid"
                            Text(upiText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }

                // Udhaar
                Button(
                    onClick = { showUdhaarCustomerDialog = true },
                    enabled = !checkoutInFlight,
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
                        Text(strings.udhaarMode, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }

        // --- CUSTOMER SELECTOR FOR UDHAAR DIALOG ---
        if (showUdhaarCustomerDialog) {
            Dialog(onDismissRequest = { if (!checkoutInFlight) showUdhaarCustomerDialog = false }) {
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
                        AppMutationStatusCard(
                            status = visibleCheckoutStatus,
                            strings = strings,
                            onRetry = if (visibleCheckoutStatus.canRetry) viewModel::retryCheckoutMutation else null,
                            onDismiss = if (!checkoutInFlight) {
                                { checkoutInputError = null; viewModel.clearCheckoutMutationStatus() }
                            } else null
                        )
                        val selectCustHeader = if (settings.appLanguage == AppLanguage.HINDI) "उधार ग्राहक चुनें" else "Select Udhaar Customer"
                        Text(
                            selectCustHeader,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )

                        // Search box
                        OutlinedTextField(
                            value = searchCustName,
                            onValueChange = { searchCustName = it },
                            placeholder = { Text(strings.searchCustomer, color = TextMutedGray) },
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
                                    val noCustFound = if (settings.appLanguage == AppLanguage.HINDI) "कोई ग्राहक नहीं मिला।" else "No customer found."
                                    Text(noCustFound, color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { showAddNewCustomerBlock = true }) {
                                        val addNewLabel = if (settings.appLanguage == AppLanguage.HINDI) "+ नया जोड़ें: '$searchCustName'" else "+ Add New: '$searchCustName'"
                                        Text(addNewLabel, color = SaffronPrimary, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filteredCustomers) { cust ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !checkoutInFlight) {
                                                    viewModel.completeBill(
                                                        paymentMode = "UDHAAR",
                                                        customerId = cust.id
                                                    )
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
                            Text(strings.addCustomer, fontWeight = FontWeight.Black, fontSize = 12.sp, color = TextNearBlack)
                            
                            OutlinedTextField(
                                value = custPhone,
                                onValueChange = { custPhone = it },
                                label = { Text(strings.customerPhone) },
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

                            val confirmUdhaarLabel = if (settings.appLanguage == AppLanguage.HINDI) "खाता खोलें और उधार लिखें" else "Open Ledger & Confirm Udhaar"
                            Button(
                                onClick = {
                                    if (searchCustName.trim().isEmpty()) {
                                        checkoutInputError = strings.checkoutCustomerError
                                    } else {
                                        viewModel.completeBill(
                                            paymentMode = "UDHAAR",
                                            customerId = null, // create dynamic
                                            customerName = searchCustName,
                                            customerPhone = custPhone
                                        )
                                    }
                                },
                                enabled = !checkoutInFlight,
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(confirmUdhaarLabel, fontWeight = FontWeight.Black)
                            }
                        }

                        TextButton(
                            onClick = { if (!checkoutInFlight) showUdhaarCustomerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.cancel, color = TextMediumGray, fontWeight = FontWeight.Bold)
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
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }
    val lastSale by viewModel.lastSale.collectAsState()
    val lastItems by viewModel.lastSaleItems.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCreamBg),
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
                text = strings.billSavedSuccess,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = SuccessGreen
            )

            lastSale?.let { sale ->
                val billNoPrefix = if (settings.appLanguage == AppLanguage.HINDI) "बिल नंबर:" else "Bill No:"
                Text(
                    text = "$billNoPrefix ${sale.billNumber}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMediumGray
                )

                Text(
                    text = "${strings.totalAmount}: ${CurrencyUtils.formatRupees(sale.totalAmount)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextNearBlack
                )

                val paymentModeLabel = if (settings.appLanguage == AppLanguage.HINDI) "भुगतान माध्यम:" else "Payment Mode:"
                val modeDisplay = when (sale.paymentMode) {
                    "UPI" -> "UPI"
                    "UDHAAR" -> strings.udhaarMode
                    else -> strings.cash
                }
                Text(
                    text = "$paymentModeLabel $modeDisplay",
                    fontWeight = FontWeight.Black,
                    color = when (sale.paymentMode) {
                        "UPI" -> Color(0xFF0E5A94)
                        "UDHAAR" -> ErrorRed
                        else -> SuccessGreen
                    }
                )
                val paymentStatusLabel = if (settings.appLanguage == AppLanguage.HINDI) "भुगतान स्थिति:" else "Payment Status:"
                Text(
                    text = "$paymentStatusLabel ${sale.paymentState}",
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray
                )
                sale.receivedAmount?.let { received ->
                    val receivedLabel = if (settings.appLanguage == AppLanguage.HINDI) "प्राप्त राशि:" else "Received:"
                    Text(
                        text = "$receivedLabel ${CurrencyUtils.formatRupees(received)}",
                        fontWeight = FontWeight.Bold,
                        color = TextNearBlack
                    )
                    if (sale.paymentMode.equals("CASH", ignoreCase = true) && received > sale.totalAmount) {
                        val changeLabel = if (settings.appLanguage == AppLanguage.HINDI) "वापसी:" else "Change:"
                        Text(
                            text = "$changeLabel ${CurrencyUtils.formatRupees(received - sale.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
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
                    Text(strings.shareReceiptWhatsApp, fontSize = 16.sp, fontWeight = FontWeight.Black)
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
                    val copyLabel = if (settings.appLanguage == AppLanguage.HINDI) "बिल कॉपी करें" else "Copy Invoice"
                    Text(copyLabel, fontWeight = FontWeight.Bold)
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
                    val histLabel = if (settings.appLanguage == AppLanguage.HINDI) "इतिहास" else "History"
                    Text(histLabel, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.Billing) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = Color.White),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("new_bill_confirm")
                ) {
                    Text(strings.newBill, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
