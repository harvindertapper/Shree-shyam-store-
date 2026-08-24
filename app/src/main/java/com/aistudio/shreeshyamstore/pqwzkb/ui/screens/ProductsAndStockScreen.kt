package com.aistudio.shreeshyamstore.pqwzkb.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuItem
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuSurface
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.BarcodeScannerDialog
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.*
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.CurrencyUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.DateTimeUtils
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.InventoryViewModel
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.Screen
import com.aistudio.shreeshyamstore.pqwzkb.viewmodel.ShopViewModel

// ==========================================
// 1. PRODUCTS MASTER LIST SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: ShopViewModel, inventoryViewModel: InventoryViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val products by inventoryViewModel.products.collectAsState()
    val categories by inventoryViewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var filterOnlyLowStock by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val lowStockProducts = remember(products) {
        products.filter { it.isActive && it.trackStock && it.currentStock <= it.lowStockAlertQty }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId, filterOnlyLowStock) {
        products.filter { prod ->
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                    (prod.barcode.isNotEmpty() && prod.barcode.contains(searchQuery, ignoreCase = true))
            val matchesLowStock = !filterOnlyLowStock || (prod.isActive && prod.trackStock && prod.currentStock <= prod.lowStockAlertQty)
            matchesCategory && matchesSearch && matchesLowStock
        }
    }

    if (showReorderDialog) {
        LowStockReorderDialog(
            viewModel = viewModel,
            lowStockProducts = lowStockProducts,
            categories = categories,
            onDismiss = { showReorderDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.productsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Re-order List Action Icon with Badge
                    IconButton(
                        onClick = { showReorderDialog = true },
                        modifier = Modifier.testTag("open_reorder_list_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (lowStockProducts.isNotEmpty()) {
                                    Badge(
                                        containerColor = ErrorRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("${lowStockProducts.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlaylistAddCheck,
                                contentDescription = "Re-order List",
                                tint = if (lowStockProducts.isNotEmpty()) ErrorRed else SaffronPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.exportStockCsv(context, products, categories) },
                        modifier = Modifier.testTag("export_stock_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Stock CSV", tint = SaffronPrimary)
                    }
                    IconButton(onClick = { showCategoryManagerDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Manage Categories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.AddEditProduct(null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addProductTitle, tint = Color.White)
            }
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
                placeholder = { Text(strings.searchProductPlaceholder, color = TextMutedGray) },
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
                            modifier = Modifier.testTag("product_scan_barcode_button")
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
                    .testTag("product_search_input"),
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

            if (showBarcodeScanner) {
                BarcodeScannerDialog(
                    onDismiss = { showBarcodeScanner = false },
                    onBarcodeScanned = { scannedCode ->
                        showBarcodeScanner = false
                        searchQuery = scannedCode.trim()
                        val scannedMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                            "बारकोड स्कैन हुआ: $scannedCode"
                        } else {
                            "Barcode Scanned: $scannedCode"
                        }
                        Toast.makeText(context, scannedMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Low Stock Re-order Quick Alert Banner
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, ErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showReorderDialog = true }
                        .testTag("low_stock_reorder_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Column {
                                val lowStockBannerTitle = if (settings.appLanguage == AppLanguage.HINDI) {
                                    "कम स्टॉक चेतावनी: ${lowStockProducts.size} सामान"
                                } else {
                                    "Low Stock Warning: ${lowStockProducts.size} items"
                                }
                                val lowStockBannerSub = if (settings.appLanguage == AppLanguage.HINDI) {
                                    "थोक खरीदारी लिस्ट देखें"
                                } else {
                                    "View wholesale re-order list"
                                }
                                Text(
                                    text = lowStockBannerTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ErrorRed
                                )
                                Text(
                                    text = lowStockBannerSub,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextNearBlack
                                )
                            }
                        }

                        Button(
                            onClick = { showReorderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            val orderListBtn = if (settings.appLanguage == AppLanguage.HINDI) "ऑर्डर लिस्ट" else "Order List"
                            Text(orderListBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category & Low Stock Selector Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null && !filterOnlyLowStock,
                        onClick = {
                            selectedCategoryId = null
                            filterOnlyLowStock = false
                        },
                        label = { Text(strings.allCategories, fontWeight = FontWeight.Bold) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterOnlyLowStock,
                        onClick = { filterOnlyLowStock = !filterOnlyLowStock },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (filterOnlyLowStock) Color.White else ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "${strings.lowStock} (${lowStockProducts.size})",
                                    fontWeight = FontWeight.Black,
                                    color = if (filterOnlyLowStock) Color.White else ErrorRed
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ErrorRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id && !filterOnlyLowStock,
                        onClick = {
                            selectedCategoryId = cat.id
                            filterOnlyLowStock = false
                        },
                        label = { Text(cat.name, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Products list view
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(54.dp), tint = BorderStrong)
                            Spacer(modifier = Modifier.height(8.dp))
                            val noProductsMsg = if (settings.appLanguage == AppLanguage.HINDI) "कोई सामान उपलब्ध नहीं है!" else "No products available!"
                            Text(noProductsMsg, color = TextMediumGray, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                } else {
                    items(filteredProducts) { prod ->
                        val catName = categories.find { it.id == prod.categoryId }?.name ?: "General"

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (prod.isActive) Color.White else SlateContainer
                            ),
                            border = BorderStroke(1.2.dp, BorderStrong),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = if (prod.isActive) TextNearBlack else TextMutedGray
                                        )
                                        if (!prod.isActive) {
                                            val inactiveLabel = if (settings.appLanguage == AppLanguage.HINDI) "निष्क्रिय" else "Inactive"
                                            SuggestionChip(onClick = {}, label = { Text(inactiveLabel, fontWeight = FontWeight.Bold) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${strings.category}: $catName",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMediumGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "MRP: ${CurrencyUtils.formatRupees(prod.mrp)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMediumGray
                                        )
                                        val spLabel = if (settings.appLanguage == AppLanguage.HINDI) "बिक्री" else "SP"
                                        Text(
                                            text = "$spLabel: ${CurrencyUtils.formatRupees(prod.getEffectivePrice())}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SaffronPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Stock Badging
                                    if (prod.trackStock) {
                                        if (prod.currentStock <= 0) {
                                            Text(
                                                strings.outOfStock,
                                                color = ErrorRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else if (prod.currentStock <= prod.lowStockAlertQty) {
                                            val lowStockMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                                                "कम स्टॉक: ${prod.currentStock} बचा है"
                                            } else {
                                                "Low Stock: ${prod.currentStock} left"
                                            }
                                            Text(
                                                lowStockMsg,
                                                color = WarningOrange,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else {
                                            val stockMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                                                "स्टॉक: ${prod.currentStock} उपलब्ध"
                                            } else {
                                                "Stock: ${prod.currentStock} available"
                                            }
                                            Text(
                                                stockMsg,
                                                color = SuccessGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    } else {
                                        val untrackedMsg = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक अनट्रैक्ड" else "Stock untracked"
                                        Text(text = untrackedMsg, color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Adjustment button
                                    if (prod.trackStock) {
                                        IconButton(onClick = { viewModel.navigateTo(Screen.StockAdjustment(prod.id)) }) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = "Adjust Stock", tint = Color.DarkGray)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateTo(Screen.AddEditProduct(prod.id)) },
                                        modifier = Modifier.testTag("edit_product_${prod.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.editProduct, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SUB INLINE CATEGORY MANAGER DIALOG ---
        if (showCategoryManagerDialog) {
            Dialog(onDismissRequest = { showCategoryManagerDialog = false }) {
                var newCatName by remember { mutableStateOf("") }
                var renamingCat by remember { mutableStateOf<Category?>(null) }
                var renameText by remember { mutableStateOf("") }

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
                        val catManagerTitle = if (settings.appLanguage == AppLanguage.HINDI) "कैटेगरी प्रबंधन" else "Category Management"
                        Text(
                            catManagerTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Create form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val newCatPlaceholder = if (settings.appLanguage == AppLanguage.HINDI) "नई कैटेगरी का नाम" else "New Category Name"
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                placeholder = { Text(newCatPlaceholder) },
                                modifier = Modifier.weight(1f).testTag("category_add_input")
                            )
                            val addBtnText = if (settings.appLanguage == AppLanguage.HINDI) "जोड़ें" else "Add"
                            Button(onClick = {
                                if (newCatName.trim().isNotEmpty()) {
                                    inventoryViewModel.addCategory(newCatName)
                                    newCatName = ""
                                }
                            }) {
                                Text(addBtnText)
                            }
                        }

                        HorizontalDivider()

                        // Editing listing dialog line
                        if (renamingCat != null) {
                            val renameTitle = if (settings.appLanguage == AppLanguage.HINDI) "कैटेगरी का नाम बदलें:" else "Rename Category:"
                            Text(renameTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    modifier = Modifier.weight(1f)
                                )
                                val changeBtn = if (settings.appLanguage == AppLanguage.HINDI) "बदलें" else "Update"
                                Button(onClick = {
                                    renamingCat?.let {
                                        inventoryViewModel.renameCategory(it, renameText)
                                        renamingCat = null
                                        renameText = ""
                                    }
                                }) {
                                    Text(changeBtn)
                                }
                            }
                        }

                        // Categories List
                        Box(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(categories) { cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (cat.name != "Miscellaneous" && cat.name != "General") {
                                            Row {
                                                IconButton(onClick = {
                                                    renamingCat = cat
                                                    renameText = cat.name
                                                }) {
                                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val doneText = if (settings.appLanguage == AppLanguage.HINDI) "पूर्ण" else "Done"
                        Button(
                            onClick = { showCategoryManagerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(doneText)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. ADD/EDIT PRODUCT DETAIL SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ShopViewModel,
    inventoryViewModel: InventoryViewModel,
    productId: Long?
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }
    val categories by inventoryViewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long>(categories.firstOrNull()?.id ?: 1L) }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var trackStock by remember { mutableStateOf(true) }
    var currentStock by remember { mutableStateOf("") }
    var lowStockQty by remember { mutableStateOf("5") }
    var barcode by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val screenTitle = if (productId != null) strings.editProduct else strings.addProductTitle

    // Validation flags
    var nameError by remember { mutableStateOf(false) }
    var mrpError by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (productId != null) {
            val prod = inventoryViewModel.getProduct(productId)
            if (prod != null) {
                name = prod.name
                categoryId = prod.categoryId
                mrp = MoneyUtils.toInputString(prod.mrp)
                sellingPrice = prod.sellingPrice?.let(MoneyUtils::toInputString) ?: ""
                purchasePrice = prod.purchasePrice?.let(MoneyUtils::toInputString) ?: ""
                trackStock = prod.trackStock
                currentStock = prod.currentStock.toString()
                lowStockQty = prod.lowStockAlertQty.toString()
                barcode = prod.barcode
                isActive = prod.isActive
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Products) }) {
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
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.trim().isNotEmpty()) nameError = false
                },
                label = { Text(strings.productName) },
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        val err = if (settings.appLanguage == AppLanguage.HINDI) "नाम आवश्यक है!" else "Name is required!"
                        Text(err, color = Color.Red)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Barcode Input with Scan Button
            val barcodeLabel = if (settings.appLanguage == AppLanguage.HINDI) "बारकोड (वैकल्पिक / Optional)" else "Barcode (Optional)"
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(barcodeLabel) },
                placeholder = { Text("EAN / UPC / QR Code") },
                trailingIcon = {
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.testTag("add_product_scan_barcode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = SaffronPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("product_barcode_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Category Selection Spinner Box
            var catDropdownExpanded by remember { mutableStateOf(false) }
            var showNewCategoryDialog by remember { mutableStateOf(false) }
            var newCategoryName by remember { mutableStateOf("") }
            val selectedCategoryName = remember(categoryId, categories) {
                categories.find { it.id == categoryId }?.name ?: "General"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { catDropdownExpanded = true }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.category, fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                AppDropdownMenuSurface(
                    expanded = catDropdownExpanded,
                    onDismissRequest = { catDropdownExpanded = false }
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
                                    categoryId = cat.id
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = BorderStrong)
                    AppDropdownMenuItem(
                        text = strings.addNewCategory,
                        onClick = {
                            catDropdownExpanded = false
                            newCategoryName = ""
                            showNewCategoryDialog = true
                        },
                        emphasized = true
                    )
                }
            }

            if (showNewCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showNewCategoryDialog = false },
                    title = {
                        Text(
                            text = if (settings.appLanguage == AppLanguage.HINDI) "नई कैटेगरी जोड़ें" else "Add New Category",
                            fontWeight = FontWeight.Black,
                            color = SaffronDark
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            singleLine = true,
                            label = {
                                Text(if (settings.appLanguage == AppLanguage.HINDI) "कैटेगरी नाम" else "Category name")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextNearBlack,
                                unfocusedTextColor = TextNearBlack,
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = BorderStrong
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                inventoryViewModel.addCategory(newCategoryName) { created ->
                                    categoryId = created.id
                                    newCategoryName = ""
                                    showNewCategoryDialog = false
                                    val message = if (settings.appLanguage == AppLanguage.HINDI) {
                                        "कैटेगरी जोड़ दी गई"
                                    } else {
                                        "Category added"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = newCategoryName.trim().isNotEmpty()
                        ) {
                            Text(if (settings.appLanguage == AppLanguage.HINDI) "जोड़ें" else "Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewCategoryDialog = false }) {
                            Text(strings.cancel, color = TextMediumGray)
                        }
                    }
                )
            }

            // MRP
            OutlinedTextField(
                value = mrp,
                onValueChange = {
                    mrp = it
                    if (MoneyUtils.parseMajorUnits(it) != null) mrpError = false
                },
                label = { Text(strings.mrpPrice) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = mrpError,
                supportingText = {
                    if (mrpError) {
                        val err = if (settings.appLanguage == AppLanguage.HINDI) "वैध MRP संख्या दर्ज करें!" else "Enter valid numeric MRP!"
                        Text(err, color = Color.Red)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("product_mrp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Selling Price (SP)
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it },
                label = { Text(strings.sellingPrice) },
                placeholder = { Text(mrp.ifEmpty { "10" }) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("product_sp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Purchase Price
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text(strings.purchasePrice) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            HorizontalDivider()

            // Track stock panel switches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(strings.trackStock, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    val trackSub = if (settings.appLanguage == AppLanguage.HINDI) "बिक्री पर स्टॉक स्वतः घटेगा" else "Stock reduces automatically on billing"
                    Text(trackSub, fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }

            if (trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Current Stock count input
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text(strings.currentStock) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Low Stock AlertsQty
                    OutlinedTextField(
                        value = lowStockQty,
                        onValueChange = { lowStockQty = it },
                        label = { Text(strings.lowStockAlertQty) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Inactive product toggle switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val activeLabel = if (settings.appLanguage == AppLanguage.HINDI) "सक्रिय सामान" else "Active Product"
                    val activeSub = if (settings.appLanguage == AppLanguage.HINDI) "निष्क्रिय करने पर बिलिंग में नहीं दिखेगा" else "Inactive items are hidden from billing"
                    Text(activeLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(activeSub, fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Product
            Button(
                onClick = {
                    val mrpValue = MoneyUtils.parseMajorUnits(mrp)
                    val spValue = MoneyUtils.parseMajorUnits(sellingPrice)
                    val purValue = MoneyUtils.parseMajorUnits(purchasePrice)
                    val stockValue = currentStock.trim().toDoubleOrNull() ?: 0.0
                    val alertValue = lowStockQty.trim().toDoubleOrNull() ?: 5.0

                    // Validations checks
                    if (name.trim().isEmpty()) {
                        nameError = true
                    }
                    if (mrpValue == null || mrpValue <= 0L) {
                        mrpError = true
                    }

                    if (name.trim().isNotEmpty() && mrpValue != null && mrpValue > 0L) {
                        inventoryViewModel.saveProduct(
                            id = productId ?: 0L,
                            name = name,
                            categoryId = categoryId,
                            mrp = mrpValue,
                            sellingPrice = if (spValue != null && spValue > 0L) spValue else null,
                            purchasePrice = if (purValue != null && purValue > 0L) purValue else null,
                            currentStock = if (trackStock) stockValue else 0.0,
                            trackStock = trackStock,
                            lowStockAlertQty = alertValue,
                            isActive = isActive,
                            barcode = barcode,
                            onSuccess = {
                                val successMsg = if (settings.appLanguage == AppLanguage.HINDI) "${name.trim()} सुरक्षित हो गया!" else "${name.trim()} saved successfully!"
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo(Screen.Products)
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_product_button")
            ) {
                Text(strings.saveProduct, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (showBarcodeScanner) {
                BarcodeScannerDialog(
                    onDismiss = { showBarcodeScanner = false },
                    onBarcodeScanned = { scannedCode ->
                        showBarcodeScanner = false
                        barcode = scannedCode.trim()
                        val msg = if (settings.appLanguage == AppLanguage.HINDI) {
                            "बारकोड दर्ज हुआ: $scannedCode"
                        } else {
                            "Barcode set: $scannedCode"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}


// ==========================================
// 3. OPENING STOCK ENTRY (BULK ADD PANEL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningStockScreen(viewModel: ShopViewModel, inventoryViewModel: InventoryViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    val categories by inventoryViewModel.categories.collectAsState()
    val products by inventoryViewModel.products.collectAsState()

    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    
    // Quick Add input values
    var name by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sp by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("10") }
    var trackStock by remember { mutableStateOf(true) }

    LaunchedEffect(categories) {
        if (selectedCatId == null && categories.isNotEmpty()) {
            selectedCatId = categories.first().id
        }
    }

    val productsInSelectedCategory = remember(products, selectedCatId) {
        products.filter { it.categoryId == selectedCatId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.openingStockTitle, fontWeight = FontWeight.Bold) },
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
                .background(Color(0xFFF7F9FC))
        ) {
            val chooseCatLabel = if (settings.appLanguage == AppLanguage.HINDI) "कैटेगरी चुनें:" else "Choose Category:"
            Text(
                chooseCatLabel,
                fontSize = 14.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            selectedCatId?.let { catId ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val fastAddHeader = if (settings.appLanguage == AppLanguage.HINDI) {
                            "त्वरित सामान जोड़ें: ${categories.find { it.id == catId }?.name}"
                        } else {
                            "Fast Add: ${categories.find { it.id == catId }?.name}"
                        }
                        Text(
                            fastAddHeader,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text(strings.productName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("opening_stock_item_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = mrp,
                                onValueChange = { mrp = it },
                                label = { Text(strings.mrpPrice) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("opening_stock_item_mrp"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = sp,
                                onValueChange = { sp = it },
                                label = { Text(strings.sellingPrice) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("opening_stock_item_sp"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = { Text(strings.currentStock) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f).testTag("opening_stock_item_qty"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = trackStock
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(strings.trackStock, fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                            }
                        }

                        val addNextBtn = if (settings.appLanguage == AppLanguage.HINDI) "सामान जोड़ें" else "Add Product"
                        Button(
                            onClick = {
                                val mrpValue = MoneyUtils.parseMajorUnits(mrp)
                                val spValue = MoneyUtils.parseMajorUnits(sp)
                                val stockValue = stock.trim().toDoubleOrNull() ?: 0.0

                                if (name.trim().isEmpty() || mrpValue == null || mrpValue <= 0L) {
                                    val err = if (settings.appLanguage == AppLanguage.HINDI) "नाम और कीमत आवश्यक है!" else "Name and MRP price required!"
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                } else {
                                    inventoryViewModel.saveProduct(
                                        id = 0L,
                                        name = name,
                                        categoryId = catId,
                                        mrp = mrpValue,
                                        sellingPrice = if (spValue != null && spValue > 0L) spValue else null,
                                        purchasePrice = null,
                                        currentStock = if (trackStock) stockValue else 0.0,
                                        trackStock = trackStock,
                                        lowStockAlertQty = 5.0,
                                        isActive = true,
                                        onSuccess = {
                                            val addedMsg = if (settings.appLanguage == AppLanguage.HINDI) "${name.trim()} जुड़ गया!" else "${name.trim()} added!"
                                            Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                                            name = ""
                                            mrp = ""
                                            sp = ""
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("opening_stock_fast_save")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(addNextBtn)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val recentItemsTitle = if (settings.appLanguage == AppLanguage.HINDI) "हाल में जोड़े गए सामान:" else "Recently Added Items:"
                    val totalLabel = if (settings.appLanguage == AppLanguage.HINDI) "कुल" else "Total"
                    Text(recentItemsTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("$totalLabel: ${productsInSelectedCategory.size}", fontSize = 12.sp, color = Color.Gray)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (productsInSelectedCategory.isEmpty()) {
                        item {
                            val emptyCategoryMsg = if (settings.appLanguage == AppLanguage.HINDI) "इस कैटेगरी में कोई सामान नहीं है।" else "No items in this category yet."
                            Text(
                                emptyCategoryMsg,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(productsInSelectedCategory.takeLast(10).reversed()) { itemInfo ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itemInfo.name, fontWeight = FontWeight.Bold)
                                        val priceLabel = if (settings.appLanguage == AppLanguage.HINDI) "कीमत" else "Price"
                                        Text(
                                            "$priceLabel: ${CurrencyUtils.formatRupees(itemInfo.getEffectivePrice())}",
                                            fontSize = 12.sp, color = Color.DarkGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (itemInfo.trackStock) {
                                            val stockLabel = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक" else "Stock"
                                            Text("$stockLabel: ${itemInfo.currentStock}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            IconButton(onClick = {
                                                inventoryViewModel.saveProduct(
                                                    id = itemInfo.id,
                                                    name = itemInfo.name,
                                                    categoryId = itemInfo.categoryId,
                                                    mrp = itemInfo.mrp,
                                                    sellingPrice = itemInfo.sellingPrice,
                                                    purchasePrice = itemInfo.purchasePrice,
                                                    currentStock = itemInfo.currentStock + 1,
                                                    trackStock = itemInfo.trackStock,
                                                    lowStockAlertQty = itemInfo.lowStockAlertQty,
                                                    isActive = itemInfo.isActive
                                                )
                                            }) {
                                                Icon(Icons.Default.AddCircleOutline, "Plus 1 to Stock", tint = Color.Gray)
                                            }
                                        } else {
                                            val untracked = if (settings.appLanguage == AppLanguage.HINDI) "अनट्रैक्ड" else "Untracked"
                                            Text(untracked, fontSize = 11.sp, color = Color.Gray)
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


// ==========================================
// 4. MANUAL STOCK ADJUSTMENTS HISTORY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    viewModel: ShopViewModel,
    inventoryViewModel: InventoryViewModel,
    productId: Long
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }

    var product by remember { mutableStateOf<Product?>(null) }
    var countedStock by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक मिलान" else "Stock count correction") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val reasons = if (settings.appLanguage == AppLanguage.HINDI) {
        listOf("स्टॉक मिलान", "नया माल आया", "खराब या एक्सपायर सामान", "ओपनिंग स्टॉक प्रविष्टि", "अन्य")
    } else {
        listOf("Stock count correction", "Purchase added", "Damaged or expired", "Opening stock entry", "Other")
    }

    val adjustmentHistory = inventoryViewModel.getAdjustmentsForProduct(productId).collectAsState(initial = emptyList())

    LaunchedEffect(productId) {
        product = inventoryViewModel.getProduct(productId)
        product?.let {
            countedStock = it.currentStock.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.stockAdjustmentTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Products) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        product?.let { prod ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF7F9FC))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "${strings.productName}: ${prod.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentStockMsg = if (settings.appLanguage == AppLanguage.HINDI) "वर्तमान स्टॉक: ${prod.currentStock}" else "Current Stock: ${prod.currentStock}"
                        Text(
                            text = currentStockMsg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                    }
                }

                // Count input
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val enterActualLabel = if (settings.appLanguage == AppLanguage.HINDI) "दुकान में मौजूद वास्तविक स्टॉक संख्या दर्ज करें:" else "Enter actual physical stock count:"
                        Text(enterActualLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = countedStock,
                            onValueChange = { countedStock = it },
                            label = { Text(strings.currentStock) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("adjustment_stock_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Reason dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val reasonLabel = if (settings.appLanguage == AppLanguage.HINDI) "कारण" else "Reason"
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$reasonLabel: $selectedReason")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            AppDropdownMenuSurface(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                if (reasons.isEmpty()) {
                                    AppDropdownMenuItem(
                                        text = strings.noReasonsAvailable,
                                        onClick = {},
                                        enabled = false
                                    )
                                } else {
                                    reasons.forEach { reas ->
                                        AppDropdownMenuItem(
                                            text = reas,
                                            onClick = {
                                                selectedReason = reas
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        val saveCorrectionBtn = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक सुधार सुरक्षित करें" else "Save Stock Adjustment"
                        Button(
                            onClick = {
                                val countVal = countedStock.toDoubleOrNull()
                                if (countVal == null || countVal < 0.0) {
                                    val err = if (settings.appLanguage == AppLanguage.HINDI) "वैध स्टॉक संख्या आवश्यक है!" else "Valid stock quantity required!"
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                } else {
                                    inventoryViewModel.adjustStock(
                                        productId = productId,
                                        actualStockCounted = countVal,
                                        reason = selectedReason
                                    )
                                    product = prod.copy(currentStock = countVal)
                                    val success = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक सुधार सुरक्षित हो गया!" else "Stock adjustment saved!"
                                    Toast.makeText(context, success, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("adjustment_save_button")
                        ) {
                            Text(saveCorrectionBtn)
                        }
                    }
                }

                // History adjustments lists logger
                val historyTitle = if (settings.appLanguage == AppLanguage.HINDI) "स्टॉक सुधार इतिहास:" else "Stock Adjustment History:"
                Text(historyTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(adjustmentHistory.value) { record ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = record.reason,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                    val prefix = if (record.difference >= 0) "+" else ""
                                    Text(
                                        text = "$prefix${record.difference}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (record.difference >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val changeMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                                        "स्टॉक: ${record.oldStock} ➔ ${record.newStock}"
                                    } else {
                                        "Stock: ${record.oldStock} ➔ ${record.newStock}"
                                    }
                                    Text(
                                        changeMsg,
                                        fontSize = 12.sp, color = Color.Gray
                                    )
                                    Text(
                                        DateTimeUtils.formatDateTime(record.createdAt),
                                        fontSize = 11.sp, color = Color.LightGray
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

// ==========================================
// 6. LOW STOCK RE-ORDER LIST DIALOG
// ==========================================
@Composable
fun LowStockReorderDialog(
    viewModel: ShopViewModel,
    lowStockProducts: List<Product>,
    categories: List<Category>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val strings = remember(settings.appLanguage) { LocaleHelper.getStrings(settings.appLanguage) }
    val categoryMap = remember(categories) { categories.associate { it.id to it.name } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("low_stock_reorder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(24.dp))
                            Text(
                                text = strings.wholesaleReorderList,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextNearBlack
                            )
                        }
                        val countMsg = if (settings.appLanguage == AppLanguage.HINDI) {
                            "कुल ${lowStockProducts.size} सामान कम हैं"
                        } else {
                            "Total ${lowStockProducts.size} items low in stock"
                        }
                        Text(
                            text = countMsg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Share
                    Button(
                        onClick = {
                            viewModel.shareReorderListViaWhatsApp(context, lowStockProducts, categories)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("share_reorder_whatsapp_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val whatsappOrder = if (settings.appLanguage == AppLanguage.HINDI) "WhatsApp ऑर्डर" else "WhatsApp Order"
                            Text(whatsappOrder, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Copy Button
                    OutlinedButton(
                        onClick = {
                            viewModel.copyReorderListToClipboard(context, lowStockProducts, categories)
                        },
                        border = BorderStroke(1.2.dp, BorderStrong),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNearBlack),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("copy_reorder_list_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, null, tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val copyBtn = if (settings.appLanguage == AppLanguage.HINDI) "कॉपी" else "Copy"
                            Text(copyBtn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderStrong)
                Spacer(modifier = Modifier.height(8.dp))

                if (lowStockProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            val noLowStockHeader = if (settings.appLanguage == AppLanguage.HINDI) "कोई सामान कम नहीं है!" else "No low stock items!"
                            val noLowStockSub = if (settings.appLanguage == AppLanguage.HINDI) "दुकान में पर्याप्त स्टॉक मौजूद है।" else "Store inventory is well stocked."
                            Text(
                                text = noLowStockHeader,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = noLowStockSub,
                                fontSize = 13.sp,
                                color = TextMediumGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lowStockProducts) { item ->
                            val catName = categoryMap[item.categoryId] ?: "General"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = WarmCreamBg),
                                border = BorderStroke(1.dp, BorderStrong)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = TextNearBlack
                                            )
                                            Text(
                                                text = "$catName | MRP: ${CurrencyUtils.formatRupees(item.mrp)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMediumGray
                                            )
                                        }

                                        // Status Pill
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (item.currentStock <= 0) ErrorRedLight else WarningOrangeLight,
                                            border = BorderStroke(1.dp, if (item.currentStock <= 0) ErrorRed else WarningOrange)
                                        ) {
                                            val pillText = if (item.currentStock <= 0) {
                                                strings.outOfStock
                                            } else {
                                                if (settings.appLanguage == AppLanguage.HINDI) "बचा: ${item.currentStock}" else "Left: ${item.currentStock}"
                                            }
                                            Text(
                                                text = pillText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (item.currentStock <= 0) ErrorRed else WarningOrange,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Restock Steppers (+5, +10, +25, +50)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val restockLabel = if (settings.appLanguage == AppLanguage.HINDI) "माल जोड़ें:" else "Add stock:"
                                        Text(
                                            text = restockLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextNearBlack
                                        )

                                        listOf(5, 10, 25, 50).forEach { qty ->
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.bulkRestockProduct(item, qty.toDouble())
                                                    val toastMsg = if (settings.appLanguage == AppLanguage.HINDI) "+$qty स्टॉक जोड़ा गया (${item.name})" else "+$qty stock added (${item.name})"
                                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f)),
                                                colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("+$qty", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SaffronDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val closeBtn = if (settings.appLanguage == AppLanguage.HINDI) "बंद करें" else "Close"
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateContainer, contentColor = TextNearBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(closeBtn, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
